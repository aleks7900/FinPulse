package md.alexlab.finpulse.presentation.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.isDynamicColorSupported

/** Theme mode as stored in DataStore ("SYSTEM", "LIGHT", "DARK"). */
enum class ThemeMode(val key: String) {
    SYSTEM("SYSTEM"), LIGHT("LIGHT"), DARK("DARK");

    companion object {
        fun fromDarkFlag(isDarkMode: Boolean?): ThemeMode = when (isDarkMode) {
            null -> SYSTEM
            true -> DARK
            false -> LIGHT
        }
    }
}

@Composable
fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.appearance_theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.appearance_theme_light)
    ThemeMode.DARK -> stringResource(R.string.appearance_theme_dark)
}

@Composable
fun AppearanceDialog(
    themeMode: ThemeMode,
    useDynamicColor: Boolean,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.appearance_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.appearance_theme_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val modes = ThemeMode.entries
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        modes.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = mode == themeMode,
                                onClick = { onThemeModeSelected(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = mode == themeMode) {
                                        Icon(
                                            imageVector = when (mode) {
                                                ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                                                ThemeMode.LIGHT -> Icons.Outlined.LightMode
                                                ThemeMode.DARK -> Icons.Outlined.DarkMode
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.padding(0.dp)
                                        )
                                    }
                                },
                                label = { Text(themeModeLabel(mode), maxLines = 1) }
                            )
                        }
                    }
                }

                val supported = isDynamicColorSupported
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = useDynamicColor && supported,
                            enabled = supported,
                            role = Role.Switch,
                            onValueChange = onDynamicColorChanged
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.appearance_dynamic_color_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource(
                                    if (supported) R.string.appearance_dynamic_color_desc
                                    else R.string.appearance_dynamic_color_unsupported
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = useDynamicColor && supported,
                            onCheckedChange = null,
                            enabled = supported
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        }
    )
}
