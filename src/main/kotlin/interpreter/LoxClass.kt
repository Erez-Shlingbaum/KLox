package interpreter

import lexer.Token
import lox.LoxRuntimeError

class LoxClass(
    val name: String,
    private val superclass: LoxClass?,
    private val methods: MutableMap<String, LoxFunction>,
) : LoxCallable {
    override var arity: Int = 0
        get() {
            val initializer = findMethod("init") ?: return field
            return initializer.arity
        }

    override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
        val loxInstance = LoxInstance(this)
        // Call constructor if there's one
        findMethod("init")?.bind(loxInstance)?.call(interpreter, arguments)
        return loxInstance
    }

    fun findMethod(name: String): LoxFunction? {
        if (name in methods)
            return methods[name]
        if (superclass != null)
            return superclass.findMethod(name)

        return null
    }

    override fun toString(): String = "<class ${name}>"
}

/**
 * Any class the implements this class, can be used with get and set expressions
 */
interface LoxInstanceBase {
    fun get(name: Token): Any?
    fun set(name: Token, value: Any?)
}

open class LoxInstance(private val loxClass: LoxClass) : LoxInstanceBase {
    private val fields: MutableMap<String, Any?> = HashMap()

    override fun get(name: Token): Any? {
        if (name.lexeme in fields)
            return fields[name.lexeme]

        val method = loxClass.findMethod(name.lexeme)
        if (method != null)
            return method.bind(this)

        throw LoxRuntimeError(name, "Undefined property '${name.lexeme}'.")
    }

    override fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }

    override fun toString(): String = "${loxClass.name} instance"
}