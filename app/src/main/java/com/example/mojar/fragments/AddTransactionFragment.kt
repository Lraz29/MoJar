package com.example.mojar.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.mojar.R
import com.example.mojar.models.Transaction
import com.example.mojar.databinding.FragmentAddTransactionBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Calendar

class AddTransactionFragment : BottomSheetDialogFragment() {

    /**BottomSheetDialog for adding a new transaction to a specific category.*/
    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    // Transaction details passed from the previous screen
    private var categoryName: String = ""
    private var categoryType: String = ""
    private var selectedMonth: Int = 0

    companion object {
        fun newInstance(category: String, type: String, month: Int) =
            AddTransactionFragment().apply {
                arguments = Bundle().apply {
                    putString("SELECTED_CATEGORY", category)
                    putString("SELECTED_TYPE", type)
                    putInt("SELECTED_MONTH", month)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryName = arguments?.getString("SELECTED_CATEGORY") ?: ""
        categoryType = arguments?.getString("SELECTED_TYPE") ?: "Expense"
        selectedMonth =
            arguments?.getInt("SELECTED_MONTH") ?: Calendar.getInstance().get(Calendar.MONTH)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvCategoryTitle.text = "Add $categoryType to: $categoryName"

        binding.btnSaveTransaction.setOnClickListener {
            val amountStr = binding.etAmount.text.toString()
            val note = binding.etNote.text.toString().trim()

            if (amountStr.isNotEmpty() && note.isNotEmpty()) {
                saveTransaction(amountStr.toDouble(), note)
            } else {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Orchestrates the saving process:
     * 1. Fetches User's Household ID.
     * 2. Creates a Transaction object.
     * 3. Saves to Firestore and updates user activity.
     */
    private fun saveTransaction(amount: Double, note: String) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return

        //Step 1: Identify which "Jar" (Household) this transaction belongs to
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDocument ->
                val userHouseholdId = userDocument.getString("householdId") ?: ""

                // Step 2: Prepare the Transaction object
                val transactionId = db.collection("transactions").document().id
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.MONTH, selectedMonth)
                val transactionDate = calendar.timeInMillis

                val transaction = Transaction(
                    id = transactionId,
                    amount = amount,
                    categoryName = categoryName,
                    type = categoryType,
                    description = note,
                    date = transactionDate,
                    householdId = userHouseholdId // הקישור הדינמי שדיברנו עליו
                )

                //Step 3: Write to Firestore
                db.collection("transactions").document(transactionId).set(transaction)
                    .addOnSuccessListener {

                        // Step 4: Update user's last activity timestamp for the Household members view
                        val updateData = hashMapOf("lastActive" to System.currentTimeMillis())
                        db.collection("users").document(userId)
                            .set(updateData, SetOptions.merge())

                        // צליל הצלחה וסגירה
                        val mp = MediaPlayer.create(context, R.raw.success_sound)
                        mp.start()

                        if (isAdded) {
                            Toast.makeText(
                                context,
                                "Saved to ${getMonthName(selectedMonth)}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error saving transaction", Toast.LENGTH_SHORT)
                            .show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Could not identify household", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getMonthName(monthIndex: Int): String {
        val months = listOf(
            "Jan",
            "Feb",
            "Mar",
            "Apr",
            "May",
            "Jun",
            "Jul",
            "Aug",
            "Sep",
            "Oct",
            "Nov",
            "Dec"
        )
        return months.getOrElse(monthIndex) { "Unknown" }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}