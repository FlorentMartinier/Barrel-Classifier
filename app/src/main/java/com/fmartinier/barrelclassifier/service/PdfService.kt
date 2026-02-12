package com.fmartinier.barrelclassifier.service

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.toColorInt
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.withClip

class PdfService(private val context: Context) {

    private val pageWidth = 1240
    private val pageHeight = 1754
    private val margin = 80f
    private val contentWidth = pageWidth - (margin * 2)

    private val gold = "#C8A96A".toColorInt()
    private val bg = "#F6F1E7".toColorInt()
    private val dark = "#2B2521".toColorInt()
    private val grey = "#7A6F66".toColorInt()

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun export(barrel: Barrel): File {
        val pdf = PdfDocument()
        var pageNumber = 1

        // --- PAGE DE GARDE ---
        var page = startPage(pdf, pageNumber++)
        drawCover(page.canvas, barrel)
        drawFooter(page.canvas)
        pdf.finishPage(page)

        // --- PAGES D'HISTORIQUE ---
        val histories = barrel.histories.sortedBy { it.beginDate }
        page = startPage(pdf, pageNumber++)
        var canvas = page.canvas
        drawTimelineTitle(canvas)

        var currentY = 240f
        val timelineX = 140f

        histories.forEachIndexed { index, history ->
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 32f
                typeface = Typeface.DEFAULT_BOLD
            }
            val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 28f
                typeface = Typeface.DEFAULT
            }

            val blockHeight = measureHistoryHeight(history, paintText, pageWidth - 260f)

            // Vérification saut de page
            if (currentY + blockHeight > pageHeight - 120f) {
                drawFooter(canvas)
                pdf.finishPage(page)

                page = startPage(pdf, pageNumber++)
                canvas = page.canvas
                drawTimelineTitle(canvas)
                currentY = 240f
            }

            // Dessin du segment de ligne (sauf si dernier de la page)
            if (currentY + blockHeight < pageHeight - 120f && index < histories.size - 1) {
                val linePaint = Paint().apply { color = gold; strokeWidth = 4f }
                canvas.drawLine(timelineX, currentY, timelineX, currentY + blockHeight, linePaint)
            }

            // Dessin de l'entrée d'historique
            currentY = drawHistoryEntry(canvas, history, currentY, timelineX)
        }

        drawFooter(canvas)
        pdf.finishPage(page)

        val file = File(context.getExternalFilesDir(null), "${barrel.name.replace(" ", "_")}.pdf")
        pdf.writeTo(FileOutputStream(file))
        pdf.close()
        return file
    }

    private fun drawCover(canvas: Canvas, barrel: Barrel) {
        canvas.drawColor(bg)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dark }

        // Header
        paint.textSize = 72f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        canvas.drawText("Barrel Manager", margin, 160f, paint)

        paint.textSize = 36f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        canvas.drawText(context.getString(R.string.technical_barrel_sheet), margin, 220f, paint)

        paint.color = gold
        canvas.drawRect(margin, 250f, margin + 400f, 256f, paint)

        // Image
        drawBarrelImage(canvas, barrel)

        // Infos
        var y = 1040f
        val lineSpace = 55f

        paint.color = dark
        paint.textSize = 54f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(barrel.name, margin, 970f, paint)

        fun drawField(label: String, value: String?) {
            if (value.isNullOrBlank()) return
            paint.textSize = 32f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("$label :", margin, y, paint)

            paint.typeface = Typeface.DEFAULT
            canvas.drawText(value, margin + 600f, y, paint)
            y += lineSpace
        }

        drawField(context.getString(R.string.brand), barrel.brand)
        drawField(context.getString(R.string.barrel_volume), "${barrel.volume} L")
        drawField(context.getString(R.string.wood_type), barrel.woodType)
        drawField(context.getString(R.string.heating_type), barrel.heatType)
        drawField(context.getString(R.string.storage_temperature), "${barrel.storageTemperature}°C")
        drawField(context.getString(R.string.storage_hygrometer), "${barrel.storageHygrometer}%")
    }

    private fun drawHistoryEntry(canvas: Canvas, history: History, startY: Float, timelineX: Float): Float {
        var y = startY
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Point timeline
        paint.color = gold
        canvas.drawCircle(timelineX, y, 12f, paint)

        // Date
        paint.color = grey
        paint.textSize = 26f
        paint.typeface = Typeface.DEFAULT_BOLD
        val dateRange = if (history.endDate != null)
            "${dateFormat.format(Date(history.beginDate))} — ${dateFormat.format(Date(history.endDate))}"
        else "${context.getString(R.string.since)} ${dateFormat.format(Date(history.beginDate))}"
        canvas.drawText(dateRange, timelineX + 50f, y + 8f, paint)

        y += 45f

        // Nom de l'étape
        paint.color = dark
        paint.textSize = 34f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(history.name, timelineX + 50f, y, paint)

        y += 40f

        // Type (Tag)
        paint.textSize = 24f
        paint.color = gold
        canvas.drawText(history.type.uppercase(), timelineX + 50f, y, paint)

        y += 35f
        paint.color = dark
        paint.typeface = Typeface.DEFAULT

        fun drawWrapped(label: String, value: String?) {
            if (value.isNullOrBlank()) return
            y = drawMultilineText(canvas, "$label : $value", timelineX + 50f, y, contentWidth - 100f, paint)
            y += 10f
        }

        drawWrapped(context.getString(R.string.detailed_description), history.description)
        drawWrapped(context.getString(R.string.angels_share), "${history.angelsShare}%")
        drawWrapped(context.getString(R.string.alcoolic_strenght), "${history.alcoholicStrength}%")

        return y + 60f // Espace entre deux blocs
    }

    private fun drawMultilineText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint): Float {
        var currentY = y
        val words = text.split(" ")
        var line = ""

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(testLine) > maxWidth) {
                canvas.drawText(line, x, currentY, paint)
                line = word
                currentY += paint.textSize + 12f
            } else {
                line = testLine
            }
        }
        canvas.drawText(line, x, currentY, paint)
        return currentY + paint.textSize
    }

    private fun measureHistoryHeight(history: History, paintText: Paint, maxWidth: Float): Float {
        var h = 180f // Hauteur fixe (Dates + Titre + Type + Marges)

        fun wrappedHeight(label: String, value: String?): Float {
            if (value.isNullOrBlank()) return 0f
            val fullText = "$label : $value"
            val words = fullText.split(" ")
            var lines = 1
            var line = ""
            for (word in words) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (paintText.measureText(test) > maxWidth) {
                    lines++
                    line = word
                } else {
                    line = test
                }
            }
            return lines * (paintText.textSize + 12f) + 10f
        }

        h += wrappedHeight("Description", history.description)
        h += wrappedHeight("Angels", history.angelsShare)
        h += wrappedHeight("ABV", history.alcoholicStrength)
        return h
    }

    private fun drawBarrelImage(canvas: Canvas, barrel: Barrel) {
        barrel.imagePath?.let { path ->
            val bmp = BitmapFactory.decodeFile(path) ?: return

            // On agrandit la zone : de y=280f à y=900f (au lieu de 850f)
            // L'image prendra presque toute la largeur disponible entre les marges
            val rect = RectF(margin, 280f, pageWidth - margin, 900f)

            // Calcul du ratio pour un affichage "Center Crop" intelligent
            val canvasRatio = rect.width() / rect.height()
            val bitmapRatio = bmp.width.toFloat() / bmp.height.toFloat()

            val drawRect: RectF
            if (bitmapRatio > canvasRatio) {
                // L'image est plus large que la zone : on centre horizontalement
                val scaledWidth = rect.height() * bitmapRatio
                val sideMargin = (scaledWidth - rect.width()) / 2
                drawRect = RectF(rect.left - sideMargin, rect.top, rect.right + sideMargin, rect.bottom)
            } else {
                // L'image est plus haute : on centre verticalement
                val scaledHeight = rect.width() / bitmapRatio
                val topMargin = (scaledHeight - rect.height()) / 2
                drawRect = RectF(rect.left, rect.top - topMargin, rect.right, rect.bottom + topMargin)
            }

            // Rayon des coins augmenté à 60f pour un aspect plus "soft"
            val cornerRadius = 60f
            val clipPath = Path().apply {
                addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            }

            canvas.withClip(clipPath) {
                // Paint avec filtre pour éviter la pixellisation lors du redimensionnement
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                drawBitmap(bmp, null, drawRect, paint)

            }

            // Optionnel : Ajouter une bordure très fine autour de l'image pour la finition
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = gold
                style = Paint.Style.STROKE
                strokeWidth = 2f
                alpha = 40 // Très léger
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
        }
    }

    private fun drawTimelineTitle(canvas: Canvas) {
        canvas.drawColor(bg)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dark
            textSize = 52f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(context.getString(R.string.barrel_history), margin, 120f, paint)

        paint.color = gold
        canvas.drawRect(margin, 140f, margin + 200f, 144f, paint)
    }

    private fun drawFooter(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grey
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${context.getString(R.string.generated_by)} Barrel Manager app — ${dateFormat.format(Date())}", (pageWidth / 2).toFloat(), pageHeight - 50f, paint)
    }

    private fun startPage(pdf: PdfDocument, number: Int): PdfDocument.Page {
        return pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, number).create())
    }
}