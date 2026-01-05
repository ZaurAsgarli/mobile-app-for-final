package com.example.warewise

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.warewise.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Session Persistence Check
        val sharedPreferences = getSharedPreferences("WareWisePrefs", MODE_PRIVATE)
        if (sharedPreferences.getBoolean("IS_LOGGED_IN", false)) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 2. Theme Persistence (Immediate check to prevent flicker on login screen)
        val themeMode = sharedPreferences.getInt("THEME_MODE", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(themeMode)

        dbHelper = DatabaseHelper(this)

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                val db = dbHelper.readableDatabase
                val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COL_USERNAME}=? AND ${DatabaseHelper.COL_PASSWORD}=?", arrayOf(username, password))
                
                if (cursor.moveToFirst()) {
                    cursor.close()
                    
                    // Fetch full user profile to get employeeId
                    val userProfile = dbHelper.getUserProfile(username)
                    if (userProfile != null) {
                        val editor = sharedPreferences.edit()
                        editor.putString("USERNAME", userProfile.username)
                        editor.putString("EMPLOYEE_ID", userProfile.employeeId)
                        editor.putBoolean("IS_LOGGED_IN", true) // Set logged in flag
                        editor.apply()
                    }

                    // Clear back stack so user can't go back to Login
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    cursor.close()
                    Toast.makeText(this, getString(R.string.login_failed), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
