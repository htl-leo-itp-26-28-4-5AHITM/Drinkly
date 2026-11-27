package at.drinkly.app.data

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Amounts are shown in litres (Corporate Design: "1.4 L of 2.5 L"); the app's texts are English, so a decimal point
private val litersFormat = DecimalFormat("0.0#", DecimalFormatSymbols(Locale.US))

/** 1500 -> "1.5" */
fun litersValue(ml: Int): String = litersFormat.format(ml / 1000.0)

/** 1500 -> "1.5 L", 250 -> "0.25 L" */
fun liters(ml: Int): String = litersValue(ml) + " L"
