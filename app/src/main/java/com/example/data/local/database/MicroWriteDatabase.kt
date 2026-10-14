package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.PracticeSessionDao
import com.example.data.local.dao.SectionDao
import com.example.data.local.dao.SectionProgressDao
import com.example.data.local.dao.SelfReviewDao
import com.example.data.local.dao.TopicDao
import com.example.data.local.dao.TopicProgressDao
import com.example.data.local.entity.PracticeSessionEntity
import com.example.data.local.entity.SectionEntity
import com.example.data.local.entity.SectionProgressEntity
import com.example.data.local.entity.SelfReviewEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.TopicProgressEntity

@Database(
    entities = [
        TopicEntity::class,
        SectionEntity::class,
        TopicProgressEntity::class,
        SectionProgressEntity::class,
        PracticeSessionEntity::class,
        SelfReviewEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MicroWriteDatabase : RoomDatabase() {
    abstract fun topicDao(): TopicDao
    abstract fun sectionDao(): SectionDao
    abstract fun topicProgressDao(): TopicProgressDao
    abstract fun sectionProgressDao(): SectionProgressDao
    abstract fun practiceSessionDao(): PracticeSessionDao
    abstract fun selfReviewDao(): SelfReviewDao

    companion object {
        @Volatile
        private var INSTANCE: MicroWriteDatabase? = null

        fun getDatabase(context: Context): MicroWriteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MicroWriteDatabase::class.java,
                    "microwrite_database.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
