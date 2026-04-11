package com.example.mojar.models

data class Category(
    val id: String = "",
    val name: String = "",
    val iconName: String = "ic_default",
    val colorHex: String = "#F5F5F5",
    val type: String = "Expense",
    val isDefault: Boolean = false,
    var totalAmount: Double = 0.0
)