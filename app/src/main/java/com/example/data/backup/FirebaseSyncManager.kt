package com.example.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class CloudSyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    PENDING_OFFLINE,
    ERROR
}

object FirebaseSyncManager {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Normalizes any user-provided Firebase URL to point directly to the REST endpoint.
     * e.g. "https://meu-estoque-default-rtdb.firebaseio.com" -> "https://meu-estoque-default-rtdb.firebaseio.com/estoque_app.json"
     */
    fun formatFirebaseEndpointUrl(rawUrl: String, pathName: String = "estoque_app"): String {
        var clean = rawUrl.trim()
        if (clean.isBlank()) return ""

        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }

        // Remove trailing slashes
        while (clean.endsWith("/")) {
            clean = clean.dropLast(1)
        }

        if (clean.endsWith(".json")) {
            return clean
        }

        return "$clean/$pathName.json"
    }

    /**
     * Uploads the entire stock database JSON to Firebase Realtime Database using HTTP PUT,
     * with automatic retry on transient network failures.
     */
    suspend fun uploadToCloud(
        rawUrl: String,
        jsonContent: String,
        maxRetries: Int = 3
    ): Result<String> = withContext(Dispatchers.IO) {
        val endpoint = formatFirebaseEndpointUrl(rawUrl)
        if (endpoint.isBlank() || (!endpoint.contains("firebaseio.com", ignoreCase = true) && !endpoint.contains("firebasedatabase.app", ignoreCase = true))) {
            return@withContext Result.failure(
                IllegalArgumentException("A URL deve ser do Firebase Realtime Database (ex: https://seu-projeto-default-rtdb.firebaseio.com)")
            )
        }

        val body = jsonContent.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(endpoint)
            .put(body)
            .build()

        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return@withContext Result.success("Dados sincronizados com a nuvem com sucesso!")
                    } else {
                        val code = response.code
                        val errorBody = response.body?.string() ?: ""
                        val errorMsg = when (code) {
                            401, 403 -> "Acesso negado pelo Firebase. Verifique se as Regras do Realtime Database estão configuradas no modo de teste (.read: true, .write: true)."
                            404 -> "Banco de dados não encontrado no Firebase. Verifique a URL digitada."
                            429 -> "Limite de requisições excedido no Firebase. Tente novamente em alguns instantes."
                            else -> "Erro $code do Firebase: $errorBody"
                        }
                        // Non-recoverable auth or path errors don't need retry
                        if (code in listOf(401, 403, 404)) {
                            return@withContext Result.failure(Exception(errorMsg))
                        }
                        lastException = Exception(errorMsg)
                    }
                }
            } catch (e: IOException) {
                lastException = when (e) {
                    is java.net.UnknownHostException -> Exception("Sem conexão com a internet ou endereço do Firebase incorreto.")
                    is java.net.SocketTimeoutException -> Exception("Tempo limite esgotado (Tentativa $attempt/$maxRetries).")
                    else -> Exception("Falha de conexão (${e.localizedMessage ?: "rede indisponível"}). Tentativa $attempt/$maxRetries.")
                }
            } catch (e: Exception) {
                lastException = e
            }

            if (attempt < maxRetries) {
                delay(1000L * attempt) // Exponential-like backoff (1s, 2s)
            }
        }

        Result.failure(lastException ?: Exception("Falha ao sincronizar com o Firebase após $maxRetries tentativas."))
    }

    /**
     * Downloads the stock database JSON from Firebase Realtime Database using HTTP GET.
     */
    suspend fun downloadFromCloud(
        rawUrl: String,
        maxRetries: Int = 2
    ): Result<String> = withContext(Dispatchers.IO) {
        val endpoint = formatFirebaseEndpointUrl(rawUrl)
        if (endpoint.isBlank() || (!endpoint.contains("firebaseio.com", ignoreCase = true) && !endpoint.contains("firebasedatabase.app", ignoreCase = true))) {
            return@withContext Result.failure(
                IllegalArgumentException("URL inválida. Cole o link do Firebase Realtime Database.")
            )
        }

        val request = Request.Builder()
            .url(endpoint)
            .get()
            .build()

        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (bodyString.isNullOrBlank() || bodyString.trim() == "null") {
                            return@withContext Result.failure(Exception("Nenhum dado encontrado no Firebase nesta URL. Envie os dados primeiro antes de baixar."))
                        } else {
                            return@withContext Result.success(bodyString)
                        }
                    } else {
                        val code = response.code
                        val errorMsg = when (code) {
                            401, 403 -> "Acesso negado pelo Firebase. Verifique se ativou o modo de teste nas Regras do Realtime Database."
                            404 -> "Banco não encontrado no Firebase."
                            else -> "Erro $code ao buscar dados no Firebase."
                        }
                        if (code in listOf(401, 403, 404)) {
                            return@withContext Result.failure(Exception(errorMsg))
                        }
                        lastException = Exception(errorMsg)
                    }
                }
            } catch (e: IOException) {
                lastException = when (e) {
                    is java.net.UnknownHostException -> Exception("Sem conexão com a internet ou URL do Firebase inacessível.")
                    is java.net.SocketTimeoutException -> Exception("Tempo limite de conexão excedido.")
                    else -> Exception("Erro de rede ao baixar dados da nuvem.")
                }
            } catch (e: Exception) {
                lastException = e
            }

            if (attempt < maxRetries) {
                delay(1000L)
            }
        }

        Result.failure(lastException ?: Exception("Falha ao baixar dados da nuvem."))
    }

    /**
     * Performs a lightweight check against the Firebase endpoint.
     */
    suspend fun testConnection(rawUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val endpoint = formatFirebaseEndpointUrl(rawUrl, "health_check")
            if (endpoint.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("URL vazia"))
            }

            val request = Request.Builder()
                .url(endpoint)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else if (response.code == 401 || response.code == 403) {
                    Result.failure(Exception("Conexão estabelecida, porém o Firebase bloqueou o acesso. Ative o Modo de Teste nas Regras (.read: true, .write: true)."))
                } else {
                    Result.failure(Exception("Resposta inesperada do Firebase: Código ${response.code}"))
                }
            }
        } catch (e: Exception) {
            val msg = when (e) {
                is java.net.UnknownHostException -> "Sem conexão ou URL do Firebase inacessível."
                else -> e.message ?: "Erro ao testar conexão."
            }
            Result.failure(Exception(msg))
        }
    }
}
