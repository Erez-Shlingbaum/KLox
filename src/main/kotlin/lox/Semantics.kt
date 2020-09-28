package lox

import parser.Expression

fun isEqual(leftValue: Any?, rightValue: Any?): Boolean {
    if (leftValue == null && rightValue == null)
        return true
    return leftValue == rightValue

}

fun isTruthy(value: Any?): Boolean {
    if (value == null)
        return false
    // Might solve some future bugs, passing expression instead of expression.something
    assert(value !is Expression)
    if (value is Boolean)
        return value
    return true
}

fun stringify(value: Any?): String {
    if (value == null)
        return "nil"

    when (value) {
        is Double -> {
            if (value % 1.0 == 0.0)
                return value.toInt().toString() // To print without trailing .0
            return value.toBigDecimal().toPlainString() // To print without scientific notation
        }
        else -> return value.toString()
    }

}