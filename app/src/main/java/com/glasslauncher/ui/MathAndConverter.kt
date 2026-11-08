package com.glasslauncher.ui

import java.text.DecimalFormat
import java.util.Locale

sealed class SearchCalculation {
    data class MathResult(val expression: String, val result: String) : SearchCalculation()
    data class ConversionResult(val fromValue: Double, val fromUnit: String, val toValue: Double, val toUnit: String, val formatted: String) : SearchCalculation()
}

object MathAndConverter {
    private val decimalFormat = DecimalFormat("#,##0.####")
    private val currencyFormat = DecimalFormat("#,##0.00")

    fun evaluate(raw: String): SearchCalculation? {
        val query = raw.trim()
        if (query.isEmpty() || query.length < 2) return null

        // 1. Try Conversion: e.g. "500 usd to inr", "100 km to miles", "5 m to ft"
        val conv = tryConvert(query)
        if (conv != null) return conv

        // 2. Try Math Expression: e.g. "14 * 28", "500 * 2", "(10 + 5) / 3", "20% of 150"
        val math = tryMath(query)
        if (math != null) return math

        return null
    }

    private fun tryConvert(query: String): SearchCalculation.ConversionResult? {
        // Pattern: <value> <unit1> to/in/into/= <unit2>
        val regex = Regex("""^([0-9]+(?:\.[0-9]+)?)\s*([a-zA-Z$€£₹°/]+)\s+(?:to|in|into|=)\s+([a-zA-Z$€£₹°/]+)$""", RegexOption.IGNORE_CASE)
        val match = regex.find(query) ?: return null

        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit1 = match.groupValues[2].lowercase(Locale.ROOT)
        val unit2 = match.groupValues[3].lowercase(Locale.ROOT)

        // Currency baseline rates to USD (approximate market rates)
        val usdRates = mapOf(
            "usd" to 1.0, "$" to 1.0,
            "inr" to 84.50, "₹" to 84.50, "rs" to 84.50,
            "eur" to 0.92, "€" to 0.92,
            "gbp" to 0.77, "£" to 0.77,
            "cad" to 1.39,
            "aud" to 1.52,
            "jpy" to 152.0, "¥" to 152.0,
            "cny" to 7.12,
            "aed" to 3.67,
            "sgd" to 1.32
        )

        if (usdRates.containsKey(unit1) && usdRates.containsKey(unit2)) {
            val fromRate = usdRates[unit1]!!
            val toRate = usdRates[unit2]!!
            // value in USD = value / fromRate; then * toRate
            val result = (value / fromRate) * toRate
            val u1Display = unit1.uppercase(Locale.ROOT)
            val u2Display = unit2.uppercase(Locale.ROOT)
            val symbol = if (u2Display == "INR" || u2Display == "RS" || u2Display == "₹") "₹" else if (u2Display == "EUR" || u2Display == "€") "€" else if (u2Display == "GBP" || u2Display == "£") "£" else "$"
            val formatted = "$symbol${currencyFormat.format(result)} $u2Display"
            return SearchCalculation.ConversionResult(value, u1Display, result, u2Display, formatted)
        }

        // Distance & Length
        val metersMap = mapOf(
            "km" to 1000.0, "kilometer" to 1000.0, "kilometres" to 1000.0,
            "m" to 1.0, "meter" to 1.0, "meters" to 1.0,
            "cm" to 0.01, "centimeter" to 0.01,
            "mm" to 0.001,
            "mile" to 1609.344, "miles" to 1609.344, "mi" to 1609.344,
            "foot" to 0.3048, "feet" to 0.3048, "ft" to 0.3048,
            "inch" to 0.0254, "inches" to 0.0254, "in" to 0.0254,
            "yard" to 0.9144, "yards" to 0.9144, "yd" to 0.9144
        )

        if (metersMap.containsKey(unit1) && metersMap.containsKey(unit2)) {
            val meters = value * metersMap[unit1]!!
            val result = meters / metersMap[unit2]!!
            val formatted = "${decimalFormat.format(result)} $unit2"
            return SearchCalculation.ConversionResult(value, unit1, result, unit2, formatted)
        }

        // Weight
        val gramsMap = mapOf(
            "kg" to 1000.0, "kilogram" to 1000.0, "kgs" to 1000.0,
            "g" to 1.0, "gram" to 1.0, "grams" to 1.0,
            "mg" to 0.001,
            "lb" to 453.59237, "lbs" to 453.59237, "pound" to 453.59237, "pounds" to 453.59237,
            "oz" to 28.3495, "ounce" to 28.3495, "ounces" to 28.3495
        )

        if (gramsMap.containsKey(unit1) && gramsMap.containsKey(unit2)) {
            val grams = value * gramsMap[unit1]!!
            val result = grams / gramsMap[unit2]!!
            val formatted = "${decimalFormat.format(result)} $unit2"
            return SearchCalculation.ConversionResult(value, unit1, result, unit2, formatted)
        }

        // Temperature
        if ((unit1 == "c" || unit1 == "celsius" || unit1 == "°c") && (unit2 == "f" || unit2 == "fahrenheit" || unit2 == "°f")) {
            val result = (value * 9.0 / 5.0) + 32.0
            return SearchCalculation.ConversionResult(value, "°C", result, "°F", "${decimalFormat.format(result)}°F")
        }
        if ((unit1 == "f" || unit1 == "fahrenheit" || unit1 == "°f") && (unit2 == "c" || unit2 == "celsius" || unit2 == "°c")) {
            val result = (value - 32.0) * 5.0 / 9.0
            return SearchCalculation.ConversionResult(value, "°F", result, "°C", "${decimalFormat.format(result)}°C")
        }

        // Speed
        if ((unit1 == "mph" || unit1 == "mi/h") && (unit2 == "kmh" || unit2 == "km/h" || unit2 == "kph")) {
            val result = value * 1.609344
            return SearchCalculation.ConversionResult(value, "mph", result, "km/h", "${decimalFormat.format(result)} km/h")
        }
        if ((unit1 == "kmh" || unit1 == "km/h" || unit1 == "kph") && (unit2 == "mph" || unit2 == "mi/h")) {
            val result = value / 1.609344
            return SearchCalculation.ConversionResult(value, "km/h", result, "mph", "${decimalFormat.format(result)} mph")
        }

        return null
    }

    private fun tryMath(query: String): SearchCalculation.MathResult? {
        var clean = query.replace(" ", "")
            .replace("×", "*")
            .replace("x", "*")
            .replace("÷", "/")

        // Percentage check: "20%of150"
        val pctRegex = Regex("""^([0-9]+(?:\.[0-9]+)?)%of([0-9]+(?:\.[0-9]+)?)$""", RegexOption.IGNORE_CASE)
        val pctMatch = pctRegex.find(clean)
        if (pctMatch != null) {
            val p = pctMatch.groupValues[1].toDoubleOrNull() ?: return null
            val total = pctMatch.groupValues[2].toDoubleOrNull() ?: return null
            val result = (p / 100.0) * total
            return SearchCalculation.MathResult(query, decimalFormat.format(result))
        }

        // Valid math chars: digits, ., +, -, *, /, ^, (, )
        if (!clean.matches(Regex("""^[0-9.+\-*/^()]+$"""))) return null

        // Must contain at least one arithmetic operator
        if (!clean.any { it in "+-*/^" }) return null

        return try {
            val value = parseMathExpression(clean)
            if (value.isNaN() || value.isInfinite()) null
            else SearchCalculation.MathResult(query, decimalFormat.format(value))
        } catch (_: Exception) {
            null
        }
    }

    private fun parseMathExpression(str: String): Double {
        return ExpressionParser(str).parse()
    }

    private class ExpressionParser(private val str: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected char: " + ch.toChar())
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> x /= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else {
                throw RuntimeException("Unexpected char: " + ch.toChar())
            }

            if (eat('^'.code)) x = Math.pow(x, parseFactor())

            return x
        }
    }
}
