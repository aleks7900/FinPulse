package md.alexlab.finpulse.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import md.alexlab.finpulse.R
import md.alexlab.finpulse.domain.model.Category

fun Category.getDisplayName(context: Context): String {
    val resId = getCategoryStringRes(id)
    return if (resId != null) context.getString(resId) else name
}

@Composable
fun Category.getDisplayName(): String {
    val resId = getCategoryStringRes(id)
    return if (resId != null) stringResource(resId) else name
}

@Composable
fun getCategoryDisplayName(categoryId: String?, fallback: String = ""): String {
    val resId = categoryId?.let { getCategoryStringRes(it) }
    return if (resId != null) {
        stringResource(resId)
    } else {
        fallback.ifEmpty { categoryId?.replace("cat_", "")?.replaceFirstChar { it.uppercase() } ?: "" }
    }
}

fun getCategoryDisplayName(categoryId: String?, fallback: String = "", context: Context): String {
    val resId = categoryId?.let { getCategoryStringRes(it) }
    return if (resId != null) {
        context.getString(resId)
    } else {
        fallback.ifEmpty { categoryId?.replace("cat_", "")?.replaceFirstChar { it.uppercase() } ?: "" }
    }
}

fun getCategoryStringRes(categoryId: String): Int? {
    return when (categoryId) {
        "cat_housing" -> R.string.cat_housing
        "cat_home_rent" -> R.string.cat_home_rent
        "cat_home_mortgage" -> R.string.cat_home_mortgage
        "cat_home_general" -> R.string.cat_home_general
        "cat_home_utilities" -> R.string.cat_home_utilities
        "cat_home_electricity" -> R.string.cat_home_electricity
        "cat_home_water" -> R.string.cat_home_water
        "cat_home_gas" -> R.string.cat_home_gas
        "cat_home_heating" -> R.string.cat_home_heating
        "cat_home_internet" -> R.string.cat_home_internet
        "cat_home_phone" -> R.string.cat_home_phone
        "cat_home_tv" -> R.string.cat_home_tv
        "cat_home_maintenance" -> R.string.cat_home_maintenance
        "cat_home_repairs" -> R.string.cat_home_repairs
        "cat_home_furniture" -> R.string.cat_home_furniture
        "cat_home_supplies" -> R.string.cat_home_supplies
        "cat_home_cleaning" -> R.string.cat_home_cleaning
        "cat_home_security" -> R.string.cat_home_security
        "cat_home_property_tax" -> R.string.cat_home_property_tax
        "cat_food" -> R.string.cat_food
        "cat_groceries" -> R.string.cat_groceries
        "cat_food_restaurants" -> R.string.cat_food_restaurants
        "cat_food_cafes" -> R.string.cat_food_cafes
        "cat_food_fast_food" -> R.string.cat_food_fast_food
        "cat_food_delivery" -> R.string.cat_food_delivery
        "cat_food_lunch" -> R.string.cat_food_lunch
        "cat_food_snacks" -> R.string.cat_food_snacks
        "cat_food_drinks" -> R.string.cat_food_drinks
        "cat_food_bakery" -> R.string.cat_food_bakery
        "cat_food_other" -> R.string.cat_food_other
        "cat_transport" -> R.string.cat_transport
        "cat_transport_public" -> R.string.cat_transport_public
        "cat_transport_taxi" -> R.string.cat_transport_taxi
        "cat_fuel" -> R.string.cat_fuel
        "cat_transport_parking" -> R.string.cat_transport_parking
        "cat_transport_maintenance" -> R.string.cat_transport_maintenance
        "cat_transport_repair" -> R.string.cat_transport_repair
        "cat_transport_insurance" -> R.string.cat_transport_insurance
        "cat_transport_wash" -> R.string.cat_transport_wash
        "cat_transport_tolls" -> R.string.cat_transport_tolls
        "cat_transport_rental" -> R.string.cat_transport_rental
        "cat_transport_bicycle" -> R.string.cat_transport_bicycle
        "cat_transport_other" -> R.string.cat_transport_other
        "cat_shopping" -> R.string.cat_shopping
        "cat_shopping_clothing" -> R.string.cat_shopping_clothing
        "cat_shopping_shoes" -> R.string.cat_shopping_shoes
        "cat_shopping_accessories" -> R.string.cat_shopping_accessories
        "cat_shopping_electronics" -> R.string.cat_shopping_electronics
        "cat_shopping_appliances" -> R.string.cat_shopping_appliances
        "cat_shopping_online" -> R.string.cat_shopping_online
        "cat_shopping_aliexpress" -> R.string.cat_shopping_aliexpress
        "cat_shopping_amazon" -> R.string.cat_shopping_amazon
        "cat_shopping_google_play" -> R.string.cat_shopping_google_play
        "cat_shopping_app_store" -> R.string.cat_shopping_app_store
        "cat_shopping_games" -> R.string.cat_shopping_games
        "cat_shopping_gifts" -> R.string.cat_shopping_gifts
        "cat_shopping_other" -> R.string.cat_shopping_other
        "cat_health" -> R.string.cat_health
        "cat_health_pharmacy" -> R.string.cat_health_pharmacy
        "cat_health_medicine" -> R.string.cat_health_medicine
        "cat_health_doctor" -> R.string.cat_health_doctor
        "cat_health_dentist" -> R.string.cat_health_dentist
        "cat_health_hospital" -> R.string.cat_health_hospital
        "cat_health_tests" -> R.string.cat_health_tests
        "cat_health_vitamins" -> R.string.cat_health_vitamins
        "cat_health_insurance" -> R.string.cat_health_insurance
        "cat_fitness" -> R.string.cat_fitness
        "cat_health_gym" -> R.string.cat_health_gym
        "cat_health_other" -> R.string.cat_health_other
        "cat_entertainment" -> R.string.cat_entertainment
        "cat_entertainment_cinema" -> R.string.cat_entertainment_cinema
        "cat_entertainment_music" -> R.string.cat_entertainment_music
        "cat_entertainment_games" -> R.string.cat_entertainment_games
        "cat_entertainment_events" -> R.string.cat_entertainment_events
        "cat_entertainment_concerts" -> R.string.cat_entertainment_concerts
        "cat_entertainment_nightlife" -> R.string.cat_entertainment_nightlife
        "cat_entertainment_hobbies" -> R.string.cat_entertainment_hobbies
        "cat_entertainment_books" -> R.string.cat_entertainment_books
        "cat_entertainment_streaming" -> R.string.cat_entertainment_streaming
        "cat_entertainment_other" -> R.string.cat_entertainment_other
        "cat_subscriptions" -> R.string.cat_subscriptions
        "cat_sub_netflix" -> R.string.cat_sub_netflix
        "cat_sub_spotify" -> R.string.cat_sub_spotify
        "cat_sub_youtube" -> R.string.cat_sub_youtube
        "cat_sub_cloud" -> R.string.cat_sub_cloud
        "cat_sub_software" -> R.string.cat_sub_software
        "cat_sub_apps" -> R.string.cat_sub_apps
        "cat_sub_digital_services" -> R.string.cat_sub_digital_services
        "cat_sub_other" -> R.string.cat_sub_other
        "cat_personal_care" -> R.string.cat_personal_care
        "cat_personal_barber" -> R.string.cat_personal_barber
        "cat_personal_beauty" -> R.string.cat_personal_beauty
        "cat_personal_cosmetics" -> R.string.cat_personal_cosmetics
        "cat_personal_spa" -> R.string.cat_personal_spa
        "cat_personal_massage" -> R.string.cat_personal_massage
        "cat_personal_other" -> R.string.cat_personal_other
        "cat_family" -> R.string.cat_family
        "cat_family_children" -> R.string.cat_family_children
        "cat_family_kindergarten" -> R.string.cat_family_kindergarten
        "cat_family_school" -> R.string.cat_family_school
        "cat_family_education" -> R.string.cat_family_education
        "cat_family_courses" -> R.string.cat_family_courses
        "cat_family_toys" -> R.string.cat_family_toys
        "cat_family_childcare" -> R.string.cat_family_childcare
        "cat_family_pocket_money" -> R.string.cat_family_pocket_money
        "cat_family_support" -> R.string.cat_family_support
        "cat_family_other" -> R.string.cat_family_other
        "cat_pets" -> R.string.cat_pets
        "cat_pets_food" -> R.string.cat_pets_food
        "cat_pets_vet" -> R.string.cat_pets_vet
        "cat_pets_supplies" -> R.string.cat_pets_supplies
        "cat_pets_grooming" -> R.string.cat_pets_grooming
        "cat_pets_other" -> R.string.cat_pets_other
        "cat_travel" -> R.string.cat_travel
        "cat_travel_flights" -> R.string.cat_travel_flights
        "cat_travel_hotels" -> R.string.cat_travel_hotels
        "cat_travel_train" -> R.string.cat_travel_train
        "cat_travel_bus" -> R.string.cat_travel_bus
        "cat_travel_taxi" -> R.string.cat_travel_taxi
        "cat_travel_car_rental" -> R.string.cat_travel_car_rental
        "cat_travel_insurance" -> R.string.cat_travel_insurance
        "cat_travel_vacation" -> R.string.cat_travel_vacation
        "cat_travel_tours" -> R.string.cat_travel_tours
        "cat_travel_other" -> R.string.cat_travel_other
        "cat_education" -> R.string.cat_education
        "cat_edu_school" -> R.string.cat_edu_school
        "cat_edu_university" -> R.string.cat_edu_university
        "cat_edu_courses" -> R.string.cat_edu_courses
        "cat_edu_books" -> R.string.cat_edu_books
        "cat_edu_online" -> R.string.cat_edu_online
        "cat_edu_training" -> R.string.cat_edu_training
        "cat_edu_other" -> R.string.cat_edu_other
        "cat_financial" -> R.string.cat_financial
        "cat_fin_bank_fees" -> R.string.cat_fin_bank_fees
        "cat_fin_interest" -> R.string.cat_fin_interest
        "cat_fin_loan_payment" -> R.string.cat_fin_loan_payment
        "cat_fin_card_payment" -> R.string.cat_fin_card_payment
        "cat_fin_insurance" -> R.string.cat_fin_insurance
        "cat_fin_taxes" -> R.string.cat_fin_taxes
        "cat_fin_fines" -> R.string.cat_fin_fines
        "cat_fin_exchange_fees" -> R.string.cat_fin_exchange_fees
        "cat_fin_services" -> R.string.cat_fin_services
        "cat_fin_other" -> R.string.cat_fin_other
        "cat_debt" -> R.string.cat_debt
        "cat_debt_loan" -> R.string.cat_debt_loan
        "cat_debt_mortgage" -> R.string.cat_debt_mortgage
        "cat_debt_consumer" -> R.string.cat_debt_consumer
        "cat_debt_car" -> R.string.cat_debt_car
        "cat_debt_card" -> R.string.cat_debt_card
        "cat_debt_repayment" -> R.string.cat_debt_repayment
        "cat_debt_other" -> R.string.cat_debt_other
        "cat_gifts" -> R.string.cat_gifts
        "cat_gifts_charity" -> R.string.cat_gifts_charity
        "cat_gifts_donations" -> R.string.cat_gifts_donations
        "cat_gifts_to_others" -> R.string.cat_gifts_to_others
        "cat_gifts_religious" -> R.string.cat_gifts_religious
        "cat_gifts_other" -> R.string.cat_gifts_other
        "cat_other_expenses" -> R.string.cat_other_expenses
        "cat_other_expense_item" -> R.string.cat_other_expense_item
        "cat_uncategorized" -> R.string.cat_uncategorized
        "cat_salary" -> R.string.cat_salary
        "cat_income_salary" -> R.string.cat_income_salary
        "cat_income_bonus" -> R.string.cat_income_bonus
        "cat_income_overtime" -> R.string.cat_income_overtime
        "cat_income_commission" -> R.string.cat_income_commission
        "cat_income_tips" -> R.string.cat_income_tips
        "cat_income_reimbursement" -> R.string.cat_income_reimbursement
        "cat_freelance" -> R.string.cat_freelance
        "cat_income_freelance" -> R.string.cat_income_freelance
        "cat_income_business" -> R.string.cat_income_business
        "cat_income_consulting" -> R.string.cat_income_consulting
        "cat_income_contract" -> R.string.cat_income_contract
        "cat_income_side_job" -> R.string.cat_income_side_job
        "cat_income_business_other" -> R.string.cat_income_business_other
        "cat_invest_return" -> R.string.cat_invest_return
        "cat_income_investment_income" -> R.string.cat_income_investment_income
        "cat_income_dividends" -> R.string.cat_income_dividends
        "cat_income_interest" -> R.string.cat_income_interest
        "cat_income_capital_gains" -> R.string.cat_income_capital_gains
        "cat_income_crypto" -> R.string.cat_income_crypto
        "cat_income_rental" -> R.string.cat_income_rental
        "cat_income_government" -> R.string.cat_income_government
        "cat_income_benefits" -> R.string.cat_income_benefits
        "cat_income_pension" -> R.string.cat_income_pension
        "cat_income_scholarship" -> R.string.cat_income_scholarship
        "cat_income_social_payments" -> R.string.cat_income_social_payments
        "cat_income_tax_refund" -> R.string.cat_income_tax_refund
        "cat_other_income" -> R.string.cat_other_income
        "cat_income_gifts" -> R.string.cat_income_gifts
        "cat_income_cashback" -> R.string.cat_income_cashback
        "cat_income_refund" -> R.string.cat_income_refund
        "cat_income_sale_of_items" -> R.string.cat_income_sale_of_items
        "cat_income_prize" -> R.string.cat_income_prize
        "cat_income_other_item" -> R.string.cat_income_other_item
        "cat_income_uncategorized" -> R.string.cat_income_uncategorized
        "cat_transfer" -> R.string.cat_transfer
        "cat_transfer_withdrawal" -> R.string.cat_transfer_withdrawal
        "cat_transfer_deposit" -> R.string.cat_transfer_deposit
        else -> null
    }
}
