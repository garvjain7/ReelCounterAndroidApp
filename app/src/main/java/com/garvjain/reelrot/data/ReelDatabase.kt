package com.garvjain.reelrot.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ReelEvent::class], version = 1, exportSchema = false)
abstract class ReelDatabase : RoomDatabase() {
    abstract fun reelDao(): ReelDao

    companion object {
        @Volatile
        private var INSTANCE: ReelDatabase? = null

        fun getDatabase(context: Context): ReelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReelDatabase::class.java,
                    "reel_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
