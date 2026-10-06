package md.alexlab.finpulse.presentation.sync

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.AmberWarning
import md.alexlab.finpulse.core.designsystem.CrimsonExpense
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.data.sync.CloudSyncCoordinator
import md.alexlab.finpulse.data.sync.GlobalSyncState
import md.alexlab.finpulse.data.sync.SyncTrigger
import kotlinx.coroutines.launch

/**
 * Universal Cloud Sync button placed in the top-right corner of main screens.
 * Visually displays real-time sync state (Idle, Syncing, Success, Error, Offline, Auth Required)
 * and triggers coordinated bidirectional synchronization with user feedback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalCloudSyncButton(
    coordinator: CloudSyncCoordinator,
    onNavigateToAccount: () -> Unit,
    onShowFeedback: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val syncState by coordinator.globalSyncState.collectAsState()
    val isSyncing by coordinator.isSyncing.collectAsState()
    val scope = rememberCoroutineScope()

    val tooltipState = rememberTooltipState()

    val infiniteTransition = rememberInfiniteTransition(label = "global_sync_rotate")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val (icon: ImageVector, tintColor: Color, description: String) = when (syncState) {
        GlobalSyncState.SYNCING -> Triple(
            Icons.Default.Sync,
            EmeraldPrimary,
            stringResource(R.string.account_status_syncing)
        )
        GlobalSyncState.SUCCESS -> Triple(
            Icons.Default.CloudDone,
            EmeraldPrimary,
            stringResource(R.string.sync_feedback_success)
        )
        GlobalSyncState.ERROR -> Triple(
            Icons.Default.ErrorOutline,
            CrimsonExpense,
            stringResource(R.string.sync_feedback_failed)
        )
        GlobalSyncState.OFFLINE -> Triple(
            Icons.Default.CloudOff,
            AmberWarning,
            stringResource(R.string.sync_feedback_offline)
        )
        GlobalSyncState.AUTHENTICATION_REQUIRED -> Triple(
            Icons.Default.CloudQueue,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            stringResource(R.string.sync_feedback_auth_required)
        )
        GlobalSyncState.IDLE -> Triple(
            Icons.Default.Cloud,
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(R.string.action_cloud_sync)
        )
    }

    val feedbackSuccessMsg = stringResource(R.string.sync_feedback_success)
    val feedbackFailedMsg = stringResource(R.string.sync_feedback_failed)
    val feedbackAuthMsg = stringResource(R.string.sync_feedback_auth_required)
    val feedbackOfflineMsg = stringResource(R.string.sync_feedback_offline)

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(text = description)
            }
        },
        state = tooltipState
    ) {
        IconButton(
            onClick = {
                when (syncState) {
                    GlobalSyncState.AUTHENTICATION_REQUIRED -> {
                        onShowFeedback(feedbackAuthMsg)
                        onNavigateToAccount()
                    }
                    GlobalSyncState.OFFLINE -> {
                        onShowFeedback(feedbackOfflineMsg)
                    }
                    GlobalSyncState.SYNCING -> {
                        // Already syncing - ignore concurrent button presses
                    }
                    else -> {
                        if (!isSyncing) {
                            scope.launch {
                                val result = coordinator.sync(SyncTrigger.MANUAL)
                                if (result.isSuccess) {
                                    onShowFeedback(feedbackSuccessMsg)
                                } else {
                                    val err = result.exceptionOrNull()?.localizedMessage
                                    onShowFeedback(err ?: feedbackFailedMsg)
                                }
                            }
                        }
                    }
                }
            },
            modifier = modifier
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = tintColor,
                modifier = Modifier
                    .size(24.dp)
                    .then(
                        if (syncState == GlobalSyncState.SYNCING || isSyncing) {
                            Modifier.rotate(rotationAngle)
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}
