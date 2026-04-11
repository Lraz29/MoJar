package com.example.mojar.utils

import com.example.mojar.models.Category

object DefaultData {
    fun getDefaultCategories(): List<Category> {
        val grey = "#F6F5EC"
        return listOf(
            Category("1", "Bills", "ic_bills", grey, "Expense", true),
            Category("2", "Car", "ic_car", grey, "Expense", true),
            Category("3", "Groceries", "ic_grocery", grey, "Expense", true),
            Category("4", "Clothing", "ic_clothing", grey, "Expense", true),
            Category("5", "Insurance", "ic_insurance", grey, "Expense", true),
            Category("6", "Travel", "ic_travel", grey, "Expense", true),
            Category("7", "Education", "ic_education", grey, "Expense", true),
            Category("8", "Electronics", "ic_electronics", grey, "Expense", true),
            Category("9", "Leisure", "ic_entertainment", grey, "Expense", true),
            Category("10", "Health", "ic_health", grey, "Expense", true),
            Category("11", "Housing", "ic_housing", grey, "Expense", true),
            Category("12", "Pets", "ic_pets", grey, "Expense", true),
            Category("13", "Phone", "ic_phone", grey, "Expense", true),
            Category("14", "Shopping", "ic_shopping", grey, "Expense", true),
            Category("15", "Sport", "ic_sport", grey, "Expense", true),
            Category("16", "Transfer", "ic_transference", grey, "Expense", true),
            Category("17", "Beauty", "ic_beauty", grey, "Expense", true),
            Category("18", "Other", "ic_other", grey, "Expense", true),
            Category("19", "Job salary", "ic_job", "#F5F5F5", "Income", true),
            Category("20", "Gift", "ic_gift", "#F5F5F5", "Income", true),
            Category("21", "Invest", "ic_invest", "#F5F5F5", "Income", true),
            Category("22", "Part-time job", "ic_part_time", "#F5F5F5", "Income", true),
            Category("23", "Other Income", "ic_default", "#F5F5F5", "Income", true)
        )
    }
}