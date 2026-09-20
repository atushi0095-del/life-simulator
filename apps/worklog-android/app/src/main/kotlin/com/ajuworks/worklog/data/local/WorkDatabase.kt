package com.ajuworks.worklog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WorkSessionEntity::class, BreakSessionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WorkDatabase : RoomDatabase() {

    abstract fun workDao(): WorkDao

    companion object {
        private const val NAME = "worklog.db"

        @Volatile
        private var instance: WorkDatabase? = null

        fun get(context: Context): WorkDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): WorkDatabase =
            Room.databaseBuilder(context, WorkDatabase::class.java, NAME)
                // Foreign keys are declared on the entities; Room enables them
                // per-connection, and the cascade is what keeps breaks from
                // outliving their session.
                .build()
    }
}
