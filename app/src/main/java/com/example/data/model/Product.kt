package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val brand: String = "", // marca
    val barcode: String = "", // código de barras / EAN / QR
    val category: String,
    val unit: String = "un", // un, kg, L, m, cx, pct, frasco, etc.
    val minStock: Double = 0.0, // estoque mínimo definido pelo usuário (alerta de reposição)
    val location: String = "", // ex: "Prateleira A1", "Galpão 2"
    val description: String = "",
    val imageUri: String = "", // URI for the product image (local gallery or camera)
    val lastUpdated: Long = System.currentTimeMillis()
)

enum class StockHealthStatus(val label: String) {
    IN_STOCK("Em Estoque"),
    LOW_STOCK("Abaixo do Mínimo"),
    OUT_OF_STOCK("Sem Estoque")
}

