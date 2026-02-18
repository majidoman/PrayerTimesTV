package com.prayer.tv

import kotlin.math.*

// CONFIG — edit these before building
object Config {
    const val LATITUDE = 23.6100
    const val LONGITUDE = 58.5400
    const val TIMEZONE = 4.0        // UTC+4 Oman
    const val IQAMA_DELAY = 15      // minutes after adhan
    const val FAJR_ANGLE = 18.5     // Umm Al-Qura
    const val ISHA_ANGLE = 90.0     // Umm Al-Qura (90 min after maghrib)
    const val CITY = "مسقط"
}

data class PrayerTimes(
    val fajr: Double,
    val sunrise: Double,
    val dhuhr: Double,
    val asr: Double,
    val maghrib: Double,
    val isha: Double
)

object PrayerCalculator {

    private fun julianDate(y: Int, m: Int, d: Int): Double {
        var year = y; var month = m
        if (month <= 2) { year--; month += 12 }
        val a = floor(year / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + d + b - 1524.5
    }

    private fun sunPosition(jd: Double): DoubleArray {
        val d = jd - 2451545.0
        val g = Math.toRadians((357.529 + 0.98560028 * d) % 360)
        val q = (280.459 + 0.98564736 * d) % 360
        val l = Math.toRadians((q + 1.915 * sin(g) + 0.020 * sin(2 * g)) % 360)
        val e = Math.toRadians(23.439 - 0.0000004 * d)
        val ra = Math.toDegrees(atan2(cos(e) * sin(l), cos(l))) / 15.0
        val dec = asin(sin(e) * sin(l))
        val eqt = q / 15.0 - ((ra + 24) % 24)
        return doubleArrayOf(dec, eqt)
    }

    private fun prayerTime(jd: Double, angle: Double, afterNoon: Boolean): Double {
        val (dec, eqt) = sunPosition(jd).let { Pair(it[0], it[1]) }
        val lat = Math.toRadians(Config.LATITUDE)
        val cosH = (sin(Math.toRadians(angle)) - sin(lat) * sin(dec)) / (cos(lat) * cos(dec))
        if (cosH < -1 || cosH > 1) return Double.NaN
        val h = Math.toDegrees(acos(cosH)) / 15.0
        val noon = 12.0 - eqt - Config.LONGITUDE / 15.0 + Config.TIMEZONE
        return if (afterNoon) noon + h else noon - h
    }

    private fun asrTime(jd: Double): Double {
        val (dec, eqt) = sunPosition(jd).let { Pair(it[0], it[1]) }
        val lat = Math.toRadians(Config.LATITUDE)
        val a = atan(1.0 / (1 + tan(abs(lat - dec))))
        val cosH = (sin(a) - sin(lat) * sin(dec)) / (cos(lat) * cos(dec))
        if (cosH < -1 || cosH > 1) return Double.NaN
        val h = Math.toDegrees(acos(cosH)) / 15.0
        val noon = 12.0 - eqt - Config.LONGITUDE / 15.0 + Config.TIMEZONE
        return noon + h
    }

    fun calculate(year: Int, month: Int, day: Int): PrayerTimes {
        val jd = julianDate(year, month, day)
        val (dec, eqt) = sunPosition(jd).let { Pair(it[0], it[1]) }
        val noon = 12.0 - eqt - Config.LONGITUDE / 15.0 + Config.TIMEZONE
        val maghrib = prayerTime(jd, -0.8333, true)
        val isha = maghrib + Config.ISHA_ANGLE / 60.0
        return PrayerTimes(
            fajr = prayerTime(jd, -Config.FAJR_ANGLE, false),
            sunrise = prayerTime(jd, -0.8333, false),
            dhuhr = noon + 0.0,
            asr = asrTime(jd),
            maghrib = maghrib,
            isha = isha
        )
    }

    fun toHHMM(t: Double): String {
        if (t.isNaN()) return "--:--"
        val total = ((t % 24 + 24) % 24)
        val h = total.toInt()
        val m = ((total - h) * 60).roundToInt().coerceIn(0, 59)
        return "%02d:%02d".format(h, m)
    }
}

fun Double.roundToInt() = kotlin.math.roundToInt(this)
