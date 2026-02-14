package com.fmartinier.barrelclassifier.data.migrations

import android.content.Context

object MigrationRegistry {

    fun getMigrations(fromVersion: Int, toVersion: Int, context: Context): List<Migration> {
        val migrations = listOf(
            MigrationV8(),
            MigrationV9(),
            MigrationV10(),
            MigrationV11(),
            MigrationV12(context),
        )

        return migrations
            .filter { it.fromVersion >= fromVersion && it.toVersion <= toVersion }
            .sortedBy { it.fromVersion }
    }
}