package com.example.stocktracker.di


import android.app.Application
import androidx.room.Room
import com.example.stocktracker.BuildConfig
import com.example.stocktracker.data.local.StockDatabase
import com.example.stocktracker.data.remote.api.AlphaVantageApi
import com.example.stocktracker.domain.repository.StockRepository
import com.stocktracker.app.data.repository.StockRepositoryImpl

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAlphaVantageApi(client: OkHttpClient): AlphaVantageApi {
        return Retrofit.Builder()
            .baseUrl(AlphaVantageApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AlphaVantageApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStockDatabase(app: Application): StockDatabase {
        return Room.databaseBuilder(
            app,
            StockDatabase::class.java,
            "stock_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideStockDao(db: StockDatabase) = db.stockDao()

    @Provides
    @Singleton
    fun provideAlertDao(db: StockDatabase) = db.alertDao()

    @Provides
    @Singleton
    fun provideNewsDao(db: StockDatabase) = db.newsDao()

    @Provides
    @Singleton
    fun provideStockRepository(
        api: AlphaVantageApi,
        db: StockDatabase
    ): StockRepository {
        return StockRepositoryImpl(
            api = api,
            stockDao = db.stockDao(),
            newsDao = db.newsDao(),
            alertDao = db.alertDao()
        )
    }
}