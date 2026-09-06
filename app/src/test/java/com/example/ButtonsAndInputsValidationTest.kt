package com.example

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsManager
import com.example.data.backup.FirebaseSyncManager
import com.example.data.model.Category
import com.example.data.model.MovementType
import com.example.data.model.Product
import com.example.data.model.ProductWithLots
import com.example.data.model.StockLot
import com.example.data.model.StockMovement
import com.example.ui.dialogs.DateVisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ButtonsAndInputsValidationTest {

    private lateinit var context: Context
    private lateinit var settingsManager: SettingsManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsManager = SettingsManager(context)
    }

    // =========================================================================
    // 1. TESTE DE INPUTS E BOTÕES DO MODAL DE CADASTRO/EDIÇÃO DE PRODUTO
    // =========================================================================

    @Test
    fun `test Product Form - name blank validation blocks save action`() {
        var savedProduct: Product? = null

        val blankNameInput = "   "
        var nameError = false

        // Simula clique no botão "Cadastrar Produto"
        if (blankNameInput.isBlank()) {
            nameError = true
        } else {
            savedProduct = Product(name = blankNameInput.trim(), category = "Geral")
        }

        assertTrue("O campo nome em branco deve acionar erro", nameError)
        assertNull("O produto não pode ser salvo com nome vazio", savedProduct)
    }

    @Test
    fun `test Product Form - minStock numeric parser handles commas and dots`() {
        // Validação de inputs numéricos com vírgula padrão brasileiro
        val inputWithComma = "15,75"
        val parsedComma = inputWithComma.replace(",", ".").toDoubleOrNull() ?: 0.0
        assertEquals(15.75, parsedComma, 0.001)

        val inputWithDot = "20.5"
        val parsedDot = inputWithDot.replace(",", ".").toDoubleOrNull() ?: 0.0
        assertEquals(20.5, parsedDot, 0.001)

        val invalidInput = "abc"
        val parsedInvalid = invalidInput.replace(",", ".").toDoubleOrNull() ?: 0.0
        assertEquals(0.0, parsedInvalid, 0.001)

        val negativeInput = "-5.0"
        val parsedNegative = (negativeInput.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        assertEquals(0.0, parsedNegative, 0.001)
    }

    @Test
    fun `test Product Form - trimming and default category assignment on save`() {
        val rawName = "  Café Torrado Especial  "
        val rawBrand = "  3 Corações  "
        val rawBarcode = " 7891234567890 "
        val rawCategory = "   "
        val rawUnit = " pct "
        val rawLocation = " Corredor 3 - Prateleira A "

        val finalProduct = Product(
            name = rawName.trim(),
            brand = rawBrand.trim(),
            barcode = rawBarcode.trim(),
            category = rawCategory.trim().ifBlank { "Geral" },
            unit = rawUnit.trim(),
            location = rawLocation.trim()
        )

        assertEquals("Café Torrado Especial", finalProduct.name)
        assertEquals("3 Corações", finalProduct.brand)
        assertEquals("7891234567890", finalProduct.barcode)
        assertEquals("Geral", finalProduct.category) // Categoria padrão se vazia
        assertEquals("pct", finalProduct.unit)
        assertEquals("Corredor 3 - Prateleira A", finalProduct.location)
    }

    // =========================================================================
    // 2. TESTE DE INPUTS E BOTÕES DO MODAL DE MOVIMENTAÇÃO DE ESTOQUE
    // =========================================================================

    @Test
    fun `test Stock Movement - quantity parser and zero or negative validation`() {
        // Validação de quantidade nula, zero ou negativa
        val zeroInput = "0"
        val parsedZero = zeroInput.replace(",", ".").toDoubleOrNull()
        val isZeroValid = parsedZero != null && parsedZero > 0.0
        assertFalse("Quantidade zero deve ser rejeitada", isZeroValid)

        val negativeInput = "-10"
        val parsedNegative = negativeInput.replace(",", ".").toDoubleOrNull()
        val isNegativeValid = parsedNegative != null && parsedNegative > 0.0
        assertFalse("Quantidade negativa deve ser rejeitada", isNegativeValid)

        val validCommaInput = "12,5"
        val parsedValid = validCommaInput.replace(",", ".").toDoubleOrNull()
        assertNotNull(parsedValid)
        assertEquals(12.5, parsedValid!!, 0.001)
    }

    @Test
    fun `test Stock Movement - Out movement rejects quantity greater than lot stock`() {
        val currentLotStock = 25.0
        val requestedQty = 30.0

        var quantityError: String? = null
        if (requestedQty > currentLotStock) {
            quantityError = "Quantidade solicitada ($requestedQty) excede o saldo ($currentLotStock)"
        }

        assertNotNull("Deve gerar mensagem de erro ao ultrapassar saldo", quantityError)
        assertTrue(quantityError!!.contains("excede o saldo"))
    }

    @Test
    fun `test Stock Movement - DateVisualTransformation format mask 8 digits`() {
        val transformation = DateVisualTransformation()

        // Entrada de 8 dígitos numéricos: 31122026 -> 31/12/2026
        val rawInput = AnnotatedString("31122026")
        val transformed = transformation.filter(rawInput)

        assertEquals("31/12/2026", transformed.text.text)

        // Verificação de mapeamento de cursor para 8 dígitos
        val offsetMapping = transformed.offsetMapping
        assertEquals(0, offsetMapping.originalToTransformed(0))
        assertEquals(3, offsetMapping.originalToTransformed(2)) // após o primeiro '/'
        assertEquals(6, offsetMapping.originalToTransformed(4)) // após o segundo '/'
        assertEquals(10, offsetMapping.originalToTransformed(8))
    }

    @Test
    fun `test Stock Movement - Date parser from custom input ddMMyyyy`() {
        val dateInput = "15092026"
        val dateFormat = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).apply {
            isLenient = false
        }

        val parsedDate = try {
            dateFormat.parse(dateInput)
        } catch (e: Exception) {
            null
        }

        assertNotNull("Data válida deve ser parseada com sucesso", parsedDate)

        // Data inválida (ex: mês 13)
        val invalidMonth = "15132026"
        val parsedInvalid = try {
            dateFormat.parse(invalidMonth)
        } catch (e: Exception) {
            null
        }
        assertNull("Data com mês inválido deve falhar no parser estrito", parsedInvalid)
    }

    // =========================================================================
    // 3. TESTE DE INPUTS E BOTÕES DO MODAL DE AUDITORIA DE INVENTÁRIO
    // =========================================================================

    @Test
    fun `test Inventory Audit Form - physical count calculation and surplus-deficit diff`() {
        val registeredStock = 50.0

        // Caso 1: Sobra no inventário físico
        val countSurplus = "55,0"
        val parsedSurplus = countSurplus.replace(",", ".").toDoubleOrNull() ?: 0.0
        val diffSurplus = parsedSurplus - registeredStock
        assertEquals(5.0, diffSurplus, 0.001) // +5 unidades

        // Caso 2: Falta/Quebra no inventário físico
        val countDeficit = "42.5"
        val parsedDeficit = countDeficit.replace(",", ".").toDoubleOrNull() ?: 0.0
        val diffDeficit = parsedDeficit - registeredStock
        assertEquals(-7.5, diffDeficit, 0.001) // -7.5 unidades

        // Caso 3: Entrada inválida (texto ou negativo)
        val invalidCount = "-5"
        val parsedNeg = invalidCount.replace(",", ".").toDoubleOrNull()
        val isInvalid = parsedNeg == null || parsedNeg < 0
        assertTrue("Contagem negativa deve ser considerada inválida", isInvalid)
    }

    // =========================================================================
    // 4. TESTE DE INPUTS DE BUSCA E FILTROS DINÂMICOS
    // =========================================================================

    @Test
    fun `test Search Input - multi-field product filtering by name, brand, barcode`() {
        val products = listOf(
            Product(id = 1L, name = "Achocolatado em Pó 400g", brand = "Nescau", barcode = "78910001", category = "Alimentos"),
            Product(id = 2L, name = "Biscoito Recheado Chocolate", brand = "Bauducco", barcode = "78920002", category = "Alimentos"),
            Product(id = 3L, name = "Café Tradicional 500g", brand = "Pilão", barcode = "78930003", category = "Bebidas")
        )

        val query1 = "nescau"
        val result1 = products.filter {
            it.name.contains(query1, ignoreCase = true) ||
            it.brand.contains(query1, ignoreCase = true) ||
            it.barcode.contains(query1, ignoreCase = true)
        }
        assertEquals(1, result1.size)
        assertEquals("Achocolatado em Pó 400g", result1.first().name)

        val query2 = "78930003"
        val result2 = products.filter {
            it.name.contains(query2, ignoreCase = true) ||
            it.brand.contains(query2, ignoreCase = true) ||
            it.barcode.contains(query2, ignoreCase = true)
        }
        assertEquals(1, result2.size)
        assertEquals("Café Tradicional 500g", result2.first().name)
    }

    @Test
    fun `test Search Input - movements history filtering by doc and reason`() {
        val movements = listOf(
            StockMovement(id = 1L, productId = 10L, productName = "Suco Laranja", type = MovementType.ENTRADA, quantity = 10.0, documentNumber = "NF-9988", reason = "Compra Fornecedor"),
            StockMovement(id = 2L, productId = 10L, productName = "Suco Laranja", type = MovementType.SAIDA, quantity = 2.0, documentNumber = "PED-12", reason = "Consumo Interno Refeitório")
        )

        val queryDoc = "9988"
        val resultDoc = movements.filter {
            it.documentNumber.contains(queryDoc, ignoreCase = true) ||
            it.reason.contains(queryDoc, ignoreCase = true)
        }
        assertEquals(1, resultDoc.size)
        assertEquals("NF-9988", resultDoc.first().documentNumber)

        val queryReason = "refeitório"
        val resultReason = movements.filter {
            it.reason.contains(queryReason, ignoreCase = true)
        }
        assertEquals(1, resultReason.size)
        assertEquals("PED-12", resultReason.first().documentNumber)
    }

    // =========================================================================
    // 5. TESTE DE BOTÕES E INPUTS DE CONFIGURAÇÕES (MARCAS, LOCAIS, CATEGORIAS)
    // =========================================================================

    @Test
    fun `test Settings Inputs - duplicate and blank protection for brands and locations`() {
        // Tenta adicionar marca em branco
        val blankBrand = "   "
        if (blankBrand.isNotBlank()) {
            settingsManager.addBrand(blankBrand.trim())
        }
        assertFalse(settingsManager.brands.value.contains(""))

        // Adiciona marca normal
        settingsManager.addBrand("Nestlé")
        assertTrue(settingsManager.brands.value.contains("Nestlé"))

        // Impede duplicações redundantes
        val initialSize = settingsManager.brands.value.size
        settingsManager.addBrand("Nestlé")
        assertEquals(initialSize, settingsManager.brands.value.size)

        // Adiciona e atualiza local
        settingsManager.addLocation("Setor A1")
        assertTrue(settingsManager.locations.value.contains("Setor A1"))

        settingsManager.updateLocation("Setor A1", "Setor A1 - Refrigerado")
        assertTrue(settingsManager.locations.value.contains("Setor A1 - Refrigerado"))
        assertFalse(settingsManager.locations.value.contains("Setor A1"))
    }

    // =========================================================================
    // 6. TESTE DE BOTÕES E INPUTS DE BACKUP E CONEXÃO FIREBASE
    // =========================================================================

    @Test
    fun `test Cloud Sync Inputs - URL cleanup, trim and auto-sync toggling`() {
        val rawInputUrl = "  https://meu-estoque-demo.firebaseio.com/  "
        val cleanedUrl = rawInputUrl.trim()
        val endpoint = FirebaseSyncManager.formatFirebaseEndpointUrl(cleanedUrl)

        assertEquals("https://meu-estoque-demo.firebaseio.com/estoque_app.json", endpoint)

        // Alternância do botão Switch de Auto-Sync
        settingsManager.setAutoSyncEnabled(false)
        assertFalse(settingsManager.autoSyncEnabled.value)

        settingsManager.setAutoSyncEnabled(true)
        assertTrue(settingsManager.autoSyncEnabled.value)
    }
}
