package com.example.mojar.fragments

import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mojar.adapters.DetailedExpenseAdapter
import com.example.mojar.models.Transaction
import com.example.mojar.databinding.FragmentCategoryDetailBinding
import com.google.firebase.firestore.FirebaseFirestore

class CategoryDetailFragment : Fragment() {

    /**Fragment that displays a detailed list of transactions for a specific category.*/
    private var _binding: FragmentCategoryDetailBinding? = null
    private val binding get() = _binding!!
    private var categoryName: String? = null
    private var categoryType: String? = null
    private var selectedMonth: Int = 0

    companion object {
        fun newInstance(category: String, type: String, month: Int) =
            CategoryDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("CATEGORY_NAME", category)
                    putString("CATEGORY_TYPE", type)
                    putInt("SELECTED_MONTH", month)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryName = arguments?.getString("CATEGORY_NAME")
        categoryType = arguments?.getString("CATEGORY_TYPE")
        selectedMonth = arguments?.getInt("SELECTED_MONTH") ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.categoryTitle.text = categoryName
        binding.detailCategoryName.text = categoryName

        setupRecyclerView()
        fetchDetailedTransactions()

        binding.backButton.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.addItemButton.setOnClickListener {
            val addDialog = AddTransactionFragment.newInstance(
                categoryName ?: "",
                categoryType ?: "Expense",
                selectedMonth
            )
            addDialog.show(childFragmentManager, "AddTransaction")
        }
    }

    private fun setupRecyclerView() {
        binding.detailedExpensesRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    //Fetches transactions for the selected category and filters them by the active month.
    private fun fetchDetailedTransactions() {
        val db = FirebaseFirestore.getInstance()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        db.collection("transactions")
            .whereEqualTo("categoryName", categoryName)
            .whereEqualTo("type", categoryType)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && _binding != null) {
                    val allList = snapshot.toObjects(Transaction::class.java)

                    val filteredList = allList.filter {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                        cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == currentYear
                    }

                    val total = filteredList.sumOf { it.amount }
                    binding.detailTotalAmount.text = "₪${total.toInt()}"

                    binding.detailedExpensesRecyclerView.adapter =
                        DetailedExpenseAdapter(filteredList) {
                            deleteTransaction(it)
                        }
                }
            }
    }

    //Deletes a transaction and uses a Firestore Transaction to safely update the category's total amount.
    private fun deleteTransaction(transaction: Transaction) {
        val db = FirebaseFirestore.getInstance()

        db.collection("transactions").document(transaction.id).delete()
            .addOnSuccessListener {
                val categoryId = "${transaction.categoryName}_${transaction.type}"
                val categoryRef = db.collection("categories").document(categoryId)

                // Use a Database Transaction to update the summary field safely
                db.runTransaction { transactionScope ->
                    val snapshot = transactionScope.get(categoryRef)
                    val currentTotal = snapshot.getDouble("totalAmount") ?: 0.0
                    // Deduct the amount, ensuring it doesn't go below zero
                    val newTotal = (currentTotal - transaction.amount).coerceAtLeast(0.0)
                    transactionScope.update(categoryRef, "totalAmount", newTotal)
                }.addOnSuccessListener {
                    if (isAdded) {
                        Toast.makeText(context, "Deleted and updated total", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}