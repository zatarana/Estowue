package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.material3.TextButton
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
import com.example.data.model.StockLot
import com.example.data.model.StockMovement
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.theme.RoyalBlue
import com.example.utils.PdfReportService
import java.util.Locale

enum class PdfReportType(val title: String, val desc: String) {
    INVENTORY("Inventário Geral", "Lista produtos, estoque total, estoque mínimo e lotes"),
    FEFO_LOTS("Validades & Lotes (FEFO)", "Lista lotes ordenados por data de vencimento"),
    LOW_STOCK("Reposição Urgente", "Lista apenas produtos abaixo do estoque mínimo"),
    MOVEMENTS("Auditoria de Movimentações", "Lista histórico de entradas, saídas e descartes")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickPdfExportDialog(
    productsWithLots: List<ProductWithLots>,
    categories: List<Category>,
    movements: List<StockMovement> = emptyList(),
    currentAlertDays: Int = 30,
    initialCategory: String? = null,
    initialBrand: String? = null,
    initialLocation: String? = null,
    initialReportType: PdfReportType = PdfReportType.INVENTORY,
    initialStockFilter: ExportStockFilter = ExportStockFilter.ALL,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var selectedReportType by rememberSaveable { mutableStateOf(initialReportType) }
    var selectedStockFilter by rememberSaveable { mutableStateOf(initialStockFilter) }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf(initialCategory) }
    var selectedBrandFilter by rememberSaveable { mutableStateOf(initialBrand) }
    var selectedLocationFilter by rememberSaveable { mutableStateOf(initialLocation) }
    var selectedProductId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedAlertDays by rememberSaveable { mutableIntStateOf(currentAlertDays) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    var brandDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var locationDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var productDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    val allProducts = remember(productsWithLots) {
        productsWithLots.map { it.product }.sortedBy { it.name }
    }

    val uniqueBrands = remember(allProducts) {
        allProducts.map { it.brand }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val uniqueLocations = remember(productsWithLots) {
        val prodLocations = productsWithLots.map { it.product.location }.filter { it.isNotBlank() }
        val lotLocations = productsWithLots.flatMap { it.lots.map { lot -> lot.location } }.filter { it.isNotBlank() }
        (prodLocations + lotLocations).distinct().sorted()
    }

    // Filter products
    val filteredProductsWithLots = remember(
        productsWithLots,
        selectedCategoryFilter,
        selectedBrandFilter,
        selectedLocationFilter,
        selectedProductId,
        selectedStockFilter,
        selectedAlertDays
    ) {
        BackupManager.filterDataForExport(
            productsWithLots = productsWithLots,
            selectedCategory = selectedCategoryFilter,
            selectedBrand = selectedBrandFilter,
            selectedLocation = selectedLocationFilter,
            selectedProductId = selectedProductId,
            stockFilter = selectedStockFilter,
            alertDays = selectedAlertDays
        )
    }

    // Filter lots for FEFO report
    val filteredLotsWithProduct = remember(
        filteredProductsWithLots,
        selectedLocationFilter,
        selectedAlertDays
    ) {
        val now = System.currentTimeMillis()
        filteredProductsWithLots.flatMap { pWithLots ->
            pWithLots.lots
                .filter { lot ->
                    val hasStock = lot.quantity > 0.001
                    val matchesLoc = selectedLocationFilter == null || lot.location.equals(selectedLocationFilter, ignoreCase = true) || pWithLots.product.location.equals(selectedLocationFilter, ignoreCase = true)
                    hasStock && matchesLoc
                }
                .map { lot -> pWithLots.product to lot }
        }.sortedBy { it.second.expirationDate }
    }

    // Filter movements
    val filteredMovements = remember(
        movements,
        selectedBrandFilter,
        selectedProductId,
        selectedLocationFilter
    ) {
        BackupManager.filterMovements(
            movements = movements,
            selectedBrand = selectedBrandFilter,
            selectedProductId = selectedProductId,
            selectedLocation = selectedLocationFilter
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
        selectedLocationFilter,
        selectedProductId,
        selectedStockFilter,
        selectedAlertDays
    ) {
        val parts = mutableListOf<String>()
        selectedCategoryFilter?.let { parts.add("Categoria: $it") }
        selectedBrandFilter?.let { parts.add("Marca: $it") }
        selectedLocationFilter?.let { parts.add("Local: $it") }
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
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
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
                                text = "Exportar Relatório PDF",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Formatação profissional para impressão e envio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // REPORT TYPE SELECTOR
                Text(
                    text = "Modelo do Relatório:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PdfReportType.values().forEach { type ->
                        if (type != PdfReportType.MOVEMENTS || movements.isNotEmpty()) {
                            FilterChip(
                                selected = selectedReportType == type,
                                onClick = {
                                    selectedReportType = type
                                    if (type == PdfReportType.LOW_STOCK) {
                                        selectedStockFilter = ExportStockFilter.LOW_STOCK
                                    } else if (type == PdfReportType.FEFO_LOTS) {
                                        selectedStockFilter = ExportStockFilter.ALL
                                    }
                                },
                                label = { Text(type.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalBlue.copy(alpha = 0.2f),
                                    selectedLabelColor = RoyalBlue
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // FILTERS CONTAINER
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
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
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Filtros Aplicados:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (selectedCategoryFilter != null || selectedBrandFilter != null || selectedLocationFilter != null || selectedProductId != null || selectedStockFilter != ExportStockFilter.ALL) {
                                TextButton(
                                    onClick = {
                                        selectedCategoryFilter = null
                                        selectedBrandFilter = null
                                        selectedLocationFilter = null
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // 1. Stock Status Filter (if inventory or generic)
                        if (selectedReportType == PdfReportType.INVENTORY) {
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
                            Spacer(modifier = Modifier.height(8.dp))
                        }

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

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. Brand & Location dropdowns
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Brand Dropdown
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

                            // Location Dropdown
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Localização:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        onClick = { locationDropdownExpanded = true }
                                    ) {
                                        Text(
                                            text = selectedLocationFilter ?: "Todos os Locais",
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(8.dp),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = locationDropdownExpanded,
                                        onDismissRequest = { locationDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Todos os Locais") },
                                            onClick = { selectedLocationFilter = null; locationDropdownExpanded = false }
                                        )
                                        uniqueLocations.forEach { loc ->
                                            DropdownMenuItem(
                                                text = { Text(loc) },
                                                onClick = { selectedLocationFilter = loc; locationDropdownExpanded = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 4. Product Dropdown
                        Column(modifier = Modifier.fillMaxWidth()) {
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
                                        text = selectedProductId?.let { id -> allProducts.find { it.id == id }?.name } ?: "Todos os Produtos",
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

                Spacer(modifier = Modifier.height(12.dp))

                // LIVE PREVIEW COUNTER
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
                                text = "Registros inclusos no PDF:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val summaryCount = when (selectedReportType) {
                                PdfReportType.MOVEMENTS -> "${filteredMovements.size} movimentação(ões)"
                                PdfReportType.FEFO_LOTS -> "${filteredLotsWithProduct.size} lote(s) listado(s)"
                                else -> "${filteredProductsWithLots.size} produto(s) • $totalActiveLots lote(s)"
                            }
                            Text(
                                text = summaryCount,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue
                            )
                        }

                        if (selectedReportType != PdfReportType.MOVEMENTS) {
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", totalStockUnits)} un",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Button(
                    onClick = {
                        isGeneratingPdf = true
                        val pdfFile = when (selectedReportType) {
                            PdfReportType.FEFO_LOTS -> {
                                if (filteredLotsWithProduct.isEmpty()) {
                                    Toast.makeText(context, "Nenhum lote atende aos filtros.", Toast.LENGTH_SHORT).show()
                                    isGeneratingPdf = false
                                    return@Button
                                }
                                PdfReportService.generateFefoLotsReportPdf(
                                    context = context,
                                    lotsWithProduct = filteredLotsWithProduct,
                                    filterName = "Validades (FEFO)",
                                    subtitleFilter = filterSummary
                                )
                            }
                            PdfReportType.MOVEMENTS -> {
                                if (filteredMovements.isEmpty()) {
                                    Toast.makeText(context, "Nenhuma movimentação atende aos filtros.", Toast.LENGTH_SHORT).show()
                                    isGeneratingPdf = false
                                    return@Button
                                }
                                PdfReportService.generateMovementsReportPdf(
                                    context = context,
                                    movements = filteredMovements,
                                    filterName = "Histórico de Movimentações",
                                    subtitleFilter = filterSummary
                                )
                            }
                            else -> {
                                if (filteredProductsWithLots.isEmpty()) {
                                    Toast.makeText(context, "Nenhum produto atende aos filtros.", Toast.LENGTH_SHORT).show()
                                    isGeneratingPdf = false
                                    return@Button
                                }
                                val reportLabel = if (selectedReportType == PdfReportType.LOW_STOCK) "Reposição Abaixo do Mínimo" else selectedStockFilter.label
                                PdfReportService.generateStockReportPdf(
                                    context = context,
                                    products = filteredProductsWithLots,
                                    filterName = reportLabel,
                                    subtitleFilter = filterSummary
                                )
                            }
                        }
                        isGeneratingPdf = false

                        if (pdfFile != null) {
                            PdfReportService.openOrSharePdf(
                                context = context,
                                file = pdfFile,
                                title = "Relatorio_${selectedReportType.name}"
                            )
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Erro ao gerar PDF.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isGeneratingPdf,
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
