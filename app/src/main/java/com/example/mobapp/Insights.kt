package com.example.mobapp

import kotlin.math.abs

/**
 * Produces short, human-readable insight sentences for a chronological daily-value series
 * (one value per day, most recent last). Used to power the "Insights" cards on the
 * Screen / Water / Habits tabs — the same shape that already feeds the 7-day trend chart.
 *
 * Scope is intentionally narrow so every tab shares the same analysis:
 *   - Peak day      — when the majority of activity happened.
 *   - Weekly trend  — first half of the window vs second half.
 *   - Biggest swing — the most striking day-over-day spike or drop.
 *
 * Empty / all-zero series get a single "start logging" fallback so the card isn't blank.
 */
object Insights {

    /**
     * @param labels short day labels aligned with [values] (e.g. ["Mon", "Tue", ..., "Today"]).
     * @param values numeric daily totals in the same unit throughout.
     * @param noun   plural noun used in the empty-state fallback (e.g. "hydration data").
     * @param formatValue formats a single day's value for display (e.g. "3 glasses", "2h 15m").
     * @return 1..4 short sentences. Never empty.
     */
    fun summarize(
        labels: List<String>,
        values: List<Double>,
        noun: String,
        formatValue: (Double) -> String
    ): List<String> {
        if (values.isEmpty() || values.all { it <= 0.0 }) {
            return listOf("No $noun yet — start logging to unlock insights.")
        }

        val out = mutableListOf<String>()

        // --- 1. Peak day ---------------------------------------------------------------
        val maxIdx = values.indices.maxByOrNull { values[it] } ?: 0
        val peakLabel = labels.getOrNull(maxIdx) ?: "recently"
        out += "Most activity on $peakLabel (${formatValue(values[maxIdx])})."

        // --- 2. Weekly trend: first half avg vs second half avg ------------------------
        if (values.size >= 4) {
            val half = values.size / 2
            val firstAvg = values.take(half).average()
            val secondAvg = values.drop(values.size - half).average()
            when {
                firstAvg > 0.0 -> {
                    val pct = ((secondAvg - firstAvg) / firstAvg) * 100.0
                    when {
                        pct >= 20.0 ->
                            out += "Trending up — recent days average ${pct.toInt()}% above earlier in the week."
                        pct <= -20.0 ->
                            out += "Trending down — recent days average ${(-pct).toInt()}% below earlier in the week."
                    }
                }
                secondAvg > 0.0 ->
                    out += "Picking up — activity restarted in the second half of the week."
            }
        }

        // --- 3. Biggest day-over-day swing (spike or drop) -----------------------------
        if (values.size >= 2) {
            var bestIdx = -1
            var bestAbsPct = 0.0
            var bestSigned = 0.0
            for (i in 1 until values.size) {
                val prev = values[i - 1]
                val cur = values[i]
                if (prev <= 0.0) continue                       // avoid div-by-zero noise
                val pct = ((cur - prev) / prev) * 100.0
                val abs = abs(pct)
                if (abs >= 50.0 && abs > bestAbsPct) {
                    bestIdx = i
                    bestAbsPct = abs
                    bestSigned = pct
                }
            }
            if (bestIdx > 0) {
                val dayLabel = labels.getOrNull(bestIdx) ?: "recently"
                val priorLabel = labels.getOrNull(bestIdx - 1) ?: "the day before"
                out += if (bestSigned > 0)
                    "Sudden jump on $dayLabel (+${bestAbsPct.toInt()}% vs $priorLabel)."
                else
                    "Sudden drop on $dayLabel (\u2212${bestAbsPct.toInt()}% vs $priorLabel)."
            }
        }

        return out
    }

    /** Join insight bullets into a single string suitable for a TextView. */
    fun asBulletText(lines: List<String>): String =
        lines.joinToString("\n") { "\u2022  $it" }
}
