package com.fmartinier.barrelclassifier.data.migrations

import android.database.sqlite.SQLiteDatabase
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HISTORY_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.TYPE_COLUMN_NAME

class MigrationV10 : Migration {
    override val fromVersion = 9
    override val toVersion = 10

    override fun migrate(db: SQLiteDatabase) {

        // History
        db.execSQL("ALTER TABLE $HISTORY_TABLE_NAME ADD $TYPE_COLUMN_NAME TEXT DEFAULT '';")
    }
}