package com.example.warewise.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warewise.DatabaseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for settings screen.
 * Handles logout, data clearing, and other settings operations.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dbHelper: DatabaseHelper,
    private val sharedPreferences: android.content.SharedPreferences
) : ViewModel() {

    private val _events = MutableLiveData<SettingsEvent>()
    val events: LiveData<SettingsEvent> = _events

    /**
     * Sealed class representing single-fire events for navigation and UI updates.
     */
    sealed class SettingsEvent {
        object NavigateToLogin : SettingsEvent()
        data class ShowToast(val message: String) : SettingsEvent()
    }

    /**
     * Clear all data from database and re-seed.
     */
    fun clearAllData() {
        viewModelScope.launch {
            dbHelper.clearAllData()
            _events.value = SettingsEvent.ShowToast("All data has been cleared.")
        }
    }

    /**
     * Perform logout by clearing SharedPreferences.
     */
    fun performLogout() {
        sharedPreferences.edit().clear().apply()
        _events.value = SettingsEvent.NavigateToLogin
    }

    /**
     * Get current dark mode preference.
     */
    fun isDarkModeEnabled(): Boolean {
        return sharedPreferences.getBoolean("dark_mode", false)
    }

    /**
     * Update dark mode preference.
     */
    fun onThemeChanged(isDarkMode: Boolean) {
        sharedPreferences.edit().putBoolean("dark_mode", isDarkMode).apply()
    }
    /**
     * Change user password.
     */
    fun changePassword(oldPass: String, newPass: String, confirmPass: String) {
        viewModelScope.launch {
            if (newPass != confirmPass) {
                _events.value = SettingsEvent.ShowToast("New passwords do not match.")
                return@launch
            }

            if (newPass.length < 4) {
                _events.value = SettingsEvent.ShowToast("Password must be at least 4 characters.")
                return@launch
            }

            val username = sharedPreferences.getString("USERNAME", "") ?: ""
            if (username.isNotEmpty()) {
                // Verify old password
                if (dbHelper.authenticateUser(username, oldPass)) {
                    val success = dbHelper.updatePassword(username, newPass)
                    if (success) {
                        _events.value = SettingsEvent.ShowToast("Password changed successfully.")
                    } else {
                        _events.value = SettingsEvent.ShowToast("Failed to update password.")
                    }
                } else {
                    _events.value = SettingsEvent.ShowToast("Incorrect old password.")
                }
            } else {
                _events.value = SettingsEvent.ShowToast("User session invalid.")
            }
        }
    }
}

