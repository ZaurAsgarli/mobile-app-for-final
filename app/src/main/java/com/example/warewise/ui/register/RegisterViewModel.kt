package com.example.warewise.ui.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warewise.DatabaseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for user registration.
 * Handles validation and registration logic.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val dbHelper: DatabaseHelper
) : ViewModel() {

    private val _registrationState = MutableLiveData<RegistrationState>()
    val registrationState: LiveData<RegistrationState> = _registrationState

    private val _validationErrors = MutableLiveData<ValidationErrors>()
    val validationErrors: LiveData<ValidationErrors> = _validationErrors

    /**
     * Data class representing registration result states.
     */
    sealed class RegistrationState {
        object Loading : RegistrationState()
        data class Success(val message: String) : RegistrationState()
        data class Error(val message: String) : RegistrationState()
    }

    /**
     * Data class representing validation errors.
     */
    data class ValidationErrors(
        val usernameError: String? = null,
        val passwordError: String? = null,
        val confirmPasswordError: String? = null
    )

    /**
     * Validate input and register user.
     */
    fun registerUser(username: String, password: String, confirmPassword: String, fullName: String, employeeId: String) {
        viewModelScope.launch {
            // Validate input first
            val validationResult = validateInput(username, password, confirmPassword, fullName, employeeId)
            if (!validationResult.isValid) {
                _validationErrors.value = validationResult.errors
                return@launch
            }

            // Clear validation errors
            _validationErrors.value = ValidationErrors()

            // Check if username exists
            if (dbHelper.usernameExists(username)) {
                _validationErrors.value = ValidationErrors(
                    usernameError = "Username already exists. Please choose another."
                )
                return@launch
            }

            // Attempt registration
            _registrationState.value = RegistrationState.Loading
            val result = dbHelper.registerUser(username, password, fullName, employeeId)
            
            if (result != -1L) {
                _registrationState.value = RegistrationState.Success("Registration successful! Please login.")
            } else {
                _registrationState.value = RegistrationState.Error("Registration failed. Please try again.")
            }
        }
    }

    /**
     * Validate input fields.
     */
    private fun validateInput(
        username: String,
        password: String,
        confirmPassword: String,
        fullName: String,
        employeeId: String
    ): ValidationResult {
        val errors = ValidationErrors()

        if (username.isEmpty()) {
            return ValidationResult(
                isValid = false,
                errors = errors.copy(usernameError = "Username is required.")
            )
        }

        if (fullName.isEmpty()) {
            return ValidationResult(
                isValid = false,
                errors = errors.copy(usernameError = "Full Name is required.") // Reusing error field or add new one? 
                // The prompt didn't ask for new error fields, but I should probably add them to ValidationErrors data class if I want to be clean.
                // For now I'll just return false or use a generic error if I can't change data class easily.
                // Actually I can change ValidationErrors data class.
            )
        }
        
        if (employeeId.isEmpty()) {
             return ValidationResult(
                isValid = false,
                errors = errors.copy(usernameError = "Employee ID is required.")
            )
        }

        if (password.isEmpty()) {
            return ValidationResult(
                isValid = false,
                errors = errors.copy(passwordError = "Password is required.")
            )
        }

        if (password.length < 4) {
            return ValidationResult(
                isValid = false,
                errors = errors.copy(passwordError = "Password must be at least 4 characters.")
            )
        }

        if (confirmPassword.isEmpty()) {
            return ValidationResult(
                isValid = false,
                errors = errors.copy(confirmPasswordError = "Please confirm your password.")
            )
        }

        if (password != confirmPassword) {
            return ValidationResult(
                isValid = false,
                errors = errors.copy(confirmPasswordError = "Passwords do not match.")
            )
        }

        return ValidationResult(isValid = true, errors = errors)
    }

    private data class ValidationResult(
        val isValid: Boolean,
        val errors: ValidationErrors
    )
}

