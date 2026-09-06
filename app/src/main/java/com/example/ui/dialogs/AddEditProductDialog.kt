package com.example.ui.dialogs
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.screens.parseHexColor

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.CameraAlt
import coil.compose.AsyncImage

import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    productToEdit: Product? = null,
    categoriesList: List<Category> = emptyList(),
    brandsList: List<String> = emptyList(),
    locationsList: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Product, Boolean) -> Unit
) {
    val isNew = productToEdit == null

    var name by rememberSaveable { mutableStateOf(productToEdit?.name ?: "") }
    var brand by rememberSaveable { mutableStateOf(productToEdit?.brand ?: "") }
    var barcode by rememberSaveable { mutableStateOf(productToEdit?.barcode ?: "") }
    var category by rememberSaveable {
        mutableStateOf(
            productToEdit?.category ?: categoriesList.firstOrNull()?.name ?: "Geral"
        )
    }
    var unit by rememberSaveable { mutableStateOf(productToEdit?.unit ?: "un") }
    var minStock by rememberSaveable {
        mutableStateOf(
            if (productToEdit != null && productToEdit.minStock > 0) {
                if (productToEdit.minStock % 1.0 == 0.0) productToEdit.minStock.toLong().toString()
                else productToEdit.minStock.toString()
            } else ""
        )
    }
    var location by rememberSaveable { mutableStateOf(productToEdit?.location ?: "") }
    var description by rememberSaveable { mutableStateOf(productToEdit?.description ?: "") }
    var imageUri by rememberSaveable { mutableStateOf(productToEdit?.imageUri ?: "") }

    var brandDropdownExpanded by remember { mutableStateOf(false) }
    var locationDropdownExpanded by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }
    val commonUnits = listOf("un", "kg", "g", "L", "ml", "cx", "pct", "frasco", "m", "par")

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_add_edit_product")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isNew) "Novo Produto" else "Editar Produto",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Dados do item para controle de estoque e validade",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Image Picker
                val context = LocalContext.current
                var showImageMenu by rememberSaveable { mutableStateOf(false) }
                var tempCameraUriStr by rememberSaveable { mutableStateOf("") }

                val galleryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        val internalUri = com.example.utils.ImageStorageHelper.copyImageToInternalStorage(context, uri)
                        imageUri = internalUri ?: uri.toString()
                    }
                }

                val cameraLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { success ->
                    if (success && tempCameraUriStr.isNotBlank()) {
                        val uri = Uri.parse(tempCameraUriStr)
                        val internalUri = com.example.utils.ImageStorageHelper.copyImageToInternalStorage(context, uri)
                        imageUri = internalUri ?: uri.toString()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showImageMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.DropdownMenu(
                        expanded = showImageMenu,
                        onDismissRequest = { showImageMenu = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Tirar Foto") },
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                            onClick = {
                                showImageMenu = false
                                val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                tempCameraUriStr = uri.toString()
                                cameraLauncher.launch(uri)
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Escolher da Galeria") },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                            onClick = {
                                showImageMenu = false
                                galleryLauncher.launch("image/*")
                            }
                        )
                    }
                    if (imageUri.isNotBlank()) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Foto do Produto",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Adicionar Foto",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Nome do Produto
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Nome do Produto *") },
                    leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    isError = nameError,
                    supportingText = { if (nameError) Text("Informe o nome do item") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Marca do Produto
                ExposedDropdownMenuBox(
                    expanded = brandDropdownExpanded && brandsList.isNotEmpty(),
                    onExpandedChange = { brandDropdownExpanded = !brandDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { 
                            brand = it
                            brandDropdownExpanded = true
                        },
                        label = { Text("Marca / Fabricante") },
                        placeholder = { Text("Ex: Nestlé, Medley, Itambé...") },
                        leadingIcon = { Icon(Icons.Default.Sell, contentDescription = null) },
                        trailingIcon = {
                            if (brandsList.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandDropdownExpanded)
                            }
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("input_product_brand"),
                        singleLine = true
                    )
                    if (brandsList.isNotEmpty()) {
                        val filteredBrands = brandsList.filter { it.contains(brand, ignoreCase = true) }
                        if (filteredBrands.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = brandDropdownExpanded,
                                onDismissRequest = { brandDropdownExpanded = false }
                            ) {
                                filteredBrands.forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text(b) },
                                        onClick = {
                                            brand = b
                                            brandDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Código de Barras / EAN
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Código de Barras / EAN") },
                    placeholder = { Text("Ex: 7891000100101") },
                    leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showBarcodeScanner = true }) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "Ler Código com a Câmera",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_barcode"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Categoria e Unidade
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Categoria Dropdown (From Configured Categories)
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Categoria") },
                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("input_product_category"),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            if (categoriesList.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Geral") },
                                    onClick = {
                                        category = "Geral"
                                        categoryDropdownExpanded = false
                                    }
                                )
                            } else {
                                categoriesList.forEach { cat ->
                                    val catColor = parseHexColor(cat.colorHex)
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .background(catColor, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(cat.name)
                                            }
                                        },
                                        onClick = {
                                            category = cat.name
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Unidade de Medida
                    ExposedDropdownMenuBox(
                        expanded = unitDropdownExpanded,
                        onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded },
                        modifier = Modifier.weight(0.7f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unid.") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("input_product_unit"),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = unitDropdownExpanded,
                            onDismissRequest = { unitDropdownExpanded = false }
                        ) {
                            commonUnits.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Estoque Mínimo (Alerta de Reposição)
                OutlinedTextField(
                    value = minStock,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*([.,]\\d*)?$"))) {
                            minStock = input
                        }
                    },
                    label = { Text("Estoque Mínimo (Alerta de Reposição)") },
                    placeholder = { Text("Ex: 10") },
                    supportingText = { Text("Alerta quando o saldo total estiver igual ou abaixo deste valor") },
                    trailingIcon = {
                        Text(
                            text = unit,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_min_stock"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Localização no Depósito / Almoxarifado
                ExposedDropdownMenuBox(
                    expanded = locationDropdownExpanded && locationsList.isNotEmpty(),
                    onExpandedChange = { locationDropdownExpanded = !locationDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { 
                            location = it
                            locationDropdownExpanded = true
                        },
                        label = { Text("Localização Física (opcional)") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        placeholder = { Text("Ex: Câmara Fria 1, Prateleira B3") },
                        trailingIcon = {
                            if (locationsList.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationDropdownExpanded)
                            }
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("input_product_location"),
                        singleLine = true
                    )
                    if (locationsList.isNotEmpty()) {
                        val filteredLocs = locationsList.filter { it.contains(location, ignoreCase = true) }
                        if (filteredLocs.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = locationDropdownExpanded,
                                onDismissRequest = { locationDropdownExpanded = false }
                            ) {
                                filteredLocs.forEach { loc ->
                                    DropdownMenuItem(
                                        text = { Text(loc) },
                                        onClick = {
                                            location = loc
                                            locationDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Descrição Adicional
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Observações (opcional)") },
                    placeholder = { Text("Detalhes do item...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_description"),
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
                            if (name.isBlank()) {
                                nameError = true
                                return@Button
                            }

                            val parsedMinStock = minStock.replace(",", ".").toDoubleOrNull() ?: 0.0

                            val savedProduct = (productToEdit ?: Product(
                                name = name.trim(),
                                brand = brand.trim(),
                                barcode = barcode.trim(),
                                category = category.trim()
                            )).copy(
                                name = name.trim(),
                                brand = brand.trim(),
                                barcode = barcode.trim(),
                                category = category.trim().ifBlank { "Geral" },
                                unit = unit.trim(),
                                minStock = parsedMinStock.coerceAtLeast(0.0),
                                location = location.trim(),
                                description = description.trim(),
                                imageUri = imageUri,
                                lastUpdated = System.currentTimeMillis()
                            )

                            onSave(savedProduct, isNew)
                        },
                        modifier = Modifier.testTag("btn_save_product")
                    ) {
                        Text(if (isNew) "Cadastrar Produto" else "Salvar")
                    }
                }
            }
        }
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { scanned ->
                barcode = scanned
                showBarcodeScanner = false
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}
