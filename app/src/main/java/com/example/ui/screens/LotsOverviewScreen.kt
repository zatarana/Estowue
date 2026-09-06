package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ProductionQuantityLimits
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.ProductWithLots
import com.example.data.model.StockLot
import com.example.data.model.StockLotsSummary
import com.example.ui.components.ExpirationBadge
import com.example.ui.components.StatCard
import com.example.ui.components.formatDateOnly
import com.example.ui.components.formatQuantity
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningLight
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleAccentLight
import com.example.ui.theme.RoseRed
import com.example.ui.theme.RoseRedLight

import androidx.compose.material3.IconButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow

enum class LotStatusFilter(val label: String) {
    ALL("Todos"),
    EXPIRED("Vencidos"),
    CRITICAL("Críticos (<7d)"),
    WARNING("Próximos (<Alerta)"),
    VALID("No Prazo")
}

@Composable
fun LotsOverviewScreen(
    summary: StockLotsSummary,
    productsWithLots: List<ProductWithLots>,
    expirationAlertDays: Int = 30,
    onSetExpirationAlertDays: (Int) -> Unit = {},
    onAddStockIn: (Product?) -> Unit,
    onStockOutLot: (Product, StockLot) -> Unit,
    onDiscardLot: (StockLot) -> Unit,
    onOpenBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val context = androidx.compose.ui.platform.LocalContext.current

    var lotSearchQuery by rememberSaveable { mutableStateOf("") }
    var selectedLotStatus by rememberSaveable { mutableStateOf(LotStatusFilter.ALL) }
    var selectedBrand by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLocation by rememberSaveable { mutableStateOf<String?>(null) }
    var brandMenuOpen by remember { mutableStateOf(false) }
    var locationMenuOpen by remember { mutableStateOf(false) }

    // All active lots sorted by expiration date (FEFO - Primeiro que Vence, Primeiro que Sai)
    val allActiveLotsWithProduct = remember(productsWithLots) {
        productsWithLots.flatMap { pWithLots ->
            pWithLots.lots.filter { it.quantity > 0.001 }.map { lot ->
                Pair(pWithLots.product, lot)
            }
        }.sortedBy { it.second.expirationDate }
    }

    val availableBrands = remember(allActiveLotsWithProduct) {
        allActiveLotsWithProduct.map { it.first.brand }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableLocations = remember(allActiveLotsWithProduct) {
        val pLocs = allActiveLotsWithProduct.map { it.first.location }.filter { it.isNotBlank() }
        val lLocs = allActiveLotsWithProduct.map { it.second.location }.filter { it.isNotBlank() }
        (pLocs + lLocs).distinct().sorted()
    }

    val activeLotsWithProduct = allActiveLotsWithProduct.filter { (product, lot) ->
        val days = lot.daysUntilExpiration(now)

        val matchesQuery = lotSearchQuery.isBlank() ||
            product.name.contains(lotSearchQuery, ignoreCase = true) ||
            product.brand.contains(lotSearchQuery, ignoreCase = true) ||
            lot.lotNumber.contains(lotSearchQuery, ignoreCase = true) ||
            lot.location.contains(lotSearchQuery, ignoreCase = true) ||
            product.location.contains(lotSearchQuery, ignoreCase = true)

        val matchesBrand = selectedBrand == null || product.brand.equals(selectedBrand, ignoreCase = true)
        val matchesLocation = selectedLocation == null ||
            product.location.equals(selectedLocation, ignoreCase = true) ||
            lot.location.equals(selectedLocation, ignoreCase = true)

        val matchesStatus = when (selectedLotStatus) {
            LotStatusFilter.ALL -> true
            LotStatusFilter.EXPIRED -> days < 0
            LotStatusFilter.CRITICAL -> days in 0..7
            LotStatusFilter.WARNING -> days in 0..expirationAlertDays
            LotStatusFilter.VALID -> days > expirationAlertDays
        }

        matchesQuery && matchesBrand && matchesLocation && matchesStatus
    }

    val expiredLots = allActiveLotsWithProduct.filter { it.second.daysUntilExpiration(now) < 0 }
    val hasActiveFilters = lotSearchQuery.isNotBlank() || selectedLotStatus != LotStatusFilter.ALL || selectedBrand != null || selectedLocation != null

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Controle de Validades",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Prioridade de saída por vencimento (FEFO)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val pdf = com.example.utils.PdfReportService.generateFefoLotsReportPdf(
                                context = context,
                                lotsWithProduct = activeLotsWithProduct,
                                filterName = "FEFO (" + (selectedBrand ?: "Todas Marcas") + " - " + (selectedLocation ?: "Todos Locais") + ")"
                            )
                            if (pdf != null) {
                                com.example.utils.PdfReportService.openOrSharePdf(
                                    context = context,
                                    file = pdf,
                                    title = "Relatorio_Validades_FEFO"
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_quick_pdf_lots")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = com.example.ui.theme.RoyalBlue, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.RoyalBlue)
                    }

                    FilledTonalButton(
                        onClick = onOpenBackup,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_open_backup")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Backup", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onAddStockIn(null) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        modifier = Modifier.testTag("btn_new_lot_entry")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Entrada", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Search & Filter Box for FEFO Lots
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = lotSearchQuery,
                        onValueChange = { lotSearchQuery = it },
                        placeholder = { Text("Buscar lote, produto, marca...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        trailingIcon = {
                            if (lotSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { lotSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar")
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Brand and Location filter row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            FilterChip(
                                selected = selectedBrand != null,
                                onClick = { brandMenuOpen = true },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(selectedBrand ?: "Marca: Todas", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = brandMenuOpen,
                                onDismissRequest = { brandMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todas as Marcas") },
                                    onClick = { selectedBrand = null; brandMenuOpen = false }
                                )
                                availableBrands.forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text(b) },
                                        onClick = { selectedBrand = b; brandMenuOpen = false }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            FilterChip(
                                selected = selectedLocation != null,
                                onClick = { locationMenuOpen = true },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(selectedLocation ?: "Local: Todos", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = locationMenuOpen,
                                onDismissRequest = { locationMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todos os Locais") },
                                    onClick = { selectedLocation = null; locationMenuOpen = false }
                                )
                                availableLocations.forEach { l ->
                                    DropdownMenuItem(
                                        text = { Text(l) },
                                        onClick = { selectedLocation = l; locationMenuOpen = false }
                                    )
                                }
                            }
                        }
                    }

                    // Status Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(LotStatusFilter.values()) { status ->
                            FilterChip(
                                selected = selectedLotStatus == status,
                                onClick = { selectedLotStatus = status },
                                label = { Text(status.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (hasActiveFilters) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${activeLotsWithProduct.size} lote(s) filtrado(s)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    lotSearchQuery = ""
                                    selectedLotStatus = LotStatusFilter.ALL
                                    selectedBrand = null
                                    selectedLocation = null
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Limpar Filtros", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Expiration Alert Antecedência Selector Bar
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Alerta de Validade:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(7, 15, 30, 45, 60).forEach { days ->
                            FilterChip(
                                selected = expirationAlertDays == days,
                                onClick = { onSetExpirationAlertDays(days) },
                                label = { Text("${days}d", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.height(28.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberWarning.copy(alpha = 0.25f),
                                    selectedLabelColor = AmberWarning
                                )
                            )
                        }
                    }
                }
            }
        }

        // Summary 4-Grid Cards + Low Stock KPI
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Itens em Estoque",
                        value = formatQuantity(summary.totalItemsQuantity),
                        subtitle = "${summary.totalProductsCount} produtos cadastrados",
                        icon = Icons.Default.Inventory2,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBg = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_total_items"
                    )

                    StatCard(
                        title = "Abaixo do Mínimo",
                        value = "${summary.lowStockProductsCount}",
                        subtitle = if (summary.lowStockProductsCount > 0) "Reposição requerida" else "Estoque adequado",
                        icon = Icons.Default.ProductionQuantityLimits,
                        iconTint = if (summary.lowStockProductsCount > 0) AmberWarning else EmeraldGreen,
                        iconBg = if (summary.lowStockProductsCount > 0) AmberWarningLight else EmeraldGreenLight,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_low_stock"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Itens Vencidos",
                        value = "${summary.expiredLotsCount}",
                        subtitle = if (summary.expiredLotsCount > 0) "Descarte necessário" else "Nenhum vencido",
                        icon = Icons.Default.DeleteSweep,
                        iconTint = if (summary.expiredLotsCount > 0) RoseRed else EmeraldGreen,
                        iconBg = if (summary.expiredLotsCount > 0) RoseRedLight else EmeraldGreenLight,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_expired_lots"
                    )

                    StatCard(
                        title = "Em Alerta (<${expirationAlertDays}d)",
                        value = "${summary.criticalLotsCount + summary.warningLotsCount}",
                        subtitle = "${summary.criticalLotsCount} críticos, ${summary.warningLotsCount} em atenção",
                        icon = Icons.Default.WarningAmber,
                        iconTint = if (summary.criticalLotsCount > 0) AmberWarning else EmeraldGreen,
                        iconBg = if (summary.criticalLotsCount > 0) AmberWarningLight else EmeraldGreenLight,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_critical_lots"
                    )
                }
            }
        }

        // Expired Alert Notice
        if (expiredLots.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = RoseRedLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(RoseRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Atenção: ${expiredLots.size} produto(s) com validade VENCIDA",
                                fontWeight = FontWeight.Bold,
                                color = RoseRed,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Efetue o descarte para manter o estoque regular.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Header: Chronological List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Próximos a Vencer (FEFO)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${activeLotsWithProduct.size} registro(s)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (activeLotsWithProduct.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhum estoque ativo com validade",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(activeLotsWithProduct, key = { it.second.id }) { (product, lot) ->
                LotOverviewCard(
                    product = product,
                    lot = lot,
                    alertDays = expirationAlertDays,
                    onStockOut = { onStockOutLot(product, lot) },
                    onDiscard = { onDiscardLot(lot) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun LotOverviewCard(
    product: Product,
    lot: StockLot,
    alertDays: Int = 30,
    onStockOut: () -> Unit,
    onDiscard: () -> Unit
) {
    val days = lot.daysUntilExpiration()
    val isExpired = days < 0

    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Product Name and Expiration status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val subtext = buildString {
                        if (product.brand.isNotBlank()) append("Marca: ${product.brand} • ")
                        append("Cat: ${product.category}")
                        if (product.barcode.isNotBlank()) append(" • EAN: ${product.barcode}")
                    }
                    Text(
                        text = subtext,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ExpirationBadge(
                    expirationDate = lot.expirationDate,
                    quantity = lot.quantity,
                    alertDays = alertDays
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity and Expiration Details
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Saldo Disponível",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${formatQuantity(lot.quantity)} ${product.unit}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Data de Validade",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDateOnly(lot.expirationDate),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpired) RoseRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExpired) {
                    OutlinedButton(
                        onClick = onDiscard,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_discard_${lot.id}")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Descartar Vencido", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onStockOut,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_stock_out_${lot.id}")
                    ) {
                        Icon(Icons.Default.Outbox, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dar Saída", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

