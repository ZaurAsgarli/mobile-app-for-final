package com.example.warewise

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.warewise.databinding.ActivityScanBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

/**
 * ScanActivity - Real QR Code Scanner using ZXing Library
 * 
 * Features:
 * - Real QR/Barcode scanning via ZXing Android Embedded
 * - Polymorphic item creation from QR data (Electronics, Food, Furniture)
 * - Fallback barcode lookup for existing items
 * - Robust error handling with try-catch
 */
class ScanActivity : BaseActivity() {

    companion object {
        private const val TAG = "ScanActivity"
    }

    private lateinit var binding: ActivityScanBinding
    private lateinit var dbHelper: DatabaseHelper

    // Modern ActivityResultLauncher for ZXing QR scanning
    private val scannerLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        handleScanResult(result)
    }

    // Permission launcher for camera
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchQRScanner()
        } else {
            Toast.makeText(this, "Camera permission is required for QR scanning", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        binding.btnScan.setOnClickListener {
            checkCameraPermissionAndScan()
        }
    }

    /**
     * Checks camera permission and launches scanner if granted.
     */
    private fun checkCameraPermissionAndScan() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchQRScanner()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(this, "Camera permission is needed to scan QR codes", Toast.LENGTH_SHORT).show()
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Launches the ZXing QR code scanner.
     */
    private fun launchQRScanner() {
        try {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                setPrompt("Scan QR Code or Barcode\n\nQR Format: Type|Name|Price|Qty|ExtraInfo")
                setCameraId(0)  // Use back camera
                setBeepEnabled(true)
                setBarcodeImageEnabled(false)
                setOrientationLocked(true)
            }
            scannerLauncher.launch(options)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching scanner: ${e.message}", e)
            Toast.makeText(this, "Error: Could not open scanner", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handles the result from QR scanner.
     * Supports two formats:
     * 1. QR Code with polymorphic data: Type|Name|Price|Qty|ExtraInfo
     * 2. Simple barcode: looks up existing item in database
     */
    private fun handleScanResult(result: ScanIntentResult) {
        if (result.contents == null) {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
            return
        }

        val scannedData = result.contents
        Log.d(TAG, "Scanned data: $scannedData")

        try {
            // First, try to parse as polymorphic QR code (Type|Name|Price|Qty|ExtraInfo)
            val polymorphicItem = InventoryItemFactory.fromQRCode(scannedData)
            
            if (polymorphicItem != null) {
                // It's a polymorphic QR code - save directly
                handlePolymorphicItem(polymorphicItem)
            } else {
                // It's a regular barcode - look up in database
                handleBarcodeItem(scannedData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing scan result: ${e.message}", e)
            Toast.makeText(this, "Error: Failed to process scan", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handles polymorphic items created from QR code data.
     */
    private fun handlePolymorphicItem(item: InventoryItem) {
        try {
            val id = dbHelper.addItem(item)
            
            if (id != -1L) {
                showSuccessBottomSheet(item)
            } else {
                Toast.makeText(this, "Database error: Could not save item", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving polymorphic item: ${e.message}", e)
            Toast.makeText(this, "Database error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shows success bottom sheet for newly added polymorphic item.
     */
    private fun showSuccessBottomSheet(item: InventoryItem) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_scan_result, null)
        bottomSheetDialog.setContentView(view)

        val tvTitle = view.findViewById<TextView>(R.id.tvScanResultTitle)
        val tvDetails = view.findViewById<TextView>(R.id.tvScanResultDetails)
        val btnAction = view.findViewById<MaterialButton>(R.id.btnScanAction)

        tvTitle.text = "✅ Item Added!"
        // POLYMORPHISM: getDetails() returns different content based on subclass
        tvDetails.text = "${item.name}\n\n${item.getDetails()}"
        btnAction.text = "Done"
        btnAction.setOnClickListener {
            bottomSheetDialog.dismiss()
            finish()
        }

        bottomSheetDialog.show()
    }

    /**
     * Handles regular barcode lookup in database.
     */
    private fun handleBarcodeItem(barcode: String) {
        try {
            val item = dbHelper.getItemByBarcode(barcode)
            showBarcodeResultBottomSheet(item, barcode)
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up barcode: ${e.message}", e)
            Toast.makeText(this, "Database error: Could not look up item", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shows bottom sheet for barcode lookup result.
     */
    private fun showBarcodeResultBottomSheet(item: InventoryItem?, barcode: String) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_scan_result, null)
        bottomSheetDialog.setContentView(view)

        val tvTitle = view.findViewById<TextView>(R.id.tvScanResultTitle)
        val tvDetails = view.findViewById<TextView>(R.id.tvScanResultDetails)
        val btnAction = view.findViewById<MaterialButton>(R.id.btnScanAction)

        if (item != null) {
            tvTitle.text = "Item Found"
            // POLYMORPHISM: getDetails() and getDisplayQuantity() work on any subclass
            tvDetails.text = "Name: ${item.name}\nQuantity: ${item.getDisplayQuantity()}\nLocation: ${item.location}\n\n${item.getDetails()}"
            btnAction.text = "Edit"
            btnAction.setOnClickListener {
                val intent = Intent(this, AddItemActivity::class.java)
                intent.putExtra("ITEM_ID", item.id)
                startActivity(intent)
                bottomSheetDialog.dismiss()
                finish()
            }
        } else {
            tvTitle.text = getString(R.string.item_not_found)
            tvDetails.text = "Barcode: $barcode\n\nThis item is not in your inventory."
            btnAction.text = getString(R.string.add_new_item)
            btnAction.setOnClickListener {
                val intent = Intent(this, AddItemActivity::class.java)
                intent.putExtra("BARCODE", barcode)
                startActivity(intent)
                bottomSheetDialog.dismiss()
                finish()
            }
        }

        bottomSheetDialog.show()
    }
}
