package com.betnow.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CachedProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BetNowDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: BetNowDatabase? = null

        fun getDatabase(context: Context): BetNowDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BetNowDatabase::class.java,
                    "betnow.db"
                ).build().also { INSTANCE = it }
            }
    }
}
