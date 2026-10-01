package com.locationdots.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LocationEntity::class, PlaceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocationDotsDatabase : RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun placeDao(): PlaceDao

    companion object {
        fun create(context: Context): LocationDotsDatabase =
            Room.databaseBuilder(
                context,
                LocationDotsDatabase::class.java,
                "location_dots.db"
            ).build()
    }
}
