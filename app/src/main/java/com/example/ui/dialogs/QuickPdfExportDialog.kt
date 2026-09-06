package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.backup.BackupManager
import com.example.data.backup.ExportStockFilter
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.ProductWithLots
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.theme.RoyalBlue
import com.example.utils.PdfReportService

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickPdfExportDialog(
    productsWithLots: List<ProductWithLots>,
    categories: List<Category>,
    currentAlertDays: Int = 30,
    initialCategory: String? = null,
    initialStockFilter: ExportStockFilter = ExportStockFilter.ALL,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var selectedStockFilter by rememberSaveable { mutableStateOf(initialStockFilter) }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf(initialCategory) }
    var selectedBrandFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedProductId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedAlertDays by rememberSaveable { mutableIntStateOf(currentAlertDays) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    var brandDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var productDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    val allProducts = remember(productsWithLots) {
        productsWithLots.map { it.product }.sortedBy { it.name }
    }

    val uniqueBrands = remember(allProducts) {
        allProducts.map { it.brand }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // Filter products
    val filteredProductsWithLots = remember(
        productsWithLots,
        selectedCategoryFilter,
        selectedBrandFilter,
        selectedProductId,
        selectedStockFilter,
        selectedAlertDays
    ) {
        BackupManager.filterDataForExport(
            productsWithLots = productsWithLots,
            selectedCategory = selectedCategoryFilter,
            selectedBrand = selectedBrandFilter,
            selectedProductId = selectedProductId,
            stockFilter = selectedStockFilter,
            alertDays = selectedAlertDays
        )
    }

    val totalActiveLots = remember(filteredProductsWithLots) {
        filteredProductsWithLots.sumOf { it.lots.count { lot -> lot.quantity > 0.001 } }
    }

    val totalStockUnits = remember(filteredProductsWithLots) {
        filteredProductsWithLots.sumOf { it.totalQuantity }
    }

    val filterSummary = remember(
        selectedCategoryFilter,
        selectedBrandFilter,
        selectedProductId,
        selectedStockFilter,
        selectedAlertDays
    ) {
        val parts = mutableListOf<String>()
        selectedCategoryFilter?.let { parts.add("Categoria: $it") }
        selectedBrandFilter?.let { parts.add("Marca: $it") }
        selectedProductId?.let { id ->
            val pName = allProducts.find { it.id == id }?.name
            if (pName != null) parts.add("Produto: $pName")
        }
        if (selectedStockFilter != ExportStockFilter.ALL) {
            parts.add(selectedStockFilter.label)
        }
        if (selectedStockFilter == ExportStockFilter.EXPIRING_SOON) {
            parts.add("(< $selectedAlertDays dias)")
        }
        if (parts.isEmpty()) "Todos os itens cadastrados" else parts.joinToString(" • ")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = RoyalBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Exportar Estoque em PDF",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Relatório formatado para impressão ou envio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // FILTERS CARD
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FilterAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Filtros do Relatório:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (selectedCategoryFilter != null || selectedBrandFilter != null || selectedProductId != null || selectedStockFilter != ExportStockFilter.ALL) {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        selectedCategoryFilter = null
                                        selectedBrandFilter = null
                                        selectedProductId = null
                                        selectedStockFilter = ExportStockFilter.ALL
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text(
                                        text = "Limpar Filtros",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 1. Stock Status Filter
                        Text("Situação do Estoque:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ExportStockFilter.values().forEach { filter ->
                                FilterChip(
                                    selected = selectedStockFilter == filter,
                                    onClick = { selectedStockFilter = filter },
                                    label = { Text(filter.label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when (filter) {
                                            ExportStockFilter.LOW_STOCK -> AmberWarning.copy(alpha = 0.25f)
                                            ExportStockFilter.EXPIRED -> RoseRed.copy(alpha = 0.25f)
                                            ExportStockFilter.EXPIRING_SOON -> AmberWarning.copy(alpha = 0.25f)
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        }
                                    )
                                )
                            }
                        }

                        // Expiring days selector
                        if (selectedStockFilter == ExportStockFilter.EXPIRING_SOON) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Janela de Vencimento:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(7, 15, 30, 45, 60, 90).forEach { days ->
                                    FilterChip(
                                        selected = selectedAlertDays == days,
                                        onClick = { selectedAlertDays = days },
                                        label = { Text("$days dias", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Category Filter
                        Text("Categoria:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text("Todas", fontSize = 11.sp) }
                            )
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategoryFilter.equals(cat.name, ignoreCase = true),
                                    onClick = {
                                        selectedCategoryFilter = if (selectedCategoryFilter.equals(cat.name, ignoreCase = true)) null else cat.name
                                    },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(parseHexColor(cat.colorHex), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(cat.name, fontSize = 11.sp)
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3. Brand & Product dropdowns
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Brand
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Marca:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        onClick = { brandDropdownExpanded = true }
                                    ) {
                                        Text(
                                            text = selectedBrandFilter ?: "Todas",
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(8.dp),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = brandDropdownExpanded,
                                        onDismissRequest = { brandDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Todas as Marcas") },
                                            onClick = { selectedBrandFilter = null; brandDropdownExpanded = false }
                                        )
                                        uniqueBrands.forEach { brand ->
                                            DropdownMenuItem(
                                                text = { Text(brand) },
                                                onClick = { selectedBrandFilter = brand; brandDropdownExpanded = false }
                                            )
                                        }
                                    }
                                }
                            }

                            // Product
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Produto Específico:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        onClick = { productDropdownExpanded = true }
                                    ) {
                                        Text(
                                            text = selectedProductId?.let { id -> allProducts.find { it.id == id }?.name } ?: "Todos",
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(8.dp),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = productDropdownExpanded,
                                        onDismissRequest = { productDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Todos os Produtos") },
                                            onClick = { selectedProductId = null; productDropdownExpanded = false }
                                        )
                                        allProducts.forEach { prod ->
                                            DropdownMenuItem(
                                                text = { Text(prod.name) },
                                                onClick = { selectedProductId = prod.id; productDropdownExpanded = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Counter Box
                Surface(
                    color = RoyalBlue.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Itens incluídos no PDF:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${filteredProductsWithLots.size} produto(s) • $totalActiveLots lote(s) ativos",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue
                            )
                        }

                        Text(
                            text = "${String.format(java.util.Locale.getDefault(), "%.1f", totalStockUnits)} un",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = {
                        if (filteredProductsWithLots.isEmpty()) {
                            Toast.makeText(context, "Nenhum produto atende aos filtros selecionados.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isGeneratingPdf = true
                        val filterLabel = selectedStockFilter.label
                        val pdfFile = PdfReportService.generateStockReportPdf(
                            context = context,
                            products = filteredProductsWithLots,
                            filterName = filterLabel,
                            subtitleFilter = filterSummary
                        )
                        isGeneratingPdf = false

                        if (pdfFile != null) {
                            PdfReportService.openOrSharePdf(
                                context = context,
                                file = pdfFile,
                                title = "Relatorio_Estoque_${filterLabel}"
                            )
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Erro ao gerar PDF.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isGeneratingPdf && filteredProductsWithLots.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_confirm_generate_pdf")
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gerando PDF...")
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gerar e Visualizar PDF", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}
