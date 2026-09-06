package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MovementType
import com.example.data.model.StockMovement
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMovementDialog(
    movement: StockMovement,
    onSaveCorrection: (
        movementId: Long,
        newQuantity: Double,
        newExpirationDate: Long?,
        newReason: String,
        newDocumentNumber: String,
        newNotes: String
    ) -> Unit,
    onDeleteOrReverse: (movementId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var quantityText by rememberSaveable { mutableStateOf(movement.quantity.toString().removeSuffix(".0")) }
    var expirationDate by rememberSaveable { mutableStateOf(movement.lotExpirationDate) }
    var reason by rememberSaveable { mutableStateOf(movement.reason) }
    var documentNumber by rememberSaveable { mutableStateOf(movement.documentNumber) }
    var notes by rememberSaveable { mutableStateOf(movement.notes) }

    var quantityError by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showReverseConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = expirationDate ?: System.currentTimeMillis()
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
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
                    Column {
                        Text(
                            text = "Corrigir Lançamento",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Edite quantidade ou data de entrada incorreta",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val badgeColor = when (movement.type) {
                        MovementType.ENTRADA -> EmeraldGreen
                        MovementType.SAIDA, MovementType.DESCARTE_VENCIDO -> RoseRed
                        MovementType.AJUSTE, MovementType.TRANSFERENCIA -> MaterialTheme.colorScheme.primary
                    }

                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = movement.type.label,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Product Info Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = movement.productName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (movement.productBrand.isNotBlank()) {
                            Text(
                                text = "Marca: ${movement.productBrand}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Registrado em: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(movement.timestamp))}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quantity Input
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                        quantityError = false
                    },
                    label = { Text("Quantidade Correta *") },
                    leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = quantityError,
                    supportingText = {
                        if (quantityError) Text("Informe uma quantidade válida maior que zero")
                        else Text("O saldo do lote e do produto serão recalculados automaticamente")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_correct_quantity"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Expiration Date (if relevant)
                if (expirationDate != null || movement.type == MovementType.ENTRADA) {
                    OutlinedTextField(
                        value = expirationDate?.let { dateFormat.format(Date(it)) } ?: "Não informada",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data de Validade Correta") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Alterar Validade")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Reason / Justification
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo / Justificativa da Correção") },
                    placeholder = { Text("Ex: Correção de digitação de NF") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Document / Invoice Number
                OutlinedTextField(
                    value = documentNumber,
                    onValueChange = { documentNumber = it },
                    label = { Text("Documento / NF (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações Adicionais") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reverse / Delete button
                    IconButton(
                        onClick = { showReverseConfirmDialog = true },
                        modifier = Modifier.testTag("btn_reverse_movement")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Estornar Lançamento",
                            tint = RoseRed
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                val cleanQty = quantityText.replace(",", ".").toDoubleOrNull()
                                if (cleanQty == null || cleanQty <= 0) {
                                    quantityError = true
                                    return@Button
                                }

                                onSaveCorrection(
                                    movement.id,
                                    cleanQty,
                                    expirationDate,
                                    reason.trim().ifBlank { "Correção de Lançamento" },
                                    documentNumber.trim(),
                                    notes.trim()
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("btn_confirm_correction")
                        ) {
                            Text("Salvar Correção", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            expirationDate = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showReverseConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showReverseConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = RoseRed) },
            title = { Text("Estornar Lançamento?") },
            text = {
                Text(
                    "Esta ação removerá este lançamento e reverterá o saldo em estoque correspondente.\n\nDeseja realmente estornar?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReverseConfirmDialog = false
                        onDeleteOrReverse(movement.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Text("Sim, Estornar Lançamento")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReverseConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
