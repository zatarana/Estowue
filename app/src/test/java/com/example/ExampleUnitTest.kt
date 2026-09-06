package com.example

import com.example.data.model.Product
import com.example.data.model.ProductWithLots
import com.example.data.model.StockHealthStatus
import com.example.data.model.StockLot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testProductWithLotsCalculations() {
        val product = Product(
            id = 1L,
            name = "Produto Teste",
            category = "Geral",
            minStock = 15.0
        )

        val lots = listOf(
            StockLot(productId = 1L, quantity = 10.0, expirationDate = System.currentTimeMillis() + 86400000L),
            StockLot(productId = 1L, quantity = 10.0, expirationDate = System.currentTimeMillis() + 86400000L * 2)
        )

        val productWithLots = ProductWithLots(product, lots)

        assertEquals(20.0, productWithLots.totalQuantity, 0.001)
        assertEquals(StockHealthStatus.IN_STOCK, productWithLots.stockHealthStatus)
    }

    @Test
    fun testLowStockHealthStatus() {
        val lowProduct = Product(
            id = 2L,
            name = "Item Baixo",
            category = "Geral",
            minStock = 10.0
        )
        
        val lots = listOf(
            StockLot(productId = 2L, quantity = 3.0, expirationDate = System.currentTimeMillis() + 86400000L)
        )
        
        val productWithLots = ProductWithLots(lowProduct, lots)
        
        assertTrue(productWithLots.isLowStock)
        assertEquals(StockHealthStatus.LOW_STOCK, productWithLots.stockHealthStatus)

        val outProduct = Product(
            id = 3L,
            name = "Item Zerado",
            category = "Geral",
            minStock = 5.0
        )
        
        val emptyLots = listOf<StockLot>()
        val outProductWithLots = ProductWithLots(outProduct, emptyLots)

        assertTrue(outProductWithLots.isOutOfStock)
        assertEquals(StockHealthStatus.OUT_OF_STOCK, outProductWithLots.stockHealthStatus)
    }
}
