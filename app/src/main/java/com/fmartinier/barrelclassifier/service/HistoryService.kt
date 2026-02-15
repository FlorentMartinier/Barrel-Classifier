package com.fmartinier.barrelclassifier.service

import android.content.Context
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.HistoryDao
import com.fmartinier.barrelclassifier.data.model.History

class HistoryService(val context: Context, dbHelper: DatabaseHelper) {

    val historyDao = HistoryDao.getInstance(dbHelper)
    val imageService = ImageService()
    val alertService = AlertService()

    fun delete(history: History) {
        val id = history.id

        // Suppression de l'image
        imageService.deleteImageIfExist(history.imagePath)

        // Annulation des alertes
        alertService.cancelByHistoryId(context, id)

        // Suppression en BDD
        historyDao.deleteById(id)
    }
}