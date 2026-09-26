package com.finpulse.app.presentation.more

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finpulse.app.core.designsystem.AmberWarning
import com.finpulse.app.core.designsystem.DebtOrange
import com.finpulse.app.core.designsystem.EmeraldPrimary
import com.finpulse.app.core.designsystem.InvestmentPurple
import com.finpulse.app.core.designsystem.PurpleAccent
import com.finpulse.app.core.designsystem.SapphireAccent
import com.finpulse.app.core.designsystem.TealSavings
import com.finpulse.app.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreHubScreen(
    baseCurrency: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Financial Hub",
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
                                text = "Base Portfolio Currency",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$baseCurrency - Primary Denomination",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Portfolio & Tools Group
            item {
                Text(
                    text = "PORTFOLIO & TRACKING",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HubItem(
                        title = "Accounts & Wallets",
                        subtitle = "Banks, cash, cards, institutions",
                        icon = Icons.Default.AccountBalance,
                        tint = SapphireAccent,
                        onClick = { onNavigate(Screen.Accounts) }
                    )
                    HubItem(
                        title = "Subscriptions & Recurring Bills",
                        subtitle = "Netflix, Spotify, rent, annualized cost",
                        icon = Icons.Default.Subscriptions,
                        tint = PurpleAccent,
                        onClick = { onNavigate(Screen.Recurring) }
                    )
                    HubItem(
                        title = "Financial Goals",
                        subtitle = "Milestones & monthly savings plan",
                        icon = Icons.Default.Savings,
                        tint = TealSavings,
                        onClick = { onNavigate(Screen.Goals) }
                    )
                    HubItem(
                        title = "Investments & Assets",
                        subtitle = "Stocks, ETFs, Crypto, Real Estate, P/L",
                        icon = Icons.Default.TrendingUp,
                        tint = InvestmentPurple,
                        onClick = { onNavigate(Screen.Investments) }
                    )
                    HubItem(
                        title = "Debt Payoff Planner",
                        subtitle = "Snowball & Avalanche amortization",
                        icon = Icons.Default.CreditCard,
                        tint = DebtOrange,
                        onClick = { onNavigate(Screen.Debt) }
                    )
                }
            }

            // Insights & System Group
            item {
                Text(
                    text = "INSIGHTS & DATA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HubItem(
                        title = "Smart Financial Insights",
                        subtitle = "Burn rate, category inflation, budget alerts",
                        icon = Icons.Default.Lightbulb,
                        tint = AmberWarning,
                        onClick = { onNavigate(Screen.Insights) }
                    )
                    HubItem(
                        title = "Categories",
                        subtitle = "Manage expense and income categories",
                        icon = Icons.Default.Category,
                        tint = EmeraldPrimary,
                        onClick = { onNavigate(Screen.Categories) }
                    )
                    HubItem(
                        title = "Backup & Data Export",
                        subtitle = "CSV ledger export, JSON backup & restore",
                        icon = Icons.Default.Download,
                        tint = SapphireAccent,
                        onClick = { onNavigate(Screen.Export) }
                    )
                    HubItem(
                        title = "Security & Privacy",
                        subtitle = "Biometrics, PIN, screenshot protection",
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
