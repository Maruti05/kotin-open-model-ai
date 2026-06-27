package com.vedica.labs.ind.app.chat.openmodels.di

import android.content.Context
import com.vedica.labs.ind.app.chat.openmodels.data.local.AppDatabase
import com.vedica.labs.ind.app.chat.openmodels.data.local.dao.BenchmarkDao
import com.vedica.labs.ind.app.chat.openmodels.data.local.dao.ChatMessageDao
import com.vedica.labs.ind.app.chat.openmodels.data.local.dao.ChatSessionDao
import com.vedica.labs.ind.app.chat.openmodels.data.local.dao.FileContextDao
import com.vedica.labs.ind.app.chat.openmodels.data.local.preferences.AppPreferences
import com.vedica.labs.ind.app.chat.openmodels.domain.benchmark.BenchmarkRunner
import com.vedica.labs.ind.app.chat.openmodels.domain.download.ModelDownloader
import com.vedica.labs.ind.app.chat.openmodels.domain.inference.GGUFInferenceEngine
import com.vedica.labs.ind.app.chat.openmodels.domain.inference.LiteRTInferenceEngine
import com.vedica.labs.ind.app.chat.openmodels.domain.inference.SimulatedInferenceEngine
import com.vedica.labs.ind.app.chat.openmodels.domain.parser.LlmOutputParser
import com.vedica.labs.ind.app.chat.openmodels.domain.util.HardwareChecker
import com.vedica.labs.ind.app.chat.openmodels.domain.util.PromptTemplateService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    fun provideChatSessionDao(db: AppDatabase): ChatSessionDao = db.chatSessionDao()

    @Provides
    fun provideChatMessageDao(db: AppDatabase): ChatMessageDao = db.chatMessageDao()

    @Provides
    fun provideBenchmarkDao(db: AppDatabase): BenchmarkDao = db.benchmarkDao()

    @Provides
    fun provideFileContextDao(db: AppDatabase): FileContextDao = db.fileContextDao()

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences =
        AppPreferences(context)

    @Provides
    @Singleton
    fun provideModelDownloader(): ModelDownloader = ModelDownloader()

    @Provides
    @Singleton
    fun provideBenchmarkRunner(): BenchmarkRunner = BenchmarkRunner()

    @Provides
    @Singleton
    fun provideHardwareChecker(): HardwareChecker = HardwareChecker()

    @Provides
    @Singleton
    fun providePromptTemplateService(): PromptTemplateService = PromptTemplateService()

    @Provides
    @Singleton
    fun provideLlmOutputParser(): LlmOutputParser = LlmOutputParser()

    @Provides
    @Singleton
    fun provideSimulatedInferenceEngine(): SimulatedInferenceEngine =
        SimulatedInferenceEngine()

    @Provides
    @Singleton
    fun provideGGUFInferenceEngine(): GGUFInferenceEngine =
        GGUFInferenceEngine()

    @Provides
    @Singleton
    fun provideLiteRTInferenceEngine(): LiteRTInferenceEngine =
        LiteRTInferenceEngine()
}
