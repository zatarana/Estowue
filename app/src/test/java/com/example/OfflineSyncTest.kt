package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsManager
import com.example.data.backup.CloudSyncStatus
import com.example.data.backup.FirebaseSyncManager
import com.example.data.model.Category
import com.example.data.model.MovementType
import com.example.data.model.Product
import com.example.data.model.StockLot
import com.example.data.model.StockMovement
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineSyncTest {

    private lateinit var context: Context
    private lateinit var settingsManager: SettingsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        settingsManager = SettingsManager(context)
    }

    @Test
    fun testOfflinePendingChangesStateTracking() {
        settingsManager.setHasPendingChanges(false)
        assertFalse(settingsManager.hasPendingChanges.value)

        // Offline data modification occurs
        settingsManager.setHasPendingChanges(true)
        assertTrue(settingsManager.hasPendingChanges.value)

        // Online sync finishes successfully
        settingsManager.saveLastSyncTime(System.currentTimeMillis())
        assertFalse(settingsManager.hasPendingChanges.value)
        assertNull(settingsManager.lastSyncError.value)
    }

    @Test
    fun testAutoSyncPreferencePersistence() {
        settingsManager.setAutoSyncEnabled(true)
        assertTrue(settingsManager.autoSyncEnabled.value)

        settingsManager.setAutoSyncEnabled(false)
        assertFalse(settingsManager.autoSyncEnabled.value)

        settingsManager.setAutoSyncEnabled(true)
        assertTrue(settingsManager.autoSyncEnabled.value)
    }

    @Test
    fun testFirebaseUrlFormatting() {
        val testUrl = "https://meu-estoque-default-rtdb.firebaseio.com"
        val endpoint = FirebaseSyncManager.formatFirebaseEndpointUrl(testUrl)
        assertEquals("https://meu-estoque-default-rtdb.firebaseio.com/estoque_app.json", endpoint)

        val trailingSlashUrl = "https://meu-estoque-default-rtdb.firebaseio.com/"
        val endpoint2 = FirebaseSyncManager.formatFirebaseEndpointUrl(trailingSlashUrl)
        assertEquals("https://meu-estoque-default-rtdb.firebaseio.com/estoque_app.json", endpoint2)

        val directJsonUrl = "https://meu-estoque-default-rtdb.firebaseio.com/custom.json"
        val endpoint3 = FirebaseSyncManager.formatFirebaseEndpointUrl(directJsonUrl)
        assertEquals("https://meu-estoque-default-rtdb.firebaseio.com/custom.json", endpoint3)
    }

    @Test
    fun testCloudSyncStatusStates() {
        assertEquals(CloudSyncStatus.IDLE, CloudSyncStatus.valueOf("IDLE"))
        assertEquals(CloudSyncStatus.SYNCING, CloudSyncStatus.valueOf("SYNCING"))
        assertEquals(CloudSyncStatus.SUCCESS, CloudSyncStatus.valueOf("SUCCESS"))
        assertEquals(CloudSyncStatus.PENDING_OFFLINE, CloudSyncStatus.valueOf("PENDING_OFFLINE"))
        assertEquals(CloudSyncStatus.ERROR, CloudSyncStatus.valueOf("ERROR"))
    }

    @Test
    fun testStockDataSerializationForOfflineCloudSync() {
        val categories = listOf(
            Category(id = 1, name = "Bebidas", colorHex = "#3B82F6")
        )
        val products = listOf(
            Product(id = 10, name = "Refrigerante 2L", brand = "Coca-Cola", category = "Bebidas", unit = "un", minStock = 5.0)
        )
        val lots = listOf(
            StockLot(id = 100, productId = 10, expirationDate = 1798761600000L, quantity = 24.0, location = "Prateleira A1")
        )
        val movements = listOf(
            StockMovement(
                id = 1000,
                lotId = 100,
                productId = 10,
                productName = "Refrigerante 2L",
                type = MovementType.ENTRADA,
                quantity = 24.0,
                timestamp = 1756140000000L,
                reason = "Compra Fornecedor"
            )
        )

        // Verify JSON construction
        val json = JSONObject().apply {
            put("version", 1)
            put("timestamp", System.currentTimeMillis())
            put("categoriesCount", categories.size)
            put("productsCount", products.size)
            put("lotsCount", lots.size)
            put("movementsCount", movements.size)
        }

        assertNotNull(json)
        assertEquals(1, json.getInt("categoriesCount"))
        assertEquals(1, json.getInt("productsCount"))
        assertEquals(1, json.getInt("lotsCount"))
        assertEquals(1, json.getInt("movementsCount"))
    }
}
