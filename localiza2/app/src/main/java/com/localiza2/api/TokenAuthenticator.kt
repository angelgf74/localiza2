package com.localiza2.api

import com.google.gson.Gson
import com.localiza2.models.RefreshResponse
import com.localiza2.utils.SessionManager
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

// Se dispara automáticamente cuando el servidor responde 401. Usa un OkHttpClient propio,
// sin el interceptor de autenticación ni este mismo authenticator, para no entrar en bucle
// al llamar a /api/auth/refresh.
class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val baseUrl: String
) : Authenticator {

    private val plainClient = OkHttpClient()
    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) > 2) return null // Ya reintentamos una vez: no insistir.

        val refreshToken = sessionManager.getRefreshToken() ?: return null

        synchronized(this) {
            // Otra petición concurrente puede haber refrescado ya mientras esperábamos el lock.
            val currentToken = sessionManager.getToken()
            val tokenUsadoEnEstaPeticion = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (currentToken != null && currentToken != tokenUsadoEnEstaPeticion) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val body = """{"refreshToken":"$refreshToken"}"""
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("${baseUrl}api/auth/refresh")
                .post(body)
                .build()

            return try {
                plainClient.newCall(req).execute().use { resp ->
                    if (resp.code == 401) {
                        // Refresh token inválido/revocado/reutilizado: la sesión ya no es recuperable.
                        sessionManager.clearSession()
                        return null
                    }
                    if (!resp.isSuccessful) return null

                    val json = resp.body?.string() ?: return null
                    val data = gson.fromJson(json, RefreshResponse::class.java)
                    sessionManager.updateTokens(data.token, data.refreshToken)

                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${data.token}")
                        .build()
                }
            } catch (e: IOException) {
                null // Fallo de red: no tocar la sesión, se reintentará en la próxima petición.
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
