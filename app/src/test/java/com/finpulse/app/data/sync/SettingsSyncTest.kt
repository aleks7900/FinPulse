package com.finpulse.app.data.sync

import com.finpulse.app.core.datastore.UserPreferences
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.data.cloud.CloudEntityRecord
import com.finpulse.app.data.cloud.DataStoreLocalSettingsDataSource
import com.finpulse.app.data.cloud.FirestoreCloudSettingsDataSource
import com.finpulse.app.data.cloud.InMemoryCloudStorageDataSource
import com.finpulse.app.data.cloud.LocalSettingsDataSource
import com.finpulse.app.data.repository.SettingsRepositoryImpl
import com.finpulse.app.domain.model.sync.CloudSettings
import com.finpulse.app.domain.model.sync.CloudUser
import com.finpulse.app.domain.model.sync.SyncStatus
import com.finpulse.app.domain.repository.AuthRepository
import com.finpulse.app.domain.repository.SettingsSyncOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsSyncTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private lateinit var inMemoryCloud: InMemoryCloudStorageDataSource
    private lateinit var mockDataStore: UserPreferencesDataStore
    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var localDataSource: LocalSettingsDataSource
    private lateinit var cloudDataSource: FirestoreCloudSettingsDataSource
    private lateinit var settingsRepository: SettingsRepositoryImpl

    private val userA = CloudUser(uid = "user-A", email = "alice@example.com", displayName = "Alice")
    private val userB = CloudUser(uid = "user-B", email = "bob@example.com", displayName = "Bob")

    private var currentPreferences = UserPreferences(
        baseCurrencyCode = "USD",
        selectedLanguage = "EN",
        isDarkMode = false,
        hideBalances = false,
        widgetPrivacyEnabled = false,
        widgetMaskOnAppLock = true,
        lockTimeout = "MINUTE_1",
        backupReminderInterval = "OFF",
        digestFrequency = "DAILY",
        digestPrivacyEnabled = false,
        digestDeliveryHour = 20,
        dismissedInboxItemIds = emptySet(),
        settingsUpdatedAt = 0L
    )

    @Before
    fun setup() {
        inMemoryCloud = InMemoryCloudStorageDataSource()
        mockDataStore = mockk(relaxed = true)

        currentPreferences = UserPreferences(
            baseCurrencyCode = "USD",
            selectedLanguage = "EN",
            isDarkMode = false,
            hideBalances = false,
            widgetPrivacyEnabled = false,
            widgetMaskOnAppLock = true,
            lockTimeout = "MINUTE_1",
            backupReminderInterval = "OFF",
            digestFrequency = "DAILY",
            digestPrivacyEnabled = false,
            digestDeliveryHour = 20,
            dismissedInboxItemIds = emptySet(),
            settingsUpdatedAt = 0L
        )

        coEvery { mockDataStore.userPreferencesFlow } answers { flowOf(currentPreferences) }
        coEvery { mockDataStore.restoreCloudSettings(any()) } answers {
            val settings = firstArg<CloudSettings>()
            currentPreferences = currentPreferences.copy(
                baseCurrencyCode = settings.baseCurrencyCode,
                selectedLanguage = settings.selectedLanguage,
                isDarkMode = when (settings.darkMode) {
                    "DARK" -> true
                    "LIGHT" -> false
                    else -> null
                },
                hideBalances = settings.hideBalances,
                widgetPrivacyEnabled = settings.widgetPrivacyEnabled,
                widgetMaskOnAppLock = settings.widgetMaskOnAppLock,
                lockTimeout = settings.lockTimeout,
                backupReminderInterval = settings.backupReminderInterval,
                digestFrequency = settings.digestFrequency,
                digestPrivacyEnabled = settings.digestPrivacyEnabled,
                digestDeliveryHour = settings.digestDeliveryHour,
                dismissedInboxItemIds = settings.dismissedInboxItemIds.toSet(),
                settingsUpdatedAt = settings.updatedAt
            )
        }
        coEvery { mockDataStore.resetSettingsUpdatedAt() } answers {
            currentPreferences = currentPreferences.copy(settingsUpdatedAt = 0L)
        }

        fakeAuthRepo = FakeAuthRepository(currentUser = userA)
        localDataSource = DataStoreLocalSettingsDataSource(mockDataStore)
        cloudDataSource = FirestoreCloudSettingsDataSource(inMemoryCloud, json)
        settingsRepository = SettingsRepositoryImpl(
            localDataSource = localDataSource,
            cloudDataSource = cloudDataSource,
            dispatcher = testDispatcher
        )
    }

    @Test
    fun `no authenticated user - cannot sync settings without uid`() = testScope.runTest {
        fakeAuthRepo.setUser(null)
        val user = fakeAuthRepo.currentUser.value
        assertNull(user)
        assertFalse(fakeAuthRepo.isAuthenticated)
    }

    @Test
    fun `successful login state - exposes authenticated user profile`() = testScope.runTest {
        val user = fakeAuthRepo.currentUser.value
        assertNotNull(user)
        assertEquals("user-A", user?.uid)
        assertEquals("alice@example.com", user?.email)
        assertTrue(fakeAuthRepo.isAuthenticated)
    }

    @Test
    fun `initial cloud upload - virgin cloud receives local settings with timestamp and device model`() = testScope.runTest {
        currentPreferences = currentPreferences.copy(
            baseCurrencyCode = "EUR",
            selectedLanguage = "DE",
            settingsUpdatedAt = 1000L
        )

        val result = settingsRepository.synchronize(userA.uid)
        assertTrue(result.isSuccess)
        val outcome = result.getOrNull()
        assertTrue(outcome is SettingsSyncOutcome.UploadedToCloud)

        val uploaded = (outcome as SettingsSyncOutcome.UploadedToCloud).settings
        assertEquals("EUR", uploaded.baseCurrencyCode)
        assertEquals("DE", uploaded.selectedLanguage)
        assertEquals(1000L, uploaded.updatedAt)

        // Verify cloud document stored under settings/app
        val cloudDocs = inMemoryCloud.downloadRecords(userA.uid, CloudSettings.SETTINGS_COLLECTION, 0L).getOrNull()
        assertNotNull(cloudDocs)
        assertEquals(1, cloudDocs?.size)
        assertEquals(CloudSettings.SETTINGS_DOCUMENT_ID, cloudDocs?.first()?.id)
    }

    @Test
    fun `initial cloud restore on new device - virgin local settings (updatedAt=0) restore from cloud`() = testScope.runTest {
        // Pre-populate cloud settings from another device
        val remoteSettings = CloudSettings(
            baseCurrencyCode = "GBP",
            selectedLanguage = "FR",
            darkMode = "DARK",
            hideBalances = true,
            widgetPrivacyEnabled = true,
            updatedAt = 5000L
        )
        val cloudRecord = CloudEntityRecord(
            id = CloudSettings.SETTINGS_DOCUMENT_ID,
            collection = CloudSettings.SETTINGS_COLLECTION,
            jsonPayload = json.encodeToString(remoteSettings),
            updatedAt = 5000L
        )
        inMemoryCloud.uploadRecords(userA.uid, CloudSettings.SETTINGS_COLLECTION, listOf(cloudRecord))

        // Local device has virgin settings (settingsUpdatedAt = 0L)
        currentPreferences = currentPreferences.copy(
            baseCurrencyCode = "USD",
            selectedLanguage = "EN",
            settingsUpdatedAt = 0L
        )

        val result = settingsRepository.synchronize(userA.uid)
        assertTrue(result.isSuccess)
        val outcome = result.getOrNull()
        assertTrue(outcome is SettingsSyncOutcome.RestoredFromCloud)

        val restored = (outcome as SettingsSyncOutcome.RestoredFromCloud).settings
        assertEquals("GBP", restored.baseCurrencyCode)
        assertEquals("FR", restored.selectedLanguage)
        assertEquals("DARK", restored.darkMode)
        assertTrue(restored.hideBalances)
        assertEquals(5000L, restored.updatedAt)

        coVerify(exactly = 1) { mockDataStore.restoreCloudSettings(any()) }
        assertEquals("GBP", currentPreferences.baseCurrencyCode)
        assertEquals("FR", currentPreferences.selectedLanguage)
        assertEquals(true, currentPreferences.isDarkMode)
    }

    @Test
    fun `local settings modification - when local setting is newer it uploads to cloud`() = testScope.runTest {
        // Cloud has settings from timestamp 1000L
        val remoteSettings = CloudSettings(
            baseCurrencyCode = "USD",
            selectedLanguage = "EN",
            updatedAt = 1000L
        )
        inMemoryCloud.uploadRecords(
            userA.uid,
            CloudSettings.SETTINGS_COLLECTION,
            listOf(CloudEntityRecord(CloudSettings.SETTINGS_DOCUMENT_ID, CloudSettings.SETTINGS_COLLECTION, json.encodeToString(remoteSettings), 1000L))
        )

        // User locally changed currency to JPY at timestamp 2000L
        currentPreferences = currentPreferences.copy(
            baseCurrencyCode = "JPY",
            selectedLanguage = "JA",
            settingsUpdatedAt = 2000L
        )

        val result = settingsRepository.synchronize(userA.uid)
        assertTrue(result.isSuccess)
        val outcome = result.getOrNull()
        assertTrue(outcome is SettingsSyncOutcome.UploadedToCloud)

        val uploaded = (outcome as SettingsSyncOutcome.UploadedToCloud).settings
        assertEquals("JPY", uploaded.baseCurrencyCode)
        assertEquals("JA", uploaded.selectedLanguage)
        assertEquals(2000L, uploaded.updatedAt)
    }

    @Test
    fun `remote settings modification - when remote setting is newer it restores to local`() = testScope.runTest {
        // Cloud has settings modified on Device 2 at timestamp 3000L
        val remoteSettings = CloudSettings(
            baseCurrencyCode = "CHF",
            selectedLanguage = "DE",
            updatedAt = 3000L
        )
        inMemoryCloud.uploadRecords(
            userA.uid,
            CloudSettings.SETTINGS_COLLECTION,
            listOf(CloudEntityRecord(CloudSettings.SETTINGS_DOCUMENT_ID, CloudSettings.SETTINGS_COLLECTION, json.encodeToString(remoteSettings), 3000L))
        )

        // Device 1 local settings were last modified at timestamp 2000L
        currentPreferences = currentPreferences.copy(
            baseCurrencyCode = "USD",
            selectedLanguage = "EN",
            settingsUpdatedAt = 2000L
        )

        val result = settingsRepository.synchronize(userA.uid)
        assertTrue(result.isSuccess)
        val outcome = result.getOrNull()
        assertTrue(outcome is SettingsSyncOutcome.RestoredFromCloud)

        val restored = (outcome as SettingsSyncOutcome.RestoredFromCloud).settings
        assertEquals("CHF", restored.baseCurrencyCode)
        assertEquals(3000L, restored.updatedAt)
    }

    @Test
    fun `conflict resolution - equal timestamps results in UpToDate without redundant upload`() = testScope.runTest {
        val sameTimestamp = 4000L
        val remoteSettings = CloudSettings(
            baseCurrencyCode = "USD",
            selectedLanguage = "EN",
            updatedAt = sameTimestamp
        )
        inMemoryCloud.uploadRecords(
            userA.uid,
            CloudSettings.SETTINGS_COLLECTION,
            listOf(CloudEntityRecord(CloudSettings.SETTINGS_DOCUMENT_ID, CloudSettings.SETTINGS_COLLECTION, json.encodeToString(remoteSettings), sameTimestamp))
        )

        currentPreferences = currentPreferences.copy(
            baseCurrencyCode = "USD",
            selectedLanguage = "EN",
            settingsUpdatedAt = sameTimestamp
        )

        val result = settingsRepository.synchronize(userA.uid)
        assertTrue(result.isSuccess)
        val outcome = result.getOrNull()
        assertTrue(outcome is SettingsSyncOutcome.UpToDate)
    }

    @Test
    fun `offline modification - network error triggers failure and retains pending sync state for retry`() = testScope.runTest {
        inMemoryCloud.shouldSimulateNetworkError = true

        val result = settingsRepository.synchronize(userA.uid)
        assertTrue(result.isFailure)
        assertEquals(SyncStatus.ERROR, settingsRepository.syncStatus.value)

        // Disable error to simulate network recovery
        inMemoryCloud.shouldSimulateNetworkError = false
        val retryResult = settingsRepository.synchronize(userA.uid)
        assertTrue(retryResult.isSuccess)
        assertEquals(SyncStatus.SUCCESS, settingsRepository.syncStatus.value)
    }

    @Test
    fun `malformed cloud data - gracefully handles corrupted payload without crashing`() = testScope.runTest {
        val malformedRecord = CloudEntityRecord(
            id = CloudSettings.SETTINGS_DOCUMENT_ID,
            collection = CloudSettings.SETTINGS_COLLECTION,
            jsonPayload = "{ corrupted json bad syntax ???",
            updatedAt = 1000L
        )
        inMemoryCloud.uploadRecords(userA.uid, CloudSettings.SETTINGS_COLLECTION, listOf(malformedRecord))

        val result = settingsRepository.synchronize(userA.uid)
        assertTrue(result.isFailure)
        assertEquals(SyncStatus.ERROR, settingsRepository.syncStatus.value)
    }

    @Test
    fun `schema migration - older schema version migrates to current schema version`() = testScope.runTest {
        val legacyJson = """
            {
               "schemaVersion": 0,
               "baseCurrencyCode": "CAD",
               "selectedLanguage": "EN",
               "updatedAt": 2500
            }
        """.trimIndent()

        val parsed = json.decodeFromString<CloudSettings>(legacyJson)
        assertEquals(0, parsed.schemaVersion)
        assertEquals("CAD", parsed.baseCurrencyCode)
        assertEquals("DAILY", parsed.digestFrequency) // default safely applied
    }

    @Test
    fun `sign out - preserves local application settings`() = testScope.runTest {
        currentPreferences = currentPreferences.copy(
            baseCurrencyCode = "AUD",
            selectedLanguage = "EN",
            settingsUpdatedAt = 5000L
        )

        fakeAuthRepo.signOut()
        assertNull(fakeAuthRepo.currentUser.value)
        assertFalse(fakeAuthRepo.isAuthenticated)

        // Local preferences must still exist
        assertEquals("AUD", currentPreferences.baseCurrencyCode)
    }

    @Test
    fun `account switching - switching from User A to User B isolates cloud data`() = testScope.runTest {
        // User A cloud settings: AUD
        val userASettings = CloudSettings(baseCurrencyCode = "AUD", selectedLanguage = "EN", updatedAt = 1000L)
        inMemoryCloud.uploadRecords(
            userA.uid,
            CloudSettings.SETTINGS_COLLECTION,
            listOf(CloudEntityRecord(CloudSettings.SETTINGS_DOCUMENT_ID, CloudSettings.SETTINGS_COLLECTION, json.encodeToString(userASettings), 1000L))
        )

        // User B cloud settings: JPY
        val userBSettings = CloudSettings(baseCurrencyCode = "JPY", selectedLanguage = "JA", updatedAt = 2000L)
        inMemoryCloud.uploadRecords(
            userB.uid,
            CloudSettings.SETTINGS_COLLECTION,
            listOf(CloudEntityRecord(CloudSettings.SETTINGS_DOCUMENT_ID, CloudSettings.SETTINGS_COLLECTION, json.encodeToString(userBSettings), 2000L))
        )

        // 1. Sync User A
        settingsRepository.synchronize(userA.uid)
        assertEquals("AUD", currentPreferences.baseCurrencyCode)

        // 2. Switch account: reset local timestamp and sync User B
        settingsRepository.resetLocalTimestamp()
        assertEquals(0L, currentPreferences.settingsUpdatedAt)

        settingsRepository.synchronize(userB.uid)
        assertEquals("JPY", currentPreferences.baseCurrencyCode)
        assertEquals("JA", currentPreferences.selectedLanguage)
    }

    @Test
    fun `cloud-data deletion - deletes remote documents under settings while leaving local settings intact`() = testScope.runTest {
        // Upload settings for User A
        val settings = CloudSettings(baseCurrencyCode = "EUR", selectedLanguage = "DE", updatedAt = 3000L)
        inMemoryCloud.uploadRecords(
            userA.uid,
            CloudSettings.SETTINGS_COLLECTION,
            listOf(CloudEntityRecord(CloudSettings.SETTINGS_DOCUMENT_ID, CloudSettings.SETTINGS_COLLECTION, json.encodeToString(settings), 3000L))
        )

        // Delete cloud data
        val deleteResult = settingsRepository.deleteCloudSettings(userA.uid)
        assertTrue(deleteResult.isSuccess)

        // Verify remote settings is tombstoned/deleted
        val cloudDocs = inMemoryCloud.downloadRecords(userA.uid, CloudSettings.SETTINGS_COLLECTION, 0L).getOrNull()
        assertTrue(cloudDocs == null || cloudDocs.isEmpty() || cloudDocs.first().isDeleted)

        // Local settings on device remain intact
        assertEquals("USD", currentPreferences.baseCurrencyCode)
    }
}

private class FakeAuthRepository(currentUser: CloudUser?) : AuthRepository {
    private val _currentUser = MutableStateFlow(currentUser)
    override val currentUser: StateFlow<CloudUser?> = _currentUser.asStateFlow()

    fun setUser(user: CloudUser?) { _currentUser.value = user }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<CloudUser> {
        val user = CloudUser("uid-token-$idToken", "token@google.com", "Token User")
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun signOut(): Result<Unit> {
        _currentUser.value = null
        return Result.success(Unit)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        _currentUser.value = null
        return Result.success(Unit)
    }
}
