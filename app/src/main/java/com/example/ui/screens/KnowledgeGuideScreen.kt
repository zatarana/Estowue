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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningLight
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleAccentLight
import com.example.ui.theme.RoseRed
import com.example.ui.theme.RoseRedLight

@Composable
fun KnowledgeGuideScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Guia Prático: Lotes & Validades",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Diretrizes essenciais de armazenagem, método FEFO e prevenção de perdas.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 1: O Método FEFO / PVPS
        item {
            GuideMethodCard(
                title = "Método FEFO / PVPS (Primeiro que Vence, Primeiro que Sai)",
                badgeText = "Regra de Ouro",
                badgeBg = EmeraldGreenLight,
                badgeColor = EmeraldGreen,
                icon = Icons.Default.AccessTime,
                iconBg = EmeraldGreenLight,
                iconTint = EmeraldGreen,
                description = "O método FEFO (First Expired, First Out) estabelece que os lotes com a data de validade mais próxima devem ser obrigatoriamente expedidos ou consumidos primeiro, independentemente de quando chegaram ao armazém.",
                points = listOf(
                    "Diferença do FIFO/PEPS: No FIFO sai o que chegou primeiro; no FEFO sai o que vence primeiro (essencial para alimentos, fármacos e cosméticos).",
                    "Redução de perdas: Evita que lotes antigos fiquem esquecidos no fundo das prateleiras.",
                    "Conformidade: Garante que os clientes finais recebam sempre produtos dentro do prazo seguro."
                )
            )
        }

        // Section 2: Zonas de Risco de Validade
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AmberWarningLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Zonas de Controle de Validade",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    StatusExplanationRow(
                        color = RoseRed,
                        title = "Lotes Vencidos (< 0 dias)",
                        explanation = "Ação imediata de quarentena e descarte regulamentar. Não podem ser comercializados ou utilizados."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StatusExplanationRow(
                        color = AmberWarning,
                        title = "Nível Crítico (0 a 15 dias)",
                        explanation = "Prioridade máxima de expedição (regra FEFO). Considere consumo acelerado."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StatusExplanationRow(
                        color = Color(0xFFD97706),
                        title = "Atenção (16 a 45 dias)",
                        explanation = "Monitoramento constante e posicionamento na frente dos pontos de separação."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StatusExplanationRow(
                        color = EmeraldGreen,
                        title = "Lotes Válidos (> 45 dias)",
                        explanation = "Armazenagem regular e estoque de segurança preservado."
                    )
                }
            }
        }

        // Section 3: Rastreabilidade por Lote
        item {
            GuideMethodCard(
                title = "Rastreabilidade e Identificação de Lote",
                badgeText = "Qualidade",
                badgeBg = PurpleAccentLight,
                badgeColor = PurpleAccent,
                icon = Icons.Default.Security,
                iconBg = PurpleAccentLight,
                iconTint = PurpleAccent,
                description = "Cada lote representa um conjunto homogêneo de itens produzidos ou recebidos sob as mesmas condições e data.",
                points = listOf(
                    "Gestão de Recall: Se houver defeito de fábrica em um lote, é possível recolher apenas o lote afetado sem descartar todo o estoque.",
                    "Localização Física: Separe fisicamente os lotes no depósito para evitar misturas acidentais durante a separação.",
                    "Etiquetagem Visível: Mantenha as datas de validade voltadas para o corredor de circulação."
                )
            )
        }

        // Section 4: Inventário Rotativo de Lotes
        item {
            GuideMethodCard(
                title = "Auditoria e Inventário Rotativo",
                badgeText = "Rotina",
                badgeBg = MaterialTheme.colorScheme.primaryContainer,
                badgeColor = MaterialTheme.colorScheme.primary,
                icon = Icons.AutoMirrored.Filled.FactCheck,
                iconBg = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                description = "Contagens físicas periódicas concentradas em lotes específicos para assegurar que a quantidade e a validade registradas no sistema coincidem 100% com o armazém real.",
                points = listOf(
                    "Conferência dupla: Conte a quantidade física e confirme a data impressa na embalagem.",
                    "Ajuste ágil: Registre sobras ou faltas imediatamente no botão de ajuste de lote.",
                    "Descarte transparente: Movimentações de descarte vencido mantêm o histórico limpo e auditável."
                )
            )
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun StatusExplanationRow(color: Color, title: String, explanation: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color)
            Text(explanation, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun GuideMethodCard(
    title: String,
    badgeText: String,
    badgeBg: Color,
    badgeColor: Color,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    description: String,
    points: List<String>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(iconBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                points.forEach { pt ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("• ", fontWeight = FontWeight.Bold, color = iconTint)
                        Text(
                            text = pt,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
