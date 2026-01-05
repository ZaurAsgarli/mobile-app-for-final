package com.example.warewise.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warewise.DatabaseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for login screen.
 * Handles authentication and validation logic.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val dbHelper: DatabaseHelper,
    private val sharedPreferences: android.content.SharedPreferences
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    private val _validationErrors = MutableLiveData<ValidationErrors>()
    val validationErrors: LiveData<ValidationErrors> = _validationErrors

    /**
     * Sealed class representing login result states.
     */
    sealed class LoginState {
        object Loading : LoginState()
        data class Success(val username: String) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    /**
     * Data class representing validation errors.
     */
    data class ValidationErrors(
        val usernameError: String? = null,
        val passwordError: String? = null
    )

    /**
     * Check if user is already logged in.
     */
    fun checkLoginStatus(): Boolean {
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }

    /**
     * Validate input and perform login.
     */
    fun login(username: String, password: String) {
        // Validate input first
        val validationResult = validateInput(username, password)
        if (!validationResult.isValid) {
            _validationErrors.value = validationResult.errors
            return
        }

        // Clear validation errors
        _validationErrors.value = ValidationErrors()

        // Attempt authentication
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val isAuthenticated = dbHelper.authenticateUser(username, password)
            
            if (isAuthenticated) {
                sharedPreferences.edit().apply {
                    putBoolean("isLoggedIn", true)
                    putString("username", username)
                    apply()
                }
                _loginState.value = LoginState.Success(username)
            } else {
                _loginState.value = LoginState.Error("Invalid username or password.")
            }
        }
    }

    /**
     * Validate input fields.
     */
    private fun validateInput(username: String, password: String): ValidationResult {
        val errors = ValidationErrors()

        if (username.isEmpty()) {
            return ValidationResult(
                isValid = false,
                errors = errors.copy(usernameError = "Username is required.")
            )
        }

        if (password.isEmpty()) {
            return ValidationResult(
                isValid = false,
                errors = errors.copy(passwordError = "Password is required.")
            )
        }

        return ValidationResult(isValid = true, errors = errors)
    }

    private data class ValidationResult(
        val isValid: Boolean,
        val errors: ValidationErrors
    )
}

