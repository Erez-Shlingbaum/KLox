package interpreter

import lexer.Token
import lox.LoxRuntimeError

/**
 * Holds symbols for a scope
 */
class Scope(val enclosingScope: Scope? = null) {
    private val values: MutableMap<String, Any?> = HashMap()

    fun get(token: Token): Any? {
        if (token.lexeme !in values) {
            if (enclosingScope == null)
                throw LoxRuntimeError(token, "Undefined variable ${token.lexeme}.")
            return enclosingScope.get(token)
        }
        return values[token.lexeme]
    }

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun assign(token: Token, value: Any?) {
        if (token.lexeme in values) {
            values[token.lexeme] = value
            return
        }
        if (enclosingScope != null)
            return enclosingScope.assign(token, value)
        throw LoxRuntimeError(token, "Undefined variable ${token.lexeme}")
    }

    fun getAt(depth: Int, lexeme: String): Any? {
        return ancestor(depth).values[lexeme]
    }

    private fun ancestor(depth: Int): Scope {
        var scope: Scope = this
        for (i in 0 until depth)
            scope =
                scope.enclosingScope ?: error("this should not be null. The resolver has a bug.")

        return scope
    }

    fun assignAt(depth: Int, name: Token, value: Any?) {
        ancestor(depth).values[name.lexeme] = value
    }
}