package com.fmartinier.barrelclassifier.service

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withClip
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.utils.DateUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        // Ajout du logo en haut à droite
        drawAppLogo(canvas, pageWidth - margin - 120f, 100f, 120f)

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
        y = drawMultilineText(canvas, barrel.name, margin, 970f, paint)

        fun drawField(label: String, value: String?) {
            if (value.isNullOrBlank() || listOf(" L", "°C", "%").contains(value)) return
            paint.textSize = 32f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("$label :", margin, y, paint)

            paint.typeface = Typeface.DEFAULT
            canvas.drawText(value, margin + 600f, y, paint)
            y += lineSpace
        }

        val firstHistory: Long = barrel.histories.minOfOrNull { it.beginDate } ?: 0

        drawField(context.getString(R.string.brand), barrel.brand)
        drawField(context.getString(R.string.barrel_volume), "${barrel.volume} L")
        drawField(context.getString(R.string.wood_type), barrel.woodType)
        drawField(context.getString(R.string.heating_type), barrel.heatType)
        drawField(context.getString(R.string.storage_temperature), "${barrel.storageTemperature}°C")
        drawField(context.getString(R.string.storage_hygrometer), "${barrel.storageHygrometer}%")
        drawField(
            context.getString(R.string.first_history_date),
            dateFormat.format(Date(firstHistory))
        )
        drawField(context.getString(R.string.aging_number), barrel.histories.size.toString())
    }

    private fun drawHistoryEntry(
        canvas: Canvas,
        history: History,
        startY: Float,
        timelineX: Float
    ): Float {
        var currentY = startY
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Paramètres de l'indicateur (cercle)
        val imageSize = 80f
        val textMarginX = timelineX + (imageSize / 2f) + 40f
        val textMaxWidth = pageWidth - textMarginX - margin

        // Positionnement du cercle sur la ligne
        // On centre le cercle verticalement par rapport à la première ligne de texte
        val circleCenterY = startY + 15f
        val rect = RectF(
            timelineX - (imageSize / 2f),
            circleCenterY - (imageSize / 2f),
            timelineX + (imageSize / 2f),
            circleCenterY + (imageSize / 2f)
        )

        // --- 1. DESSIN DE L'INDICATEUR (PHOTO OU DISQUE DORÉ) ---
        if (!history.imagePath.isNullOrBlank()) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(history.imagePath, options)
            options.inSampleSize = calculateInSampleSize(options, 150, 150)
            options.inJustDecodeBounds = false

            val bmp = BitmapFactory.decodeFile(history.imagePath, options)
            if (bmp != null) {
                val clipPath = Path().apply {
                    addCircle(rect.centerX(), rect.centerY(), imageSize / 2f, Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(clipPath)

                // Logique Center Crop
                val bitmapRatio = bmp.width.toFloat() / bmp.height.toFloat()
                val drawRect = if (bitmapRatio > 1f) {
                    val scaledWidth = imageSize * bitmapRatio
                    val sideMargin = (scaledWidth - imageSize) / 2
                    RectF(rect.left - sideMargin, rect.top, rect.right + sideMargin, rect.bottom)
                } else {
                    val scaledHeight = imageSize / bitmapRatio
                    val topMargin = (scaledHeight - imageSize) / 2
                    RectF(rect.left, rect.top - topMargin, rect.right, rect.bottom + topMargin)
                }

                canvas.drawBitmap(bmp, null, drawRect, Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
                bmp.recycle()

                // Petite bordure dorée pour finir le contour de la photo
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = gold
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawCircle(rect.centerX(), rect.centerY(), imageSize / 2f, borderPaint)
            }
        } else {
            // S'il n'y a pas d'image : on dessine un disque doré plein (le fameux sceau)
            paint.style = Paint.Style.FILL
            paint.color = gold
            // On prend 90% de la taille pour un aspect "point d'étape" imposant mais propre
            canvas.drawCircle(rect.centerX(), rect.centerY(), (imageSize / 2f) * 0.9f, paint)
        }

        // --- 2. DESSIN DU TEXTE ---
        // Date et Durée
        paint.style = Paint.Style.FILL
        paint.color = grey
        paint.textSize = 26f
        paint.typeface = Typeface.DEFAULT_BOLD

        val calculationTime = DateUtils.calculateDuration(context, history.beginDate, history.endDate ?: DateUtils.getCurrentDate())
        val dateRange = if (history.endDate != null)
            "${dateFormat.format(Date(history.beginDate))} — ${dateFormat.format(Date(history.endDate))} ($calculationTime)"
        else
            "${context.getString(R.string.since)} ${dateFormat.format(Date(history.beginDate))} ($calculationTime)"

        canvas.drawText(dateRange, textMarginX, startY + 5f, paint)
        currentY += 45f

        // Titre de l'étape (ex: Nom du rhum ou de l'opération)
        paint.color = dark
        paint.textSize = 34f
        paint.typeface = Typeface.DEFAULT_BOLD
        currentY = drawMultilineText(canvas, history.name, textMarginX, currentY, paint, textMaxWidth)

        // Type d'opération (AGING, FILLING, etc.)
        paint.textSize = 24f
        paint.color = gold
        canvas.drawText(history.type.uppercase(), textMarginX, currentY, paint)
        currentY += 35f

        // Détails techniques
        paint.color = dark
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 28f

        fun drawWrapped(label: String, value: String?) {
            if (value.isNullOrBlank() || value == "%") return
            currentY = drawMultilineText(canvas, "$label : $value", textMarginX, currentY, paint, textMaxWidth)
            currentY += 10f
        }

        drawWrapped(context.getString(R.string.detailed_description), history.description)
        drawWrapped(context.getString(R.string.angels_share), "${history.angelsShare}%")
        drawWrapped(context.getString(R.string.alcoolic_strenght), "${history.alcoholicStrength}%")

        // Retourne la hauteur totale consommée pour savoir où commence la ligne suivante
        // On s'assure de laisser un espace après le dernier élément (texte ou cercle)
        val blockBottom = maxOf(currentY + 40f, rect.bottom + 40f)
        return blockBottom
    }

    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint
    ): Float {
        var currentY = y
        val words = text.split(" ")
        var line = ""

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(testLine) > contentWidth - 100f) {
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
        val imageSize = 80f
        val textMarginX = 140f + (imageSize / 2f) + 40f
        val textMaxWidth = pageWidth - textMarginX - 80f // 80f est la marge droite

        var hText = 100f // Marge initiale pour date, titre et type

        fun wrappedHeight(label: String, value: String?): Float {
            if (value.isNullOrBlank() || value == "%") return 0f
            val fullText = "$label : $value"
            val words = fullText.split(" ")
            var lines = 1
            var line = ""
            for (word in words) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (paintText.measureText(test) > textMaxWidth) {
                    lines++
                    line = word
                } else {
                    line = test
                }
            }
            return lines * (paintText.textSize + 12f) + 12f
        }

        hText += wrappedHeight("Nom", history.name)
        hText += wrappedHeight("Description", history.description)
        hText += wrappedHeight("Angels", history.angelsShare)
        hText += wrappedHeight("ABV", history.alcoholicStrength)

        return maxOf(hText, imageSize + 20f) + 40f
    }

    private fun drawBarrelImage(canvas: Canvas, barrel: Barrel) {
        barrel.imagePath?.let { path ->
            // --- OPTIMISATION ICI AUSSI ---
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)

            // Pour la grande image, on demande une résolution un peu plus haute (ex: 1000px)
            options.inSampleSize = calculateInSampleSize(options, 1000, 800)
            options.inJustDecodeBounds = false

            val bmp = BitmapFactory.decodeFile(path, options) ?: return

            val rect = RectF(margin, 280f, pageWidth - margin, 900f)
            val canvasRatio = rect.width() / rect.height()
            val bitmapRatio = bmp.width.toFloat() / bmp.height.toFloat()

            val drawRect = if (bitmapRatio > canvasRatio) {
                val scaledWidth = rect.height() * bitmapRatio
                val sideMargin = (scaledWidth - rect.width()) / 2
                RectF(rect.left - sideMargin, rect.top, rect.right + sideMargin, rect.bottom)
            } else {
                val scaledHeight = rect.width() / bitmapRatio
                val topMargin = (scaledHeight - rect.height()) / 2
                RectF(rect.left, rect.top - topMargin, rect.right, rect.bottom + topMargin)
            }

            val cornerRadius = 60f
            val clipPath =
                Path().apply { addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW) }

            canvas.withClip(clipPath) {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                drawBitmap(bmp, null, drawRect, paint)
            }

            bmp.recycle() // INDISPENSABLE pour la fluidité

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = gold
                style = Paint.Style.STROKE
                strokeWidth = 2f
                alpha = 40
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
        }

        val footerText =
            "${context.getString(R.string.generated_by)} Barrel Manager — ${dateFormat.format(Date())}"
        val textWidth = paint.measureText(footerText)
        val logoSize = 30f
        val spacing = 15f

        // Calcul pour centrer l'ensemble [Logo + Texte]
        val totalWidth = logoSize + spacing + textWidth
        val startX = (pageWidth - totalWidth) / 2

        // Dessin du mini logo
        drawAppLogo(canvas, startX, pageHeight - 70f, logoSize)

        // Dessin du texte
        canvas.drawText(
            footerText,
            startX + logoSize + spacing,
            pageHeight - 48f,
            paint
        )
    }

    private fun startPage(pdf: PdfDocument, number: Int): PdfDocument.Page {
        return pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, number).create())
    }

    private fun drawAppLogo(canvas: Canvas, x: Float, y: Float, size: Float) {
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher_foreground)
            ?: ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            ?: return

        val rect = RectF(x, y, x + size, y + size)
        val cornerRadius = size * 0.2f
        val path = Path().apply {
            addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        }

        canvas.withClip(path) {
            val offset = size * 0.25f
            drawable.setBounds(
                (x - offset).toInt(),
                (y - offset).toInt(),
                (x + size + offset).toInt(),
                (y + size + offset).toInt()
            )

            drawable.draw(this)
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }

    // Utilitaire pour calculer la réduction de l'image
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // Modifier drawMultilineText pour accepter une largeur personnalisée
    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        customWidth: Float? = null
    ): Float {
        var currentY = y
        val maxWidth = customWidth ?: (contentWidth - 100f)
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
}