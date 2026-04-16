package com.example.skill2career.network

import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    /**
     *  SERVER CONFIGURATION:
     *  1. If you are using the ANDROID EMULATOR: Use "http://10.0.2.2:8080/"
     *  2. If you are using a PHYSICAL DEVICE:
     *     - Your phone and laptop MUST be on the same WiFi.
     *     - Find your laptop's IP (Run 'ipconfig' in CMD and look for IPv4 Address).
     *     - Use "http://YOUR_IP_HERE:8080/"
     */
    private const val BASE_URL = "http://192.168.137.198:8080/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // In-memory token store. In production you should persist this (EncryptedSharedPreferences / DataStore).
    @Volatile
    private var authToken: String? = null

    fun setAuthToken(token: String?) {
        authToken = token
    }

    private val authInterceptor = Interceptor { chain ->
        val token = authToken
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
    
    fun getBaseUrl(): String = BASE_URL
}
