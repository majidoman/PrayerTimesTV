package com.prayer.tv

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import java.util.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        setContentView(PrayerBoardView(this))
    }
}

class PrayerBoardView(context: Context) : View(context) {

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() { invalidate(); handler.postDelayed(this, 1000) }
    }

    // Paints
    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D4AF37"); textAlign = Paint.Align.CENTER
    }
    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER
    }
    private val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50"); textAlign = Paint.Align.CENTER
    }
    private val purplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9C27B0"); textAlign = Paint.Align.CENTER
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D4AF37"); style = Paint.Style.STROKE; strokeWidth = 2f
    }
    private val greenBgPaint = Paint().apply { color = Color.parseColor("#1B5E20") }
    private val rowBgPaint = Paint().apply { color = Color.parseColor("#0D1F0D") }
    private val headerBgPaint = Paint().apply { color = Color.parseColor("#0A1628") }

    // Hijri month names
    private val hijriMonths = arrayOf(
        "محرم","صفر","ربيع الأول","ربيع الثاني",
        "جمادى الأولى","جمادى الثانية","رجب","شعبان",
        "رمضان","شوال","ذو القعدة","ذو الحجة"
    )
    private val arabicDays = arrayOf("الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت")
    private val arabicMonths = arrayOf(
        "يناير","فبراير","مارس","أبريل","مايو","يونيو",
        "يوليو","أغسطس","سبتمبر","أكتوبر","نوفمبر","ديسمبر"
    )
    private val prayerNames = arrayOf("الفجر","الظهر","العصر","المغرب","العشاء","الشروق")
    private val bottomText = "علم ما بين أيديهم وما خلفهم ولا يحيطون بشيء من علمه إلا بما شاء"

    init { handler.post(ticker) }

    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); handler.removeCallbacksAndMessages(null) }

    // Gregorian to Hijri conversion
    private fun toHijri(y: Int, m: Int, d: Int): Triple<Int, Int, Int> {
        var jd = (367 * y - (7 * (y + (m + 9) / 12)) / 4 + (275 * m) / 9 + d + 1721013.5).toInt()
        val l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        val ll = l - 10631 * n + 354
        val j = ((10985 - ll) / 5316) * ((50 * ll) / 17719) + (ll / 5670) * ((43 * ll) / 15238)
        val lll = ll - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val hm = (24 * lll) / 709
        val hd = lll - (709 * hm) / 24
        val hy = 30 * n + j - 30
        return Triple(hy, hm, hd)
    }

    override fun onDraw(canvas: Canvas) {
        val W = width.toFloat()
        val H = height.toFloat()
        canvas.drawRect(0f, 0f, W, H, bgPaint)

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dow = cal.get(Calendar.DAY_OF_WEEK) - 1
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val min = cal.get(Calendar.MINUTE)
        val sec = cal.get(Calendar.SECOND)

        val (hy, hm, hd) = toHijri(year, month, day)
        val prayers = PrayerCalculator.calculate(year, month, day)

        val adhanTimes = doubleArrayOf(prayers.fajr, prayers.dhuhr, prayers.asr, prayers.maghrib, prayers.isha, prayers.sunrise)
        val iqamaTimes = adhanTimes.map { if (it.isNaN()) it else it + Config.IQAMA_DELAY / 60.0 }.toDoubleArray()

        // ── HEADER ──────────────────────────────────────────
        val headerH = H * 0.22f
        canvas.drawRect(0f, 0f, W, headerH, headerBgPaint)
        canvas.drawRect(0f, headerH - 2f, W, headerH, borderPaint.apply { style = Paint.Style.FILL })

        // Row 1: day badge | clock | month badge
        val r1y = headerH * 0.30f
        val badgeW = W * 0.10f
        val badgeH = headerH * 0.35f

        // Day badge
        canvas.drawRoundRect(W * 0.05f, r1y - badgeH * 0.8f, W * 0.05f + badgeW, r1y + badgeH * 0.2f, 12f, 12f, borderPaint.apply { style = Paint.Style.STROKE; strokeWidth = 2f })
        goldPaint.textSize = badgeH * 0.9f; goldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(hd.toString(), W * 0.05f + badgeW / 2, r1y, goldPaint)

        // Clock
        whitePaint.textSize = headerH * 0.28f; whitePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("%02d:%02d:%02d".format(hour, min, sec), W * 0.5f, r1y, whitePaint)

        // Month badge
        canvas.drawRoundRect(W - W * 0.05f - badgeW, r1y - badgeH * 0.8f, W - W * 0.05f, r1y + badgeH * 0.2f, 12f, 12f, borderPaint.apply { style = Paint.Style.STROKE; strokeWidth = 2f })
        goldPaint.textSize = badgeH * 0.9f; goldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(day.toString(), W - W * 0.05f - badgeW / 2, r1y, goldPaint)

        // Row 2: Hijri month | day name | Gregorian month
        val r2y = headerH * 0.65f
        goldPaint.textSize = headerH * 0.18f
        canvas.drawText(hijriMonths[(hm - 1).coerceIn(0, 11)], W * 0.20f, r2y, goldPaint)
        whitePaint.textSize = headerH * 0.20f
        canvas.drawText(arabicDays[dow], W * 0.5f, r2y, whitePaint)
        goldPaint.textSize = headerH * 0.18f
        canvas.drawText(arabicMonths[month - 1], W * 0.80f, r2y, goldPaint)

        // Row 3: Hijri year | Gregorian year
        val r3y = headerH * 0.92f
        whitePaint.textSize = headerH * 0.15f
        canvas.drawText("$hy هـ", W * 0.30f, r3y, whitePaint)
        canvas.drawText("$year م", W * 0.70f, r3y, whitePaint)

        // ── PRAYER TABLE ─────────────────────────────────────
        val tableTop = headerH + H * 0.01f
        val bottomBarH = H * 0.07f
        val countdownH = H * 0.10f
        val tableH = H - tableTop - countdownH - bottomBarH
        val rowH = tableH / 6f
        val col1 = W * 0.15f  // iqama x-center
        val col2 = W * 0.50f  // name x-center
        val col3 = W * 0.85f  // adhan x-center

        // Column headers
        val hdrY = tableTop + rowH * 0.45f
        goldPaint.textSize = rowH * 0.30f
        canvas.drawText("الإقامة", col1, hdrY, goldPaint)
        canvas.drawText("الصلاة", col2, hdrY, goldPaint)
        canvas.drawText("الأذان", col3, hdrY, goldPaint)

        // Header underline
        canvas.drawLine(W * 0.02f, tableTop + rowH * 0.55f, W * 0.98f, tableTop + rowH * 0.55f, borderPaint.apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.parseColor("#D4AF37") })

        // Prayer rows
        val nowMins = hour * 60 + min
        var nextPrayerIdx = -1
        for (i in 0..4) {
            val at = adhanTimes[i]
            if (!at.isNaN()) {
                val total = ((at % 24 + 24) % 24)
                val pm = (total.toInt()) * 60 + ((total - total.toInt()) * 60).toInt()
                if (pm > nowMins && nextPrayerIdx == -1) nextPrayerIdx = i
            }
        }

        for (i in 0..5) {
            val rowTop = tableTop + rowH * (i + 1)
            val rowMid = rowTop + rowH * 0.65f

            if (i == nextPrayerIdx) {
                canvas.drawRect(W * 0.01f, rowTop + 2f, W * 0.99f, rowTop + rowH - 2f, rowBgPaint)
                canvas.drawRect(W * 0.01f, rowTop + 2f, W * 0.99f, rowTop + rowH - 2f, borderPaint.apply { style = Paint.Style.STROKE; strokeWidth = 1.5f; color = Color.parseColor("#D4AF37") })
            }

            val ts = rowH * 0.38f
            greenPaint.textSize = ts
            whitePaint.textSize = ts
            purplePaint.textSize = ts

            // Iqama (green, left)
            if (i < 5) {
                greenPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(PrayerCalculator.toHHMM(iqamaTimes[i]), col1, rowMid, greenPaint)
            }

            // Prayer name (white, center)
            whitePaint.textAlign = Paint.Align.CENTER
            canvas.drawText(prayerNames[i], col2, rowMid, whitePaint)

            // Adhan (purple, right)
            purplePaint.textAlign = Paint.Align.CENTER
            canvas.drawText(PrayerCalculator.toHHMM(adhanTimes[i]), col3, rowMid, purplePaint)

            // Row divider
            if (i < 5) canvas.drawLine(W * 0.02f, rowTop + rowH, W * 0.98f, rowTop + rowH, borderPaint.apply { style = Paint.Style.STROKE; strokeWidth = 0.5f; color = Color.parseColor("#333333") })
        }

        // Vertical dividers
        canvas.drawLine(W * 0.30f, tableTop, W * 0.30f, tableTop + tableH, borderPaint.apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.parseColor("#333333") })
        canvas.drawLine(W * 0.70f, tableTop, W * 0.70f, tableTop + tableH, borderPaint.apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.parseColor("#333333") })

        // ── COUNTDOWN ─────────────────────────────────────────
        val cdTop = tableTop + tableH
        if (nextPrayerIdx >= 0) {
            val at = adhanTimes[nextPrayerIdx]
            val total = ((at % 24 + 24) % 24)
            val pm = (total.toInt()) * 60 + ((total - total.toInt()) * 60).toInt()
            val diff = pm - nowMins
            val dh = diff / 60; val dm = diff % 60
            goldPaint.textSize = countdownH * 0.28f; goldPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("الأذان بعد", W * 0.5f, cdTop + countdownH * 0.40f, goldPaint)
            whitePaint.textSize = countdownH * 0.45f; whitePaint.textAlign = Paint.Align.CENTER
            canvas.drawText("%02d:%02d".format(dh, dm), W * 0.5f, cdTop + countdownH * 0.85f, whitePaint)
        }

        // ── BOTTOM BAR ────────────────────────────────────────
        val barTop = H - bottomBarH
        canvas.drawRect(0f, barTop, W, H, greenBgPaint)
        canvas.drawLine(0f, barTop, W, barTop, borderPaint.apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.parseColor("#D4AF37") })
        whitePaint.textSize = bottomBarH * 0.35f; whitePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(bottomText, W * 0.5f, barTop + bottomBarH * 0.62f, whitePaint)
    }
}
