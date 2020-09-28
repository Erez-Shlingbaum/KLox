package interpreter

import parser.FunStatement

internal interface LoxCallable {
    var arity: Int
    fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any?
}

/**
 * @param declaration function declaration statement, to execute
 * @param closure scope context in which to run this function
 * @param isInitializer is this function a constructor or not
 */
class LoxFunction(
    private val declaration: FunStatement,
    private val closure: Scope,
    private val isInitializer: Boolean = false,
) : LoxCallable {
    override var arity: Int = declaration.parameters.size

    override fun call(interpreter: Interpreter<Any?>, arguments: List<Any?>): Any? {
        val newScope = Scope(closure)

        // Define function parameters inside scope with the given value
        for (i in declaration.parameters.indices)
            newScope.define(declaration.parameters[i].lexeme, arguments[i])
        try {
            interpreter.executeBlock(declaration.body, newScope)
        } catch (retValue: LoxReturnValue) {
            if (isInitializer)
                return closure.getAt(0, "this")
            return retValue.value
        }

        // If init function is explicitly called, return this, instead of nil
        if (isInitializer)
            return closure.getAt(0, "this")
        return null
    }

    override fun toString(): String {
        return "<fun: ${declaration.name.lexeme}>"
    }

    fun bind(loxInstance: LoxInstance): LoxFunction {
        val scope = Scope(closure)
        scope.define("this", loxInstance)
        return LoxFunction(declaration, scope, isInitializer)
    }
}