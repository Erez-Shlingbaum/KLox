package interpreter

import lexer.Token
import lox.LoxRuntimeError

//val LoxObjectClass: LoxClass = LoxClass("object", null, hashMapOf())

class LoxClass(
    val name: String,
    private val superclass: LoxClass?,
    private val methods: MutableMap<String, LoxFunction>,
) : LoxCallable {

//    private val superclass: LoxClass?
//
//    init {
//         If this class has no superclass, automatically inherit LoxObjectClass
//        if (superclass == null && name != "object") {
//            this.superclass = LoxClass("object", null, hashMapOf(
//                "str" to LoxFunction(FunStatement())
//            ))
//        } else
//            this.superclass = null
//    }

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

class LoxInstance(private val loxClass: LoxClass) {
    private val fields: MutableMap<String, Any?> = HashMap()

    fun get(name: Token): Any? {
        if (name.lexeme in fields)
            return fields[name.lexeme]

        val method = loxClass.findMethod(name.lexeme)
        if (method != null)
            return method.bind(this)

        throw LoxRuntimeError(name, "Undefined property '${name.lexeme}'.")
    }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }

    override fun toString(): String = "${loxClass.name} instance"
}
