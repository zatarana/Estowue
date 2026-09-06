package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MovementType
import com.example.data.model.Product
import com.example.data.model.ProductWithLots
import com.example.data.model.StockLot
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.ExpirationBadge
import com.example.ui.components.formatDateOnly
import com.example.ui.components.formatQuantity
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockMovementDialog(
    initialProduct: Product? = null,
    initialLot: StockLot? = null,
    allProductsWithLots: List<ProductWithLots>,
    locationsList: List<String> = emptyList(),
    initialType: MovementType = MovementType.ENTRADA,
    onDismiss: () -> Unit,
    onConfirmIn: (productId: Long, expDate: Long, qty: Double, mfgDate: Long?, location: String, reason: String, doc: String, notes: String) -> Unit,
    onConfirmOut: (lotId: Long, qty: Double, isDiscard: Boolean, reason: String, doc: String, notes: String) -> Unit,
    onConfirmTransfer: (lotId: Long, qty: Double, destLocation: String, doc: String, notes: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var selectedType by rememberSaveable {
        mutableStateOf(if (initialType == MovementType.ENTRADA) MovementType.ENTRADA else MovementType.SAIDA)
    }

    var selectedProductId by rememberSaveable {
        mutableStateOf(initialProduct?.id ?: allProductsWithLots.firstOrNull()?.product?.id ?: 0L)
    }

    val currentProductWithLots = allProductsWithLots.find { it.product.id == selectedProductId }
    val product = currentProductWithLots?.product

    // Product Dropdown state
    var productDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var showBarcodeScanner by rememberSaveable { mutableStateOf(false) }

    // Common Form States
    var quantityText by rememberSaveable { mutableStateOf("") }
    var documentNumber by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var quantityError by rememberSaveable { mutableStateOf<String?>(null) }
    var dateError by rememberSaveable { mutableStateOf<String?>(null) }

    // --- ENTRADA STATES ---
    var lotLocation by rememberSaveable { mutableStateOf(product?.location ?: "") }
    var inReason by rememberSaveable { mutableStateOf("Recebimento de Mercadoria") }

    // Expiration date calculation helper
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, 30) // default 30 days
    var expirationTimestamp by rememberSaveable { mutableStateOf(initialLot?.expirationDate ?: cal.timeInMillis) }
    var customDateString by rememberSaveable {
        mutableStateOf(SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Date(expirationTimestamp)))
    }

    // --- SAÍDA STATES ---
    val availableLots = currentProductWithLots?.lots?.filter { it.quantity > 0.001 } ?: emptyList()
    val fefoRecommendedLot = availableLots.minByOrNull { it.expirationDate }

    var selectedLotId by rememberSaveable {
        mutableStateOf(initialLot?.id ?: fefoRecommendedLot?.id ?: 0L)
    }
    val selectedLot = availableLots.find { it.id == selectedLotId } ?: fefoRecommendedLot

    var isDiscard by rememberSaveable {
        mutableStateOf(initialType == MovementType.DESCARTE_VENCIDO || (selectedLot?.daysUntilExpiration() ?: 1) < 0)
    }
    var outReason by rememberSaveable {
        mutableStateOf(if (isDiscard) "Descarte por Vencimento" else "Expedição / Consumo")
    }

    // --- TRANSFERÊNCIA STATES ---
    var destLocation by rememberSaveable { mutableStateOf("") }
    var destLocationDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_stock_movement")
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
                    Column {
                        Text(
                            text = "Movimentação de Estoque",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Registro de entrada ou saída por validade",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Selector: Entrada vs Saída vs Transferência
                TabRow(
                    selectedTabIndex = when (selectedType) {
                        MovementType.ENTRADA -> 0
                        MovementType.SAIDA -> 1
                        MovementType.TRANSFERENCIA -> 2
                        else -> 1
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedType == MovementType.ENTRADA,
                        onClick = { selectedType = MovementType.ENTRADA },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Entrada", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedType == MovementType.SAIDA,
                        onClick = { selectedType = MovementType.SAIDA },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Saída", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedType == MovementType.TRANSFERENCIA,
                        onClick = { selectedType = MovementType.TRANSFERENCIA },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Transferir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Product Selector Row with Barcode Scanner Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = productDropdownExpanded,
                        onExpandedChange = { productDropdownExpanded = !productDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        val prodDisplay = if (product != null) {
                            if (product.brand.isNotBlank()) "${product.name} • ${product.brand}" else product.name
                        } else {
                            "Selecione um produto"
                        }
                        OutlinedTextField(
                            value = prodDisplay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Produto") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("select_movement_product")
                        )
                        ExposedDropdownMenu(
                            expanded = productDropdownExpanded,
                            onDismissRequest = { productDropdownExpanded = false }
                        ) {
                            allProductsWithLots.forEach { pWithLots ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(pWithLots.product.name, fontWeight = FontWeight.Bold)
                                            val brandInfo = if (pWithLots.product.brand.isNotBlank()) "Marca: ${pWithLots.product.brand} • " else ""
                                            val barcodeInfo = if (pWithLots.product.barcode.isNotBlank()) "EAN: ${pWithLots.product.barcode} • " else ""
                                            val subtitle = "$brandInfo${barcodeInfo}Saldo: ${formatQuantity(pWithLots.totalQuantity)} ${pWithLots.product.unit}"
                                            Text(
                                                subtitle,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedProductId = pWithLots.product.id
                                        lotLocation = pWithLots.product.location
                                        productDropdownExpanded = false
                                        selectedLotId = pWithLots.lots.filter { it.quantity > 0.001 }.minByOrNull { it.expirationDate }?.id ?: 0L
                                    }
                                )
                            }
                        }
                    }

                    // Quick Barcode Scan Icon Button
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(54.dp)
                    ) {
                        IconButton(
                            onClick = { showBarcodeScanner = true },
                            modifier = Modifier.fillMaxSize().testTag("btn_scan_movement_barcode")
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "Escanear Código de Barras",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==================== ENTRADA FORM ====================
                if (selectedType == MovementType.ENTRADA) {
                    // Expiration Date (Validade)
                    Text(
                        text = "Data de Validade *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quick date presets (+7d, +15d, +30d, +60d, +180d, +1 ano)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf(
                            Pair("+7 dias", 7),
                            Pair("+15 dias", 15),
                            Pair("+30 dias", 30),
                            Pair("+60 dias", 60),
                            Pair("+6 meses", 180),
                            Pair("+1 ano", 365)
                        )
                        items(presets.size) { idx ->
                            val (lbl, days) = presets[idx]
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val c = Calendar.getInstance()
                                    c.add(Calendar.DAY_OF_MONTH, days)
                                    expirationTimestamp = c.timeInMillis
                                    customDateString = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Date(expirationTimestamp))
                                    dateError = null
                                },
                                label = { Text(lbl, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customDateString,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(8)
                            customDateString = digits
                            dateError = null
                            if (digits.length == 8) {
                                try {
                                    val sdf = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).apply { isLenient = false }
                                    val parsed = sdf.parse(digits)
                                    if (parsed != null) {
                                        expirationTimestamp = parsed.time
                                    } else {
                                        dateError = "Data de validade inválida"
                                    }
                                } catch (_: Exception) {
                                    dateError = "Data inválida (ex: 31/12/2026)"
                                }
                            }
                        },
                        label = { Text("Data de Validade (DD/MM/AAAA) *") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        placeholder = { Text("Ex: 15102026") },
                        isError = dateError != null,
                        supportingText = { dateError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = DateVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_lot_expiration_date"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quantidade a dar entrada
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = {
                            quantityText = it
                            quantityError = null
                        },
                        label = { Text("Quantidade a Inserir (${product?.unit ?: "un"}) *") },
                        leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = quantityError != null,
                        supportingText = { quantityError?.let { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_movement_quantity"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Motivo da Entrada
                    OutlinedTextField(
                        value = inReason,
                        onValueChange = { inReason = it },
                        label = { Text("Motivo da Entrada") },
                        placeholder = { Text("Ex: Recebimento Fornecedor, Reposição") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Localização / NF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = lotLocation,
                            onValueChange = { lotLocation = it },
                            label = { Text("Local no Depósito") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = documentNumber,
                            onValueChange = { documentNumber = it },
                            label = { Text("Doc / NF / Pedido") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                } else {
                    // ==================== SAÍDA FORM ====================
                    if (availableLots.isEmpty()) {
                        Surface(
                            color = RoseRed.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Não há saldo disponível para este produto.",
                                color = RoseRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        // Lot Selection List with FEFO Recommendation
                        Text(
                            text = "Selecione a Validade de Origem (FEFO) *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableLots.forEach { lot ->
                                val isSelected = (selectedLot?.id == lot.id)
                                val isFefo = (fefoRecommendedLot?.id == lot.id)

                                OutlinedCard(
                                    onClick = {
                                        selectedLotId = lot.id
                                        isDiscard = lot.daysUntilExpiration() < 0
                                        outReason = if (isDiscard) "Descarte por Vencimento" else "Expedição / Consumo"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Venc: ${formatDateOnly(lot.expirationDate)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                if (isFefo) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = Color(0xFFFEF3C7),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(11.dp))
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text("Prioritário (FEFO)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                                        }
                                                    }
                                                }
                                            }
                                            val lotLoc = lot.location.ifBlank { product?.location ?: "" }
                                            val locSuffix = if (lotLoc.isNotBlank()) " • Local: $lotLoc" else ""
                                            Text(
                                                text = "Saldo: ${formatQuantity(lot.quantity)} ${product?.unit ?: "un"}$locSuffix",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        ExpirationBadge(expirationDate = lot.expirationDate, quantity = lot.quantity)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (selectedType == MovementType.TRANSFERENCIA) {
                            val sourceLoc = selectedLot?.location?.ifBlank { product?.location ?: "" } ?: ""
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Origem atual: ${sourceLoc.ifBlank { "Sem Local" }}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        // Quantidade de Saída
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = {
                                quantityText = it
                                quantityError = null
                            },
                            label = { Text("Quantidade a Retirar (${product?.unit ?: "un"}) *") },
                            leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = quantityError != null,
                            supportingText = {
                                if (quantityError != null) {
                                    Text(quantityError ?: "")
                                } else {
                                    Text("Disponível nesta validade: ${formatQuantity(selectedLot?.quantity ?: 0.0)} ${product?.unit ?: "un"}")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_movement_quantity"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (selectedType == MovementType.TRANSFERENCIA) {
                            // Transferência specific fields
                            ExposedDropdownMenuBox(
                                expanded = destLocationDropdownExpanded,
                                onExpandedChange = { destLocationDropdownExpanded = !destLocationDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = destLocation,
                                    onValueChange = { destLocation = it },
                                    label = { Text("Destino (Novo Local) *") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destLocationDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    singleLine = true
                                )
                                if (locationsList.isNotEmpty()) {
                                    ExposedDropdownMenu(
                                        expanded = destLocationDropdownExpanded,
                                        onDismissRequest = { destLocationDropdownExpanded = false }
                                    ) {
                                        locationsList.forEach { loc ->
                                            DropdownMenuItem(
                                                text = { Text(loc) },
                                                onClick = {
                                                    destLocation = loc
                                                    destLocationDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        } else {
                            // Is discard toggle chip (Only for SAIDA)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !isDiscard,
                                    onClick = {
                                        isDiscard = false
                                        outReason = "Expedição / Consumo"
                                    },
                                    label = { Text("Saída Normal") }
                                )
                                FilterChip(
                                    selected = isDiscard,
                                    onClick = {
                                        isDiscard = true
                                        outReason = "Descarte por Vencimento"
                                    },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = RoseRed, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Descarte (Vencido/Avaria)")
                                        }
                                    }
                                )
                            }
    
                            Spacer(modifier = Modifier.height(10.dp))
    
                            // Motivo da Saída
                            OutlinedTextField(
                                value = outReason,
                                onValueChange = { outReason = it },
                                label = { Text("Motivo da Saída") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
    
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Doc / Requisição
                        OutlinedTextField(
                            value = documentNumber,
                            onValueChange = { documentNumber = it },
                            label = { Text(if (selectedType == MovementType.TRANSFERENCIA) "Documento / OP (opcional)" else "Nº Pedido / Requisição / Documento") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Observações
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)") },
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
                            val qty = quantityText.replace(",", ".").toDoubleOrNull()
                            if (qty == null || qty <= 0.0) {
                                quantityError = "Informe uma quantidade válida maior que 0"
                                return@Button
                            }

                            if (selectedType == MovementType.ENTRADA) {
                                if (customDateString.length != 8) {
                                    dateError = "Digite a data completa (8 dígitos - DD/MM/AAAA)"
                                    return@Button
                                }
                                try {
                                    val sdf = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).apply { isLenient = false }
                                    val parsed = sdf.parse(customDateString)
                                    if (parsed == null) {
                                        dateError = "Data de validade inválida"
                                        return@Button
                                    }
                                    expirationTimestamp = parsed.time
                                } catch (_: Exception) {
                                    dateError = "Data inválida. Use o formato DD/MM/AAAA"
                                    return@Button
                                }

                                onConfirmIn(
                                    selectedProductId,
                                    expirationTimestamp,
                                    qty,
                                    null,
                                    lotLocation.trim(),
                                    inReason.trim(),
                                    documentNumber.trim(),
                                    notes.trim()
                                )
                            } else if (selectedType == MovementType.TRANSFERENCIA) {
                                val currentLot = selectedLot
                                if (currentLot == null) {
                                    quantityError = "Selecione uma validade de origem"
                                    return@Button
                                }
                                if (qty > currentLot.quantity) {
                                    quantityError = "Quantidade solicitada ($qty) excede o saldo (${currentLot.quantity})"
                                    return@Button
                                }
                                if (destLocation.isBlank()) {
                                    quantityError = "Informe o destino (novo local)"
                                    return@Button
                                }
                                onConfirmTransfer(
                                    currentLot.id,
                                    qty,
                                    destLocation.trim(),
                                    documentNumber.trim(),
                                    notes.trim()
                                )
                            } else {
                                val currentLot = selectedLot
                                if (currentLot == null) {
                                    quantityError = "Selecione uma validade de origem"
                                    return@Button
                                }
                                if (qty > currentLot.quantity) {
                                    quantityError = "Quantidade solicitada ($qty) excede o saldo (${currentLot.quantity})"
                                    return@Button
                                }
                                onConfirmOut(
                                    currentLot.id,
                                    qty,
                                    isDiscard,
                                    outReason.trim(),
                                    documentNumber.trim(),
                                    notes.trim()
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (selectedType) {
                                MovementType.ENTRADA -> EmeraldGreen
                                MovementType.TRANSFERENCIA -> Color(0xFFE65100) // Orange
                                else -> if (isDiscard) RoseRed else MaterialTheme.colorScheme.primary
                            }
                        ),
                        modifier = Modifier.testTag("btn_confirm_movement")
                    ) {
                        Text(
                            text = when (selectedType) {
                                MovementType.ENTRADA -> "Confirmar Entrada"
                                MovementType.TRANSFERENCIA -> "Confirmar Transferência"
                                else -> if (isDiscard) "Confirmar Descarte" else "Confirmar Saída"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showBarcodeScanner) {
        val productsList = allProductsWithLots.map { it.product }
        BarcodeScannerDialog(
            availableProducts = productsList,
            onBarcodeScanned = { barcode ->
                val matched = productsList.find { it.barcode.equals(barcode, ignoreCase = true) }
                if (matched != null) {
                    selectedProductId = matched.id
                    lotLocation = matched.location
                    val matchedLots = allProductsWithLots.find { it.product.id == matched.id }?.lots ?: emptyList()
                    selectedLotId = matchedLots.filter { it.quantity > 0.001 }.minByOrNull { it.expirationDate }?.id ?: 0L
                } else {
                    android.widget.Toast.makeText(context, "Produto não encontrado: $barcode", android.widget.Toast.LENGTH_SHORT).show()
                }
                showBarcodeScanner = false
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}

class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1 || i == 3) out += "/"
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 3) return offset + 1
                if (offset <= 8) return offset + 2
                return 10
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                if (offset <= 10) return offset - 2
                return 8
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
