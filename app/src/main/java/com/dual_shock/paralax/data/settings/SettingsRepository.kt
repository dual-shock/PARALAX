package com.dual_shock.paralax.data.settings

import android.content.Context
import kotlinx.coroutines.flow.Flow
import com.dual_shock.paralax.data.datastore.settingsDataStore
import com.dual_shock.paralax.model.settings.LaunchWindowLimits

class SettingsRepository(private val context: Context){

    val settings: Flow<LaunchWindowLimits> = context.settingsDataStore.data

    suspend fun updateData(newSettings: LaunchWindowLimits) {
        context.settingsDataStore.updateData { newSettings }
    }

}