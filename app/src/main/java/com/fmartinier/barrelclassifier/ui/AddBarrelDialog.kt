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

class AddBarrelDialog(
    private val onBarrelAdded: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_barrel, null)

        val edtBarrelName = view.findViewById<EditText>(R.id.edtBarrelName)
        val edtVolume = view.findViewById<EditText>(R.id.edtVolume)
        val edtBrand = view.findViewById<EditText>(R.id.edtBrand)
        val edtWoodType = view.findViewById<EditText>(R.id.edtWoodType)

        return AlertDialog.Builder(requireContext())
            .setTitle(requireContext().resources.getString(R.string.add_barrel))
            .setView(view)
            .setPositiveButton(requireContext().resources.getString(R.string.add)) { _, _ ->
                val barrel = Barrel(
                    name = edtBarrelName.text.toString(),
                    volume = edtVolume.text.toString().toInt(),
                    brand = edtBrand.text.toString(),
                    woodType = edtWoodType.text.toString(),
                    imagePath = null,
                    histories = listOf()
                )

                val db = DatabaseHelper(requireContext())
                BarrelDao(db).insert(barrel)

                onBarrelAdded()
            }
            .setNegativeButton(requireContext().resources.getString(R.string.cancel), null)
            .create()
    }
}
