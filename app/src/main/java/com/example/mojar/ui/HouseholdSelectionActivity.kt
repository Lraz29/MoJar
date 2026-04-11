package com.example.mojar.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mojar.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

/** Activity for joining or creating a shared 'Household' (Jar).*/
class HouseholdSelectionActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_household_selection)

        val btnCreate = findViewById<Button>(R.id.btnCreateHousehold)
        val btnJoin = findViewById<Button>(R.id.btnJoinHousehold)
        val etInviteCode = findViewById<EditText>(R.id.etInviteCode)

        btnCreate.setOnClickListener { createNewHousehold() }

        btnJoin.setOnClickListener {
            val code = etInviteCode.text.toString().trim().uppercase()
            if (code.isNotEmpty()) {
                joinExistingHousehold(code)
            } else {
                Toast.makeText(this, "Please enter a code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Logic for creating a new household:
     * 1. Generates a unique 6-character code.
     * 2. Creates a 'household' document in Firestore.
     * 3. Links the current user to this household ID.
     */
    private fun createNewHousehold() {
        Log.d("MOJAR_DEBUG", "Starting household creation")
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.e("MOJAR_DEBUG", "User not authenticated")
            return
        }
        val inviteCode = UUID.randomUUID().toString().substring(0, 6).uppercase()

        val household = hashMapOf(
            "adminId" to userId,
            "inviteCode" to inviteCode,
            "members" to listOf(userId),
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("households").document(inviteCode)
            .set(household)
            .addOnSuccessListener {
                Log.d("MOJAR_DEBUG", "Household $inviteCode created successfully")
                updateUserHousehold(userId, inviteCode, true)
            }
            .addOnFailureListener { e ->
                Log.e("MOJAR_DEBUG", "Firestore Error", e)
            }
    }

    /**
     * Logic for joining an existing household:
     * 1. Validates the code exists.
     * 2. Adds user ID to the household's members array.
     * 3. Updates user's profile with the household ID.
     */
    private fun joinExistingHousehold(code: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("households").document(code).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {

                    db.collection("households").document(code)
                        .update(
                            "members",
                            FieldValue.arrayUnion(userId)
                        )

                    updateUserHousehold(userId, code, false)
                    Toast.makeText(this, "Welcome to the Jar!", Toast.LENGTH_SHORT).show()
                    goToMainClean()
                } else {
                    Toast.makeText(
                        this,
                        "Invalid code. Please check and try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun updateUserHousehold(userId: String, inviteCode: String, isCreation: Boolean) {
        db.collection("users").document(userId)
            .update("householdId", inviteCode)
            .addOnSuccessListener {
                if (isCreation) {
                    Toast.makeText(this, "New Jar Created: $inviteCode", Toast.LENGTH_LONG).show()
                }
                goToMainClean()
            }
    }


    // Navigates to MainActivity and clears the activity stack.
    private fun goToMainClean() {
        val intent = Intent(this, MainActivity::class.java)
        // Intent Flags to ensure the user cannot navigate back to selection/login
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Close current activity
    }
}