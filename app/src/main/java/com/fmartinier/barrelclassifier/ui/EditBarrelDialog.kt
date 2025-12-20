package com.fmartinier.barrelclassifier.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.data.model.Barrel

class EditBarrelDialog(
    private val barrel: Barrel,
    private val onBarrelUpdated: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_barrel, null)

        val edtBarrelName = view.findViewById<EditText>(R.id.edtBarrelName)
        val edtVolume = view.findViewById<EditText>(R.id.edtVolume)
        val edtBrand = view.findViewById<EditText>(R.id.edtBrand)
        val edtWoodType = view.findViewById<EditText>(R.id.edtWoodType)

        // Pré-remplissage
        edtBarrelName.setText(barrel.name)
        edtVolume.setText(barrel.volume.toString())
        edtBrand.setText(barrel.brand)
        edtWoodType.setText(barrel.woodType)

        return AlertDialog.Builder(requireContext())
            .setTitle(requireContext().resources.getString(R.string.modify_barrel))
            .setView(view)
            .setPositiveButton(requireContext().resources.getString(R.string.modify)) { _, _ ->
                val updatedBarrel = barrel.copy(
                    name = edtBarrelName.text.toString(),
                    volume = edtVolume.text.toString().toInt(),
                    brand = edtBrand.text.toString(),
                    woodType = edtWoodType.text.toString()
                )

                val db = DatabaseHelper(requireContext())
                BarrelDao(db).update(updatedBarrel)

                onBarrelUpdated()
            }
            .setNegativeButton(requireContext().resources.getString(R.string.cancel), null)
            .create()
    }
}
