package com.example.warewise.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warewise.DatabaseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for dashboard screen.
 * Manages dashboard data including totals, low stock alerts, and recent activity.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dbHelper: DatabaseHelper,
    private val sharedPreferences: android.content.SharedPreferences
) : ViewModel() {

    private val _uiState = MutableLiveData<DashboardUiState>()
    val uiState: LiveData<DashboardUiState> = _uiState

    /**
     * Data class representing the complete dashboard UI state.
     */
    data class DashboardUiState(
        val greeting: String = "",
        val totalStockValue: String = "$0.00",
        val lowStockCount: Int = 0,
        val totalItemsCount: Int = 0,
        val recentActivity: String = "No recent activity",
        val recentItemsList: List<com.example.warewise.InventoryItem> = emptyList()
    )

    init {
        loadDashboardData()
    }

    /**
     * Load all dashboard data.
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            val username = sharedPreferences.getString("USERNAME", "User") ?: "User"
            val fullName = dbHelper.getUserFullName(username) ?: username
            val greeting = getGreeting()
            
            val totalItems = dbHelper.getTotalItemsCount()
            val totalValue = dbHelper.getTotalValue()
            val lowStockCount = dbHelper.getLowStockItemsCount(5)
            
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
            val recentItems = dbHelper.getRecentItems()
            val recentActivity = getRecentActivity()

            _uiState.value = DashboardUiState(
                greeting = "$greeting, $fullName",
                totalStockValue = currencyFormat.format(totalValue.toDouble()),
                lowStockCount = lowStockCount,
                totalItemsCount = totalItems,
                recentActivity = recentActivity,
                recentItemsList = recentItems
            )
        }
    }

    /**
     * Get time-based greeting message.
     */
    /**
     * Get time-based greeting message.
     */
    private fun getGreeting(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            in 18..21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    /**
     * Get recent activity message from SharedPreferences.
     */
    private fun getRecentActivity(): String {
        val lastScanTime = sharedPreferences.getLong("last_scan_time", 0)
        if (lastScanTime > 0) {
            val timeDiff = System.currentTimeMillis() - lastScanTime
            val minutesAgo = (timeDiff / 60000).toInt()
            return "Last Scan: $minutesAgo mins ago"
        }
        return "No recent activity"
    }
}

