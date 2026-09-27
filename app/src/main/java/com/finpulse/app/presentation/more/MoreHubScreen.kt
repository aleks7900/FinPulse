package com.finpulse.app.presentation.more

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finpulse.app.R
import com.finpulse.app.core.designsystem.AmberWarning
import com.finpulse.app.core.designsystem.DebtOrange
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.InvestmentPurple
import com.finpulse.app.core.designsystem.PurpleAccent
import com.finpulse.app.core.designsystem.SapphireAccent
import com.finpulse.app.core.designsystem.TealSavings
import com.finpulse.app.core.locale.AppLanguage
import com.finpulse.app.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreHubScreen(
    baseCurrency: String,
    selectedLanguageCode: String,
    onLanguageSelected: (AppLanguage) -> Unit,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentAppLanguage = remember(selectedLanguageCode) {
        AppLanguage.fromCode(selectedLanguageCode)
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentAppLanguage,
            onLanguageSelected = { lang ->
                onLanguageSelected(lang)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.more_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile & Currency Banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = baseCurrency,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.more_currency_title),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = baseCurrency,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Preferences Group (Language, etc.)
            item {
                Text(
                    text = stringResource(R.string.more_section_preferences).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HubItem(
                        title = stringResource(R.string.more_account_sync_title),
                        subtitle = stringResource(R.string.more_account_sync_disconnected),
                        icon = Icons.Default.CloudSync,
                        tint = EmeraldPrimary,
                        onClick = { onNavigate(Screen.Account) }
                    )
                    HubItem(
                        title = stringResource(R.string.more_language_title),
                        subtitle = "${currentAppLanguage.displayName} (${stringResource(currentAppLanguage.localizedNameRes)})",
                        icon = Icons.Default.Language,
                        tint = EmeraldPrimary,
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            // Portfolio & Tools Group
            item {
                Text(
                    text = stringResource(R.string.more_section_financial_tools).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HubItem(
                        title = "Financial Calendar & Cash Flow",
                        subtitle = "Forecast upcoming bills, income, loans & balance timeline",
                        icon = Icons.Default.CalendarMonth,
                        tint = EmeraldPrimary,
                        onClick = { onNavigate(Screen.Calendar) }
                    )
                    HubItem(
                        title = stringResource(R.string.accounts_title),
                        subtitle = stringResource(R.string.account_empty_desc),
                        icon = Icons.Default.AccountBalance,
                        tint = SapphireAccent,
                        onClick = { onNavigate(Screen.Accounts) }
                    )
                    HubItem(
                        title = stringResource(R.string.recurring_title),
                        subtitle = stringResource(R.string.recurring_empty_desc),
                        icon = Icons.Default.Subscriptions,
                        tint = PurpleAccent,
                        onClick = { onNavigate(Screen.Recurring) }
                    )
                    HubItem(
                        title = stringResource(R.string.goals_title),
                        subtitle = stringResource(R.string.goal_empty_desc),
                        icon = Icons.Default.Savings,
                        tint = TealSavings,
                        onClick = { onNavigate(Screen.Goals) }
                    )
                    HubItem(
                        title = stringResource(R.string.investments_title),
                        subtitle = stringResource(R.string.asset_empty_desc),
                        icon = Icons.Default.TrendingUp,
                        tint = InvestmentPurple,
                        onClick = { onNavigate(Screen.Investments) }
                    )
                    HubItem(
                        title = stringResource(R.string.debt_title),
                        subtitle = stringResource(R.string.debt_empty_desc),
                        icon = Icons.Default.CreditCard,
                        tint = DebtOrange,
                        onClick = { onNavigate(Screen.Debt) }
                    )
                }
            }

            // Insights & System Group
            item {
                Text(
                    text = stringResource(R.string.more_section_data_security).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HubItem(
                        title = stringResource(R.string.insights_title),
                        subtitle = stringResource(R.string.more_insights_desc),
                        icon = Icons.Default.Lightbulb,
                        tint = AmberWarning,
                        onClick = { onNavigate(Screen.Insights) }
                    )
                    HubItem(
                        title = "Daily Review Inbox",
                        subtitle = "Clean up uncategorized, duplicates, bills & alerts",
                        icon = Icons.Default.AutoAwesome,
                        tint = AmberWarning,
                        onClick = { onNavigate(Screen.ReviewInbox) }
                    )
                    HubItem(
                        title = stringResource(R.string.categories_title),
                        subtitle = stringResource(R.string.category_empty),
                        icon = Icons.Default.Category,
                        tint = EmeraldPrimary,
                        onClick = { onNavigate(Screen.Categories) }
                    )
                    HubItem(
                        title = stringResource(R.string.more_rules_title),
                        subtitle = stringResource(R.string.more_rules_desc),
                        icon = Icons.Default.Settings,
                        tint = PurpleAccent,
                        onClick = { onNavigate(Screen.CategorizationRules) }
                    )
                    HubItem(
                        title = stringResource(R.string.more_csv_title),
                        subtitle = stringResource(R.string.more_csv_desc),
                        icon = Icons.Default.CloudUpload,
                        tint = EmeraldPrimary,
                        onClick = { onNavigate(Screen.CsvImport) }
                    )
                    HubItem(
                        title = stringResource(R.string.more_export_title),
                        subtitle = stringResource(R.string.export_format_csv),
                        icon = Icons.Default.Download,
                        tint = SapphireAccent,
                        onClick = { onNavigate(Screen.Export) }
                    )
                    HubItem(
                        title = stringResource(R.string.more_security_title),
                        subtitle = stringResource(R.string.more_security_desc),
                        icon = Icons.Default.Lock,
                        tint = EmeraldPrimary,
                        onClick = { onNavigate(Screen.Security) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.select_language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppLanguage.entries.forEach { lang ->
                    val isSelected = lang == currentLanguage
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) EmeraldPrimary.copy(alpha = 0.12f) else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.5.dp, EmeraldPrimary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(lang) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lang.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(lang.localizedNameRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_done), color = EmeraldPrimary)
            }
        }
    )
}

@Composable
fun HubItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = tint.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
