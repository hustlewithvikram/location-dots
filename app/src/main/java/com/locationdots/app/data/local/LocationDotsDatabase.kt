package com.locationdots.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LocationEntity::class, PlaceEntity::class, TimelineEventEntity::class],
    version = 2,
    exportSchema = false
)
abstract class LocationDotsDatabase : RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun placeDao(): PlaceDao
    abstract fun timelineEventDao(): TimelineEventDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS timeline_events (
                        id TEXT NOT NULL,
                        type TEXT NOT NULL,
                        timestampEpochMillis INTEGER NOT NULL,
                        placeId TEXT,
                        arrivalEpochMillis INTEGER,
                        departureEpochMillis INTEGER,
                        startPlaceId TEXT,
                        endPlaceId TEXT,
                        startedAtEpochMillis INTEGER,
                        endedAtEpochMillis INTEGER,
                        distanceMeters REAL,
                        PRIMARY KEY(id)
                    )
                """)
            }
        }

        fun create(context: Context): LocationDotsDatabase =
            Room.databaseBuilder(
                context,
                LocationDotsDatabase::class.java,
                "location_dots.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
