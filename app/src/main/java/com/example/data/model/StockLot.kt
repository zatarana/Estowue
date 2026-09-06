package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

@Entity(
    tableName = "stock_lots",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId"), Index("expirationDate")]
)
data class StockLot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val lotNumber: String = "",
    val quantity: Double,
    val initialQuantity: Double = quantity,
    val expirationDate: Long, // timestamp in ms
    val manufacturingDate: Long? = null, // timestamp in ms
    val location: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Number of full calendar days until expiration (can be negative if expired).
     * Normalizes both timestamps to midnight so hourly variations do not cause off-by-one days.
     */
    fun daysUntilExpiration(currentTime: Long = System.currentTimeMillis()): Long {
        val calExp = java.util.Calendar.getInstance().apply {
            timeInMillis = expirationDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val calNow = java.util.Calendar.getInstance().apply {
            timeInMillis = currentTime
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val diffMs = calExp.timeInMillis - calNow.timeInMillis
        return diffMs / 86_400_000L
    }

    fun getStatus(currentTime: Long = System.currentTimeMillis(), alertDays: Int = 30): LotExpirationStatus {
        val days = daysUntilExpiration(currentTime)
        return when {
            quantity <= 0.001 -> LotExpirationStatus.EMPTY
            days < 0 -> LotExpirationStatus.EXPIRED
            days <= (alertDays / 2).coerceAtLeast(7) -> LotExpirationStatus.CRITICAL
            days <= alertDays -> LotExpirationStatus.WARNING
            else -> LotExpirationStatus.VALID
        }
    }
}

enum class LotExpirationStatus(val label: String) {
    VALID("Válido"),
    WARNING("Atenção (Próximo)"),
    CRITICAL("Crítico (Urgente)"),
    EXPIRED("Vencido"),
    EMPTY("Esgotado")
}

data class ProductWithLots(
    val product: Product,
    val lots: List<StockLot>
) {
    val totalQuantity: Double
        get() = lots.sumOf { it.quantity }

    val activeLotsCount: Int
        get() = lots.count { it.quantity > 0.001 }

    val isOutOfStock: Boolean
        get() = totalQuantity <= 0.001

    val isLowStock: Boolean
        get() = product.minStock > 0 && totalQuantity <= product.minStock && totalQuantity > 0.001

    val stockHealthStatus: StockHealthStatus
        get() = when {
            isOutOfStock -> StockHealthStatus.OUT_OF_STOCK
            isLowStock -> StockHealthStatus.LOW_STOCK
            else -> StockHealthStatus.IN_STOCK
        }

    fun expiredLotsCount(currentTime: Long = System.currentTimeMillis()): Int =
        lots.count { it.quantity > 0.001 && it.daysUntilExpiration(currentTime) < 0 }

    fun criticalLotsCount(currentTime: Long = System.currentTimeMillis(), alertDays: Int = 30): Int =
        lots.count { it.quantity > 0.001 && it.daysUntilExpiration(currentTime) in 0..(alertDays / 2).coerceAtLeast(7) }

    fun warningLotsCount(currentTime: Long = System.currentTimeMillis(), alertDays: Int = 30): Int =
        lots.count { it.quantity > 0.001 && it.daysUntilExpiration(currentTime) in ((alertDays / 2).coerceAtLeast(7) + 1)..alertDays }

    val nextExpiringLot: StockLot?
        get() = lots.filter { it.quantity > 0.001 }.minByOrNull { it.expirationDate }
}

