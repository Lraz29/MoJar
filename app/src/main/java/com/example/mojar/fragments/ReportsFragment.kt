package com.example.mojar.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mojar.R
import com.example.mojar.adapters.TopExpensesAdapter
import com.example.mojar.models.Transaction
import com.example.mojar.databinding.FragmentReportsBinding
import com.example.mojar.models.filterByMonthAndYear
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar

/** Fragment for financial analysis and visual reports.*/
class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchReportsData()
    }


    // Fetches all transactions for the current household to generate global reports.
    private fun fetchReportsData() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Get the shared household ID first
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val householdId = userDoc.getString("householdId") ?: ""

                if (householdId.isEmpty()) return@addOnSuccessListener

                // Listen to transactions ordered by date for correct graph timeline
                db.collection("transactions")
                    .whereEqualTo("householdId", householdId)
                    .orderBy("date", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null || _binding == null) return@addSnapshotListener

                        val list = snapshot.toObjects(Transaction::class.java)


                        setupGrowingGraph(list)
                        updateBiggestExpenses(list)
                        updateSpendingMood(list)
                    }
            }
    }


    // Analyzes the ratio between income and expenses to determine a "financial mood".
    private fun updateSpendingMood(transactions: List<Transaction>) {
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)

        val currentMonthTrans = transactions.filterByMonthAndYear(currentMonth, currentYear)

        if (currentMonthTrans.isEmpty()) {
            setMood(
                R.drawable.ic_mood_neutral,
                "No Data",
                "No transactions for ${getMonthName(currentMonth)} yet.",
                "#808080"
            )
            return
        }

        val totalIncome = currentMonthTrans.filter { it.type == "Income" }.sumOf { it.amount }
        val totalExpense = currentMonthTrans.filter { it.type == "Expense" }.sumOf { it.amount }

        if (totalIncome == 0.0) {
            if (totalExpense > 0.0) {
                setMood(
                    R.drawable.ic_mood_bad,
                    "Warning",
                    "Spending without income recorded!",
                    "#E57373"
                )
            } else {
                setMood(R.drawable.ic_mood_neutral, "Quiet Month", "No activity yet.", "#808080")
            }
            return
        }

        val ratio = totalExpense / totalIncome
        when {
            ratio < 0.5 -> setMood(
                R.drawable.ic_mood_good,
                "Great!",
                "Using less than 50% of income. Well done!",
                "#81C784"
            )

            ratio < 0.8 -> setMood(
                R.drawable.ic_mood_neutral,
                "Stable",
                "Your spending is balanced.",
                "#FBC02D"
            )

            ratio < 1.0 -> setMood(
                R.drawable.ic_mood_bad,
                "Careful",
                "Over 80% of income used. Watch out.",
                "#FF9800"
            )

            else -> setMood(
                R.drawable.ic_mood_bad,
                "Overbudget",
                "Spent more than earned! Stop now.",
                "#E57373"
            )
        }
    }


    // Identifies the top 3 expense categories for the current month.

    private fun updateBiggestExpenses(transactions: List<Transaction>) {
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)

        val monthExpenses = transactions.filterByMonthAndYear(currentMonth, currentYear)
            .filter { it.type == "Expense" }

        val topCategories = monthExpenses
            .groupBy { it.categoryName?.trim()?.ifEmpty { "General" } ?: "General" }
            .map { (name, list) -> Pair(name, list.sumOf { it.amount }) }
            .sortedByDescending { it.second }
            .take(3)

        if (_binding != null) {
            if (topCategories.isNotEmpty()) {
                binding.rvTopExpenses.layoutManager = LinearLayoutManager(context)
                binding.rvTopExpenses.adapter = TopExpensesAdapter(topCategories)
                binding.rvTopExpenses.visibility = View.VISIBLE
            } else {
                binding.rvTopExpenses.visibility = View.GONE
            }
        }
    }


    //Configures the LineChart to show cumulative income vs expenses over the year.
    private fun setupGrowingGraph(transactions: List<Transaction>) {
        if (transactions.isEmpty() || _binding == null) return

        val incomeEntries = ArrayList<Entry>()
        val expenseEntries = ArrayList<Entry>()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val maxMonth = transactions.maxOfOrNull {
            Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.MONTH)
        } ?: Calendar.getInstance().get(Calendar.MONTH)

        for (m in 0..maxMonth) {
            val monthTrans = transactions.filterByMonthAndYear(m, currentYear)
            incomeEntries.add(
                Entry(
                    m.toFloat(),
                    monthTrans.filter { it.type == "Income" }.sumOf { it.amount }.toFloat()
                )
            )
            expenseEntries.add(
                Entry(
                    m.toFloat(),
                    monthTrans.filter { it.type == "Expense" }.sumOf { it.amount }.toFloat()
                )
            )
        }

        val incomeSet = LineDataSet(incomeEntries, "Income").apply {
            color = Color.parseColor("#81C784")
            lineWidth = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        val expenseSet = LineDataSet(expenseEntries, "Outcome").apply {
            color = Color.parseColor("#E57373")
            lineWidth = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.growingLineChart.data = LineData(incomeSet, expenseSet)
        binding.growingLineChart.apply {
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter = object : ValueFormatter() {
                private val monthsLabels = arrayOf(
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

                override fun getFormattedValue(value: Float): String =
                    monthsLabels.getOrNull(value.toInt()) ?: ""
            }
            axisRight.isEnabled = false
            description.isEnabled = false
            animateX(1000) // Visual entry animation
            invalidate()
        }
    }

    private fun setMood(iconRes: Int, title: String, desc: String, colorHex: String) {
        if (_binding == null) return
        binding.imgSpendingMood.setImageResource(iconRes)
        binding.tvSpendingMoodTitle.text = title
        binding.tvSpendingMoodTitle.setTextColor(Color.parseColor(colorHex))
        binding.tvSpendingMoodDesc.text = desc
    }

    private fun getMonthName(monthIndex: Int): String {
        return arrayOf(
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
        )[monthIndex]
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}