package com.fmartinier.barrelclassifier.data.migrations

import android.database.sqlite.SQLiteDatabase
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.ALCOHOLIC_STRENGTH_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.ANGEL_SHARE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BARREL_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.DESCRIPTION_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HEATING_TYPE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HISTORY_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.STORAGE_HYGROMETER_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.STORAGE_TEMPERATURE_COLUMN_NAME

class MigrationV9 : Migration {
    override val fromVersion = 8
    override val toVersion = 9

    override fun migrate(db: SQLiteDatabase) {

        // Barrel
        db.execSQL("ALTER TABLE $BARREL_TABLE_NAME ADD $HEATING_TYPE_COLUMN_NAME TEXT;")
        db.execSQL("ALTER TABLE $BARREL_TABLE_NAME ADD $STORAGE_HYGROMETER_COLUMN_NAME TEXT;")
        db.execSQL("ALTER TABLE $BARREL_TABLE_NAME ADD $STORAGE_TEMPERATURE_COLUMN_NAME TEXT;")

        // History
        db.execSQL("ALTER TABLE $HISTORY_TABLE_NAME ADD $DESCRIPTION_COLUMN_NAME TEXT;")
        db.execSQL("ALTER TABLE $HISTORY_TABLE_NAME ADD $ANGEL_SHARE_COLUMN_NAME TEXT;")
        db.execSQL("ALTER TABLE $HISTORY_TABLE_NAME ADD $ALCOHOLIC_STRENGTH_COLUMN_NAME TEXT;")
    }
}