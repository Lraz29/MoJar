package com.example.mojar.fragments

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mojar.R
import com.example.mojar.models.Transaction
import com.example.mojar.databinding.FragmentSavingsBinding
import com.example.mojar.models.filterByMonthAndYear
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.util.Calendar

/** Fragment responsible for tracking financial goals and monthly savings progress.*/
class SavingsFragment : Fragment() {

    private var _binding: FragmentSavingsBinding? = null
    private val binding get() = _binding!!

    private var totalSavingsGoal = 0.0
    private var monthlyContributionPlan = 0.0
    private var currentHouseholdId = ""

    private var settingsListener: ListenerRegistration? = null
    private var transactionsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.savingsCard.setOnClickListener {
            if (currentHouseholdId.isNotEmpty()) {
                showSavingsTargetDialog()
            }
        }

        fetchSavingsData()
    }

    private fun fetchSavingsData() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            currentHouseholdId = userDoc.getString("householdId") ?: ""

            if (currentHouseholdId.isEmpty()) return@addOnSuccessListener

            settingsListener = db.collection("households").document(currentHouseholdId)
                .addSnapshotListener { householdSnapshot, _ ->
                    if (_binding == null) return@addSnapshotListener

                    totalSavingsGoal = householdSnapshot?.getDouble("totalGoal") ?: 0.0
                    monthlyContributionPlan = householdSnapshot?.getDouble("monthlyPlan") ?: 0.0

                    transactionsListener = db.collection("transactions")
                        .whereEqualTo("householdId", currentHouseholdId)
                        .addSnapshotListener { transSnapshot, _ ->
                            if (transSnapshot != null && _binding != null) {
                                val allTransactions = transSnapshot.toObjects(Transaction::class.java)

                                val now = Calendar.getInstance()
                                val currentMonth = now.get(Calendar.MONTH)
                                val currentYear = now.get(Calendar.YEAR)

                                val currentMonthTrans = allTransactions.filterByMonthAndYear(currentMonth, currentYear)

                                val totalIncome = currentMonthTrans.filter { it.type == "Income" }.sumOf { it.amount }
                                val totalExpense = currentMonthTrans.filter { it.type == "Expense" }.sumOf { it.amount }

                                val currentBalance = totalIncome - totalExpense

                                updateUI(currentBalance)
                            }
                        }
                }
        }
    }

    private fun updateUI(totalBalance: Double) {
        if (_binding == null) return

        binding.totalSavingsHeader.text = "₪${String.format("%,.0f", totalBalance)}"

        val progressPercent = if (totalSavingsGoal > 0) {
            ((monthlyContributionPlan / totalSavingsGoal) * 100).toInt().coerceIn(0, 100)
        } else 0

        binding.headerProgressBar.progress = progressPercent
        binding.headerPercentText.text = "$progressPercent%"
        binding.programAmount.text = "₪${monthlyContributionPlan.toInt()}"

        val leftOver = (totalBalance - monthlyContributionPlan).coerceAtLeast(0.0)
        binding.leftOverAmount.text = "₪${leftOver.toInt()}"

        setupDonutChart(monthlyContributionPlan, progressPercent, totalSavingsGoal)
    }

    private fun setupDonutChart(currentSavings: Double, progressPercent: Int, target: Double) {
        val entries = ArrayList<PieEntry>()

        if (target <= 0) {
            entries.add(PieEntry(1f, ""))
        } else {
            entries.add(PieEntry(currentSavings.toFloat(), ""))
            val remaining = (target - currentSavings).coerceAtLeast(0.0).toFloat()
            entries.add(PieEntry(remaining, ""))
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = if (target > 0) {
                listOf(Color.parseColor("#4CAF50"), Color.parseColor("#F1F8E9"))
            } else {
                listOf(Color.parseColor("#E0E0E0"))
            }
            setDrawValues(false)
            sliceSpace = 3f
        }

        binding.savingsDonutChart.apply {
            data = PieData(dataSet)
            isDrawHoleEnabled = true
            holeRadius = 85f

            val line1 = "$progressPercent%"
            val line2 = "₪${currentSavings.toInt()} of ₪${target.toInt()}"
            val line3 = "\nSaved"
            val fullText = "$line1\n$line2$line3"

            val centerSpannable = SpannableString(fullText).apply {
                setSpan(AbsoluteSizeSpan(40, true), 0, line1.length, 0)
                setSpan(StyleSpan(Typeface.BOLD), 0, line1.length, 0)

                val startGoal = line1.length + 1
                val endGoal = startGoal + line2.length
                setSpan(AbsoluteSizeSpan(16, true), startGoal, endGoal, 0)
                setSpan(ForegroundColorSpan(Color.parseColor("#424242")), startGoal, endGoal, 0)
                setSpan(StyleSpan(Typeface.BOLD), startGoal, endGoal, 0)
            }

            centerText = centerSpannable
            description.isEnabled = false
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun showSavingsTargetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_savings, null)
        val etTotal = dialogView.findViewById<EditText>(R.id.etTotalGoal)
        val etMonthly = dialogView.findViewById<EditText>(R.id.etMonthlyContribution)

        etTotal.setText(if (totalSavingsGoal > 0) totalSavingsGoal.toInt().toString() else "")
        etMonthly.setText(if (monthlyContributionPlan > 0) monthlyContributionPlan.toInt().toString() else "")

        AlertDialog.Builder(requireContext())
            .setTitle("Set Jar Goals")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newTotal = etTotal.text.toString().toDoubleOrNull() ?: 0.0
                val newMonthly = etMonthly.text.toString().toDoubleOrNull() ?: 0.0
                saveSavingsSettings(newTotal, newMonthly)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveSavingsSettings(total: Double, monthly: Double) {
        if (currentHouseholdId.isEmpty()) return

        val updates = hashMapOf<String, Any>(
            "totalGoal" to total,
            "monthlyPlan" to monthly
        )

        FirebaseFirestore.getInstance().collection("households").document(currentHouseholdId)
            .update(updates)
            .addOnSuccessListener {
                if (isAdded) Toast.makeText(context, "Goals updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                FirebaseFirestore.getInstance().collection("households")
                    .document(currentHouseholdId)
                    .set(updates, SetOptions.merge())
            }
    }

    override fun onDestroyView() {
        settingsListener?.remove()
        transactionsListener?.remove()
        super.onDestroyView()
        _binding = null
    }
}