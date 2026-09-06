package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.ProductWithLots
import com.example.data.model.StockHealthStatus
import com.example.data.model.StockLot
import com.example.ui.ExpirationFilter
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.ExpirationBadge
import com.example.ui.components.StatusChip
import com.example.ui.components.formatDateOnly
import com.example.ui.components.formatQuantity
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoseRed

import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

@Composable
fun ProductsScreen(
    productsWithLots: List<ProductWithLots>,
    categories: List<Category>,
    searchQuery: String,
    selectedCategory: String?,
    selectedBrand: String? = null,
    selectedLocation: String? = null,
    selectedHealth: StockHealthStatus?,
    selectedExpFilter: ExpirationFilter,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onBrandSelect: (String?) -> Unit = {},
    onLocationSelect: (String?) -> Unit = {},
    onHealthSelect: (StockHealthStatus?) -> Unit,
    onExpFilterSelect: (ExpirationFilter) -> Unit,
    onClearFilters: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onRegisterStockIn: (Product) -> Unit,
    onRegisterStockOut: (Product, StockLot) -> Unit,
    onAuditLot: (Product, StockLot) -> Unit,
    onDiscardLot: (StockLot) -> Unit,
    onNavigateToCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showQuickPdfDialog by remember { mutableStateOf(false) }
    var brandDropdownExpanded by remember { mutableStateOf(false) }
    var locationDropdownExpanded by remember { mutableStateOf(false) }

    val availableBrands = remember(productsWithLots) {
        productsWithLots.map { it.product.brand }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableLocations = remember(productsWithLots) {
        productsWithLots.flatMap { p ->
            val active = p.lots.filter { it.quantity > 0.001 }
            if (active.isNotEmpty()) {
                active.map { it.location.ifBlank { p.product.location } }
            } else if (p.product.location.isNotBlank()) {
                listOf(p.product.location)
            } else {
                emptyList()
            }
        }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val hasActiveFilters = searchQuery.isNotBlank() || selectedCategory != null || selectedBrand != null || selectedLocation != null || selectedHealth != null || selectedExpFilter != ExpirationFilter.ALL

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search & Category Filters Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Search text field with Barcode scanner button and Quick PDF button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text("Buscar produto, marca, lote...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpar")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_product_input"),
                            singleLine = true
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(52.dp)
                        ) {
                            IconButton(
                                onClick = { showBarcodeScanner = true },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("btn_search_scanner")
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Escanear Código de Barras",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Surface(
                            color = com.example.ui.theme.RoyalBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(52.dp)
                        ) {
                            IconButton(
                                onClick = { showQuickPdfDialog = true },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("btn_top_pdf_products")
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = "Exportar PDF do Estoque",
                                    tint = com.example.ui.theme.RoyalBlue
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Categories Horizontal Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { onCategorySelect(null) },
                                label = { Text("Todas") },
                                modifier = Modifier.testTag("filter_cat_all")
                            )
                        }

                        items(categories, key = { it.id }) { cat ->
                            val catColor = parseHexColor(cat.colorHex)
                            FilterChip(
                                selected = selectedCategory.equals(cat.name, ignoreCase = true),
                                onClick = { onCategorySelect(cat.name) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(catColor, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${cat.name} (${cat.productCount})", fontSize = 12.sp)
                                    }
                                }
                            )
                        }

                        item {
                            IconButton(
                                onClick = onNavigateToCategories,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = "Configurar Categorias",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Brand & Location Dropdown / Quick Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand filter chip/menu
                        Box(modifier = Modifier.weight(1f)) {
                            FilterChip(
                                selected = selectedBrand != null,
                                onClick = { brandDropdownExpanded = true },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(selectedBrand ?: "Marca: Todas", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = brandDropdownExpanded,
                                onDismissRequest = { brandDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todas as Marcas") },
                                    onClick = { onBrandSelect(null); brandDropdownExpanded = false }
                                )
                                availableBrands.forEach { brand ->
                                    DropdownMenuItem(
                                        text = { Text(brand) },
                                        onClick = { onBrandSelect(brand); brandDropdownExpanded = false }
                                    )
                                }
                            }
                        }

                        // Location filter chip/menu
                        Box(modifier = Modifier.weight(1f)) {
                            FilterChip(
                                selected = selectedLocation != null,
                                onClick = { locationDropdownExpanded = true },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(selectedLocation ?: "Local: Todos", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = locationDropdownExpanded,
                                onDismissRequest = { locationDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todos os Locais") },
                                    onClick = { onLocationSelect(null); locationDropdownExpanded = false }
                                )
                                availableLocations.forEach { loc ->
                                    DropdownMenuItem(
                                        text = { Text(loc) },
                                        onClick = { onLocationSelect(loc); locationDropdownExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quick Expiration / Health Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedExpFilter == ExpirationFilter.ALL,
                                onClick = { onExpFilterSelect(ExpirationFilter.ALL) },
                                label = { Text("Todos", fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedExpFilter == ExpirationFilter.LOW_STOCK,
                                onClick = { onExpFilterSelect(ExpirationFilter.LOW_STOCK) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.HourglassBottom,
                                            contentDescription = null,
                                            tint = AmberWarning,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Abaixo do Mínimo", fontSize = 11.sp)
                                    }
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedExpFilter == ExpirationFilter.EXPIRED,
                                onClick = { onExpFilterSelect(ExpirationFilter.EXPIRED) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.DeleteSweep,
                                            contentDescription = null,
                                            tint = RoseRed,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Vencidos", fontSize = 11.sp)
                                    }
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedExpFilter == ExpirationFilter.EXPIRING_SOON,
                                onClick = { onExpFilterSelect(ExpirationFilter.EXPIRING_SOON) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.HourglassBottom,
                                            contentDescription = null,
                                            tint = AmberWarning,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Próximos a Vencer", fontSize = 11.sp)
                                    }
                                }
                            )
                        }
                    }

                    // Active Filters bar with clear button
                    if (hasActiveFilters) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filtro ativo (${productsWithLots.size} exibidos)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = onClearFilters,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Limpar Filtros", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Products List
            if (productsWithLots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum produto cadastrado ou encontrado",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Cadastre novos produtos para controlar estoque e validades.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddProduct,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("empty_add_product_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cadastrar Produto")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${productsWithLots.size} produto(s) cadastrado(s)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            androidx.compose.material3.FilledTonalButton(
                                onClick = { showQuickPdfDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp).testTag("btn_quick_pdf_products_screen")
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.RoyalBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Exportar PDF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.RoyalBlue
                                )
                            }
                        }
                    }

                    items(productsWithLots, key = { it.product.id }) { pWithLots ->
                        SimpleProductCard(
                            productWithLots = pWithLots,
                            categories = categories,
                            onEdit = { onEditProduct(pWithLots.product) },
                            onDelete = { productToDelete = pWithLots.product },
                            onStockIn = { onRegisterStockIn(pWithLots.product) },
                            onStockOutLot = { lot -> onRegisterStockOut(pWithLots.product, lot) },
                            onAuditLot = { lot -> onAuditLot(pWithLots.product, lot) },
                            onDiscardLot = onDiscardLot
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // FAB to add product
        FloatingActionButton(
            onClick = onAddProduct,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_product")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Novo Produto")
        }
    }

    // Confirmation dialog for product deletion
    productToDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Excluir Produto") },
            text = { Text("Deseja realmente excluir '${prod.name}' e seus registros vinculados?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(prod)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showBarcodeScanner) {
        val productsList = productsWithLots.map { it.product }
        BarcodeScannerDialog(
            availableProducts = productsList,
            onBarcodeScanned = { barcode ->
                onSearchChange(barcode)
                showBarcodeScanner = false
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    if (showQuickPdfDialog) {
        com.example.ui.dialogs.QuickPdfExportDialog(
            productsWithLots = productsWithLots,
            categories = categories,
            initialCategory = selectedCategory,
            initialBrand = selectedBrand,
            initialLocation = selectedLocation,
            onDismiss = { showQuickPdfDialog = false }
        )
    }
}

@Composable
fun SimpleProductCard(
    productWithLots: ProductWithLots,
    categories: List<Category>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStockIn: () -> Unit,
    onStockOutLot: (StockLot) -> Unit,
    onAuditLot: (StockLot) -> Unit,
    onDiscardLot: (StockLot) -> Unit,
    modifier: Modifier = Modifier
) {
    val product = productWithLots.product
    val activeLots = productWithLots.lots.filter { it.quantity > 0.001 }
    val activeLocations = remember(productWithLots) {
        if (activeLots.isNotEmpty()) {
            activeLots.map { it.location.ifBlank { product.location } }.filter { it.isNotBlank() }.distinct()
        } else if (product.location.isNotBlank()) {
            listOf(product.location)
        } else {
            emptyList()
        }
    }
    val locationDisplay = activeLocations.joinToString(", ")
    var isExpanded by remember { mutableStateOf(false) }

    val matchedCategory = categories.find { it.name.equals(product.category, ignoreCase = true) }
    val categoryColor = if (matchedCategory != null) parseHexColor(matchedCategory.colorHex) else Color(0xFF64748B)

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expand_rotation"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 2.dp else 0.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("product_card_${product.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header / Compact summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Thumbnail or Category initial
                if (product.imageUri.isNotBlank()) {
                    AsyncImage(
                        model = product.imageUri,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                } else {
                    Surface(
                        color = categoryColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = product.name.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = categoryColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Center Column: Product Name, Category & Location info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Category Pill
                        Surface(
                            color = categoryColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = product.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = categoryColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }

                        if (product.brand.isNotBlank()) {
                            Text(
                                text = "• ${product.brand}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (locationDisplay.isNotBlank()) {
                            Text(
                                text = "• $locationDisplay",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Alerts line when collapsed
                    val expiredCount = productWithLots.expiredLotsCount()
                    if (!isExpanded && (productWithLots.isLowStock || expiredCount > 0)) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (expiredCount > 0) {
                                Surface(color = RoseRed.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = "$expiredCount vencido(s)",
                                        color = RoseRed,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (productWithLots.isLowStock) {
                                Surface(color = AmberWarning.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = if (productWithLots.isOutOfStock) "Sem estoque" else "Estoque baixo",
                                        color = AmberWarning,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Right Column: Stock Balance & Expand Chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${formatQuantity(productWithLots.totalQuantity)} ${product.unit}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when (productWithLots.stockHealthStatus) {
                                StockHealthStatus.OUT_OF_STOCK -> RoseRed
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        Text(
                            text = "${activeLots.size} lote(s)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Recolher detalhes" else "Expandir detalhes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(rotationAngle)
                        )
                    }
                }
            }

            // Expanded Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Secondary Details (Barcode, Location, Min Stock)
                    val details = mutableListOf<String>()
                    if (product.barcode.isNotBlank()) details.add("EAN: ${product.barcode}")
                    if (locationDisplay.isNotBlank()) details.add("Local: $locationDisplay")
                    if (product.minStock > 0) details.add("Mínimo: ${formatQuantity(product.minStock)} ${product.unit}")

                    if (details.isNotEmpty()) {
                        Text(
                            text = details.joinToString("  •  "),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (product.description.isNotBlank()) {
                        Text(
                            text = product.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // Low Stock Alert Banner
                    if (productWithLots.isLowStock) {
                        Surface(
                            color = AmberWarning.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (productWithLots.isOutOfStock)
                                        "Estoque zerado! Abaixo do mínimo (${formatQuantity(product.minStock)} ${product.unit})"
                                    else
                                        "Estoque baixo! Abaixo do mínimo (${formatQuantity(product.minStock)} ${product.unit})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AmberWarning
                                )
                            }
                        }
                    }

                    // Action buttons row: Quick Entrada, Quick Saída, Edit, Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = onStockIn,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Entrada", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }

                            if (activeLots.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        val firstExpiringLot = activeLots.first()
                                        onStockOutLot(firstExpiringLot)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Outbox, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("- Saída", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(status = productWithLots.stockHealthStatus)
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(15.dp))
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Excluir",
                                    tint = RoseRed.copy(alpha = 0.8f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Validades / Lots breakdown
                    if (activeLots.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Validades e Lotes (FEFO):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            activeLots.forEach { lot ->
                                val days = lot.daysUntilExpiration()
                                val isExpired = days < 0

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Venc: ${formatDateOnly(lot.expirationDate)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isExpired) RoseRed else MaterialTheme.colorScheme.onSurface
                                            )
                                            val effectiveLotLoc = lot.location.ifBlank { product.location }
                                            val lotLocStr = if (effectiveLotLoc.isNotBlank()) " | Loc: $effectiveLotLoc" else ""
                                            Text(
                                                text = "Saldo: ${formatQuantity(lot.quantity)} ${product.unit}$lotLocStr",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            ExpirationBadge(
                                                expirationDate = lot.expirationDate,
                                                quantity = lot.quantity
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))

                                            IconButton(
                                                onClick = { onAuditLot(lot) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.FactCheck,
                                                    contentDescription = "Ajustar Saldo",
                                                    tint = PurpleAccent,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }

                                            if (isExpired) {
                                                IconButton(
                                                    onClick = { onDiscardLot(lot) },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteSweep,
                                                        contentDescription = "Descartar",
                                                        tint = RoseRed,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = { onStockOutLot(lot) },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Outbox,
                                                        contentDescription = "Dar Saída",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
