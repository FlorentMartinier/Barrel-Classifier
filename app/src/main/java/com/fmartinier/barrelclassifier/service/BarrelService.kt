package com.fmartinier.barrelclassifier.service

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.ui.AddBarrelDialog
import com.fmartinier.barrelclassifier.ui.AddHistoryDialog

class BarrelService(val context: Context, dbHelper: DatabaseHelper) {

    val barrelDao = BarrelDao.getInstance(dbHelper)
    val imageService = ImageService()
    val alertService = AlertService()

    fun delete(barrel: Barrel) {
        // Suppression de toutes les images
        barrel.histories.forEach {
            imageService.deleteImageIfExist(it.imagePath)
        }
        imageService.deleteImageIfExist(barrel.imagePath)

        // Annulation de toutes les alertes
        barrel.histories.forEach {
            alertService.cancelByHistoryId(context, it.id)
        }

        // Suppression en BDD
        barrelDao.deleteById(barrel.id)
    }

    fun openAddHistoryDialog(barrel: Barrel, historyId: Long? = null, manager: FragmentManager) {
        AddHistoryDialog.newInstance(barrel, historyId)
            .show(manager, AddHistoryDialog.TAG)
    }

    fun openEditBarrel(barrel: Barrel, manager: FragmentManager) {
        AddBarrelDialog.newInstance(barrel.id)
            .show(manager, AddBarrelDialog.TAG)
    }
}