package com.fmartinier.barrelclassifier.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.ui.compose.AddBarrelDialogScreen
import com.fmartinier.barrelclassifier.ui.compose.AddBarrelEvent
import com.fmartinier.barrelclassifier.ui.compose.AddBarrelViewModel
import com.fmartinier.barrelclassifier.ui.compose.AddBarrelViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddBarrelDialog : DialogFragment() {

    private var barrelId: Long? = null
    private var modificationMode = false

    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = DatabaseHelper.getInstance(requireContext())

        // Check if we're in modification mode
        arguments
            ?.takeIf { it.containsKey(ARG_BARREL_ID) }
            ?.getLong(ARG_BARREL_ID)
            ?.let {
                modificationMode = true
                barrelId = it
            }

        //setStyle(STYLE_NO_TITLE, R.style.AlertDialog)
    }

    override fun onStart() {
        super.onStart()
        // Make dialog window background transparent to avoid the white rectangle behind the Compose content
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    AddBarrelDialogWithViewModel(
                        barrelId = barrelId,
                        dbHelper = db,
                        onDismiss = { dismiss() },
                        onSuccess = {
                            parentFragmentManager.setFragmentResult(
                                "add_barrel_result",
                                Bundle.EMPTY
                            )
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "AddBarrelDialog"
        private const val ARG_BARREL_ID = "barrel_id"

        fun newInstance(barrelId: Long? = null): AddBarrelDialog {
            return AddBarrelDialog().apply {
                arguments = Bundle().apply {
                    barrelId?.let {
                        putLong(ARG_BARREL_ID, barrelId)
                    }
                }
            }
        }
    }
}

@Composable
fun AddBarrelDialogWithViewModel(
    barrelId: Long?,
    dbHelper: DatabaseHelper,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val factory = AddBarrelViewModelFactory(dbHelper)
    val viewModel: AddBarrelViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Initialize ViewModel with barrel data if in modification mode
    LaunchedEffect(barrelId) {
        if (barrelId != null) {
            val barrel = withContext(Dispatchers.IO) {
                try {
                    dbHelper.readableDatabase.use { db ->
                        // Get barrel from database using BarrelDao
                        val dao = BarrelDao.getInstance(dbHelper)
                        dao.findById(barrelId)
                    }
                } catch (_: Exception) {
                    null
                }
            }
            viewModel.initializeWithBarrel(barrel)
        }
    }

    // Handle events from ViewModel
    LaunchedEffect(Unit) {
        // This would be better with a shared flow, but for simplicity we'll use callbacks
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AddBarrelDialogScreen(
            uiState = uiState,
            onBarrelNameChange = viewModel::updateBarrelName,
            onVolumeChange = viewModel::updateVolume,
            onBrandChange = viewModel::updateBrand,
            onWoodTypeChange = viewModel::updateWoodType,
            onHeatTypeChange = viewModel::updateHeatType,
            onHumidityChange = viewModel::updateHumidity,
            onTemperatureChange = viewModel::updateTemperature,
            onDescriptionChange = viewModel::updateDescription,
            onToggleAdvanced = viewModel::toggleAdvancedOptions,
            onSave = {
                viewModel.validateAndSave { event ->
                    when (event) {
                        is AddBarrelEvent.ShowError -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(event.message)
                            }
                        }
                        is AddBarrelEvent.ShowSuccess -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(event.message)
                                onSuccess()
                            }
                        }
                        is AddBarrelEvent.Dismiss -> {
                            onDismiss()
                        }
                    }
                }
            },
            onDismiss = onDismiss,
            brandList = listOf("Allary", "Navarre", "Seguin Moreau", "Taransaud", "Radoux", "Damy"),
            woodTypeList = listOf("Chêne Français", "Chêne Américain", "Chêne Européen", "Acacia", "Châtaignier", "Cerisier", "Mûrier"),
            heatingTypeList = listOf("Légère", "Moyenne", "Moyenne Plus", "Forte", "Forte Plus", "Vapeur"),
            snackbarHostState = snackbarHostState
        )
    }
}
