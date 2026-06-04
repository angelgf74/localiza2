package com.localiza2.api

import com.localiza2.models.SuggestionDto
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface SuggestionsApiService {

    @POST("api/Sugerencias")
    suspend fun sendSuggestion(@Body dto: SuggestionDto): Response<Unit>

    companion object {
        private const val BASE_URL = "https://angelgf.com.es/gestorsugerenciasapi/"

        fun create(): SuggestionsApiService =
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SuggestionsApiService::class.java)
    }
}
