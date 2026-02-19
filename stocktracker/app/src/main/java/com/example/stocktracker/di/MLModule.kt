package com.example.stocktracker.di

import android.content.Context
import com.example.stocktracker.data.ml.MLRepository
import com.example.stocktracker.data.ml.MLRepositoryImpl
import com.example.stocktracker.data.remote.api.MLApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MLModule {

    // ============================================
    // CHOOSE YOUR BACKEND URL HERE
    // ============================================

    // Option A: Android Emulator → VS Code on same computer (MOST COMMON)
    private const val ML_BASE_URL = "http://10.0.2.2:8000/"

    // Option B: Physical Android Device → Computer (replace with your computer's IP)
    // private const val ML_BASE_URL = "http://192.168.1.5:8000/"

    // Option C: Deployed backend (Render, Railway, AWS, etc.)
    // private const val ML_BASE_URL = "https://your-app.onrender.com/"

    // Option D: Localhost for testing (won't work on emulator, only for unit tests)
    // private const val ML_BASE_URL = "http://localhost:8000/"

    @Provides
    @Singleton
    @Named("mlOkHttpClient")
    fun provideMLOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("mlRetrofit")
    fun provideMLRetrofit(@Named("mlOkHttpClient") client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ML_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMLApiService(@Named("mlRetrofit") retrofit: Retrofit): MLApiService {
        return retrofit.create(MLApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMLRepository(
        @ApplicationContext context: Context,
        mlApiService: MLApiService
    ): MLRepository {
        return MLRepositoryImpl(context, mlApiService)
    }
}