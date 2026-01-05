package com.example.warewise

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * WareWise Database Helper
 * 
 * Features:
 * - Full CRUD operations with try-catch error handling
 * - Polymorphic item support (UnitItem, WeightItem, Electronics, Food, Furniture)
 * - Factory pattern for creating correct item types from database
 * - User authentication and profile management
 * - Report/audit logging
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "DatabaseHelper"
        private const val DATABASE_NAME = "WareWise.db"
        private const val DATABASE_VERSION = 4

        // Users Table
        const val TABLE_USERS = "users"
        const val COL_USER_ID = "id"
        const val COL_USERNAME = "username"
        const val COL_PASSWORD = "password"
        const val COL_FULL_NAME = "full_name"
        const val COL_EMPLOYEE_ID = "employee_id"
        const val COL_PROFILE_PHOTO_URI = "profile_photo_uri"
        const val COL_COMPANY_NAME = "company_name"

        // Reports Table
        const val TABLE_REPORTS = "reports"
        const val COL_REPORT_ID = "id"
        const val COL_ACTION_DESCRIPTION = "action_description"
        const val COL_TIMESTAMP = "timestamp"

        // Items Table
        const val TABLE_ITEMS = "items"
        const val COL_ITEM_ID = "id"
        const val COL_NAME = "name"
        const val COL_BARCODE = "barcode"
        const val COL_DESCRIPTION = "description"
        const val COL_SUPPLIER = "supplier"
        const val COL_LOCATION = "location"
        const val COL_COST_PRICE = "cost_price"
        const val COL_TYPE = "type"
        const val COL_QUANTITY = "quantity"
        const val COL_WEIGHT = "weight"
        const val COL_EXTRA_INFO = "extra_info"
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            val createUsersTable = """
                CREATE TABLE $TABLE_USERS (
                    $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_USERNAME TEXT,
                    $COL_PASSWORD TEXT,
                    $COL_FULL_NAME TEXT,
                    $COL_EMPLOYEE_ID TEXT,
                    $COL_PROFILE_PHOTO_URI TEXT,
                    $COL_COMPANY_NAME TEXT
                )
            """.trimIndent()
            db.execSQL(createUsersTable)

            val createReportsTable = """
                CREATE TABLE $TABLE_REPORTS (
                    $COL_REPORT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_ACTION_DESCRIPTION TEXT,
                    $COL_TIMESTAMP TEXT
                )
            """.trimIndent()
            db.execSQL(createReportsTable)

            val createItemsTable = """
                CREATE TABLE $TABLE_ITEMS (
                    $COL_ITEM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_NAME TEXT,
                    $COL_BARCODE TEXT,
                    $COL_DESCRIPTION TEXT,
                    $COL_SUPPLIER TEXT,
                    $COL_LOCATION TEXT,
                    $COL_COST_PRICE REAL,
                    $COL_TYPE TEXT,
                    $COL_QUANTITY INTEGER,
                    $COL_WEIGHT REAL,
                    $COL_EXTRA_INFO TEXT
                )
            """.trimIndent()
            db.execSQL(createItemsTable)

            // Seed Admin User
            val cv = ContentValues().apply {
                put(COL_USERNAME, "admin")
                put(COL_PASSWORD, "1234")
                put(COL_FULL_NAME, "Administrator")
                put(COL_EMPLOYEE_ID, "ADM001")
            }
            db.insert(TABLE_USERS, null, cv)
            
            Log.d(TAG, "Database created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating database: ${e.message}", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_ITEMS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_REPORTS")
            onCreate(db)
        } catch (e: Exception) {
            Log.e(TAG, "Error upgrading database: ${e.message}", e)
        }
    }

    // =========================================================================
    // ITEM OPERATIONS (Polymorphic with Try-Catch)
    // =========================================================================

    /**
     * Adds an inventory item to the database.
     * Uses polymorphism - handles any InventoryItem subclass.
     */
    fun addItem(item: InventoryItem): Long {
        var db: SQLiteDatabase? = null
        return try {
            db = this.writableDatabase
            val cv = ContentValues().apply {
                put(COL_NAME, item.name)
                put(COL_BARCODE, item.barcode)
                put(COL_DESCRIPTION, item.description)
                put(COL_SUPPLIER, item.supplier)
                put(COL_LOCATION, item.location)
                put(COL_COST_PRICE, item.costPrice)
                put(COL_TYPE, item.getType())
                put(COL_EXTRA_INFO, item.getExtraInfo())
            }

            // Handle quantity/weight based on item type
            when (item) {
                is UnitItem -> {
                    cv.put(COL_QUANTITY, item.quantity)
                    cv.put(COL_WEIGHT, 0.0)
                }
                is WeightItem -> {
                    cv.put(COL_WEIGHT, item.weight)
                    cv.put(COL_QUANTITY, 0)
                }
                is Electronics -> {
                    cv.put(COL_QUANTITY, item.quantity)
                    cv.put(COL_WEIGHT, 0.0)
                }
                is Food -> {
                    cv.put(COL_QUANTITY, item.quantity)
                    cv.put(COL_WEIGHT, 0.0)
                }
                is Furniture -> {
                    cv.put(COL_QUANTITY, item.quantity)
                    cv.put(COL_WEIGHT, 0.0)
                }
            }

            val result = db.insert(TABLE_ITEMS, null, cv)
            if (result != -1L) {
                logReportInternal(db, "Item '${item.name}' added.")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error adding item: ${e.message}", e)
            -1L
        } finally {
            db?.close()
        }
    }

    /**
     * Retrieves all items from the database.
     * Uses Factory pattern to create correct polymorphic subclass.
     */
    fun getAllItems(): List<InventoryItem> {
        val itemList = ArrayList<InventoryItem>()
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        
        try {
            db = this.readableDatabase
            cursor = db.rawQuery("SELECT * FROM $TABLE_ITEMS ORDER BY $COL_ITEM_ID DESC", null)

            if (cursor.moveToFirst()) {
                do {
                    val item = createItemFromCursor(cursor)
                    if (item != null) {
                        itemList.add(item)
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all items: ${e.message}", e)
        } finally {
            cursor?.close()
            db?.close()
        }
        
        return itemList
    }

    /**
     * Creates an InventoryItem from a cursor using the Factory pattern.
     */
    private fun createItemFromCursor(cursor: Cursor): InventoryItem? {
        return try {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ITEM_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)) ?: ""
            val barcode = cursor.getString(cursor.getColumnIndexOrThrow(COL_BARCODE)) ?: ""
            val description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)) ?: ""
            val supplier = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUPPLIER)) ?: ""
            val location = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATION)) ?: ""
            val costPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_COST_PRICE))
            val type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE)) ?: "UNIT"
            val quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COL_QUANTITY))
            val weight = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_WEIGHT))
            
            val extraInfoIndex = cursor.getColumnIndex(COL_EXTRA_INFO)
            val extraInfo = if (extraInfoIndex >= 0) cursor.getString(extraInfoIndex) ?: "" else ""

            // POLYMORPHISM: Factory creates the correct subclass based on type
            InventoryItemFactory.create(
                type, id, name, barcode, description, supplier, location,
                costPrice, quantity, weight, extraInfo
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating item from cursor: ${e.message}", e)
            null
        }
    }

    /**
     * Gets an item by barcode.
     */
    fun getItemByBarcode(barcode: String): InventoryItem? {
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        
        return try {
            db = this.readableDatabase
            cursor = db.query(TABLE_ITEMS, null, "$COL_BARCODE=?", arrayOf(barcode), null, null, null)
            
            if (cursor.moveToFirst()) {
                createItemFromCursor(cursor)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting item by barcode: ${e.message}", e)
            null
        } finally {
            cursor?.close()
            db?.close()
        }
    }

    /**
     * Gets an item by ID.
     */
    fun getItemById(id: Int): InventoryItem? {
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        
        return try {
            db = this.readableDatabase
            cursor = db.query(TABLE_ITEMS, null, "$COL_ITEM_ID=?", arrayOf(id.toString()), null, null, null)
            
            if (cursor.moveToFirst()) {
                createItemFromCursor(cursor)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting item by ID: ${e.message}", e)
            null
        } finally {
            cursor?.close()
            db?.close()
        }
    }

    /**
     * Updates an existing item.
     */
    fun updateItem(item: InventoryItem): Boolean {
        var db: SQLiteDatabase? = null
        
        return try {
            db = this.writableDatabase
            val cv = ContentValues().apply {
                put(COL_NAME, item.name)
                put(COL_BARCODE, item.barcode)
                put(COL_DESCRIPTION, item.description)
                put(COL_SUPPLIER, item.supplier)
                put(COL_LOCATION, item.location)
                put(COL_COST_PRICE, item.costPrice)
                put(COL_TYPE, item.getType())
                put(COL_EXTRA_INFO, item.getExtraInfo())
            }

            when (item) {
                is UnitItem -> {
                    cv.put(COL_QUANTITY, item.quantity)
                    cv.put(COL_WEIGHT, 0.0)
                }
                is WeightItem -> {
                    cv.put(COL_WEIGHT, item.weight)
                    cv.put(COL_QUANTITY, 0)
                }
                is Electronics -> {
                    cv.put(COL_QUANTITY, item.quantity)
                    cv.put(COL_WEIGHT, 0.0)
                }
                is Food -> {
                    cv.put(COL_QUANTITY, item.quantity)
                    cv.put(COL_WEIGHT, 0.0)
                }
                is Furniture -> {
                    cv.put(COL_QUANTITY, item.quantity)
                    cv.put(COL_WEIGHT, 0.0)
                }
            }

            val result = db.update(TABLE_ITEMS, cv, "$COL_ITEM_ID=?", arrayOf(item.id.toString()))
            result > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error updating item: ${e.message}", e)
            false
        } finally {
            db?.close()
        }
    }

    /**
     * Deletes an item by ID.
     */
    fun deleteItem(itemId: Int): Boolean {
        var db: SQLiteDatabase? = null
        
        return try {
            db = this.writableDatabase
            val result = db.delete(TABLE_ITEMS, "$COL_ITEM_ID=?", arrayOf(itemId.toString()))
            if (result > 0) {
                logReportInternal(db, "Item with ID $itemId deleted.")
            }
            result > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting item: ${e.message}", e)
            false
        } finally {
            db?.close()
        }
    }

    /**
     * Searches items by name or barcode.
     */
    fun searchItems(query: String): List<InventoryItem> {
        val itemList = ArrayList<InventoryItem>()
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        
        return try {
            db = this.readableDatabase
            cursor = db.rawQuery(
                "SELECT * FROM $TABLE_ITEMS WHERE $COL_NAME LIKE ? OR $COL_BARCODE LIKE ?",
                arrayOf("%$query%", "%$query%")
            )

            if (cursor.moveToFirst()) {
                do {
                    val item = createItemFromCursor(cursor)
                    if (item != null) {
                        itemList.add(item)
                    }
                } while (cursor.moveToNext())
            }
            itemList
        } catch (e: Exception) {
            Log.e(TAG, "Error searching items: ${e.message}", e)
            itemList
        } finally {
            cursor?.close()
            db?.close()
        }
    }

    /**
     * Gets recent items (last 10).
     */
    fun getRecentItems(): List<InventoryItem> {
        val itemList = ArrayList<InventoryItem>()
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        
        return try {
            db = this.readableDatabase
            cursor = db.rawQuery("SELECT * FROM $TABLE_ITEMS ORDER BY $COL_ITEM_ID DESC LIMIT 10", null)

            if (cursor.moveToFirst()) {
                do {
                    val item = createItemFromCursor(cursor)
                    if (item != null) {
                        itemList.add(item)
                    }
                } while (cursor.moveToNext())
            }
            itemList
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recent items: ${e.message}", e)
            itemList
        } finally {
            cursor?.close()
            db?.close()
        }
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    fun getTotalItemsCount(): Int {
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        
        return try {
            db = this.readableDatabase
            cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_ITEMS", null)
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total items count: ${e.message}", e)
            0
        } finally {
            cursor?.close()
            db?.close()
        }
    }

    fun getTotalValue(): Double {
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        
        return try {
            db = this.readableDatabase
            cursor = db.rawQuery(
                "SELECT $COL_COST_PRICE, $COL_TYPE, $COL_QUANTITY, $COL_WEIGHT FROM $TABLE_ITEMS",
                null
            )
            var totalValue = 0.0

            if (cursor.moveToFirst()) {
                do {
                    val costPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_COST_PRICE))
                    val type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE))
                    
                    totalValue += if (type == "WEIGHT") {
                        val weight = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_WEIGHT))
                        costPrice * weight
                    } else {
                        val quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COL_QUANTITY))
                        costPrice * quantity
                    }
                } while (cursor.moveToNext())
            }
            totalValue
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total value: ${e.message}", e)
            0.0
        } finally {
            cursor?.close()
            db?.close()
        }
    }

    fun getLowStockItemsCount(threshold: Int = 10): Int {
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        
        return try {
            db = this.readableDatabase
            cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_ITEMS WHERE ($COL_TYPE != 'WEIGHT' AND $COL_QUANTITY < $threshold) OR ($COL_TYPE = 'WEIGHT' AND $COL_WEIGHT < 5.0)",
                null
            )
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting low stock count: ${e.message}", e)
            0
        } finally {
            cursor?.close()
            db?.close()
        }
    }

    // =========================================================================
    // USER OPERATIONS
    // =========================================================================

    fun authenticateUser(username: String, password: String): Boolean {
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        
        return try {
            db = this.readableDatabase
            cursor = db.query(TABLE_USERS, arrayOf(COL_USER_ID), "$COL_USERNAME=? AND $COL_PASSWORD=?", arrayOf(username, password), null, null, null)
            cursor.count > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error authenticating user: ${e.message}", e)
            false
        } finally {
            cursor?.close()
            db?.close()
        }
    }

    fun registerUser(username: String, password: String, fullName: String, employeeId: String): Long {
        var db: SQLiteDatabase? = null
        
        return try {
            db = this.writableDatabase
            val cv = ContentValues().apply {
                put(COL_USERNAME, username)
                put(COL_PASSWORD, password)
                put(COL_FULL_NAME, fullName)
                put(COL_EMPLOYEE_ID, employeeId)
            }
            db.insert(TABLE_USERS, null, cv)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user: ${e.message}", e)
            -1L
        } finally {
            db?.close()
        }
    }

    fun usernameExists(username: String): Boolean {
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        
        return try {
            db = this.readableDatabase
            cursor = db.query(TABLE_USERS, arrayOf(COL_USER_ID), "$COL_USERNAME=?", arrayOf(username), null, null, null)
            cursor.count > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking username: ${e.message}", e)
            false
        } finally {
            cursor?.close()
            db?.close()
        }
    }

    fun getUserFullName(username: String): String? {
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        
        return try {
            db = this.readableDatabase
            cursor = db.query(TABLE_USERS, arrayOf(COL_FULL_NAME), "$COL_USERNAME=?", arrayOf(username), null, null, null)
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user full name: ${e.message}", e)
            null
        } finally {
            cursor?.close()
            db?.close()
        }
    }

    fun getUserProfile(username: String): UserProfile? {
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        
        return try {
            db = this.readableDatabase
            cursor = db.query(TABLE_USERS, null, "$COL_USERNAME=?", arrayOf(username), null, null, null)
            if (cursor.moveToFirst()) {
                UserProfile(
                    username = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
                    fullName = cursor.getString(cursor.getColumnIndexOrThrow(COL_FULL_NAME)) ?: "",
                    employeeId = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMPLOYEE_ID)) ?: "",
                    companyName = cursor.getString(cursor.getColumnIndexOrThrow(COL_COMPANY_NAME)),
                    profilePhotoUri = cursor.getString(cursor.getColumnIndexOrThrow(COL_PROFILE_PHOTO_URI))
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile: ${e.message}", e)
            null
        } finally {
            cursor?.close()
            db?.close()
        }
    }

    fun updateUser(user: UserProfile): Boolean {
        var db: SQLiteDatabase? = null
        
        return try {
            db = this.writableDatabase
            val cv = ContentValues().apply {
                put(COL_FULL_NAME, user.fullName)
                put(COL_COMPANY_NAME, user.companyName)
                put(COL_PROFILE_PHOTO_URI, user.profilePhotoUri)
            }
            val result = db.update(TABLE_USERS, cv, "$COL_EMPLOYEE_ID=?", arrayOf(user.employeeId))
            result > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user: ${e.message}", e)
            false
        } finally {
            db?.close()
        }
    }

    fun updatePassword(username: String, newPass: String): Boolean {
        var db: SQLiteDatabase? = null
        
        return try {
            db = this.writableDatabase
            val cv = ContentValues().apply {
                put(COL_PASSWORD, newPass)
            }
            val result = db.update(TABLE_USERS, cv, "$COL_USERNAME=?", arrayOf(username))
            result > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error updating password: ${e.message}", e)
            false
        } finally {
            db?.close()
        }
    }

    // =========================================================================
    // REPORTS / AUDIT LOG
    // =========================================================================

    fun addReport(description: String) {
        logReport(description)
    }

    fun logReport(action: String) {
        var db: SQLiteDatabase? = null
        
        try {
            db = this.writableDatabase
            logReportInternal(db, action)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging report: ${e.message}", e)
        } finally {
            db?.close()
        }
    }

    private fun logReportInternal(db: SQLiteDatabase, action: String) {
        try {
            val cv = ContentValues().apply {
                put(COL_ACTION_DESCRIPTION, action)
                put(COL_TIMESTAMP, System.currentTimeMillis().toString())
            }
            db.insert(TABLE_REPORTS, null, cv)
        } catch (e: Exception) {
            Log.e(TAG, "Error in logReportInternal: ${e.message}", e)
        }
    }

    fun getAllReports(): List<ReportItem> {
        val reportList = ArrayList<ReportItem>()
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        
        return try {
            db = this.readableDatabase
            cursor = db.rawQuery("SELECT * FROM $TABLE_REPORTS ORDER BY $COL_REPORT_ID DESC", null)

            if (cursor.moveToFirst()) {
                do {
                    val report = ReportItem(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REPORT_ID)),
                        description = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACTION_DESCRIPTION)) ?: "",
                        timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)) ?: ""
                    )
                    reportList.add(report)
                } while (cursor.moveToNext())
            }
            reportList
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all reports: ${e.message}", e)
            reportList
        } finally {
            cursor?.close()
            db?.close()
        }
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    fun clearAllData() {
        var db: SQLiteDatabase? = null
        
        try {
            db = this.writableDatabase
            db.execSQL("DELETE FROM $TABLE_ITEMS")
            db.execSQL("DELETE FROM $TABLE_USERS")
            db.execSQL("DELETE FROM $TABLE_REPORTS")
            
            // Re-seed admin
            val cv = ContentValues().apply {
                put(COL_USERNAME, "admin")
                put(COL_PASSWORD, "1234")
                put(COL_FULL_NAME, "Administrator")
                put(COL_EMPLOYEE_ID, "ADM001")
            }
            db.insert(TABLE_USERS, null, cv)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all data: ${e.message}", e)
        } finally {
            db?.close()
        }
    }

    fun updateItemQuantity(itemId: Long, newQuantity: Double): Boolean {
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        
        return try {
            db = this.writableDatabase
            cursor = db.query(TABLE_ITEMS, arrayOf(COL_TYPE), "$COL_ITEM_ID=?", arrayOf(itemId.toString()), null, null, null)
            
            if (cursor.moveToFirst()) {
                val type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE))
                val cv = ContentValues()
                
                if (type == "WEIGHT") {
                    cv.put(COL_WEIGHT, newQuantity)
                } else {
                    cv.put(COL_QUANTITY, newQuantity.toInt())
                }
                
                cursor.close()
                val result = db.update(TABLE_ITEMS, cv, "$COL_ITEM_ID=?", arrayOf(itemId.toString()))
                result > 0
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating item quantity: ${e.message}", e)
            false
        } finally {
            cursor?.close()
            db?.close()
        }
    }
}

// Data classes for User and Report
data class UserProfile(
    val username: String,
    val fullName: String,
    val employeeId: String,
    val companyName: String?,
    val profilePhotoUri: String?
)

data class ReportItem(
    val id: Int,
    val description: String,
    val timestamp: String
)
