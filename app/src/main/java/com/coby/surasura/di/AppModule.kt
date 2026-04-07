package com.coby.surasura.di

import com.coby.surasura.data.api.GoogleSpeechApi
import com.coby.surasura.data.api.GoogleTTSApi
import com.coby.surasura.data.api.GoogleTranslationApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt DI 모듈
 * - Retrofit 인스턴스 3개 (Speech, Translation, TTS)
 * - OkHttpClient 공유
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Google Speech-to-Text API
    // POST https://speech.googleapis.com/v1/speech:recognize
    // ──────────────────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("speech")
    fun provideSpeechRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://speech.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGoogleSpeechApi(@Named("speech") retrofit: Retrofit): GoogleSpeechApi {
        return retrofit.create(GoogleSpeechApi::class.java)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Google Translation API
    // POST https://translation.googleapis.com/language/translate/v2
    // ──────────────────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("translation")
    fun provideTranslationRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://translation.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGoogleTranslationApi(@Named("translation") retrofit: Retrofit): GoogleTranslationApi {
        return retrofit.create(GoogleTranslationApi::class.java)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Google Text-to-Speech API
    // POST https://texttospeech.googleapis.com/v1/text:synthesize
    // ──────────────────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("tts")
    fun provideTTSRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://texttospeech.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGoogleTTSApi(@Named("tts") retrofit: Retrofit): GoogleTTSApi {
        return retrofit.create(GoogleTTSApi::class.java)
    }
}
