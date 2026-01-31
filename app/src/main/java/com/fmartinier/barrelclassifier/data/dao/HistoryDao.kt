package com.fmartinier.barrelclassifier.data.dao

import android.content.ContentValues
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.ALERT_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BARREL_ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BEGIN_DATE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.DATE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.END_DATE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HISTORY_ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HISTORY_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.NAME_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.TYPE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.model.Alert
import com.fmartinier.barrelclassifier.data.model.History

class HistoryDao(private val dbHelper: DatabaseHelper) {

    fun insert(history: History): Long {
        val db = dbHelper.writableDatabase
        val values = getContentValues(history)
        return db.insert(HISTORY_TABLE_NAME, null, values)
    }

    fun update(historyId: Long, history: History): Int {
        val db = dbHelper.writableDatabase
        val values = getContentValues(history)

        return db.update(
            HISTORY_TABLE_NAME,
            values,
            "$ID_COLUMN_NAME = ?",
            arrayOf(historyId.toString())
        )
    }

    private fun getContentValues(history: History): ContentValues {
        return ContentValues().apply {
            put(BARREL_ID_COLUMN_NAME, history.barrelId)
            put(NAME_COLUMN_NAME, history.name)
            put(BEGIN_DATE_COLUMN_NAME, history.beginDate)

            if (history.endDate != null) {
                put(END_DATE_COLUMN_NAME, history.endDate)
            } else {
                putNull(END_DATE_COLUMN_NAME)
            }
        }
    }

    fun delete(id: Long) {
        dbHelper.writableDatabase.delete(
            HISTORY_TABLE_NAME,
            "$ID_COLUMN_NAME = ?",
            arrayOf(id.toString())
        )
    }

    fun getById(id: Long): History {
        val db = dbHelper.writableDatabase
        val list = mutableListOf<History>()
        val cursor = db.rawQuery(
            "SELECT * FROM $HISTORY_TABLE_NAME WHERE $ID_COLUMN_NAME = ?",
            arrayOf(id.toString())
        )

        while (cursor.moveToNext()) {
            list.add(
                History(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(ID_COLUMN_NAME)),
                    barrelId = cursor.getLong(cursor.getColumnIndexOrThrow(BARREL_ID_COLUMN_NAME)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(NAME_COLUMN_NAME)),
                    beginDate = cursor.getLong(cursor.getColumnIndexOrThrow(BEGIN_DATE_COLUMN_NAME)),
                    endDate = if (cursor.isNull(cursor.getColumnIndexOrThrow(END_DATE_COLUMN_NAME)))
                        null
                    else
                        cursor.getLong(cursor.getColumnIndexOrThrow(END_DATE_COLUMN_NAME))
                )
            )
        }

        cursor.close()
        if (list.size > 1) {
            throw Exception("Il y a une incohérence dans le nombre d'historiques (${list.size}) possédant l'id $id")
        }
        return list[0]
    }

}
