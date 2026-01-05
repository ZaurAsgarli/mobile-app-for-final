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
class AddItemActivity : AppCompatActivity() {

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
                
                if (item.getType() == "UNIT") {
                    binding.rbUnit.isChecked = true
                    binding.etItemQuantity.setText((item as UnitItem).quantity.toString())
                } else {
                    binding.rbWeight.isChecked = true
                    binding.etItemQuantity.setText((item as WeightItem).weight.toString())
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

        val item: InventoryItem = if (isUnit) {
            val quantity = quantityStr.toIntOrNull() ?: 0
            UnitItem(if (isEditMode) editingItemId else 0, name, barcode, description, supplier, location, costPrice, quantity)
        } else {
            val weight = quantityStr.toDoubleOrNull() ?: 0.0
            WeightItem(if (isEditMode) editingItemId else 0, name, barcode, description, supplier, location, costPrice, weight)
        }

        if (isEditMode) {
            viewModel.updateItem(item)
        } else {
            viewModel.saveNewItem(item)
        }
    }
}
