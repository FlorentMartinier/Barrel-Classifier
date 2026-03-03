package com.fmartinier.barrelclassifier.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.History
import com.fmartinier.barrelclassifier.ui.BarrelAdapter.BarrelViewHolder
import com.fmartinier.barrelclassifier.ui.model.CustomMarkerView
import com.fmartinier.barrelclassifier.utils.DateUtils
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.calculateNbDaysHistory
import com.fmartinier.barrelclassifier.utils.DateUtils.Companion.getEquivalenceRatio
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlin.math.exp
import kotlin.math.pow
import com.fmartinier.barrelclassifier.utils.BarrelUtils.Companion.STANDARD_BARREL_VOLUME
import com.fmartinier.barrelclassifier.utils.TooltipUtils

class StatisticsDrawer(
    val context: Context,
) {
    private lateinit var layoutRecordDuration: LinearLayout
    private lateinit var layoutTotalDuration: LinearLayout
    private lateinit var layoutPieChartTypes: LinearLayout
    private lateinit var layoutProgressStrength: LinearLayout
    private lateinit var layoutBarChartTypes: LinearLayout
    private lateinit var textProgressStrength: TextView
    private lateinit var txtRecordDuration: TextView
    private lateinit var txtTotalDuration: TextView
    private lateinit var txtAvgAlcoholicStrength: TextView
    private lateinit var progressAbv: ProgressBar
    private lateinit var pieChartTypes: PieChart
    private lateinit var txtAvgAngelShare: TextView
    private lateinit var progressAngelShare: ProgressBar
    private lateinit var layoutProgressAngelShare: LinearLayout
    private lateinit var textProgressAngelShare: TextView
    private lateinit var textRepartitionType: TextView
    private lateinit var textRepartitionDuration: TextView
    private lateinit var barChart: HorizontalBarChart
    private lateinit var tanninChart: LineChart
    private lateinit var verdictText: TextView
    private lateinit var tanninChartTitle: TextView
    private lateinit var angelsVerdictText: TextView
    private lateinit var angelsChart: LineChart
    private lateinit var angelsChartTitle: TextView

    val colorList = listOf(
        ContextCompat.getColor(context, R.color.chart_primary),
        ContextCompat.getColor(context, R.color.chart_secondary),
        ContextCompat.getColor(context, R.color.chart_tertiary),
    )

    fun displayAllForBarrel(holder: BarrelViewHolder, barrel: Barrel) {
        val view = holder.layoutStats
        layoutRecordDuration = view.findViewById<LinearLayout>(R.id.layoutRecordDuration)
        layoutTotalDuration = view.findViewById<LinearLayout>(R.id.layoutTotalDuration)
        layoutPieChartTypes = view.findViewById<LinearLayout>(R.id.layoutPieChartTypes)
        layoutProgressStrength = view.findViewById<LinearLayout>(R.id.layoutProgressStrength)
        layoutBarChartTypes = view.findViewById<LinearLayout>(R.id.layoutBarChartTypes)
        textProgressStrength = view.findViewById<TextView>(R.id.textProgressStrength)
        txtRecordDuration = view.findViewById<TextView>(R.id.txtRecordDuration)
        txtTotalDuration = view.findViewById<TextView>(R.id.txtTotalDuration)
        txtAvgAlcoholicStrength = view.findViewById<TextView>(R.id.txtAvgAlcoholicStrength)
        progressAbv = view.findViewById<ProgressBar>(R.id.progressAbv)
        pieChartTypes = view.findViewById<PieChart>(R.id.pieChartTypes)
        txtAvgAngelShare = view.findViewById<TextView>(R.id.txtAvgAngelShare)
        progressAngelShare = view.findViewById<ProgressBar>(R.id.progressAngelShare)
        layoutProgressAngelShare = view.findViewById<LinearLayout>(R.id.layoutProgressAngelShare)
        textProgressAngelShare = view.findViewById<TextView>(R.id.textProgressAngelShare)
        textRepartitionType = view.findViewById<TextView>(R.id.textRepartitionType)
        textRepartitionDuration = view.findViewById<TextView>(R.id.textRepartitionDuration)
        barChart = view.findViewById<HorizontalBarChart>(R.id.barChartAvgDuration)
        tanninChart = view.findViewById<LineChart>(R.id.tanninChart)
        verdictText = view.findViewById<TextView>(R.id.verdictText)
        tanninChartTitle = view.findViewById<TextView>(R.id.tanninChartTitle)
        angelsVerdictText = view.findViewById<TextView>(R.id.angelsVerdictText)
        angelsChart = view.findViewById<LineChart>(R.id.angelsChart)
        angelsChartTitle = view.findViewById<TextView>(R.id.angelsChartTitle)

        val histories = barrel.histories
        if (histories.isEmpty()) {
            layoutRecordDuration.visibility = View.GONE
            layoutTotalDuration.visibility = View.GONE
            layoutPieChartTypes.visibility = View.GONE
            layoutProgressStrength.visibility = View.GONE
            textProgressStrength.visibility = View.GONE
            layoutBarChartTypes.visibility = View.GONE
            textProgressAngelShare.visibility = View.GONE
            layoutProgressAngelShare.visibility = View.GONE
            textRepartitionType.visibility = View.GONE
            textRepartitionDuration.visibility = View.GONE
        } else {
            layoutRecordDuration.visibility = View.VISIBLE
            layoutTotalDuration.visibility = View.VISIBLE
            layoutPieChartTypes.visibility = View.VISIBLE
            layoutProgressStrength.visibility = View.VISIBLE
            textProgressStrength.visibility = View.VISIBLE
            layoutBarChartTypes.visibility = View.VISIBLE
            textProgressAngelShare.visibility = View.VISIBLE
            layoutProgressAngelShare.visibility = View.VISIBLE
            textRepartitionType.visibility = View.VISIBLE
            textRepartitionDuration.visibility = View.VISIBLE
        }

        // 1. Calculs de base
        val totalDays = histories.sumOf {
            DateUtils.calculateNbDaysBetweenDates(it.beginDate, it.endDate)
        }
        val recordDays = histories.maxOfOrNull {
            DateUtils.calculateNbDaysBetweenDates(it.beginDate, it.endDate)
        } ?: 0

        // 2. Affichage textes
        txtTotalDuration.text = context.resources.getString(R.string.nb_days, totalDays.toString())
        txtRecordDuration.text =
            context.resources.getString(R.string.nb_days, recordDays.toString())

        setupAlcohol(histories)
        setupAngelShare(histories)
        setupAgeingDistribution(histories)
        setupAverageAgeingDuration(histories)
        setupTanninEstimation(barrel.volume.toDouble(), barrel)
        setupAngelsShareEstimation(barrel, barrel.volume.toDouble())
    }

    private fun setupAlcohol(histories: List<History>) {
        val averageAlcoholicStrength = histories
            .mapNotNull { it.alcoholicStrength }
            .filter { !it.isEmpty() }
            .map { it.toFloat() }
            .average()

        if (averageAlcoholicStrength.isNaN()) {
            textProgressStrength.visibility = View.GONE
            layoutProgressStrength.visibility = View.GONE
        } else {
            layoutProgressStrength.visibility = View.VISIBLE
            textProgressStrength.visibility = View.VISIBLE
            txtAvgAlcoholicStrength.text = "$averageAlcoholicStrength %"
            progressAbv.progress = averageAlcoholicStrength.toInt()
        }
    }

    private fun setupAngelShare(histories: List<History>) {
        val averageAngelsShare = histories
            .mapNotNull { it.angelsShare }
            .filter { !it.isEmpty() }
            .map { it.toFloat() }
            .average()

        if (averageAngelsShare.isNaN()) {
            textProgressAngelShare.visibility = View.GONE
            layoutProgressAngelShare.visibility = View.GONE
        } else {
            layoutProgressAngelShare.visibility = View.VISIBLE
            textProgressAngelShare.visibility = View.VISIBLE
            txtAvgAngelShare.text = "$averageAngelsShare %"
            progressAngelShare.progress = averageAngelsShare.toInt()
        }
    }

    private fun setupAgeingDistribution(histories: List<History>) {
        val typeCounts = histories.groupingBy { it.type }.eachCount()
        val pieEntries = typeCounts.map { PieEntry(it.value.toFloat(), it.key) }

        val pieDataSet = PieDataSet(pieEntries, "").apply {
            colors = colorList
            valueTextSize = 10f
            setDrawValues(true)
            valueFormatter = PercentFormatter(pieChartTypes)
        }
        pieChartTypes.apply {
            data = PieData(pieDataSet)
            setDrawEntryLabels(false)
            setUsePercentValues(true)
            description.isEnabled = false
            legend.apply {
                isEnabled = true
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                textColor = ContextCompat.getColor(context, R.color.chart_primary)
            }
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            invalidate()
        }
    }

    private fun setupAverageAgeingDuration(histories: List<History>) {
        val avgDurations = histories.groupBy { it.type }.mapValues { entry ->
            entry.value.sumOf { h ->
                val end = h.endDate ?: System.currentTimeMillis()
                (end - h.beginDate)
            } / entry.value.size / (1000 * 60 * 60 * 24)
        }

        val barEntries = avgDurations.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        val barDataSet =
            BarDataSet(barEntries, "").apply {
                colors = colorList
                valueTextColor = ContextCompat.getColor(context, R.color.chart_primary)
                valueTextSize = 10f
                setDrawValues(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${value.toInt()} j"
                    }
                }
            }

        barChart.apply {
            setExtraOffsets(0f, 0f, 20f, 0f)
            description.isEnabled = false
            legend.isEnabled = false
            setDrawValueAboveBar(true)
            setDrawGridBackground(false)
            data = BarData(barDataSet)
            setMaxVisibleValueCount(1000)
            clipChildren = false
            data.setValueTextColor(ContextCompat.getColor(context, R.color.chart_primary))

            // Configuration des axes pour un rendu horizontal propre
            xAxis.apply {
                isEnabled = true
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(context, R.color.chart_primary)
                valueFormatter =
                    IndexAxisValueFormatter(avgDurations.keys.toList())
                granularity = 1f
            }

            axisLeft.apply {
                isEnabled = false
                axisMinimum = 0f
            }

            axisRight.apply {
                isEnabled = true
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                textColor = ContextCompat.getColor(context, R.color.chart_primary)
                axisMinimum = 0f
            }

            setFitBars(true)
            animateY(1000)
            invalidate()
        }
    }

    private fun setupTanninEstimation(barrelVolume: Double, barrel: Barrel) {
        val histories = barrel.histories
        val historyNdDays = histories
            .filter { it.endDate != null } // Le calcul doit sur faire uniquement sur les historiques précédents. Sinon la courbe va bouger tous les jours.
            .sumOf { calculateNbDaysHistory(it) }

        tanninChartTitle.setOnClickListener {
            TooltipUtils.createTooltip(context, context.getString(R.string.tannin_estimation_tooltip, barrelVolume.toInt().toString(), historyNdDays.toString())).showAlignTop(it)
        }

        val maxMonths = when {
            barrelVolume <= 5 -> 24f
            barrelVolume <= 20 -> 60f
            barrelVolume <= 100 -> 120f
            else -> 240f
        }

        // Calculs de base
        val barrelPotency = calculatePotency(historyNdDays, barrelVolume)
        val accelFactor = (STANDARD_BARREL_VOLUME / barrelVolume).pow(0.33).toFloat()
        val speedFast = 0.4f * accelFactor
        val speedSlow = 0.005f
        val oxidationAccel = (STANDARD_BARREL_VOLUME / barrelVolume).pow(0.2).toFloat()
        val oxidationSpeed = 0.02f * oxidationAccel

        val extractionEntries = ArrayList<Entry>()
        val oxidationEntries = ArrayList<Entry>()

        for (month in 0..maxMonths.toInt()) {
            val x = month.toFloat()
            val extractionY = (100 * barrelPotency * (1 - exp(-speedFast * x)) +
                    100 * (1 - barrelPotency) * (1 - exp(-speedSlow * x))).toFloat()
            val oxidationY = (100 * (1 - exp(-oxidationSpeed * x))).toFloat()

            extractionEntries.add(Entry(x, extractionY))
            oxidationEntries.add(Entry(x, oxidationY))
        }

        // Sets de données
        val extractionSet = LineDataSet(extractionEntries, context.getString(R.string.wood)).apply {
            color = ContextCompat.getColor(context, R.color.chart_primary)
            setDrawCircles(false)
            lineWidth = 3f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(false)
        }

        val oxidationSet = LineDataSet(oxidationEntries, context.getString(R.string.oxydation)).apply {
            color = ContextCompat.getColor(context, R.color.chart_secondary)
            setDrawCircles(false)
            lineWidth = 3f
            enableDashedLine(10f, 5f, 0f)
            setDrawValues(false)
        }

        var currentSet: LineDataSet? = null

        barrel.getCurrentHistory()?.let {
            val currentDays = calculateNbDaysHistory(it)
            val currentMonths = currentDays / 30.44f
            val currentY = (100 * barrelPotency * (1 - exp(-speedFast * currentMonths)) +
                    100 * (1 - barrelPotency) * (1 - exp(-speedSlow * currentMonths))).toFloat()

            currentSet = LineDataSet(listOf(Entry(currentMonths, currentY)), "").apply {
                setDrawCircles(true)
                circleRadius = 8f
                circleHoleRadius = 5f
                setCircleColor(ContextCompat.getColor(context, R.color.chart_primary))
                circleHoleColor = Color.WHITE
                setDrawValues(true)
                valueTextSize = 11f
                valueTextColor = ContextCompat.getColor(context, R.color.chart_primary)
                valueFormatter = object : ValueFormatter() {
                    override fun getPointLabel(entry: Entry?): String =
                        context.getString(R.string.current_ageing)
                }
                form = Legend.LegendForm.NONE
                lineWidth = 0f
            }

            // Mise à jour du verdict avec style
            this.verdictText.visibility = View.VISIBLE
            val verdict = generateVerdict(currentMonths, currentY, (100 * (1 - exp(-oxidationSpeed * currentMonths))).toFloat())
            this.verdictText.text = context.getString(R.string.verdict, verdict)
        }
        val markerMaturation = CustomMarkerView(context, R.layout.layout_marker_view, "%")
        tanninChart.apply {
            setMaxVisibleValueCount(1000)
            marker = markerMaturation
            data = if (currentSet != null) LineData(extractionSet, oxidationSet, currentSet)
            else LineData(extractionSet, oxidationSet)

            axisRight.isEnabled = false
            description.isEnabled = false
            setExtraOffsets(5f, 15f, 5f, 20f)
            clipChildren = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = ContextCompat.getColor(context, R.color.chart_primary)
                setDrawGridLines(false)
                axisMinimum = 0f
                axisMaximum = maxMonths
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = "${value.toInt()}m"
                }
            }

            axisLeft.apply {
                textColor = ContextCompat.getColor(context, R.color.chart_primary)
                axisMinimum = 0f
                axisMaximum = 100f
                setLabelCount(5, true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = "${value.toInt()}%"
                }
            }

            legend.apply {
                textColor = ContextCompat.getColor(context, R.color.chart_primary)
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                xEntrySpace = 20f
            }

            // Animation douce pour rendre le résultat moins "brut"
            animateX(1000)
            invalidate()
        }
        markerMaturation.chartView = tanninChart
    }

    private fun generateVerdict(months: Float, extraction: Float, oxidation: Float): String {
        return when {
            months < 3 -> context.getString(R.string.start_phase)
            extraction > oxidation + 10 -> context.getString(R.string.ageing_phase)
            oxidation > extraction -> context.getString(R.string.maturation_phase)
            else -> context.getString(R.string.harmony_phase)
        }
    }

    fun calculatePotency(previousDaysTotal: Int, barrelVolume: Double): Double {
        if (barrelVolume <= 0.0) return 1.0

        // 1. On récupère le ratio d'accélération (ex: 4.54 pour 2L)
        val accelerationRatio = getEquivalenceRatio(barrelVolume)

        // 2. On calcule l'usure équivalente
        // 100 jours dans un 2L usent le bois autant que ~450 jours dans un 200L
        val equivalentDaysOfWear = previousDaysTotal * accelerationRatio

        // 2. Calcul du point d'épuisement dynamique
        // Un petit fût a moins de "réserve" de bois (douelles plus fines).
        // On peut estimer que la réserve croît avec la racine cubique du volume.
        val baseExhaustionStandard = 1825.0 // 5 ans pour un 200L
        val volumeCorrection = (barrelVolume / STANDARD_BARREL_VOLUME).pow(0.2) // Facteur d'épaisseur/réserve
        val dynamicExhaustionPoint = baseExhaustionStandard * volumeCorrection

        // 4. Calcul de la puissance restante (décroissance exponentielle)
        // On part de 1.0 (neuf) et on descend vers 0.1 (épuisé)
        val potency = exp(-1.0 * (equivalentDaysOfWear / dynamicExhaustionPoint))

        return potency.coerceAtLeast(0.1)
    }

    private fun setupAngelsShareEstimation(barrel: Barrel, barrelVolume: Double) {
        // 1. Récupération des données environnementales (ou valeurs par défaut)
        val storageTemperature = barrel.storageTemperature
        val storageHygrometer = barrel.storageHygrometer
        val temp = if(!storageTemperature.isNullOrEmpty()) {
            storageTemperature.toDouble()
        } else 15.0
        val humidity = if (!storageHygrometer.isNullOrEmpty()) {
            storageHygrometer.toDouble()
        } else 70.0

        angelsChartTitle.setOnClickListener {
            TooltipUtils.createTooltip(context, context.getString(
                R.string.angels_share_estimation_tooltip,
                barrelVolume.toString(),
                temp.toString(),
                humidity.toString()
            )).showAlignTop(it)
        }

        val maxMonths = when {
            barrelVolume <= 5 -> 24f
            barrelVolume <= 20 -> 60f
            barrelVolume <= 100 -> 120f
            else -> 240f
        }

        val entries = ArrayList<Entry>()

        // 2. Génération de la courbe
        for (month in 0..maxMonths.toInt()) {
            val loss = calculateAngelsShare(month.toFloat(), barrelVolume, temp, humidity)
            entries.add(Entry(month.toFloat(), loss.toFloat()))
        }

        val angelsSet = LineDataSet(entries, context.getString(R.string.evaporation)).apply {
            color = ContextCompat.getColor(context, R.color.chart_primary)
            setDrawCircles(false)
            lineWidth = 3f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(context, R.color.chart_primary)
            fillAlpha = 40
            setDrawValues(false)
        }

        // 3. Point Actuel
        var currentSet: LineDataSet? = null
        barrel.getCurrentHistory()?.let {
            val currentMonths = calculateNbDaysHistory(it) / 30.44f
            val currentLoss = calculateAngelsShare(currentMonths, barrelVolume, temp, humidity)

            currentSet = LineDataSet(listOf(Entry(currentMonths, currentLoss.toFloat())), "").apply {
                setDrawCircles(true)
                circleRadius = 8f
                circleHoleRadius = 5f
                setCircleColor(ContextCompat.getColor(context, R.color.chart_primary))
                circleHoleColor = Color.WHITE
                setDrawValues(true)
                valueTextSize = 11f
                valueTextColor = ContextCompat.getColor(context, R.color.chart_primary)
                valueFormatter = object : ValueFormatter() {
                    override fun getPointLabel(entry: Entry?): String =
                        context.getString(R.string.estimated_loss)
                }
                form = Legend.LegendForm.NONE
            }

            // Affichage du verdict
            angelsVerdictText.visibility = View.VISIBLE
            angelsVerdictText.text = generateAngelsVerdict(temp, humidity, currentLoss)
        }

        // 4. Configuration du Chart
        val markerAngels = CustomMarkerView(context, R.layout.layout_marker_view, "%")
        angelsChart.apply {
            setMaxVisibleValueCount(1000)
            marker = markerAngels
            data = if (currentSet != null) LineData(angelsSet, currentSet) else LineData(angelsSet)

            description.isEnabled = false
            axisRight.isEnabled = false
            setExtraOffsets(5f, 15f, 5f, 25f)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                axisMinimum = 0f
                axisMaximum = maxMonths
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = "${value.toInt()}m"
                }
                textColor = ContextCompat.getColor(context, R.color.chart_primary)
            }

            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = if (entries.maxOf { it.y } > 15f) entries.maxOf { it.y } + 5f else 20f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = "${value.toInt()}%"
                }
                textColor = ContextCompat.getColor(context, R.color.chart_primary)
            }

            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                yOffset = 10f
                textColor = ContextCompat.getColor(context, R.color.chart_primary)
            }

            invalidate()
        }
        markerAngels.chartView = angelsChart
    }

    private fun generateAngelsVerdict(temp: Double, humidity: Double, loss: Double): String {
        // 1. Analyse de l'intensité (Vitesse)
        val intensity = when {
            temp > 22 -> context.getString(R.string.high_evaporation)
            temp < 12 -> context.getString(R.string.slow_and_preserved_ageing)
            else -> context.getString(R.string.controlled_evaporation)
        }

        // 2. Analyse du profil (Eau vs Alcool)
        val profile = when {
            humidity < 55 -> context.getString(R.string.dry_environment)
            humidity > 75 -> context.getString(R.string.damp_environment)
            else -> context.getString(R.string.balanced_humidity)
        }

        // 3. Alerte spécifique sur le volume (si la perte est déjà importante)
        val warning = if (loss > 20) context.getString(R.string.significant_volume_loss) else ""

        return "💡 $intensity\n💡 $profile$warning"
    }

    private fun calculateAngelsShare(
        months: Float,
        volume: Double,
        temp: Double? = 15.0, // Facultatif
        humidity: Double? = 70.0 // Facultatif
    ): Double {
        val t = temp ?: 15.0
        val h = humidity ?: 70.0

        // 1. Taux de base annuel (environ 2.5%)
        var annualRate = 0.025

        // 2. Ajustement selon le volume (Effet d'échelle)
        // Un 2L évapore environ 3 à 4 fois plus vite qu'un 200L
        val volumeFactor = (STANDARD_BARREL_VOLUME / volume).pow(0.25)
        annualRate *= volumeFactor

        // 3. Ajustement Température (loi simplifiée : +0.1% de perte par degré au dessus de 15°C)
        val tempFactor = 1.0 + (t - 15.0) * 0.05
        annualRate *= tempFactor

        // 4. Ajustement Hygrométrie
        // Plus c'est sec (< 70%), plus l'évaporation totale augmente légèrement
        val humidityFactor = 1.0 + (70.0 - h) * 0.005
        annualRate *= humidityFactor

        // 5. Calcul final composé (mois par mois)
        // Formule : Valeur * (1 - taux_mensuel)^nb_mois
        val monthlyRate = annualRate / 12.0
        val totalLossPercentage = (1.0 - (1.0 - monthlyRate).pow(months.toDouble())) * 100.0

        return totalLossPercentage
    }
}