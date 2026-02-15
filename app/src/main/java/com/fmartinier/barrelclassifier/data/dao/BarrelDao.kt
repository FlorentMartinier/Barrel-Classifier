package com.fmartinier.barrelclassifier.data.dao

import android.content.ContentValues
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BARREL_ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BARREL_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BRAND_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HEATING_TYPE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HISTORY_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.IMAGE_PATH_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.NAME_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.STORAGE_HYGROMETER_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.STORAGE_TEMPERATURE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.VOLUME_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.WOOD_TYPE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.model.Barrel

class BarrelDao private constructor(private val dbHelper: DatabaseHelper) {

    private val historyDao = HistoryDao.getInstance(dbHelper)

    fun insert(barrel: Barrel): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(NAME_COLUMN_NAME, barrel.name)
            put(VOLUME_COLUMN_NAME, barrel.volume)
            put(BRAND_COLUMN_NAME, barrel.brand)
            put(WOOD_TYPE_COLUMN_NAME, barrel.woodType)
            put(IMAGE_PATH_COLUMN_NAME, barrel.imagePath)
            put(HEATING_TYPE_COLUMN_NAME, barrel.heatType)
            put(STORAGE_HYGROMETER_COLUMN_NAME, barrel.storageHygrometer)
            put(STORAGE_TEMPERATURE_COLUMN_NAME, barrel.storageTemperature)
        }
        return db.insert(BARREL_TABLE_NAME, null, values)
    }

    fun findAllWithHistories(): List<Barrel> {
        val db = dbHelper.readableDatabase
        val barrels = mutableListOf<Barrel>()

        val cursor = db.rawQuery("SELECT * FROM $BARREL_TABLE_NAME", null)

        while (cursor.moveToNext()) {
            val barrelId = cursor.getLong(cursor.getColumnIndexOrThrow(ID_COLUMN_NAME))

            val histories = historyDao.findAllByBarrelId(barrelId)

            barrels.add(
                Barrel(
                    id = barrelId,
                    name = cursor.getString(cursor.getColumnIndexOrThrow(NAME_COLUMN_NAME)),
                    volume = cursor.getInt(cursor.getColumnIndexOrThrow(VOLUME_COLUMN_NAME)),
                    brand = cursor.getString(cursor.getColumnIndexOrThrow(BRAND_COLUMN_NAME)),
                    woodType = cursor.getString(cursor.getColumnIndexOrThrow(WOOD_TYPE_COLUMN_NAME)),
                    imagePath = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PATH_COLUMN_NAME)),
                    heatType = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            HEATING_TYPE_COLUMN_NAME
                        )
                    ),
                    storageHygrometer = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            STORAGE_HYGROMETER_COLUMN_NAME
                        )
                    ),
                    storageTemperature = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            STORAGE_TEMPERATURE_COLUMN_NAME
                        )
                    ),
                    histories = histories
                )
            )
        }

        cursor.close()
        return barrels
    }

    fun deleteById(barrelId: Long) {
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
            put(HEATING_TYPE_COLUMN_NAME, barrel.heatType)
            put(STORAGE_HYGROMETER_COLUMN_NAME, barrel.storageHygrometer)
            put(STORAGE_TEMPERATURE_COLUMN_NAME, barrel.storageTemperature)
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

    fun findById(barrelId: Long): Barrel {
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            BARREL_TABLE_NAME,
            arrayOf(
                ID_COLUMN_NAME,
                NAME_COLUMN_NAME,
                VOLUME_COLUMN_NAME,
                BRAND_COLUMN_NAME,
                WOOD_TYPE_COLUMN_NAME,
                IMAGE_PATH_COLUMN_NAME,
                HEATING_TYPE_COLUMN_NAME,
                STORAGE_HYGROMETER_COLUMN_NAME,
                STORAGE_TEMPERATURE_COLUMN_NAME
            ),
            "$ID_COLUMN_NAME = ?",
            arrayOf(barrelId.toString()),
            null,
            null,
            null
        )

        if (!cursor.moveToFirst()) {
            cursor.close()
            throw IllegalStateException("Barrel not found for id=$barrelId")
        }

        val barrel = Barrel(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(ID_COLUMN_NAME)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(NAME_COLUMN_NAME)),
            volume = cursor.getInt(cursor.getColumnIndexOrThrow(VOLUME_COLUMN_NAME)),
            brand = cursor.getString(cursor.getColumnIndexOrThrow(BRAND_COLUMN_NAME)),
            woodType = cursor.getString(cursor.getColumnIndexOrThrow(WOOD_TYPE_COLUMN_NAME)),
            imagePath = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PATH_COLUMN_NAME)),
            heatType = cursor.getString(cursor.getColumnIndexOrThrow(HEATING_TYPE_COLUMN_NAME)),
            storageHygrometer = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    STORAGE_HYGROMETER_COLUMN_NAME
                )
            ),
            storageTemperature = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    STORAGE_TEMPERATURE_COLUMN_NAME
                )
            ),
            histories = historyDao.findAllByBarrelId(barrelId)
        )

        cursor.close()
        return barrel
    }

    companion object {
        @Volatile
        private var INSTANCE: BarrelDao? = null

        fun getInstance(dbHelper: DatabaseHelper): BarrelDao {
            return INSTANCE ?: synchronized(this) {
                val instance = BarrelDao(dbHelper)
                INSTANCE = instance
                instance
            }
        }
    }

}
