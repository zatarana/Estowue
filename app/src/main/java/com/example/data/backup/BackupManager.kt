package com.example.data.backup

import android.content.Context
import android.content.Intent
import kotlin.math.abs
import com.example.data.model.Category
import com.example.data.model.MovementType
import com.example.data.model.Product
import com.example.data.model.ProductWithLots
import com.example.data.model.StockLot
import com.example.data.model.StockMovement
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportStockFilter(val label: String) {
    ALL("Todos os Produtos"),
    LOW_STOCK("Apenas Abaixo do Mínimo"),
    EXPIRED("Apenas Lotes Vencidos"),
    EXPIRING_SOON("Apenas Próximos a Vencer"),
    POSITIVE_STOCK("Apenas com Saldo em Estoque")
}

object BackupManager {

    /**
     * Filters productsWithLots according to user criteria.
     */
    fun filterDataForExport(
        productsWithLots: List<ProductWithLots>,
        selectedCategory: String? = null,
        stockFilter: ExportStockFilter = ExportStockFilter.ALL,
        selectedProductId: Long? = null,
        selectedBrand: String? = null,
        selectedLocation: String? = null,
        alertDays: Int = 30,
        now: Long = System.currentTimeMillis()
    ): List<ProductWithLots> {
        return productsWithLots.filter { pWithLots ->
            val p = pWithLots.product
            val matchesCategory = selectedCategory == null || p.category.equals(selectedCategory, ignoreCase = true)
            val matchesProduct = selectedProductId == null || p.id == selectedProductId
            val matchesBrand = selectedBrand == null || p.brand.equals(selectedBrand, ignoreCase = true)
            val activeLots = pWithLots.lots.filter { it.quantity > 0.001 }
            val matchesLocation = selectedLocation == null ||
                if (activeLots.isNotEmpty()) {
                    activeLots.any { (it.location.ifBlank { p.location }).equals(selectedLocation, ignoreCase = true) }
                } else {
                    p.location.equals(selectedLocation, ignoreCase = true)
                }
            
            val matchesStock = when (stockFilter) {
                ExportStockFilter.ALL -> true
                ExportStockFilter.LOW_STOCK -> pWithLots.isLowStock
                ExportStockFilter.EXPIRED -> pWithLots.lots.any { it.quantity > 0.001 && it.daysUntilExpiration(now) < 0 }
                ExportStockFilter.EXPIRING_SOON -> pWithLots.lots.any { it.quantity > 0.001 && it.daysUntilExpiration(now) in 0..alertDays }
                ExportStockFilter.POSITIVE_STOCK -> pWithLots.totalQuantity > 0.001
            }
            matchesCategory && matchesProduct && matchesBrand && matchesLocation && matchesStock
        }
    }

    /**
     * Filters movements according to period in days and other filters.
     */
    fun filterMovements(
        movements: List<StockMovement>,
        periodDays: Int? = null,
        movementType: MovementType? = null,
        selectedBrand: String? = null,
        selectedProductId: Long? = null,
        selectedLocation: String? = null,
        discardOnly: Boolean = false,
        now: Long = System.currentTimeMillis()
    ): List<StockMovement> {
        return movements.filter { mov ->
            val matchesPeriod = if (periodDays == null) true else mov.timestamp >= (now - (periodDays.toLong() * 86400000L))
            val matchesType = if (movementType == null) true else mov.type == movementType
            val matchesBrand = if (selectedBrand == null) true else mov.productBrand.equals(selectedBrand, ignoreCase = true)
            val matchesProduct = if (selectedProductId == null) true else mov.productId == selectedProductId
            val matchesLocation = if (selectedLocation == null) true else (
                mov.reason.contains(selectedLocation, ignoreCase = true) ||
                mov.notes.contains(selectedLocation, ignoreCase = true)
            )
            val matchesDiscard = if (!discardOnly) true else (
                mov.type == MovementType.DESCARTE_VENCIDO || mov.reason.contains("descarte", ignoreCase = true)
            )
            
            matchesPeriod && matchesType && matchesBrand && matchesProduct && matchesLocation && matchesDiscard
        }
    }

    /**
     * Serializes inventory data into a structured JSON string with optional filters.
     */
    fun createBackupJson(
        products: List<Product>,
        categories: List<Category>,
        lots: List<StockLot>,
        movements: List<StockMovement>,
        filterLabel: String = "Completo"
    ): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("appName", "Controle de Estoque com Validades")
        root.put("filterApplied", filterLabel)
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        root.put("timestamp", System.currentTimeMillis())

        // Categories
        val catsArray = JSONArray()
        categories.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("colorHex", c.colorHex)
            obj.put("description", c.description)
            catsArray.put(obj)
        }
        root.put("categories", catsArray)

        // Products
        val prodsArray = JSONArray()
        products.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("brand", p.brand)
            obj.put("barcode", p.barcode)
            obj.put("category", p.category)
            obj.put("unit", p.unit)
            obj.put("minStock", p.minStock)
            obj.put("location", p.location)
            obj.put("description", p.description)
            obj.put("lastUpdated", p.lastUpdated)
            prodsArray.put(obj)
        }
        root.put("products", prodsArray)

        // Lots
        val lotsArray = JSONArray()
        lots.forEach { l ->
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("productId", l.productId)
            obj.put("quantity", l.quantity)
            obj.put("initialQuantity", l.initialQuantity)
            obj.put("expirationDate", l.expirationDate)
            obj.put("manufacturingDate", l.manufacturingDate ?: 0L)
            obj.put("location", l.location)
            obj.put("notes", l.notes)
            obj.put("createdAt", l.createdAt)
            lotsArray.put(obj)
        }
        root.put("lots", lotsArray)

        // Movements
        val movsArray = JSONArray()
        movements.forEach { m ->
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("productId", m.productId)
            obj.put("productName", m.productName)
            obj.put("productBrand", m.productBrand)
            obj.put("lotId", m.lotId ?: 0L)
            obj.put("lotExpirationDate", m.lotExpirationDate ?: 0L)
            obj.put("type", m.type.name)
            obj.put("quantity", m.quantity)
            obj.put("previousLotStock", m.previousLotStock)
            obj.put("resultingLotStock", m.resultingLotStock)
            obj.put("previousTotalStock", m.previousTotalStock)
            obj.put("resultingTotalStock", m.resultingTotalStock)
            obj.put("reason", m.reason)
            obj.put("documentNumber", m.documentNumber)
            obj.put("notes", m.notes)
            obj.put("timestamp", m.timestamp)
            movsArray.put(obj)
        }
        root.put("movements", movsArray)

        return root.toString(2)
    }

    /**
     * Generates a clean CSV file format representing filtered inventory status with lots and minStock.
     */
    fun createInventoryCsv(
        productsWithLots: List<ProductWithLots>,
        alertDays: Int = 30
    ): String {
        val sb = StringBuilder()
        sb.appendLine("ID;Produto;Marca;Código de Barras;Categoria;Unidade;Estoque Mínimo;Localização;Lote;Validade;Dias Restantes;Status Validade;Status Estoque;Saldo em Estoque")

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val now = System.currentTimeMillis()

        productsWithLots.forEach { pWithLots ->
            val p = pWithLots.product
            val healthLabel = pWithLots.stockHealthStatus.label
            val activeLots = pWithLots.lots.filter { it.quantity > 0.001 }

            if (activeLots.isEmpty()) {
                sb.appendLine(
                    "${p.id};\"${p.name}\";\"${p.brand}\";\"${p.barcode}\";\"${p.category}\";\"${p.unit}\";${p.minStock};\"${p.location}\";\"Sem Lote\";\"Sem Lote\";\"-\";\"Sem Saldo\";\"$healthLabel\";0"
                )
            } else {
                activeLots.forEach { lot ->
                    val dateStr = dateFormat.format(Date(lot.expirationDate))
                    val days = lot.daysUntilExpiration(now)
                    val statusStr = lot.getStatus(now, alertDays).label
                    val lotStr = if (lot.lotNumber.isNotBlank()) lot.lotNumber else "S/N"
                    sb.appendLine(
                        "${p.id};\"${p.name}\";\"${p.brand}\";\"${p.barcode}\";\"${p.category}\";\"${p.unit}\";${p.minStock};\"${lot.location.ifBlank { p.location }}\";\"$lotStr\";\"$dateStr\";$days;\"$statusStr\";\"$healthLabel\";${lot.quantity}"
                    )
                }
            }
        }
        return sb.toString()
    }

    /**
     * Generates a comprehensive formatted text report for executive sharing with custom filters.
     */
    fun createExecutiveSummaryReport(
        productsWithLots: List<ProductWithLots>,
        movements: List<StockMovement>,
        alertDays: Int = 30,
        filterDescription: String = "Todos os Registros"
    ): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val now = System.currentTimeMillis()
        val activeLots = productsWithLots.flatMap { it.lots }.filter { it.quantity > 0.001 }

        val expired = activeLots.filter { it.daysUntilExpiration(now) < 0 }
        val critical = activeLots.filter { it.daysUntilExpiration(now) in 0..(alertDays / 2).coerceAtLeast(7) }
        val warning = activeLots.filter { it.daysUntilExpiration(now) in ((alertDays / 2).coerceAtLeast(7) + 1)..alertDays }
        val lowStockProducts = productsWithLots.filter { it.isLowStock }

        val sb = StringBuilder()
        sb.appendLine("==============================================")
        sb.appendLine("📦 RELATÓRIO DE ESTOQUE & VALIDADES")
        sb.appendLine("Emitido em: ${dateFormat.format(Date(now))}")
        sb.appendLine("Filtro Aplicado: $filterDescription")
        sb.appendLine("==============================================")
        sb.appendLine("\n📊 RESUMO EXECUTIVO:")
        sb.appendLine("• Produtos Listados: ${productsWithLots.size}")
        sb.appendLine("• Volume Total em Estoque: ${String.format(Locale.getDefault(), "%.2f", activeLots.sumOf { it.quantity })}")
        sb.appendLine("• Lotes Ativos com Saldo: ${activeLots.size}")
        sb.appendLine("• Produtos Abaixo do Mínimo: ${lowStockProducts.size}")
        sb.appendLine("• Lotes Vencidos: ${expired.size}")
        sb.appendLine("• Lotes em Alerta (< $alertDays dias): ${critical.size + warning.size}")

        if (lowStockProducts.isNotEmpty()) {
            sb.appendLine("\n⚠️ PRODUTOS ABAIXO DO ESTOQUE MÍNIMO (REPOSIÇÃO NECESSÁRIA):")
            lowStockProducts.forEach { pWithLots ->
                val p = pWithLots.product
                val brandStr = if (p.brand.isNotBlank()) " (${p.brand})" else ""
                sb.appendLine("  🚨 ${p.name}$brandStr: Saldo Atual ${pWithLots.totalQuantity} ${p.unit} | Mínimo Configurado: ${p.minStock} ${p.unit}")
            }
        }

        if (expired.isNotEmpty()) {
            sb.appendLine("\n🚨 LOTES VENCIDOS (DESCARTE RECOMENDADO):")
            expired.forEach { lot ->
                val prod = productsWithLots.find { it.product.id == lot.productId }?.product
                val pName = prod?.name ?: "Produto #${lot.productId}"
                val pBrand = if (!prod?.brand.isNullOrBlank()) " (${prod?.brand})" else ""
                val lotStr = if (lot.lotNumber.isNotBlank()) " (Lote: ${lot.lotNumber})" else ""
                sb.appendLine("  ❌ $pName$pBrand$lotStr: Saldo ${lot.quantity} ${prod?.unit ?: "un"} - Venceu em: ${dateOnlyFormat.format(Date(lot.expirationDate))}")
            }
        }

        if (critical.isNotEmpty()) {
            sb.appendLine("\n⏰ LOTES PRÓXIMOS A VENCER (PRIORIZAR SAÍDA/FEFO):")
            critical.sortedBy { it.expirationDate }.forEach { lot ->
                val prod = productsWithLots.find { it.product.id == lot.productId }?.product
                val pName = prod?.name ?: "Produto #${lot.productId}"
                val pBrand = if (!prod?.brand.isNullOrBlank()) " (${prod?.brand})" else ""
                val days = lot.daysUntilExpiration(now)
                val lotStr = if (lot.lotNumber.isNotBlank()) " (Lote: ${lot.lotNumber})" else ""
                sb.appendLine("  ⏳ $pName$pBrand$lotStr: ${lot.quantity} ${prod?.unit ?: "un"} - Vence em $days dias (${dateOnlyFormat.format(Date(lot.expirationDate))})")
            }
        }

        sb.appendLine("\n----------------------------------------------")
        sb.appendLine("📋 INVENTÁRIO DETALHADO:")
        productsWithLots.forEach { pWithLots ->
            val p = pWithLots.product
            val brandStr = if (p.brand.isNotBlank()) " [${p.brand}]" else ""
            val barcodeStr = if (p.barcode.isNotBlank()) " (EAN: ${p.barcode})" else ""
            val minStockStr = if (p.minStock > 0) " | Mínimo: ${p.minStock} ${p.unit}" else ""
            sb.appendLine("\n• ${p.name}$brandStr$barcodeStr - Categoria: ${p.category}")
            sb.appendLine("  Saldo Total: ${pWithLots.totalQuantity} ${p.unit}$minStockStr | Status: ${pWithLots.stockHealthStatus.label}")
            if (p.location.isNotBlank()) sb.appendLine("  Localização: ${p.location}")

            val pLots = pWithLots.lots.filter { it.quantity > 0.001 }
            if (pLots.isEmpty()) {
                sb.appendLine("  (Sem saldo em estoque)")
            } else {
                pLots.sortedBy { it.expirationDate }.forEach { lot ->
                    val d = lot.daysUntilExpiration(now)
                    val statusTxt = when {
                        d < 0 -> "❌ VENCIDO (${abs(d)}d atrás)"
                        d <= (alertDays / 2).coerceAtLeast(7) -> "⚠️ CRÍTICO ($d dias)"
                        d <= alertDays -> "⏰ ATENÇÃO ($d dias)"
                        else -> "✅ Válido ($d dias)"
                    }
                    val loc = if (lot.location.isNotBlank()) " | Local: ${lot.location}" else ""
                    sb.appendLine("    - Saldo: ${lot.quantity} ${p.unit} | Venc: ${dateOnlyFormat.format(Date(lot.expirationDate))} | $statusTxt$loc")
                }
            }
        }

        if (movements.isNotEmpty()) {
            sb.appendLine("\n----------------------------------------------")
            sb.appendLine("🔄 RESUMO DE MOVIMENTAÇÕES (NO PERÍODO/FILTRO):")
            val entradas = movements.filter { it.type == MovementType.ENTRADA }
            val saidas = movements.filter { it.type == MovementType.SAIDA || it.type == MovementType.DESCARTE_VENCIDO }
            val transferencias = movements.filter { it.type == MovementType.TRANSFERENCIA }
            
            sb.appendLine("• Total de Entradas: ${String.format(Locale.getDefault(), "%.2f", entradas.sumOf { it.quantity })} un (${entradas.size} registros)")
            sb.appendLine("• Total de Saídas/Descartes: ${String.format(Locale.getDefault(), "%.2f", saidas.sumOf { it.quantity })} un (${saidas.size} registros)")
            if (transferencias.isNotEmpty()) {
                sb.appendLine("• Total de Transferências Internas: ${transferencias.size} registros")
            }
        }

        sb.appendLine("\n==============================================")
        sb.appendLine("Fim do Relatório.")
        return sb.toString()
    }

    /**
     * Shares or saves data to Google Drive, WhatsApp, Files, or email via standard Android Intent.
     */
    fun shareContent(context: Context, text: String, title: String, mimeType: String = "text/plain") {
        if (mimeType == "text/plain") {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_SUBJECT, title)
                type = mimeType
            }
            val shareIntent = Intent.createChooser(sendIntent, title)
            context.startActivity(shareIntent)
            return
        }

        try {
            val file = java.io.File(context.cacheDir, title)
            file.writeText(text, Charsets.UTF_8)
            
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                type = mimeType
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            val shareIntent = Intent.createChooser(sendIntent, title)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Erro ao exportar arquivo", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

