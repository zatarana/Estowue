package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MovementType
import com.example.data.model.StockMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC")
    fun getMovementsForProduct(productId: Long): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements WHERE type = :type ORDER BY timestamp DESC")
    fun getMovementsByType(type: MovementType): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements WHERE id = :id LIMIT 1")
    suspend fun getMovementById(id: Long): StockMovement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movements: List<StockMovement>): List<Long>

    @Update
    suspend fun updateMovement(movement: StockMovement)

    @Delete
    suspend fun deleteMovement(movement: StockMovement)

    @Query("DELETE FROM stock_movements WHERE id = :id")
    suspend fun deleteMovementById(id: Long)

    @Query("SELECT COUNT(*) FROM stock_movements")
    suspend fun getMovementsCount(): Int
}
