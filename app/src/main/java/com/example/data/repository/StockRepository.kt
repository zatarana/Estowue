package com.example.data.repository

import com.example.data.local.CategoryDao
import com.example.data.local.ProductDao
import com.example.data.local.StockLotDao
import com.example.data.local.StockMovementDao
import com.example.data.model.Category
import com.example.data.model.MovementType
import com.example.data.model.Product
import com.example.data.model.ProductWithLots
import com.example.data.model.StockHealthStatus
import com.example.data.model.StockLot
import com.example.data.model.StockLotsSummary
import com.example.data.model.StockMovement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

import androidx.room.withTransaction

class StockRepository(
    private val db: com.example.data.local.StockDatabase,
    private val productDao: ProductDao,
    private val lotDao: StockLotDao,
    private val movementDao: StockMovementDao,
    private val categoryDao: CategoryDao
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allLots: Flow<List<StockLot>> = lotDao.getAllLots()
    val activeLots: Flow<List<StockLot>> = lotDao.getActiveLots()
    val allMovements: Flow<List<StockMovement>> = movementDao.getAllMovements()

    // Configured Categories from DB
    val dbCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    // Categories with calculated active product counts
    val categoriesWithCount: Flow<List<Category>> = combine(dbCategories, allProducts) { categories, products ->
        val productCountsByCategory = products.groupingBy { it.category }.eachCount()
        categories.map { cat ->
            cat.copy(productCount = productCountsByCategory[cat.name] ?: 0)
        }
    }

    val productsWithLots: Flow<List<ProductWithLots>> = combine(allProducts, allLots) { products, lots ->
        val lotsByProduct = lots.groupBy { it.productId }
        products.map { product ->
            ProductWithLots(
                product = product,
                lots = (lotsByProduct[product.id] ?: emptyList()).sortedBy { it.expirationDate }
            )
        }
    }

    fun getLotsForProduct(productId: Long): Flow<List<StockLot>> =
        lotDao.getLotsForProduct(productId)

    fun getMovementsForProduct(productId: Long): Flow<List<StockMovement>> =
        movementDao.getMovementsForProduct(productId)

    // Category CRUD operations
    suspend fun insertCategory(category: Category): Long {
        val existing = categoryDao.getCategoryByName(category.name.trim())
        return if (existing == null) {
            categoryDao.insertCategory(category.copy(name = category.name.trim()))
        } else {
            existing.id
        }
    }

    suspend fun updateCategory(category: Category, oldName: String) {
        val newName = category.name.trim()
        categoryDao.updateCategory(category.copy(name = newName))
        if (oldName.isNotBlank() && oldName != newName) {
            categoryDao.updateProductsCategoryName(oldName, newName)
        }
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.reassignProductsFromDeletedCategory(category.name)
        categoryDao.deleteCategory(category)
    }

    // Product CRUD operations
    suspend fun insertProduct(product: Product): Long {
        // Ensure category exists in DB
        if (product.category.isNotBlank()) {
            val cat = categoryDao.getCategoryByName(product.category.trim())
            if (cat == null) {
                categoryDao.insertCategory(Category(name = product.category.trim()))
            }
        }
        return productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        if (product.category.isNotBlank()) {
            val cat = categoryDao.getCategoryByName(product.category.trim())
            if (cat == null) {
                categoryDao.insertCategory(Category(name = product.category.trim()))
            }
        }
        productDao.updateProduct(product.copy(lastUpdated = System.currentTimeMillis()))
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    suspend fun insertLot(lot: StockLot): Long {
        return lotDao.insertLot(lot)
    }

    suspend fun updateLot(lot: StockLot) {
        lotDao.updateLot(lot)
    }

    suspend fun deleteLot(lot: StockLot) {
        lotDao.deleteLot(lot)
    }

    suspend fun getProductByBarcode(barcode: String): Product? =
        productDao.getProductByBarcode(barcode.trim())

    suspend fun getProductById(id: Long): Product? =
        productDao.getProductByIdDirect(id)

    suspend fun getMovementById(id: Long): StockMovement? =
        movementDao.getMovementById(id)

    /**
     * Registers a stock entry (Entrada de Estoque com Validade).
     */
    suspend fun registerStockIn(
        productId: Long,
        expirationDate: Long,
        quantity: Double,
        manufacturingDate: Long? = null,
        location: String = "",
        reason: String = "Recebimento de Mercadoria",
        documentNumber: String = "",
        notes: String = ""
    ): Result<Long> {
        return db.withTransaction {
        val product = productDao.getProductByIdDirect(productId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Produto não encontrado"))

        val existingLots = lotDao.getLotsForProductDirect(productId)
        val prevTotalStock = existingLots.sumOf { it.quantity }
        val newTotalStock = prevTotalStock + quantity

        // Find existing active lot with the exact same expiration date to merge, or create new lot
        val existingLot = existingLots.firstOrNull {
            kotlin.math.abs(it.expirationDate - expirationDate) < 86400000L // same day
        }
        val lotId: Long
        val prevLotStock: Double
        val resultingLotStock: Double

        if (existingLot != null) {
            lotId = existingLot.id
            prevLotStock = existingLot.quantity
            resultingLotStock = prevLotStock + quantity

            lotDao.updateLot(
                existingLot.copy(
                    quantity = resultingLotStock,
                    expirationDate = expirationDate,
                    location = if (location.isNotBlank()) location else existingLot.location
                )
            )
        } else {
            prevLotStock = 0.0
            resultingLotStock = quantity
            val newLot = StockLot(
                productId = productId,
                lotNumber = "",
                quantity = quantity,
                initialQuantity = quantity,
                expirationDate = expirationDate,
                manufacturingDate = manufacturingDate,
                location = location,
                notes = notes
            )
            lotId = lotDao.insertLot(newLot)
        }

        productDao.updateProduct(product.copy(lastUpdated = System.currentTimeMillis()))

        val movement = StockMovement(
            productId = productId,
            productName = product.name,
            productBrand = product.brand,
            lotId = lotId,
            lotNumber = "",
            lotExpirationDate = expirationDate,
            type = MovementType.ENTRADA,
            quantity = quantity,
            previousLotStock = prevLotStock,
            resultingLotStock = resultingLotStock,
            previousTotalStock = prevTotalStock,
            resultingTotalStock = newTotalStock,
            reason = reason,
            documentNumber = documentNumber,
            notes = notes,
            timestamp = System.currentTimeMillis()
        )
        movementDao.insertMovement(movement)
        return@withTransaction Result.success(lotId)
            }
    }


    /**
     * Registers a stock dispatch (Saída por Lote / FEFO).
     */
    suspend fun registerStockOut(
        lotId: Long,
        quantity: Double,
        isDiscard: Boolean = false,
        reason: String = if (isDiscard) "Descarte por Vencimento" else "Expedição / Consumo",
        documentNumber: String = "",
        notes: String = ""
    ): Result<StockMovement> {
        return db.withTransaction {
        val lot = lotDao.getLotByIdDirect(lotId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Lote não encontrado"))

        val product = productDao.getProductByIdDirect(lot.productId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Produto não encontrado"))

        if (quantity > lot.quantity) {
            return@withTransaction Result.failure(
                IllegalStateException("Quantidade solicitada ($quantity) maior que o saldo disponível (${lot.quantity})")
            )
        }

        val allProductLots = lotDao.getLotsForProductDirect(product.id)
        val prevTotalStock = allProductLots.sumOf { it.quantity }
        val prevLotStock = lot.quantity
        val resultingLotStock = (prevLotStock - quantity).coerceAtLeast(0.0)
        val newTotalStock = (prevTotalStock - quantity).coerceAtLeast(0.0)

        lotDao.updateLot(lot.copy(quantity = resultingLotStock))
        productDao.updateProduct(product.copy(lastUpdated = System.currentTimeMillis()))

        val movementType = if (isDiscard) MovementType.DESCARTE_VENCIDO else MovementType.SAIDA

        val movement = StockMovement(
            productId = product.id,
            productName = product.name,
            productBrand = product.brand,
            lotId = lot.id,
            lotNumber = "",
            lotExpirationDate = lot.expirationDate,
            type = movementType,
            quantity = quantity,
            previousLotStock = prevLotStock,
            resultingLotStock = resultingLotStock,
            previousTotalStock = prevTotalStock,
            resultingTotalStock = newTotalStock,
            reason = reason,
            documentNumber = documentNumber,
            notes = notes,
            timestamp = System.currentTimeMillis()
        )
        movementDao.insertMovement(movement)
        return@withTransaction Result.success(movement)
            }
    }


    /**
     * Corrects a mistaken stock entry or movement.
     */
    suspend fun correctStockMovement(
        movementId: Long,
        newQuantity: Double,
        newExpirationDate: Long?,
        newReason: String,
        newDocumentNumber: String,
        newNotes: String
    ): Result<StockMovement> {
        return db.withTransaction {
            val movement = movementDao.getMovementById(movementId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Movimentação não encontrada"))

            if (movement.type == MovementType.TRANSFERENCIA) {
                return@withTransaction Result.failure(IllegalStateException("Edição de Transferência não suportada diretamente."))
            }

            val lot = movement.lotId?.let { lotDao.getLotByIdDirect(it) }
            val qtyDifference = newQuantity - movement.quantity
            var adjustedLotQty = lot?.quantity ?: 0.0

            if (lot != null) {
                adjustedLotQty = when (movement.type) {
                    MovementType.ENTRADA -> lot.quantity + qtyDifference
                    MovementType.SAIDA, MovementType.DESCARTE_VENCIDO -> lot.quantity - qtyDifference
                    MovementType.AJUSTE -> newQuantity
                    MovementType.TRANSFERENCIA -> lot.quantity
                }

                if (adjustedLotQty < -0.001) {
                    return@withTransaction Result.failure(
                        IllegalStateException("A correção resultaria em saldo negativo no lote (${String.format("%.2f", adjustedLotQty)}).")
                    )
                }

                val updatedLot = lot.copy(
                    quantity = adjustedLotQty.coerceAtLeast(0.0),
                    expirationDate = newExpirationDate ?: lot.expirationDate
                )
                lotDao.updateLot(updatedLot)
            }

            val product = productDao.getProductByIdDirect(movement.productId)
            if (product != null) {
                productDao.updateProduct(product.copy(lastUpdated = System.currentTimeMillis()))
            }
            val allProductLots = lotDao.getLotsForProductDirect(movement.productId)
            val newTotalStock = allProductLots.sumOf { it.quantity }

            val updatedMovement = movement.copy(
                quantity = newQuantity,
                lotExpirationDate = newExpirationDate ?: movement.lotExpirationDate,
                resultingLotStock = if (lot != null) adjustedLotQty.coerceAtLeast(0.0) else movement.resultingLotStock,
                resultingTotalStock = newTotalStock,
                reason = newReason,
                documentNumber = newDocumentNumber,
                notes = newNotes
            )
            movementDao.updateMovement(updatedMovement)
            return@withTransaction Result.success(updatedMovement)
        }
    }


    /**
     * Reverses / Deletes a mistaken stock movement completely.
     */
    suspend fun deleteOrReverseMovement(movementId: Long): Result<Boolean> {
        return db.withTransaction {
            val movement = movementDao.getMovementById(movementId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Movimentação não encontrada"))

            if (movement.type == MovementType.TRANSFERENCIA) {
                return@withTransaction Result.failure(IllegalStateException("Reversão de Transferência não suportada diretamente."))
            }

            val lot = movement.lotId?.let { lotDao.getLotByIdDirect(it) }
            if (lot != null) {
                val restoredQty = when (movement.type) {
                    MovementType.ENTRADA -> (lot.quantity - movement.quantity).coerceAtLeast(0.0)
                    MovementType.SAIDA, MovementType.DESCARTE_VENCIDO -> (lot.quantity + movement.quantity)
                    MovementType.AJUSTE -> lot.quantity // Keep current for adjustment reversal
                    MovementType.TRANSFERENCIA -> lot.quantity
                }
                lotDao.updateLot(lot.copy(quantity = restoredQty))
            }

            val product = productDao.getProductByIdDirect(movement.productId)
            if (product != null) {
                productDao.updateProduct(product.copy(lastUpdated = System.currentTimeMillis()))
            }

            movementDao.deleteMovement(movement)
            return@withTransaction Result.success(true)
        }
    }


    /**
     * Registers a physical inventory count adjustment for a specific batch/lot.
     */
    suspend fun registerLotAdjustment(
        lotId: Long,
        physicalCount: Double,
        reason: String = "Inventário de Lote",
        notes: String = ""
    ): Result<StockMovement> {
        return db.withTransaction {
        val lot = lotDao.getLotByIdDirect(lotId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Lote não encontrado"))

        val product = productDao.getProductByIdDirect(lot.productId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Produto não encontrado"))

        val allProductLots = lotDao.getLotsForProductDirect(product.id)
        val prevTotalStock = allProductLots.sumOf { it.quantity }
        val prevLotStock = lot.quantity
        val diff = physicalCount - prevLotStock
        val newTotalStock = (prevTotalStock + diff).coerceAtLeast(0.0)

        lotDao.updateLot(lot.copy(quantity = physicalCount))
        productDao.updateProduct(product.copy(lastUpdated = System.currentTimeMillis()))

        val movement = StockMovement(
            productId = product.id,
            productName = product.name,
            productBrand = product.brand,
            lotId = lot.id,
            lotNumber = "",
            lotExpirationDate = lot.expirationDate,
            type = MovementType.AJUSTE,
            quantity = kotlin.math.abs(diff),
            previousLotStock = prevLotStock,
            resultingLotStock = physicalCount,
            previousTotalStock = prevTotalStock,
            resultingTotalStock = newTotalStock,
            reason = reason,
            documentNumber = "AJU-${System.currentTimeMillis() % 10000}",
            notes = if (notes.isNotBlank()) notes else "Ajuste físico: ${if (diff >= 0) "+$diff" else "$diff"} ${product.unit}",
            timestamp = System.currentTimeMillis()
        )
        movementDao.insertMovement(movement)
        return@withTransaction Result.success(movement)
            }
    }


    /**
     * Registers a transfer of stock from one lot (location) to another location.
     */
    suspend fun registerLotTransfer(
        sourceLotId: Long,
        quantity: Double,
        destinationLocation: String,
        reason: String = "Transferência Interna",
        notes: String = ""
    ): Result<StockMovement> {
        return db.withTransaction {
        val sourceLot = lotDao.getLotByIdDirect(sourceLotId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Lote de origem não encontrado"))

        if (quantity > sourceLot.quantity) {
            return@withTransaction Result.failure(
                IllegalStateException("Quantidade solicitada ($quantity) maior que o saldo disponível na origem (${sourceLot.quantity})")
            )
        }

        val product = productDao.getProductByIdDirect(sourceLot.productId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Produto não encontrado"))

        val allProductLots = lotDao.getLotsForProductDirect(product.id)
        val prevTotalStock = allProductLots.sumOf { it.quantity }

        val destLocationTrimmed = destinationLocation.trim()
        
        // Find existing lot in the destination location with same expiration and manufacturing date
        val existingDestLot = allProductLots.firstOrNull {
            it.location.equals(destLocationTrimmed, ignoreCase = true) &&
            kotlin.math.abs(it.expirationDate - sourceLot.expirationDate) < 86400000L &&
            it.manufacturingDate == sourceLot.manufacturingDate &&
            it.id != sourceLot.id
        }

        val sourcePrevQty = sourceLot.quantity
        val sourceNewQty = (sourcePrevQty - quantity).coerceAtLeast(0.0)
        lotDao.updateLot(sourceLot.copy(quantity = sourceNewQty))

        var destLotId: Long
        if (existingDestLot != null) {
            destLotId = existingDestLot.id
            lotDao.updateLot(existingDestLot.copy(quantity = existingDestLot.quantity + quantity))
        } else {
            val newLot = sourceLot.copy(
                id = 0,
                quantity = quantity,
                initialQuantity = quantity,
                location = destLocationTrimmed
            )
            destLotId = lotDao.insertLot(newLot)
        }

        productDao.updateProduct(product.copy(lastUpdated = System.currentTimeMillis()))

        val movement = StockMovement(
            productId = product.id,
            productName = product.name,
            productBrand = product.brand,
            lotId = sourceLotId, // Record against the source lot
            lotNumber = sourceLot.lotNumber,
            lotExpirationDate = sourceLot.expirationDate,
            type = MovementType.TRANSFERENCIA,
            quantity = quantity,
            previousLotStock = sourcePrevQty,
            resultingLotStock = sourceNewQty,
            previousTotalStock = prevTotalStock,
            resultingTotalStock = prevTotalStock, // Total stock doesn't change
            reason = reason,
            documentNumber = "TRF-${System.currentTimeMillis() % 10000}",
            notes = "Transferido de '${sourceLot.location}' para '$destLocationTrimmed'.\n$notes",
            timestamp = System.currentTimeMillis()
        )
        movementDao.insertMovement(movement)
        return@withTransaction Result.success(movement)
            }
    }


    fun calculateSummary(productsWithLots: List<ProductWithLots>, alertDays: Int = 30): StockLotsSummary {
        val now = System.currentTimeMillis()
        val allActiveLots = productsWithLots.flatMap { it.lots }.filter { it.quantity > 0.001 }

        val expired = allActiveLots.count { it.daysUntilExpiration(now) < 0 }
        val criticalDaysThreshold = (alertDays / 2).coerceAtLeast(7)
        val critical = allActiveLots.count { it.daysUntilExpiration(now) in 0..criticalDaysThreshold }
        val warning = allActiveLots.count { it.daysUntilExpiration(now) in (criticalDaysThreshold + 1)..alertDays }

        val lowStock = productsWithLots.count { it.isLowStock }
        val outOfStock = productsWithLots.count { it.isOutOfStock }

        return StockLotsSummary(
            totalProductsCount = productsWithLots.size,
            totalItemsQuantity = allActiveLots.sumOf { it.quantity },
            totalActiveLots = allActiveLots.size,
            expiredLotsCount = expired,
            criticalLotsCount = critical,
            warningLotsCount = warning,
            lowStockProductsCount = lowStock,
            outOfStockProductsCount = outOfStock
        )
    }
}
