package com.example.warewise

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class DbBackupHelper(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun exportData(uri: Uri): Boolean {
        return try {
            val items = dbHelper.getAllItems()
            val jsonArray = JSONArray()

            for (item in items) {
                val jsonObject = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("barcode", item.barcode)
                    put("description", item.description)
                    put("supplier", item.supplier)
                    put("location", item.location)
                    put("cost_price", item.costPrice)
                    put("type", item.getType())
                    put("extra_info", item.getExtraInfo())

                    when (item) {
                        is UnitItem -> {
                            put("quantity", item.quantity)
                            put("weight", 0.0)
                        }
                        is WeightItem -> {
                            put("quantity", 0)
                            put("weight", item.weight)
                        }
                        is Electronics -> {
                            put("quantity", item.quantity)
                            put("weight", 0.0)
                        }
                        is Food -> {
                            put("quantity", item.quantity)
                            put("weight", 0.0)
                        }
                        is Furniture -> {
                            put("quantity", item.quantity)
                            put("weight", 0.0)
                        }
                    }
                }
                jsonArray.put(jsonObject)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonArray.toString(4)) // Pretty print
                }
            }
            true
        } catch (e: Exception) {
            Log.e("DbBackupHelper", "Export failed: ${e.message}", e)
            false
        }
    }

    fun importData(uri: Uri): Boolean {
        return try {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                }
            }

            val jsonArray = JSONArray(stringBuilder.toString())
            
            // 1. Schema Validation
            if (!validateSchema(jsonArray)) {
                return false
            }

            // 2. Clear Database
            dbHelper.clearAllData()

            // 3. Insert Items
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                
                val type = obj.optString("type", "UNIT")
                val name = obj.getString("name")
                val barcode = obj.optString("barcode", "")
                val description = obj.optString("description", "")
                val supplier = obj.optString("supplier", "")
                val location = obj.optString("location", "")
                val costPrice = obj.optDouble("cost_price", 0.0)
                val quantity = obj.optInt("quantity", 0)
                val weight = obj.optDouble("weight", 0.0)
                val extraInfo = obj.optString("extra_info", "")

                // We don't restore ID to let AutoIncrement handle it, 
                // OR we could preserve it if we want exact clone. 
                // Let's rely on DB to generate new IDs to avoid conflicts or just ignore ID from JSON.
                // The prompt implies "batch insert", simpler to treat as new insertions or preserve.
                // Factory.create normally takes ID, but addItem inserts new.
                // We'll construct objects with ID=0 for insertion.

                val item = InventoryItemFactory.create(
                    type, 0, name, barcode, description, supplier, location,
                    costPrice, quantity, weight, extraInfo
                )
                
                dbHelper.addItem(item)
            }
            true
        } catch (e: Exception) {
            Log.e("DbBackupHelper", "Import failed: ${e.message}", e)
            false
        }
    }

    private fun validateSchema(jsonArray: JSONArray): Boolean {
        if (jsonArray.length() == 0) return true // Empty is technically valid schema-wise? Or fail? Let's say valid but nothing to import.

        // Check first item as sample, or check all? Checking all is safer.
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (!obj.has("name") || !obj.has("cost_price") || !obj.has("type")) {
                Log.e("DbBackupHelper", "Validation failed: Missing required fields at index $i")
                return false
            }
            
            val type = obj.getString("type")
            val validTypes = setOf("UNIT", "WEIGHT", "ELECTRONICS", "FOOD", "FURNITURE")
            if (type.uppercase() !in validTypes) {
                Log.e("DbBackupHelper", "Validation failed: Invalid type '$type' at index $i")
                return false
            }
        }
        return true
    }
}
