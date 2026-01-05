package com.example.warewise.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warewise.DatabaseHelper
import com.example.warewise.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val dbHelper: DatabaseHelper,
    private val sharedPreferences: android.content.SharedPreferences
) : ViewModel() {

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _updateStatus = MutableLiveData<String>()
    val updateStatus: LiveData<String> = _updateStatus

    fun loadUserProfile() {
        viewModelScope.launch {
            val username = sharedPreferences.getString("USERNAME", "") ?: ""
            if (username.isNotEmpty()) {
                val profile = dbHelper.getUserProfile(username)
                _userProfile.value = profile
            }
        }
    }

    fun saveProfile(fullName: String, companyName: String, profilePhotoUri: String?) {
        viewModelScope.launch {
            val username = sharedPreferences.getString("USERNAME", "") ?: ""
            val employeeId = sharedPreferences.getString("EMPLOYEE_ID", "") ?: ""
            
            if (username.isNotEmpty() && employeeId.isNotEmpty()) {
                val user = UserProfile(
                    username = username,
                    fullName = fullName,
                    employeeId = employeeId,
                    companyName = companyName,
                    profilePhotoUri = profilePhotoUri
                )
                
                val success = dbHelper.updateUser(user)
                if (success) {
                    dbHelper.addReport("User profile updated.")
                    _updateStatus.value = "Profile updated successfully"
                    loadUserProfile() // Reload to refresh UI
                } else {
                    _updateStatus.value = "Failed to update profile"
                }
            } else {
                _updateStatus.value = "Error: User session invalid"
            }
        }
    }
}
