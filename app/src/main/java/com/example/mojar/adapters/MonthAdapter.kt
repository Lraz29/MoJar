package com.example.mojar.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mojar.databinding.ItemMonthBinding

/**
 * Adapter for the horizontal month picker.
 * Manages selection states and visual feedback for the time filter.
 */
class MonthAdapter(
    private val months: List<String>,
    private var selectedPosition: Int, // Keeps track of which month is currently active
    private val onMonthSelected: (Int) -> Unit
) : RecyclerView.Adapter<MonthAdapter.ViewHolder>() {


    class ViewHolder(val binding: ItemMonthBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val binding = ItemMonthBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tvMonthName.text = months[position]

        /**
         * Visual Feedback Logic:
         * Highlights the selected month with Sage Green and dims others to guide the user's eye.
         */
        if (position == selectedPosition) {
            holder.binding.tvMonthName.setTextColor(Color.parseColor("#A8B5A2"))
            holder.binding.tvMonthName.alpha = 1.0f
        } else {
            holder.binding.tvMonthName.setTextColor(Color.parseColor("#BCBCBC"))
            holder.binding.tvMonthName.alpha = 0.6f
        }

        holder.itemView.setOnClickListener {
            if (selectedPosition != position) {
                // Efficiency: Only update the items that actually changed state
                val previousPosition = selectedPosition
                selectedPosition = position

                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                onMonthSelected(position)
            }
        }
    }

    override fun getItemCount() = months.size
}