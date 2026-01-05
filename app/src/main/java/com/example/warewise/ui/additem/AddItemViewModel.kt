package com.example.warewise.ui.additem

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warewise.DatabaseHelper
import com.example.warewise.InventoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val dbHelper: DatabaseHelper
) : ViewModel() {

    private val _item = MutableLiveData<InventoryItem?>()
    val item: LiveData<InventoryItem?> = _item

    private val _saveStatus = MutableLiveData<Boolean>()
    val saveStatus: LiveData<Boolean> = _saveStatus

    fun loadItem(itemId: Int) {
        viewModelScope.launch {
            val loadedItem = dbHelper.getItemById(itemId)
            _item.value = loadedItem
        }
    }

    fun saveNewItem(item: InventoryItem) {
        viewModelScope.launch {
            val result = dbHelper.addItem(item)
            _saveStatus.value = result != -1L
        }
    }

    fun updateItem(item: InventoryItem) {
        viewModelScope.launch {
            val success = dbHelper.updateItem(item)
            _saveStatus.value = success
        }
    }
}
