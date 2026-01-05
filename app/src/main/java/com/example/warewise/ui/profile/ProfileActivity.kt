package com.example.warewise.ui.profile

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.warewise.BaseActivity
import com.example.warewise.databinding.ActivityProfileBinding
import com.example.warewise.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private var selectedPhotoUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val imageUri: Uri? = data?.data
            if (imageUri != null) {
                try {
                    val contentResolver = applicationContext.contentResolver
                    val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(imageUri, takeFlags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                selectedPhotoUri = imageUri
                binding.ivProfilePhoto.setImageURI(imageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
        
        viewModel.loadUserProfile()
    }

    private fun setupObservers() {
        viewModel.userProfile.observe(this) { profile ->
            profile?.let {
                binding.etFullName.setText(it.fullName)
                binding.etEmployeeId.setText(it.employeeId)
                binding.etCompanyName.setText(it.companyName)
                
                if (!it.profilePhotoUri.isNullOrEmpty()) {
                    try {
                        val uri = Uri.parse(it.profilePhotoUri)
                        binding.ivProfilePhoto.setImageURI(uri)
                        selectedPhotoUri = uri
                    } catch (e: SecurityException) {
                        android.util.Log.e("ProfileActivity", "Permission for URI lost. Consider asking user to re-select.", e)
                        binding.ivProfilePhoto.setImageResource(R.drawable.ic_person)
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileActivity", "Failed to load image URI.", e)
                        binding.ivProfilePhoto.setImageResource(R.drawable.ic_person)
                    }
                } else {
                    binding.ivProfilePhoto.setImageResource(R.drawable.ic_person)
                }
            }
        }

        viewModel.updateStatus.observe(this) { status ->
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.ivProfilePhoto.setOnClickListener {
            val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(android.content.Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        binding.btnSaveChanges.setOnClickListener {
            val fullName = binding.etFullName.text.toString()
            val companyName = binding.etCompanyName.text.toString()
            
            if (fullName.isNotEmpty()) {
                val currentProfile = viewModel.userProfile.value
                if (currentProfile != null && currentProfile.fullName != fullName) {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Confirm Name Change")
                        .setMessage("Are you sure you want to change your name?")
                        .setPositiveButton("Yes") { _, _ ->
                            viewModel.saveProfile(fullName, companyName, selectedPhotoUri?.toString())
                        }
                        .setNegativeButton("No", null)
                        .show()
                } else {
                    viewModel.saveProfile(fullName, companyName, selectedPhotoUri?.toString())
                }
            } else {
                Toast.makeText(this, "Full Name is required", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
