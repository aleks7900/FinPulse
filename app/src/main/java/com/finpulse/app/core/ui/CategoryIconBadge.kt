package com.finpulse.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun getCategoryIconVector(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "fastfood", "food", "restaurants" -> Icons.Default.Fastfood
        "shoppingbag", "shopping" -> Icons.Default.ShoppingBag
        "home", "housing" -> Icons.Default.Home
        "directionscar", "transport" -> Icons.Default.DirectionsCar
        "localgasstation", "fuel" -> Icons.Default.LocalGasStation
        "movie", "entertainment" -> Icons.Default.Movie
        "localhospital", "health" -> Icons.Default.LocalHospital
        "school", "education" -> Icons.Default.School
        "fitnesscenter", "fitness" -> Icons.Default.FitnessCenter
        "work", "salary", "freelance" -> Icons.Default.Work
        "subscriptions" -> Icons.Default.Subscriptions
        "trendingup", "investments" -> Icons.Default.TrendingUp
        "redeem", "gifts" -> Icons.Default.Redeem
        "accountbalance", "bank" -> Icons.Default.AccountBalance
        else -> Icons.Default.Paid
    }
}

@Composable
fun CategoryIconBadge(
    iconName: String,
    colorHex: Long,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    val baseColor = Color(colorHex)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(baseColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getCategoryIconVector(iconName),
            contentDescription = null,
            tint = baseColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
