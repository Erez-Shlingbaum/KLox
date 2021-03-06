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
    // To print without scientific notation
    return when (value) {
        is Double -> value.toBigDecimal().toPlainString()
        is Int -> value.toString()
        else -> value.toString()
    }

}