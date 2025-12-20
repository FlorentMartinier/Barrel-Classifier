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

        val edtNom = view.findViewById<EditText>(R.id.edtBarrelName)
        val edtContenance = view.findViewById<EditText>(R.id.edtContenance)
        val edtMarque = view.findViewById<EditText>(R.id.edtMarque)
        val edtTypeBois = view.findViewById<EditText>(R.id.edtTypeBois)

        // Pré-remplissage
        edtNom.setText(barrel.name)
        edtContenance.setText(barrel.volume.toString())
        edtMarque.setText(barrel.brand)
        edtTypeBois.setText(barrel.woodType)

        return AlertDialog.Builder(requireContext())
            .setTitle(requireContext().resources.getString(R.string.modify_barrel))
            .setView(view)
            .setPositiveButton(requireContext().resources.getString(R.string.modify)) { _, _ ->
                val updatedBarrel = barrel.copy(
                    name = edtNom.text.toString(),
                    volume = edtContenance.text.toString().toInt(),
                    brand = edtMarque.text.toString(),
                    woodType = edtTypeBois.text.toString()
                )

                val db = DatabaseHelper(requireContext())
                BarrelDao(db).update(updatedBarrel)

                onBarrelUpdated()
            }
            .setNegativeButton(requireContext().resources.getString(R.string.cancel), null)
            .create()
    }
}
