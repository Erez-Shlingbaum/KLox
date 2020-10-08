package interpreter

import lexer.Token
import lox.LoxCallError
import lox.LoxRuntimeError

/**
 * @param values list of values to initialzie this list
 */
class LoxListInstance(values: List<Any?>? = null) : LoxInstanceBase {
    // This is the list "constructor" function (builtin classes don't need "LoxClass.kt"
    companion object NewList : LoxCallable {
        override var arity: Int = 0

        override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
            return LoxListInstance()
        }
    }

    // Inner list object
    val list: MutableList<Any?> = ArrayList()

    init {
        if (values != null)
            list.addAll(values)
    }

    private val methods: MutableMap<String, LoxCallable> = HashMap()

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

            override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? =
                list.removeLastOrNull() ?: throw LoxCallError("List is empty!")
        }
        methods["clear"] = object : LoxCallable {
            override var arity: Int = 0

            override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? = list.clear()
        }

        methods["slice"] = object : LoxCallable {
            override var arity: Int = 2

            override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
                val i = arguments[0]
                val j = arguments[1]
                if (i !is Int || j !is Int)
                    throw LoxCallError("slice: parameters should be integers")
                if (i < 0 || i >= list.size || j < 0 || j > list.size)
                    throw LoxCallError("slice: parameters are not in valid range")
                if (i >= j)
                    throw LoxCallError("slice: parameters first index should be smaller than second index")

                return LoxListInstance(list.slice(i until j))
            }
        }


        methods["len"] = object : LoxCallable {
            override var arity: Int = 0

            override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? = list.size
        }
        methods["get_at"] = object : LoxCallable {
            override var arity: Int = 1
            override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? = getAt(arguments[0])
        }
        methods["set_at"] = object : LoxCallable {
            override var arity: Int = 2

            override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? =
                setAt(arguments[0], arguments[1])
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

    fun getAt(index: Any?): Any? {
        if (index !is Int)
            throw LoxCallError("index should be integer")
        if (index < 0 || index >= list.size)
            throw LoxCallError("index is no in valid range")

        return list[index]
    }

    fun setAt(index: Any?, value: Any?): Nothing? {
        if (index !is Int)
            throw LoxCallError("index should be integer")
        if (index < 0 || index >= list.size)
            throw LoxCallError("index is no in valid range")
        list[index] = value
        return null
    }

    override fun toString(): String = list.joinToString(",", "[", "]")
}