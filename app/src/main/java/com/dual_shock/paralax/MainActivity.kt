package com.dual_shock.paralax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import com.dual_shock.paralax.ui.dashboard.DashboardMode
import com.dual_shock.paralax.ui.dashboard.DashboardScreen
import com.dual_shock.paralax.ui.dashboard.DashboardScreenViewModel
import com.dual_shock.paralax.ui.forecast.ForecastScreen
import com.dual_shock.paralax.ui.locationselection.LocationSelectionScreen
import com.dual_shock.paralax.ui.map.HomeMapScreen
import com.dual_shock.paralax.ui.map.MapStyleViewModel
import com.dual_shock.paralax.ui.rocketconfigurations.RocketConfigurationsViewModel
import com.dual_shock.paralax.ui.settings.SettingsScreen
import com.dual_shock.paralax.ui.settings.SettingsViewModel
import com.dual_shock.paralax.ui.theme.ParalaxTheme
import com.dual_shock.paralax.ui.trajectory.TrajectoryMode
import com.dual_shock.paralax.ui.trajectory.TrajectoryScreen
import com.dual_shock.paralax.ui.trajectory.TrajectoryViewModel
import com.dual_shock.paralax.ui.trajectorysetup.TrajectorySetupScreen


object Screen {
    @Serializable
    object Dashboard
    @Serializable
    object MapScreen
    @Serializable
    data class LocationSelection(val editSiteId: Int? = null)
    @Serializable
    data class Settings(val location: String = "", val day: Int = 1, val hour: String = "")
    @Serializable
    data class Forecast72hr(val location: String = "", val day: Int = 1, val hour: String = "")
    @Serializable
    data class LaunchSiteDetail(val id: Int, val location: String = "", val day: Int = 1, val hour: String = "", val viabilityScore: Int = 0)
    @Serializable
    data class Trajectory(val id: Int, val location: String = "", val day: Int = 1, val hour: String = "", val viabilityScore: Int = 0)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ParalaxApplication
        val launchSiteRepository = app.launchSiteRepository
        val rocketViewModel by viewModels<RocketConfigurationsViewModel> {
            RocketConfigurationsViewModel.Companion.factory(app.rocketModelRepository)
        }
        val mapStyleViewModel by viewModels<MapStyleViewModel>()
        val settingsViewModel by viewModels<SettingsViewModel> {
            SettingsViewModel.Companion.Factory(app.settingsRepository)
        }
        val dashboardViewModel by viewModels<DashboardScreenViewModel> {
            DashboardScreenViewModel.Companion.Factory(
                repository = launchSiteRepository,
                settingsViewModel = settingsViewModel,
                hourRepository = app.hourRepository,
                initialMode = DashboardMode.Upcoming
            )
        }


        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            ParalaxTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Screen.Dashboard) {
                    composable<Screen.Dashboard> { backStackEntry ->
                        val selectSiteIndex =
                            backStackEntry.savedStateHandle.get<Int>("selectedSiteIndex")
                        LaunchedEffect(selectSiteIndex) {
                            if (selectSiteIndex != null) {
                                backStackEntry.savedStateHandle.remove<Int>("selectedSiteIndex")
                            }
                        }
                        DashboardScreen(
                            onNavigateToSettings = { loc, day, hour ->
                                navController.navigate(
                                    Screen.Settings(
                                        loc,
                                        day,
                                        hour
                                    )
                                )
                            },
                            onNavigateToForecast = { loc, day, hour ->
                                navController.navigate(
                                    Screen.Forecast72hr(
                                        loc,
                                        day,
                                        hour
                                    )
                                )
                            },
                            onNavigateToTrajectory = { id, loc, day, hour, viabilityScore ->
                                navController.navigate(
                                    Screen.Trajectory(
                                        id = id,
                                        location = loc,
                                        day = day,
                                        hour = hour,
                                        viabilityScore = viabilityScore
                                    )
                                )
                            },
                            onNavigateToMap = { navController.navigate(Screen.MapScreen) },
                            onNavigateToAddLocation = { navController.navigate(Screen.LocationSelection()) },
                            onNavigateToEditLocation = { siteId ->
                                navController.navigate(
                                    Screen.LocationSelection(
                                        editSiteId = siteId
                                    )
                                )
                            },
                            mapStyleViewModel = mapStyleViewModel,
                            selectSiteIndex = selectSiteIndex,
                            onSelectTimeWindow = { selectedDay, selectedHour ->
                                dashboardViewModel.selectDay(selectedDay)
                                dashboardViewModel.selectSpecificHour(selectedHour)
                            },
                            viewModel = dashboardViewModel,
                            onToggleTheme = { isDarkTheme = !isDarkTheme }
                        )
                    }
                    composable<Screen.MapScreen> {
                        HomeMapScreen(
                            repository = launchSiteRepository,
                            onNavigateToLaunchSites = { id ->
                                val sites = dashboardViewModel.uiState.value.launchSites
                                val index = sites.indexOfFirst { it.id == id }
                                if (index >= 0) {
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("selectedSiteIndex", index)
                                }
                                navController.popBackStack()
                            },
                            onNavigateBack = { navController.popBackStack() },
                            mapStyleViewModel = mapStyleViewModel
                        )
                    }
                    composable<Screen.LocationSelection> { backStackEntry ->
                        val args = backStackEntry.toRoute<Screen.LocationSelection>()
                        LocationSelectionScreen(
                            repository = launchSiteRepository,
                            interpolRepository = app.interpolRepository,
                            mapStyleViewModel = mapStyleViewModel,
                            editSiteId = args.editSiteId,
                            onNavigateBack = { navController.popBackStack() },
                            onLocationAdded = { index ->
                                navController.previousBackStackEntry?.savedStateHandle?.set(
                                    "selectedSiteIndex",
                                    index
                                )
                                navController.popBackStack()
                            }
                        )
                    }
                    composable<Screen.LaunchSiteDetail> { backStackEntry ->
                        val args = backStackEntry.toRoute<Screen.LaunchSiteDetail>()
                        TrajectorySetupScreen(
                            repository = launchSiteRepository,
                            rocketModelRepository = app.rocketModelRepository,
                            location = args.location,
                            day = args.day,
                            hour = args.hour,
                            viabilityScore = args.viabilityScore,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToTrajectory = { id ->
                                navController.navigate(
                                    Screen.Trajectory(
                                        id = id,
                                        location = args.location,
                                        day = args.day,
                                        hour = args.hour,
                                        viabilityScore = args.viabilityScore
                                    )
                                )
                            },
                            rocketConfigViewModel = rocketViewModel,
                            mapStyleViewModel = mapStyleViewModel
                        )
                    }
                    composable<Screen.Trajectory> { backStackEntry ->
                        val args = backStackEntry.toRoute<Screen.Trajectory>()
                        val trajectoryViewModel: TrajectoryViewModel = viewModel(
                            factory = TrajectoryViewModel.Companion.Factory(
                                repository = app.launchSiteRepository,
                                interpolRepository = app.interpolRepository,
                                rocketViewModel = rocketViewModel,
                                gribRepository = app.gribRepository,
                                initialMode = TrajectoryMode.Grib
                            )
                        )
                        TrajectoryScreen(
                            viewModel = trajectoryViewModel,
                            rocketConfigViewModel = rocketViewModel,
                            settingsViewModel = settingsViewModel,
                            mapStyleViewModel = mapStyleViewModel,
                            onNavigateBack = {
                                navController.popBackStack(
                                    Screen.Dashboard,
                                    inclusive = false
                                )
                            },
                            onNavigateToSetup = {
                                navController.navigate(
                                    Screen.LaunchSiteDetail(
                                        id = args.id,
                                        location = args.location,
                                        day = args.day,
                                        hour = args.hour,
                                        viabilityScore = args.viabilityScore
                                    )
                                )
                            }
                        )
                    }
                    composable<Screen.Settings> { backStackEntry ->
                        val args = backStackEntry.toRoute<Screen.Settings>()
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onToggleTheme = { isDarkTheme = !isDarkTheme }
                        )
                    }
                    composable<Screen.Forecast72hr> { backStackEntry ->
                        val args = backStackEntry.toRoute<Screen.Forecast72hr>()
                        ForecastScreen(
                            location = args.location,
                            day = args.day,
                            hour = args.hour,
                            onNavigateBack = { navController.popBackStack() },
                            onSelectTimeWindow = { selectedDay, selectedHour ->
                                navController.popBackStack()
                                dashboardViewModel.selectDay(selectedDay)
                                dashboardViewModel.selectSpecificHour(selectedHour)
                            },
                            dashboardViewModel = dashboardViewModel
                        )
                    }
                }
            }
        }
    }
}