package com.example.warewise

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.warewise.databinding.ActivityInventoryListBinding

class InventoryListActivity : BaseActivity(), InventoryAdapter.OnItemInteractionListener {

    private lateinit var binding: ActivityInventoryListBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: InventoryAdapter
    private var allItems: List<InventoryItem> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation(binding.bottomNavigation, R.id.nav_inventory)

        dbHelper = DatabaseHelper(this)
        
        binding.rvInventory.layoutManager = LinearLayoutManager(this)
        
        loadItems()

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }



            override fun onQueryTextChange(newText: String?): Boolean {
                filter(newText)
                return true
            }
        })

        setupFilters()
    }

    private fun setupFilters() {
        @Suppress("DEPRECATION")
        binding.chipGroupInfo.setOnCheckedChangeListener { _, _ ->
            // Re-apply filter with current search text
            val currentQuery = binding.searchView.query.toString()
            filter(currentQuery)
        }
    }
    
    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.inventory_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_info -> {
                android.widget.Toast.makeText(this, "This screen shows all inventory items.", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadItems()
        binding.bottomNavigation.selectedItemId = R.id.nav_inventory
    }

    private fun loadItems() {
        allItems = dbHelper.getAllItems()
        if (allItems.isEmpty()) {
            binding.rvInventory.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvInventory.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            adapter = InventoryAdapter(allItems, this)
            binding.rvInventory.adapter = adapter
        }
    }

    private fun filter(text: String?) {
        val query = text?.trim() ?: ""
        
        // 1. Filter by Search Query
        var filteredList = if (query.isEmpty()) {
            allItems
        } else {
            allItems.filter {
                it.name.contains(query, ignoreCase = true) || it.barcode.contains(query)
            }
        }

        // 2. Filter by Chip Selection
        val checkedId = binding.chipGroupInfo.checkedChipId
        filteredList = when (checkedId) {
            binding.chipElectronics.id -> filteredList.filter { it.type == ItemType.ELECTRONICS }
            binding.chipFood.id -> filteredList.filter { it.type == ItemType.FOOD }
            binding.chipFurniture.id -> filteredList.filter { it.type == ItemType.FURNITURE }
            binding.chipLowStock.id -> filteredList.filter { it.isLowStock() }
            else -> filteredList // "All" or nothing selected
        }

        adapter.updateList(filteredList)
    }

    override fun onEditClicked(item: InventoryItem) {
        val intent = Intent(this, AddItemActivity::class.java)
        intent.putExtra("ITEM_ID", item.id)
        startActivity(intent)
    }

    override fun onDeleteClicked(item: InventoryItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete '${item.name}'?")
            .setPositiveButton("Yes") { _, _ ->
                if (dbHelper.deleteItem(item.id)) {
                    dbHelper.addReport("Item with ID: ${item.id} was deleted.")
                    loadItems()
                    android.widget.Toast.makeText(this, "Item deleted", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(this, "Failed to delete item", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
