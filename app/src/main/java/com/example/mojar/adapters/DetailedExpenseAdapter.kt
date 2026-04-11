package com.example.mojar.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mojar.models.Transaction
import com.example.mojar.databinding.ItemDetailedExpenseBinding

/** Adapter for showing detailed transactions with a delete option.*/
class DetailedExpenseAdapter(
    private val transactions: List<Transaction>,
    // Lambda function to handle delete clicks in the Fragment
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<DetailedExpenseAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDetailedExpenseBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetailedExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.binding.apply {
            // Bind data to views
            tvDescription.text = transaction.description
            tvAmount.text = "₪${transaction.amount.toInt()}"

            btnDelete.setOnClickListener {
                onDeleteClick(transaction)
            }
        }
    }

    override fun getItemCount() = transactions.size
}