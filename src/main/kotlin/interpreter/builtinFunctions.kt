package interpreter

import lox.LoxCallError
import lox.stringify

object LoxPrintFunction : LoxCallable {
    override var arity: Int = 1

    override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
        println(stringify(arguments[0]))
        return null
    }

    override fun toString(): String {
        return "<native function: print>"
    }
}

object LoxClockFunction : LoxCallable {
    override var arity: Int = 0

    override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
        return System.currentTimeMillis().toDouble() / 1000.0
    }

    override fun toString(): String {
        return "<native function: clock>"
    }
}

object LoxReadLineFunction : LoxCallable {
    override var arity: Int = 1

    override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
        print(stringify(arguments[0]))
        return readLine()
    }

    override fun toString(): String {
        return "<native function: readline>"
    }
}

object LoxIntFunction : LoxCallable {
    override var arity: Int = 1

    override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
        val loxError = LoxCallError("Can not convert argument to integer")
        return when (val arg = arguments[0]) {
            is Int -> arg
            is Double -> arg.toInt()
            is String -> arg.toIntOrNull() ?: throw loxError
            else -> throw loxError
        }
    }

    override fun toString(): String {
        return "<native function: int>"
    }
}

object LoxFloatFunction : LoxCallable {
    override var arity: Int = 1

    override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
        val loxError = LoxCallError("Can not convert argument to float")
        return when (val arg = arguments[0]) {
            is Int -> arg.toDouble()
            is Double -> arg.toDouble()
            is String -> arg.toDoubleOrNull() ?: throw loxError
            else -> throw loxError
        }
    }

    override fun toString(): String {
        return "<native function: int>"
    }
}


object LoxStrFunction : LoxCallable {
    override var arity: Int = 1

    override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
        return stringify(arguments[0])
    }

    override fun toString(): String {
        return "<native function: str>"
    }
}

object LoxTypeFunction : LoxCallable {
    override var arity: Int = 1

    override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
        return when (val arg = arguments[0]) {
            is Int -> "<int>"
            is Double -> "<float>"
            is String -> "<str>"
            is Boolean -> "<bool>"
            is LoxClass -> arg.toString()
            is LoxInstance -> "<$arg>"
            is LoxCallable -> arg.toString()
            else -> "TODO" // Should not happen
        }
    }

    override fun toString(): String {
        return "<native function: type>"
    }
}