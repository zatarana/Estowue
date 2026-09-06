package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.MovementType
import com.example.data.model.ProductWithLots
import com.example.data.model.StockMovement
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.theme.RoyalBlue
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data representation of Turnover rate calculations.
 */
data class CategoryTurnoverMetric(
    val categoryName: String,
    val colorHex: String,
    val currentStock: Double,
    val periodExits: Double,
    val averageStock: Double,
    val turnoverRate: Double,
    val daysOfInventory: Double,
    val productsCount: Int,
    val classification: TurnoverHealth
)

enum class TurnoverHealth(val label: String, val color: Color, val description: String) {
    HIGH("Alto Giro", EmeraldGreen, "Fluxo rápido e reposição constante"),
    MODERATE("Giro Equilibrado", RoyalBlue, "Demanda estável e estoque balanceado"),
    LOW("Giro Lento", AmberWarning, "Atenção: giro reduzido, risco de validade"),
    STAGNANT("Estagnado", RoseRed, "Sem saídas no período: capital parado")
}

data class AbcProductItem(
    val productName: String,
    val brand: String,
    val category: String,
    val currentStock: Double,
    val exitsQty: Double,
    val sharePercent: Double,
    val cumulativePercent: Double,
    val classification: String // "A", "B", "C"
)

data class DonutSliceData(
    val label: String,
    val value: Double,
    val percentage: Double,
    val color: Color,
    val subtext: String = ""
)

/**
 * Reusable Dashboard Widget inspired by D3 / Recharts data visualization paradigms,
 * implemented in native high-performance Jetpack Compose Canvas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryTurnoverAndValueWidget(
    productsWithLots: List<ProductWithLots>,
    categories: List<Category>,
    movements: List<StockMovement>,
    modifier: Modifier = Modifier
) {
    var selectedTimeframeDays by remember { mutableIntStateOf(30) }
    var selectedVisualTab by remember { mutableIntStateOf(0) } // 0: Giro de Estoque, 1: Distribuição de Saldo/Valor, 2: Curva ABC
    var selectedSliceIndex by remember { mutableIntStateOf(-1) }

    val now = remember { System.currentTimeMillis() }
    val timeframeCutoff = remember(selectedTimeframeDays, now) {
        if (selectedTimeframeDays <= 0) 0L else now - (selectedTimeframeDays * 24L * 60L * 60L * 1000L)
    }

    // Filter movements within timeframe
    val periodMovements = remember(movements, timeframeCutoff) {
        if (timeframeCutoff == 0L) movements
        else movements.filter { it.timestamp >= timeframeCutoff }
    }

    // Precompute Category Turnover Data
    val categoryTurnovers = remember(productsWithLots, categories, periodMovements, selectedTimeframeDays) {
        val totalDays = if (selectedTimeframeDays <= 0) 90.0 else selectedTimeframeDays.toDouble()
        val catMap = categories.associateBy { it.name }

        val allCategoryNames = (categories.map { it.name } + productsWithLots.map { it.product.category }).distinct()

        allCategoryNames.map { catName ->
            val prods = productsWithLots.filter { it.product.category == catName }
            val currentStock = prods.sumOf { it.totalQuantity }
            val prodIds = prods.map { it.product.id }.toSet()

            val exitsInPeriod = periodMovements
                .filter { it.productId in prodIds && (it.type == MovementType.SAIDA || it.type == MovementType.DESCARTE_VENCIDO) }
                .sumOf { it.quantity }

            val entriesInPeriod = periodMovements
                .filter { it.productId in prodIds && it.type == MovementType.ENTRADA }
                .sumOf { it.quantity }

            // Estimate average stock in period
            val estimatedInitialStock = (currentStock + exitsInPeriod - entriesInPeriod).coerceAtLeast(0.0)
            val avgStock = ((estimatedInitialStock + currentStock) / 2.0).coerceAtLeast(if (currentStock > 0) currentStock * 0.5 else 0.001)

            val turnoverRate = if (avgStock > 0.001) (exitsInPeriod / avgStock) else 0.0
            val daysOfInv = if (turnoverRate > 0.001) (totalDays / turnoverRate) else 999.0

            val health = when {
                exitsInPeriod <= 0.001 && currentStock > 0.001 -> TurnoverHealth.STAGNANT
                turnoverRate >= 1.5 -> TurnoverHealth.HIGH
                turnoverRate >= 0.5 -> TurnoverHealth.MODERATE
                turnoverRate > 0.001 -> TurnoverHealth.LOW
                else -> TurnoverHealth.STAGNANT
            }

            CategoryTurnoverMetric(
                categoryName = catName,
                colorHex = catMap[catName]?.colorHex ?: "#3B82F6",
                currentStock = currentStock,
                periodExits = exitsInPeriod,
                averageStock = avgStock,
                turnoverRate = turnoverRate,
                daysOfInventory = daysOfInv,
                productsCount = prods.size,
                classification = health
            )
        }.sortedByDescending { it.turnoverRate }
    }

    // Overall Global Turnover KPI
    val globalStock = remember(productsWithLots) { productsWithLots.sumOf { it.totalQuantity } }
    val globalExits = remember(periodMovements) {
        periodMovements.filter { it.type == MovementType.SAIDA || it.type == MovementType.DESCARTE_VENCIDO }.sumOf { it.quantity }
    }
    val globalTurnover = remember(globalStock, globalExits) {
        if (globalStock > 0.001) globalExits / globalStock else 0.0
    }
    val globalDsi = remember(globalTurnover, selectedTimeframeDays) {
        val days = if (selectedTimeframeDays <= 0) 90.0 else selectedTimeframeDays.toDouble()
        if (globalTurnover > 0.001) days / globalTurnover else 0.0
    }

    // Donut Slices Data for Category Stock Distribution
    val donutSlices = remember(categoryTurnovers, globalStock) {
        if (globalStock <= 0.001) {
            listOf(DonutSliceData("Sem Itens", 1.0, 100.0, Color.LightGray, "0 un"))
        } else {
            categoryTurnovers.filter { it.currentStock > 0.001 }.map { item ->
                val percent = (item.currentStock / globalStock) * 100.0
                DonutSliceData(
                    label = item.categoryName,
                    value = item.currentStock,
                    percentage = percent,
                    color = parseHexColor(item.colorHex),
                    subtext = "${String.format(Locale.getDefault(), "%.1f", item.currentStock)} un (${String.format(Locale.getDefault(), "%.1f", percent)}%)"
                )
            }
        }
    }

    // ABC Curve Data
    val abcItems = remember(productsWithLots, periodMovements) {
        val movementsByProd = periodMovements
            .filter { it.type == MovementType.SAIDA || it.type == MovementType.DESCARTE_VENCIDO }
            .groupBy { it.productId }

        val ranked = productsWithLots.map { pWithLots ->
            val exits = movementsByProd[pWithLots.product.id]?.sumOf { it.quantity } ?: 0.0
            pWithLots to exits
        }.sortedByDescending { it.second }

        val totalMovements = ranked.sumOf { it.second }.coerceAtLeast(0.001)
        var cumulative = 0.0

        ranked.map { (pWithLots, exits) ->
            val share = (exits / totalMovements) * 100.0
            cumulative += share
            val classification = when {
                cumulative <= 80.0 || (cumulative - share < 80.0) -> "A"
                cumulative <= 95.0 || (cumulative - share < 95.0) -> "B"
                else -> "C"
            }
            AbcProductItem(
                productName = pWithLots.product.name,
                brand = pWithLots.product.brand,
                category = pWithLots.product.category,
                currentStock = pWithLots.totalQuantity,
                exitsQty = exits,
                sharePercent = share,
                cumulativePercent = cumulative.coerceAtMost(100.0),
                classification = classification
            )
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_turnover_widget")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Icon and Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Giro & Distribuição de Estoque",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dashboard Analítico com Taxas de Giro e Proporções",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timeframe Selector Chips (30D, 60D, 90D, Total)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Período:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val periods = listOf(
                    30 to "30 Dias",
                    60 to "60 Dias",
                    90 to "90 Dias",
                    0 to "Geral"
                )

                periods.forEach { (days, label) ->
                    FilterChip(
                        selected = selectedTimeframeDays == days,
                        onClick = { selectedTimeframeDays = days },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Macro Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Turnover Rate Card
                TurnoverMetricSummaryCard(
                    title = "Taxa de Giro Global",
                    value = String.format(Locale.getDefault(), "%.2fx", globalTurnover),
                    subtext = if (globalTurnover >= 1.0) "Giro rápido" else if (globalTurnover > 0.3) "Giro moderado" else "Giro baixo",
                    icon = Icons.Default.Autorenew,
                    accentColor = if (globalTurnover >= 1.0) EmeraldGreen else if (globalTurnover > 0.3) RoyalBlue else AmberWarning,
                    modifier = Modifier.weight(1f)
                )

                // DSI Card (Days of Inventory)
                TurnoverMetricSummaryCard(
                    title = "Cobertura Média",
                    value = if (globalDsi > 0.1 && globalDsi < 365) "${globalDsi.toInt()} dias" else "N/A",
                    subtext = "Tempo estimado de estoque",
                    icon = Icons.Default.HourglassBottom,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visual Mode Tabs (Giro por Categoria | Gráfico Donut de Saldo | Curva ABC)
            TabRow(
                selectedTabIndex = selectedVisualTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = selectedVisualTab == 0,
                    onClick = { selectedVisualTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Giro por Cat.", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedVisualTab == 1,
                    onClick = { selectedVisualTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Distribuição", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedVisualTab == 2,
                    onClick = { selectedVisualTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Curva ABC", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TAB 0: Category Turnover Multi-Bars
            if (selectedVisualTab == 0) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Eficiência de Giro por Categoria",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (categoryTurnovers.isEmpty() || globalStock <= 0.001) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum produto cadastrado para calcular o giro.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        categoryTurnovers.forEach { metric ->
                            CategoryTurnoverBarItem(metric = metric)
                        }
                    }

                    // Legend description
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Taxa de Giro = Saídas do Período ÷ Estoque Médio. Quanto maior a taxa, mais rápido o produto se converte e renova sem gerar perdas.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // TAB 1: D3/Recharts-Style Interactive Native Canvas Donut Chart
            if (selectedVisualTab == 1) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Participação do Saldo em Estoque",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Native Canvas Donut Chart
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CanvasDonutChart(
                            slices = donutSlices,
                            selectedIndex = selectedSliceIndex,
                            onSliceSelected = { index ->
                                selectedSliceIndex = if (selectedSliceIndex == index) -1 else index
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Center Text
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            if (selectedSliceIndex in donutSlices.indices) {
                                val current = donutSlices[selectedSliceIndex]
                                Text(
                                    text = current.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = current.color,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", current.percentage),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%.1f", current.value)} un",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Total Geral",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f", globalStock),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "unidades",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Interactive Donut Legend Badges
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        donutSlices.forEachIndexed { index, slice ->
                            val isSelected = selectedSliceIndex == index
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) slice.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, slice.color) else null,
                                modifier = Modifier.clickable {
                                    selectedSliceIndex = if (isSelected) -1 else index
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(slice.color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${slice.label} (${String.format(Locale.getDefault(), "%.1f%%", slice.percentage)})",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) slice.color else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TAB 2: Curva ABC de Estoque (Pareto 80/15/5)
            if (selectedVisualTab == 2) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Curva ABC (Princípio de Pareto)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // ABC Explanation Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        Box(modifier = Modifier.weight(0.80f).fillMaxHeight().background(EmeraldGreen))
                        Box(modifier = Modifier.weight(0.15f).fillMaxHeight().background(RoyalBlue))
                        Box(modifier = Modifier.weight(0.05f).fillMaxHeight().background(AmberWarning))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Classe A (80% giro)", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        Text("Classe B (15%)", fontSize = 11.sp, color = RoyalBlue, fontWeight = FontWeight.Bold)
                        Text("Classe C (5%)", fontSize = 11.sp, color = AmberWarning, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (abcItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhuma movimentação registrada no período.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    } else {
                        abcItems.take(8).forEach { item ->
                            AbcProductRowItem(item = item)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Metric KPI Box for Turnover
 */
@Composable
private fun TurnoverMetricSummaryCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtext,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Visual multi-bar item representing a category's turnover rate and health
 */
@Composable
private fun CategoryTurnoverBarItem(metric: CategoryTurnoverMetric) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(parseHexColor(metric.colorHex), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = metric.categoryName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = metric.classification.color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${metric.classification.label} (${String.format(Locale.getDefault(), "%.2fx", metric.turnoverRate)})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = metric.classification.color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progressive Visual Turnover Bar
            val fillFraction = (metric.turnoverRate / 3.0f).coerceIn(0.04, 1.0).toFloat()
            val animatedFraction by animateFloatAsState(
                targetValue = fillFraction,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "barAnimation"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(metric.classification.color)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Saídas: ${String.format(Locale.getDefault(), "%.1f", metric.periodExits)} un | Saldo: ${String.format(Locale.getDefault(), "%.1f", metric.currentStock)} un",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (metric.daysOfInventory < 365) "Cobertura: ${metric.daysOfInventory.toInt()} dias" else "Sem saídas",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = metric.classification.color
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Diagnóstico: ${metric.classification.description}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

/**
 * Custom Canvas Donut Chart with touch selection
 */
@Composable
private fun CanvasDonutChart(
    slices: List<DonutSliceData>,
    selectedIndex: Int,
    onSliceSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(slices) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val dx = offset.x - centerX
                    val dy = offset.y - centerY
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                    val radius = (size.width.coerceAtMost(size.height) / 2f)

                    // Check if tap inside ring
                    if (distance in (radius * 0.45f)..(radius * 1.05f)) {
                        var angle = (atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f
                        // Canvas starts at -90 degrees (top)
                        angle = (angle + 90f) % 360f

                        var accumulatedAngle = 0f
                        slices.forEachIndexed { index, slice ->
                            val sweep = (slice.percentage.toFloat() / 100f) * 360f
                            if (angle in accumulatedAngle..(accumulatedAngle + sweep)) {
                                onSliceSelected(index)
                                return@detectTapGestures
                            }
                            accumulatedAngle += sweep
                        }
                    } else if (distance < radius * 0.45f) {
                        onSliceSelected(-1) // reset
                    }
                }
            }
    ) {
        val diameter = size.minDimension
        val strokeWidth = diameter * 0.18f
        val arcSize = Size(diameter - strokeWidth, diameter - strokeWidth)
        val topLeft = Offset(
            (size.width - arcSize.width) / 2f,
            (size.height - arcSize.height) / 2f
        )

        var startAngle = -90f

        slices.forEachIndexed { index, slice ->
            val sweepAngle = ((slice.percentage.toFloat() / 100f) * 360f).coerceAtLeast(0.5f)
            val isSelected = index == selectedIndex
            val currentStroke = if (isSelected) strokeWidth * 1.25f else strokeWidth

            drawArc(
                color = slice.color,
                startAngle = startAngle + 1f, // 1 deg gap for clean visual separation
                sweepAngle = (sweepAngle - 2f).coerceAtLeast(0.1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = currentStroke, cap = StrokeCap.Round)
            )

            startAngle += sweepAngle
        }
    }
}

/**
 * Item row for Pareto ABC curve
 */
@Composable
private fun AbcProductRowItem(item: AbcProductItem) {
    val tagColor = when (item.classification) {
        "A" -> EmeraldGreen
        "B" -> RoyalBlue
        else -> AmberWarning
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tagColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Classe ${item.classification}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tagColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = item.productName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Cat: ${item.category} • Saldo: ${String.format(Locale.getDefault(), "%.1f", item.currentStock)} un",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(Locale.getDefault(), "%.1f", item.exitsQty)} saídas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${String.format(Locale.getDefault(), "%.1f%%", item.sharePercent)} do total",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
