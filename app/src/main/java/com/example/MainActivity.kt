package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.utils.StockNotificationHelper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MovementType
import com.example.data.model.Product
import com.example.data.model.StockLot
import com.example.data.model.StockMovement
import com.example.ui.StockViewModel
import com.example.ui.dialogs.AddEditProductDialog
import com.example.ui.dialogs.BackupExportDialog
import com.example.ui.dialogs.QuickPdfExportDialog
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.WifiOff
import com.example.data.backup.CloudSyncStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.theme.RoyalBlue
import com.example.ui.dialogs.EditMovementDialog
import com.example.ui.dialogs.InventoryAuditDialog
import com.example.ui.dialogs.StockMovementDialog
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.LotsOverviewScreen
import com.example.ui.screens.MovementsHistoryScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

sealed class NavItem(val title: String, val icon: ImageVector, val index: Int) {
    data object Products : NavItem("Estoque", Icons.Default.Inventory2, 0)
    data object LotsOverview : NavItem("Validades", Icons.Default.HourglassTop, 1)
    data object Reports : NavItem("Relatórios", Icons.Default.Assessment, 2)
    data object Movements : NavItem("Histórico", Icons.Default.History, 3)
    data object Categories : NavItem("Categorias", Icons.Default.Category, 4)
}

class MainActivity : ComponentActivity() {

    private val viewModel: StockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create the notification channel on app startup
        StockNotificationHelper.createNotificationChannel(applicationContext)

        intent?.getStringExtra("SEARCH_QUERY")?.let { query ->
            if (query.isNotBlank()) {
                viewModel.setSearchQuery(query)
            }
        }

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: StockViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Notification Permission for Android 13+ (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Permission granted: trigger check for low stock
                viewModel.triggerLowStockCheck()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Dialog state holders
    var showAddEditProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    var showMovementDialog by remember { mutableStateOf(false) }
    var movementProduct by remember { mutableStateOf<Product?>(null) }
    var movementLot by remember { mutableStateOf<StockLot?>(null) }
    var movementInitialType by remember { mutableStateOf(MovementType.ENTRADA) }

    var auditProduct by remember { mutableStateOf<Product?>(null) }
    var auditLot by remember { mutableStateOf<StockLot?>(null) }

    var movementToEdit by remember { mutableStateOf<StockMovement?>(null) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showQuickPdfDialog by remember { mutableStateOf(false) }

    // Observables
    val productsWithLots by viewModel.productsWithLots.collectAsStateWithLifecycle()
    val filteredProductsWithLots by viewModel.filteredProductsWithLots.collectAsStateWithLifecycle()
    val movements by viewModel.allMovements.collectAsStateWithLifecycle()
    val categories by viewModel.configuredCategories.collectAsStateWithLifecycle()
    val brands by viewModel.brands.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val activeLots by viewModel.allActiveLots.collectAsStateWithLifecycle()
    val allLots by viewModel.allLots.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedBrandFilter by viewModel.selectedBrandFilter.collectAsStateWithLifecycle()
    val selectedLocationFilter by viewModel.selectedLocationFilter.collectAsStateWithLifecycle()
    val selectedHealthFilter by viewModel.selectedHealthFilter.collectAsStateWithLifecycle()
    val selectedExpFilter by viewModel.selectedExpirationFilter.collectAsStateWithLifecycle()
    val expirationAlertDays by viewModel.expirationAlertDays.collectAsStateWithLifecycle()
    val firebaseUrl by viewModel.firebaseUrl.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()
    val hasPendingChanges by viewModel.hasPendingChanges.collectAsStateWithLifecycle()
    val lastSyncError by viewModel.lastSyncError.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()

    // Feedback SnackBar
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTabIndex) {
                            0 -> "Controle de Estoque"
                            1 -> "Controle de Validades (FEFO)"
                            2 -> "Relatórios & Indicadores"
                            3 -> "Histórico de Movimentações"
                            4 -> "Gerenciar Categorias"
                            else -> "Controle de Estoque"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showQuickPdfDialog = true },
                        modifier = Modifier.testTag("top_quick_pdf_button")
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "Exportar Relatório PDF",
                            tint = RoyalBlue
                        )
                    }
                    IconButton(
                        onClick = {
                            val count = viewModel.triggerLowStockCheck()
                            val msg = if (count > 0) {
                                "$count produto(s) com alerta de estoque mínimo notificado(s)!"
                            } else {
                                "Nenhum produto atingiu o estoque mínimo no momento."
                            }
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("top_notification_check_button")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Verificar Alertas de Estoque Mínimo")
                    }
                    IconButton(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier.testTag("top_backup_button")
                    ) {
                        val iconColor = when {
                            !isOnline -> AmberWarning
                            cloudSyncStatus == CloudSyncStatus.SYNCING -> RoyalBlue
                            lastSyncError != null -> RoseRed
                            hasPendingChanges -> AmberWarning
                            lastSyncTime > 0L -> EmeraldGreen
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        val syncIcon = when {
                            !isOnline -> Icons.Default.WifiOff
                            cloudSyncStatus == CloudSyncStatus.SYNCING -> Icons.Default.CloudSync
                            lastSyncError != null -> Icons.Default.SyncProblem
                            hasPendingChanges -> Icons.Default.CloudSync
                            lastSyncTime > 0L -> Icons.Default.CloudDone
                            else -> Icons.Default.CloudSync
                        }
                        Icon(
                            imageVector = syncIcon,
                            contentDescription = "Sincronização e Backup",
                            tint = iconColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                val navItems = listOf(
                    NavItem.Products,
                    NavItem.LotsOverview,
                    NavItem.Reports,
                    NavItem.Movements,
                    NavItem.Categories
                )

                navItems.forEach { item ->
                    val badgeCount = when (item) {
                        NavItem.LotsOverview -> summary.expiredLotsCount + summary.criticalLotsCount + summary.warningLotsCount
                        NavItem.Products -> summary.lowStockProductsCount + summary.outOfStockProductsCount
                        else -> 0
                    }

                    NavigationBarItem(
                        selected = selectedTabIndex == item.index,
                        onClick = { selectedTabIndex = item.index },
                        icon = {
                            if (badgeCount > 0) {
                                androidx.compose.material3.BadgedBox(
                                    badge = {
                                        androidx.compose.material3.Badge(
                                            containerColor = if (item == NavItem.LotsOverview && summary.expiredLotsCount > 0) com.example.ui.theme.RoseRed else com.example.ui.theme.AmberWarning,
                                            contentColor = androidx.compose.ui.graphics.Color.White
                                        ) {
                                            Text(if (badgeCount > 99) "99+" else badgeCount.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(item.icon, contentDescription = item.title)
                                }
                            } else {
                                Icon(item.icon, contentDescription = item.title)
                            }
                        },
                        label = {
                            Text(
                                item.title,
                                fontSize = 10.sp,
                                fontWeight = if (selectedTabIndex == item.index) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "Screen Transition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> ProductsScreen(
                    productsWithLots = filteredProductsWithLots,
                    categories = categories,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategoryFilter,
                    selectedBrand = selectedBrandFilter,
                    selectedLocation = selectedLocationFilter,
                    selectedHealth = selectedHealthFilter,
                    selectedExpFilter = selectedExpFilter,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onCategorySelect = { viewModel.setCategoryFilter(it) },
                    onBrandSelect = { viewModel.setBrandFilter(it) },
                    onLocationSelect = { viewModel.setLocationFilter(it) },
                    onHealthSelect = { viewModel.setHealthFilter(it) },
                    onExpFilterSelect = { viewModel.setExpirationFilter(it) },
                    onClearFilters = { viewModel.clearFilters() },
                    onAddProduct = {
                        productToEdit = null
                        showAddEditProductDialog = true
                    },
                    onEditProduct = { product ->
                        productToEdit = product
                        showAddEditProductDialog = true
                    },
                    onDeleteProduct = { product ->
                        viewModel.deleteProduct(product)
                    },
                    onRegisterStockIn = { prod ->
                        movementProduct = prod
                        movementLot = null
                        movementInitialType = MovementType.ENTRADA
                        showMovementDialog = true
                    },
                    onRegisterStockOut = { prod, lot ->
                        movementProduct = prod
                        movementLot = lot
                        movementInitialType = MovementType.SAIDA
                        showMovementDialog = true
                    },
                    onAuditLot = { prod, lot ->
                        auditProduct = prod
                        auditLot = lot
                    },
                    onDiscardLot = { lot ->
                        viewModel.discardExpiredLot(lot)
                    },
                    onNavigateToCategories = {
                        selectedTabIndex = 4
                    }
                )

                1 -> LotsOverviewScreen(
                    summary = summary,
                    productsWithLots = productsWithLots,
                    expirationAlertDays = expirationAlertDays,
                    onSetExpirationAlertDays = { viewModel.setExpirationAlertDays(it) },
                    onAddStockIn = { prod ->
                        movementProduct = prod
                        movementLot = null
                        movementInitialType = MovementType.ENTRADA
                        showMovementDialog = true
                    },
                    onStockOutLot = { prod, lot ->
                        movementProduct = prod
                        movementLot = lot
                        movementInitialType = MovementType.SAIDA
                        showMovementDialog = true
                    },
                    onDiscardLot = { lot ->
                        viewModel.discardExpiredLot(lot)
                    },
                    onOpenBackup = { showBackupDialog = true }
                )


                2 -> ReportsScreen(
                    summary = summary,
                    productsWithLots = productsWithLots,
                    categories = categories,
                    movements = movements,
                    onOpenFilteredExport = { showBackupDialog = true }
                )


                3 -> MovementsHistoryScreen(
                    movements = movements,
                    onEditMovement = { mov ->
                        movementToEdit = mov
                    }
                )

                4 -> {
                    CategoriesScreen(
                        categories = categories,
                        brands = brands,
                        locations = locations,
                        onSaveCategory = { cat, oldName ->
                            viewModel.saveCategory(cat, oldName)
                        },
                        onDeleteCategory = { cat ->
                            viewModel.deleteCategory(cat)
                        },
                        onAddBrand = { viewModel.addBrand(it) },
                        onEditBrand = { old, new -> viewModel.updateBrand(old, new) },
                        onDeleteBrand = { viewModel.removeBrand(it) },
                        onAddLocation = { viewModel.addLocation(it) },
                        onEditLocation = { old, new -> viewModel.updateLocation(old, new) },
                        onDeleteLocation = { viewModel.removeLocation(it) },
                        onFilterByStockCategory = { categoryName ->
                            viewModel.setCategoryFilter(categoryName)
                            selectedTabIndex = 0
                        }
                    )
                }
                }
            }
        }
    }

    // Modal: Add / Edit Product
    if (showAddEditProductDialog || productToEdit != null) {
        AddEditProductDialog(
            productToEdit = productToEdit,
            categoriesList = categories,
            brandsList = brands,
            locationsList = locations,
            onDismiss = {
                showAddEditProductDialog = false
                productToEdit = null
            },
            onSave = { product, isNew ->
                viewModel.saveProduct(product, isNew)
                showAddEditProductDialog = false
                productToEdit = null
            }
        )
    }

    // Modal: Stock Movement (Entrada e Saída)
    if (showMovementDialog && productsWithLots.isNotEmpty()) {
        StockMovementDialog(
            initialProduct = movementProduct,
            initialLot = movementLot,
            allProductsWithLots = productsWithLots,
            locationsList = locations,
            initialType = movementInitialType,
            onDismiss = {
                showMovementDialog = false
                movementProduct = null
                movementLot = null
            },
            onConfirmIn = { productId, expDate, qty, mfgDate, location, reason, doc, notes ->
                viewModel.registerStockIn(
                    productId = productId,
                    expirationDate = expDate,
                    quantity = qty,
                    manufacturingDate = mfgDate,
                    location = location,
                    reason = reason,
                    documentNumber = doc,
                    notes = notes
                )
                showMovementDialog = false
                movementProduct = null
                movementLot = null
            },
            onConfirmOut = { lotId, qty, isDiscard, reason, doc, notes ->
                viewModel.registerStockOut(
                    lotId = lotId,
                    quantity = qty,
                    isDiscard = isDiscard,
                    reason = reason,
                    documentNumber = doc,
                    notes = notes
                )
                showMovementDialog = false
                movementProduct = null
                movementLot = null
            },
            onConfirmTransfer = { lotId, qty, destLocation, doc, notes ->
                viewModel.registerLotTransfer(
                    sourceLotId = lotId,
                    quantity = qty,
                    destinationLocation = destLocation,
                    reason = "Transferência Interna",
                    notes = notes + if (doc.isNotBlank()) "\nDoc: $doc" else ""
                )
                showMovementDialog = false
                movementProduct = null
                movementLot = null
            }
        )
    }

    // Modal: Inventory Audit
    auditProduct?.let { prod ->
        val prodLots = productsWithLots.find { it.product.id == prod.id }?.lots ?: emptyList()
        InventoryAuditDialog(
            product = prod,
            lots = prodLots,
            initialLot = auditLot,
            onDismiss = {
                auditProduct = null
                auditLot = null
            },
            onConfirmAudit = { lotId, physicalCount, notes ->
                viewModel.registerLotAdjustment(lotId, physicalCount, notes = notes)
                auditProduct = null
                auditLot = null
            }
        )
    }

    // Modal: Edit or Reverse wrong movement
    movementToEdit?.let { mov ->
        EditMovementDialog(
            movement = mov,
            onSaveCorrection = { movId, newQty, newExpDate, newReason, newDoc, newNotes ->
                viewModel.correctStockMovement(movId, newQty, newExpDate, newReason, newDoc, newNotes)
                movementToEdit = null
            },
            onDeleteOrReverse = { movId ->
                viewModel.deleteOrReverseMovement(movId)
                movementToEdit = null
            },
            onDismiss = { movementToEdit = null }
        )
    }

    // Modal: Backup / Restore / Export to Google Drive & Local storage
    if (showBackupDialog) {
        BackupExportDialog(
            products = allProducts,
            categories = categories,
            lots = allLots,
            movements = movements,
            productsWithLots = productsWithLots,
            firebaseUrl = firebaseUrl,
            lastSyncTime = lastSyncTime,
            isOnline = isOnline,
            autoSyncEnabled = autoSyncEnabled,
            hasPendingChanges = hasPendingChanges,
            lastSyncError = lastSyncError,
            cloudSyncStatus = cloudSyncStatus,
            onToggleAutoSync = { enabled -> viewModel.setAutoSyncEnabled(enabled) },
            onSaveFirebaseUrl = { url -> viewModel.saveFirebaseUrl(url) },
            onCloudUpload = { onComplete -> viewModel.syncUploadToCloud(onComplete = onComplete) },
            onCloudDownload = { onComplete -> viewModel.syncDownloadFromCloud(onComplete = onComplete) },
            onTestFirebase = { url, onResult -> viewModel.testFirebaseConnection(url, onResult) },
            onRestoreBackup = { json ->
                viewModel.restoreBackupJson(json)
            },
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showQuickPdfDialog) {
        QuickPdfExportDialog(
            productsWithLots = productsWithLots,
            categories = categories,
            currentAlertDays = expirationAlertDays,
            initialCategory = selectedCategoryFilter,
            onDismiss = { showQuickPdfDialog = false }
        )
    }
}
