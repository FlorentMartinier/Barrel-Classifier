package com.fmartinier.barrelclassifier.data.dao

import android.content.ContentValues
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BARREL_ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BEGIN_DATE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.END_DATE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HISTORY_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.NAME_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.model.History

class HistoryDao(private val dbHelper: DatabaseHelper) {

    fun insert(history: History): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(BARREL_ID_COLUMN_NAME, history.barrelId)
            put(NAME_COLUMN_NAME, history.name)
            put(BEGIN_DATE_COLUMN_NAME, history.beginDate)
            history.endDate?.let {
                put(END_DATE_COLUMN_NAME, history.endDate)
            }
        }
        return db.insert(HISTORY_TABLE_NAME, null, values)
    }

    fun getByBarrelId(barrelId: Long): List<History> {
        val list = mutableListOf<History>()
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM $HISTORY_TABLE_NAME WHERE $BARREL_ID_COLUMN_NAME = ?",
            arrayOf(barrelId.toString())
        )

        while (cursor.moveToNext()) {
            list.add(
                History(
                    id = cursor.getLong(0),
                    barrelId = cursor.getLong(1),
                    name = cursor.getString(2),
                    beginDate = cursor.getLong(3),
                    endDate = cursor.getLong(4)
                )
            )
        }
        cursor.close()
        return list
    }

    fun deleteHistory(id: Long) {
        dbHelper.writableDatabase.delete(HISTORY_TABLE_NAME, "$ID_COLUMN_NAME = ?", arrayOf(id.toString()))
    }

}
