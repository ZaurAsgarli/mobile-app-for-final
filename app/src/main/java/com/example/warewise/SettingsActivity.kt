package com.example.warewise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.viewModels
import com.example.warewise.databinding.ActivitySettingsBinding
import com.example.warewise.ui.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var dbBackupHelper: DbBackupHelper

    private val exportLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val success = dbBackupHelper.exportData(uri)
            if (success) {
                android.widget.Toast.makeText(this, "Data exported successfully", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(this, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val importLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val success = dbBackupHelper.importData(uri)
            if (success) {
                android.widget.Toast.makeText(this, "Data imported successfully", android.widget.Toast.LENGTH_SHORT).show()
                // Ideally refresh data if we were viewing it, but here we are in Settings.
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Import Failed")
                    .setMessage("Invalid File Format or Corrupt Data.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation(binding.bottomNavigation, R.id.nav_settings)
        
        dbBackupHelper = DbBackupHelper(this)
        
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.events.observe(this) { event ->
            when (event) {
                is SettingsViewModel.SettingsEvent.NavigateToLogin -> {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                is SettingsViewModel.SettingsEvent.ShowToast -> {
                    android.widget.Toast.makeText(this, event.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupListeners() {
        // ADVANCED THEME SETTINGS: Show dialog on row click
        binding.layoutTheme.setOnClickListener {
            showThemeSelectionDialog()
        }

        binding.btnExportData.setOnClickListener {
            // Suggest a filename
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
            exportLauncher.launch("warewise_backup_$timestamp.json")
        }

        binding.btnImportData.setOnClickListener {
            importLauncher.launch("application/json")
        }

        binding.btnFactoryReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.factory_reset))
                .setMessage(getString(R.string.confirm_reset))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    viewModel.clearAllData()
                    // Restart app
                    val intent = Intent(this, SplashActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton(getString(R.string.no), null)
                .show()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnLogout.setOnClickListener {
            viewModel.performLogout()
        }
    }
    
    /**
     * Shows a dialog to select Light, Dark, or System Default theme.
     * Persists the choice and updates UI.
     */
    private fun showThemeSelectionDialog() {
        val modes = arrayOf(
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_system)
        )
        
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val checkedItem = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> 0
            AppCompatDelegate.MODE_NIGHT_YES -> 1
            else -> 2
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_theme))
            .setSingleChoiceItems(modes, checkedItem) { dialog, which ->
                val mode = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_NO
                    1 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                
                // 1. Save Preference
                getSharedPreferences(WareWiseApplication.PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(WareWiseApplication.KEY_THEME, mode)
                    .apply()

                // 2. Apply Theme
                AppCompatDelegate.setDefaultNightMode(mode)
                
                // 3. Update Text
                updateThemeValueText(mode)
                
                // 4. Update VM if needed (optional)
                viewModel.onThemeChanged(mode == AppCompatDelegate.MODE_NIGHT_YES)
                
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateThemeValueText(mode: Int) {
        val text = when (mode) {
            AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.theme_light)
            AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.theme_dark)
            else -> getString(R.string.theme_system)
        }
        binding.tvThemeValue.text = text
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etOldPass = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOldPassword)
        val etNewPass = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNewPassword)
        val etConfirmPass = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirmNewPassword)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val oldPass = etOldPass.text.toString()
                val newPass = etNewPass.text.toString()
                val confirmPass = etConfirmPass.text.toString()
                viewModel.changePassword(oldPass, newPass, confirmPass)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_settings
        // Update theme text on resume
        updateThemeValueText(AppCompatDelegate.getDefaultNightMode())
    }
}
