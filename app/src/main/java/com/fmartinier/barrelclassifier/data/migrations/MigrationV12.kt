package com.fmartinier.barrelclassifier.data.migrations

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.BARREL_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.HISTORY_TABLE_NAME
import com.fmartinier.barrelclassifier.data.DatabaseHelper.Companion.IMAGE_PATH_COLUMN_NAME

class MigrationV12(private val context: Context) : Migration {
    override val fromVersion = 11
    override val toVersion = 12

    override fun migrate(db: SQLiteDatabase) {
        cleanOrphanImages(db)
    }

    private fun cleanOrphanImages(db: SQLiteDatabase) {
        val validPaths = mutableSetOf<String>()

        // Récupération des paths de fut
        val cursorBarrels =
            db.rawQuery("SELECT $IMAGE_PATH_COLUMN_NAME FROM $BARREL_TABLE_NAME", null)
        while (cursorBarrels.moveToNext()) {
            cursorBarrels.getString(0)?.let { validPaths.add(it) }
        }
        cursorBarrels.close()

        // Récupération des paths d'historique
        val cursorHistory =
            db.rawQuery("SELECT $IMAGE_PATH_COLUMN_NAME FROM $HISTORY_TABLE_NAME", null)
        while (cursorHistory.moveToNext()) {
            cursorHistory.getString(0)?.let { validPaths.add(it) }
        }
        cursorHistory.close()

        // 2. Accéder au dossier files du contexte
        val storageDir = context.filesDir

        // 3. Lister et supprimer les fichiers orphelins
        storageDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.name.startsWith("img_")
                        || file.name.startsWith("barrel")
                        || file.name.startsWith("history_")
                    )
            ) {
                if (!validPaths.contains(file.absolutePath)) {
                    file.delete()
                }
            }
        }
    }
}