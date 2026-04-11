package com.example.mojar.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mojar.models.Category
import com.example.mojar.adapters.CategoryGridAdapter
import com.example.mojar.adapters.DetailedExpenseAdapter
import com.example.mojar.R
import com.example.mojar.models.Transaction
import com.example.mojar.databinding.FragmentExpenseBreakdownBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

/** Fragment that provides a detailed breakdown of expenses or income by category.*/
class ExpenseBreakdownFragment : Fragment() {

    private var _binding: FragmentExpenseBreakdownBinding? = null
    private val binding get() = _binding!!

    // Internal state controlled by parameters from Dashboard
    private var displayType: String = "Expense"
    private var selectedMonth: Int = 0
    private lateinit var categoryAdapter: CategoryGridAdapter

    companion object {
        private const val ARG_TYPE = "type"
        private const val ARG_MONTH = "selected_month"


        // Factory method to encapsulate fragment creation with arguments.

        fun newInstance(type: String, month: Int): ExpenseBreakdownFragment {
            val fragment = ExpenseBreakdownFragment()
            val args = Bundle()
            args.putString(ARG_TYPE, type)
            args.putInt(ARG_MONTH, month)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayType = arguments?.getString(ARG_TYPE) ?: "Expense"
        selectedMonth = arguments?.getInt(ARG_MONTH) ?: Calendar.getInstance().get(Calendar.MONTH)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseBreakdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTitle.text =
            if (displayType == "Expense") "Expenses Breakdown" else "Income Breakdown"

        setupRecyclerView()
        fetchData()
    }

    //Sets up the RecyclerView with a custom click listener to open the details dialog.
    private fun setupRecyclerView() {
        // Higher-order function: Passing the click logic into the adapter
        categoryAdapter = CategoryGridAdapter(listOf()) { category ->
            showCategoryDetails(category.name)
        }

        binding.categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }
    }

    /**
     * Real-time Data Sync:
     * 1. Fetches categories of the current type.
     * 2. Fetches all transactions to calculate per-category totals for the selected month.
     */
    private fun fetchData() {
        val db = FirebaseFirestore.getInstance()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        db.collection("categories")
            .whereEqualTo("type", displayType)
            .addSnapshotListener { catSnapshot, _ ->
                if (catSnapshot == null || _binding == null) return@addSnapshotListener
                val categories = catSnapshot.toObjects(Category::class.java)

                db.collection("transactions")
                    .whereEqualTo("type", displayType)
                    .addSnapshotListener { transSnapshot, _ ->
                        if (transSnapshot == null || _binding == null) return@addSnapshotListener
                        val allTransactions = transSnapshot.toObjects(Transaction::class.java)

                        val filteredTransactions = allTransactions.filter {
                            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                            cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == currentYear
                        }

                        categories.forEach { category ->
                            category.totalAmount = filteredTransactions
                                .filter { it.categoryName == category.name }
                                .sumOf { it.amount }
                        }

                        categoryAdapter.updateData(categories)
                    }
            }
    }

    //Displays a BottomSheet with detailed transaction logs for the chosen category.
    private fun showCategoryDetails(categoryName: String) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_category_details, null)
        dialog.setContentView(view)

        val rv = view.findViewById<RecyclerView>(R.id.rvDetailedTransactions)
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        tvTitle.text = "Transactions: $categoryName"

        val db = FirebaseFirestore.getInstance()

        db.collection("transactions")
            .whereEqualTo("categoryName", categoryName)
            .whereEqualTo("type", displayType)
            .addSnapshotListener { snapshot, _ ->
                val allTrans = snapshot?.toObjects(Transaction::class.java) ?: listOf()

                val filtered = allTrans.filter {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    cal.get(Calendar.MONTH) == selectedMonth
                }

                rv.layoutManager = LinearLayoutManager(context)
                rv.adapter = DetailedExpenseAdapter(filtered) { transactionToDelete ->
                    deleteTransaction(transactionToDelete, dialog)
                }
            }
        dialog.show()
    }

    private fun deleteTransaction(transaction: Transaction, dialog: DialogInterface) {
        val db = FirebaseFirestore.getInstance()
        // Standard deletion using Firestore document ID
        db.collection("transactions").document(transaction.id).delete()
            .addOnSuccessListener {
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}