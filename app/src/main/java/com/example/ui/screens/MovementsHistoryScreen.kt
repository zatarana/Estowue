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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MovementType
import com.example.data.model.StockMovement
import com.example.ui.components.MovementTypeBadge
import com.example.ui.components.formatDate
import com.example.ui.components.formatDateOnly
import com.example.ui.components.formatQuantity

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow

enum class MovementPeriodFilter(val label: String, val days: Int?) {
    ALL("Todo o Histórico", null),
    TODAY("Hoje", 1),
    LAST_7_DAYS("Últimos 7 dias", 7),
    LAST_30_DAYS("Últimos 30 dias", 30)
}

@Composable
fun MovementsHistoryScreen(
    movements: List<StockMovement>,
    onEditMovement: (StockMovement) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var filterType by rememberSaveable { mutableStateOf<MovementType?>(null) }
    var filterQuery by rememberSaveable { mutableStateOf("") }
    var selectedBrand by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPeriod by rememberSaveable { mutableStateOf(MovementPeriodFilter.ALL) }
    var brandMenuOpen by remember { mutableStateOf(false) }

    val availableBrands = remember(movements) {
        movements.map { it.productBrand }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val now = System.currentTimeMillis()
    val filteredList = movements.filter { mov ->
        val matchesType = filterType == null || mov.type == filterType
        val matchesQuery = filterQuery.isBlank() ||
            mov.productName.contains(filterQuery, ignoreCase = true) ||
            mov.productBrand.contains(filterQuery, ignoreCase = true) ||
            mov.reason.contains(filterQuery, ignoreCase = true) ||
            mov.lotNumber.contains(filterQuery, ignoreCase = true) ||
            mov.documentNumber.contains(filterQuery, ignoreCase = true)

        val matchesBrand = selectedBrand == null || mov.productBrand.equals(selectedBrand, ignoreCase = true)

        val matchesPeriod = when (val days = selectedPeriod.days) {
            null -> true
            else -> {
                val cutoff = now - (days * 24L * 60L * 60L * 1000L)
                mov.timestamp >= cutoff
            }
        }

        matchesType && matchesQuery && matchesBrand && matchesPeriod
    }

    val hasActiveFilters = filterQuery.isNotBlank() || filterType != null || selectedBrand != null || selectedPeriod != MovementPeriodFilter.ALL

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Filter bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = filterQuery,
                        onValueChange = { filterQuery = it },
                        placeholder = { Text("Buscar produto, marca, NF, motivo...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (filterQuery.isNotEmpty()) {
                                IconButton(onClick = { filterQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_search_movements"),
                        singleLine = true
                    )

                    Surface(
                        color = com.example.ui.theme.RoyalBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val pdf = com.example.utils.PdfReportService.generateMovementsReportPdf(
                                    context = context,
                                    movements = filteredList,
                                    filterName = "Histórico de Movimentações (" + (filterType?.label ?: "Todos") + " - " + (selectedBrand ?: "Todas Marcas") + ")"
                                )
                                if (pdf != null) {
                                    com.example.utils.PdfReportService.openOrSharePdf(
                                        context = context,
                                        file = pdf,
                                        title = "Relatorio_Movimentacoes"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("btn_pdf_movements_screen")
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "Exportar PDF de Movimentações",
                                tint = com.example.ui.theme.RoyalBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Brand & Period Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Brand dropdown
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

                    // Period chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        items(MovementPeriodFilter.values().toList(), key = { it.name }) { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                label = { Text(period.label, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Movement Type Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filterType == null,
                            onClick = { filterType = null },
                            label = { Text("Todas (${filteredList.size})") },
                            modifier = Modifier.testTag("filter_mov_all")
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == MovementType.ENTRADA,
                            onClick = { filterType = MovementType.ENTRADA },
                            label = { Text("Entradas (+)") },
                            modifier = Modifier.testTag("filter_mov_in")
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == MovementType.SAIDA,
                            onClick = { filterType = MovementType.SAIDA },
                            label = { Text("Saídas (-)") },
                            modifier = Modifier.testTag("filter_mov_out")
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == MovementType.DESCARTE_VENCIDO,
                            onClick = { filterType = MovementType.DESCARTE_VENCIDO },
                            label = { Text("Descartes Vencidos") },
                            modifier = Modifier.testTag("filter_mov_discard")
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == MovementType.AJUSTE,
                            onClick = { filterType = MovementType.AJUSTE },
                            label = { Text("Ajustes de Saldo") },
                            modifier = Modifier.testTag("filter_mov_adjust")
                        )
                    }
                }

                if (hasActiveFilters) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredList.size} movimentação(ões) encontrada(s)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = {
                                filterQuery = ""
                                filterType = null
                                selectedBrand = null
                                selectedPeriod = MovementPeriodFilter.ALL
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

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhuma movimentação registrada",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "As entradas e saídas aparecerão aqui.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { mov ->
                    MovementItemCard(mov = mov, onEdit = { onEditMovement(mov) })
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

@Composable
fun MovementItemCard(mov: StockMovement, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MovementTypeBadge(type = mov.type)
                Text(
                    text = formatDate(mov.timestamp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mov.productName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            val subInfo = if (mov.productBrand.isNotBlank()) {
                "Marca: ${mov.productBrand} • Motivo: ${mov.reason.ifBlank { "Não informado" }}"
            } else {
                "Motivo: ${mov.reason.ifBlank { "Não informado" }}"
            }
            Text(
                text = subInfo,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (mov.lotExpirationDate != null && mov.lotExpirationDate > 0L) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Validade: ${formatDateOnly(mov.lotExpirationDate)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (mov.documentNumber.isNotBlank()) {
                Text(
                    text = "Doc/NF: ${mov.documentNumber}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val prefix = when (mov.type) {
                    MovementType.ENTRADA -> "+"
                    MovementType.SAIDA, MovementType.DESCARTE_VENCIDO -> "-"
                    MovementType.AJUSTE, MovementType.TRANSFERENCIA -> "±"
                }
                Text(
                    text = "Qtd Movimentada: $prefix${formatQuantity(mov.quantity)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                // Edit / Reverse Button for wrong entry correction
                if (mov.type != MovementType.TRANSFERENCIA) {
                    FilledTonalButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Corrigir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (mov.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Obs: ${mov.notes}",
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
