package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.MovementType
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

    // A4 Dimensions in points (72 points per inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 559f
    private const val CONTENT_WIDTH = MARGIN_RIGHT - MARGIN_LEFT // 523f

    // Primary Colors
    private const val COLOR_PRIMARY = 0xFF1E3A8A.toInt() // Dark Royal Blue #1E3A8A
    private const val COLOR_PRIMARY_LIGHT = 0xFFEFF6FF.toInt() // #EFF6FF
    private const val COLOR_SECONDARY = 0xFF0D9488.toInt() // Teal #0D9488
    private const val COLOR_TEXT_DARK = 0xFF1E293B.toInt() // Slate 800 #1E293B
    private const val COLOR_TEXT_MUTED = 0xFF64748B.toInt() // Slate 500 #64748B
    private const val COLOR_BG_HEADER = 0xFFF1F5F9.toInt() // Slate 100 #F1F5F9
    private const val COLOR_BG_ROW_ALT = 0xFFF8FAFC.toInt() // Slate 50 #F8FAFC
    private const val COLOR_BORDER = 0xFFE2E8F0.toInt() // Slate 200 #E2E8F0
    private const val COLOR_GREEN = 0xFF10B981.toInt()
    private const val COLOR_AMBER = 0xFFF59E0B.toInt()
    private const val COLOR_RED = 0xFFEF4444.toInt()

    /**
     * Generates a beautifully formatted, multi-page, professional PDF Stock Report.
     */
    fun generateStockReportPdf(
        context: Context,
        products: List<ProductWithLots>,
        filterName: String = "Inventário Completo",
        subtitleFilter: String = ""
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()

        val sdfDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val generatedAt = sdfDateTime.format(Date())

        val totalProducts = products.size
        val totalStockUnits = products.sumOf { it.totalQuantity }
        val allActiveLots = products.flatMap { it.lots.filter { lot -> lot.quantity > 0.001 } }
        val now = System.currentTimeMillis()
        val expiredCount = allActiveLots.count { it.daysUntilExpiration(now) < 0 }
        val lowStockCount = products.count { it.isLowStock }

        // Paints
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        var pageNumber = 1
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas

        fun drawHeaderAndFooter(c: Canvas, pNum: Int) {
            // Header Top Bar
            paint.color = COLOR_PRIMARY
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 64f, paint)

            // Header Title
            paint.color = Color.WHITE
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            c.drawText("GESTOR DE ESTOQUE & VALIDADES", MARGIN_LEFT, 28f, paint)

            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = 0xFFE0E7FF.toInt()
            c.drawText("Relatório Oficial de Inventário e Rastreabilidade (FEFO)", MARGIN_LEFT, 46f, paint)

            // Date in top right
            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = 9f
            paint.color = 0xFFE0E7FF.toInt()
            c.drawText("Gerado em: $generatedAt", MARGIN_RIGHT, 38f, paint)
            paint.textAlign = Paint.Align.LEFT

            // Footer
            paint.color = COLOR_BORDER
            c.drawLine(MARGIN_LEFT, 810f, MARGIN_RIGHT, 810f, paint)

            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            c.drawText("Documento gerado automaticamente pelo aplicativo de Gestão de Estoque", MARGIN_LEFT, 825f, paint)

            paint.textAlign = Paint.Align.RIGHT
            c.drawText("Página $pNum", MARGIN_RIGHT, 825f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        fun drawTableHeader(c: Canvas, curY: Float) {
            paint.color = COLOR_BG_HEADER
            c.drawRoundRect(RectF(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 22f), 4f, 4f, paint)

            paint.color = COLOR_TEXT_DARK
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            val textY = curY + 15f
            c.drawText("PRODUTO / MARCA", MARGIN_LEFT + 8f, textY, paint)
            c.drawText("CATEGORIA", MARGIN_LEFT + 200f, textY, paint)
            c.drawText("SALDO TOTAL", MARGIN_LEFT + 320f, textY, paint)
            c.drawText("EST. MÍN.", MARGIN_LEFT + 410f, textY, paint)
            c.drawText("SITUAÇÃO", MARGIN_LEFT + 475f, textY, paint)
        }

        // Draw First Page Header
        drawHeaderAndFooter(canvas, pageNumber)

        var y = 78f

        // Title and Filter Badges
        paint.color = COLOR_PRIMARY
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Relatório: $filterName", MARGIN_LEFT, y + 12f, paint)

        if (subtitleFilter.isNotBlank()) {
            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Filtros Aplicados: $subtitleFilter", MARGIN_LEFT, y + 26f, paint)
            y += 34f
        } else {
            y += 22f
        }

        // Executive Summary KPI Box (Single Row Cards)
        paint.color = COLOR_PRIMARY_LIGHT
        canvas.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + 42f), 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = 0xFFBFDBFE.toInt()
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + 42f), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // KPI Columns
        val kpiWidth = CONTENT_WIDTH / 4f

        // KPI 1: Total Products
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("TOTAL PRODUTOS", MARGIN_LEFT + 10f, y + 14f, paint)
        paint.color = COLOR_PRIMARY
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$totalProducts itens", MARGIN_LEFT + 10f, y + 32f, paint)

        // KPI 2: Total Volume
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("VOLUME EM ESTOQUE", MARGIN_LEFT + kpiWidth + 10f, y + 14f, paint)
        paint.color = COLOR_PRIMARY
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(String.format(Locale.getDefault(), "%.1f un", totalStockUnits), MARGIN_LEFT + kpiWidth + 10f, y + 32f, paint)

        // KPI 3: Below Minimum
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("ABAIXO DO MÍNIMO", MARGIN_LEFT + (kpiWidth * 2) + 10f, y + 14f, paint)
        paint.color = if (lowStockCount > 0) COLOR_AMBER else COLOR_SECONDARY
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$lowStockCount produto(s)", MARGIN_LEFT + (kpiWidth * 2) + 10f, y + 32f, paint)

        // KPI 4: Expired Lots
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("LOTES VENCIDOS", MARGIN_LEFT + (kpiWidth * 3) + 10f, y + 14f, paint)
        paint.color = if (expiredCount > 0) COLOR_RED else COLOR_GREEN
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$expiredCount lote(s)", MARGIN_LEFT + (kpiWidth * 3) + 10f, y + 32f, paint)

        y += 54f

        // Initial Table Header
        drawTableHeader(canvas, y)
        y += 26f

        var isAltRow = false

        for (item in products) {
            val product = item.product
            val activeLots = item.lots.filter { it.quantity > 0.001 }.sortedBy { it.expirationDate }
            val rowsNeeded = 1 + (if (activeLots.isNotEmpty()) activeLots.size else 0)
            val blockHeight = 22f + (if (activeLots.isNotEmpty()) activeLots.size * 16f + 4f else 4f)

            // Page Break Check
            if (y + blockHeight > 790f) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                drawHeaderAndFooter(canvas, pageNumber)
                y = 78f
                drawTableHeader(canvas, y)
                y += 26f
            }

            // Draw Product Row Background
            val rowRect = RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + blockHeight)
            if (isAltRow) {
                paint.color = COLOR_BG_ROW_ALT
                canvas.drawRoundRect(rowRect, 3f, 3f, paint)
            }
            paint.style = Paint.Style.STROKE
            paint.color = COLOR_BORDER
            paint.strokeWidth = 0.5f
            canvas.drawRoundRect(rowRect, 3f, 3f, paint)
            paint.style = Paint.Style.FILL

            val textBaseY = y + 15f

            // Product Name & Brand
            paint.color = COLOR_TEXT_DARK
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val nameDisplay = if (product.name.length > 28) product.name.substring(0, 26) + "..." else product.name
            canvas.drawText(nameDisplay, MARGIN_LEFT + 8f, textBaseY, paint)

            if (product.brand.isNotBlank()) {
                paint.color = COLOR_TEXT_MUTED
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(" • ${product.brand}", MARGIN_LEFT + 8f + paint.measureText(nameDisplay) + 20f, textBaseY, paint)
            }

            // Category
            paint.color = COLOR_TEXT_DARK
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val catDisplay = if (product.category.length > 18) product.category.substring(0, 16) + ".." else product.category
            canvas.drawText(catDisplay, MARGIN_LEFT + 200f, textBaseY, paint)

            // Total Stock Quantity
            paint.color = COLOR_TEXT_DARK
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val qtyDisplay = "${String.format(Locale.getDefault(), "%.2f", item.totalQuantity)} ${product.unit}"
            canvas.drawText(qtyDisplay, MARGIN_LEFT + 320f, textBaseY, paint)

            // Min Stock
            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val minStockDisplay = if (product.minStock > 0) "${String.format(Locale.getDefault(), "%.1f", product.minStock)} ${product.unit}" else "-"
            canvas.drawText(minStockDisplay, MARGIN_LEFT + 410f, textBaseY, paint)

            // Health Status Badge
            val statusColor = when (item.stockHealthStatus) {
                StockHealthStatus.OUT_OF_STOCK -> COLOR_RED
                StockHealthStatus.LOW_STOCK -> COLOR_AMBER
                StockHealthStatus.IN_STOCK -> COLOR_GREEN
            }

            val badgeRect = RectF(MARGIN_LEFT + 475f, y + 4f, MARGIN_LEFT + 550f, y + 18f)
            paint.color = statusColor
            paint.alpha = 35
            canvas.drawRoundRect(badgeRect, 3f, 3f, paint)
            paint.alpha = 255

            paint.color = statusColor
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val statusLabel = item.stockHealthStatus.label
            canvas.drawText(statusLabel, MARGIN_LEFT + 480f, y + 14f, paint)

            // Draw Detailed Lots underneath product if available
            var lotY = y + 20f
            if (activeLots.isNotEmpty()) {
                activeLots.forEach { lot ->
                    lotY += 15f
                    val days = lot.daysUntilExpiration(now)
                    val lotStatusColor = when {
                        days < 0 -> COLOR_RED
                        days <= 30 -> COLOR_AMBER
                        else -> COLOR_TEXT_MUTED
                    }

                    paint.color = COLOR_TEXT_MUTED
                    paint.textSize = 8f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                    val lotNum = if (lot.lotNumber.isNotBlank()) lot.lotNumber else "S/N"
                    val locInfo = if (lot.location.isNotBlank()) " | Local: ${lot.location}" else ""
                    val expInfo = sdfDate.format(Date(lot.expirationDate))
                    val expStatusText = when {
                        days < 0 -> " (VENCIDO há ${-days} dias)"
                        days <= 30 -> " (Vence em $days dias)"
                        else -> ""
                    }

                    canvas.drawText("↳ Lote: $lotNum | Qtd: ${String.format(Locale.getDefault(), "%.2f", lot.quantity)} ${product.unit} | Validade: $expInfo$locInfo", MARGIN_LEFT + 16f, lotY, paint)

                    if (expStatusText.isNotBlank()) {
                        paint.color = lotStatusColor
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(expStatusText, MARGIN_LEFT + 410f, lotY, paint)
                    }
                }
            }

            y += blockHeight + 4f
            isAltRow = !isAltRow
        }

        pdfDocument.finishPage(currentPage)

        // Save PDF to Documents
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
            ?: context.cacheDir

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val cleanFilterName = filterName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(outputDir, "Relatorio_${cleanFilterName}_${System.currentTimeMillis()}.pdf")

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

        val chooserIntent = Intent.createChooser(shareIntent, "Abrir ou Enviar $title").apply {
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
