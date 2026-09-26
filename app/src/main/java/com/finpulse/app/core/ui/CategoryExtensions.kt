package com.finpulse.app.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.finpulse.app.R
import com.finpulse.app.domain.model.Category

fun Category.getDisplayName(context: Context): String {
    val resId = getCategoryStringRes(id)
    return if (resId != null) context.getString(resId) else name
}

@Composable
fun Category.getDisplayName(): String {
    val resId = getCategoryStringRes(id)
    return if (resId != null) stringResource(resId) else name
}

fun getCategoryStringRes(categoryId: String): Int? {
    return when (categoryId) {
        "cat_food" -> R.string.cat_food
        "cat_groceries" -> R.string.cat_groceries
        "cat_housing" -> R.string.cat_housing
        "cat_transport" -> R.string.cat_transport
        "cat_fuel" -> R.string.cat_fuel
        "cat_shopping" -> R.string.cat_shopping
        "cat_entertainment" -> R.string.cat_entertainment
        "cat_health" -> R.string.cat_health
        "cat_education" -> R.string.cat_education
        "cat_fitness" -> R.string.cat_fitness
        "cat_subscriptions" -> R.string.cat_subscriptions
        "cat_gifts" -> R.string.cat_gifts
        "cat_salary" -> R.string.cat_salary
        "cat_freelance" -> R.string.cat_freelance
        "cat_invest_return" -> R.string.cat_invest_return
        "cat_other_income" -> R.string.cat_other_income
        "cat_uncategorized" -> R.string.cat_uncategorized
        else -> null
    }
}
