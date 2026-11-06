package at.drinkly.app.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt

enum class HydrationStatus(val label: String) {
    GOAL_REACHED("Goal reached"),
    ON_TRACK("Well hydrated"),
    SLIGHTLY_BEHIND("Slightly behind"),
    BEHIND("Time for a drink"),
}

object Hydration {

    fun dailyTotals(entries: List<DrinkEntry>, zone: ZoneId = ZoneId.systemDefault()): Map<LocalDate, Int> =
        entries.groupBy { it.timestamp.atZone(zone).toLocalDate() }
            .mapValues { (_, drinks) -> drinks.sumOf { it.milliliters } }

    /** Days in a row the goal was met. Today only counts once it is reached, so an open day doesn't break the streak. */
    fun streak(totals: Map<LocalDate, Int>, goalMl: Int, today: LocalDate): Int {
        var day = if ((totals[today] ?: 0) >= goalMl) today else today.minusDays(1)
        var count = 0
        while ((totals[day] ?: 0) >= goalMl) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun longestStreak(totals: Map<LocalDate, Int>, goalMl: Int): Int {
        var best = 0
        for (day in totals.keys) {
            if ((totals[day.minusDays(1)] ?: 0) >= goalMl) continue // not the start of a run
            var run = 0
            var d = day
            while ((totals[d] ?: 0) >= goalMl) {
                run++
                d = d.plusDays(1)
            }
            best = maxOf(best, run)
        }
        return best
    }

    /** How much should have been drunk by [now] if the goal is spread evenly over the waking hours. */
    fun expectedByNow(goalMl: Int, now: LocalTime, wakeHour: Int, sleepHour: Int): Int {
        val start = wakeHour * 60
        val end = sleepHour * 60
        val minute = now.hour * 60 + now.minute
        val fraction = when {
            end <= start || minute <= start -> 0.0
            minute >= end -> 1.0
            else -> (minute - start).toDouble() / (end - start)
        }
        return (goalMl * fraction).roundToInt()
    }

    fun status(todayMl: Int, goalMl: Int, now: LocalTime, wakeHour: Int, sleepHour: Int): HydrationStatus {
        if (todayMl >= goalMl) return HydrationStatus.GOAL_REACHED
        val expected = expectedByNow(goalMl, now, wakeHour, sleepHour)
        if (todayMl >= expected) return HydrationStatus.ON_TRACK
        return if (todayMl >= expected * 0.6) HydrationStatus.SLIGHTLY_BEHIND else HydrationStatus.BEHIND
    }

    /** Common rule of thumb: about 35 ml per kg of body weight, rounded to 50 ml. */
    fun suggestedGoal(weightKg: Int): Int = ((weightKg * 35) / 50.0).roundToInt() * 50
}
