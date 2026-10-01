package md.alexlab.finpulse.data.auth

import md.alexlab.finpulse.core.datastore.UserPreferences
import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.data.repository.FirebaseAuthRepositoryImpl
import md.alexlab.finpulse.domain.model.sync.CloudUser
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockDataStore: UserPreferencesDataStore
    private lateinit var mockFirebaseAuth: FirebaseAuth
    private lateinit var mockFirebaseUser: FirebaseUser

    @Before
    fun setup() {
        mockDataStore = mockk(relaxed = true)
        mockFirebaseAuth = mockk(relaxed = true)
        mockFirebaseUser = mockk(relaxed = true)
    }

    @Test
    fun `init restores session from UserPreferencesDataStore when Firebase user is null`() = testScope.runTest {
        val savedPrefs = UserPreferences(
            currentUserId = "saved-uid-123",
            userEmail = "saved@finpulse.app",
            userDisplayName = "Saved FinPulse User",
            userPhotoUrl = "https://example.com/photo.png"
        )
        every { mockFirebaseAuth.currentUser } returns null
        coEvery { mockDataStore.userPreferencesFlow } returns flowOf(savedPrefs)

        val repository = FirebaseAuthRepositoryImpl(
            userPreferencesDataStore = mockDataStore,
            firebaseAuthProvider = { mockFirebaseAuth },
            scope = testScope
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val currentUser = repository.currentUser.value
        assertNotNull(currentUser)
        assertEquals("saved-uid-123", currentUser?.uid)
        assertEquals("saved@finpulse.app", currentUser?.email)
        assertEquals("Saved FinPulse User", currentUser?.displayName)
    }

    @Test
    fun `signOut clears local datastore session and sets current user to null`() = testScope.runTest {
        val savedPrefs = UserPreferences(currentUserId = "uid-to-signout")
        coEvery { mockDataStore.userPreferencesFlow } returns flowOf(savedPrefs)

        val repository = FirebaseAuthRepositoryImpl(
            userPreferencesDataStore = mockDataStore,
            firebaseAuthProvider = { mockFirebaseAuth },
            scope = testScope
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val result = repository.signOut()
        assertTrue(result.isSuccess)
        assertNull(repository.currentUser.value)
        coVerify(exactly = 1) { mockDataStore.clearUserSession() }
    }

    @Test
    fun `signInWithGoogleIdToken authenticates, saves session, and exposes CloudUser`() = testScope.runTest {
        mockkStatic(GoogleAuthProvider::class)
        val mockCredential = mockk<AuthCredential>()
        every { GoogleAuthProvider.getCredential("sample-id-token", null) } returns mockCredential

        val mockAuthResult = mockk<AuthResult>()
        val mockNewFirebaseUser = mockk<FirebaseUser>(relaxed = true)
        every { mockNewFirebaseUser.uid } returns "new-google-uid-789"
        every { mockNewFirebaseUser.email } returns "user@google.com"
        every { mockNewFirebaseUser.displayName } returns "Google FinPulse User"
        every { mockNewFirebaseUser.photoUrl } returns null
        every { mockNewFirebaseUser.isAnonymous } returns false
        every { mockAuthResult.user } returns mockNewFirebaseUser

        val mockSignInTask = mockk<Task<AuthResult>>()
        every { mockSignInTask.isComplete } returns true
        every { mockSignInTask.isSuccessful } returns true
        every { mockSignInTask.isCanceled } returns false
        every { mockSignInTask.result } returns mockAuthResult
        every { mockSignInTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<AuthResult>>()
            listener.onSuccess(mockAuthResult)
            mockSignInTask
        }
        every { mockSignInTask.addOnFailureListener(any()) } returns mockSignInTask
        every { mockSignInTask.addOnCanceledListener(any()) } returns mockSignInTask

        every { mockFirebaseAuth.signInWithCredential(mockCredential) } returns mockSignInTask

        val repository = FirebaseAuthRepositoryImpl(
            userPreferencesDataStore = mockDataStore,
            firebaseAuthProvider = { mockFirebaseAuth },
            scope = testScope
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val result = repository.signInWithGoogleIdToken("sample-id-token")
        assertTrue(result.isSuccess)

        val cloudUser = result.getOrNull()
        assertNotNull(cloudUser)
        assertEquals("new-google-uid-789", cloudUser?.uid)
        assertEquals("user@google.com", cloudUser?.email)
        assertEquals("Google FinPulse User", cloudUser?.displayName)

        coVerify(exactly = 1) {
            mockDataStore.setUserSession(
                uid = "new-google-uid-789",
                email = "user@google.com",
                displayName = "Google FinPulse User",
                photoUrl = null
            )
        }
        assertEquals("new-google-uid-789", repository.currentUser.value?.uid)
    }

    @Test
    fun `deleteAccount deletes firebase user and clears local session`() = testScope.runTest {
        val mockDeleteTask = mockk<Task<Void>>()
        every { mockDeleteTask.isComplete } returns true
        every { mockDeleteTask.isSuccessful } returns true
        every { mockDeleteTask.isCanceled } returns false
        every { mockDeleteTask.result } returns null
        every { mockDeleteTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<Void>>()
            listener.onSuccess(null)
            mockDeleteTask
        }
        every { mockDeleteTask.addOnFailureListener(any()) } returns mockDeleteTask
        every { mockDeleteTask.addOnCanceledListener(any()) } returns mockDeleteTask

        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
        every { mockFirebaseUser.delete() } returns mockDeleteTask

        val repository = FirebaseAuthRepositoryImpl(
            userPreferencesDataStore = mockDataStore,
            firebaseAuthProvider = { mockFirebaseAuth },
            scope = testScope
        )

        val result = repository.deleteAccount()
        assertTrue(result.isSuccess)
        assertNull(repository.currentUser.value)
        coVerify(exactly = 1) { mockFirebaseUser.delete() }
        coVerify(exactly = 1) { mockDataStore.clearUserSession() }
    }
}
