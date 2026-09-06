package com.example.ui.dialogs

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.backup.BackupManager
import com.example.data.backup.ExportStockFilter
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.ProductWithLots
import com.example.data.model.StockLot
import com.example.data.model.StockMovement
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.theme.RoyalBlue
import androidx.compose.material.icons.filled.SignalWifiConnectedNoInternet4
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.PictureAsPdf
import com.example.data.backup.CloudSyncStatus
import com.example.utils.PdfReportService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupExportDialog(
    products: List<Product>,
    categories: List<Category>,
    lots: List<StockLot>,
    movements: List<StockMovement>,
    productsWithLots: List<ProductWithLots>,
    currentAlertDays: Int = 30,
    firebaseUrl: String = "",
    lastSyncTime: Long = 0L,
    isOnline: Boolean = true,
    autoSyncEnabled: Boolean = true,
    hasPendingChanges: Boolean = false,
    lastSyncError: String? = null,
    cloudSyncStatus: CloudSyncStatus = CloudSyncStatus.IDLE,
    onToggleAutoSync: (Boolean) -> Unit = {},
    onSaveFirebaseUrl: (String) -> Unit = {},
    onCloudUpload: (onComplete: (Boolean, String) -> Unit) -> Unit = {},
    onCloudDownload: (onComplete: (Boolean, String) -> Unit) -> Unit = {},
    onTestFirebase: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onRestoreBackup: (jsonContent: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var restoreJsonText by rememberSaveable { mutableStateOf("") }
    var showRestoreConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showCloudDownloadConfirmDialog by rememberSaveable { mutableStateOf(false) }

    // Firebase Cloud States
    var currentFirebaseUrl by rememberSaveable { mutableStateOf(firebaseUrl) }
    var isTestingConnection by rememberSaveable { mutableStateOf(false) }
    var isCloudUploading by rememberSaveable { mutableStateOf(false) }
    var isCloudDownloading by rememberSaveable { mutableStateOf(false) }
    var cloudStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isCloudStatusSuccess by rememberSaveable { mutableStateOf(true) }
    var showStepByStepGuide by rememberSaveable { mutableStateOf(false) }

    // Export Filter States
    var brandDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var productDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var movTypeDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    var selectedCategoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedBrandFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLocationFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedProductId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedMovementType by rememberSaveable { mutableStateOf<com.example.data.model.MovementType?>(null) }
    var isDiscardOnly by rememberSaveable { mutableStateOf(false) }
    var selectedStockFilter by rememberSaveable { mutableStateOf(ExportStockFilter.ALL) }
    var selectedAlertDays by rememberSaveable { mutableIntStateOf(currentAlertDays) }
    var selectedMovementPeriodDays by rememberSaveable { mutableStateOf<Int?>(null) }

    val uniqueBrands = remember(products) {
        products.map { it.brand }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val uniqueLocations = remember(products, productsWithLots) {
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

    val sortedProducts = remember(products) {
        products.sortedBy { it.name }
    }

    val filteredProductsWithLots = remember(
        productsWithLots,
        selectedCategoryFilter,
        selectedBrandFilter,
        selectedLocationFilter,
        selectedProductId,
        selectedStockFilter,
        selectedAlertDays
    ) {
        BackupManager.filterDataForExport(
            productsWithLots = productsWithLots,
            selectedCategory = selectedCategoryFilter,
            selectedBrand = selectedBrandFilter,
            selectedLocation = selectedLocationFilter,
            selectedProductId = selectedProductId,
            stockFilter = selectedStockFilter,
            alertDays = selectedAlertDays
        )
    }

    val filteredProducts = remember(filteredProductsWithLots) {
        filteredProductsWithLots.map { it.product }
    }

    val filteredLots = remember(filteredProductsWithLots) {
        filteredProductsWithLots.flatMap { it.lots }
    }

    val filteredMovements = remember(movements, filteredProducts, selectedMovementPeriodDays, selectedMovementType, isDiscardOnly, selectedBrandFilter, selectedProductId, selectedLocationFilter) {
        val prodIds = filteredProducts.map { it.id }.toSet()
        val byProduct = movements.filter { prodIds.contains(it.productId) }
        BackupManager.filterMovements(
            movements = byProduct,
            periodDays = selectedMovementPeriodDays,
            movementType = selectedMovementType,
            selectedBrand = selectedBrandFilter,
            selectedProductId = selectedProductId,
            selectedLocation = selectedLocationFilter,
            discardOnly = isDiscardOnly
        )
    }

    val filterSummaryDescription = remember(
        selectedCategoryFilter,
        selectedBrandFilter,
        selectedLocationFilter,
        selectedProductId,
        selectedStockFilter,
        selectedAlertDays,
        selectedMovementPeriodDays,
        selectedMovementType,
        isDiscardOnly
    ) {
        val parts = mutableListOf<String>()
        parts.add(selectedCategoryFilter?.let { "Cat: $it" } ?: "Todas as Categorias")
        parts.add(selectedBrandFilter?.let { "Marca: $it" } ?: "Todas as Marcas")
        selectedLocationFilter?.let { parts.add("Local: $it") }
        parts.add(selectedProductId?.let { id -> "Produto: " + products.find { it.id == id }?.name } ?: "Todos os Produtos")
        parts.add(selectedStockFilter.label)
        if (selectedStockFilter == ExportStockFilter.EXPIRING_SOON) {
            parts.add("(< $selectedAlertDays dias)")
        }
        parts.add(selectedMovementType?.let { it.label } ?: "Todas Mov.")
        if (isDiscardOnly) parts.add("Apenas Descartes")
        selectedMovementPeriodDays?.let { parts.add("Últimos $it dias") }
        parts.joinToString(" • ")
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Backup & Nuvem Online",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sincronização Firebase, Drive ou Arquivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = RoyalBlue,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedTab == 0) RoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Nuvem Online",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 0) RoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedTab == 1) RoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Relatórios & Arquivos",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 1) RoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedTab == 2) RoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Restaurar",
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 2) RoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // TAB 0: ONLINE CLOUD (FIREBASE)
                if (selectedTab == 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RoyalBlue.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Banco de Dados Firebase",
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue,
                                        fontSize = 14.sp
                                    )
                                }

                                TextButton(
                                    onClick = { showStepByStepGuide = !showStepByStepGuide }
                                ) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (showStepByStepGuide) "Ocultar Guia" else "Ver Passo a Passo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Live Connectivity & Sync State Banner
                            Surface(
                                color = when {
                                    !isOnline -> AmberWarning.copy(alpha = 0.15f)
                                    cloudSyncStatus == CloudSyncStatus.SYNCING -> RoyalBlue.copy(alpha = 0.15f)
                                    lastSyncError != null -> RoseRed.copy(alpha = 0.12f)
                                    hasPendingChanges -> AmberWarning.copy(alpha = 0.12f)
                                    else -> EmeraldGreen.copy(alpha = 0.12f)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when {
                                            !isOnline -> Icons.Default.WifiOff
                                            cloudSyncStatus == CloudSyncStatus.SYNCING -> Icons.Default.Sync
                                            lastSyncError != null -> Icons.Default.ErrorOutline
                                            hasPendingChanges -> Icons.Default.CloudUpload
                                            else -> Icons.Default.CheckCircle
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            !isOnline -> AmberWarning
                                            cloudSyncStatus == CloudSyncStatus.SYNCING -> RoyalBlue
                                            lastSyncError != null -> RoseRed
                                            hasPendingChanges -> AmberWarning
                                            else -> EmeraldGreen
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = when {
                                                !isOnline -> "Modo Offline (Sem Conexão)"
                                                cloudSyncStatus == CloudSyncStatus.SYNCING -> "Sincronizando com a nuvem..."
                                                lastSyncError != null -> "Erro na última sincronização"
                                                hasPendingChanges -> "Alterações pendentes salvas no aparelho"
                                                else -> "Online e Sincronizado"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = when {
                                                !isOnline -> AmberWarning
                                                cloudSyncStatus == CloudSyncStatus.SYNCING -> RoyalBlue
                                                lastSyncError != null -> RoseRed
                                                hasPendingChanges -> AmberWarning
                                                else -> EmeraldGreen
                                            }
                                        )
                                        Text(
                                            text = when {
                                                !isOnline -> if (hasPendingChanges) "Alterações salvas localmente. O envio ocorrerá automaticamente ao reconectar." else "O app funciona normalmente sem internet."
                                                cloudSyncStatus == CloudSyncStatus.SYNCING -> "Enviando dados para o Firebase..."
                                                lastSyncError != null -> lastSyncError
                                                hasPendingChanges -> "Suas alterações serão enviadas para o Firebase."
                                                lastSyncTime > 0L -> "Tudo atualizado no Firebase."
                                                else -> "Pronto para enviar ou baixar."
                                            },
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (showStepByStepGuide) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("📱 Como criar seu banco grátis no celular:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBlue)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("1. Acesse console.firebase.google.com no Chrome do celular.", fontSize = 11.sp)
                                        Text("2. Toque em 'Criar um projeto' e avance até concluir.", fontSize = 11.sp)
                                        Text("3. No menu ☰, vá em Criação > Realtime Database > Criar banco.", fontSize = 11.sp)
                                        Text("4. Escolha 'Iniciar no modo de teste' (.read: true, .write: true) e toque em Ativar.", fontSize = 11.sp)
                                        Text("5. Copie o link (https://...firebaseio.com) e cole no campo abaixo!", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.firebase.google.com/"))
                                                    context.startActivity(browserIntent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Abra o Chrome e acesse console.firebase.google.com", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Abrir Firebase no Navegador", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = currentFirebaseUrl,
                                onValueChange = {
                                    currentFirebaseUrl = it
                                    onSaveFirebaseUrl(it)
                                },
                                label = { Text("URL do Firebase Realtime Database") },
                                placeholder = { Text("https://seu-projeto-default-rtdb.firebaseio.com/") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_firebase_url"),
                                trailingIcon = {
                                    if (currentFirebaseUrl.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                isTestingConnection = true
                                                cloudStatusMessage = "Testando conexão..."
                                                onTestFirebase(currentFirebaseUrl) { success, msg ->
                                                    isTestingConnection = false
                                                    isCloudStatusSuccess = success
                                                    cloudStatusMessage = msg
                                                }
                                            }
                                        ) {
                                            if (isTestingConnection) {
                                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Default.Sync, contentDescription = "Testar Conexão", tint = RoyalBlue)
                                            }
                                        }
                                    }
                                }
                            )

                            // Auto Sync Toggle Switch
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "Auto-sincronizar ao Reconectar",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Envia dados automaticamente para o Firebase após recuperar internet.",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = autoSyncEnabled,
                                        onCheckedChange = { onToggleAutoSync(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = RoyalBlue,
                                            checkedTrackColor = RoyalBlue.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }

                            if (lastSyncTime > 0L) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Último envio com sucesso: ${dateFormat.format(Date(lastSyncTime))}",
                                    fontSize = 11.sp,
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (cloudStatusMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = if (isCloudStatusSuccess) EmeraldGreen.copy(alpha = 0.12f) else RoseRed.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (isCloudStatusSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = if (isCloudStatusSuccess) EmeraldGreen else RoseRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cloudStatusMessage ?: "",
                                            fontSize = 11.sp,
                                            color = if (isCloudStatusSuccess) EmeraldGreen else RoseRed,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (currentFirebaseUrl.isBlank()) {
                                            Toast.makeText(context, "Cole a URL do Firebase acima antes de enviar.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isCloudUploading = true
                                        cloudStatusMessage = "Enviando dados para o Firebase..."
                                        onCloudUpload { success, msg ->
                                            isCloudUploading = false
                                            isCloudStatusSuccess = success
                                            cloudStatusMessage = msg
                                        }
                                    },
                                    enabled = !isCloudUploading && !isCloudDownloading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoyalBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("btn_cloud_upload")
                                ) {
                                    if (isCloudUploading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Enviando...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Enviar p/ Nuvem", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                FilledTonalButton(
                                    onClick = {
                                        if (currentFirebaseUrl.isBlank()) {
                                            Toast.makeText(context, "Cole a URL do Firebase acima antes de baixar.", Toast.LENGTH_SHORT).show()
                                            return@FilledTonalButton
                                        }
                                        showCloudDownloadConfirmDialog = true
                                    },
                                    enabled = !isCloudUploading && !isCloudDownloading,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("btn_cloud_download")
                                ) {
                                    if (isCloudDownloading) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Baixando...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Baixar da Nuvem", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 1: GOOGLE DRIVE / CSV / EXPORT
                if (selectedTab == 1) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.FilterAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Filtros de Exportação",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (selectedCategoryFilter != null || selectedStockFilter != ExportStockFilter.ALL || selectedMovementPeriodDays != null) {
                                    Text(
                                        text = "Limpar Filtros",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.clickable {
                                            selectedCategoryFilter = null
                                            selectedStockFilter = ExportStockFilter.ALL
                                            selectedMovementPeriodDays = null
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 1. Stock Status Filter
                            Text("Situação do Estoque:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ExportStockFilter.values().forEach { filter ->
                                    FilterChip(
                                        selected = selectedStockFilter == filter,
                                        onClick = { selectedStockFilter = filter },
                                        label = { Text(filter.label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = when (filter) {
                                                ExportStockFilter.LOW_STOCK -> AmberWarning.copy(alpha = 0.2f)
                                                ExportStockFilter.EXPIRED -> RoseRed.copy(alpha = 0.2f)
                                                ExportStockFilter.EXPIRING_SOON -> AmberWarning.copy(alpha = 0.2f)
                                                else -> MaterialTheme.colorScheme.primaryContainer
                                            }
                                        )
                                    )
                                }
                            }

                            // 2. Alert window selection if EXPIRING_SOON is selected
                            if (selectedStockFilter == ExportStockFilter.EXPIRING_SOON) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Antecedência do Alerta:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(7, 15, 30, 45, 60, 90).forEach { days ->
                                        FilterChip(
                                            selected = selectedAlertDays == days,
                                            onClick = { selectedAlertDays = days },
                                            label = { Text("$days dias", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. Category Filter
                            Text("Filtrar por Categoria:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = selectedCategoryFilter == null,
                                    onClick = { selectedCategoryFilter = null },
                                    label = { Text("Todas", fontSize = 11.sp) }
                                )
                                categories.forEach { cat ->
                                    FilterChip(
                                        selected = selectedCategoryFilter.equals(cat.name, ignoreCase = true),
                                        onClick = {
                                            selectedCategoryFilter = if (selectedCategoryFilter.equals(cat.name, ignoreCase = true)) null else cat.name
                                        },
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(parseHexColor(cat.colorHex), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(cat.name, fontSize = 11.sp)
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 4. Brand and Product Filter
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Marca:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(8.dp),
                                            onClick = { brandDropdownExpanded = true }
                                        ) {
                                            Text(
                                                text = selectedBrandFilter ?: "Todas",
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(8.dp),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                        DropdownMenu(expanded = brandDropdownExpanded, onDismissRequest = { brandDropdownExpanded = false }) {
                                            DropdownMenuItem(
                                                text = { Text("Todas as Marcas") },
                                                onClick = { selectedBrandFilter = null; brandDropdownExpanded = false }
                                            )
                                            uniqueBrands.forEach { brand ->
                                                DropdownMenuItem(
                                                    text = { Text(brand) },
                                                    onClick = { selectedBrandFilter = brand; brandDropdownExpanded = false }
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Produto:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(8.dp),
                                            onClick = { productDropdownExpanded = true }
                                        ) {
                                            Text(
                                                text = selectedProductId?.let { id -> sortedProducts.find { it.id == id }?.name } ?: "Todos",
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(8.dp),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                        DropdownMenu(expanded = productDropdownExpanded, onDismissRequest = { productDropdownExpanded = false }) {
                                            DropdownMenuItem(
                                                text = { Text("Todos os Produtos") },
                                                onClick = { selectedProductId = null; productDropdownExpanded = false }
                                            )
                                            sortedProducts.forEach { p ->
                                                DropdownMenuItem(
                                                    text = { Text(p.name) },
                                                    onClick = { selectedProductId = p.id; productDropdownExpanded = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Live summary
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📊 Seleção: ${filteredProducts.size} produto(s), ${filteredLots.count { it.quantity > 0.001 }} lote(s) ativo(s)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Option 0: PDF Report Export
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(RoyalBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Relatório Formatado em PDF",
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Layout A4 profissional para impressão e envio",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Gera um documento PDF oficial com cabeçalho da empresa, tabela detalhada de lotes, estoque mínimo e resumo executivo.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (filteredProductsWithLots.isEmpty()) {
                                        Toast.makeText(context, "Nenhum produto atende aos filtros selecionados.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val pdfFile = PdfReportService.generateStockReportPdf(
                                        context = context,
                                        products = filteredProductsWithLots,
                                        filterName = selectedStockFilter.label,
                                        subtitleFilter = filterSummaryDescription
                                    )
                                    if (pdfFile != null) {
                                        PdfReportService.openOrSharePdf(
                                            context = context,
                                            file = pdfFile,
                                            title = "Relatorio_Estoque_${selectedStockFilter.label}"
                                        )
                                    } else {
                                        Toast.makeText(context, "Erro ao gerar PDF", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = filteredProductsWithLots.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoyalBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_export_pdf_dialog")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Gerar e Abrir Relatório PDF (${filteredProductsWithLots.size} itens)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 1: Drive / JSON
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RoyalBlue.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(RoyalBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Salvar no Google Drive / JSON",
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Backup completo de segurança",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Gera o arquivo de backup para envio direto ao Google Drive, WhatsApp ou armazenamento em nuvem.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val backupJson = BackupManager.createBackupJson(
                                            products = filteredProducts,
                                            categories = categories,
                                            lots = filteredLots,
                                            movements = filteredMovements,
                                            filterLabel = filterSummaryDescription
                                        )
                                        BackupManager.shareContent(
                                            context = context,
                                            text = backupJson,
                                            title = "Backup_Estoque_${System.currentTimeMillis()}.json",
                                            mimeType = "application/json"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoyalBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_export_drive")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Salvar no Drive", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                FilledTonalButton(
                                    onClick = {
                                        val backupJson = BackupManager.createBackupJson(
                                            products = filteredProducts,
                                            categories = categories,
                                            lots = filteredLots,
                                            movements = filteredMovements,
                                            filterLabel = filterSummaryDescription
                                        )
                                        clipboardManager.setText(AnnotatedString(backupJson))
                                        Toast.makeText(context, "JSON copiado com sucesso!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar JSON", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copiar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 2: CSV
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(EmeraldGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Planilha CSV (Excel / Sheets)",
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Para análise no Excel, LibreOffice e Google Sheets",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Planilha com estoque mínimo, saldos, validades e locais de armazenamento dos itens filtrados.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val csv = BackupManager.createInventoryCsv(
                                            productsWithLots = filteredProductsWithLots,
                                            alertDays = selectedAlertDays
                                        )
                                        BackupManager.shareContent(
                                            context = context,
                                            text = csv,
                                            title = "Estoque_${System.currentTimeMillis()}.csv",
                                            mimeType = "text/csv"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreen,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_export_csv")
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Exportar CSV", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                FilledTonalButton(
                                    onClick = {
                                        val reportText = BackupManager.createExecutiveSummaryReport(
                                            productsWithLots = filteredProductsWithLots,
                                            movements = filteredMovements,
                                            alertDays = selectedAlertDays,
                                            filterDescription = filterSummaryDescription
                                        )
                                        BackupManager.shareContent(
                                            context = context,
                                            text = reportText,
                                            title = "Relatorio_Estoque_${System.currentTimeMillis()}.txt"
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Compartilhar Relatório", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Texto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // TAB 2: RESTORE LOCAL JSON
                if (selectedTab == 2) {
                    Text(
                        text = "Restaurar Backup do Estoque:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cole o conteúdo do arquivo JSON de backup exportado anteriormente para recuperar seus produtos, estoques mínimos e movimentações.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        label = { Text("Conteúdo do Backup JSON") },
                        placeholder = { Text("Cole o JSON do backup aqui...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("input_restore_json"),
                        maxLines = 6
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (restoreJsonText.isNotBlank()) {
                                showRestoreConfirmDialog = true
                            }
                        },
                        enabled = restoreJsonText.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_confirm_restore")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restaurar Dados do Texto", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Fechar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Confirmation dialog for Cloud Download
    if (showCloudDownloadConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCloudDownloadConfirmDialog = false },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = RoyalBlue) },
            title = { Text("Baixar Dados da Nuvem?") },
            text = {
                Text("Atenção: Os dados atuais deste aparelho serão substituídos pela versão armazenada no Firebase Realtime Database. Deseja continuar?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCloudDownloadConfirmDialog = false
                        isCloudDownloading = true
                        cloudStatusMessage = "Baixando e restaurando dados..."
                        onCloudDownload { success, msg ->
                            isCloudDownloading = false
                            isCloudStatusSuccess = success
                            cloudStatusMessage = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Text("Sim, Baixar e Substituir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloudDownloadConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Confirmation dialog for local JSON restore
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            icon = { Icon(Icons.Default.History, contentDescription = null, tint = RoseRed) },
            title = { Text("Atenção: Sobrescrita de Dados!") },
            text = {
                Text("Deseja importar este backup? ATENÇÃO: Todos os dados atuais do aplicativo (categorias, produtos, lotes e histórico) serão APAGADOS e substituídos pelos dados do arquivo de backup de forma irreversível.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        onRestoreBackup(restoreJsonText)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Text("Restaurar e Substituir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
