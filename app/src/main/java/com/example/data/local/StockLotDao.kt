package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.StockLot
import kotlinx.coroutines.flow.Flow

@Dao
interface StockLotDao {
    @Query("SELECT * FROM stock_lots ORDER BY expirationDate ASC")
    fun getAllLots(): Flow<List<StockLot>>

    @Query("SELECT * FROM stock_lots WHERE quantity > 0 ORDER BY expirationDate ASC")
    fun getActiveLots(): Flow<List<StockLot>>

    @Query("SELECT * FROM stock_lots WHERE productId = :productId ORDER BY expirationDate ASC")
    fun getLotsForProduct(productId: Long): Flow<List<StockLot>>

    @Query("SELECT * FROM stock_lots WHERE productId = :productId ORDER BY expirationDate ASC")
    suspend fun getLotsForProductDirect(productId: Long): List<StockLot>

    @Query("SELECT * FROM stock_lots WHERE id = :id LIMIT 1")
    fun getLotById(id: Long): Flow<StockLot?>

    @Query("SELECT * FROM stock_lots WHERE id = :id LIMIT 1")
    suspend fun getLotByIdDirect(id: Long): StockLot?

    @Query("SELECT * FROM stock_lots WHERE productId = :productId AND lotNumber = :lotNumber LIMIT 1")
    suspend fun getLotByNumberAndProduct(productId: Long, lotNumber: String): StockLot?

    @Query("SELECT * FROM stock_lots WHERE quantity > 0 AND expirationDate <= :timestampLimit ORDER BY expirationDate ASC")
    fun getExpiringLots(timestampLimit: Long): Flow<List<StockLot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLot(lot: StockLot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lots: List<StockLot>): List<Long>

    @Update
    suspend fun updateLot(lot: StockLot)

    @Delete
    suspend fun deleteLot(lot: StockLot)

    @Query("DELETE FROM stock_lots WHERE id = :id")
    suspend fun deleteLotById(id: Long)

    @Query("SELECT COUNT(*) FROM stock_lots WHERE quantity > 0")
    suspend fun getActiveLotsCount(): Int
}
