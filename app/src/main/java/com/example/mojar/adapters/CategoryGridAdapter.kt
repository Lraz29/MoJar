package com.example.mojar.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mojar.models.Category
import com.example.mojar.R
import com.example.mojar.databinding.ItemCategoryGridBinding

/**Adapter for the category grid. Handles dynamic icons and background colors from Firestore.*/
class CategoryGridAdapter(
    private var categories: List<Category>,
    private val onItemClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryGridAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCategoryGridBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemCategoryGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        val context = holder.binding.root.context

        holder.binding.apply {
            // Text update
            tvCategoryName.text = category.name
            tvCategoryAmount.text = "₪${category.totalAmount.toInt()}"

            ivCategoryIcon.setColorFilter(Color.parseColor("#757575"))

            //Card background color
            try {
                categoryCardLayout.setBackgroundColor(Color.parseColor(category.colorHex))
            } catch (e: Exception) {
                categoryCardLayout.setBackgroundColor(Color.parseColor("#F6F5EC")) // צבע ברירת מחדל אם הקוד לא תקין
            }
            // Dynamic icon loading
            val resourceId =
                context.resources.getIdentifier(category.iconName, "drawable", context.packageName)
            if (resourceId != 0) {
                ivCategoryIcon.setImageResource(resourceId)
            } else {
                ivCategoryIcon.setImageResource(R.drawable.ic_default)
            }
            root.setOnClickListener { onItemClick(category) }
        }
    }

    //Helper function to refresh the adapter data when the user switches months
    fun updateData(newList: List<Category>) {
        this.categories = newList
        notifyDataSetChanged()
    }

    override fun getItemCount() = categories.size
}