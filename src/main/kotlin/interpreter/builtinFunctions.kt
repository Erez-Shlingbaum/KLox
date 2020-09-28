package interpreter

import lox.stringify

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