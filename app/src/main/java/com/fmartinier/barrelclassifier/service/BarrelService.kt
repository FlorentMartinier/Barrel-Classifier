package com.fmartinier.barrelclassifier.service

import android.content.Context
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.data.model.Barrel

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
}