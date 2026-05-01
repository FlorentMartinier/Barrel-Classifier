package com.fmartinier.barrelclassifier.ui.compose

import androidx.lifecycle.ViewModelProvider
import com.fmartinier.barrelclassifier.data.DatabaseHelper

class AddBarrelViewModelFactory(
    private val dbHelper: DatabaseHelper,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(AddBarrelViewModel::class.java)) {
            AddBarrelViewModel(dbHelper) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

