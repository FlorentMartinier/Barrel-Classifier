package com.fmartinier.barrelclassifier.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import com.fmartinier.barrelclassifier.R
import com.fmartinier.barrelclassifier.data.DatabaseHelper
import com.fmartinier.barrelclassifier.data.dao.BarrelDao
import com.fmartinier.barrelclassifier.data.model.Barrel
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class AddBarrelDialog : DialogFragment() {

    private lateinit var edtBarrelName: EditText
    private lateinit var edtVolume: EditText
    private lateinit var edtBrand: MaterialAutoCompleteTextView
    private lateinit var edtWoodType: MaterialAutoCompleteTextView
    private lateinit var toggle: TextView
    private lateinit var advancedLayout: LinearLayout
    private lateinit var edtHeatType: MaterialAutoCompleteTextView
    private lateinit var edtHumidity: EditText
    private lateinit var edtTemperature: EditText
    private lateinit var barrel: Barrel
    private var barrelId: Long? = null
    private var modificationMode = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_add_barrel, null)

        edtBarrelName = view.findViewById(R.id.edtBarrelName)
        edtVolume = view.findViewById(R.id.edtVolume)
        edtBrand = view.findViewById<MaterialAutoCompleteTextView>(R.id.autoCompleteBrand)
        edtWoodType = view.findViewById<MaterialAutoCompleteTextView>(R.id.autoCompleteWood)
        toggle = view.findViewById<TextView>(R.id.txtToggleAdvanced)
        advancedLayout = view.findViewById<LinearLayout>(R.id.layoutAdvanced)
        edtHeatType = view.findViewById<MaterialAutoCompleteTextView>(R.id.edtHeatType)
        edtHumidity = view.findViewById(R.id.edtHumidity)
        edtTemperature = view.findViewById(R.id.edtTemperature)

        val brandAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            BARREL_BRANDS
        )
        val woodTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.wood_types_array)
        )
        val heatingTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.heating_types_array)
        )

        edtBrand.setAdapter(brandAdapter)
        edtWoodType.setAdapter(woodTypeAdapter)
        edtHeatType.setAdapter(heatingTypeAdapter)

        edtBrand.setOnClickListener {
            edtBrand.showDropDown()
        }

        edtWoodType.setOnClickListener {
            edtWoodType.showDropDown()
        }

        edtHeatType.setOnClickListener {
            edtHeatType.showDropDown()
        }

        toggle.setOnClickListener {
            if (advancedLayout.isGone) {
                advancedLayout.visibility = View.VISIBLE
                toggle.text = getString(R.string.advanced_option_up)
            } else {
                advancedLayout.visibility = View.GONE
                toggle.text = getString(R.string.advanced_option_down)
            }
        }

        // Pré-remplissage
        arguments
            ?.takeIf { it.containsKey(ARG_BARREL_ID) }
            ?.getLong(ARG_BARREL_ID)
            ?.let {
                modificationMode = true
                barrelId = it
                val db = DatabaseHelper(requireContext())
                barrel = BarrelDao(db).getById(it)
                edtBarrelName.setText(barrel.name)
                edtVolume.setText(barrel.volume.toString())
                edtBrand.setText(barrel.brand)
                edtWoodType.setText(barrel.woodType)
                edtHeatType.setText(barrel.heatType)
                edtHumidity.setText(barrel.storageHygrometer)
                edtTemperature.setText(barrel.storageTemperature)
            }

        val dialogTitle = if (modificationMode) R.string.modify_barrel else R.string.add_barrel
        val positiveButtonTitle = if (modificationMode) R.string.modify else R.string.add
        return AlertDialog.Builder(requireContext())
            .setTitle(getString(dialogTitle))
            .setView(view)
            .setPositiveButton(getString(positiveButtonTitle), null)
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

            // Options avancées
            val heatType = edtHeatType.text?.toString()?.trim()
            val humidity = edtHumidity.text?.toString()?.trim()
            val temperature = edtTemperature.text?.toString()?.trim()

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
                        id = this.barrelId ?: 0,
                        name = name,
                        volume = volumeText.toInt(),
                        brand = brand,
                        woodType = woodType,
                        imagePath = null,
                        heatType = heatType,
                        storageHygrometer = humidity,
                        storageTemperature = temperature,
                        histories = emptyList()
                    )

                    val db = DatabaseHelper(requireContext())
                    val barrelDao = BarrelDao(db)
                    if (modificationMode) {
                        barrelDao.update(barrel)
                    } else {
                        barrelDao.insert(barrel)
                    }

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
        private const val ARG_BARREL_ID = "barrel_id"

        private val BARREL_BRANDS =
            arrayOf("Allary", "Navarre", "Seguin Moreau", "Taransaud", "Radoux", "Damy").sorted()

        fun newInstance(barrelId: Long? = null): AddBarrelDialog {
            return AddBarrelDialog().apply {
                arguments = Bundle().apply {
                    barrelId?.let {
                        putLong(ARG_BARREL_ID, barrelId)
                    }
                }
            }
        }
    }
}
