package com.dual_shock.paralax.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.dual_shock.paralax.model.settings.LaunchWindowLimits

val Context.settingsDataStore: DataStore<LaunchWindowLimits> by dataStore(
    fileName = "launch_window_limits.json",
    serializer = LaunchWindowLimitsSerializer
)
