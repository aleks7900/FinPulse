package md.alexlab.finpulse.presentation.account

import android.app.Activity
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.AmberWarning
import md.alexlab.finpulse.core.designsystem.CrimsonExpense
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.designsystem.SapphireAccent
import md.alexlab.finpulse.domain.model.sync.SyncStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    AccountScreen(
        uiState = uiState,
        onSignInWithGoogle = { ctx, webClientId -> viewModel.signInWithGoogle(ctx, webClientId) },
        onSyncNow = { ctx -> viewModel.syncNow(ctx) },
        onSignOut = viewModel::signOut,
        onConfirmSignOutWithSync = viewModel::confirmSignOutWithSync,
        onDismissSignOutDialog = viewModel::dismissSignOutDialog,
        onDeleteCloudData = viewModel::requestDeleteCloudData,
        onConfirmDeleteCloudData = viewModel::confirmDeleteCloudData,
        onDismissDeleteCloudDataDialog = viewModel::dismissDeleteCloudDataDialog,
        onDismissMessages = viewModel::clearMessages,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    uiState: AccountUiState,
    onSignInWithGoogle: (Context, String) -> Unit,
    onSyncNow: (Context) -> Unit,
    onSignOut: () -> Unit,
    onConfirmSignOutWithSync: () -> Unit,
    onDismissSignOutDialog: () -> Unit,
    onDeleteCloudData: () -> Unit,
    onConfirmDeleteCloudData: () -> Unit,
    onDismissDeleteCloudDataDialog: () -> Unit,
    onDismissMessages: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Read web client ID from strings or fallback
    val webClientId = remember(context) {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) context.getString(resId) else "500924060314-rfem2fmrnai3c9r3svjk4j86t8e34cpq.apps.googleusercontent.com"
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage, uiState.errorMessageRes, uiState.successMessageRes) {
        val error = uiState.errorMessage ?: uiState.errorMessageRes?.let { context.getString(it) }
        val success = uiState.successMessage ?: uiState.successMessageRes?.let { resId ->
            if (uiState.successMessageArgs.isNotEmpty()) context.getString(resId, *uiState.successMessageArgs.toTypedArray())
            else context.getString(resId)
        }
        error?.let {
            snackbarHostState.showSnackbar(it)
            onDismissMessages()
        }
        success?.let {
            snackbarHostState.showSnackbar(it)
            onDismissMessages()
        }
    }

    if (uiState.showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissSignOutDialog,
            icon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = AmberWarning) },
            title = { Text(stringResource(R.string.account_unsynced_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.account_unsynced_dialog_desc,
                        uiState.pendingChangesCount
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmSignOutWithSync()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(stringResource(R.string.account_sync_and_sign_out))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onSignOut()
                    }
                ) {
                    Text(stringResource(R.string.account_sign_out_anyway), color = CrimsonExpense)
                }
            }
        )
    }

    if (uiState.showDeleteCloudDataDialog) {
        AlertDialog(
            onDismissRequest = onDismissDeleteCloudDataDialog,
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CrimsonExpense) },
            title = { Text(stringResource(R.string.account_delete_cloud_data_title)) },
            text = {
                Text(stringResource(R.string.account_delete_cloud_data_desc))
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDeleteCloudData,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonExpense)
                ) {
                    Text(stringResource(R.string.account_delete_cloud_data_confirm), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteCloudDataDialog) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!uiState.isAuthenticated) {
                LoggedOutContent(
                    isLoading = uiState.isLoading,
                    onSignInClick = {
                        onSignInWithGoogle(context, webClientId)
                    }
                )
            } else {
                LoggedInContent(
                    uiState = uiState,
                    onSyncClick = { onSyncNow(context) },
                    onSignOutClick = onSignOut,
                    onDeleteCloudDataClick = onDeleteCloudData
                )
            }
        }
    }
}


@Composable
private fun LoggedOutContent(
    isLoading: Boolean,
    onSignInClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = "FinPulse Logo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.account_google_account_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = stringResource(R.string.account_not_signed_in),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.account_sync_hero_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )


            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSignInClick,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Google "G" Badge Icon representation
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF4285F4)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.account_continue_with_google),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Benefits Section
    Text(
        text = stringResource(R.string.account_features_title).uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FeatureItem(
            icon = Icons.Default.Devices,
            title = stringResource(R.string.account_feature_multi_device_title),
            description = stringResource(R.string.account_feature_multi_device_desc),
            tint = SapphireAccent
        )
        FeatureItem(
            icon = Icons.Default.CloudOff,
            title = stringResource(R.string.account_feature_offline_title),
            description = stringResource(R.string.account_feature_offline_desc),
            tint = EmeraldPrimary
        )
        FeatureItem(
            icon = Icons.Default.Restore,
            title = stringResource(R.string.account_feature_restore_title),
            description = stringResource(R.string.account_feature_restore_desc),
            tint = EmeraldPrimary
        )
        FeatureItem(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.account_feature_security_title),
            description = stringResource(R.string.account_feature_security_desc),
            tint = SapphireAccent
        )
    }
}

@Composable
private fun LoggedInContent(
    uiState: AccountUiState,
    onSyncClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteCloudDataClick: () -> Unit
) {
    val user = uiState.currentUser

    // 1. User Profile Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar monogram with gradient
            val initials = remember(user) {
                val name = user?.displayName ?: user?.email ?: "U"
                name.take(2).uppercase()
            }
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(EmeraldPrimary, SapphireAccent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.account_google_account_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = user?.displayName ?: stringResource(R.string.account_default_username),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                user?.email?.let { email ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.account_cloud_sync_on),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }


    // 2. Sync Status Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.account_cloud_sync_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                SyncStatusBadge(status = uiState.syncStatus)
            }

            // Last synced time
            val lastSyncedText = remember(uiState.lastSyncTimestamp) {
                if (uiState.lastSyncTimestamp > 0L) {
                    val instant = Instant.ofEpochMilli(uiState.lastSyncTimestamp)
                    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                        .withZone(ZoneId.systemDefault())
                    formatter.format(instant)
                } else {
                    "—"
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.account_last_synced_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = lastSyncedText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (uiState.pendingChangesCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AmberWarning.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = stringResource(R.string.account_pending_changes_count, uiState.pendingChangesCount),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarning,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Sync Now Button
            val isSyncing = uiState.syncStatus == SyncStatus.SYNCING || uiState.isLoading
            val infiniteTransition = rememberInfiniteTransition(label = "sync_rotate")
            val rotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Button(
                onClick = onSyncClick,
                enabled = !isSyncing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .then(if (isSyncing) Modifier.rotate(rotationAngle) else Modifier)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSyncing) stringResource(R.string.account_syncing) else stringResource(R.string.account_sync_now),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // 3. Cloud Security Note
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = SapphireAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.account_security_header),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.account_security_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // 4. Sign Out Button
    OutlinedButton(
        onClick = onSignOutClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = CrimsonExpense
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.account_sign_out), fontWeight = FontWeight.SemiBold)
        }
    }

    // 5. Delete Cloud Data Button
    TextButton(
        onClick = onDeleteCloudDataClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = CrimsonExpense.copy(alpha = 0.8f)
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.account_delete_cloud_data_title), fontWeight = FontWeight.Medium)
        }
    }
}


@Composable
private fun SyncStatusBadge(status: SyncStatus) {
    val (label, icon, color) = when (status) {
        SyncStatus.SUCCESS -> Triple(stringResource(R.string.account_status_synced), Icons.Default.CheckCircle, EmeraldPrimary)
        SyncStatus.SYNCING -> Triple(stringResource(R.string.account_status_syncing), Icons.Default.Sync, SapphireAccent)
        SyncStatus.WAITING_FOR_NETWORK -> Triple(stringResource(R.string.account_status_waiting_network), Icons.Default.CloudOff, AmberWarning)
        SyncStatus.ERROR -> Triple(stringResource(R.string.account_status_error), Icons.Default.ErrorOutline, CrimsonExpense)
        SyncStatus.IDLE -> Triple(stringResource(R.string.account_status_idle), Icons.Default.Cloud, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
