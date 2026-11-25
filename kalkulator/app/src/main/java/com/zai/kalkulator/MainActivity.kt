package com.zai.kalkulator

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor

class MainActivity : android.app.Activity() {

    private lateinit var expressionView: TextView
    private lateinit var resultView: TextView

    private var expression = ""
    private var resultText = ""
    private var lastResult = ""
    private var justEvaluated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_BG)
            setPadding(dp(16), dp(20), dp(16), dp(12))
        }

        expressionView = TextView(this).apply {
            textSize = 26f
            setTextColor(COLOR_HINT)
            gravity = Gravity.END
            setSingleLine(true)
        }
        resultView = TextView(this).apply {
            textSize = 56f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.END
            setSingleLine(true)
            setAutoSizeTextTypeUniformWithConfiguration(26, 56, 2, TypedValue.COMPLEX_UNIT_SP)
        }

        val display = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            minimumHeight = dp(200)
            addView(expressionView)
            addView(resultView)
        }
        root.addView(display)

        val rows = arrayOf(
            arrayOf("C", "⌫", "%", "÷"),
            arrayOf("7", "8", "9", "×"),
            arrayOf("4", "5", "6", "−"),
            arrayOf("1", "2", "3", "+"),
            arrayOf("0", ".", "=")
        )
        for (labels in rows) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (label in labels) {
                row.addView(makeButton(label))
            }
            root.addView(row)
        }

        setContentView(root)
        refresh()
    }

    private fun makeButton(label: String): Button {
        val weight = if (label == "0") 2f else 1f
        val bg = when (label) {
            "=", "÷", "×", "−", "+" -> COLOR_OPERATOR
            "C", "⌫", "%" -> COLOR_FUNCTION
            else -> COLOR_NUMBER
        }
        val params = LinearLayout.LayoutParams(0, dp(62), weight)
        params.setMargins(dp(5), dp(5), dp(5), dp(5))

        val shape = GradientDrawable().apply {
            cornerRadius = dp(28).toFloat()
            setColor(bg)
        }
        return Button(this).apply {
            text = label
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = shape
            stateListAnimator = null
            layoutParams = params
            setAllCaps(false)
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> v.alpha = 0.65f
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.alpha = 1f
                }
                false
            }
            setOnClickListener { onPress(label) }
        }
    }

    private fun onPress(label: String) {
        when (label) {
            "C" -> {
                expression = ""; resultText = ""; lastResult = ""; justEvaluated = false
            }
            "⌫" -> {
                if (justEvaluated) {
                    expression = ""; resultText = ""; lastResult = ""; justEvaluated = false
                } else {
                    expression = expression.dropLast(1)
                }
            }
            "=" -> onEquals()
            "÷", "×", "−", "+" -> onOperator(label)
            else -> onInput(label)
        }
        refresh()
    }

    private fun onOperator(op: String) {
        if (justEvaluated) {
            if (lastResult == "Error") return
            expression = lastResult
            resultText = ""
            justEvaluated = false
        }
        if (expression.isEmpty()) {
            if (op == "−") expression = "−"
            return
        }
        expression = if (expression.last() in "÷×−+") {
            expression.dropLast(1) + op
        } else {
            expression + op
        }
    }

    private fun onInput(ch: String) {
        if (resultText == "Error" || justEvaluated) {
            expression = ""
            resultText = ""
            lastResult = ""
            justEvaluated = false
        }
        if (expression.length > 48) return
        when (ch) {
            "." -> {
                val last = expression.lastOrNull()
                when {
                    last == null || last in "÷×−+" -> expression += "0."
                    last.isDigit() && !currentNumberHasDot() -> expression += "."
                }
            }
            "%" -> {
                val last = expression.lastOrNull()
                if (last != null && (last.isDigit() || last == '.')) expression += "%"
            }
            else -> expression += ch
        }
    }

    private fun onEquals() {
        var src = expression
        while (src.isNotEmpty() && src.last() in "÷×−+") src = src.dropLast(1)
        if (src.isEmpty() || src == "−") return
        val value = try {
            Parser(src).parse()
        } catch (e: Exception) {
            null
        }
        if (value == null || value.isNaN() || value.isInfinite()) {
            resultText = "Error"
            justEvaluated = false
            return
        }
        lastResult = format(value)
        resultText = lastResult
        justEvaluated = true
    }

    private fun currentNumberHasDot(): Boolean {
        val segment = expression.takeLastWhile { it !in "÷×−+%" }
        return segment.contains('.')
    }

    private fun refresh() {
        expressionView.text = expression.ifEmpty { "0" }
        resultView.text = resultText
    }

    private fun format(value: Double): String {
        return if (value == floor(value) && abs(value) < 1e15) {
            value.toLong().toString()
        } else {
            BigDecimal(value)
                .setScale(10, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private class Parser(private val src: String) {
        private var pos = 0

        fun parse(): Double {
            val value = sum()
            if (pos < src.length) throw IllegalArgumentException("Input tidak valid")
            return value
        }

        private fun peek(): Char? = src.getOrNull(pos)

        private fun sum(): Double {
            var value = product()
            while (true) {
                when (peek()) {
                    '+' -> { pos++; value += product() }
                    '-', '−' -> { pos++; value -= product() }
                    else -> return value
                }
            }
        }

        private fun product(): Double {
            var value = factor()
            while (true) {
                when (peek()) {
                    '×', '*' -> { pos++; value *= factor() }
                    '÷', '/' -> {
                        pos++
                        val divisor = factor()
                        if (divisor == 0.0) throw ArithmeticException("Pembagian nol")
                        value /= divisor
                    }
                    else -> return value
                }
            }
        }

        private fun factor(): Double {
            var value = primary()
            while (peek() == '%') {
                pos++
                value /= 100.0
            }
            return value
        }

        private fun primary(): Double {
            val c = peek() ?: throw IllegalArgumentException("Input tidak lengkap")
            return when {
                c == '-' || c == '−' -> { pos++; -primary() }
                c.isDigit() || c == '.' -> number()
                else -> throw IllegalArgumentException("Karakter tidak dikenal")
            }
        }

        private fun number(): Double {
            val start = pos
            while (peek()?.isDigit() == true || peek() == '.') pos++
            return src.substring(start, pos).toDoubleOrNull()
                ?: throw IllegalArgumentException("Angka tidak valid")
        }
    }

    private companion object {
        val COLOR_BG = Color.parseColor("#121218")
        val COLOR_HINT = Color.parseColor("#9E9EA8")
        val COLOR_NUMBER = Color.parseColor("#26262E")
        val COLOR_FUNCTION = Color.parseColor("#45454F")
        val COLOR_OPERATOR = Color.parseColor("#FF9500")
    }
}
