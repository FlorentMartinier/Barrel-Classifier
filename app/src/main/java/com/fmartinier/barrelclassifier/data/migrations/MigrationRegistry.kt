package com.fmartinier.barrelclassifier.data.migrations

object MigrationRegistry {

    val migrations: List<Migration> = listOf(
        MigrationV8(),
        MigrationV9(),
        MigrationV10(),
        MigrationV11(),
    )

    fun getMigrations(fromVersion: Int, toVersion: Int): List<Migration> {
        return migrations
            .filter { it.fromVersion >= fromVersion && it.toVersion <= toVersion }
            .sortedBy { it.fromVersion }
    }
}