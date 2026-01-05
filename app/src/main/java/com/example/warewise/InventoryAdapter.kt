package com.example.warewise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.warewise.databinding.ItemInventoryBinding

/**
 * InventoryAdapter - Polymorphic RecyclerView Adapter
 * 
 * ACADEMIC REQUIREMENT: Demonstrates Runtime Polymorphism
 * - Holds List<InventoryItem> (abstract base class)
 * - Calls polymorphic methods (getDetails(), getDisplayQuantity()) without type checking
 * - Each subclass (Electronics, Food, Furniture, UnitItem, WeightItem) provides its own implementation
 */
class InventoryAdapter(
    private var items: List<InventoryItem>,
    private val listener: OnItemInteractionListener? = null
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    /**
     * Interface for item interaction callbacks.
     */
    interface OnItemInteractionListener {
        fun onEditClicked(item: InventoryItem)
        fun onDeleteClicked(item: InventoryItem)
    }

    /**
     * ViewHolder with ViewBinding.
     */
    class InventoryViewHolder(val binding: ItemInventoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val binding = ItemInventoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InventoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        val item = items[position]
        
        // Basic info
        holder.binding.tvItemName.text = item.name
        holder.binding.tvItemBarcode.text = "Barcode: ${item.barcode}"
        holder.binding.tvItemLocation.text = "Location: ${item.location}"
        
        // POLYMORPHISM IN ACTION!
        // getDisplayQuantity() returns different content based on subclass:
        // - UnitItem: "10 units"
        // - WeightItem: "5.50 kg"
        // - Electronics: "10 units"
        // - Food: "10 units"
        // - Furniture: "10 units"
        holder.binding.tvItemQuantity.text = item.getDisplayQuantity()

        // PURE POLYMORPHISM: isLowStock() is overridden by each subclass
        // NO type checking (if/when) needed - each class defines its own threshold
        val isLowStock = item.isLowStock()

        // Visual indicator for low stock
        if (isLowStock) {
            holder.binding.tvItemQuantity.setBackgroundResource(R.drawable.bg_badge_red)
        } else {
            holder.binding.tvItemQuantity.setBackgroundResource(R.drawable.bg_badge)
        }

        // Click to expand/collapse action buttons
        holder.itemView.setOnClickListener {
            if (listener != null) {
                holder.binding.layoutButtons.visibility = 
                    if (holder.binding.layoutButtons.visibility == View.VISIBLE) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
            }
        }

        // Edit button - POLYMORPHISM: getDetails() provides type-specific info
        holder.binding.btnEditItem.setOnClickListener {
            listener?.onEditClicked(item)
        }

        // Delete button
        holder.binding.btnDeleteItem.setOnClickListener {
            listener?.onDeleteClicked(item)
        }
    }

    override fun getItemCount(): Int = items.size

    /**
     * Updates the adapter's data.
     * @param newItems List of InventoryItem (can contain any subclass)
     */
    fun updateList(newItems: List<InventoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    /**
     * Filters items by search query.
     */
    fun filter(query: String, allItems: List<InventoryItem>) {
        items = if (query.isEmpty()) {
            allItems
        } else {
            allItems.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.barcode.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }
}
