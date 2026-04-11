package com.example.mojar.models

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",

    @get:PropertyName("displayName") @set:PropertyName("displayName") var name: String = "",

    val email: String = "",
    val householdId: String = "",
    val lastActive: Long = 0
)