package com.fmartinier.barrelclassifier.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.data.model.Barrel

class EditBarrelDialog : DialogFragment() {

    private lateinit var edtBarrelName: EditText
    private lateinit var edtVolume: EditText
    private lateinit var edtBrand: EditText
    private lateinit var edtWoodType: EditText
    private lateinit var barrel: Barrel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_add_barrel, null)

        val barrelId = requireArguments().getLong(ARG_BARREL_ID)
        val db = DatabaseHelper(requireContext())
        barrel = BarrelDao(db).getById(barrelId)
        edtBarrelName = view.findViewById(R.id.edtBarrelName)
        edtVolume = view.findViewById(R.id.edtVolume)
        edtBrand = view.findViewById(R.id.edtBrand)
        edtWoodType = view.findViewById(R.id.edtWoodType)

        // Pré-remplissage
        edtBarrelName.setText(barrel.name)
        edtVolume.setText(barrel.volume.toString())
        edtBrand.setText(barrel.brand)
        edtWoodType.setText(barrel.woodType)

        return AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.modify_barrel))
            .setView(view)
            .setPositiveButton(getString(R.string.modify), null) // IMPORTANT
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as AlertDialog
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        positiveButton.setOnClickListener {

            val name = edtBarrelName.text.toString().trim()
            val volumeText = edtVolume.text.toString().trim()
            val brand = edtBrand.text.toString().trim()
            val woodType = edtWoodType.text.toString().trim()

            when {
                name.isEmpty() -> {
                    showToast(getString(R.string.barrel_name) + " " + getString(R.string.required))
                }

                volumeText.isEmpty() -> {
                    showToast(getString(R.string.barrel_volume) + " " + getString(R.string.required))
                }

                brand.isEmpty() -> {
                    showToast(getString(R.string.brand) + " " + getString(R.string.required))
                }

                woodType.isEmpty() -> {
                    showToast(getString(R.string.wood_type) + " " + getString(R.string.required))
                }

                else -> {
                    val updatedBarrel = barrel.copy(
                        name = name,
                        volume = volumeText.toInt(),
                        brand = brand,
                        woodType = woodType
                    )

                    BarrelDao(DatabaseHelper(requireContext()))
                        .update(updatedBarrel)

                    parentFragmentManager.setFragmentResult(
                        "add_barrel_result",
                        Bundle.EMPTY
                    )
                    dismiss() // 👈 fermeture contrôlée
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val TAG = "EditBarrelDialog"
        private const val ARG_BARREL_ID = "barrel_id"

        fun newInstance(barrelId: Long): EditBarrelDialog {
            return EditBarrelDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_BARREL_ID, barrelId)
                }
            }
        }
    }
}
