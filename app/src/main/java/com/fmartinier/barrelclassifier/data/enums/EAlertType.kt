package com.fmartinier.barrelclassifier.data.enums

import com.fmartinier.barrelclassifier.R

enum class EAlertType(val alertDescription: Int) {
    VERIFY(R.string.alert_verify_description), // Vérification générale
    BOTTLE_UP(R.string.alert_bottle_up_description), // Embouteiller
    TASTE(R.string.alert_taste_description), // Gouter
    CHANGE(R.string.alert_change_description), // Changer de fût
    TOPPING_UP(R.string.alert_topping_up_description), // Ouillage (reremplissage)
    MOVE(R.string.alert_move_description), // Déplacer le fût
    RACKING(R.string.alert_racking_description), // Soutirer
    LEES_STIRRING(R.string.alert_lees_stirring_description), // Batonner
}