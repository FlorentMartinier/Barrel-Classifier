package com.fmartinier.barrelclassifier.data.dao

import android.content.ContentValues
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.ALCOHOLIC_STRENGTH_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.ANGEL_SHARE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BARREL_ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BEGIN_DATE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.DESCRIPTION_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.END_DATE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HISTORY_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.IMAGE_PATH_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.NAME_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.TYPE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.model.History

class HistoryDao private constructor(private val dbHelper: DatabaseHelper) {

    val alertDao = AlertDao.getInstance(dbHelper)

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

    fun deleteById(id: Long) {
        dbHelper.writableDatabase.delete(
            HISTORY_TABLE_NAME,
            "$ID_COLUMN_NAME = ?",
            arrayOf(id.toString())
        )
    }

    fun findById(id: Long): History {
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
                    type = cursor.getString(cursor.getColumnIndexOrThrow(TYPE_COLUMN_NAME)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(DESCRIPTION_COLUMN_NAME)),
                    angelsShare = cursor.getString(cursor.getColumnIndexOrThrow(ANGEL_SHARE_COLUMN_NAME)),
                    alcoholicStrength = cursor.getString(cursor.getColumnIndexOrThrow(ALCOHOLIC_STRENGTH_COLUMN_NAME)),
                    endDate = if (cursor.isNull(cursor.getColumnIndexOrThrow(END_DATE_COLUMN_NAME)))
                        null
                    else
                        cursor.getLong(cursor.getColumnIndexOrThrow(END_DATE_COLUMN_NAME)),
                    imagePath = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PATH_COLUMN_NAME)),
                    alerts = alertDao.findAllByHistoryId(id),
                )
            )
        }

        cursor.close()
        if (list.size > 1) {
            throw Exception("Il y a une incohérence dans le nombre d'historiques (${list.size}) possédant l'id $id")
        }
        return list[0]
    }

    fun findAllByBarrelId(barrelId: Long): List<History> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<History>()

        val cursor = db.rawQuery(
            "SELECT * FROM $HISTORY_TABLE_NAME WHERE $BARREL_ID_COLUMN_NAME = ? ORDER BY $BEGIN_DATE_COLUMN_NAME",
            arrayOf(barrelId.toString())
        )

        while (cursor.moveToNext()) {
            list.add(
                History(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(ID_COLUMN_NAME)),
                    barrelId = barrelId,
                    name = cursor.getString(cursor.getColumnIndexOrThrow(NAME_COLUMN_NAME)),
                    beginDate = cursor.getLong(cursor.getColumnIndexOrThrow(BEGIN_DATE_COLUMN_NAME)),
                    endDate = if (cursor.isNull(cursor.getColumnIndexOrThrow(END_DATE_COLUMN_NAME)))
                        null
                    else
                        cursor.getLong(cursor.getColumnIndexOrThrow(END_DATE_COLUMN_NAME)),
                    type = cursor.getString(cursor.getColumnIndexOrThrow(TYPE_COLUMN_NAME)),
                    description = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            DESCRIPTION_COLUMN_NAME
                        )
                    ),
                    angelsShare = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            ANGEL_SHARE_COLUMN_NAME
                        )
                    ),
                    alcoholicStrength = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            ALCOHOLIC_STRENGTH_COLUMN_NAME
                        )
                    ),
                    imagePath = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PATH_COLUMN_NAME)),
                    alerts = alertDao.findAllByHistoryId(cursor.getLong(cursor.getColumnIndexOrThrow(ID_COLUMN_NAME)))
                )
            )
        }

        cursor.close()
        return list
    }

    fun updateImage(historyId: Long, imagePath: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(IMAGE_PATH_COLUMN_NAME, imagePath)
        }

        db.update(
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
            put(DESCRIPTION_COLUMN_NAME, history.description)
            put(ANGEL_SHARE_COLUMN_NAME, history.angelsShare)
            put(ALCOHOLIC_STRENGTH_COLUMN_NAME, history.alcoholicStrength)
            put(TYPE_COLUMN_NAME, history.type)
            put(IMAGE_PATH_COLUMN_NAME, history.imagePath)

            if (history.endDate != null) {
                put(END_DATE_COLUMN_NAME, history.endDate)
            } else {
                putNull(END_DATE_COLUMN_NAME)
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: HistoryDao? = null

        fun getInstance(dbHelper: DatabaseHelper): HistoryDao {
            return INSTANCE ?: synchronized(this) {
                val instance = HistoryDao(dbHelper)
                INSTANCE = instance
                instance
            }
        }
    }
}
