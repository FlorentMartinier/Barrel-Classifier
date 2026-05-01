package com.fmartinier.barrelclassifier.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fmartinier.barrelclassifier.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBarrelDialogScreen(
    uiState: AddBarrelUiState,
    onBarrelNameChange: (String) -> Unit,
    onVolumeChange: (String) -> Unit,
    onBrandChange: (String) -> Unit,
    onWoodTypeChange: (String) -> Unit,
    onHeatTypeChange: (String) -> Unit,
    onHumidityChange: (String) -> Unit,
    onTemperatureChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onToggleAdvanced: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    brandList: List<String> = emptyList(),
    woodTypeList: List<String> = emptyList(),
    heatingTypeList: List<String> = emptyList(),
    snackbarHostState: SnackbarHostState
) {
    var expandedBrand by remember { mutableStateOf(false) }
    var expandedWoodType by remember { mutableStateOf(false) }
    var expandedHeatType by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorResource(R.color.dialog_bg),
        title = {
            Text(
                text = stringResource(
                    if (uiState.barrelId != null) R.string.modify_barrel else R.string.add_barrel
                ),
                color = colorResource(id = R.color.text_secondary)
            )
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    // Barrel Name
                    Text(
                        text = stringResource(R.string.barrel_name),
                        modifier = Modifier.padding(top = 8.dp),
                        color = colorResource(id = R.color.text_secondary)
                    )
                    OutlinedTextField(
                        value = uiState.barrelName,
                        onValueChange = onBarrelNameChange,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorResource(R.color.accent_whisky),
                            focusedTextColor = colorResource(R.color.text_tertiary),
                            unfocusedTextColor = colorResource(R.color.text_tertiary),
                            cursorColor = colorResource(R.color.accent_whisky),
                        ),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.ex_barrel_name),
                                color = colorResource(id = R.color.text_tertiary)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    // Volume
                    Text(
                        text = stringResource(R.string.barrel_volume),
                        modifier = Modifier.padding(top = 12.dp),
                        color = colorResource(id = R.color.text_secondary)
                    )
                    OutlinedTextField(
                        value = uiState.volume,
                        onValueChange = onVolumeChange,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorResource(R.color.accent_whisky),
                            focusedTextColor = colorResource(R.color.text_tertiary),
                            unfocusedTextColor = colorResource(R.color.text_tertiary),
                            cursorColor = colorResource(R.color.accent_whisky),
                        ),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.ex_barrel_volume),
                                color = colorResource(id = R.color.text_tertiary)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Brand
                    Text(
                        text = stringResource(R.string.brand),
                        modifier = Modifier.padding(top = 12.dp),
                        color = colorResource(id = R.color.text_secondary)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedBrand,
                        onExpandedChange = { expandedBrand = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.brand,
                            onValueChange = onBrandChange,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorResource(R.color.accent_whisky),
                                focusedTextColor = colorResource(R.color.text_tertiary),
                                unfocusedTextColor = colorResource(R.color.text_tertiary),
                                cursorColor = colorResource(R.color.accent_whisky),
                            ),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.ex_brand),
                                    color = colorResource(id = R.color.text_tertiary)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBrand) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expandedBrand,
                            onDismissRequest = { expandedBrand = false }
                        ) {
                            brandList.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        onBrandChange(item)
                                        expandedBrand = false
                                    }
                                )
                            }
                        }
                    }

                    // Wood Type
                    Text(
                        text = stringResource(R.string.wood_type),
                        modifier = Modifier.padding(top = 12.dp),
                        color = colorResource(id = R.color.text_secondary)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedWoodType,
                        onExpandedChange = { expandedWoodType = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.woodType,
                            onValueChange = onWoodTypeChange,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorResource(R.color.accent_whisky),
                                focusedTextColor = colorResource(R.color.text_tertiary),
                                unfocusedTextColor = colorResource(R.color.text_tertiary),
                                cursorColor = colorResource(R.color.accent_whisky),
                            ),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.ex_wood_type),
                                    color = colorResource(id = R.color.text_tertiary)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWoodType) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expandedWoodType,
                            onDismissRequest = { expandedWoodType = false }
                        ) {
                            woodTypeList.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        onWoodTypeChange(item)
                                        expandedWoodType = false
                                    }
                                )
                            }
                        }
                    }

                    // Advanced Options Toggle Button
                    TextButton(
                        onClick = onToggleAdvanced,
                        modifier = Modifier.padding(top = 12.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colorResource(id = R.color.accent_whisky)
                        ),
                    ) {
                        Text(
                            text = stringResource(
                                if (uiState.showAdvanced) R.string.advanced_option_up else R.string.advanced_option_down
                            )
                        )
                    }

                    // Advanced Options Section
                    AnimatedVisibility(visible = uiState.showAdvanced) {
                        Column {
                            // Heat Type
                            Text(
                                text = stringResource(R.string.heating_type),
                                modifier = Modifier.padding(top = 12.dp),
                                color = colorResource(id = R.color.text_secondary)
                            )
                            ExposedDropdownMenuBox(
                                expanded = expandedHeatType,
                                onExpandedChange = { expandedHeatType = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                OutlinedTextField(
                                    value = uiState.heatType,
                                    onValueChange = onHeatTypeChange,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colorResource(R.color.accent_whisky),
                                        focusedTextColor = colorResource(R.color.text_tertiary),
                                        unfocusedTextColor = colorResource(R.color.text_tertiary),
                                        cursorColor = colorResource(R.color.accent_whisky),
                                    ),
                                    placeholder = {
                                        Text(
                                            text = stringResource(R.string.heating_type_exemple),
                                            color = colorResource(id = R.color.text_tertiary)
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = expandedHeatType
                                        )
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedHeatType,
                                    onDismissRequest = { expandedHeatType = false }
                                ) {
                                    heatingTypeList.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = {
                                                onHeatTypeChange(item)
                                                expandedHeatType = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Humidity
                            Text(
                                text = stringResource(R.string.storage_hygrometer),
                                modifier = Modifier.padding(top = 12.dp),
                                color = colorResource(id = R.color.text_secondary)
                            )
                            OutlinedTextField(
                                value = uiState.humidity,
                                onValueChange = onHumidityChange,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colorResource(R.color.accent_whisky),
                                    focusedTextColor = colorResource(R.color.text_tertiary),
                                    unfocusedTextColor = colorResource(R.color.text_tertiary),
                                    cursorColor = colorResource(R.color.accent_whisky),
                                ),
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.storage_hygrometer_exemple),
                                        color = colorResource(id = R.color.text_tertiary)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )

                            // Temperature
                            Text(
                                text = stringResource(R.string.storage_temperature),
                                modifier = Modifier.padding(top = 12.dp),
                                color = colorResource(id = R.color.text_secondary)
                            )
                            OutlinedTextField(
                                value = uiState.temperature,
                                onValueChange = onTemperatureChange,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colorResource(R.color.accent_whisky),
                                    focusedTextColor = colorResource(R.color.text_tertiary),
                                    unfocusedTextColor = colorResource(R.color.text_tertiary),
                                    cursorColor = colorResource(R.color.accent_whisky),
                                ),
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.storage_temperature_ex),
                                        color = colorResource(id = R.color.text_tertiary)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )

                            // Description
                            Text(
                                text = stringResource(R.string.detailed_description),
                                modifier = Modifier.padding(top = 12.dp),
                                color = colorResource(id = R.color.text_secondary)
                            )
                            OutlinedTextField(
                                value = uiState.description,
                                onValueChange = onDescriptionChange,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colorResource(R.color.accent_whisky),
                                    focusedTextColor = colorResource(R.color.text_tertiary),
                                    unfocusedTextColor = colorResource(R.color.text_tertiary),
                                    cursorColor = colorResource(R.color.accent_whisky),
                                ),
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.detailed_description),
                                        color = colorResource(id = R.color.text_tertiary)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                minLines = 4,
                                maxLines = 8,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                            )
                        }
                    }
                }

                // Snackbar inside the dialog window so it appears above dialog content
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorResource(id = R.color.accent_whisky)
                )
            ) {
                Text(
                    if (uiState.isLoading)
                        stringResource(R.string.saving)
                    else
                        stringResource(
                            if (uiState.barrelId != null) R.string.modify else R.string.add
                        )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorResource(id = R.color.accent_whisky)
                ),
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
