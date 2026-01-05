package com.example.warewise

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Suppress("DEPRECATION")
    protected fun setupBottomNavigation(bottomNavigationView: BottomNavigationView, selectedItemId: Int) {
        bottomNavigationView.selectedItemId = selectedItemId
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (selectedItemId != R.id.nav_home) {
                        startActivity(Intent(this, DashboardActivity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }
                R.id.nav_inventory -> {
                    if (selectedItemId != R.id.nav_inventory) {
                        startActivity(Intent(this, InventoryListActivity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }
                R.id.nav_settings -> {
                    if (selectedItemId != R.id.nav_settings) {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
    }
}
