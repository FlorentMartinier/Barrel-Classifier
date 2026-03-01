package com.fmartinier.barrelclassifier.data.migrations

import android.database.sqlite.SQLiteDatabase
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BARREL_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.DESCRIPTION_COLUMN_NAME

class MigrationV13() : Migration {
    override val fromVersion = 12
    override val toVersion = 13

    override fun migrate(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE $BARREL_TABLE_NAME ADD $DESCRIPTION_COLUMN_NAME TEXT;")
    }
}