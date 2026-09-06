package com.example.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Product
import com.example.data.model.ProductWithLots
import com.example.ui.components.formatQuantity

object StockNotificationHelper {

    const val CHANNEL_ID = "stock_min_level_alerts"
    const val CHANNEL_NAME = "Alertas de Estoque Mínimo"
    const val CHANNEL_DESCRIPTION = "Notificações disparadas quando o estoque de um produto atinge ou fica abaixo do nível mínimo."

    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESCRIPTION
                    enableLights(true)
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun sendLowStockNotification(context: Context, product: Product, currentStock: Double) {
        try {
            createNotificationChannel(context)

            // Verify permission for Android 13+ (TIRAMISU)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("SEARCH_QUERY", product.name)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                (product.id.toInt() and 0x7FFFFFFF),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val formattedCurrent = formatQuantity(currentStock)
            val formattedMin = formatQuantity(product.minStock)
            val isOutOfStock = currentStock <= 0.0001

            val title = if (isOutOfStock) {
                "🚨 Estoque Zerado: ${product.name}"
            } else {
                "⚠️ Estoque Mínimo Atingido: ${product.name}"
            }

            val shortMessage = "O produto '${product.name}' atingiu $formattedCurrent ${product.unit} (Mínimo: $formattedMin ${product.unit})."

            val bigText = buildString {
                if (isOutOfStock) {
                    append("O estoque de '${product.name}' está totalmente zerado!\n\n")
                } else {
                    append("O produto '${product.name}' atingiu ou ficou abaixo do estoque de segurança!\n\n")
                }
                append("• Saldo Atual: $formattedCurrent ${product.unit}\n")
                append("• Estoque Mínimo Definido: $formattedMin ${product.unit}\n")
                if (product.category.isNotBlank()) {
                    append("• Categoria: ${product.category}\n")
                }
                if (product.location.isNotBlank()) {
                    append("• Localização: ${product.location}\n")
                }
                append("\nRecomendado providenciar reabastecimento imediato.")
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_alert)
                .setContentTitle(title)
                .setContentText(shortMessage)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(0xFFF59E0B.toInt()) // Amber Warning color
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val notificationId = (product.id.toInt() and 0x7FFFFFFF) + 1000

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if a product needs a low-stock notification and triggers it if needed.
     */
    fun checkAndNotifyProduct(context: Context, product: Product, newTotalStock: Double) {
        if (product.minStock > 0 && newTotalStock <= product.minStock) {
            sendLowStockNotification(context, product, newTotalStock)
        }
    }

    /**
     * Evaluates all products and sends notifications for those at or below minStock.
     */
    fun checkAllProducts(context: Context, productsWithLots: List<ProductWithLots>): Int {
        var notifiedCount = 0
        productsWithLots.forEach { item ->
            val totalQty = item.totalQuantity
            if (item.product.minStock > 0 && totalQty <= item.product.minStock) {
                sendLowStockNotification(context, item.product, totalQty)
                notifiedCount++
            }
        }
        return notifiedCount
    }
}
