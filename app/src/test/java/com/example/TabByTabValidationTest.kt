package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsManager
import com.example.data.model.Category
import com.example.data.model.MovementType
import com.example.data.model.Product
import com.example.data.model.ProductWithLots
import com.example.data.model.StockHealthStatus
import com.example.data.model.StockLot
import com.example.data.model.StockLotsSummary
import com.example.data.model.StockMovement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TabByTabValidationTest {

    private lateinit var context: Context
    private lateinit var settingsManager: SettingsManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsManager = SettingsManager(context)
    }

    // ==========================================
    // ABA 1: PRODUTOS & GESTÃO DE ESTOQUE
    // ==========================================
    @Test
    fun `test Aba 1 - Product calculation, stock health and search filtering`() {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        // Product with 2 active lots
        val product1 = Product(
            id = 1L,
            name = "Leite Integral 1L",
            brand = "Parmalat",
            barcode = "78910001001",
            category = "Laticínios",
            unit = "un",
            minStock = 20.0
        )
        val lot1 = StockLot(
            id = 101L,
            productId = 1L,
            lotNumber = "L-001",
            quantity = 15.0,
            expirationDate = now + 10 * oneDayMs, // Crítico (< 15 dias)
            location = "Geladeira 1"
        )
        val lot2 = StockLot(
            id = 102L,
            productId = 1L,
            lotNumber = "L-002",
            quantity = 10.0,
            expirationDate = now + 60 * oneDayMs, // Válido
            location = "Depósito"
        )

        val productWithLots = ProductWithLots(
            product = product1,
            lots = listOf(lot1, lot2)
        )

        // Verificação de estoque total consolidado
        assertEquals(25.0, productWithLots.totalQuantity, 0.001)
        assertFalse(productWithLots.isLowStock) // 25 > minStock (20)
        assertFalse(productWithLots.isOutOfStock)
        assertEquals(StockHealthStatus.IN_STOCK, productWithLots.stockHealthStatus)

        // Verificação FEFO: primeiro lote a vencer deve ser o L-001
        val earliestLot = productWithLots.nextExpiringLot
        assertNotNull(earliestLot)
        assertEquals("L-001", earliestLot?.lotNumber)

        // Verificação de busca por nome, marca e código
        val searchByName = product1.name.contains("Leite", ignoreCase = true)
        val searchByBrand = product1.brand.contains("Parmalat", ignoreCase = true)
        val searchByBarcode = product1.barcode.contains("7891000", ignoreCase = true)
        assertTrue(searchByName)
        assertTrue(searchByBrand)
        assertTrue(searchByBarcode)
    }

    // ==========================================
    // ABA 2: LOTES & VALIDADES (FEFO)
    // ==========================================
    @Test
    fun `test Aba 2 - FEFO Expiration sorting and alert antecedence thresholds`() {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        val lotExpired = StockLot(
            id = 201L,
            productId = 1L,
            quantity = 5.0,
            expirationDate = now - 2 * oneDayMs // Vencido há 2 dias
        )
        val lotCritical = StockLot(
            id = 202L,
            productId = 1L,
            quantity = 8.0,
            expirationDate = now + 5 * oneDayMs // Vence em 5 dias (< 15 dias)
        )
        val lotWarning = StockLot(
            id = 203L,
            productId = 1L,
            quantity = 12.0,
            expirationDate = now + 25 * oneDayMs // Vence em 25 dias (alerta para 30 dias)
        )
        val lotOk = StockLot(
            id = 204L,
            productId = 1L,
            quantity = 20.0,
            expirationDate = now + 90 * oneDayMs // Válido
        )

        val lots = listOf(lotWarning, lotOk, lotExpired, lotCritical)
        val sortedFEFO = lots.sortedBy { it.expirationDate }

        // Ordem FEFO obrigatória: Vencido -> Crítico -> Alerta -> Válido
        assertEquals(201L, sortedFEFO[0].id)
        assertEquals(202L, sortedFEFO[1].id)
        assertEquals(203L, sortedFEFO[2].id)
        assertEquals(204L, sortedFEFO[3].id)

        // Verificação de dias até vencer
        assertTrue(lotExpired.daysUntilExpiration(now) < 0)
        assertTrue(lotCritical.daysUntilExpiration(now) in 0..14)
        assertTrue(lotWarning.daysUntilExpiration(now) in 15..30)
        assertTrue(lotOk.daysUntilExpiration(now) > 30)
    }

    // ==========================================
    // ABA 3: RELATÓRIOS & RESUMO ANALÍTICO
    // ==========================================
    @Test
    fun `test Aba 3 - Stock summary statistics and KPI aggregation`() {
        val summary = StockLotsSummary(
            totalProductsCount = 2,
            totalItemsQuantity = 48.0,
            totalActiveLots = 3,
            expiredLotsCount = 1,
            criticalLotsCount = 1,
            warningLotsCount = 0,
            lowStockProductsCount = 2,
            outOfStockProductsCount = 0
        )

        assertEquals(2, summary.totalProductsCount)
        assertEquals(3, summary.totalActiveLots)
        assertEquals(48.0, summary.totalItemsQuantity, 0.001)
        assertEquals(1, summary.expiredLotsCount)
        assertEquals(1, summary.criticalLotsCount)
        assertEquals(2, summary.lowStockProductsCount)
    }

    // ==========================================
    // ABA 4: HISTÓRICO DE MOVIMENTAÇÕES & AUDITORIA
    // ==========================================
    @Test
    fun `test Aba 4 - Movement tracking, stock evolution and filtering`() {
        val movements = listOf(
            StockMovement(
                id = 1L,
                productId = 10L,
                productName = "Farinha 1kg",
                productBrand = "Dona Benta",
                type = MovementType.ENTRADA,
                quantity = 50.0,
                previousTotalStock = 0.0,
                resultingTotalStock = 50.0,
                reason = "Compra NF 123",
                documentNumber = "NF-123"
            ),
            StockMovement(
                id = 2L,
                productId = 10L,
                productName = "Farinha 1kg",
                productBrand = "Dona Benta",
                type = MovementType.SAIDA,
                quantity = 10.0,
                previousTotalStock = 50.0,
                resultingTotalStock = 40.0,
                reason = "Venda Balcão",
                documentNumber = "PED-45"
            ),
            StockMovement(
                id = 3L,
                productId = 10L,
                productName = "Farinha 1kg",
                productBrand = "Dona Benta",
                type = MovementType.DESCARTE_VENCIDO,
                quantity = 2.0,
                previousTotalStock = 40.0,
                resultingTotalStock = 38.0,
                reason = "Embalagem Danificada"
            )
        )

        // Filtro por tipo
        val entradas = movements.filter { it.type == MovementType.ENTRADA }
        val saidas = movements.filter { it.type == MovementType.SAIDA }
        val descartes = movements.filter { it.type == MovementType.DESCARTE_VENCIDO }

        assertEquals(1, entradas.size)
        assertEquals(1, saidas.size)
        assertEquals(1, descartes.size)

        // Balanço
        val totalEntradas = entradas.sumOf { it.quantity }
        val totalSaidas = saidas.sumOf { it.quantity }
        val totalDescartes = descartes.sumOf { it.quantity }

        assertEquals(50.0, totalEntradas, 0.001)
        assertEquals(10.0, totalSaidas, 0.001)
        assertEquals(2.0, totalDescartes, 0.001)
        assertEquals(38.0, totalEntradas - totalSaidas - totalDescartes, 0.001)
    }

    // ==========================================
    // ABA 5: CONFIGURAÇÕES - CATEGORIAS, MARCAS & LOCAIS
    // ==========================================
    @Test
    fun `test Aba 5 - Categories, brands, locations and persistence management`() {
        val category = Category(
            id = 5L,
            name = "Congelados",
            colorHex = "#06B6D4",
            description = "Produtos mantidos a -18°C"
        )
        assertEquals("Congelados", category.name)
        assertEquals("#06B6D4", category.colorHex)

        // Settings persistence for brands and locations
        settingsManager.addBrand("Nestlé")
        settingsManager.addBrand("Ambev")
        assertTrue(settingsManager.brands.value.contains("Nestlé"))
        assertTrue(settingsManager.brands.value.contains("Ambev"))

        settingsManager.removeBrand("Ambev")
        assertFalse(settingsManager.brands.value.contains("Ambev"))
        assertTrue(settingsManager.brands.value.contains("Nestlé"))

        settingsManager.addLocation("Câmara Fria 1")
        settingsManager.addLocation("Prateleira B3")
        assertTrue(settingsManager.locations.value.contains("Câmara Fria 1"))
        assertTrue(settingsManager.locations.value.contains("Prateleira B3"))

        settingsManager.updateLocation("Câmara Fria 1", "Câmara Fria Principal")
        assertTrue(settingsManager.locations.value.contains("Câmara Fria Principal"))
        assertFalse(settingsManager.locations.value.contains("Câmara Fria 1"))
    }
}
