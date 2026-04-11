package com.example.mojar.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mojar.models.Category
import com.example.mojar.adapters.CategoryGridAdapter
import com.example.mojar.adapters.DetailedExpenseAdapter
import com.example.mojar.R
import com.example.mojar.models.Transaction
import com.example.mojar.databinding.FragmentAddCategoryBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar

class AddFragment : Fragment() {

    /**Fragment for selecting a category and viewing/adding transactions.*/
    private var _binding: FragmentAddCategoryBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var currentType = "Expense"
    private var selectedMonth = Calendar.getInstance().get(Calendar.MONTH)

    // Firebase Listeners for real-time synchronization
    private var categoriesListener: ListenerRegistration? = null
    private var transactionsListener: ListenerRegistration? = null

    companion object {
        fun newInstance(type: String, month: Int): AddFragment {
            val fragment = AddFragment()
            val args = Bundle()
            args.putString("type", type)
            args.putInt("month", month)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentType = arguments?.getString("type") ?: "Expense"
        selectedMonth = arguments?.getInt("month") ?: Calendar.getInstance().get(Calendar.MONTH)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        if (currentType == "Income") {
            binding.toggleGroup.check(R.id.btnSelectIncome)
        } else {
            binding.toggleGroup.check(R.id.btnSelectExpense)
        }
        // Listener for switching between Income and Expense categories
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentType = if (checkedId == R.id.btnSelectExpense) "Expense" else "Income"
                listenToCategories()
            }
        }
        listenToCategories()
    }

    private fun setupRecyclerView() {
        binding.rvCategoriesGrid.layoutManager = GridLayoutManager(context, 3)
    }

    //Nested real-time listener: Fetches categories and then calculates their totals based on transactions.
    private fun listenToCategories() {
        categoriesListener?.remove()
        transactionsListener?.remove()

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        // Step 1: Listen to Categories of the selected type
        categoriesListener = db.collection("categories")
            .whereEqualTo("type", currentType)
            .addSnapshotListener { catSnapshot, _ ->
                if (_binding == null || !isAdded) return@addSnapshotListener
                val categories = catSnapshot?.toObjects(Category::class.java) ?: listOf()

                // Step 2: Listen to Transactions to calculate totals per category
                transactionsListener = db.collection("transactions")
                    .addSnapshotListener { transSnapshot, _ ->
                        if (_binding == null || !isAdded) return@addSnapshotListener
                        val allTransactions =
                            transSnapshot?.toObjects(Transaction::class.java) ?: listOf()

                        // Filter transactions by the selected month/year
                        val filteredTrans = allTransactions.filter {
                            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                            cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == currentYear
                        }

                        // Calculate sum for each category dynamically
                        categories.forEach { category ->
                            category.totalAmount = filteredTrans
                                .filter { it.categoryName == category.name }
                                .sumOf { it.amount }
                        }

                        // Update the adapter with the fresh data and click logic
                        val adapter = CategoryGridAdapter(categories) { category ->
                            showCategoryDetails(category)
                        }
                        binding.rvCategoriesGrid.adapter = adapter
                    }
            }
    }

    //Opens a BottomSheet dialog to show detailed transactions for a specific category.
    private fun showCategoryDetails(category: Category) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_category_details, null)
        dialog.setContentView(view)

        val rv = view.findViewById<RecyclerView>(R.id.rvDetailedTransactions)
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val btnAdd = view.findViewById<Button>(R.id.btnAddNewTransaction)

        tvTitle.text = "Transactions: ${category.name}"

        db.collection("transactions")
            .whereEqualTo("categoryName", category.name)
            .whereEqualTo("type", currentType)
            .addSnapshotListener { snapshot, _ ->
                val allTrans = snapshot?.toObjects(Transaction::class.java) ?: listOf()
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)

                // Time filter to ensure we only see relevant data
                val filtered = allTrans.filter {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == currentYear
                }

                rv.layoutManager = LinearLayoutManager(context)
                rv.adapter = DetailedExpenseAdapter(filtered) { transactionToDelete ->
                    deleteTransaction(transactionToDelete)
                }
            }

        btnAdd.setOnClickListener {
            val addSheet =
                AddTransactionFragment.newInstance(category.name, category.type, selectedMonth)
            addSheet.show(parentFragmentManager, "add_trans")
            dialog.dismiss()
        }
        dialog.show()
    }

    //Deletes a specific transaction from Firestore.
    private fun deleteTransaction(transaction: Transaction) {
        val db = FirebaseFirestore.getInstance()

        db.collection("transactions").document(transaction.id).delete()
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        categoriesListener?.remove()
        transactionsListener?.remove()
        super.onDestroyView()
        _binding = null
    }
}