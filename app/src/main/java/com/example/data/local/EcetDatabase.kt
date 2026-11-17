package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SyllabusTopicEntity::class,
        StudySessionEntity::class,
        DailyTaskEntity::class,
        McqSessionEntity::class,
        MockTestEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EcetDatabase : RoomDatabase() {
    abstract fun ecetDao(): EcetDao

    companion object {
        @Volatile
        private var INSTANCE: EcetDatabase? = null

        fun getInstance(context: Context): EcetDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EcetDatabase::class.java,
                    "ecet_tracker_2027.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
