package com.example.ui.dialogs

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Product
import com.example.data.model.StockLot
import com.example.ui.components.ExpirationBadge
import com.example.ui.components.formatDateOnly
import com.example.ui.components.formatQuantity
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryAuditDialog(
    product: Product,
    lots: List<StockLot>,
    initialLot: StockLot? = null,
    onDismiss: () -> Unit,
    onConfirmAudit: (lotId: Long, physicalCount: Double, notes: String) -> Unit
) {
    var selectedLotId by rememberSaveable {
        mutableStateOf(initialLot?.id ?: lots.firstOrNull()?.id ?: 0L)
    }
    val currentLot = lots.find { it.id == selectedLotId } ?: lots.firstOrNull()

    var physicalCountText by rememberSaveable {
        mutableStateOf(currentLot?.quantity?.toString()?.removeSuffix(".0") ?: "0")
    }
    var notes by rememberSaveable { mutableStateOf("") }
    var countError by rememberSaveable { mutableStateOf(false) }

    var lotDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    val registeredStock = currentLot?.quantity ?: 0.0
    val physicalStock = physicalCountText.replace(",", ".").toDoubleOrNull() ?: registeredStock
    val difference = physicalStock - registeredStock

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_inventory_audit")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PurpleAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Ajuste de Saldo / Inventário",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val sub = if (product.brand.isNotBlank()) "${product.name} (${product.brand})" else product.name
                            Text(
                                text = sub,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Expiration batch selector if multiple
                if (lots.size > 1) {
                    ExposedDropdownMenuBox(
                        expanded = lotDropdownExpanded,
                        onExpandedChange = { lotDropdownExpanded = !lotDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "Venc: ${formatDateOnly(currentLot?.expirationDate ?: 0L)} (Saldo: ${formatQuantity(currentLot?.quantity ?: 0.0)} ${product.unit})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Validade para Ajuste") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lotDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = lotDropdownExpanded,
                            onDismissRequest = { lotDropdownExpanded = false }
                        ) {
                            lots.forEach { lot ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("Validade: ${formatDateOnly(lot.expirationDate)}", fontWeight = FontWeight.Bold)
                                            Text(
                                                "Saldo Atual: ${formatQuantity(lot.quantity)} ${product.unit}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedLotId = lot.id
                                        physicalCountText = lot.quantity.toString().removeSuffix(".0")
                                        lotDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else if (currentLot != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Validade Selecionada", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatDateOnly(currentLot.expirationDate), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            ExpirationBadge(expirationDate = currentLot.expirationDate, quantity = currentLot.quantity)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Balance Comparison Box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Saldo Sistema", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${formatQuantity(registeredStock)} ${product.unit}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Divergência", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val diffText = when {
                                difference > 0 -> "+${formatQuantity(difference)} ${product.unit} (Sobra)"
                                difference < 0 -> "${formatQuantity(difference)} ${product.unit} (Falta)"
                                else -> "0 (Sem divergência)"
                            }
                            val diffColor = when {
                                difference > 0 -> EmeraldGreen
                                difference < 0 -> RoseRed
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                text = diffText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = diffColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Physical Count Input
                OutlinedTextField(
                    value = physicalCountText,
                    onValueChange = {
                        physicalCountText = it
                        countError = false
                    },
                    label = { Text("Contagem Física Apurada (${product.unit}) *") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = countError,
                    supportingText = { if (countError) Text("Informe a quantidade contada") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_physical_count"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Motivo / Observações
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observação do Ajuste (opcional)") },
                    placeholder = { Text("Ex: Contagem física quinzenal de almoxarifado") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val count = physicalCountText.replace(",", ".").toDoubleOrNull()
                            if (count == null || count < 0) {
                                countError = true
                                return@Button
                            }
                            if (currentLot != null) {
                                onConfirmAudit(currentLot.id, count, notes.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        modifier = Modifier.testTag("btn_confirm_audit")
                    ) {
                        Text("Aplicar Ajuste de Saldo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
