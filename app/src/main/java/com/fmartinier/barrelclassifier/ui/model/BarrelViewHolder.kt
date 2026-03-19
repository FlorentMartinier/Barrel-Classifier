package com.fmartinier.barrelclassifier.ui.model

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fmartinier.barrelclassifier.R
import com.google.android.material.chip.ChipGroup

class BarrelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val txtBarrelName: TextView = itemView.findViewById(R.id.txtBarrelName)
    val imgBarrel: ImageView = itemView.findViewById(R.id.imgBarrel)
    val txtBarrelDetails: TextView? = itemView.findViewById(R.id.txtBarrelDetails)
    val txtBarrelVolume: TextView? = itemView.findViewById(R.id.txtBarrelVolume)
    val btnMenu: ImageButton? = itemView.findViewById(R.id.btnBarrelMenu)
    val photoOverlay: LinearLayout? = itemView.findViewById(R.id.photoOverlay)
    val layoutStats: LinearLayout? = itemView.findViewById(R.id.layoutStats)
    val layoutHistory: LinearLayout? = itemView.findViewById(R.id.layoutHistory)
    val layoutToggleStats: LinearLayout? = itemView.findViewById(R.id.layoutToggleStats)
    val layoutToggleHistory: LinearLayout? = itemView.findViewById(R.id.layoutToggleHistory)
    val imgChevronStats: ImageView? = itemView.findViewById(R.id.imgChevronStats)
    val imgChevronHistory: ImageView? = itemView.findViewById(R.id.imgChevronHistory)
    val btnAddHistory: TextView? = itemView.findViewById(R.id.btnAddHistory)
    val txtDescription: TextView? = itemView.findViewById(R.id.txtBarrelDescription)
    val txtExpandDescription: TextView? = itemView.findViewById(R.id.txtBarrelExpandDescription)
    val chipGroup: ChipGroup? = itemView.findViewById<ChipGroup>(R.id.chipGroupAdvanced)
    val txtNextAlertDate: TextView? = itemView.findViewById<TextView>(R.id.txtNextAlertDate)
    val layoutNextAlert: LinearLayout? = itemView.findViewById<LinearLayout>(R.id.layoutNextAlert)

    var isHistoryExpanded: Boolean = false
    var isStatsExpanded: Boolean = false
}