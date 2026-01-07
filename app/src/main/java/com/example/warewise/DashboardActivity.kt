package com.example.warewise

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.warewise.databinding.ActivityDashboardBinding
import com.example.warewise.ui.dashboard.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Theme handled by BaseActivity
        
        setupBottomNavigation(binding.bottomNavigation, R.id.nav_home)

        setupObservers()
        setupListeners()
        setupRecyclerView()
        
        viewModel.loadDashboardData()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDashboardData()
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    private lateinit var adapter: InventoryAdapter

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            binding.tvHeader.text = state.greeting
            binding.tvTotalValue.text = state.totalStockValue
            binding.tvTotalItems.text = state.totalItemsCount.toString()
            
            if (::adapter.isInitialized) {
                adapter.updateList(state.recentItemsList)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = InventoryAdapter(emptyList())
        binding.rvRecentActivities.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvRecentActivities.adapter = adapter
    }

    private fun setupListeners() {
        binding.cvQuickScan.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }

        binding.cvAddItem.setOnClickListener {
            startActivity(Intent(this, AddItemActivity::class.java))
        }
        
        binding.cvProfile.setOnClickListener {
            startActivity(Intent(this, com.example.warewise.ui.profile.ProfileActivity::class.java))
        }
        
        binding.cvReports.setOnClickListener {
            startActivity(Intent(this, com.example.warewise.ui.reports.ReportsActivity::class.java))
        }
    }
}
