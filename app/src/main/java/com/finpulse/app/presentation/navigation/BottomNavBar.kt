package com.finpulse.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finpulse.app.R
import com.finpulse.app.core.designsystem.EmeraldPrimary

sealed class BottomNavItem(
    val screen: Screen,
    val titleRes: Int,
    val icon: ImageVector
) {
    data object Dashboard : BottomNavItem(Screen.Dashboard, R.string.nav_dashboard, Icons.Default.Dashboard)
    data object Transactions : BottomNavItem(Screen.Transactions, R.string.nav_transactions, Icons.AutoMirrored.Filled.ReceiptLong)
    data object Budgets : BottomNavItem(Screen.Budgets, R.string.nav_budgets, Icons.Default.PieChart)
    data object Analytics : BottomNavItem(Screen.Analytics, R.string.nav_analytics, Icons.Default.BarChart)
    data object More : BottomNavItem(Screen.More, R.string.nav_more, Icons.Default.Widgets)

    companion object {
        val items = listOf(Dashboard, Transactions, Budgets, Analytics, More)
    }
}

@Composable
fun FinPulseBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        BottomNavItem.items.forEach { item ->
            val selected = currentRoute == item.screen.route
            val itemTitle = stringResource(item.titleRes)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.screen) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = itemTitle
                    )
                },
                label = {
                    Text(
                        text = itemTitle,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = EmeraldPrimary,
                    indicatorColor = EmeraldPrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
