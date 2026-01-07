package com.example.warewise

import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.warewise.databinding.ActivityAddItemBinding
import com.example.warewise.ui.additem.AddItemViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddItemActivity : BaseActivity() {

    private lateinit var binding: ActivityAddItemBinding
    private val viewModel: AddItemViewModel by viewModels()
    private var isEditMode = false
    private var editingItemId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add New Item"

        setupSpinner()
        setupObservers()
        setupListeners()
        
        // Check for Edit Mode
        if (intent.hasExtra("ITEM_ID")) {
            isEditMode = true
            editingItemId = intent.getIntExtra("ITEM_ID", -1)
            supportActionBar?.title = "Edit Item"
            binding.btnSaveItem.text = "Update Item"
            viewModel.loadItem(editingItemId)
        } else {
            // Check if barcode passed from ScanActivity (only for new items)
            val scannedBarcode = intent.getStringExtra("BARCODE")
            if (scannedBarcode != null) {
                binding.etItemBarcode.setText(scannedBarcode)
            }
        }
        }


    private fun setupObservers() {
        viewModel.item.observe(this) { item ->
            if (item != null) {
                binding.etItemName.setText(item.name)
                binding.etItemBarcode.setText(item.barcode)
                binding.etItemDescription.setText(item.description)
                binding.etItemSupplier.setText(item.supplier)
                binding.etItemLocation.setText(item.location)
                binding.etItemCostPrice.setText(item.costPrice.toString())
                

                // Handle Item Type and Extra Info
                // Handle Item Type and Extra Info
                val type = item.type // Now getting Enum
                // val spinnerAdapter = binding.spItemType.adapter as android.widget.ArrayAdapter<String> // Unused
                val typeArray = arrayOf("Standard", "Electronics", "Food", "Furniture") // Must match setupSpinner
                
                // Map internal type to spinner display
                var spinnerIndex = when (type) {
                    ItemType.ELECTRONICS -> typeArray.indexOf("Electronics")
                    ItemType.FOOD -> typeArray.indexOf("Food")
                    ItemType.FURNITURE -> typeArray.indexOf("Furniture")
                    ItemType.UNIT, ItemType.WEIGHT -> typeArray.indexOf("Standard")
                    // No else needed if exhaustive or default to 0
                }
                
                if (spinnerIndex < 0) spinnerIndex = 0
                
                if (spinnerIndex >= 0) {
                    binding.spItemType.setSelection(spinnerIndex)
                }

                // Populate Quantity/Weight and Extra Info
                when (item) {
                    is UnitItem -> {
                        binding.rbUnit.isChecked = true
                        binding.etItemQuantity.setText(item.quantity.toString())
                    }
                    is WeightItem -> {
                        binding.rbWeight.isChecked = true
                        binding.etItemQuantity.setText(item.weight.toString())
                    }
                    is Electronics -> {
                        binding.rbUnit.isChecked = true
                        binding.etItemQuantity.setText(item.quantity.toString())
                        binding.etExtraInfo.setText(item.warranty)
                    }
                    is Food -> {
                        binding.rbUnit.isChecked = true
                        binding.etItemQuantity.setText(item.quantity.toString())
                        binding.etExtraInfo.setText(item.expiryDate)
                    }
                    is Furniture -> {
                        binding.rbUnit.isChecked = true
                        binding.etItemQuantity.setText(item.quantity.toString())
                        binding.etExtraInfo.setText(item.material)
                    }
                }
            }
        }

        viewModel.saveStatus.observe(this) { success ->
            if (success) {
                Toast.makeText(this, if (isEditMode) "Item updated successfully" else getString(R.string.item_added), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, if (isEditMode) "Failed to update item" else "Error adding item", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        binding.rgType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbUnit) {
                binding.tilQuantity.hint = getString(R.string.quantity)
                binding.etItemQuantity.inputType = InputType.TYPE_CLASS_NUMBER
            } else {
                binding.tilQuantity.hint = getString(R.string.weight)
                binding.etItemQuantity.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
        }

        binding.btnSaveItem.setOnClickListener {
            saveItem()
        }
    }

    private fun setupSpinner() {
        val itemTypes = arrayOf("Standard", "Electronics", "Food", "Furniture")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, itemTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spItemType.adapter = adapter

        binding.spItemType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedType = itemTypes[position]
                updateUIForType(selectedType)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun updateUIForType(type: String) {
        when (type) {
            "Standard" -> {
                binding.tilExtraInfo.visibility = android.view.View.GONE
                binding.rbUnit.isEnabled = true
                binding.rbWeight.isEnabled = true
            }
            "Electronics" -> {
                binding.tilExtraInfo.visibility = android.view.View.VISIBLE
                binding.tilExtraInfo.hint = "Enter Warranty (e.g., 2 Years)"
                binding.rbUnit.isChecked = true
                binding.rbUnit.isEnabled = false
                binding.rbWeight.isEnabled = false
            }
            "Food" -> {
                binding.tilExtraInfo.visibility = android.view.View.VISIBLE
                binding.tilExtraInfo.hint = "Enter Expiry Date"
                binding.rbUnit.isChecked = true
                binding.rbUnit.isEnabled = false
                binding.rbWeight.isEnabled = false
            }
            "Furniture" -> {
                binding.tilExtraInfo.visibility = android.view.View.VISIBLE
                binding.tilExtraInfo.hint = "Enter Material"
                binding.rbUnit.isChecked = true
                binding.rbUnit.isEnabled = false
                binding.rbWeight.isEnabled = false
            }
        }
    }

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val barcode = result.data?.getStringExtra("SCANNED_BARCODE")
            binding.etItemBarcode.setText(barcode)
        }
    }

    private fun saveItem() {
        val name = binding.etItemName.text.toString()
        val barcode = binding.etItemBarcode.text.toString()
        val description = binding.etItemDescription.text.toString()
        val costPriceStr = binding.etItemCostPrice.text.toString()
        val location = binding.etItemLocation.text.toString()
        val supplier = binding.etItemSupplier.text.toString()
        val quantityStr = binding.etItemQuantity.text.toString()

        if (name.isEmpty() || barcode.isEmpty() || costPriceStr.isEmpty() || quantityStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        val costPrice = costPriceStr.toDoubleOrNull() ?: 0.0
        val isUnit = binding.rbUnit.isChecked
        val selectedType = binding.spItemType.selectedItem.toString()
        val extraInfo = binding.etExtraInfo.text.toString()

        val item: InventoryItem = when (selectedType) {
            "Electronics" -> {
                val quantity = quantityStr.toIntOrNull() ?: 0
                Electronics(if (isEditMode) editingItemId else 0, name, barcode, description, supplier, location, costPrice, quantity, extraInfo)
            }
            "Food" -> {
                val quantity = quantityStr.toIntOrNull() ?: 0
                Food(if (isEditMode) editingItemId else 0, name, barcode, description, supplier, location, costPrice, quantity, extraInfo)
            }
            "Furniture" -> {
                val quantity = quantityStr.toIntOrNull() ?: 0
                Furniture(if (isEditMode) editingItemId else 0, name, barcode, description, supplier, location, costPrice, quantity, extraInfo)
            }
            "Standard", "UNIT" -> { // Handle Standard or fallback
                 if (isUnit) {
                    val quantity = quantityStr.toIntOrNull() ?: 0
                    UnitItem(if (isEditMode) editingItemId else 0, name, barcode, description, supplier, location, costPrice, quantity)
                } else {
                    val weight = quantityStr.toDoubleOrNull() ?: 0.0
                    WeightItem(if (isEditMode) editingItemId else 0, name, barcode, description, supplier, location, costPrice, weight)
                }
            }
            else -> {
                // Default fallback
                 if (isUnit) {
                    val quantity = quantityStr.toIntOrNull() ?: 0
                    UnitItem(if (isEditMode) editingItemId else 0, name, barcode, description, supplier, location, costPrice, quantity)
                } else {
                    val weight = quantityStr.toDoubleOrNull() ?: 0.0
                    WeightItem(if (isEditMode) editingItemId else 0, name, barcode, description, supplier, location, costPrice, weight)
                }
            }
        }

        if (isEditMode) {
            viewModel.updateItem(item)
        } else {
            viewModel.saveNewItem(item)
        }
    }
}
