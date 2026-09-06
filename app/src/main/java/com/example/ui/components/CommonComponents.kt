package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MovementType
import com.example.data.model.StockHealthStatus
import com.example.data.model.StockLot
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningLight
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleAccentLight
import com.example.ui.theme.RoseRed
import com.example.ui.theme.RoseRedLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatQuantity(value: Double, unit: String = ""): String {
    val isInteger = value % 1.0 == 0.0
    val formatted = if (isInteger) value.toLong().toString() else String.format(Locale.forLanguageTag("pt-BR"), "%.2f", value)
    return if (unit.isNotBlank()) "$formatted $unit" else formatted
}

fun parseHexColor(hex: String, defaultColor: Color = Color(0xFF3B82F6)): Color {
    return try {
        if (hex.isBlank()) defaultColor
        else Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
    } catch (e: Exception) {
        defaultColor
    }
}


fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.forLanguageTag("pt-BR"))
    return sdf.format(Date(timestamp))
}

fun formatDateOnly(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
    return sdf.format(Date(timestamp))
}

fun formatDateShort(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yy", Locale.forLanguageTag("pt-BR"))
    return sdf.format(Date(timestamp))
}

@Composable
fun ExpirationBadge(
    expirationDate: Long,
    quantity: Double = 1.0,
    alertDays: Int = 30,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val diffMs = expirationDate - now
    val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMs)

    val criticalDaysThreshold = (alertDays / 2).coerceAtLeast(7)

    val (bgColor, textColor, icon, label) = when {
        quantity <= 0.001 -> Quadruple(
            Color(0xFFE2E8F0),
            Color(0xFF64748B),
            Icons.Default.CheckCircle,
            "Esgotado"
        )
        days < 0 -> Quadruple(
            RoseRedLight,
            RoseRed,
            Icons.Default.ErrorOutline,
            "Vencido (${Math.abs(days)}d atrás)"
        )
        days == 0L -> Quadruple(
            RoseRedLight,
            RoseRed,
            Icons.Default.HourglassBottom,
            "Vence Hoje!"
        )
        days <= criticalDaysThreshold -> Quadruple(
            RoseRedLight,
            RoseRed,
            Icons.Default.HourglassBottom,
            "Vence em ${days}d (Crítico)"
        )
        days <= alertDays -> Quadruple(
            AmberWarningLight,
            AmberWarning,
            Icons.Default.HourglassTop,
            "Vence em ${days}d"
        )
        else -> Quadruple(
            EmeraldGreenLight,
            EmeraldGreen,
            Icons.Default.CheckCircle,
            "Vence: ${formatDateOnly(expirationDate)}"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun StatusChip(status: StockHealthStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, icon) = when (status) {
        StockHealthStatus.IN_STOCK -> Triple(EmeraldGreenLight, EmeraldGreen, Icons.Default.CheckCircle)
        StockHealthStatus.LOW_STOCK -> Triple(AmberWarningLight, AmberWarning, Icons.Default.WarningAmber)
        StockHealthStatus.OUT_OF_STOCK -> Triple(RoseRedLight, RoseRed, Icons.Default.ErrorOutline)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status.label,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MovementTypeBadge(type: MovementType, modifier: Modifier = Modifier) {
    val (bgColor, textColor, icon) = when (type) {
        MovementType.ENTRADA -> Triple(EmeraldGreenLight, EmeraldGreen, Icons.Default.ArrowDownward)
        MovementType.SAIDA -> Triple(Color(0xFFDBEAFE), Color(0xFF1D4ED8), Icons.Default.ArrowUpward)
        MovementType.AJUSTE -> Triple(PurpleAccentLight, PurpleAccent, Icons.Default.Sync)
        MovementType.DESCARTE_VENCIDO -> Triple(RoseRedLight, RoseRed, Icons.Default.DeleteSweep)
        MovementType.TRANSFERENCIA -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), Icons.Default.SwapHoriz)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = type.label,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = type.label.uppercase(),
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StockProgressBar(
    current: Double,
    min: Double,
    max: Double,
    modifier: Modifier = Modifier
) {
    val safeMax = if (max > 0) max else (min * 2).coerceAtLeast(10.0)
    val progress = (current / safeMax).toFloat().coerceIn(0f, 1f)

    val progressColor = when {
        current <= 0.001 -> RoseRed
        current <= min -> AmberWarning
        current > safeMax -> PurpleAccent
        else -> EmeraldGreen
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Mín: ${formatQuantity(min)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Máx: ${formatQuantity(safeMax)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .testTag(testTag)
            .border(
                1.dp,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
