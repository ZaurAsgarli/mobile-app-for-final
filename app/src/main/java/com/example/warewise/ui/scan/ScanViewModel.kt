package com.example.warewise.ui.scan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warewise.DatabaseHelper
import com.example.warewise.InventoryItem
import com.example.warewise.UnitItem
import com.example.warewise.WeightItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for scan screen.
 * Handles barcode processing, item lookup, and quantity updates.
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val dbHelper: DatabaseHelper,
    private val sharedPreferences: android.content.SharedPreferences
) : ViewModel() {

    private val _scanEvents = MutableLiveData<ScanEvent>()
    val scanEvents: LiveData<ScanEvent> = _scanEvents

    /**
     * Sealed class representing scan UI events.
     */
    sealed class ScanEvent {
        data class ItemFound(val item: InventoryItem, val barcode: String) : ScanEvent()
        data class ItemNotFound(val barcode: String) : ScanEvent()
        data class StockUpdated(val message: String) : ScanEvent()
        data class UpdateFailed(val message: String) : ScanEvent()
    }

    /**
     * Process scanned barcode.
     */
    fun processBarcode(barcode: String) {
        // Save last scan time for dashboard recent activity
        sharedPreferences.edit().putLong("last_scan_time", System.currentTimeMillis()).apply()

        viewModelScope.launch {
            val item = dbHelper.getItemByBarcode(barcode)
            if (item != null) {
                _scanEvents.value = ScanEvent.ItemFound(item, barcode)
            } else {
                _scanEvents.value = ScanEvent.ItemNotFound(barcode)
            }
        }
    }

    /**
     * Update item quantity.
     */
    fun updateItemQuantity(itemId: Long, newQuantity: Double) {
        viewModelScope.launch {
            val success = dbHelper.updateItemQuantity(itemId, newQuantity)
            if (success) {
                _scanEvents.value = ScanEvent.StockUpdated("Stock updated successfully")
            } else {
                _scanEvents.value = ScanEvent.UpdateFailed("Failed to update stock")
            }
        }
    }

    /**
     * Get quantity increment/decrement value based on item type.
     */
    fun getQuantityStep(item: InventoryItem): Double {
        return when (item) {
            is UnitItem -> 1.0
            is WeightItem -> 0.5
            else -> 1.0
        }
    }

    /**
     * Format quantity display string based on item type.
     */
    fun formatQuantity(quantity: Double, item: InventoryItem): String {
        return when (item) {
            is UnitItem -> "${quantity.toInt()} pcs"
            is WeightItem -> String.format("%.2f kg", quantity)
            else -> item.getDisplayQuantity()
        }
    }
}

