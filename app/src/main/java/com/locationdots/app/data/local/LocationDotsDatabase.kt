package com.locationdots.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LocationEntity::class, PlaceEntity::class, TimelineEventEntity::class],
    version = 4,
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE timeline_events ADD COLUMN journeyMode TEXT"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE timeline_events ADD COLUMN pathEncoded TEXT")
            }
        }

        fun create(context: Context): LocationDotsDatabase =
            Room.databaseBuilder(
                context,
                LocationDotsDatabase::class.java,
                "location_dots.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
