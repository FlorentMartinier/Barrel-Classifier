package com.fmartinier.barrelclassifier.data.dao

import android.content.ContentValues
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.ALERT_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.DATE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HISTORY_ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.TYPE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.model.Alert

class AlertDao private constructor(private val dbHelper: DatabaseHelper) {

    fun insert(alerts: List<Alert>, historyId: Long): List<Alert> {
        val db = dbHelper.writableDatabase
        return alerts.map {
            val values = ContentValues().apply {
                put(HISTORY_ID_COLUMN_NAME, historyId)
                put(TYPE_COLUMN_NAME, it.type)
                put(DATE_COLUMN_NAME, it.date)
            }
            val savedAlertId = db.insert(ALERT_TABLE_NAME, null, values)
            it.copy(id = savedAlertId)
        }
    }

    fun findAllByHistoryId(historyId: Long): List<Alert> {
        val db = dbHelper.writableDatabase
        val list = mutableListOf<Alert>()
        val cursor = db.rawQuery(
            "SELECT * FROM $ALERT_TABLE_NAME WHERE $HISTORY_ID_COLUMN_NAME = ? ORDER BY $DATE_COLUMN_NAME",
            arrayOf(historyId.toString())
        )

        while (cursor.moveToNext()) {
            list.add(
                Alert(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(ID_COLUMN_NAME)),
                    historyId = historyId,
                    type = cursor.getString(cursor.getColumnIndexOrThrow(TYPE_COLUMN_NAME)),
                    date = cursor.getLong(cursor.getColumnIndexOrThrow(DATE_COLUMN_NAME))
                )
            )
        }

        cursor.close()
        return list
    }

    fun deleteByHistoryId(id: Long) {
        dbHelper.writableDatabase.delete(
            ALERT_TABLE_NAME,
            "$HISTORY_ID_COLUMN_NAME = ?",
            arrayOf(id.toString())
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: AlertDao? = null

        fun getInstance(dbHelper: DatabaseHelper): AlertDao {
            return INSTANCE ?: synchronized(this) {
                val instance = AlertDao(dbHelper)
                INSTANCE = instance
                instance
            }
        }
    }

}
