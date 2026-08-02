package com.dual_shock.paralax

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.dual_shock.paralax.data.DB
import com.dual_shock.paralax.data.frost.FrostRepository
import com.dual_shock.paralax.data.grib.GribDataSource
import com.dual_shock.paralax.data.grib.GribRepository
import com.dual_shock.paralax.data.hour.HourRepository
import com.dual_shock.paralax.data.interpol.InterpolDataSource
import com.dual_shock.paralax.data.interpol.InterpolRepository
import com.dual_shock.paralax.data.launchsites.EXAMPLE_LAUNCH_SITES
import com.dual_shock.paralax.data.launchsites.LaunchSiteRepository
import com.dual_shock.paralax.data.locationforecast.LocationForecastDataSource
import com.dual_shock.paralax.data.locationforecast.LocationForecastRepository
import com.dual_shock.paralax.data.rocketmodels.RocketModelRepository
import com.dual_shock.paralax.data.settings.SettingsRepository


class ParalaxApplication : Application() {

    val database: DB by lazy {
        Room.databaseBuilder(
            applicationContext,
            DB::class.java,
            "app-database-v2"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    println("DB: onCreate fired")
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = database.LaunchSiteDao()
                        EXAMPLE_LAUNCH_SITES.forEach {
                            dao.insert(it)
                            println("DB: Inserted site: ${it.name}")
                        }
                    }
                }
            })
            // !!! DENNE DELEN FETCHER EKSEMPEL DATAEN, FJERN NÅR VI IKKE SKAL VISE EKSEMPLER

            .build()
    }

    val locationForecastRepo by lazy {
        LocationForecastRepository(LocationForecastDataSource(cacheDir))
    }

    val gribRepository by lazy {
        GribRepository(
            dataSource = GribDataSource(cacheDir),
            locationForecastRepo = locationForecastRepo
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(applicationContext)
    }

    val interpolDataSource: InterpolDataSource by lazy { InterpolDataSource(cacheDir) }

    val launchSiteRepository by lazy {
        LaunchSiteRepository(database.LaunchSiteDao())
    }

    val rocketModelRepository by lazy {
        RocketModelRepository(database.RocketConfigDao())
    }
    val interpolRepository by lazy {
        InterpolRepository(interpolDataSource, database.InterpolDao())
    }

    val frostRepository by lazy { FrostRepository() }

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val hourRepository by lazy {
        HourRepository(
            locationForecastRepo = locationForecastRepo,
            interpolRepo = interpolRepository,
            gribRepo = gribRepository,
            settingsRepo = settingsRepository,
            frostRepo = frostRepository,
            context = applicationContext,
            externalScope = appScope
        )
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        CoroutineScope(Dispatchers.IO).launch {
            launch {
                runCatching { gribRepository.initialize() }
                    .onFailure { println("Grib fetch failed ${it}") }
            }
            launch {
                runCatching { interpolRepository.getLocations() }
                    .onFailure { println("InterpolInit failed ${it}") }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "watched_hour_changes",
                "Watched Hour Changes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when watched launch windows change"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
