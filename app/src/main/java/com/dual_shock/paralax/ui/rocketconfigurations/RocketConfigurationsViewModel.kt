package com.dual_shock.paralax.ui.rocketconfigurations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dual_shock.paralax.data.rocketmodels.RocketModel
import com.dual_shock.paralax.data.rocketmodels.RocketModelRepository
import kotlin.math.round

data class RocketConfig(
    val dryMass: Double,
    val fuelMass: Double,
    val thrust: Double,
    val burnTime: Double,
    val dragCoefficient: Double,
    val diameter: Double,
    val launchAngle: Double,
    val direction: Double,
    val firstChuteReleaseHeight: Int = 0,
    val firstChuteTerminalVelocity: Double = 20.0,
    val firstChuteAtApogee: Boolean = true,
    val secondChuteReleaseHeight: Int = 400,
    val secondChuteTerminalVelocity: Double = 5.0
)

data class RocketConfigState(
    val dryMass: String = "5.0",
    val fuelMass: String = "3.0",
    val thrust: String = "2000.0",
    val burnTime: String = "4.0",
    val dragCoefficient: String = "0.5",
    val diameter: String = "0.1",
    val launchAngle: String = "85.0",
    val direction: String = "300.0",
    val firstChuteReleaseHeight: String = "0",
    val firstChuteTerminalVelocity: String = "20.0",
    val firstChuteAtApogee: Boolean = true,
    val secondChuteReleaseHeight: String = "400",
    val secondChuteTerminalVelocity: String = "5.0",
    val isValid: Boolean = true,
    val savedModels: List<RocketModel> = emptyList(),
    val selectedModel: RocketModel? = null,
    val showModelList: Boolean = false
)

fun RocketConfigState.toDomain(): RocketConfig {
    return RocketConfig(
        dryMass = dryMass.toDouble(),
        fuelMass = fuelMass.toDouble(),
        thrust = thrust.toDouble(),
        burnTime = burnTime.toDouble(),
        dragCoefficient = dragCoefficient.toDouble(),
        diameter = diameter.toDouble(),
        launchAngle = launchAngle.toDouble(),
        direction = direction.toDouble(),
        firstChuteReleaseHeight = firstChuteReleaseHeight.toIntOrNull() ?: 0,
        firstChuteTerminalVelocity = firstChuteTerminalVelocity.toDoubleOrNull() ?: 0.0,
        firstChuteAtApogee = firstChuteAtApogee,
        secondChuteReleaseHeight = secondChuteReleaseHeight.toIntOrNull() ?: 0,
        secondChuteTerminalVelocity = secondChuteTerminalVelocity.toDoubleOrNull() ?: 0.0
    )
}

class RocketConfigurationsViewModel(
    private val rocketModelRepository: RocketModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RocketConfigState())
    val uiState: StateFlow<RocketConfigState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            rocketModelRepository.getRocketModels().collect { models ->
                if (models.isEmpty()) {
                    rocketModelRepository.insert(DEFAULT_MODEL)
                } else if (_uiState.value.selectedModel == null) {
                    val firstModel = models.firstOrNull()
                    if (firstModel != null) {
                        selectModel(firstModel)
                    }
                }
                _uiState.update { it.copy(savedModels = models) }
            }
        }
    }

    fun toggleModelList() {
        _uiState.update { it.copy(showModelList = !it.showModelList) }
    }

    fun selectModel(model: RocketModel) {
        _uiState.update {
            it.copy(
                selectedModel = model,
                showModelList = false,
                dryMass = model.dryMass.toString(),
                fuelMass = model.fuelMass.toString(),
                thrust = model.thrust.toString(),
                burnTime = model.burnTime.toString(),
                dragCoefficient = model.dragCoefficient.toString(),
                diameter = model.diameter.toString(),
                launchAngle = model.launchAngle.toString(),
                direction = model.direction.toString(),
                firstChuteReleaseHeight = model.firstChuteReleaseHeight.toString(),
                firstChuteTerminalVelocity = model.firstChuteTerminalVelocity.toString(),
                firstChuteAtApogee = model.firstChuteAtApogee,
                secondChuteReleaseHeight = model.secondChuteReleaseHeight.toString(),
                secondChuteTerminalVelocity = model.secondChuteTerminalVelocity.toString()
            )
        }
    }

    fun addModel(name: String) {
        val s = _uiState.value
        val model = RocketModel(
            name = name,
            dryMass = s.dryMass.toDouble(),
            fuelMass = s.fuelMass.toDouble(),
            thrust = s.thrust.toDouble(),
            burnTime = s.burnTime.toDouble(),
            dragCoefficient = s.dragCoefficient.toDouble(),
            diameter = s.diameter.toDouble(),
            launchAngle = s.launchAngle.toDouble(),
            direction = s.direction.toDouble(),
            firstChuteReleaseHeight = s.firstChuteReleaseHeight.toIntOrNull() ?: 0,
            firstChuteTerminalVelocity = s.firstChuteTerminalVelocity.toDoubleOrNull() ?: 0.0,
            firstChuteAtApogee = s.firstChuteAtApogee,
            secondChuteReleaseHeight = s.secondChuteReleaseHeight.toIntOrNull() ?: 0,
            secondChuteTerminalVelocity = s.secondChuteTerminalVelocity.toDoubleOrNull() ?: 0.0
        )
        viewModelScope.launch { rocketModelRepository.insert(model) }
    }
    fun deleteModel(model: RocketModel) {
        viewModelScope.launch { rocketModelRepository.delete(model) }
    }

    fun updateModel(oldName: String, newName: String) {
        val s = _uiState.value
        val model = RocketModel(
            name = newName,
            dryMass = s.dryMass.toDouble(),
            fuelMass = s.fuelMass.toDouble(),
            thrust = s.thrust.toDouble(),
            burnTime = s.burnTime.toDouble(),
            dragCoefficient = s.dragCoefficient.toDouble(),
            diameter = s.diameter.toDouble(),
            launchAngle = s.launchAngle.toDouble(),
            direction = s.direction.toDouble(),
            firstChuteReleaseHeight = s.firstChuteReleaseHeight.toIntOrNull() ?: 0,
            firstChuteTerminalVelocity = s.firstChuteTerminalVelocity.toDoubleOrNull() ?: 0.0,
            firstChuteAtApogee = s.firstChuteAtApogee,
            secondChuteReleaseHeight = s.secondChuteReleaseHeight.toIntOrNull() ?: 0,
            secondChuteTerminalVelocity = s.secondChuteTerminalVelocity.toDoubleOrNull() ?: 0.0
        )
        viewModelScope.launch {
            rocketModelRepository.updateModel(oldName, model)
            _uiState.update { it.copy(selectedModel = model) }
        }
    }

    private fun sanitize(value: String) = value.replace(',', '.')

    private fun saveCurrentToSelectedModel() {
        val selected = _uiState.value.selectedModel ?: return
        val s = _uiState.value
        val updated = RocketModel(
            name = selected.name,
            dryMass = s.dryMass.toDoubleOrNull() ?: selected.dryMass,
            fuelMass = s.fuelMass.toDoubleOrNull() ?: selected.fuelMass,
            thrust = s.thrust.toDoubleOrNull() ?: selected.thrust,
            burnTime = s.burnTime.toDoubleOrNull() ?: selected.burnTime,
            dragCoefficient = s.dragCoefficient.toDoubleOrNull() ?: selected.dragCoefficient,
            diameter = s.diameter.toDoubleOrNull() ?: selected.diameter,
            launchAngle = s.launchAngle.toDoubleOrNull() ?: selected.launchAngle,
            direction = s.direction.toDoubleOrNull() ?: selected.direction,
            firstChuteReleaseHeight = s.firstChuteReleaseHeight.toIntOrNull()
                ?: selected.firstChuteReleaseHeight,
            firstChuteTerminalVelocity = s.firstChuteTerminalVelocity.toDoubleOrNull()
                ?: selected.firstChuteTerminalVelocity,
            firstChuteAtApogee = s.firstChuteAtApogee,
            secondChuteReleaseHeight = s.secondChuteReleaseHeight.toIntOrNull()
                ?: selected.secondChuteReleaseHeight,
            secondChuteTerminalVelocity = s.secondChuteTerminalVelocity.toDoubleOrNull()
                ?: selected.secondChuteTerminalVelocity
        )
        viewModelScope.launch {
            rocketModelRepository.insert(updated)
            _uiState.update { it.copy(selectedModel = updated) }
        }
    }

    fun updateDryMass(value: String) {
        val updated = _uiState.value.copy(dryMass = sanitize(value))
        _uiState.value = updated.copy(isValid = validate(updated))
    }
    fun updateFuelMass(value: String) { _uiState.update { it.copy(fuelMass = sanitize(value)) } }
    fun updateThrust(value: String) { _uiState.update { it.copy(thrust = sanitize(value)) } }
    fun updateBurnTime(value: String) { _uiState.update { it.copy(burnTime = sanitize(value)) } }
    fun updateDragCoefficient(value: String) { _uiState.update { it.copy(dragCoefficient = sanitize(value)) } }
    fun updateDiameter(value: String) { _uiState.update { it.copy(diameter = sanitize(value)) } }
    fun updateDirection(value: String) {
        val rounded = sanitize(value).toFloatOrNull()?.let {
            round(it).toInt().toString() + ".0"
        } ?: sanitize(value)
        _uiState.update { it.copy(direction = rounded) }
        saveCurrentToSelectedModel()
    }
    fun updateFirstChuteReleaseHeight(value: String) { _uiState.update { it.copy(firstChuteReleaseHeight = value) } }
    fun updateFirstChuteTerminalVelocity(value: String) { _uiState.update { it.copy(firstChuteTerminalVelocity = sanitize(value)) } }
    fun toggleFirstChuteAtApogee() { _uiState.update { it.copy(firstChuteAtApogee = !it.firstChuteAtApogee) } }
    fun updateSecondChuteReleaseHeight(value: String) { _uiState.update { it.copy(secondChuteReleaseHeight = value) } }
    fun updateSecondChuteTerminalVelocity(value: String) { _uiState.update { it.copy(secondChuteTerminalVelocity = sanitize(value)) } }

    fun updateLaunchAngle(value: String) {
        val updated = _uiState.value.copy(launchAngle = sanitize(value))
        _uiState.value = updated.copy(isValid = validate(updated))
        saveCurrentToSelectedModel()
    }

    private fun validate(state: RocketConfigState): Boolean {
        val mass = state.dryMass.toDoubleOrNull()
        val angle = state.launchAngle.toDoubleOrNull()
        return mass != null && mass > 0 && angle != null && angle in 0.0..90.0
    }

    fun isConfigComplete(): Boolean {
        val s = _uiState.value
        val positiveFields = listOf(s.dryMass, s.fuelMass, s.thrust, s.burnTime, s.dragCoefficient, s.diameter, s.launchAngle)
        val positiveValid = positiveFields.all { it.toDoubleOrNull()?.let { d -> d > 0 } == true }
        val directionValid = s.direction.toDoubleOrNull()?.let { d -> d in 0.0..360.0 } == true
        return positiveValid && directionValid
    }

    companion object {
        private val DEFAULT_MODEL = RocketModel(
            name = "Example Rocket",
            dryMass = 5.0,
            fuelMass = 3.0,
            thrust = 2000.0,
            burnTime = 4.0,
            dragCoefficient = 0.5,
            diameter = 0.1,
            launchAngle = 85.0,
            direction = 300.0,
            firstChuteReleaseHeight = 0,
            firstChuteTerminalVelocity = 20.0,
            firstChuteAtApogee = true,
            secondChuteReleaseHeight = 400,
            secondChuteTerminalVelocity = 5.0
        )

        fun factory(repository: RocketModelRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { RocketConfigurationsViewModel(repository) }
            }
    }
}