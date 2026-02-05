package com.fmartinier.barrelclassifier.data.migrations

import android.database.sqlite.SQLiteDatabase

interface Migration {
    val fromVersion: Int
    val toVersion: Int
    fun migrate(db: SQLiteDatabase)
}