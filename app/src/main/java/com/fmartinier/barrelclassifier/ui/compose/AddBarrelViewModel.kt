package com.fmartinier.barrelclassifier.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.service.AnalyticsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AddBarrelUiState(
    val barrelId: Long? = null,
    val barrelName: String = "",
    val volume: String = "",
    val brand: String = "",
    val woodType: String = "",
    val heatType: String = "",
    val humidity: String = "",
    val temperature: String = "",
    val description: String = "",
    val showAdvanced: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed class AddBarrelEvent {
    data class ShowError(val messageId: Int) : AddBarrelEvent()
    data class ShowSuccess(val messageId: Int) : AddBarrelEvent()
    object Dismiss : AddBarrelEvent()
}

class AddBarrelViewModel(
    dbHelper: DatabaseHelper,
) : ViewModel() {

    private val barrelDao: BarrelDao = BarrelDao.getInstance(dbHelper)

    private val _uiState = MutableStateFlow(AddBarrelUiState())
    val uiState: StateFlow<AddBarrelUiState> = _uiState.asStateFlow()

    private var currentBarrel: com.fmartinier.barrelclassifier.data.model.Barrel? = null
    private var isModificationMode = false

    fun initializeWithBarrel(barrel: com.fmartinier.barrelclassifier.data.model.Barrel?) {
        currentBarrel = barrel
        isModificationMode = barrel != null

        _uiState.value = _uiState.value.copy(
            barrelId = barrel?.id,
            barrelName = barrel?.name ?: "",
            volume = barrel?.volume?.toString() ?: "",
            brand = barrel?.brand ?: "",
            woodType = barrel?.woodType ?: "",
            heatType = barrel?.heatType ?: "",
            humidity = barrel?.storageHygrometer ?: "",
            temperature = barrel?.storageTemperature ?: "",
            description = barrel?.description ?: "",
            showAdvanced = false,
            errorMessage = null,
            successMessage = null
        )
    }

    fun updateBarrelName(name: String) {
        _uiState.value = _uiState.value.copy(barrelName = name, errorMessage = null)
    }

    fun updateVolume(volume: String) {
        _uiState.value = _uiState.value.copy(volume = volume, errorMessage = null)
    }

    fun updateBrand(brand: String) {
        _uiState.value = _uiState.value.copy(brand = brand, errorMessage = null)
    }

    fun updateWoodType(woodType: String) {
        _uiState.value = _uiState.value.copy(woodType = woodType, errorMessage = null)
    }

    fun updateHeatType(heatType: String) {
        _uiState.value = _uiState.value.copy(heatType = heatType)
    }

    fun updateHumidity(humidity: String) {
        _uiState.value = _uiState.value.copy(humidity = humidity)
    }

    fun updateTemperature(temperature: String) {
        _uiState.value = _uiState.value.copy(temperature = temperature)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun toggleAdvancedOptions() {
        _uiState.value = _uiState.value.copy(showAdvanced = !_uiState.value.showAdvanced)
    }

    fun validateAndSave(onResult: (AddBarrelEvent) -> Unit) {
        val state = _uiState.value

        // Validation
        val validationError = when {
            state.barrelName.isEmpty() -> R.string.barrel_name_required
            state.volume.isEmpty() -> R.string.volume_required
            state.brand.isEmpty() -> R.string.brand_required
            state.woodType.isEmpty() -> R.string.wood_type_required
            else -> null
        }

        if (validationError != null) {
            onResult(AddBarrelEvent.ShowError(validationError))
            return
        }

        // Création du fût
        val barrel = com.fmartinier.barrelclassifier.data.model.Barrel(
            id = currentBarrel?.id ?: 0,
            name = state.barrelName,
            volume = state.volume.toIntOrNull() ?: 1,
            brand = state.brand,
            woodType = state.woodType,
            imagePath = currentBarrel?.imagePath,
            heatType = state.heatType.takeIf { it.isNotEmpty() },
            storageHygrometer = state.humidity.takeIf { it.isNotEmpty() },
            storageTemperature = state.temperature.takeIf { it.isNotEmpty() },
            description = state.description.takeIf { it.isNotEmpty() },
            histories = currentBarrel?.histories ?: emptyList()
        )

        // Sauvegarde en base
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                withContext(Dispatchers.IO) {
                    if (isModificationMode) {
                        barrelDao.update(barrel)
                    } else {
                        AnalyticsService.logBarrelAdded(barrel.woodType, barrel.volume.toString())
                        barrelDao.insert(barrel)
                    }
                }

                val successMessage = if (isModificationMode)
                    R.string.barrel_modified_success
                else
                    R.string.barrel_added_success
                onResult(AddBarrelEvent.ShowSuccess(successMessage))

            } catch (e: Exception) {
                e.printStackTrace()
                onResult(AddBarrelEvent.ShowError(R.string.barrel_save_error))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
