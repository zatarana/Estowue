package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Category
import com.example.data.model.MovementType
import com.example.data.model.Product
import com.example.data.model.StockLot
import com.example.data.model.StockMovement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Product::class, StockLot::class, StockMovement::class, Category::class],
    version = 6,
    exportSchema = false
)
abstract class StockDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun stockLotDao(): StockLotDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: StockDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): StockDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    StockDatabase::class.java,
                    "controle_estoque_lotes_db"
                )
                .addCallback(StockDatabaseCallback(appContext, scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class StockDatabaseCallback(
            private val context: Context,
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                scope.launch(Dispatchers.IO) {
                    try {
                        val database = getDatabase(context, scope)
                        populateInitialData(
                            database.productDao(),
                            database.stockLotDao(),
                            database.stockMovementDao(),
                            database.categoryDao()
                        )
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }

        suspend fun populateInitialData(
            productDao: ProductDao,
            lotDao: StockLotDao,
            movementDao: StockMovementDao,
            categoryDao: CategoryDao
        ) {
            val now = System.currentTimeMillis()
            val oneDay = 86400000L

            // 1. Initial Categories
            val defaultCategories = listOf(
                Category(name = "Laticínios", colorHex = "#3B82F6", description = "Leites, queijos e derivados"),
                Category(name = "Alimentos & Bebidas", colorHex = "#10B981", description = "Itens secos, grãos e bebidas"),
                Category(name = "Farmácia", colorHex = "#8B5CF6", description = "Medicamentos e primeiros socorros"),
                Category(name = "Frios & Carnes", colorHex = "#F59E0B", description = "Carnes e embutidos refrigerados"),
                Category(name = "Higiene & Limpeza", colorHex = "#06B6D4", description = "Produtos químicos e higiene"),
                Category(name = "Geral", colorHex = "#64748B", description = "Itens diversos")
            )
            categoryDao.insertAll(defaultCategories)

            // 2. Initial Products
            val initialProducts = listOf(
                Product(
                    name = "Leite Integral 1L",
                    brand = "Itambé",
                    barcode = "7891000100101",
                    category = "Laticínios",
                    unit = "cx",
                    minStock = 50.0, // Saldo inicial 44 -> abaixo do mínimo!
                    location = "Câmara Fria 1",
                    description = "Caixa de leite integral longa vida"
                ),
                Product(
                    name = "Iogurte Natural 170g",
                    brand = "Nestlé",
                    barcode = "7891000200202",
                    category = "Laticínios",
                    unit = "un",
                    minStock = 20.0,
                    location = "Geladeira Expositora",
                    description = "Iogurte natural integral pote"
                ),
                Product(
                    name = "Café Gourmet em Grãos 1kg",
                    brand = "Melitta",
                    barcode = "7891000300303",
                    category = "Alimentos & Bebidas",
                    unit = "pct",
                    minStock = 10.0,
                    location = "Armazém Seco C",
                    description = "Café arábica torra média"
                ),
                Product(
                    name = "Dipirona 500mg Cartela c/ 10",
                    brand = "Medley",
                    barcode = "7891000400404",
                    category = "Farmácia",
                    unit = "cx",
                    minStock = 25.0,
                    location = "Armário Farmácia",
                    description = "Analgésico e antitérmico"
                ),
                Product(
                    name = "Queijo Mussarela Peça 4kg",
                    brand = "Scala",
                    barcode = "7891000500505",
                    category = "Frios & Carnes",
                    unit = "kg",
                    minStock = 12.0, // Saldo atual 8 -> abaixo do mínimo!
                    location = "Câmara Fria 2",
                    description = "Peça inteira para fatiamento"
                ),
                Product(
                    name = "Álcool 70% Líquido 1L",
                    brand = "Coperalcool",
                    barcode = "7891000600606",
                    category = "Higiene & Limpeza",
                    unit = "frasco",
                    minStock = 15.0,
                    location = "Almoxarifado D1",
                    description = "Desinfetante hospitalar e geral"
                )
            )

            val productIds = productDao.insertAll(initialProducts)
            val p0 = productIds.getOrElse(0) { 1L }
            val p1 = productIds.getOrElse(1) { 2L }
            val p2 = productIds.getOrElse(2) { 3L }
            val p3 = productIds.getOrElse(3) { 4L }
            val p4 = productIds.getOrElse(4) { 5L }
            val p5 = productIds.getOrElse(5) { 6L }

            // 3. Initial Lots
            val initialLots = listOf(
                // Leite
                StockLot(
                    productId = p0,
                    lotNumber = "LT-LEI-0801",
                    quantity = 14.0,
                    initialQuantity = 24.0,
                    expirationDate = now + (oneDay * 6), // 6 dias (Crítico)
                    manufacturingDate = now - (oneDay * 40),
                    location = "Câmara Fria 1",
                    notes = "Prioridade de saída (FEFO)"
                ),
                StockLot(
                    productId = p0,
                    lotNumber = "LT-LEI-0915",
                    quantity = 30.0,
                    initialQuantity = 30.0,
                    expirationDate = now + (oneDay * 28), // 28 dias
                    manufacturingDate = now - (oneDay * 10),
                    location = "Câmara Fria 1"
                ),

                // Iogurte: 1 lote vencido, 1 lote ativo
                StockLot(
                    productId = p1,
                    lotNumber = "LT-IOG-0710",
                    quantity = 6.0,
                    initialQuantity = 20.0,
                    expirationDate = now - (oneDay * 3), // VENCIDO!
                    manufacturingDate = now - (oneDay * 33),
                    location = "Setor Retenção",
                    notes = "Aguardando descarte"
                ),
                StockLot(
                    productId = p1,
                    lotNumber = "LT-IOG-0820",
                    quantity = 25.0,
                    initialQuantity = 30.0,
                    expirationDate = now + (oneDay * 14), // 14 dias (Crítico)
                    manufacturingDate = now - (oneDay * 16),
                    location = "Geladeira 2"
                ),

                // Café: 1 lote longo
                StockLot(
                    productId = p2,
                    lotNumber = "LT-CAF-2026",
                    quantity = 28.0,
                    initialQuantity = 30.0,
                    expirationDate = now + (oneDay * 180), // Válido (6 meses)
                    manufacturingDate = now - (oneDay * 15),
                    location = "Armazém Seco C"
                ),

                // Dipirona: 2 lotes
                StockLot(
                    productId = p3,
                    lotNumber = "LT-DIP-89A",
                    quantity = 8.0,
                    initialQuantity = 20.0,
                    expirationDate = now + (oneDay * 22),
                    manufacturingDate = now - (oneDay * 300),
                    location = "Gaveta 1"
                ),
                StockLot(
                    productId = p3,
                    lotNumber = "LT-DIP-94B",
                    quantity = 40.0,
                    initialQuantity = 40.0,
                    expirationDate = now + (oneDay * 360),
                    manufacturingDate = now - (oneDay * 20),
                    location = "Gaveta 2"
                ),

                // Queijo Mussarela: 1 lote crítico
                StockLot(
                    productId = p4,
                    lotNumber = "LT-MZ-440",
                    quantity = 8.0,
                    initialQuantity = 16.0,
                    expirationDate = now + (oneDay * 4), // 4 dias (Crítico)
                    manufacturingDate = now - (oneDay * 26),
                    location = "Câmara Fria 2"
                ),

                // Álcool 70%: 1 lote válido
                StockLot(
                    productId = p5,
                    lotNumber = "LT-ALC-771",
                    quantity = 35.0,
                    initialQuantity = 50.0,
                    expirationDate = now + (oneDay * 420),
                    manufacturingDate = now - (oneDay * 30),
                    location = "Almoxarifado D1"
                )
            )

            val lotIds = lotDao.insertAll(initialLots)

            // 4. Initial movements
            val initialMovements = listOf(
                StockMovement(
                    productId = p0,
                    productName = "Leite Integral 1L",
                    productBrand = "Itambé",
                    lotId = lotIds.getOrElse(0) { 1L },
                    lotNumber = "LT-LEI-0801",
                    lotExpirationDate = now + (oneDay * 6),
                    type = MovementType.ENTRADA,
                    quantity = 24.0,
                    previousLotStock = 0.0,
                    resultingLotStock = 24.0,
                    previousTotalStock = 0.0,
                    resultingTotalStock = 24.0,
                    reason = "Recebimento Fornecedor",
                    documentNumber = "NF-10492",
                    notes = "Entrada do lote LT-LEI-0801",
                    timestamp = now - (oneDay * 12)
                ),
                StockMovement(
                    productId = p0,
                    productName = "Leite Integral 1L",
                    productBrand = "Itambé",
                    lotId = lotIds.getOrElse(0) { 1L },
                    lotNumber = "LT-LEI-0801",
                    lotExpirationDate = now + (oneDay * 6),
                    type = MovementType.SAIDA,
                    quantity = 10.0,
                    previousLotStock = 24.0,
                    resultingLotStock = 14.0,
                    previousTotalStock = 54.0,
                    resultingTotalStock = 44.0,
                    reason = "Expedição / Consumo",
                    documentNumber = "REQ-881",
                    notes = "Saída pelo método FEFO (vencimento mais próximo)",
                    timestamp = now - (oneDay * 2)
                )
            )

            movementDao.insertAll(initialMovements)
        }
    }
}
