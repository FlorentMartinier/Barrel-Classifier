package com.fmartinier.barrelclassifier.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import com.fmartinier.barrelclassifier.data.migrations.MigrationRegistry

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "barrel.db", null, 10) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $BARREL_TABLE_NAME (" +
                    "    $ID_COLUMN_NAME INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    $NAME_COLUMN_NAME TEXT NOT NULL," +
                    "    $VOLUME_COLUMN_NAME INTEGER NOT NULL," +
                    "    $BRAND_COLUMN_NAME TEXT NOT NULL," +
                    "    $WOOD_TYPE_COLUMN_NAME TEXT NOT NULL," +
                    "    $HEATING_TYPE_COLUMN_NAME TEXT," +
                    "    $STORAGE_HYGROMETER_COLUMN_NAME TEXT," +
                    "    $STORAGE_TEMPERATURE_COLUMN_NAME TEXT," +
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
                    "    $TYPE_COLUMN_NAME TEXT," +
                    "    $DESCRIPTION_COLUMN_NAME TEXT," +
                    "    $ANGEL_SHARE_COLUMN_NAME TEXT," +
                    "    $ALCOHOLIC_STRENGTH_COLUMN_NAME TEXT," +
                    "    FOREIGN KEY ($BARREL_ID_COLUMN_NAME) REFERENCES $BARREL_TABLE_NAME($ID_COLUMN_NAME) ON DELETE CASCADE" +
                    ");"
        )
        db.execSQL(
            "CREATE TABLE $ALERT_TABLE_NAME (" +
                    "    $ID_COLUMN_NAME INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    $HISTORY_ID_COLUMN_NAME INTEGER NOT NULL," +
                    "    $TYPE_COLUMN_NAME NOT NULL," +
                    "    $DATE_COLUMN_NAME INTEGER NOT NULL," +
                    "    FOREIGN KEY ($HISTORY_ID_COLUMN_NAME) REFERENCES $HISTORY_TABLE_NAME($ID_COLUMN_NAME) ON DELETE CASCADE" +
                    ");"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val migrations = MigrationRegistry.getMigrations(oldVersion, newVersion)

        db.transaction {
            migrations.forEach { migration ->
                println("Exécution de la migration : ${migration.toVersion}")
                migration.migrate(this)
            }
        }
    }

    companion object {
        // Nom des tables
        const val BARREL_TABLE_NAME: String = "barrel"
        const val HISTORY_TABLE_NAME: String = "history"
        const val ALERT_TABLE_NAME: String = "alert"

        // Nom des colonnes des Barrel
        const val VOLUME_COLUMN_NAME: String = "volume"
        const val BRAND_COLUMN_NAME: String = "brand"
        const val WOOD_TYPE_COLUMN_NAME: String = "wood_type"
        const val IMAGE_PATH_COLUMN_NAME: String = "image_path"
        const val HEATING_TYPE_COLUMN_NAME: String = "heating_type"
        const val STORAGE_HYGROMETER_COLUMN_NAME: String = "storage_hygrometer"
        const val STORAGE_TEMPERATURE_COLUMN_NAME: String = "storage_temperature"

        // Nom des colonnes des History
        const val BEGIN_DATE_COLUMN_NAME: String = "begin_date"
        const val END_DATE_COLUMN_NAME: String = "end_date"
        const val BARREL_ID_COLUMN_NAME: String = "barrel_id"
        const val DESCRIPTION_COLUMN_NAME: String = "description"
        const val ANGEL_SHARE_COLUMN_NAME: String = "angel_share_id"
        const val ALCOHOLIC_STRENGTH_COLUMN_NAME: String = "alcoholic_strength_id"

        // Nom des colonnes des Alertes
        const val DATE_COLUMN_NAME: String = "date"
        const val HISTORY_ID_COLUMN_NAME: String = "history_id"

        // Colonnes transverses
        const val TYPE_COLUMN_NAME: String = "type"
        const val NAME_COLUMN_NAME: String = "name"
        const val ID_COLUMN_NAME: String = "id"
    }
}