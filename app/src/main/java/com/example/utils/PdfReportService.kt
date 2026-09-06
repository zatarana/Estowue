package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.MovementType
import com.example.data.model.Product
import com.example.data.model.ProductWithLots
import com.example.data.model.StockHealthStatus
import com.example.data.model.StockLot
import com.example.data.model.StockMovement
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportService {

    // A4 Dimensions in points (72 points per inch): 595 x 842
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 32f
    private const val MARGIN_RIGHT = 563f
    private const val CONTENT_WIDTH = MARGIN_RIGHT - MARGIN_LEFT // 531f

    // Color Palette
    private const val COLOR_HEADER_BG = 0xFF0F2A66.toInt() // Deep Navy / Royal Blue
    private const val COLOR_HEADER_ACCENT = 0xFF2563EB.toInt() // Blue Accent
    private const val COLOR_KPI_BG = 0xFFF1F5F9.toInt() // Slate 100
    private const val COLOR_KPI_BORDER = 0xFFCBD5E1.toInt() // Slate 300
    private const val COLOR_TEXT_TITLE = 0xFF0F172A.toInt() // Slate 900
    private const val COLOR_TEXT_BODY = 0xFF334155.toInt() // Slate 700
    private const val COLOR_TEXT_MUTED = 0xFF64748B.toInt() // Slate 500
    private const val COLOR_TH_BG = 0xFFE2E8F0.toInt() // Slate 200
    private const val COLOR_ROW_ALT = 0xFFF8FAFC.toInt() // Slate 50
    private const val COLOR_BORDER = 0xFFE2E8F0.toInt() // Slate 200

    // Badges / Alerts
    private const val COLOR_GREEN = 0xFF059669.toInt() // Emerald 600
    private const val COLOR_AMBER = 0xFFD97706.toInt() // Amber 600
    private const val COLOR_RED = 0xFFDC2626.toInt() // Red 600
    private const val COLOR_BLUE = 0xFF2563EB.toInt() // Blue 600
    private const val COLOR_PURPLE = 0xFF7C3AED.toInt() // Purple 600

    private val sdfDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    /**
     * Common method to draw header and footer for every page.
     */
    private fun drawPageDecoration(
        canvas: Canvas,
        paint: Paint,
        pageNumber: Int,
        reportTitle: String,
        generatedAt: String
    ) {
        // Top Header Banner
        paint.color = COLOR_HEADER_BG
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 62f, paint)

        // Blue accent underline
        paint.color = COLOR_HEADER_ACCENT
        canvas.drawRect(0f, 62f, PAGE_WIDTH.toFloat(), 65f, paint)

        // App/Company Title
        paint.color = Color.WHITE
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("GESTOR DE ESTOQUE & RASTREABILIDADE", MARGIN_LEFT, 26f, paint)

        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = 0xFFE2E8F0.toInt()
        canvas.drawText(reportTitle, MARGIN_LEFT, 44f, paint)

        // Generation Timestamp
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 8.5f
        paint.color = 0xFFCBD5E1.toInt()
        canvas.drawText("Emissão: $generatedAt", MARGIN_RIGHT, 34f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Page Footer Line
        paint.color = COLOR_BORDER
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN_LEFT, 808f, MARGIN_RIGHT, 808f, paint)

        // Footer Text
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Documento gerado automaticamente pelo aplicativo de Gestão de Estoque", MARGIN_LEFT, 822f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Página $pageNumber", MARGIN_RIGHT, 822f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    /**
     * Generates a complete inventory stock report PDF with detailed lot breakdown.
     */
    fun generateStockReportPdf(
        context: Context,
        products: List<ProductWithLots>,
        filterName: String = "Inventário Geral",
        subtitleFilter: String = ""
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val generatedAt = sdfDateTime.format(Date())
        val now = System.currentTimeMillis()

        val totalProducts = products.size
        val totalUnits = products.sumOf { it.totalQuantity }
        val allActiveLots = products.flatMap { it.lots.filter { lot -> lot.quantity > 0.001 } }
        val expiredLotsCount = allActiveLots.count { it.daysUntilExpiration(now) < 0 }
        val lowStockCount = products.count { it.isLowStock }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var pageNumber = 1
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas

        drawPageDecoration(canvas, paint, pageNumber, "Relatório Oficial de Inventário e Saldo de Produtos", generatedAt)

        var y = 78f

        // Report Title & Applied Filters
        paint.color = COLOR_TEXT_TITLE
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Relatório: $filterName", MARGIN_LEFT, y + 10f, paint)

        if (subtitleFilter.isNotBlank()) {
            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Filtros Ativos: $subtitleFilter", MARGIN_LEFT, y + 24f, paint)
            y += 32f
        } else {
            y += 20f
        }

        // Summary KPI Box (4 Columns)
        val kpiHeight = 36f
        paint.color = COLOR_KPI_BG
        canvas.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + kpiHeight), 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = COLOR_KPI_BORDER
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + kpiHeight), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        val colW = CONTENT_WIDTH / 4f

        // KPI 1: Total Produtos
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("PRODUTOS", MARGIN_LEFT + 8f, y + 12f, paint)
        paint.color = COLOR_TEXT_TITLE
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$totalProducts itens", MARGIN_LEFT + 8f, y + 27f, paint)

        // KPI 2: Volume Total
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("ESTOQUE TOTAL", MARGIN_LEFT + colW + 8f, y + 12f, paint)
        paint.color = COLOR_HEADER_ACCENT
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(String.format(Locale.getDefault(), "%.1f un", totalUnits), MARGIN_LEFT + colW + 8f, y + 27f, paint)

        // KPI 3: Abaixo do Mínimo
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("ABAIXO DO MÍNIMO", MARGIN_LEFT + (colW * 2) + 8f, y + 12f, paint)
        paint.color = if (lowStockCount > 0) COLOR_AMBER else COLOR_GREEN
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$lowStockCount produto(s)", MARGIN_LEFT + (colW * 2) + 8f, y + 27f, paint)

        // KPI 4: Lotes Vencidos
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("LOTES VENCIDOS", MARGIN_LEFT + (colW * 3) + 8f, y + 12f, paint)
        paint.color = if (expiredLotsCount > 0) COLOR_RED else COLOR_GREEN
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$expiredLotsCount lote(s)", MARGIN_LEFT + (colW * 3) + 8f, y + 27f, paint)

        y += kpiHeight + 12f

        fun drawProductTableHeader(c: Canvas, curY: Float) {
            paint.color = COLOR_TH_BG
            c.drawRoundRect(RectF(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 20f), 4f, 4f, paint)

            paint.color = COLOR_TEXT_TITLE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            val textY = curY + 14f
            c.drawText("PRODUTO / MARCA", MARGIN_LEFT + 8f, textY, paint)
            c.drawText("CATEGORIA", MARGIN_LEFT + 205f, textY, paint)
            c.drawText("SALDO TOTAL", MARGIN_LEFT + 325f, textY, paint)
            c.drawText("EST. MÍN.", MARGIN_LEFT + 415f, textY, paint)
            c.drawText("SITUAÇÃO", MARGIN_LEFT + 480f, textY, paint)
        }

        drawProductTableHeader(canvas, y)
        y += 24f

        var isAltRow = false

        for (item in products) {
            val product = item.product
            val activeLots = item.lots.filter { it.quantity > 0.001 }.sortedBy { it.expirationDate }
            val blockHeight = 22f + (if (activeLots.isNotEmpty()) activeLots.size * 15f + 4f else 4f)

            // Page Break Check
            if (y + blockHeight > 790f) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                drawPageDecoration(canvas, paint, pageNumber, "Relatório Oficial de Inventário e Saldo de Produtos", generatedAt)
                y = 78f
                drawProductTableHeader(canvas, y)
                y += 24f
            }

            val rowRect = RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + blockHeight)
            if (isAltRow) {
                paint.color = COLOR_ROW_ALT
                canvas.drawRoundRect(rowRect, 3f, 3f, paint)
            }
            paint.style = Paint.Style.STROKE
            paint.color = COLOR_BORDER
            paint.strokeWidth = 0.5f
            canvas.drawRoundRect(rowRect, 3f, 3f, paint)
            paint.style = Paint.Style.FILL

            val textBaseY = y + 14f

            // Product Name
            paint.color = COLOR_TEXT_TITLE
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val nameDisplay = if (product.name.length > 28) product.name.substring(0, 26) + "..." else product.name
            canvas.drawText(nameDisplay, MARGIN_LEFT + 8f, textBaseY, paint)

            // Product Brand
            if (product.brand.isNotBlank()) {
                paint.color = COLOR_TEXT_MUTED
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(" • ${product.brand}", MARGIN_LEFT + 8f + paint.measureText(nameDisplay) + 16f, textBaseY, paint)
            }

            // Category
            paint.color = COLOR_TEXT_BODY
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val catDisplay = if (product.category.length > 18) product.category.substring(0, 16) + ".." else product.category
            canvas.drawText(catDisplay, MARGIN_LEFT + 205f, textBaseY, paint)

            // Quantity
            paint.color = COLOR_TEXT_TITLE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val qtyDisplay = "${String.format(Locale.getDefault(), "%.2f", item.totalQuantity)} ${product.unit}"
            canvas.drawText(qtyDisplay, MARGIN_LEFT + 325f, textBaseY, paint)

            // Min Stock
            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val minStockDisplay = if (product.minStock > 0) "${String.format(Locale.getDefault(), "%.1f", product.minStock)} ${product.unit}" else "-"
            canvas.drawText(minStockDisplay, MARGIN_LEFT + 415f, textBaseY, paint)

            // Health Status Badge
            val statusColor = when (item.stockHealthStatus) {
                StockHealthStatus.OUT_OF_STOCK -> COLOR_RED
                StockHealthStatus.LOW_STOCK -> COLOR_AMBER
                StockHealthStatus.IN_STOCK -> COLOR_GREEN
            }

            val badgeRect = RectF(MARGIN_LEFT + 478f, y + 4f, MARGIN_LEFT + 555f, y + 17f)
            paint.color = statusColor
            paint.alpha = 30
            canvas.drawRoundRect(badgeRect, 3f, 3f, paint)
            paint.alpha = 255

            paint.color = statusColor
            paint.textSize = 7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(item.stockHealthStatus.label, MARGIN_LEFT + 484f, y + 13f, paint)

            // Active Lots Sub-rows
            if (activeLots.isNotEmpty()) {
                var lotY = y + 18f
                activeLots.forEach { lot ->
                    lotY += 14f
                    val days = lot.daysUntilExpiration(now)
                    val lotStatusColor = when {
                        days < 0 -> COLOR_RED
                        days <= 30 -> COLOR_AMBER
                        else -> COLOR_TEXT_MUTED
                    }

                    paint.color = COLOR_TEXT_MUTED
                    paint.textSize = 7.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                    val lotNum = if (lot.lotNumber.isNotBlank()) lot.lotNumber else "S/N"
                    val locInfo = if (lot.location.isNotBlank()) " | Local: ${lot.location}" else ""
                    val expDateStr = sdfDate.format(Date(lot.expirationDate))

                    val lineText = "↳ Lote: $lotNum | Qtd: ${String.format(Locale.getDefault(), "%.2f", lot.quantity)} ${product.unit} | Val: $expDateStr$locInfo"
                    canvas.drawText(lineText, MARGIN_LEFT + 14f, lotY, paint)

                    val expStatusText = when {
                        days < 0 -> " (VENCIDO há ${-days}d)"
                        days <= 30 -> " (Vence em ${days}d)"
                        else -> ""
                    }
                    if (expStatusText.isNotBlank()) {
                        paint.color = lotStatusColor
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(expStatusText, MARGIN_LEFT + 420f, lotY, paint)
                    }
                }
            }

            y += blockHeight + 3f
            isAltRow = !isAltRow
        }

        pdfDocument.finishPage(currentPage)
        return savePdfToFile(context, pdfDocument, "Inventario_${filterName}")
    }

    /**
     * Generates a dedicated FEFO Lots and Expirations report PDF.
     */
    fun generateFefoLotsReportPdf(
        context: Context,
        lotsWithProduct: List<Pair<Product, StockLot>>,
        filterName: String = "Controle de Validades (FEFO)",
        subtitleFilter: String = ""
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val generatedAt = sdfDateTime.format(Date())
        val now = System.currentTimeMillis()

        val totalLots = lotsWithProduct.size
        val totalQuantity = lotsWithProduct.sumOf { it.second.quantity }
        val expiredCount = lotsWithProduct.count { it.second.daysUntilExpiration(now) < 0 }
        val criticalCount = lotsWithProduct.count {
            val d = it.second.daysUntilExpiration(now)
            d in 0..15
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var pageNumber = 1
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas

        drawPageDecoration(canvas, paint, pageNumber, "Relatório Especial de Validades e Rastreabilidade (FEFO)", generatedAt)

        var y = 78f

        // Title & Filters
        paint.color = COLOR_TEXT_TITLE
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Relatório: $filterName", MARGIN_LEFT, y + 10f, paint)

        if (subtitleFilter.isNotBlank()) {
            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Filtros Ativos: $subtitleFilter", MARGIN_LEFT, y + 24f, paint)
            y += 32f
        } else {
            y += 20f
        }

        // Summary KPI Box
        val kpiHeight = 36f
        paint.color = COLOR_KPI_BG
        canvas.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + kpiHeight), 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = COLOR_KPI_BORDER
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + kpiHeight), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        val colW = CONTENT_WIDTH / 4f

        // KPI 1: Lotes
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("TOTAL DE LOTES", MARGIN_LEFT + 8f, y + 12f, paint)
        paint.color = COLOR_TEXT_TITLE
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$totalLots lotes", MARGIN_LEFT + 8f, y + 27f, paint)

        // KPI 2: Quantidade
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("VOLUME TOTAL", MARGIN_LEFT + colW + 8f, y + 12f, paint)
        paint.color = COLOR_HEADER_ACCENT
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(String.format(Locale.getDefault(), "%.1f un", totalQuantity), MARGIN_LEFT + colW + 8f, y + 27f, paint)

        // KPI 3: Vencidos
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("LOTES VENCIDOS", MARGIN_LEFT + (colW * 2) + 8f, y + 12f, paint)
        paint.color = if (expiredCount > 0) COLOR_RED else COLOR_GREEN
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$expiredCount lote(s)", MARGIN_LEFT + (colW * 2) + 8f, y + 27f, paint)

        // KPI 4: Críticos <= 15d
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("CRÍTICOS (≤15 DIAS)", MARGIN_LEFT + (colW * 3) + 8f, y + 12f, paint)
        paint.color = if (criticalCount > 0) COLOR_AMBER else COLOR_GREEN
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$criticalCount lote(s)", MARGIN_LEFT + (colW * 3) + 8f, y + 27f, paint)

        y += kpiHeight + 12f

        fun drawFefoTableHeader(c: Canvas, curY: Float) {
            paint.color = COLOR_TH_BG
            c.drawRoundRect(RectF(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 20f), 4f, 4f, paint)

            paint.color = COLOR_TEXT_TITLE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            val textY = curY + 14f
            c.drawText("PRODUTO / MARCA", MARGIN_LEFT + 6f, textY, paint)
            c.drawText("LOTE", MARGIN_LEFT + 175f, textY, paint)
            c.drawText("LOCAL", MARGIN_LEFT + 245f, textY, paint)
            c.drawText("VALIDADE", MARGIN_LEFT + 325f, textY, paint)
            c.drawText("SALDO", MARGIN_LEFT + 405f, textY, paint)
            c.drawText("SITUAÇÃO", MARGIN_LEFT + 475f, textY, paint)
        }

        drawFefoTableHeader(canvas, y)
        y += 24f

        var isAltRow = false

        for ((product, lot) in lotsWithProduct) {
            val rowHeight = 22f

            if (y + rowHeight > 790f) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                drawPageDecoration(canvas, paint, pageNumber, "Relatório Especial de Validades e Rastreabilidade (FEFO)", generatedAt)
                y = 78f
                drawFefoTableHeader(canvas, y)
                y += 24f
            }

            val rowRect = RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + rowHeight)
            if (isAltRow) {
                paint.color = COLOR_ROW_ALT
                canvas.drawRoundRect(rowRect, 3f, 3f, paint)
            }
            paint.style = Paint.Style.STROKE
            paint.color = COLOR_BORDER
            paint.strokeWidth = 0.5f
            canvas.drawRoundRect(rowRect, 3f, 3f, paint)
            paint.style = Paint.Style.FILL

            val textY = y + 14f
            val days = lot.daysUntilExpiration(now)

            // Product Name
            paint.color = COLOR_TEXT_TITLE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val nameDisplay = if (product.name.length > 24) product.name.substring(0, 22) + "..." else product.name
            canvas.drawText(nameDisplay, MARGIN_LEFT + 6f, textY, paint)

            // Lot
            paint.color = COLOR_TEXT_BODY
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val lotDisplay = if (lot.lotNumber.isNotBlank()) lot.lotNumber else "S/N"
            canvas.drawText(lotDisplay, MARGIN_LEFT + 175f, textY, paint)

            // Location
            val locDisplay = if (lot.location.isNotBlank()) {
                if (lot.location.length > 14) lot.location.substring(0, 12) + ".." else lot.location
            } else "-"
            canvas.drawText(locDisplay, MARGIN_LEFT + 245f, textY, paint)

            // Expiration Date
            val expDateDisplay = sdfDate.format(Date(lot.expirationDate))
            canvas.drawText(expDateDisplay, MARGIN_LEFT + 325f, textY, paint)

            // Quantity
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val qtyDisplay = "${String.format(Locale.getDefault(), "%.1f", lot.quantity)} ${product.unit}"
            canvas.drawText(qtyDisplay, MARGIN_LEFT + 405f, textY, paint)

            // Status Badge
            val (statusText, statusColor) = when {
                days < 0 -> "Vencido (${-days}d)" to COLOR_RED
                days <= 7 -> "Urgente (${days}d)" to COLOR_RED
                days <= 30 -> "Atenção (${days}d)" to COLOR_AMBER
                else -> "No Prazo (${days}d)" to COLOR_GREEN
            }

            val badgeRect = RectF(MARGIN_LEFT + 472f, y + 3f, MARGIN_LEFT + 558f, y + 18f)
            paint.color = statusColor
            paint.alpha = 30
            canvas.drawRoundRect(badgeRect, 3f, 3f, paint)
            paint.alpha = 255

            paint.color = statusColor
            paint.textSize = 6.8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(statusText, MARGIN_LEFT + 476f, y + 13.5f, paint)

            y += rowHeight + 2f
            isAltRow = !isAltRow
        }

        pdfDocument.finishPage(currentPage)
        return savePdfToFile(context, pdfDocument, "Validades_FEFO_${filterName}")
    }

    /**
     * Generates a complete movement history PDF report.
     */
    fun generateMovementsReportPdf(
        context: Context,
        movements: List<StockMovement>,
        filterName: String = "Histórico de Movimentações",
        subtitleFilter: String = ""
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val generatedAt = sdfDateTime.format(Date())

        val totalMovements = movements.size
        val totalIn = movements.filter { it.type == MovementType.ENTRADA }.sumOf { it.quantity }
        val totalOut = movements.filter { it.type == MovementType.SAIDA }.sumOf { it.quantity }
        val totalDiscards = movements.filter { it.type == MovementType.DESCARTE_VENCIDO }.sumOf { it.quantity }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var pageNumber = 1
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas

        drawPageDecoration(canvas, paint, pageNumber, "Relatório de Auditoria e Movimentações de Estoque", generatedAt)

        var y = 78f

        // Title & Filter
        paint.color = COLOR_TEXT_TITLE
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Relatório: $filterName", MARGIN_LEFT, y + 10f, paint)

        if (subtitleFilter.isNotBlank()) {
            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Filtros Ativos: $subtitleFilter", MARGIN_LEFT, y + 24f, paint)
            y += 32f
        } else {
            y += 20f
        }

        // Summary KPI Box
        val kpiHeight = 36f
        paint.color = COLOR_KPI_BG
        canvas.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + kpiHeight), 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = COLOR_KPI_BORDER
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + kpiHeight), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        val colW = CONTENT_WIDTH / 4f

        // KPI 1: Registros
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("TOTAL REGISTROS", MARGIN_LEFT + 8f, y + 12f, paint)
        paint.color = COLOR_TEXT_TITLE
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$totalMovements ops", MARGIN_LEFT + 8f, y + 27f, paint)

        // KPI 2: Total Entradas
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("TOTAL ENTRADAS", MARGIN_LEFT + colW + 8f, y + 12f, paint)
        paint.color = COLOR_GREEN
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(String.format(Locale.getDefault(), "+%.1f un", totalIn), MARGIN_LEFT + colW + 8f, y + 27f, paint)

        // KPI 3: Total Saídas
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("TOTAL SAÍDAS", MARGIN_LEFT + (colW * 2) + 8f, y + 12f, paint)
        paint.color = COLOR_HEADER_ACCENT
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(String.format(Locale.getDefault(), "-%.1f un", totalOut), MARGIN_LEFT + (colW * 2) + 8f, y + 27f, paint)

        // KPI 4: Descartes
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("DESCARTE VENCIDO", MARGIN_LEFT + (colW * 3) + 8f, y + 12f, paint)
        paint.color = if (totalDiscards > 0) COLOR_RED else COLOR_TEXT_MUTED
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(String.format(Locale.getDefault(), "-%.1f un", totalDiscards), MARGIN_LEFT + (colW * 3) + 8f, y + 27f, paint)

        y += kpiHeight + 12f

        fun drawMovementsTableHeader(c: Canvas, curY: Float) {
            paint.color = COLOR_TH_BG
            c.drawRoundRect(RectF(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 20f), 4f, 4f, paint)

            paint.color = COLOR_TEXT_TITLE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            val textY = curY + 14f
            c.drawText("DATA / HORA", MARGIN_LEFT + 6f, textY, paint)
            c.drawText("PRODUTO / MARCA", MARGIN_LEFT + 95f, textY, paint)
            c.drawText("TIPO", MARGIN_LEFT + 265f, textY, paint)
            c.drawText("QTD", MARGIN_LEFT + 345f, textY, paint)
            c.drawText("LOTE / DOC", MARGIN_LEFT + 405f, textY, paint)
            c.drawText("MOTIVO", MARGIN_LEFT + 480f, textY, paint)
        }

        drawMovementsTableHeader(canvas, y)
        y += 24f

        var isAltRow = false

        for (mov in movements) {
            val rowHeight = 22f

            if (y + rowHeight > 790f) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                drawPageDecoration(canvas, paint, pageNumber, "Relatório de Auditoria e Movimentações de Estoque", generatedAt)
                y = 78f
                drawMovementsTableHeader(canvas, y)
                y += 24f
            }

            val rowRect = RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + rowHeight)
            if (isAltRow) {
                paint.color = COLOR_ROW_ALT
                canvas.drawRoundRect(rowRect, 3f, 3f, paint)
            }
            paint.style = Paint.Style.STROKE
            paint.color = COLOR_BORDER
            paint.strokeWidth = 0.5f
            canvas.drawRoundRect(rowRect, 3f, 3f, paint)
            paint.style = Paint.Style.FILL

            val textY = y + 14f

            // Date
            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val dateStr = sdfDateTime.format(Date(mov.timestamp))
            canvas.drawText(dateStr, MARGIN_LEFT + 6f, textY, paint)

            // Product Name
            paint.color = COLOR_TEXT_TITLE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val prodStr = if (mov.productName.length > 24) mov.productName.substring(0, 22) + ".." else mov.productName
            canvas.drawText(prodStr, MARGIN_LEFT + 95f, textY, paint)

            // Type Badge
            val (typeColor, typeLabel) = when (mov.type) {
                MovementType.ENTRADA -> COLOR_GREEN to "Entrada (+)"
                MovementType.SAIDA -> COLOR_BLUE to "Saída (-)"
                MovementType.DESCARTE_VENCIDO -> COLOR_RED to "Descarte"
                MovementType.AJUSTE -> COLOR_AMBER to "Ajuste"
                MovementType.TRANSFERENCIA -> COLOR_PURPLE to "Transf."
            }

            val typeBadge = RectF(MARGIN_LEFT + 262f, y + 3f, MARGIN_LEFT + 338f, y + 18f)
            paint.color = typeColor
            paint.alpha = 28
            canvas.drawRoundRect(typeBadge, 3f, 3f, paint)
            paint.alpha = 255

            paint.color = typeColor
            paint.textSize = 7.2f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(typeLabel, MARGIN_LEFT + 267f, y + 13.5f, paint)

            // Quantity
            paint.color = if (mov.type.isPositive) COLOR_GREEN else COLOR_TEXT_TITLE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val prefix = if (mov.type.isPositive) "+" else "-"
            val qtyStr = "$prefix${String.format(Locale.getDefault(), "%.1f", mov.quantity)}"
            canvas.drawText(qtyStr, MARGIN_LEFT + 345f, textY, paint)

            // Lot / Doc
            paint.color = COLOR_TEXT_BODY
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val lotDoc = if (mov.lotNumber.isNotBlank()) "Lt: ${mov.lotNumber}" else if (mov.documentNumber.isNotBlank()) "Doc: ${mov.documentNumber}" else "-"
            val lotDocTrunc = if (lotDoc.length > 12) lotDoc.substring(0, 10) + ".." else lotDoc
            canvas.drawText(lotDocTrunc, MARGIN_LEFT + 405f, textY, paint)

            // Reason
            val reasonStr = if (mov.reason.isNotBlank()) mov.reason else "-"
            val reasonTrunc = if (reasonStr.length > 14) reasonStr.substring(0, 12) + ".." else reasonStr
            canvas.drawText(reasonTrunc, MARGIN_LEFT + 480f, textY, paint)

            y += rowHeight + 2f
            isAltRow = !isAltRow
        }

        pdfDocument.finishPage(currentPage)
        return savePdfToFile(context, pdfDocument, "Movimentacoes_${filterName}")
    }

    private fun savePdfToFile(context: Context, pdfDocument: PdfDocument, baseFileName: String): File? {
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
            ?: context.cacheDir

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val cleanName = baseFileName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(outputDir, "${cleanName}_${System.currentTimeMillis()}.pdf")

        return try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                pdfDocument.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Helper to open or share the generated PDF directly with one touch.
     */
    fun openOrSharePdf(context: Context, file: File, title: String = "Relatório de Estoque") {
        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao obter URI do PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        }

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Visualizar ou Enviar $title").apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            try {
                context.startActivity(viewIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Nenhum visualizador de PDF instalado no aparelho.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
