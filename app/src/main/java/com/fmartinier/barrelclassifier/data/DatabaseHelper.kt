package com.fmartinier.barrelclassifier.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "fut.db", null, 5) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $BARREL_TABLE_NAME (" +
                    "    $ID_COLUMN_NAME INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    $NAME_COLUMN_NAME TEXT NOT NULL," +
                    "    $VOLUME_COLUMN_NAME INTEGER NOT NULL," +
                    "    $BRAND_COLUMN_NAME TEXT NOT NULL," +
                    "    $WOOD_TYPE_COLUMN_NAME TEXT NOT NULL," +
                    "    $IMAGE_PATH_COLUMN_NAME TEXT" +
                    ");"
        )
        db.execSQL(
            "CREATE TABLE $HISTORY_TABLE_NAME (" +
                    "    $ID_COLUMN_NAME INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    $BARREL_ID_COLUMN_NAME INTEGER NOT NULL," +
                    "    $NAME_COLUMN_NAME TEXT NOT NULL," +
                    "    $BEGIN_DATE_COLUMN_NAME INTEGER NOT NULL," +
                    "    $END_DATE_COLUMN_NAME INTEGER," +
                    "    FOREIGN KEY ($BARREL_ID_COLUMN_NAME) REFERENCES $BARREL_TABLE_NAME($ID_COLUMN_NAME) ON DELETE CASCADE" +
                    ");"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $HISTORY_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $BARREL_TABLE_NAME")
        onCreate(db)
    }

    companion object {
        // Nom des tables
        const val BARREL_TABLE_NAME: String = "barrel"
        const val HISTORY_TABLE_NAME: String = "history"

        // Nom des colonnes des Barrel
        const val VOLUME_COLUMN_NAME: String = "volume"
        const val BRAND_COLUMN_NAME: String = "brand"
        const val WOOD_TYPE_COLUMN_NAME: String = "wood_type"
        const val IMAGE_PATH_COLUMN_NAME: String = "image_path"

        // Nom des colonnes des History
        const val BARREL_ID_COLUMN_NAME: String = "barrel_id"
        const val BEGIN_DATE_COLUMN_NAME: String = "begin_date"
        const val END_DATE_COLUMN_NAME: String = "end_date"

        // Colonnes transverses
        const val NAME_COLUMN_NAME: String = "name"
        const val ID_COLUMN_NAME: String = "id"
    }
}