package com.fmartinier.barrelclassifier.data.dao

import android.content.ContentValues
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BARREL_ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BARREL_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BEGIN_DATE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BRAND_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.END_DATE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HISTORY_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.IMAGE_PATH_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.NAME_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.VOLUME_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.WOOD_TYPE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History

class BarrelDao(private val dbHelper: DatabaseHelper) {

    fun insert(barrel: Barrel): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(NAME_COLUMN_NAME, barrel.name)
            put(VOLUME_COLUMN_NAME, barrel.volume)
            put(BRAND_COLUMN_NAME, barrel.brand)
            put(WOOD_TYPE_COLUMN_NAME, barrel.woodType)
            put(IMAGE_PATH_COLUMN_NAME, barrel.imagePath)
        }
        return db.insert(BARREL_TABLE_NAME, null, values)
    }

    fun getAllBarrelsWithHistories(): List<Barrel> {
        val db = dbHelper.readableDatabase
        val barrels = mutableListOf<Barrel>()

        val cursor = db.rawQuery("SELECT * FROM $BARREL_TABLE_NAME", null)

        while (cursor.moveToNext()) {
            val barrelId = cursor.getLong(cursor.getColumnIndexOrThrow(ID_COLUMN_NAME))

            val histories = getHistoriesForBarrel(barrelId)

            barrels.add(
                Barrel(
                    id = barrelId,
                    name = cursor.getString(cursor.getColumnIndexOrThrow(NAME_COLUMN_NAME)),
                    volume = cursor.getInt(cursor.getColumnIndexOrThrow(VOLUME_COLUMN_NAME)),
                    brand = cursor.getString(cursor.getColumnIndexOrThrow(BRAND_COLUMN_NAME)),
                    woodType = cursor.getString(cursor.getColumnIndexOrThrow(WOOD_TYPE_COLUMN_NAME)),
                    imagePath = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PATH_COLUMN_NAME)),
                    histories = histories
                )
            )
        }

        cursor.close()
        return barrels
    }

    fun deleteBarrel(barrelId: Long) {
        dbHelper.writableDatabase.delete(
            HISTORY_TABLE_NAME,
            "$BARREL_ID_COLUMN_NAME = ?",
            arrayOf(barrelId.toString())
        )
        dbHelper.writableDatabase.delete(
            BARREL_TABLE_NAME,
            "$ID_COLUMN_NAME = ?",
            arrayOf(barrelId.toString())
        )
    }

    fun update(barrel: Barrel) {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(NAME_COLUMN_NAME, barrel.name)
            put(VOLUME_COLUMN_NAME, barrel.volume)
            put(BRAND_COLUMN_NAME, barrel.brand)
            put(WOOD_TYPE_COLUMN_NAME, barrel.woodType)
        }

        db.update(
            BARREL_TABLE_NAME,
            values,
            "$ID_COLUMN_NAME = ?",
            arrayOf(barrel.id.toString())
        )
    }

    fun updateImage(barrelId: Long, imagePath: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(IMAGE_PATH_COLUMN_NAME, imagePath)
        }

        db.update(
            BARREL_TABLE_NAME,
            values,
            "$ID_COLUMN_NAME = ?",
            arrayOf(barrelId.toString())
        )
    }

    private fun getHistoriesForBarrel(barrelId: Long): List<History> {
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
                        cursor.getLong(cursor.getColumnIndexOrThrow(END_DATE_COLUMN_NAME))
                )
            )
        }

        cursor.close()
        return list
    }

}
