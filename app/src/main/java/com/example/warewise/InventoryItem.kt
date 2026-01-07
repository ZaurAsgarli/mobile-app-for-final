package com.example.warewise

import java.io.Serializable

/**
 * Enum for Type Safety
 */
enum class ItemType {
    UNIT,
    WEIGHT,
    ELECTRONICS,
    FOOD,
    FURNITURE
}

/**
 * Abstract base class for all inventory items.
 * Demonstrates runtime polymorphism with getDetails() method.
 */
abstract class InventoryItem(
    open var id: Int = 0,
    open var name: String,
    open var barcode: String,
    open var description: String,
    open var supplier: String,
    open var location: String,
    open var costPrice: Double,
    open val type: ItemType // NEW: Strict Enum Type
) : Serializable {
    
    // Abstract methods
    abstract fun getDisplayQuantity(): String
    
    // Polyphormic method for academic requirement
    abstract fun getDetails(): String
    
    // Returns extra info specific to item type
    open fun getExtraInfo(): String = ""
    
    // Polymorphic low stock check
    abstract fun isLowStock(): Boolean
}

// =============================================================================
// ORIGINAL ITEM TYPES (UnitItem and WeightItem)
// =============================================================================

/**
 * Item measured by discrete units (quantity).
 */
class UnitItem(
    override var id: Int = 0,
    override var name: String,
    override var barcode: String,
    override var description: String,
    override var supplier: String,
    override var location: String,
    override var costPrice: Double,
    var quantity: Int
) : InventoryItem(id, name, barcode, description, supplier, location, costPrice, ItemType.UNIT) {
    
    override fun getDisplayQuantity(): String = "$quantity units"
    
    override fun getDetails(): String {
        return "📦 Unit Item\nQuantity: $quantity units\nPrice: $${"%.2f".format(costPrice)}\nLocation: $location"
    }
    
    override fun isLowStock(): Boolean = quantity < 5
}

/**
 * Item measured by weight.
 */
class WeightItem(
    override var id: Int = 0,
    override var name: String,
    override var barcode: String,
    override var description: String,
    override var supplier: String,
    override var location: String,
    override var costPrice: Double,
    var weight: Double
) : InventoryItem(id, name, barcode, description, supplier, location, costPrice, ItemType.WEIGHT) {
    
    override fun getDisplayQuantity(): String = "${"%.2f".format(weight)} kg"
    
    override fun getDetails(): String {
        return "⚖️ Weight Item\nWeight: ${"%.2f".format(weight)} kg\nPrice: $${"%.2f".format(costPrice)}/kg\nLocation: $location"
    }
    
    override fun isLowStock(): Boolean = weight < 5.0
}

// =============================================================================
// NEW POLYMORPHIC ITEM TYPES (Electronics, Food, Furniture)
// For the academic requirement demonstrating runtime polymorphism
// =============================================================================

/**
 * Electronics inventory item with warranty information.
 */
class Electronics(
    override var id: Int = 0,
    override var name: String,
    override var barcode: String = "",
    override var description: String = "",
    override var supplier: String = "",
    override var location: String = "",
    override var costPrice: Double,
    var quantity: Int,
    val warranty: String
) : InventoryItem(id, name, barcode, description, supplier, location, costPrice, ItemType.ELECTRONICS) {
    
    override fun getDisplayQuantity(): String = "$quantity units"
    
    override fun getDetails(): String {
        return "📱 Electronics\nWarranty: $warranty\nPrice: $${"%.2f".format(costPrice)} | Stock: $quantity"
    }
    
    override fun getExtraInfo(): String = warranty
    
    override fun isLowStock(): Boolean = quantity < 5
}

/**
 * Food inventory item with expiry date information.
 */
class Food(
    override var id: Int = 0,
    override var name: String,
    override var barcode: String = "",
    override var description: String = "",
    override var supplier: String = "",
    override var location: String = "",
    override var costPrice: Double,
    var quantity: Int,
    val expiryDate: String
) : InventoryItem(id, name, barcode, description, supplier, location, costPrice, ItemType.FOOD) {
    
    override fun getDisplayQuantity(): String = "$quantity units"
    
    override fun getDetails(): String {
        return "🍎 Food Item\nExpiry: $expiryDate\nPrice: $${"%.2f".format(costPrice)} | Stock: $quantity"
    }
    
    override fun getExtraInfo(): String = expiryDate
    
    override fun isLowStock(): Boolean = quantity < 5
}

/**
 * Furniture inventory item with material information.
 */
class Furniture(
    override var id: Int = 0,
    override var name: String,
    override var barcode: String = "",
    override var description: String = "",
    override var supplier: String = "",
    override var location: String = "",
    override var costPrice: Double,
    var quantity: Int,
    val material: String
) : InventoryItem(id, name, barcode, description, supplier, location, costPrice, ItemType.FURNITURE) {
    
    override fun getDisplayQuantity(): String = "$quantity units"
    
    override fun getDetails(): String {
        return "🪑 Furniture\nMaterial: $material\nPrice: $${"%.2f".format(costPrice)} | Stock: $quantity"
    }
    
    override fun getExtraInfo(): String = material
    
    override fun isLowStock(): Boolean = quantity < 5
}

// =============================================================================
// FACTORY for creating items from QR codes and database
// =============================================================================

/**
 * Factory object to create the appropriate InventoryItem subclass based on type.
 */
object InventoryItemFactory {
    
    /**
     * Creates the correct InventoryItem subclass based on the type Enum.
     * UPDATED: Accepts ItemType instead of String for safety.
     * If you have a string, convert it BEFORE calling this (using try-catch).
     */
    fun create(
        type: ItemType,
        id: Int,
        name: String,
        barcode: String,
        description: String,
        supplier: String,
        location: String,
        costPrice: Double,
        quantity: Int,
        weight: Double,
        extraInfo: String
    ): InventoryItem {
        return when (type) {
            ItemType.UNIT -> UnitItem(id, name, barcode, description, supplier, location, costPrice, quantity)
            ItemType.WEIGHT -> WeightItem(id, name, barcode, description, supplier, location, costPrice, weight)
            ItemType.ELECTRONICS -> Electronics(id, name, barcode, description, supplier, location, costPrice, quantity, extraInfo)
            ItemType.FOOD -> Food(id, name, barcode, description, supplier, location, costPrice, quantity, extraInfo)
            ItemType.FURNITURE -> Furniture(id, name, barcode, description, supplier, location, costPrice, quantity, extraInfo)
        }
    }
    
    /**
     * Parses a QR code string and creates the appropriate InventoryItem.
     * Format: Type|Name|Price|Quantity|ExtraInfo
     * Example: Electronics|PlayStation 5|499.99|10|1 Year Warranty
     */
    fun fromQRCode(qrData: String): InventoryItem? {
        return try {
            val parts = qrData.split("|")
            if (parts.size != 5) return null
            
            val typeStr = parts[0].trim().uppercase()
            // ROBUST: Try to parse Enum, fallback to null/catch if invalid
            val type = try {
                ItemType.valueOf(typeStr)
            } catch (e: IllegalArgumentException) {
                return null // Invalid type in QR
            }

            val name = parts[1].trim()
            val price = parts[2].trim().toDoubleOrNull() ?: return null
            val quantity = parts[3].trim().toIntOrNull() ?: return null
            val extraInfo = parts[4].trim()
            
            when (type) {
                ItemType.ELECTRONICS -> Electronics(0, name, "", "", "", "", price, quantity, extraInfo)
                ItemType.FOOD -> Food(0, name, "", "", "", "", price, quantity, extraInfo)
                ItemType.FURNITURE -> Furniture(0, name, "", "", "", "", price, quantity, extraInfo)
                else -> null // QR factory currently supports these 3 specific types as per brief
            }
        } catch (e: Exception) {
            null
        }
    }
}
