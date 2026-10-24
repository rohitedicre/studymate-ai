package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        FolderEntity::class,
        StudyNoteEntity::class,
        FlashcardSetEntity::class,
        FlashcardItemEntity::class,
        QuizHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudyMateDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun folderDao(): FolderDao
    abstract fun studyNoteDao(): StudyNoteDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun quizHistoryDao(): QuizHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: StudyMateDatabase? = null

        fun getDatabase(context: Context): StudyMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyMateDatabase::class.java,
                    "studymate_db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
