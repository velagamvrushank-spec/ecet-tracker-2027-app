package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SyllabusTopicEntity::class,
        StudySessionEntity::class,
        DailyTaskEntity::class,
        McqSessionEntity::class,
        MockTestEntity::class,
        AiSummaryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class EcetDatabase : RoomDatabase() {
    abstract fun ecetDao(): EcetDao

    companion object {
        @Volatile
        private var INSTANCE: EcetDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_summaries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `sourceTitle` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `summaryMarkdown` TEXT NOT NULL,
                        `createdAtMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): EcetDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EcetDatabase::class.java,
                    "ecet_tracker_2027.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
