package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductWithLots
import com.example.data.model.StockLot
import com.example.ui.components.formatQuantity
import com.example.ui.theme.RoseRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LocationDistributionTab(
    productsWithLots: List<ProductWithLots>,
    alertDays: Int
) {
    // 1. Process data
    val now = System.currentTimeMillis()
    
    // Create a list of all active lots wrapped with their product name/unit
    data class LotDetail(val lot: StockLot, val productName: String, val unit: String, val effectiveLocation: String)
    
    val allLots = productsWithLots.flatMap { p -> 
        p.lots.filter { it.quantity > 0.001 }.map { 
            val loc = it.location.ifBlank { p.product.location }.ifBlank { "Sem Local" }
            LotDetail(it, p.product.name, p.product.unit, loc) 
        }
    }
    
    // Group by location
    val lotsByLocation = allLots.groupBy { it.effectiveLocation }
    
    // Calculate totals per location
    data class LocationStats(
        val location: String,
        val totalQuantity: Double,
        val expiringLots: List<LotDetail> // Lots expiring within alertDays or already expired
    )
    
    val stats = lotsByLocation.map { (loc, lots) ->
        val totalQty = lots.sumOf { it.lot.quantity }
        val expiring = lots.filter { it.lot.daysUntilExpiration(now) <= alertDays }
            .sortedBy { it.lot.expirationDate }
            
        LocationStats(loc, totalQty, expiring)
    }.sortedByDescending { it.totalQuantity }

    if (stats.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Nenhum saldo físico disponível para distribuir por locais.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxQty = stats.maxOfOrNull { it.totalQuantity } ?: 1.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Text(
            text = "Distribuição de Volume (Qtd Total)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // Native Horizontal Bar Chart
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stats.forEach { stat ->
                    val proportion = (stat.totalQuantity / maxQty).toFloat().coerceIn(0f, 1f)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stat.location,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatQuantity(stat.totalQuantity),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // The Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(proportion)
                                    .height(12.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                            )
                        }
                        
                        if (stat.expiringLots.isNotEmpty()) {
                            Text(
                                text = "${stat.expiringLots.size} lote(s) requerem atenção (FEFO)",
                                fontSize = 11.sp,
                                color = RoseRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Lotes Críticos por Local (FEFO)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Expiring Lots Breakdown
        stats.filter { it.expiringLots.isNotEmpty() }.forEach { stat ->
            Text(
                text = "📍 ${stat.location}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            
            stat.expiringLots.forEach { detail ->
                val days = detail.lot.daysUntilExpiration(now)
                val isExpired = days < 0
                val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = RoseRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = RoseRed, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = detail.productName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            val lotStr = if (detail.lot.lotNumber.isNotBlank()) "Lote: ${detail.lot.lotNumber}" else "S/N"
                            Text(
                                text = "$lotStr • Saldo: ${formatQuantity(detail.lot.quantity)} ${detail.unit}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Vence em: ${dateFmt.format(Date(detail.lot.expirationDate))} (${if (isExpired) "Vencido" else "$days dias"})",
                                fontSize = 12.sp,
                                color = RoseRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
