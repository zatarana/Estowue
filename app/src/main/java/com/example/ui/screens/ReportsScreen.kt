package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.BackupManager
import com.example.data.model.Category
import com.example.data.model.MovementType
import com.example.data.model.ProductWithLots
import com.example.data.model.StockLotsSummary
import com.example.data.model.StockMovement
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.theme.RoyalBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ProductionQuantityLimits

@Composable
fun ReportsScreen(
    summary: StockLotsSummary,
    productsWithLots: List<ProductWithLots>,
    categories: List<Category>,
    movements: List<StockMovement>,
    onOpenFilteredExport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val reportTabs = listOf("Geral & Ações", "Giro & Distribuição", "Estoque Mínimo", "Validades (FEFO)", "Movimentações", "Por Categoria", "Locais (FEFO)")


    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title & Export Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Central de Relatórios",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Análises de saldo, perdas, validades e movimentações",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Icon(
                            Icons.Default.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onOpenFilteredExport,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.FilterAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportação Avançada (Filtros)", fontWeight = FontWeight.Bold)
                    }

                    // Action Buttons Row: PDF, Google Drive / Share, CSV, Copy Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val file = com.example.utils.PdfReportService.generateStockReportPdf(context, productsWithLots, reportTabs[selectedTab])
                                if (file != null) {
                                    com.example.utils.PdfReportService.openOrSharePdf(
                                        context = context,
                                        file = file,
                                        title = "Relatorio_${reportTabs[selectedTab]}"
                                    )
                                } else {
                                    Toast.makeText(context, "Erro ao gerar PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF", fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }

                        Button(
                            onClick = {
                                val reportText = BackupManager.createExecutiveSummaryReport(
                                    productsWithLots = productsWithLots,
                                    movements = movements
                                )
                                BackupManager.shareContent(
                                    context = context,
                                    text = reportText,
                                    title = "Relatorio_Estoque_${System.currentTimeMillis()}.txt"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f).testTag("btn_report_share_drive")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvar / Enviar", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }

                        FilledTonalButton(
                            onClick = {
                                val csv = BackupManager.createInventoryCsv(productsWithLots)
                                BackupManager.shareContent(
                                    context = context,
                                    text = csv,
                                    title = "Planilha_Estoque.csv",
                                    mimeType = "text/csv"
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("btn_report_export_csv")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }

                        Button(
                            onClick = {
                                val reportText = BackupManager.createExecutiveSummaryReport(
                                    productsWithLots = productsWithLots,
                                    movements = movements
                                )
                                clipboardManager.setText(AnnotatedString(reportText))
                                Toast.makeText(context, "Resumo copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar Resumo", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Summary KPI Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportKpiCard(
                        title = "Volume em Estoque",
                        value = String.format(Locale.getDefault(), "%.1f", summary.totalItemsQuantity),
                        subtitle = "${summary.totalProductsCount} produtos cadastrados",
                        icon = Icons.Default.Inventory,
                        color = RoyalBlue,
                        modifier = Modifier.weight(1f)
                    )
                    ReportKpiCard(
                        title = "Abaixo do Mínimo",
                        value = "${summary.lowStockProductsCount}",
                        subtitle = if (summary.lowStockProductsCount > 0) "Reposição requerida" else "Estoque adequado",
                        icon = Icons.Default.ProductionQuantityLimits,
                        color = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportKpiCard(
                        title = "Validades Vencidas",
                        value = "${summary.expiredLotsCount}",
                        subtitle = if (summary.expiredLotsCount > 0) "Ação imediata necessária" else "Nenhum item vencido",
                        icon = Icons.Default.Warning,
                        color = RoseRed,
                        modifier = Modifier.weight(1f)
                    )
                    ReportKpiCard(
                        title = "Validade Crítica",
                        value = "${summary.criticalLotsCount}",
                        subtitle = "Vencimento em até 15 dias",
                        icon = Icons.Default.Warning,
                        color = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Tab Selector
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                reportTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
                }
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                // General Overview & Quick Insights
                item {
                    val entriesCount = movements.count { it.type == MovementType.ENTRADA }
                    val entriesQty = movements.filter { it.type == MovementType.ENTRADA }.sumOf { it.quantity }
                    val exitsCount = movements.count { it.type == MovementType.SAIDA || it.type == MovementType.DESCARTE_VENCIDO }
                    val exitsQty = movements.filter { it.type == MovementType.SAIDA || it.type == MovementType.DESCARTE_VENCIDO }.sumOf { it.quantity }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Balanço de Movimentações Registradas",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldGreen)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Entradas Totais:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                    Text(
                                        text = "+${String.format(Locale.getDefault(), "%.1f", entriesQty)} un ($entriesCount reg.)",
                                        color = EmeraldGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = RoseRed)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Saídas Totais:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                    Text(
                                        text = "-${String.format(Locale.getDefault(), "%.1f", exitsQty)} un ($exitsCount reg.)",
                                        color = RoseRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    com.example.ui.components.InventoryTurnoverAndValueWidget(
                        productsWithLots = productsWithLots,
                        categories = categories,
                        movements = movements
                    )
                }
            }

            1 -> {
                // Giro & Distribuição de Estoque (D3 / Recharts-style Native Compose Analytics)
                item {
                    com.example.ui.components.InventoryTurnoverAndValueWidget(
                        productsWithLots = productsWithLots,
                        categories = categories,
                        movements = movements
                    )
                }
            }

            2 -> {
                // Estoque Mínimo & Reposição
                val lowStockProducts = productsWithLots.filter { it.isLowStock }
                val normalStockProducts = productsWithLots.filter { !it.isLowStock && it.product.minStock > 0 }

                if (lowStockProducts.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Inventory, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Estoque Regular!",
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Todos os produtos configurados estão acima do estoque mínimo.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Produtos para Reposição (${lowStockProducts.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )
                            FilledTonalButton(
                                onClick = {
                                    val count = com.example.utils.StockNotificationHelper.checkAllProducts(context, productsWithLots)
                                    Toast.makeText(
                                        context,
                                        if (count > 0) "$count notificação(ões) de alerta enviadas!" else "Nenhum item com estoque crítico.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_notify_low_stock")
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Alertar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(lowStockProducts) { pWithLots ->
                        val deficit = pWithLots.product.minStock - pWithLots.totalQuantity
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pWithLots.product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "Cat: ${pWithLots.product.category} • Mínimo: ${pWithLots.product.minStock} ${pWithLots.product.unit}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (pWithLots.isOutOfStock) "ESTOQUE ZERADO" else "Déficit de reposição: ${String.format(Locale.getDefault(), "%.1f", deficit)} ${pWithLots.product.unit}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pWithLots.isOutOfStock) RoseRed else AmberWarning
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%.1f", pWithLots.totalQuantity)} ${pWithLots.product.unit}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = if (pWithLots.isOutOfStock) RoseRed else AmberWarning
                                    )
                                    Surface(
                                        color = if (pWithLots.isOutOfStock) RoseRed.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (pWithLots.isOutOfStock) "Sem Saldo" else "Abaixo do Mínimo",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (pWithLots.isOutOfStock) RoseRed else AmberWarning,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // Expiring Lots Report
                val allActiveLots = productsWithLots.flatMap { pWithLots ->
                    pWithLots.lots.filter { it.quantity > 0.001 }.map { lot -> Pair(pWithLots.product, lot) }
                }.sortedBy { it.second.expirationDate }

                if (allActiveLots.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhum lote ativo registrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(allActiveLots) { (product, lot) ->
                        val days = lot.daysUntilExpiration()
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val sub = if (product.brand.isNotBlank()) "Marca: ${product.brand} • Cat: ${product.category}" else "Cat: ${product.category}"
                                    Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Vencimento: ${dateFormat.format(Date(lot.expirationDate))}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when {
                                            days < 0 -> RoseRed
                                            days <= 15 -> AmberWarning
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${lot.quantity} ${product.unit}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                    Surface(
                                        color = when {
                                            days < 0 -> RoseRed.copy(alpha = 0.15f)
                                            days <= 15 -> AmberWarning.copy(alpha = 0.15f)
                                            days <= 45 -> AmberWarning.copy(alpha = 0.10f)
                                            else -> EmeraldGreen.copy(alpha = 0.15f)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                days < 0 -> "Vencido (${Math.abs(days)}d)"
                                                days == 0L -> "Vence Hoje!"
                                                else -> "Restam $days dias"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                days < 0 -> RoseRed
                                                days <= 15 -> AmberWarning
                                                else -> EmeraldGreen
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            4 -> {
                // Movements Breakdown
                items(movements.take(20)) { mov ->
                    val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                    
                    val movColor = when (mov.type) {
                        MovementType.ENTRADA -> EmeraldGreen
                        MovementType.SAIDA, MovementType.DESCARTE_VENCIDO -> RoseRed
                        MovementType.TRANSFERENCIA -> Color(0xFFE65100)
                        MovementType.AJUSTE -> MaterialTheme.colorScheme.primary
                    }
                    val movPrefix = when (mov.type) {
                        MovementType.ENTRADA -> "+"
                        MovementType.SAIDA, MovementType.DESCARTE_VENCIDO -> "-"
                        MovementType.AJUSTE, MovementType.TRANSFERENCIA -> "±"
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = movColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = mov.type.label,
                                            color = movColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(mov.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                val notesInfo = if (mov.notes.isNotBlank()) " | Obs: ${mov.notes.replace("\n", " ")}" else ""
                                Text(
                                    text = "${dateFormat.format(Date(mov.timestamp))} • ${mov.reason.ifBlank { "Sem motivo informado" }}$notesInfo",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "$movPrefix${mov.quantity}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = movColor
                            )
                        }
                    }
                }
            }

            5 -> {
                // Categories Breakdown
                val categoryGroups = productsWithLots.groupBy { it.product.category }
                val totalQtyAll = productsWithLots.sumOf { it.totalQuantity }.coerceAtLeast(0.001)

                items(categories) { cat ->
                    val prodsInCat = categoryGroups[cat.name] ?: emptyList()
                    val totalQtyCat = prodsInCat.sumOf { it.totalQuantity }
                    val percent = ((totalQtyCat / totalQtyAll) * 100).toInt()

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(parseHexColor(cat.colorHex), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text("${prodsInCat.size} produtos", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Saldo Total:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format(Locale.getDefault(), "%.1f", totalQtyCat)} un ($percent%)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Proportion Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(parseHexColor(cat.colorHex))
                                )
                            }
                        }
                    }
                }
            }
            6 -> {
                item {
                    LocationDistributionTab(
                        productsWithLots = productsWithLots,
                        alertDays = 30
                    )
                }
            }
        }

    }
}

@Composable
private fun ReportKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
