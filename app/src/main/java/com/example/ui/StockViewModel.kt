package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.backup.CloudSyncStatus
import com.example.data.backup.FirebaseSyncManager
import com.example.data.local.StockDatabase
import com.example.data.model.Category
import com.example.data.model.MovementType
import com.example.data.model.Product
import com.example.data.model.ProductWithLots
import com.example.data.model.StockHealthStatus
import com.example.data.model.StockLot
import com.example.data.model.StockLotsSummary
import com.example.data.model.StockMovement
import com.example.data.repository.StockRepository
import com.example.utils.NetworkConnectivityObserver
import com.example.utils.StockNotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExpirationFilter(val label: String) {
    ALL("Todos"),
    LOW_STOCK("Abaixo do Mínimo"),
    EXPIRED("Vencidos"),
    EXPIRING_SOON("Próximos a Vencer"),
    VALID("Válidos")
}

class StockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StockRepository
    val settingsManager = com.example.data.SettingsManager(application)
    private val db: StockDatabase
    private val networkObserver = NetworkConnectivityObserver(application)

    val brands: StateFlow<List<String>> = settingsManager.brands
    val locations: StateFlow<List<String>> = settingsManager.locations
    val firebaseUrl: StateFlow<String> = settingsManager.firebaseUrl
    val lastSyncTime: StateFlow<Long> = settingsManager.lastSyncTime
    val autoSyncEnabled: StateFlow<Boolean> = settingsManager.autoSyncEnabled
    val hasPendingChanges: StateFlow<Boolean> = settingsManager.hasPendingChanges
    val lastSyncError: StateFlow<String?> = settingsManager.lastSyncError

    val isOnline: StateFlow<Boolean> = networkObserver.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, networkObserver.isCurrentlyOnline())

    private val _cloudSyncStatus = MutableStateFlow(
        if (settingsManager.hasPendingChanges.value) CloudSyncStatus.PENDING_OFFLINE else CloudSyncStatus.IDLE
    )
    val cloudSyncStatus: StateFlow<CloudSyncStatus> = _cloudSyncStatus.asStateFlow()

    private var autoSyncJob: Job? = null

    init {
        db = StockDatabase.getDatabase(application, viewModelScope)
        repository = StockRepository(
            db = db,
            productDao = db.productDao(),
            lotDao = db.stockLotDao(),
            movementDao = db.stockMovementDao(),
            categoryDao = db.categoryDao()
        )

        // Monitor network and auto sync pending offline changes
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online) {
                    if (settingsManager.hasPendingChanges.value &&
                        settingsManager.autoSyncEnabled.value &&
                        settingsManager.firebaseUrl.value.isNotBlank()
                    ) {
                        scheduleAutoSync(delayMs = 1500L)
                    } else if (!settingsManager.hasPendingChanges.value && settingsManager.lastSyncError.value == null && settingsManager.lastSyncTime.value > 0L) {
                        _cloudSyncStatus.value = CloudSyncStatus.SUCCESS
                    }
                } else {
                    if (settingsManager.hasPendingChanges.value) {
                        _cloudSyncStatus.value = CloudSyncStatus.PENDING_OFFLINE
                    }
                }
            }
        }
    }

    val productsWithLots: StateFlow<List<ProductWithLots>> = repository.productsWithLots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMovements: StateFlow<List<StockMovement>> = repository.allMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val configuredCategories: StateFlow<List<Category>> = repository.categoriesWithCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActiveLots: StateFlow<List<StockLot>> = repository.activeLots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLots: StateFlow<List<StockLot>> = repository.allLots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // SharedPreferences for persisting user settings
    private val sharedPreferences = application.getSharedPreferences("stock_settings", Context.MODE_PRIVATE)

    // Customizable Expiration Alert Antecedence (in days, decided by user)
    private val _expirationAlertDays = MutableStateFlow(sharedPreferences.getInt("expiration_alert_days", 30))
    val expirationAlertDays: StateFlow<Int> = _expirationAlertDays.asStateFlow()

    // UI Filtering & Search State
    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedHealthFilter = MutableStateFlow<StockHealthStatus?>(null)
    val selectedHealthFilter: StateFlow<StockHealthStatus?> = _selectedHealthFilter.asStateFlow()

    private val _selectedExpirationFilter = MutableStateFlow(ExpirationFilter.ALL)
    val selectedExpirationFilter: StateFlow<ExpirationFilter> = _selectedExpirationFilter.asStateFlow()

    private data class FilterParams(
        val query: String,
        val category: String?,
        val health: StockHealthStatus?,
        val expFilter: ExpirationFilter,
        val alertDays: Int
    )

    // Filtered Products
    val filteredProductsWithLots: StateFlow<List<ProductWithLots>> = combine(
        productsWithLots,
        combine(
            _searchQuery,
            _selectedCategoryFilter,
            _selectedHealthFilter,
            _selectedExpirationFilter,
            _expirationAlertDays
        ) { query, category, health, expFilter, alertDays ->
            FilterParams(query, category, health, expFilter, alertDays)
        }
    ) { products, params ->
        val now = System.currentTimeMillis()

        products.filter { pWithLots ->
            val product = pWithLots.product

            val matchesQuery = params.query.isBlank() ||
                product.name.contains(params.query, ignoreCase = true) ||
                product.brand.contains(params.query, ignoreCase = true) ||
                product.barcode.contains(params.query, ignoreCase = true) ||
                product.category.contains(params.query, ignoreCase = true) ||
                product.location.contains(params.query, ignoreCase = true)

            val matchesCategory = params.category == null || product.category.equals(params.category, ignoreCase = true)
            val matchesHealth = params.health == null || pWithLots.stockHealthStatus == params.health

            val matchesExpFilter = when (params.expFilter) {
                ExpirationFilter.ALL -> true
                ExpirationFilter.LOW_STOCK -> pWithLots.isLowStock
                ExpirationFilter.EXPIRED -> pWithLots.lots.any { it.quantity > 0.001 && it.daysUntilExpiration(now) < 0 }
                ExpirationFilter.EXPIRING_SOON -> pWithLots.lots.any { it.quantity > 0.001 && it.daysUntilExpiration(now) in 0..params.alertDays }
                ExpirationFilter.VALID -> pWithLots.lots.any { it.quantity > 0.001 && it.daysUntilExpiration(now) > params.alertDays }
            }

            matchesQuery && matchesCategory && matchesHealth && matchesExpFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // Summary calculation dynamically responding to user-defined alert window
    val summary: StateFlow<StockLotsSummary> = combine(productsWithLots, _expirationAlertDays) { list, alertDays ->
        repository.calculateSummary(list, alertDays)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StockLotsSummary())

    // UI Feedback Event Channel
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    fun setExpirationAlertDays(days: Int) {
        val coercedDays = days.coerceIn(1, 365)
        _expirationAlertDays.value = coercedDays
        sharedPreferences.edit().putInt("expiration_alert_days", coercedDays).apply()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun setHealthFilter(health: StockHealthStatus?) {
        _selectedHealthFilter.value = if (_selectedHealthFilter.value == health) null else health
    }

    fun setExpirationFilter(filter: ExpirationFilter) {
        _selectedExpirationFilter.value = filter
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategoryFilter.value = null
        _selectedHealthFilter.value = null
        _selectedExpirationFilter.value = ExpirationFilter.ALL
    }

    // Category Management
    fun saveCategory(category: Category, oldName: String = "") {
        viewModelScope.launch {
            if (category.id == 0L) {
                repository.insertCategory(category)
                _uiEvents.emit("Categoria '${category.name}' criada!")
            } else {
                repository.updateCategory(category, oldName)
                _uiEvents.emit("Categoria '${category.name}' atualizada!")
            }
            notifyLocalDataChanged()
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _uiEvents.emit("Categoria '${category.name}' removida. Produtos realocados para 'Geral'.")
            notifyLocalDataChanged()
        }
    }

    // Product Management
    fun saveProduct(product: Product, isNew: Boolean = false) {
        viewModelScope.launch {
            if (product.barcode.isNotBlank()) {
                val existing = repository.getProductByBarcode(product.barcode)
                if (existing != null && existing.id != product.id) {
                    _uiEvents.emit("Erro: Código de barras '${product.barcode}' já está em uso pelo produto '${existing.name}'.")
                    return@launch
                }
            }
            if (isNew || product.id == 0L) {
                val newId = repository.insertProduct(product)
                _uiEvents.emit("Produto '${product.name}' cadastrado!")
                if (product.minStock > 0) {
                    StockNotificationHelper.sendLowStockNotification(getApplication(), product.copy(id = newId), 0.0)
                }
            } else {
                repository.updateProduct(product)
                _uiEvents.emit("Produto '${product.name}' atualizado!")
                val currentItem = productsWithLots.value.find { it.product.id == product.id }
                val currentQty = currentItem?.totalQuantity ?: 0.0
                if (product.minStock > 0 && currentQty <= product.minStock) {
                    StockNotificationHelper.sendLowStockNotification(getApplication(), product, currentQty)
                }
            }
            notifyLocalDataChanged()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _uiEvents.emit("Produto '${product.name}' excluído.")
            notifyLocalDataChanged()
        }
    }

    fun registerStockIn(
        productId: Long,
        expirationDate: Long,
        quantity: Double,
        manufacturingDate: Long? = null,
        location: String = "",
        reason: String = "Recebimento de Mercadoria",
        documentNumber: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val result = repository.registerStockIn(
                productId = productId,
                expirationDate = expirationDate,
                quantity = quantity,
                manufacturingDate = manufacturingDate,
                location = location,
                reason = reason,
                documentNumber = documentNumber,
                notes = notes
            )
            result.onSuccess {
                _uiEvents.emit("Entrada de $quantity un registrada com sucesso!")
                notifyLocalDataChanged()
            }.onFailure { e ->
                _uiEvents.emit("Erro: ${e.message ?: "Falha ao registrar entrada"}")
            }
        }
    }

    fun registerLotTransfer(
        sourceLotId: Long,
        quantity: Double,
        destinationLocation: String,
        reason: String = "Transferência Interna",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val result = repository.registerLotTransfer(
                sourceLotId, quantity, destinationLocation, reason, notes
            )
            if (result.isSuccess) {
                _uiEvents.emit("Transferência registrada com sucesso!")
                notifyLocalDataChanged()
            } else {
                val e = result.exceptionOrNull()
                _uiEvents.emit("Erro: ${e?.message ?: "Falha ao registrar transferência"}")
            }
        }
    }

    fun registerStockOut(
        lotId: Long,
        quantity: Double,
        isDiscard: Boolean = false,
        reason: String = if (isDiscard) "Descarte por Validade" else "Saída / Expedição",
        documentNumber: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val result = repository.registerStockOut(
                lotId = lotId,
                quantity = quantity,
                isDiscard = isDiscard,
                reason = reason,
                documentNumber = documentNumber,
                notes = notes
            )
            result.onSuccess { movement ->
                _uiEvents.emit("Saída de $quantity un concluída!")
                notifyLocalDataChanged()
                val product = repository.getProductById(movement.productId)
                if (product != null && product.minStock > 0 && movement.resultingTotalStock <= product.minStock) {
                    StockNotificationHelper.sendLowStockNotification(getApplication(), product, movement.resultingTotalStock)
                }
            }.onFailure { e ->
                _uiEvents.emit("Atenção: ${e.message ?: "Erro ao registrar saída"}")
            }
        }
    }

    fun correctStockMovement(
        movementId: Long,
        newQuantity: Double,
        newExpirationDate: Long?,
        newReason: String,
        newDocumentNumber: String,
        newNotes: String
    ) {
        viewModelScope.launch {
            val result = repository.correctStockMovement(
                movementId = movementId,
                newQuantity = newQuantity,
                newExpirationDate = newExpirationDate,
                newReason = newReason,
                newDocumentNumber = newDocumentNumber,
                newNotes = newNotes
            )
            result.onSuccess { movement ->
                _uiEvents.emit("Lançamento corrigido com sucesso! Saldo recalculado.")
                notifyLocalDataChanged()
                val product = repository.getProductById(movement.productId)
                if (product != null && product.minStock > 0 && movement.resultingTotalStock <= product.minStock) {
                    StockNotificationHelper.sendLowStockNotification(getApplication(), product, movement.resultingTotalStock)
                }
            }.onFailure { e ->
                _uiEvents.emit("Erro ao corrigir: ${e.message}")
            }
        }
    }

    fun deleteOrReverseMovement(movementId: Long) {
        viewModelScope.launch {
            val result = repository.deleteOrReverseMovement(movementId)
            result.onSuccess {
                _uiEvents.emit("Lançamento estornado e excluído com sucesso!")
                notifyLocalDataChanged()
            }.onFailure { e ->
                _uiEvents.emit("Erro no estorno: ${e.message}")
            }
        }
    }

    fun registerLotAdjustment(
        lotId: Long,
        physicalCount: Double,
        reason: String = "Inventário de Saldo",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val result = repository.registerLotAdjustment(
                lotId = lotId,
                physicalCount = physicalCount,
                reason = reason,
                notes = notes
            )
            result.onSuccess { movement ->
                _uiEvents.emit("Saldo ajustado para $physicalCount un!")
                notifyLocalDataChanged()
                val product = repository.getProductById(movement.productId)
                if (product != null && product.minStock > 0 && movement.resultingTotalStock <= product.minStock) {
                    StockNotificationHelper.sendLowStockNotification(getApplication(), product, movement.resultingTotalStock)
                }
            }.onFailure { e ->
                _uiEvents.emit("Erro: ${e.message ?: "Falha no ajuste"}")
            }
        }
    }

    fun discardExpiredLot(lot: StockLot, notes: String = "Descarte de lote vencido") {
        viewModelScope.launch {
            val result = repository.registerStockOut(
                lotId = lot.id,
                quantity = lot.quantity,
                isDiscard = true,
                reason = "Descarte por Vencimento",
                notes = notes
            )
            result.onSuccess { movement ->
                _uiEvents.emit("Saldo de ${lot.quantity} un descartado do estoque.")
                notifyLocalDataChanged()
                val product = repository.getProductById(movement.productId)
                if (product != null && product.minStock > 0 && movement.resultingTotalStock <= product.minStock) {
                    StockNotificationHelper.sendLowStockNotification(getApplication(), product, movement.resultingTotalStock)
                }
            }.onFailure { e ->
                _uiEvents.emit("Erro no descarte: ${e.message}")
            }
        }
    }

    fun triggerLowStockCheck(): Int {
        return StockNotificationHelper.checkAllProducts(getApplication(), productsWithLots.value)
    }

    fun restoreBackupJson(jsonString: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val root = JSONObject(jsonString)
                val version = root.optInt("version", 1)
                
                db.clearAllTables()

                val catsArray = root.optJSONArray("categories")
                if (catsArray != null) {
                    for (i in 0 until catsArray.length()) {
                        val obj = catsArray.getJSONObject(i)
                        val c = Category(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            colorHex = obj.optString("colorHex", "#3B82F6"),
                            description = obj.optString("description", "")
                        )
                        repository.insertCategory(c)
                    }
                }

                val prodsArray = root.optJSONArray("products")
                if (prodsArray != null) {
                    for (i in 0 until prodsArray.length()) {
                        val obj = prodsArray.getJSONObject(i)
                        val p = Product(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            brand = obj.optString("brand", ""),
                            barcode = obj.optString("barcode", ""),
                            category = obj.optString("category", "Geral"),
                            unit = obj.optString("unit", "un"),
                            minStock = obj.optDouble("minStock", 0.0),
                            location = obj.optString("location", ""),
                            description = obj.optString("description", ""),
                            lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
                        )
                        repository.insertProduct(p)
                    }
                }

                val lotsArray = root.optJSONArray("lots")
                if (lotsArray != null) {
                    for (i in 0 until lotsArray.length()) {
                        val obj = lotsArray.getJSONObject(i)
                        val l = StockLot(
                            id = obj.optLong("id", 0L),
                            productId = obj.getLong("productId"),
                            lotNumber = obj.optString("lotNumber", ""),
                            quantity = obj.getDouble("quantity"),
                            initialQuantity = obj.optDouble("initialQuantity", obj.getDouble("quantity")),
                            expirationDate = obj.getLong("expirationDate"),
                            manufacturingDate = if (obj.has("manufacturingDate")) obj.getLong("manufacturingDate") else null,
                            location = obj.optString("location", ""),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        repository.insertLot(l)
                    }
                }

                val movsArray = root.optJSONArray("movements")
                if (movsArray != null) {
                    for (i in 0 until movsArray.length()) {
                        val obj = movsArray.getJSONObject(i)
                        val typeStr = obj.optString("type", "SAIDA")
                        val type = try { MovementType.valueOf(typeStr) } catch(e: Exception) { MovementType.SAIDA }
                        val m = StockMovement(
                            id = obj.optLong("id", 0L),
                            productId = obj.getLong("productId"),
                            productName = obj.optString("productName", ""),
                            productBrand = obj.optString("productBrand", ""),
                            lotId = if (obj.has("lotId") && obj.getLong("lotId") != 0L) obj.getLong("lotId") else null,
                            lotNumber = obj.optString("lotNumber", ""),
                            lotExpirationDate = if (obj.has("lotExpirationDate") && obj.getLong("lotExpirationDate") != 0L) obj.getLong("lotExpirationDate") else null,
                            type = type,
                            quantity = obj.getDouble("quantity"),
                            previousLotStock = obj.optDouble("previousLotStock", 0.0),
                            resultingLotStock = obj.optDouble("resultingLotStock", 0.0),
                            previousTotalStock = obj.optDouble("previousTotalStock", 0.0),
                            resultingTotalStock = obj.optDouble("resultingTotalStock", 0.0),
                            reason = obj.optString("reason", ""),
                            documentNumber = obj.optString("documentNumber", ""),
                            notes = obj.optString("notes", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        // Insert movement via DAO since repository.insertMovement is not directly exposed
                        db.stockMovementDao().insertMovement(m)
                    }
                }

                _uiEvents.emit("Backup restaurado com sucesso! ($version)")
            } catch (e: Exception) {
                _uiEvents.emit("Falha ao restaurar backup: ${e.message}")
            }
        }
    }

    fun addBrand(b: String) {
        settingsManager.addBrand(b)
        notifyLocalDataChanged()
    }

    fun updateBrand(old: String, new: String) {
        settingsManager.updateBrand(old, new)
        notifyLocalDataChanged()
    }

    fun removeBrand(b: String) {
        settingsManager.removeBrand(b)
        notifyLocalDataChanged()
    }

    fun addLocation(l: String) {
        settingsManager.addLocation(l)
        notifyLocalDataChanged()
    }

    fun updateLocation(old: String, new: String) {
        settingsManager.updateLocation(old, new)
        notifyLocalDataChanged()
    }

    fun removeLocation(l: String) {
        settingsManager.removeLocation(l)
        notifyLocalDataChanged()
    }

    fun saveFirebaseUrl(url: String) {
        settingsManager.saveFirebaseUrl(url)
        if (url.isNotBlank() && settingsManager.hasPendingChanges.value && networkObserver.isCurrentlyOnline()) {
            scheduleAutoSync(delayMs = 1000L)
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        settingsManager.setAutoSyncEnabled(enabled)
        if (enabled && settingsManager.hasPendingChanges.value && networkObserver.isCurrentlyOnline() && settingsManager.firebaseUrl.value.isNotBlank()) {
            scheduleAutoSync(delayMs = 1000L)
        }
    }

    private fun notifyLocalDataChanged() {
        settingsManager.setHasPendingChanges(true)
        if (!networkObserver.isCurrentlyOnline()) {
            _cloudSyncStatus.value = CloudSyncStatus.PENDING_OFFLINE
        } else if (settingsManager.autoSyncEnabled.value && settingsManager.firebaseUrl.value.isNotBlank()) {
            scheduleAutoSync(delayMs = 1500L)
        }
    }

    private fun scheduleAutoSync(delayMs: Long = 1500L) {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch {
            delay(delayMs)
            syncUploadToCloud(isAutomatic = true)
        }
    }

    fun syncUploadToCloud(
        isAutomatic: Boolean = false,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val url = settingsManager.firebaseUrl.value
            if (url.isBlank()) {
                val errorMsg = "Configure a URL do Firebase primeiro."
                if (!isAutomatic) _uiEvents.emit(errorMsg)
                _cloudSyncStatus.value = CloudSyncStatus.IDLE
                onComplete?.invoke(false, errorMsg)
                return@launch
            }

            if (!networkObserver.isCurrentlyOnline()) {
                val offlineMsg = "Dispositivo offline. Sincronização agendada para quando a internet voltar."
                _cloudSyncStatus.value = CloudSyncStatus.PENDING_OFFLINE
                if (!isAutomatic) _uiEvents.emit(offlineMsg)
                onComplete?.invoke(false, offlineMsg)
                return@launch
            }

            _cloudSyncStatus.value = CloudSyncStatus.SYNCING

            val backupJson = BackupManager.createBackupJson(
                products = allProducts.value,
                categories = configuredCategories.value,
                lots = allLots.value,
                movements = allMovements.value,
                filterLabel = "Sincronização Online Firebase"
            )

            val result = FirebaseSyncManager.uploadToCloud(url, backupJson, maxRetries = if (isAutomatic) 3 else 2)
            result.onSuccess { msg ->
                val now = System.currentTimeMillis()
                settingsManager.saveLastSyncTime(now)
                _cloudSyncStatus.value = CloudSyncStatus.SUCCESS
                if (!isAutomatic) {
                    _uiEvents.emit("☁️ $msg")
                }
                onComplete?.invoke(true, msg)
            }.onFailure { err ->
                val errorMsg = err.message ?: "Erro na sincronização"
                settingsManager.setLastSyncError(errorMsg)
                _cloudSyncStatus.value = if (!networkObserver.isCurrentlyOnline()) {
                    CloudSyncStatus.PENDING_OFFLINE
                } else {
                    CloudSyncStatus.ERROR
                }
                if (!isAutomatic) {
                    _uiEvents.emit("Erro na sincronização: $errorMsg")
                }
                onComplete?.invoke(false, errorMsg)
            }
        }
    }

    fun syncDownloadFromCloud(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val url = settingsManager.firebaseUrl.value
            if (url.isBlank()) {
                val errorMsg = "Configure a URL do Firebase primeiro."
                _uiEvents.emit(errorMsg)
                onComplete?.invoke(false, errorMsg)
                return@launch
            }

            if (!networkObserver.isCurrentlyOnline()) {
                val offlineMsg = "Sem conexão com a internet. Não é possível baixar da nuvem."
                _uiEvents.emit(offlineMsg)
                onComplete?.invoke(false, offlineMsg)
                return@launch
            }

            _cloudSyncStatus.value = CloudSyncStatus.SYNCING

            val result = FirebaseSyncManager.downloadFromCloud(url)
            result.onSuccess { jsonContent ->
                restoreBackupJson(jsonContent)
                val now = System.currentTimeMillis()
                settingsManager.saveLastSyncTime(now)
                _cloudSyncStatus.value = CloudSyncStatus.SUCCESS
                _uiEvents.emit("📥 Dados baixados do Firebase e restaurados com sucesso!")
                onComplete?.invoke(true, "Dados baixados com sucesso!")
            }.onFailure { err ->
                val msg = "Falha ao baixar da nuvem: ${err.message}"
                settingsManager.setLastSyncError(err.message)
                _cloudSyncStatus.value = CloudSyncStatus.ERROR
                _uiEvents.emit(msg)
                onComplete?.invoke(false, msg)
            }
        }
    }

    fun testFirebaseConnection(url: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = FirebaseSyncManager.testConnection(url)
            result.onSuccess {
                onResult(true, "Conexão com o Firebase realizada com sucesso!")
            }.onFailure { err ->
                onResult(false, err.message ?: "Erro ao testar conexão.")
            }
        }
    }
}
