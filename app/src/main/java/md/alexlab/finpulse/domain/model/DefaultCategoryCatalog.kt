package md.alexlab.finpulse.domain.model

import md.alexlab.finpulse.core.database.entity.CategoryEntity

/**
 * Single source of truth for built-in financial categories catalog.
 * Every built-in category has a stable internal ID independent of localized display strings.
 */
object DefaultCategoryCatalog {

    val ALL: List<CategoryEntity> = buildList {
        var order = 1

        // ==========================================
        // 1. HOME & UTILITIES (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_housing", "Home & Utilities", "EXPENSE", null, "home", 0xFF3F51B5, true, order++))
        add(CategoryEntity("cat_home_rent", "Rent", "EXPENSE", "cat_housing", "home", 0xFF3F51B5, true, order++))
        add(CategoryEntity("cat_home_mortgage", "Mortgage", "EXPENSE", "cat_housing", "accountbalance", 0xFF303F9F, true, order++))
        add(CategoryEntity("cat_home_general", "Home", "EXPENSE", "cat_housing", "home", 0xFF5C6BC0, true, order++))
        add(CategoryEntity("cat_home_utilities", "Utilities", "EXPENSE", "cat_housing", "utilities", 0xFF00ACC1, true, order++))
        add(CategoryEntity("cat_home_electricity", "Electricity", "EXPENSE", "cat_housing", "electricbolt", 0xFFFFB300, true, order++))
        add(CategoryEntity("cat_home_water", "Water", "EXPENSE", "cat_housing", "waterdrop", 0xFF039BE5, true, order++))
        add(CategoryEntity("cat_home_gas", "Gas", "EXPENSE", "cat_housing", "gas", 0xFFFF7043, true, order++))
        add(CategoryEntity("cat_home_heating", "Heating", "EXPENSE", "cat_housing", "heating", 0xFFFF5722, true, order++))
        add(CategoryEntity("cat_home_internet", "Internet", "EXPENSE", "cat_housing", "wifi", 0xFF00897B, true, order++))
        add(CategoryEntity("cat_home_phone", "Mobile Phone", "EXPENSE", "cat_housing", "phone", 0xFF43A047, true, order++))
        add(CategoryEntity("cat_home_tv", "TV", "EXPENSE", "cat_housing", "tv", 0xFF7E57C2, true, order++))
        add(CategoryEntity("cat_home_maintenance", "Home Maintenance", "EXPENSE", "cat_housing", "handyman", 0xFF8D6E63, true, order++))
        add(CategoryEntity("cat_home_repairs", "Home Repairs", "EXPENSE", "cat_housing", "construction", 0xFF6D4C41, true, order++))
        add(CategoryEntity("cat_home_furniture", "Furniture", "EXPENSE", "cat_housing", "chair", 0xFF795548, true, order++))
        add(CategoryEntity("cat_home_supplies", "Household Supplies", "EXPENSE", "cat_housing", "cleaningservices", 0xFF26A69A, true, order++))
        add(CategoryEntity("cat_home_cleaning", "Cleaning", "EXPENSE", "cat_housing", "cleaningservices", 0xFF00BFA5, true, order++))
        add(CategoryEntity("cat_home_security", "Security", "EXPENSE", "cat_housing", "security", 0xFF455A64, true, order++))
        add(CategoryEntity("cat_home_property_tax", "Property Tax", "EXPENSE", "cat_housing", "receipt", 0xFF546E7A, true, order++))

        // ==========================================
        // 2. FOOD & DINING (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_food", "Food & Dining", "EXPENSE", null, "fastfood", 0xFFFF9800, true, order++))
        add(CategoryEntity("cat_groceries", "Groceries", "EXPENSE", "cat_food", "shoppingcart", 0xFF4CAF50, true, order++))
        add(CategoryEntity("cat_food_restaurants", "Restaurants", "EXPENSE", "cat_food", "restaurant", 0xFFFF9800, true, order++))
        add(CategoryEntity("cat_food_cafes", "Cafes", "EXPENSE", "cat_food", "cafe", 0xFF795548, true, order++))
        add(CategoryEntity("cat_food_fast_food", "Fast Food", "EXPENSE", "cat_food", "fastfood", 0xFFFF7043, true, order++))
        add(CategoryEntity("cat_food_delivery", "Food Delivery", "EXPENSE", "cat_food", "delivery", 0xFFFF5722, true, order++))
        add(CategoryEntity("cat_food_lunch", "Lunch", "EXPENSE", "cat_food", "lunch", 0xFFFFA726, true, order++))
        add(CategoryEntity("cat_food_snacks", "Snacks", "EXPENSE", "cat_food", "snack", 0xFFFFB74D, true, order++))
        add(CategoryEntity("cat_food_drinks", "Drinks", "EXPENSE", "cat_food", "drinks", 0xFF26C6DA, true, order++))
        add(CategoryEntity("cat_food_bakery", "Bakery", "EXPENSE", "cat_food", "bakery", 0xFFD4E157, true, order++))
        add(CategoryEntity("cat_food_other", "Other Food", "EXPENSE", "cat_food", "fastfood", 0xFFFFA000, true, order++))

        // ==========================================
        // 3. TRANSPORTATION (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_transport", "Transportation", "EXPENSE", null, "transport", 0xFF2196F3, true, order++))
        add(CategoryEntity("cat_transport_public", "Public Transport", "EXPENSE", "cat_transport", "directionsbus", 0xFF1E88E5, true, order++))
        add(CategoryEntity("cat_transport_taxi", "Taxi", "EXPENSE", "cat_transport", "taxi", 0xFFFDD835, true, order++))
        add(CategoryEntity("cat_fuel", "Fuel & Gas", "EXPENSE", "cat_transport", "fuel", 0xFF00BCD4, true, order++))
        add(CategoryEntity("cat_transport_parking", "Parking", "EXPENSE", "cat_transport", "parking", 0xFF5C6BC0, true, order++))
        add(CategoryEntity("cat_transport_maintenance", "Car Maintenance", "EXPENSE", "cat_transport", "build", 0xFF78909C, true, order++))
        add(CategoryEntity("cat_transport_repair", "Car Repair", "EXPENSE", "cat_transport", "construction", 0xFF607D8B, true, order++))
        add(CategoryEntity("cat_transport_insurance", "Car Insurance", "EXPENSE", "cat_transport", "security", 0xFF3949AB, true, order++))
        add(CategoryEntity("cat_transport_wash", "Car Wash", "EXPENSE", "cat_transport", "carwash", 0xFF29B6F6, true, order++))
        add(CategoryEntity("cat_transport_tolls", "Tolls", "EXPENSE", "cat_transport", "toll", 0xFF26A69A, true, order++))
        add(CategoryEntity("cat_transport_rental", "Car Rental", "EXPENSE", "cat_transport", "carrental", 0xFF00ACC1, true, order++))
        add(CategoryEntity("cat_transport_bicycle", "Bicycle", "EXPENSE", "cat_transport", "directionsbike", 0xFF66BB6A, true, order++))
        add(CategoryEntity("cat_transport_other", "Other Transportation", "EXPENSE", "cat_transport", "transport", 0xFF42A5F5, true, order++))

        // ==========================================
        // 4. SHOPPING (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_shopping", "Shopping", "EXPENSE", null, "shopping", 0xFFE91E63, true, order++))
        add(CategoryEntity("cat_shopping_clothing", "Clothing", "EXPENSE", "cat_shopping", "checkroom", 0xFFEC407A, true, order++))
        add(CategoryEntity("cat_shopping_shoes", "Shoes", "EXPENSE", "cat_shopping", "shoes", 0xFFAB47BC, true, order++))
        add(CategoryEntity("cat_shopping_accessories", "Accessories", "EXPENSE", "cat_shopping", "accessories", 0xFFBA68C8, true, order++))
        add(CategoryEntity("cat_shopping_electronics", "Electronics", "EXPENSE", "cat_shopping", "devices", 0xFF5C6BC0, true, order++))
        add(CategoryEntity("cat_shopping_appliances", "Appliances", "EXPENSE", "cat_shopping", "kitchen", 0xFF7E57C2, true, order++))
        add(CategoryEntity("cat_shopping_online", "Online Shopping", "EXPENSE", "cat_shopping", "shoppingcart", 0xFFE91E63, true, order++))
        add(CategoryEntity("cat_shopping_aliexpress", "AliExpress", "EXPENSE", "cat_shopping", "store", 0xFFFF5722, true, order++))
        add(CategoryEntity("cat_shopping_amazon", "Amazon", "EXPENSE", "cat_shopping", "store", 0xFFFF9800, true, order++))
        add(CategoryEntity("cat_shopping_google_play", "Google Play", "EXPENSE", "cat_shopping", "shop", 0xFF4CAF50, true, order++))
        add(CategoryEntity("cat_shopping_app_store", "App Store", "EXPENSE", "cat_shopping", "shop", 0xFF2196F3, true, order++))
        add(CategoryEntity("cat_shopping_games", "Games", "EXPENSE", "cat_shopping", "gamepad", 0xFF9C27B0, true, order++))
        add(CategoryEntity("cat_shopping_gifts", "Gifts", "EXPENSE", "cat_shopping", "gifts", 0xFFFF4081, true, order++))
        add(CategoryEntity("cat_shopping_other", "Other Shopping", "EXPENSE", "cat_shopping", "shopping", 0xFFF06292, true, order++))

        // ==========================================
        // 5. HEALTH (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_health", "Health & Medical", "EXPENSE", null, "health", 0xFFF44336, true, order++))
        add(CategoryEntity("cat_health_pharmacy", "Pharmacy", "EXPENSE", "cat_health", "pharmacy", 0xFFE53935, true, order++))
        add(CategoryEntity("cat_health_medicine", "Medicine", "EXPENSE", "cat_health", "medication", 0xFFEF5350, true, order++))
        add(CategoryEntity("cat_health_doctor", "Doctor", "EXPENSE", "cat_health", "health", 0xFFE57373, true, order++))
        add(CategoryEntity("cat_health_dentist", "Dentist", "EXPENSE", "cat_health", "dentist", 0xFF00ACC1, true, order++))
        add(CategoryEntity("cat_health_hospital", "Hospital", "EXPENSE", "cat_health", "hospital", 0xFFD32F2F, true, order++))
        add(CategoryEntity("cat_health_tests", "Medical Tests", "EXPENSE", "cat_health", "science", 0xFF8E24AA, true, order++))
        add(CategoryEntity("cat_health_vitamins", "Vitamins", "EXPENSE", "cat_health", "medication", 0xFF43A047, true, order++))
        add(CategoryEntity("cat_health_insurance", "Health Insurance", "EXPENSE", "cat_health", "security", 0xFF1E88E5, true, order++))
        add(CategoryEntity("cat_fitness", "Fitness & Sports", "EXPENSE", "cat_health", "fitness", 0xFF009688, true, order++))
        add(CategoryEntity("cat_health_gym", "Gym", "EXPENSE", "cat_health", "fitness", 0xFF00897B, true, order++))
        add(CategoryEntity("cat_health_other", "Other Health", "EXPENSE", "cat_health", "health", 0xFFEF5350, true, order++))

        // ==========================================
        // 6. ENTERTAINMENT (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_entertainment", "Entertainment", "EXPENSE", null, "entertainment", 0xFF9C27B0, true, order++))
        add(CategoryEntity("cat_entertainment_cinema", "Cinema", "EXPENSE", "cat_entertainment", "movie", 0xFFAB47BC, true, order++))
        add(CategoryEntity("cat_entertainment_music", "Music", "EXPENSE", "cat_entertainment", "music", 0xFF8E24AA, true, order++))
        add(CategoryEntity("cat_entertainment_games", "Games", "EXPENSE", "cat_entertainment", "gamepad", 0xFF7B1FA2, true, order++))
        add(CategoryEntity("cat_entertainment_events", "Events", "EXPENSE", "cat_entertainment", "event", 0xFFE91E63, true, order++))
        add(CategoryEntity("cat_entertainment_concerts", "Concerts", "EXPENSE", "cat_entertainment", "music", 0xFFD81B60, true, order++))
        add(CategoryEntity("cat_entertainment_nightlife", "Nightlife", "EXPENSE", "cat_entertainment", "nightlife", 0xFFC2185B, true, order++))
        add(CategoryEntity("cat_entertainment_hobbies", "Hobbies", "EXPENSE", "cat_entertainment", "palette", 0xFF673AB7, true, order++))
        add(CategoryEntity("cat_entertainment_books", "Books", "EXPENSE", "cat_entertainment", "book", 0xFF5E35B1, true, order++))
        add(CategoryEntity("cat_entertainment_streaming", "Streaming Services", "EXPENSE", "cat_entertainment", "subscriptions", 0xFF3949AB, true, order++))
        add(CategoryEntity("cat_entertainment_other", "Other Entertainment", "EXPENSE", "cat_entertainment", "entertainment", 0xFFBA68C8, true, order++))

        // ==========================================
        // 7. SUBSCRIPTIONS & DIGITAL SERVICES (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_subscriptions", "Subscriptions", "EXPENSE", null, "subscriptions", 0xFF673AB7, true, order++))
        add(CategoryEntity("cat_sub_netflix", "Netflix", "EXPENSE", "cat_subscriptions", "movie", 0xFFE50914, true, order++))
        add(CategoryEntity("cat_sub_spotify", "Spotify", "EXPENSE", "cat_subscriptions", "music", 0xFF1DB954, true, order++))
        add(CategoryEntity("cat_sub_youtube", "YouTube", "EXPENSE", "cat_subscriptions", "movie", 0xFFFF0000, true, order++))
        add(CategoryEntity("cat_sub_cloud", "Cloud Storage", "EXPENSE", "cat_subscriptions", "cloud", 0xFF4285F4, true, order++))
        add(CategoryEntity("cat_sub_software", "Software", "EXPENSE", "cat_subscriptions", "devices", 0xFF5C6BC0, true, order++))
        add(CategoryEntity("cat_sub_apps", "Mobile Apps", "EXPENSE", "cat_subscriptions", "phone", 0xFF26A69A, true, order++))
        add(CategoryEntity("cat_sub_digital_services", "Digital Services", "EXPENSE", "cat_subscriptions", "wifi", 0xFF7E57C2, true, order++))
        add(CategoryEntity("cat_sub_other", "Other Subscriptions", "EXPENSE", "cat_subscriptions", "subscriptions", 0xFF9575CD, true, order++))

        // ==========================================
        // 8. PERSONAL CARE (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_personal_care", "Personal Care", "EXPENSE", null, "spa", 0xFF00ACC1, true, order++))
        add(CategoryEntity("cat_personal_barber", "Hairdresser / Barber", "EXPENSE", "cat_personal_care", "barber", 0xFF0097A7, true, order++))
        add(CategoryEntity("cat_personal_beauty", "Beauty", "EXPENSE", "cat_personal_care", "spa", 0xFF26C6DA, true, order++))
        add(CategoryEntity("cat_personal_cosmetics", "Cosmetics", "EXPENSE", "cat_personal_care", "brush", 0xFFEC407A, true, order++))
        add(CategoryEntity("cat_personal_spa", "Spa", "EXPENSE", "cat_personal_care", "spa", 0xFF00BCD4, true, order++))
        add(CategoryEntity("cat_personal_massage", "Massage", "EXPENSE", "cat_personal_care", "spa", 0xFF4DD0E1, true, order++))
        add(CategoryEntity("cat_personal_other", "Other Personal Care", "EXPENSE", "cat_personal_care", "spa", 0xFF80DEEA, true, order++))

        // ==========================================
        // 9. FAMILY & CHILDREN (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_family", "Family & Children", "EXPENSE", null, "childcare", 0xFFFFB300, true, order++))
        add(CategoryEntity("cat_family_children", "Children", "EXPENSE", "cat_family", "childcare", 0xFFFFA000, true, order++))
        add(CategoryEntity("cat_family_kindergarten", "Kindergarten", "EXPENSE", "cat_family", "school", 0xFFFF8F00, true, order++))
        add(CategoryEntity("cat_family_school", "School", "EXPENSE", "cat_family", "school", 0xFFFFB300, true, order++))
        add(CategoryEntity("cat_family_education", "Education", "EXPENSE", "cat_family", "school", 0xFFFFC107, true, order++))
        add(CategoryEntity("cat_family_courses", "Courses", "EXPENSE", "cat_family", "school", 0xFFFFD54F, true, order++))
        add(CategoryEntity("cat_family_toys", "Toys", "EXPENSE", "cat_family", "gamepad", 0xFFFF7043, true, order++))
        add(CategoryEntity("cat_family_childcare", "Childcare", "EXPENSE", "cat_family", "childcare", 0xFFFF8A65, true, order++))
        add(CategoryEntity("cat_family_pocket_money", "Pocket Money", "EXPENSE", "cat_family", "paid", 0xFF4CAF50, true, order++))
        add(CategoryEntity("cat_family_support", "Family Support", "EXPENSE", "cat_family", "volunteeractivism", 0xFF8D6E63, true, order++))
        add(CategoryEntity("cat_family_other", "Other Family Expenses", "EXPENSE", "cat_family", "childcare", 0xFFFFB74D, true, order++))

        // ==========================================
        // 10. PETS (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_pets", "Pets", "EXPENSE", null, "pets", 0xFF8D6E63, true, order++))
        add(CategoryEntity("cat_pets_food", "Pet Food", "EXPENSE", "cat_pets", "pets", 0xFF6D4C41, true, order++))
        add(CategoryEntity("cat_pets_vet", "Veterinary", "EXPENSE", "cat_pets", "health", 0xFF795548, true, order++))
        add(CategoryEntity("cat_pets_supplies", "Pet Supplies", "EXPENSE", "cat_pets", "shoppingcart", 0xFFA1887F, true, order++))
        add(CategoryEntity("cat_pets_grooming", "Grooming", "EXPENSE", "cat_pets", "barber", 0xFFBCAAA4, true, order++))
        add(CategoryEntity("cat_pets_other", "Other Pet Expenses", "EXPENSE", "cat_pets", "pets", 0xFFD7CCC8, true, order++))

        // ==========================================
        // 11. TRAVEL (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_travel", "Travel", "EXPENSE", null, "flight", 0xFF009688, true, order++))
        add(CategoryEntity("cat_travel_flights", "Flights", "EXPENSE", "cat_travel", "flight", 0xFF00796B, true, order++))
        add(CategoryEntity("cat_travel_hotels", "Hotels", "EXPENSE", "cat_travel", "hotel", 0xFF00897B, true, order++))
        add(CategoryEntity("cat_travel_train", "Train", "EXPENSE", "cat_travel", "train", 0xFF00ACC1, true, order++))
        add(CategoryEntity("cat_travel_bus", "Bus", "EXPENSE", "cat_travel", "directionsbus", 0xFF0097A7, true, order++))
        add(CategoryEntity("cat_travel_taxi", "Taxi / Transfers", "EXPENSE", "cat_travel", "taxi", 0xFFFBC02D, true, order++))
        add(CategoryEntity("cat_travel_car_rental", "Car Rental", "EXPENSE", "cat_travel", "carrental", 0xFF26A69A, true, order++))
        add(CategoryEntity("cat_travel_insurance", "Travel Insurance", "EXPENSE", "cat_travel", "security", 0xFF3949AB, true, order++))
        add(CategoryEntity("cat_travel_vacation", "Vacation", "EXPENSE", "cat_travel", "hotel", 0xFF4DB6AC, true, order++))
        add(CategoryEntity("cat_travel_tours", "Tours & Activities", "EXPENSE", "cat_travel", "event", 0xFF80CBC4, true, order++))
        add(CategoryEntity("cat_travel_other", "Other Travel", "EXPENSE", "cat_travel", "flight", 0xFFB2DFDB, true, order++))

        // ==========================================
        // 12. EDUCATION (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_education", "Education", "EXPENSE", null, "education", 0xFF795548, true, order++))
        add(CategoryEntity("cat_edu_school", "School", "EXPENSE", "cat_education", "school", 0xFF5D4037, true, order++))
        add(CategoryEntity("cat_edu_university", "University", "EXPENSE", "cat_education", "school", 0xFF4E342E, true, order++))
        add(CategoryEntity("cat_edu_courses", "Courses", "EXPENSE", "cat_education", "school", 0xFF6D4C41, true, order++))
        add(CategoryEntity("cat_edu_books", "Books", "EXPENSE", "cat_education", "book", 0xFF8D6E63, true, order++))
        add(CategoryEntity("cat_edu_online", "Online Courses", "EXPENSE", "cat_education", "devices", 0xFFA1887F, true, order++))
        add(CategoryEntity("cat_edu_training", "Training", "EXPENSE", "cat_education", "school", 0xFFBCAAA4, true, order++))
        add(CategoryEntity("cat_edu_other", "Other Education", "EXPENSE", "cat_education", "education", 0xFFD7CCC8, true, order++))

        // ==========================================
        // 13. FINANCIAL EXPENSES (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_financial", "Financial Expenses", "EXPENSE", null, "accountbalance", 0xFF607D8B, true, order++))
        add(CategoryEntity("cat_fin_bank_fees", "Bank Fees", "EXPENSE", "cat_financial", "accountbalance", 0xFF546E7A, true, order++))
        add(CategoryEntity("cat_fin_interest", "Interest", "EXPENSE", "cat_financial", "trendingup", 0xFF455A64, true, order++))
        add(CategoryEntity("cat_fin_loan_payment", "Loan Payment", "EXPENSE", "cat_financial", "receipt", 0xFF37474F, true, order++))
        add(CategoryEntity("cat_fin_card_payment", "Credit Card Payment", "EXPENSE", "cat_financial", "creditcard", 0xFF263238, true, order++))
        add(CategoryEntity("cat_fin_insurance", "Insurance", "EXPENSE", "cat_financial", "security", 0xFF78909C, true, order++))
        add(CategoryEntity("cat_fin_taxes", "Taxes", "EXPENSE", "cat_financial", "receipt", 0xFF90A4AE, true, order++))
        add(CategoryEntity("cat_fin_fines", "Fines", "EXPENSE", "cat_financial", "warning", 0xFFD32F2F, true, order++))
        add(CategoryEntity("cat_fin_exchange_fees", "Currency Exchange Fees", "EXPENSE", "cat_financial", "currencyexchange", 0xFF00897B, true, order++))
        add(CategoryEntity("cat_fin_services", "Financial Services", "EXPENSE", "cat_financial", "accountbalance", 0xFF00ACC1, true, order++))
        add(CategoryEntity("cat_fin_other", "Other Financial Expenses", "EXPENSE", "cat_financial", "accountbalance", 0xFFB0BEC5, true, order++))

        // ==========================================
        // 14. LOANS & DEBT (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_debt", "Loans & Debt", "EXPENSE", null, "receipt", 0xFFE53935, true, order++))
        add(CategoryEntity("cat_debt_loan", "Loan", "EXPENSE", "cat_debt", "receipt", 0xFFC62828, true, order++))
        add(CategoryEntity("cat_debt_mortgage", "Mortgage Payment", "EXPENSE", "cat_debt", "home", 0xFFB71C1C, true, order++))
        add(CategoryEntity("cat_debt_consumer", "Consumer Loan", "EXPENSE", "cat_debt", "shoppingcart", 0xFFD32F2F, true, order++))
        add(CategoryEntity("cat_debt_car", "Car Loan", "EXPENSE", "cat_debt", "transport", 0xFFE53935, true, order++))
        add(CategoryEntity("cat_debt_card", "Credit Card", "EXPENSE", "cat_debt", "creditcard", 0xFFF44336, true, order++))
        add(CategoryEntity("cat_debt_repayment", "Debt Repayment", "EXPENSE", "cat_debt", "paid", 0xFFEF5350, true, order++))
        add(CategoryEntity("cat_debt_other", "Other Debt", "EXPENSE", "cat_debt", "receipt", 0xFFE57373, true, order++))

        // ==========================================
        // 15. CHARITY & DONATIONS (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_gifts", "Gifts & Donations", "EXPENSE", null, "gifts", 0xFFFF4081, true, order++))
        add(CategoryEntity("cat_gifts_charity", "Charity", "EXPENSE", "cat_gifts", "volunteeractivism", 0xFFE91E63, true, order++))
        add(CategoryEntity("cat_gifts_donations", "Donations", "EXPENSE", "cat_gifts", "volunteeractivism", 0xFFD81B60, true, order++))
        add(CategoryEntity("cat_gifts_to_others", "Gifts to Others", "EXPENSE", "cat_gifts", "gifts", 0xFFFF4081, true, order++))
        add(CategoryEntity("cat_gifts_religious", "Religious Donations", "EXPENSE", "cat_gifts", "volunteeractivism", 0xFFC2185B, true, order++))
        add(CategoryEntity("cat_gifts_other", "Other Donations", "EXPENSE", "cat_gifts", "gifts", 0xFFF06292, true, order++))

        // ==========================================
        // 16. OTHER EXPENSES (EXPENSE)
        // ==========================================
        add(CategoryEntity("cat_other_expenses", "Other Expenses", "EXPENSE", null, "paid", 0xFF9E9E9E, true, order++))
        add(CategoryEntity("cat_other_expense_item", "Other Expense", "EXPENSE", "cat_other_expenses", "paid", 0xFF757575, true, order++))
        add(CategoryEntity("cat_uncategorized", "Uncategorized", "EXPENSE", "cat_other_expenses", "help", 0xFF9E9E9E, true, 999))

        // ==========================================
        // 17. EMPLOYMENT (INCOME)
        // ==========================================
        order = 1
        add(CategoryEntity("cat_salary", "Salary & Employment", "INCOME", null, "salary", 0xFF4CAF50, true, order++))
        add(CategoryEntity("cat_income_salary", "Salary", "INCOME", "cat_salary", "salary", 0xFF388E3C, true, order++))
        add(CategoryEntity("cat_income_bonus", "Bonus", "INCOME", "cat_salary", "trendingup", 0xFF43A047, true, order++))
        add(CategoryEntity("cat_income_overtime", "Overtime", "INCOME", "cat_salary", "work", 0xFF4CAF50, true, order++))
        add(CategoryEntity("cat_income_commission", "Commission", "INCOME", "cat_salary", "paid", 0xFF66BB6A, true, order++))
        add(CategoryEntity("cat_income_tips", "Tips", "INCOME", "cat_salary", "paid", 0xFF81C784, true, order++))
        add(CategoryEntity("cat_income_reimbursement", "Reimbursement", "INCOME", "cat_salary", "receipt", 0xFFA5D6A7, true, order++))

        // ==========================================
        // 18. BUSINESS & FREELANCE (INCOME)
        // ==========================================
        add(CategoryEntity("cat_freelance", "Freelance & Consulting", "INCOME", null, "work", 0xFF8BC34A, true, order++))
        add(CategoryEntity("cat_income_freelance", "Freelance", "INCOME", "cat_freelance", "work", 0xFF689F38, true, order++))
        add(CategoryEntity("cat_income_business", "Business Income", "INCOME", "cat_freelance", "work", 0xFF7CB342, true, order++))
        add(CategoryEntity("cat_income_consulting", "Consulting", "INCOME", "cat_freelance", "work", 0xFF8BC34A, true, order++))
        add(CategoryEntity("cat_income_contract", "Contract Work", "INCOME", "cat_freelance", "work", 0xFF9CCC65, true, order++))
        add(CategoryEntity("cat_income_side_job", "Side Job", "INCOME", "cat_freelance", "work", 0xFFAED581, true, order++))
        add(CategoryEntity("cat_income_business_other", "Other Business Income", "INCOME", "cat_freelance", "work", 0xFFC5E1A5, true, order++))

        // ==========================================
        // 19. INVESTMENTS (INCOME)
        // ==========================================
        add(CategoryEntity("cat_invest_return", "Investment Return", "INCOME", null, "investments", 0xFF009688, true, order++))
        add(CategoryEntity("cat_income_investment_income", "Investment Income", "INCOME", "cat_invest_return", "trendingup", 0xFF00796B, true, order++))
        add(CategoryEntity("cat_income_dividends", "Dividends", "INCOME", "cat_invest_return", "trendingup", 0xFF00897B, true, order++))
        add(CategoryEntity("cat_income_interest", "Interest Income", "INCOME", "cat_invest_return", "trendingup", 0xFF009688, true, order++))
        add(CategoryEntity("cat_income_capital_gains", "Capital Gains", "INCOME", "cat_invest_return", "trendingup", 0xFF26A69A, true, order++))
        add(CategoryEntity("cat_income_crypto", "Crypto Income", "INCOME", "cat_invest_return", "crypto", 0xFFFF9800, true, order++))
        add(CategoryEntity("cat_income_rental", "Rental Income", "INCOME", "cat_invest_return", "home", 0xFF3F51B5, true, order++))

        // ==========================================
        // 20. GOVERNMENT & BENEFITS (INCOME)
        // ==========================================
        add(CategoryEntity("cat_income_government", "Government & Benefits", "INCOME", null, "accountbalance", 0xFF0288D1, true, order++))
        add(CategoryEntity("cat_income_benefits", "Benefits", "INCOME", "cat_income_government", "accountbalance", 0xFF0277BD, true, order++))
        add(CategoryEntity("cat_income_pension", "Pension", "INCOME", "cat_income_government", "accountbalance", 0xFF0288D1, true, order++))
        add(CategoryEntity("cat_income_scholarship", "Scholarship", "INCOME", "cat_income_government", "school", 0xFF039BE5, true, order++))
        add(CategoryEntity("cat_income_social_payments", "Social Payments", "INCOME", "cat_income_government", "volunteeractivism", 0xFF29B6F6, true, order++))
        add(CategoryEntity("cat_income_tax_refund", "Tax Refund", "INCOME", "cat_income_government", "receipt", 0xFF4FC3F7, true, order++))

        // ==========================================
        // 21. OTHER INCOME (INCOME)
        // ==========================================
        add(CategoryEntity("cat_other_income", "Other Income", "INCOME", null, "paid", 0xFF607D8B, true, order++))
        add(CategoryEntity("cat_income_gifts", "Gifts Received", "INCOME", "cat_other_income", "gifts", 0xFFFF4081, true, order++))
        add(CategoryEntity("cat_income_cashback", "Cashback", "INCOME", "cat_other_income", "paid", 0xFF4CAF50, true, order++))
        add(CategoryEntity("cat_income_refund", "Refund", "INCOME", "cat_other_income", "receipt", 0xFF2196F3, true, order++))
        add(CategoryEntity("cat_income_sale_of_items", "Sale of Items", "INCOME", "cat_other_income", "shoppingcart", 0xFF8E24AA, true, order++))
        add(CategoryEntity("cat_income_prize", "Prize / Winnings", "INCOME", "cat_other_income", "trophy", 0xFFFFB300, true, order++))
        add(CategoryEntity("cat_income_other_item", "Other Income", "INCOME", "cat_other_income", "paid", 0xFF78909C, true, order++))
        add(CategoryEntity("cat_income_uncategorized", "Uncategorized Income", "INCOME", "cat_other_income", "help", 0xFF9E9E9E, true, 999))

        // ==========================================
        // 22. TRANSFERS
        // ==========================================
        add(CategoryEntity("cat_transfer", "Account Transfer", "EXPENSE", null, "swaphoriz", 0xFF3F51B5, true, order++))
        add(CategoryEntity("cat_transfer_withdrawal", "Cash Withdrawal", "EXPENSE", "cat_transfer", "atm", 0xFF00ACC1, true, order++))
        add(CategoryEntity("cat_transfer_deposit", "Cash Deposit", "INCOME", "cat_transfer", "paid", 0xFF4CAF50, true, order++))
    }
}
