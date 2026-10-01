package md.alexlab.finpulse.presentation.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import md.alexlab.finpulse.FinPulseApplication
import md.alexlab.finpulse.R
import md.alexlab.finpulse.core.designsystem.DarkBackground
import md.alexlab.finpulse.core.designsystem.DarkOutline
import md.alexlab.finpulse.core.designsystem.DarkSurface
import md.alexlab.finpulse.core.designsystem.DarkSurfaceElevated
import md.alexlab.finpulse.core.designsystem.EmeraldPrimary
import md.alexlab.finpulse.core.designsystem.FinPulseTheme
import md.alexlab.finpulse.domain.model.Account

class WidgetConfigurationActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val app = application as FinPulseApplication
        val container = app.container

        val existingConfig = WidgetPreferences.getWidgetConfig(this, appWidgetId)

        setContent {
            FinPulseTheme(darkTheme = true) {
                val accounts by container.accountRepository.getActiveAccountsFlow().collectAsState(initial = emptyList())

                var selectedType by remember { mutableStateOf(existingConfig.displayType) }
                var selectedPrivacy by remember { mutableStateOf(existingConfig.privacySetting) }
                var selectedAccountId by remember { mutableStateOf(existingConfig.accountId) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.widget_config_title),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = DarkBackground
                            )
                        )
                    },
                    containerColor = DarkBackground
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Widget Type Selection
                            item {
                                SectionHeader("Display Mode")
                            }
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    WidgetTypeOption(
                                        title = stringResource(R.string.widget_label_current_balance),
                                        subtitle = "Show net liquid cash across accounts",
                                        isSelected = selectedType == WidgetDisplayType.BALANCE,
                                        onClick = { selectedType = WidgetDisplayType.BALANCE }
                                    )
                                    WidgetTypeOption(
                                        title = stringResource(R.string.widget_label_monthly_spending),
                                        subtitle = "Track this month's expense totals",
                                        isSelected = selectedType == WidgetDisplayType.SPENDING,
                                        onClick = { selectedType = WidgetDisplayType.SPENDING }
                                    )
                                    WidgetTypeOption(
                                        title = stringResource(R.string.widget_label_budget_remaining),
                                        subtitle = "View active budget limits and remaining funds",
                                        isSelected = selectedType == WidgetDisplayType.BUDGET,
                                        onClick = { selectedType = WidgetDisplayType.BUDGET }
                                    )
                                    WidgetTypeOption(
                                        title = stringResource(R.string.widget_label_upcoming_bills),
                                        subtitle = "View obligations due in next 14 days",
                                        isSelected = selectedType == WidgetDisplayType.BILLS,
                                        onClick = { selectedType = WidgetDisplayType.BILLS }
                                    )
                                    WidgetTypeOption(
                                        title = "Dashboard Overview",
                                        subtitle = "Balance + Quick Actions + Key Metrics",
                                        isSelected = selectedType == WidgetDisplayType.DASHBOARD,
                                        onClick = { selectedType = WidgetDisplayType.DASHBOARD }
                                    )
                                }
                            }

                            // 2. Privacy Mode
                            item {
                                SectionHeader(stringResource(R.string.widget_config_privacy))
                            }
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    WidgetTypeOption(
                                        title = stringResource(R.string.widget_config_privacy_system),
                                        subtitle = "Follow in-app privacy and app lock settings",
                                        isSelected = selectedPrivacy == WidgetPrivacySetting.FOLLOW_APP,
                                        onClick = { selectedPrivacy = WidgetPrivacySetting.FOLLOW_APP }
                                    )
                                    WidgetTypeOption(
                                        title = stringResource(R.string.widget_config_privacy_hide),
                                        subtitle = "Always mask amounts with ••••••",
                                        isSelected = selectedPrivacy == WidgetPrivacySetting.ALWAYS_HIDE,
                                        onClick = { selectedPrivacy = WidgetPrivacySetting.ALWAYS_HIDE }
                                    )
                                    WidgetTypeOption(
                                        title = stringResource(R.string.widget_config_privacy_show),
                                        subtitle = "Always show exact financial amounts",
                                        isSelected = selectedPrivacy == WidgetPrivacySetting.ALWAYS_SHOW,
                                        onClick = { selectedPrivacy = WidgetPrivacySetting.ALWAYS_SHOW }
                                    )
                                }
                            }

                            // 3. Account Filter (if applicable)
                            item {
                                SectionHeader(stringResource(R.string.widget_config_account))
                            }
                            item {
                                WidgetTypeOption(
                                    title = stringResource(R.string.widget_config_all_accounts),
                                    subtitle = "Include all active accounts",
                                    isSelected = selectedAccountId == null,
                                    onClick = { selectedAccountId = null }
                                )
                            }
                            items(accounts) { acc ->
                                WidgetTypeOption(
                                    title = acc.name,
                                    subtitle = "${acc.type.name} • ${acc.balance.formatted()}",
                                    isSelected = selectedAccountId == acc.id,
                                    onClick = { selectedAccountId = acc.id }
                                )
                            }
                        }

                        // Save Button
                        Button(
                            onClick = {
                                val newConfig = WidgetConfig(
                                    appWidgetId = appWidgetId,
                                    displayType = selectedType,
                                    privacySetting = selectedPrivacy,
                                    accountId = selectedAccountId
                                )
                                WidgetPreferences.saveWidgetConfig(this@WidgetConfigurationActivity, newConfig)
                                FinPulseWidgetUpdater.updateAllWidgets(this@WidgetConfigurationActivity)

                                val resultValue = Intent().apply {
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                }
                                setResult(Activity.RESULT_OK, resultValue)
                                finish()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.widget_config_save),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF94A3B8),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun WidgetTypeOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) DarkSurfaceElevated else DarkSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) EmeraldPrimary else DarkOutline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(EmeraldPrimary, shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
