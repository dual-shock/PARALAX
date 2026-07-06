package com.dual_shock.paralax.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dual_shock.paralax.data.interpol.InterpolDao
import com.dual_shock.paralax.data.interpol.InterpolLocation
import com.dual_shock.paralax.data.launchsites.LaunchSite
import com.dual_shock.paralax.data.launchsites.LaunchSiteDao
import com.dual_shock.paralax.data.rocketmodels.RocketConfigDao
import com.dual_shock.paralax.data.rocketmodels.RocketModel

@Database(entities = [LaunchSite::class, InterpolLocation::class, RocketModel::class], version = 6, exportSchema = false)
abstract class DB : RoomDatabase() {
    abstract fun LaunchSiteDao(): LaunchSiteDao
    abstract fun InterpolDao(): InterpolDao

    abstract fun RocketConfigDao(): RocketConfigDao
}