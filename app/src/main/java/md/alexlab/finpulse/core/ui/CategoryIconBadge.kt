package md.alexlab.finpulse.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
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
        "fastfood", "fast_food", "food", "lunch", "snack", "snacks" -> Icons.Default.Fastfood
        "restaurant", "restaurants" -> Icons.Default.Restaurant
        "cafe", "cafes", "coffee" -> Icons.Default.LocalCafe
        "delivery" -> Icons.Default.DeliveryDining
        "drinks", "bar" -> Icons.Default.LocalBar
        "bakery" -> Icons.Default.BakeryDining
        "shoppingcart", "shopping_cart", "cart" -> Icons.Default.ShoppingCart
        "shoppingbag", "shopping", "shop", "store" -> Icons.Default.ShoppingBag
        "checkroom", "clothing", "clothes" -> Icons.Default.Checkroom
        "devices", "electronics", "software", "appliances", "kitchen" -> Icons.Default.Devices
        "home", "housing" -> Icons.Default.Home
        "chair", "furniture" -> Icons.Default.Chair
        "cleaningservices", "cleaning", "supplies" -> Icons.Default.CleaningServices
        "utilities", "electricbolt", "electricity" -> Icons.Default.ElectricBolt
        "waterdrop", "water" -> Icons.Default.WaterDrop
        "gas", "heating" -> Icons.Default.LocalGasStation
        "wifi", "internet" -> Icons.Default.Wifi
        "phone", "mobile" -> Icons.Default.PhoneAndroid
        "tv" -> Icons.Default.Tv
        "handyman", "build", "maintenance" -> Icons.Default.Build
        "construction", "repair", "repairs" -> Icons.Default.Construction
        "security", "insurance" -> Icons.Default.Security
        "receipt", "tax", "taxes", "reimbursement" -> Icons.Default.Receipt
        "transport", "directionscar", "car" -> Icons.Default.DirectionsCar
        "directionsbus", "bus", "public" -> Icons.Default.DirectionsBus
        "taxi" -> Icons.Default.LocalTaxi
        "fuel", "localgasstation" -> Icons.Default.LocalGasStation
        "parking" -> Icons.Default.LocalParking
        "carwash", "wash" -> Icons.Default.LocalCarWash
        "toll", "tolls" -> Icons.Default.Toll
        "carrental", "rental" -> Icons.Default.CarRental
        "directionsbike", "bicycle", "bike" -> Icons.Default.DirectionsBike
        "gamepad", "games" -> Icons.Default.SportsEsports
        "gifts", "redeem" -> Icons.Default.CardGiftcard
        "health", "doctor", "hospital", "localhospital" -> Icons.Default.LocalHospital
        "pharmacy", "medication", "medicine", "vitamins" -> Icons.Default.Medication
        "dentist" -> Icons.Default.MedicalServices
        "science", "tests" -> Icons.Default.Science
        "fitness", "fitnesscenter", "gym" -> Icons.Default.FitnessCenter
        "movie", "cinema" -> Icons.Default.Movie
        "music", "concerts" -> Icons.Default.MusicNote
        "nightlife" -> Icons.Default.Nightlife
        "palette", "hobbies" -> Icons.Default.Palette
        "book", "books" -> Icons.Default.MenuBook
        "subscriptions" -> Icons.Default.Subscriptions
        "cloud" -> Icons.Default.Cloud
        "spa", "beauty", "cosmetics", "massage" -> Icons.Default.Spa
        "barber", "brush", "hairdresser", "grooming" -> Icons.Default.ContentCut
        "childcare", "children" -> Icons.Default.ChildCare
        "school", "education", "university", "courses", "training" -> Icons.Default.School
        "volunteeractivism", "charity", "donations", "support" -> Icons.Default.VolunteerActivism
        "pets" -> Icons.Default.Pets
        "flight", "flights" -> Icons.Default.Flight
        "hotel", "hotels", "vacation" -> Icons.Default.Hotel
        "train" -> Icons.Default.Train
        "accountbalance", "bank", "pension", "benefits" -> Icons.Default.AccountBalance
        "creditcard", "card" -> Icons.Default.CreditCard
        "currencyexchange" -> Icons.Default.CurrencyExchange
        "warning", "fines" -> Icons.Default.Warning
        "salary", "work", "freelance", "business" -> Icons.Default.Work
        "trendingup", "investments", "bonus", "capital", "dividends", "interest" -> Icons.Default.TrendingUp
        "crypto" -> Icons.Default.CurrencyBitcoin
        "trophy", "prize" -> Icons.Default.EmojiEvents
        "swaphoriz", "transfer" -> Icons.Default.SwapHoriz
        "atm", "withdrawal" -> Icons.Default.LocalAtm
        "paid" -> Icons.Default.Paid
        "help", "uncategorized" -> Icons.Default.Help
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
