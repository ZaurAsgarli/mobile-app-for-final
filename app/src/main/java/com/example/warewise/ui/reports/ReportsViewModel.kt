package com.example.warewise.ui.reports

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warewise.DatabaseHelper
import com.example.warewise.InventoryItem
import com.example.warewise.ReportItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val dbHelper: DatabaseHelper
) : ViewModel() {

    private val _reports = MutableLiveData<List<ReportItem>>()
    val reports: LiveData<List<ReportItem>> = _reports

    private val _recentItems = MutableLiveData<List<InventoryItem>>()
    val recentItems: LiveData<List<InventoryItem>> = _recentItems

    fun loadReports() {
        viewModelScope.launch {
            val reportList = dbHelper.getAllReports()
            _reports.value = reportList
            
            val items = dbHelper.getRecentItems()
            _recentItems.value = items
        }
    }
}
