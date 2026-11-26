package at.drinkly.app.data

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class HydrationStatus(val label: String) {
    GOAL_REACHED("Goal reached"),
    ON_TRACK("On track"),
    SLIGHTLY_BEHIND("A little behind"),
    BEHIND("Time for a drink"),
    TOO_MUCH("Take it easy"),
}

object Hydration {

    /** Above this share of the goal, more water doesn't help any more. */
    const val TOO_MUCH_FACTOR = 1.5

    /** The kidneys can only handle about a litre per hour; drinking faster than that is risky. */
    const val MAX_PER_HOUR_ML = 1000

    /** Drinks and app use before this hour still belong to the night, not to a new day. */
    const val EARLIEST_DAY_START_HOUR = 4

    fun dailyTotals(entries: List<DrinkEntry>, zone: ZoneId = ZoneId.systemDefault()): Map<LocalDate, Int> =
        entries.groupBy { it.timestamp.atZone(zone).toLocalDate() }
            .mapValues { (_, drinks) -> drinks.sumOf { it.milliliters } }

    // Goal history

    /** The goal that applied on [day]. Days before the first recorded change use the oldest known goal. */
    fun goalOn(day: LocalDate, history: List<GoalChange>, fallback: Int): Int =
        history.lastOrNull { !it.day.isAfter(day) }?.goalMl ?: history.firstOrNull()?.goalMl ?: fallback

    /** Records [goalMl] as the goal from [day] on; several changes on one day keep only the last. */
    fun recordGoal(goalMl: Int, day: LocalDate, history: List<GoalChange>): List<GoalChange> {
        val result = history.filter { it.day.isBefore(day) }
        return if (result.lastOrNull()?.goalMl == goalMl) result else result + GoalChange(day, goalMl)
    }

    // Streaks

    /** Days in a row the goal was met. Today only counts once it is reached, so an open day doesn't break the streak. */
    fun streak(totals: Map<LocalDate, Int>, goal: (LocalDate) -> Int, today: LocalDate): Int {
        var day = if ((totals[today] ?: 0) >= goal(today)) today else today.minusDays(1)
        var count = 0
        while ((totals[day] ?: 0) >= goal(day)) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun longestStreak(totals: Map<LocalDate, Int>, goal: (LocalDate) -> Int): Int {
        fun reached(day: LocalDate) = (totals[day] ?: 0) >= goal(day)
        var best = 0
        for (day in totals.keys) {
            if (reached(day.minusDays(1))) continue // not the start of a run
            var run = 0
            var d = day
            while (reached(d)) {
                run++
                d = d.plusDays(1)
            }
            best = maxOf(best, run)
        }
        return best
    }

    // Pace

    /**
     * When today started: the first sign of life (app opened or a drink logged) after [EARLIEST_DAY_START_HOUR],
     * or the fixed wake-up hour if that detection is off or nothing happened yet.
     */
    fun dayStart(
        now: LocalDateTime,
        wakeHour: Int,
        detectWakeUp: Boolean,
        firstActive: LocalDateTime?,
        entries: List<DrinkEntry>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): LocalDateTime {
        val fixed = now.toLocalDate().atTime(wakeHour.coerceIn(0, 23), 0)
        if (!detectWakeUp) return fixed
        val earliest = now.toLocalDate().atTime(EARLIEST_DAY_START_HOUR, 0)
        val signs = entries.map { LocalDateTime.ofInstant(it.timestamp, zone) } + listOfNotNull(firstActive)
        return signs.filter { !it.isBefore(earliest) && it.toLocalDate() == now.toLocalDate() }.minOrNull() ?: fixed
    }

    /** How much should have been drunk by [now] if the goal is spread evenly between [start] and the sleep hour. */
    fun expectedBy(now: LocalTime, goalMl: Int, start: LocalTime, sleepHour: Int): Int {
        val startMinute = start.hour * 60 + start.minute
        val end = sleepHour * 60
        val minute = now.hour * 60 + now.minute
        val fraction = when {
            end <= startMinute || minute <= startMinute -> 0.0
            minute >= end -> 1.0
            else -> (minute - startMinute).toDouble() / (end - startMinute)
        }
        return (goalMl * fraction).roundToInt()
    }

    /**
     * Millilitres drunk in the hour before [now]. No upper bound: the server stamps the drinks,
     * and its clock may run slightly ahead of the phone's.
     */
    fun lastHour(entries: List<DrinkEntry>, now: Instant): Int =
        entries.filter { it.timestamp.isAfter(now.minus(Duration.ofHours(1))) }.sumOf { it.milliliters }

    fun status(todayMl: Int, goalMl: Int, expectedMl: Int, lastHourMl: Int): HydrationStatus = when {
        lastHourMl > MAX_PER_HOUR_ML || todayMl >= goalMl * TOO_MUCH_FACTOR -> HydrationStatus.TOO_MUCH
        todayMl >= goalMl -> HydrationStatus.GOAL_REACHED
        todayMl >= expectedMl -> HydrationStatus.ON_TRACK
        todayMl >= expectedMl * 0.6 -> HydrationStatus.SLIGHTLY_BEHIND
        else -> HydrationStatus.BEHIND
    }

    fun message(todayMl: Int, goalMl: Int, expectedMl: Int, lastHourMl: Int, streak: Int): HydrationMessage {
        val status = status(todayMl, goalMl, expectedMl, lastHourMl)
        val behind = roundUp(expectedMl - todayMl)
        return when (status) {
            HydrationStatus.TOO_MUCH -> if (lastHourMl > MAX_PER_HOUR_ML) {
                HydrationMessage(status, "Slow down", "$lastHourMl ml in the last hour. Your body can take about 1 l per hour.")
            } else {
                HydrationMessage(status, "That's plenty", "You're ${todayMl - goalMl} ml over your goal. No need for more today.")
            }
            HydrationStatus.GOAL_REACHED -> HydrationMessage(
                status, "Goal reached",
                if (streak > 1) "Well done, that's $streak days in a row!" else "Well done! Come back tomorrow.",
            )
            HydrationStatus.ON_TRACK -> if (todayMl == 0) {
                HydrationMessage(status, "Let's get started", "A glass of water is a good first step.")
            } else {
                HydrationMessage(status, "On track", "${goalMl - todayMl} ml to go today.")
            }
            HydrationStatus.SLIGHTLY_BEHIND -> HydrationMessage(status, "A little behind", "Drink about $behind ml to catch up.")
            HydrationStatus.BEHIND -> HydrationMessage(status, "Time for a drink", "You're $behind ml behind your pace.")
        }
    }

    /** Common rule of thumb: about 35 ml per kg of body weight, rounded to 50 ml. */
    fun suggestedGoal(weightKg: Int): Int = ((weightKg * 35) / 50.0).roundToInt() * 50

    /** Up to the next 50 ml, so suggestions read like amounts you'd actually pour. */
    private fun roundUp(ml: Int) = maxOf(50, ceil(ml / 50.0).toInt() * 50)
}
