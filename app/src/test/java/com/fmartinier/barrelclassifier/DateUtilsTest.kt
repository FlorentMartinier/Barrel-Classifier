package com.fmartinier.barrelclassifier

import com.fmartinier.barrelclassifier.utils.DateUtils
import org.junit.Test

import org.junit.Assert.*
import kotlin.math.abs

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class DateUtilsTest {
    @Test
    fun calculate_equivalence_with_margin() {
        // Liste de tests basés sur ton tableau : (jours, volume, cible_attendue)
        val testCases = listOf(
            Triple(58, 1.0, 365),
            Triple(80, 2.0, 365),
            Triple(90, 3.0, 365),
            Triple(105, 5.0, 365),
            Triple(134, 10.0, 365),
            Triple(173, 20.0, 365)
        )

        testCases.forEach { (days, volume, expected) ->
            val result = DateUtils.calculate228lEquivalentAge(days, volume)
            assertWithinTenPercent(expected, result, "Volume: $volume L")
        }
    }

    private fun assertWithinTenPercent(expected: Int, actual: Int, message: String) {
        val margin = expected * 0.10
        val diff = abs(expected - actual)

        assertTrue(
            "$message | Attendu: $expected, Obtenu: $actual (Diff: $diff, Max permis: $margin)",
            diff <= margin
        )
    }
}