package com.fmartinier.barrelclassifier.data.migrations

import android.database.sqlite.SQLiteDatabase
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.ALERT_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.DATE_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HISTORY_ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HISTORY_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.ID_COLUMN_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.TYPE_COLUMN_NAME

class MigrationV8 : Migration {
    override val fromVersion = 7
    override val toVersion = 8

    override fun migrate(db: SQLiteDatabase) {
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
}