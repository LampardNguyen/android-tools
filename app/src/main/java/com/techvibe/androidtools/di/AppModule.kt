package com.techvibe.androidtools.di

import android.content.Context
import com.techvibe.androidtools.data.preferences.PreferencesHelper
import com.techvibe.androidtools.data.repository.BatteryRepositoryImpl
import com.techvibe.androidtools.domain.repository.BatteryRepository
import com.techvibe.androidtools.service.NotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing app-wide dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun providePreferencesHelper(@ApplicationContext context: Context): PreferencesHelper {
        return PreferencesHelper(context)
    }
    
    @Provides
    @Singleton
    fun provideBatteryRepository(
        @ApplicationContext context: Context,
        preferencesHelper: PreferencesHelper
    ): BatteryRepository {
        return BatteryRepositoryImpl(context, preferencesHelper)
    }
    
    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }
}
