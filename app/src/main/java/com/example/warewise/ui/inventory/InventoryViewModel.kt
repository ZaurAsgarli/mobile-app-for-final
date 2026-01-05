package com.example.warewise.ui.inventory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warewise.DatabaseHelper
import com.example.warewise.InventoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for inventory list screen.
 * Manages item list, search, and empty state.
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val dbHelper: DatabaseHelper
) : ViewModel() {

    private val _items = MutableLiveData<List<InventoryItem>>()
    val items: LiveData<List<InventoryItem>> = _items

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    init {
        loadAllItems()
    }

    /**
     * Load all items from database.
     */
    fun loadAllItems() {
        viewModelScope.launch {
            val itemsList = dbHelper.getAllItems()
            _items.value = itemsList
            _isEmpty.value = itemsList.isEmpty()
        }
    }

    /**
     * Search items by query string.
     */
    fun searchItems(query: String) {
        viewModelScope.launch {
            val itemsList = if (query.isBlank()) {
                dbHelper.getAllItems()
            } else {
                dbHelper.searchItems(query)
            }
            _items.value = itemsList
            _isEmpty.value = itemsList.isEmpty()
        }
    }
}

