package com.fmartinier.barrelclassifier.service

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.AlertDao
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.data.dao.HistoryDao
import com.fmartinier.barrelclassifier.data.exception.ImportFileNotCompatibleException
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.utils.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.collections.forEach

class ImportExportService(val activity: Activity) {
    private var db: DatabaseHelper = DatabaseHelper.getInstance(activity)
    private var barrelDao: BarrelDao = BarrelDao.getInstance(db)
    private var historyDao: HistoryDao = HistoryDao.getInstance(db)
    private var alertDao: AlertDao = AlertDao.getInstance(db)

    fun exportToZip(uri: Uri) {
        val (jsonString, imagePaths) = exportBarrelsWithImages()
        val zipFile = File(activity.cacheDir, "barrel_manager_export.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { out ->
            // 1. Ajouter le JSON
            val entry = ZipEntry("data.json")
            out.putNextEntry(entry)
            out.write(jsonString.toByteArray())
            out.closeEntry()

            // 2. Ajouter les images
            imagePaths.forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    out.putNextEntry(ZipEntry("images/${file.name}"))
                    file.inputStream().use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }
        activity.contentResolver.openOutputStream(uri)?.use { outputStream ->
            zipFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
            zipFile.delete()

            Toast.makeText(activity, activity.getString(R.string.zip_export_success), Toast.LENGTH_SHORT).show()
        }
    }

    fun importZipArchive(zipUri: Uri) {
        val tempDir = File(activity.applicationContext.cacheDir, "import_temp_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            FileUtils.unzip(activity.applicationContext, zipUri, tempDir)

            // 2. Chercher le fichier JSON dans les fichiers extraits
            val jsonFile = File(tempDir, "data.json")
            if (!jsonFile.exists()) throw Exception(activity.getString(R.string.data_json_missing))

            val jsonString = jsonFile.readText()
            val importedData = importJson(jsonString)

            // 4. Traiter et déplacer les images
            val finalImageDir = File(activity.applicationContext.filesDir, "")
            if (!finalImageDir.exists()) finalImageDir.mkdirs()

            importedData.flatMap { barrel ->
                val fileName = File(barrel.imagePath ?: "").name
                if (fileName.isNullOrEmpty()) {
                    return@flatMap emptyList<History>()
                }
                val tempImage = File(tempDir, "images/$fileName")

                if (tempImage.exists()) {
                    val permanentImage = File(finalImageDir, fileName)
                    tempImage.copyTo(permanentImage, overwrite = true)
                    barrelDao.updateImage(barrel.id, permanentImage.absolutePath)
                }
                barrel.histories
            }.forEach { history ->
                val fileName = File(history.imagePath ?: "").name
                if (fileName.isNullOrEmpty()) {
                    return@forEach
                }
                val tempImage = File(tempDir, "images/$fileName")
                if (tempImage.exists()) {
                    val permanentImage = File(finalImageDir, fileName)
                    tempImage.copyTo(permanentImage, overwrite = true)
                    historyDao.updateImage(history.id, permanentImage.absolutePath)
                }
            }

            Toast.makeText(activity.applicationContext,
                activity.getString(R.string.zip_import_success), Toast.LENGTH_SHORT).show()
            AnalyticsService.logImportSuccess()
        } catch(e: ImportFileNotCompatibleException) {
            Toast.makeText(activity,
                activity.getString(R.string.imported_file_incompatible), Toast.LENGTH_LONG).show()
            AnalyticsService.logImportError(e.throwable.message.toString())
        } catch (e: Exception) {
            Toast.makeText(activity.applicationContext,
                activity.getString(R.string.zip_import_error), Toast.LENGTH_SHORT).show()
            AnalyticsService.logImportError(e.message.toString())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun exportBarrelsWithImages(): Pair<String, List<String>> {
        barrelDao.findAllWithHistories().let {
            val barrelImages = it.mapNotNull { barrel -> barrel.imagePath }
            val historyImages = it
                .flatMap { barrel -> barrel.histories }
                .mapNotNull { history -> history.imagePath }
            val allImages = barrelImages + historyImages

            return Pair(jacksonObjectMapper().writeValueAsString(it), allImages)
        }
    }

    private fun importJson(jsonString: String): List<Barrel> {
        try {
            val mapper = jacksonObjectMapper()
            val listType = mapper.typeFactory.constructCollectionType(List::class.java, Barrel::class.java)
            val importedData: List<Barrel> = mapper.readValue(jsonString, listType)

            // 3. Injecter dans la BDD
            importedData.forEach { barrel ->
                val barrelId = barrelDao.insert(barrel)
                barrel.histories.forEach { history ->
                    val historyToSave = history.copy(barrelId = barrelId)
                    val historyId = historyDao.insert(historyToSave)
                    val alertsToSave = history.alerts.map { alert ->
                        alert.copy(historyId = historyId)
                    }
                    alertDao.insert(alertsToSave, historyId)
                        .forEach {
                            AlertService().schedule(activity.applicationContext, it, barrel.name, historyId)
                        }
                }
            }

            return importedData
        } catch (e: Exception) {
            throw ImportFileNotCompatibleException(e)
        }
        return listOf()
    }
}