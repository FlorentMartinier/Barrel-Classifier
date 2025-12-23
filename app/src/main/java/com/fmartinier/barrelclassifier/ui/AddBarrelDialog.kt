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

class AddBarrelDialog : DialogFragment() {

    private lateinit var edtBarrelName: EditText
    private lateinit var edtVolume: EditText
    private lateinit var edtBrand: EditText
    private lateinit var edtWoodType: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_add_barrel, null)

        edtBarrelName = view.findViewById(R.id.edtBarrelName)
        edtVolume = view.findViewById(R.id.edtVolume)
        edtBrand = view.findViewById(R.id.edtBrand)
        edtWoodType = view.findViewById(R.id.edtWoodType)

        return AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_barrel))
            .setView(view)
            .setPositiveButton(getString(R.string.add), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as AlertDialog
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        positiveButton.setOnClickListener {

            // 🔍 Récupération des valeurs
            val name = edtBarrelName.text.toString().trim()
            val volumeText = edtVolume.text.toString().trim()
            val brand = edtBrand.text.toString().trim()
            val woodType = edtWoodType.text.toString().trim()

            // ❌ Validation
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
                    // ✅ Tout est valide → insertion
                    val barrel = Barrel(
                        name = name,
                        volume = volumeText.toInt(),
                        brand = brand,
                        woodType = woodType,
                        imagePath = null,
                        histories = emptyList()
                    )

                    val db = DatabaseHelper(requireContext())
                    BarrelDao(db).insert(barrel)

                    parentFragmentManager.setFragmentResult(
                        "add_barrel_result",
                        Bundle.EMPTY
                    )
                    dismiss() // 👈 fermeture MANUELLE
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val TAG = "AddBarrelDialog"

        fun newInstance(): AddBarrelDialog {
            return AddBarrelDialog()
        }
    }
}
