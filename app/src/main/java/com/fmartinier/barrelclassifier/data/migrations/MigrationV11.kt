package com.fmartinier.barrelclassifier.data.migrations

import android.database.sqlite.SQLiteDatabase
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HISTORY_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.IMAGE_PATH_COLUMN_NAME

class MigrationV11 : Migration {
    override val fromVersion = 10
    override val toVersion = 11

    override fun migrate(db: SQLiteDatabase) {

        // History
        db.execSQL("ALTER TABLE $HISTORY_TABLE_NAME ADD $IMAGE_PATH_COLUMN_NAME TEXT;")
    }
}