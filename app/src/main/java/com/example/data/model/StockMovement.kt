package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MovementType(val label: String, val isPositive: Boolean) {
    ENTRADA("Entrada", true),
    SAIDA("Saída", false),
    AJUSTE("Ajuste", false),
    DESCARTE_VENCIDO("Descarte Vencido", false),
    TRANSFERENCIA("Transferência", true)
}

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId"), Index("timestamp")]
)
data class StockMovement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val productBrand: String = "",
    val lotId: Long? = null,
    val lotNumber: String = "",
    val lotExpirationDate: Long? = null,
    val type: MovementType,
    val quantity: Double,
    val previousLotStock: Double = 0.0,
    val resultingLotStock: Double = 0.0,
    val previousTotalStock: Double = 0.0,
    val resultingTotalStock: Double = 0.0,
    val reason: String = "",
    val documentNumber: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class StockLotsSummary(
    val totalProductsCount: Int = 0,
    val totalItemsQuantity: Double = 0.0,
    val totalActiveLots: Int = 0,
    val expiredLotsCount: Int = 0,
    val criticalLotsCount: Int = 0,
    val warningLotsCount: Int = 0,
    val lowStockProductsCount: Int = 0,
    val outOfStockProductsCount: Int = 0
)
