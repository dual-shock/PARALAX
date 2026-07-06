package com.dual_shock.paralax

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.dual_shock.paralax.data.rocketmodels.RocketConfigDao
import com.dual_shock.paralax.data.rocketmodels.RocketModel
import com.dual_shock.paralax.data.rocketmodels.RocketModelRepository
import com.dual_shock.paralax.ui.rocketconfigurations.RocketConfigurationsViewModel
import org.junit.Assert.*
import org.junit.Test

class FakeRocketConfigDao : RocketConfigDao {
    private val models = mutableListOf<RocketModel>()

    override fun getAllConfigs(): Flow<List<RocketModel>> = flow { emit(models.toList()) }

    override fun getConfigByName(name: String): RocketModel = models.first { it.name == name }

    override suspend fun deleteConfig(config: RocketModel) { models.remove(config) }

    override suspend fun insertConfig(config: RocketModel) { models.add(config) }

    override suspend fun deleteByName(oldName: String) { models.removeAll { it.name == oldName } }
}

class RocketConfigViewModelTest {

    private fun createViewModel(): RocketConfigurationsViewModel {
        val fakeDao = FakeRocketConfigDao()
        val repository = RocketModelRepository(fakeDao)
        return RocketConfigurationsViewModel(repository)
    }

    @Test
    fun negativeValue() {
        val viewmodel = createViewModel()
        viewmodel.updateDryMass("-1")
        assertFalse(viewmodel.uiState.value.isValid)
    }

    @Test
    fun emptyValue() {
        val viewmodel = createViewModel()
        viewmodel.updateDryMass("")
        assertFalse(viewmodel.uiState.value.isValid)
    }

    @Test
    fun invalidValue() {
        val viewmodel = createViewModel()
        viewmodel.updateDryMass("zzzz")
        assertFalse(viewmodel.uiState.value.isValid)
    }

    @Test
    fun WronglaunchAngle() {
        val viewmodel = createViewModel()
        viewmodel.updateLaunchAngle("95")
        assertFalse(viewmodel.uiState.value.isValid)
    }
}