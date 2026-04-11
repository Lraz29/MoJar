package com.example.mojar.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mojar.databinding.ItemTopExpenseBinding

/** Specialized adapter for displaying the "Top 3 Expenses" in the Reports screen.*/
class TopExpensesAdapter(private val items: List<Pair<String, Double>>) :
    RecyclerView.Adapter<TopExpensesAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTopExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (name, amount) = items[position]

        holder.binding.apply {
            itemCategory.text = name
            itemAmount.text = "₪${amount.toInt()}"

            itemAmount.setTextColor(Color.parseColor("#E57373"))
        }
    }

    override fun getItemCount() = items.size
}