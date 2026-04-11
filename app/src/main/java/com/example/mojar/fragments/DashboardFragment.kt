package com.example.mojar.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mojar.models.Category
import com.example.mojar.utils.DefaultData
import com.example.mojar.adapters.MonthAdapter
import com.example.mojar.R
import com.example.mojar.models.Transaction
import com.example.mojar.databinding.FragmentDashboardBinding
import com.example.mojar.models.filterByMonthAndYear
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Calendar

/**Main Dashboard Fragment. Displays financial summaries, PieCharts, and BarCharts.*/
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private var selectedMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var allTransactions = listOf<Transaction>()
    private var allCategories = listOf<Category>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMonthPicker()     // Initialize horizontal month selector
        setupClickListeners()  // Set navigation to breakdown screens
        setupDefaultCategories() // Ensure standard categories exist in DB
        loadDashboardData()    // Fetch real-time data from Firestore
    }

    //Initializes the horizontal month selector using a RecyclerView.
    private fun setupMonthPicker() {
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

        val monthAdapter = MonthAdapter(months, selectedMonth) { position ->
            selectedMonth = position
            filterAndRefreshUI()
        }

        binding.rvMonthPicker.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = monthAdapter
            scrollToPosition(selectedMonth)
        }
    }

    private fun setupClickListeners() {
        binding.expenseCard.setOnClickListener {
            val fragment = AddFragment.newInstance("Expense", selectedMonth)
            navigateToBreakdown(fragment)
        }

        binding.incomeCard.setOnClickListener {
            val fragment = AddFragment.newInstance("Income", selectedMonth)
            navigateToBreakdown(fragment)
        }
    }

    /**
     * Core Data Loading Logic:
     * 1. Get user's Household ID.
     * 2. Listen to shared Categories.
     * 3. Listen to Transactions filtered by Household ID (Security & Isolation).
     * */
    private fun loadDashboardData() {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            val currentHouseholdId = userDoc.getString("householdId") ?: ""

            if (currentHouseholdId.isEmpty()) {
                Log.d("Dashboard", "No household ID found for this user")
                return@addOnSuccessListener
            }

            db.collection("categories").addSnapshotListener { catSnapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                allCategories = catSnapshot?.toObjects(Category::class.java) ?: listOf()

                db.collection("transactions")
                    .whereEqualTo("householdId", currentHouseholdId)
                    .addSnapshotListener { transSnapshot, e ->
                        if (_binding == null) return@addSnapshotListener

                        if (e != null) {
                            Log.e("Dashboard", "Listen failed", e)
                            return@addSnapshotListener
                        }

                        allTransactions =
                            transSnapshot?.toObjects(Transaction::class.java) ?: listOf()

                        filterAndRefreshUI()
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("Dashboard", "Error fetching user data", e)
        }
    }

    //Main UI Refresh Loop: Applies time filtering and updates all visual components.
    private fun filterAndRefreshUI() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val filteredTransactions = allTransactions.filterByMonthAndYear(selectedMonth, currentYear)

        updateDashboardSummary(filteredTransactions)

        val expenseTotals = mutableMapOf<String, Double>()

        allCategories.filter { it.type == "Expense" }.forEach {
            expenseTotals[it.name] = 0.0
        }

        filteredTransactions.filter { it.type == "Expense" }.forEach { trans ->
            val currentSum = expenseTotals[trans.categoryName] ?: 0.0
            expenseTotals[trans.categoryName] = currentSum + trans.amount
        }

        setupPieChart(expenseTotals)

        setupBarChart(filteredTransactions)
    }

    //Configures the Donut PieChart using MPAndroidChart library.
    private fun updateDashboardSummary(transactions: List<Transaction>) {
        val incomeSum = transactions.filter { it.type == "Income" }.sumOf { it.amount }
        val expenseSum = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val savingsSum = incomeSum - expenseSum

        binding.totalIncomeText.text = "₪${incomeSum.toInt()}"
        binding.totalExpensesText.text = "₪${expenseSum.toInt()}"
        binding.totalSavingsText.text = "₪${savingsSum.toInt()}"
    }

    private fun setupPieChart(expenseMap: Map<String, Double>) {
        val activeExpenses = expenseMap.filter { it.value > 0 }

        if (activeExpenses.isEmpty()) {
            binding.expensePieChart.apply {
                data = null
                centerText = "No data"
                invalidate()
            }
            return
        }

        val totalExpense = activeExpenses.values.sum()
        val entries = activeExpenses.map { PieEntry(it.value.toFloat(), it.key) }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#A8B5A2"), Color.parseColor("#C2D3DA"),
                Color.parseColor("#E1C7C2"), Color.parseColor("#D4D4D4"),
                Color.parseColor("#B8C5D6"), Color.parseColor("#E9DCC9")
            )
            sliceSpace = 3f
            setDrawValues(false)
        }

        binding.expensePieChart.apply {
            data = PieData(dataSet)

            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)

            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.TOP // מתחיל מלמעלה
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                orientation = Legend.LegendOrientation.VERTICAL

                setDrawInside(false)
                isWordWrapEnabled = true

                yEntrySpace = 6f
                textSize = 11f
                textColor = Color.parseColor("#4A4A4A")
            }

            setExtraOffsets(60f, 15f, 5f, 15f)

            description.isEnabled = false
            setDrawEntryLabels(false)
            holeRadius = 72f

            centerText = "Total\n₪${totalExpense.toInt()}"
            setCenterTextSize(15f)
            setCenterTextColor(Color.parseColor("#4A4A4A"))
            setCenterTextTypeface(Typeface.DEFAULT_BOLD)

            animateY(1000)
            invalidate()
        }
    }

    private fun setupBarChart(transactions: List<Transaction>) {
        val incomeTotal = transactions.filter { it.type == "Income" }.sumOf { it.amount }.toFloat()
        val expenseTotal =
            transactions.filter { it.type == "Expense" }.sumOf { it.amount }.toFloat()

        val entries = listOf(BarEntry(0f, incomeTotal), BarEntry(1f, expenseTotal))
        val dataSet = BarDataSet(entries, "").apply {
            colors = listOf(Color.parseColor("#A8B5A2"), Color.parseColor("#E1C7C2"))
            valueTextColor = Color.parseColor("#2D2D2D")
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = "₪${value.toInt()}"
            }
        }

        binding.incomeExpenseBarChart.apply {
            data = BarData(dataSet)
            xAxis.apply {
                isEnabled = true
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.parseColor("#444444")
                textSize = 10f
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String =
                        if (value == 0f) "Income" else "Expense"
                }
            }
            axisLeft.isEnabled = true
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    //Self-healing logic to ensure required categories exist in Firestore.
    private fun setupDefaultCategories() {
        val db = FirebaseFirestore.getInstance()
        for (cat in DefaultData.getDefaultCategories()) {
            val docId = "${cat.name}_${cat.type}"
            db.collection("categories").document(docId)
                .set(cat, SetOptions.merge())
        }
    }

    private fun navigateToBreakdown(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out)
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}