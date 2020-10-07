package interpreter

import lexer.Token
import lox.LoxCallError
import lox.LoxRuntimeError

class LoxListInstance : LoxInstanceBase {
    // This is the list "constructor" function (builtin classes don't need "LoxClass.kt"
    companion object NewList : LoxCallable {
        override var arity: Int = 0

        override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
            return LoxListInstance()
        }
    }

    private val methods: MutableMap<String, LoxCallable> = HashMap()

    // Inner list object
    val list: MutableList<Any?> = ArrayList()

    // Initialize this class methods
    init {
        methods["push"] = object : LoxCallable {
            override var arity: Int = 1

            override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
                list += arguments[0]
                return null
            }
        }
        methods["pop"] = object : LoxCallable {
            override var arity: Int = 0

            override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
                return list.removeLastOrNull() ?: throw LoxCallError("List is empty!")
            }
        }
        methods["clear"] = object : LoxCallable {
            override var arity: Int = 0

            override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
                return list.clear()
            }
        }
        methods["len"] = object : LoxCallable {
            override var arity: Int = 0

            override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
                return list.size
            }
        }
        methods["get_at"] = object : LoxCallable {
            override var arity: Int = 1

            override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
                val index = arguments[0]
                if (index !is Int)
                    throw LoxCallError("index should be integer")
                if (index < 0 || index >= list.size)
                    throw LoxCallError("index is no in valid range")

                return list[index]
            }
        }
        methods["set_at"] = object : LoxCallable {
            override var arity: Int = 2

            override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
                val index = arguments[0]
                if (index !is Int)
                    throw LoxCallError("index should be integer")
                if (index < 0 || index >= list.size)
                    throw LoxCallError("index is no in valid range")
                list[index] = arguments[1]
                return null
            }
        }
    }

    override fun get(name: Token): Any? {
        if (name.lexeme !in methods)
            throw LoxRuntimeError(name, "Undefined property '${name.lexeme}'.")
        return methods[name.lexeme]
    }

    override fun set(name: Token, value: Any?) {
        throw LoxCallError("Can't set attribute for builtin classes.")
    }
}