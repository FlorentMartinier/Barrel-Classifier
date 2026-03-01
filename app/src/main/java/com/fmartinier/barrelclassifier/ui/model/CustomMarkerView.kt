package com.fmartinier.barrelclassifier.ui.model

import android.content.Context
import android.widget.TextView
import com.fmartinier.barrelclassifier.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class CustomMarkerView(
    context: Context,
    layoutResource: Int = R.layout.layout_marker_view,
    val unit: String = ""
) : MarkerView(context, layoutResource) {

    constructor(context: Context) : this(context, R.layout.layout_marker_view, "")

    private val tvContent: TextView = findViewById(R.id.markerContent)

    // Cette méthode est appelée à chaque fois que le marqueur est redessiné
    override fun refreshContent(e: Entry, highlight: Highlight) {
        // e.y contient la valeur (%, etc.), e.x contient le temps (mois)
        tvContent.text = String.format("%.1f %s", e.y, unit)

        // Appelle la méthode parente pour ajuster la taille
        super.refreshContent(e, highlight)
    }

    // Ajuste la position pour que la bulle soit centrée au-dessus du point
    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }
}