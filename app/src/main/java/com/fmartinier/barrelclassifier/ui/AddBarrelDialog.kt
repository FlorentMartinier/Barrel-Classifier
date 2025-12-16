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

        val edtNom = view.findViewById<EditText>(R.id.edtBarrelName)
        val edtContenance = view.findViewById<EditText>(R.id.edtContenance)
        val edtMarque = view.findViewById<EditText>(R.id.edtMarque)
        val edtTypeBois = view.findViewById<EditText>(R.id.edtTypeBois)

        return AlertDialog.Builder(requireContext())
            .setTitle("Ajouter un fût")
            .setView(view)
            .setPositiveButton("Ajouter") { _, _ ->
                val fut = Barrel(
                    name = edtNom.text.toString(),
                    volume = edtContenance.text.toString().toInt(),
                    brand = edtMarque.text.toString(),
                    woodType = edtTypeBois.text.toString(),
                    imagePath = null,
                    histories = listOf()
                )

                val db = DatabaseHelper(requireContext())
                BarrelDao(db).insert(fut)

                onBarrelAdded()
            }
            .setNegativeButton("Annuler", null)
            .create()
    }
}
