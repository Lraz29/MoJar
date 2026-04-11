package com.example.mojar.models

import java.util.Calendar

data class Transaction(
    val id: String = "",
    val amount: Double = 0.0,
    val categoryName: String = "",
    val type: String = "Expense",
    val description: String = "",
    val date: Long = 0,
    val category: String = "",
    val householdId: String = ""
)

fun List<Transaction>.filterByMonthAndYear(month: Int, year: Int): List<Transaction> {
    val cal = Calendar.getInstance()
    return this.filter {
        cal.timeInMillis = it.date
        cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
    }
}