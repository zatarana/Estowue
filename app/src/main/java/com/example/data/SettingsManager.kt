package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _brands = MutableStateFlow<List<String>>(emptyList())
    val brands: StateFlow<List<String>> = _brands.asStateFlow()

    private val _locations = MutableStateFlow<List<String>>(emptyList())
    val locations: StateFlow<List<String>> = _locations.asStateFlow()

    private val _firebaseUrl = MutableStateFlow<String>("")
    val firebaseUrl: StateFlow<String> = _firebaseUrl.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long>(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _autoSyncEnabled = MutableStateFlow<Boolean>(true)
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

    private val _hasPendingChanges = MutableStateFlow<Boolean>(false)
    val hasPendingChanges: StateFlow<Boolean> = _hasPendingChanges.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val brandsStr = prefs.getString("brands", "[]")
        val locsStr = prefs.getString("locations", "[]")
        _firebaseUrl.value = prefs.getString("firebase_url", "") ?: ""
        _lastSyncTime.value = prefs.getLong("last_sync_time", 0L)
        _autoSyncEnabled.value = prefs.getBoolean("auto_sync_enabled", true)
        _hasPendingChanges.value = prefs.getBoolean("has_pending_changes", false)
        _lastSyncError.value = prefs.getString("last_sync_error", null)

        val bList = mutableListOf<String>()
        val lList = mutableListOf<String>()

        try {
            val bJson = JSONArray(brandsStr)
            for (i in 0 until bJson.length()) bList.add(bJson.getString(i))

            val lJson = JSONArray(locsStr)
            for (i in 0 until lJson.length()) lList.add(lJson.getString(i))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _brands.value = bList.sorted()
        _locations.value = lList.sorted()
    }

    fun addBrand(brand: String) {
        if (brand.isBlank() || _brands.value.contains(brand)) return
        val newList = _brands.value.toMutableList().apply { add(brand) }.sorted()
        saveBrands(newList)
    }

    fun removeBrand(brand: String) {
        val newList = _brands.value.toMutableList().apply { remove(brand) }
        saveBrands(newList)
    }

    fun updateBrand(oldBrand: String, newBrand: String) {
        if (newBrand.isBlank() || oldBrand == newBrand) return
        val newList = _brands.value.toMutableList().apply {
            remove(oldBrand)
            if (!contains(newBrand)) add(newBrand)
        }.sorted()
        saveBrands(newList)
    }

    private fun saveBrands(list: List<String>) {
        _brands.value = list
        prefs.edit().putString("brands", JSONArray(list).toString()).apply()
    }

    fun addLocation(location: String) {
        if (location.isBlank() || _locations.value.contains(location)) return
        val newList = _locations.value.toMutableList().apply { add(location) }.sorted()
        saveLocations(newList)
    }

    fun removeLocation(location: String) {
        val newList = _locations.value.toMutableList().apply { remove(location) }
        saveLocations(newList)
    }

    fun updateLocation(oldLocation: String, newLocation: String) {
        if (newLocation.isBlank() || oldLocation == newLocation) return
        val newList = _locations.value.toMutableList().apply {
            remove(oldLocation)
            if (!contains(newLocation)) add(newLocation)
        }.sorted()
        saveLocations(newList)
    }

    fun saveFirebaseUrl(url: String) {
        val trimmed = url.trim()
        _firebaseUrl.value = trimmed
        prefs.edit().putString("firebase_url", trimmed).apply()
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
        prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
    }

    fun setHasPendingChanges(hasPending: Boolean) {
        _hasPendingChanges.value = hasPending
        prefs.edit().putBoolean("has_pending_changes", hasPending).apply()
    }

    fun setLastSyncError(error: String?) {
        _lastSyncError.value = error
        if (error == null) {
            prefs.edit().remove("last_sync_error").apply()
        } else {
            prefs.edit().putString("last_sync_error", error).apply()
        }
    }

    fun saveLastSyncTime(timestamp: Long) {
        _lastSyncTime.value = timestamp
        _hasPendingChanges.value = false
        _lastSyncError.value = null
        prefs.edit()
            .putLong("last_sync_time", timestamp)
            .putBoolean("has_pending_changes", false)
            .remove("last_sync_error")
            .apply()
    }

    private fun saveLocations(list: List<String>) {
        _locations.value = list
        prefs.edit().putString("locations", JSONArray(list).toString()).apply()
    }
}
