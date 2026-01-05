package com.example.warewise.ui.reports

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.warewise.BaseActivity
import com.example.warewise.InventoryAdapter
import com.example.warewise.databinding.ActivityReportsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReportsActivity : BaseActivity() {

    private lateinit var binding: ActivityReportsBinding
    private val viewModel: ReportsViewModel by viewModels()
    private lateinit var reportsAdapter: ReportsAdapter
    private lateinit var recentItemsAdapter: InventoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupObservers()
        
        viewModel.loadReports()
    }

    private fun setupRecyclerViews() {
        reportsAdapter = ReportsAdapter(emptyList())
        binding.rvReports.layoutManager = LinearLayoutManager(this)
        binding.rvReports.adapter = reportsAdapter

        recentItemsAdapter = InventoryAdapter(emptyList()) // No listener needed
        binding.rvRecentItems.layoutManager = LinearLayoutManager(this)
        binding.rvRecentItems.adapter = recentItemsAdapter
    }

    private fun setupObservers() {
        viewModel.reports.observe(this) { reportList ->
            if (reportList.isEmpty()) {
                binding.rvReports.visibility = View.GONE
                binding.tvEmptyReports.visibility = View.VISIBLE
            } else {
                binding.rvReports.visibility = View.VISIBLE
                binding.tvEmptyReports.visibility = View.GONE
                reportsAdapter.updateList(reportList)
            }
        }

        viewModel.recentItems.observe(this) { itemList ->
            recentItemsAdapter.updateList(itemList)
        }
    }
}
