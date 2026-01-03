package com.nerdpace.statusholder.di



// App Module

import android.content.Context
import androidx.room.Room
import com.whatsappstatussaver.data.local.StatusDatabase
import com.whatsappstatussaver.data.local.dao.StatusMediaDao
import com.whatsappstatussaver.data.repository.StatusRepositoryImpl
import com.whatsappstatussaver.domain.repository.StatusRepository
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
    fun provideStatusDatabase(
        @ApplicationContext context: Context
    ): StatusDatabase {
        return Room.databaseBuilder(
            context,
            StatusDatabase::class.java,
            "status_saver_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideStatusMediaDao(database: StatusDatabase): StatusMediaDao {
        return database.statusMediaDao()
    }

    @Provides
    @Singleton
    fun provideStatusRepository(
        @ApplicationContext context: Context,
        dao: StatusMediaDao
    ): StatusRepository {
        return StatusRepositoryImpl(context, dao)
    }
}