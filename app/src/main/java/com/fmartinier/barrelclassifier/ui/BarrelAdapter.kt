package com.fmartinier.barrelclassifier.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.ListAdapter
import coil.load
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.fmartinier.barrelclassifier.data.model.BarrelDiffCallback
import com.fmartinier.barrelclassifier.ui.model.BarrelViewHolder
import java.io.File

class BarrelAdapter(
    private val context: Activity,
    private var isGrid: Boolean,
    val barrelFullViewBinder: BarrelFullViewBinder,
    private val onBarrelClick: (Intent, ActivityOptionsCompat?) -> Unit,
) : ListAdapter<Barrel, BarrelViewHolder>(BarrelDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return if (isGrid) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarrelViewHolder {
        val layout = if (viewType == 0) R.layout.item_barrel_grid else R.layout.item_barrel
        return BarrelViewHolder(LayoutInflater.from(context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: BarrelViewHolder, position: Int) {
        val barrel = getItem(position)
        holder.txtBarrelName.text = barrel.name

        if (barrel.imagePath == null) {
            holder.imgBarrel.setImageResource(R.drawable.ic_barrel_placeholder)
        } else {
            holder.imgBarrel.load(File(barrel.imagePath)) {
                crossfade(true) // Petite animation de fondu pour la fluidité
                placeholder(R.drawable.ic_barrel_placeholder)
                error(R.drawable.ic_barrel_placeholder)
            }
            holder.imgBarrel.setImageBitmap(BitmapFactory.decodeFile(barrel.imagePath))
        }

        if (isGrid) {
            holder.txtBarrelDetails?.text = barrel.brand
            holder.txtBarrelVolume?.text = "${barrel.volume}L"
            holder.itemView.setOnClickListener {
                val intent = Intent(context, BarrelDetailActivity::class.java)
                intent.putExtra("BARREL_ID", barrel.id)

                // 2. Préparer l'animation de transition
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    context,
                    holder.itemView, // L'élément qui se transforme
                    "barrel_detail_transition" // Nom unique de la transition
                )
                onBarrelClick(intent, options)
            }
        } else {
            barrelFullViewBinder.bind(
                holder = holder,
                barrel = barrel
            )
        }
    }

    fun updateData(newBarrels: List<Barrel>) {
        submitList(newBarrels)
    }

    fun updateLayout(isGrid: Boolean) {
        this.isGrid = isGrid
        notifyDataSetChanged()
    }
}