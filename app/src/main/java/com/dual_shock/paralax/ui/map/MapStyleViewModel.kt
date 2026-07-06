package com.dual_shock.paralax.ui.map

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MapStyleOption(val label: String, val preset: String)

val MAP_STYLES = listOf(
    MapStyleOption("Dawn", "dawn"),
    MapStyleOption("Day", "day"),
    MapStyleOption("Dusk", "dusk"),
    MapStyleOption("Night", "night"),
)

class MapStyleViewModel : ViewModel() {
    private val _selectedStyle = MutableStateFlow(MAP_STYLES[0])
    val selectedStyle: StateFlow<MapStyleOption> = _selectedStyle.asStateFlow()

    fun setStyle(style: MapStyleOption) {
        _selectedStyle.value = style
    }
}
