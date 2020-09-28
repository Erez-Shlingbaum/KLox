package interpreter

import lexer.Token
import lexer.TokenType
import lox.LoxRuntimeError
import lox.isEqual
import lox.isTruthy
import lox.stringify
import parser.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set
import kotlin.reflect.KFunction1


private fun checkNumberOperand(operator: Token, operand: Any?) {
    if (operand !is Double)
        throw LoxRuntimeError(operator, "Operand must be a number.")
}

private fun checkNumberOperands(operator: Token, leftOperand: Any?, rightOperand: Any?) {
    if (leftOperand !is Double || rightOperand !is Double)
        throw LoxRuntimeError(operator, "Operands must be a numbers.")
}

// This exception is used when returning from a function in lox
data class LoxReturnValue(val value: Any?) : RuntimeException()


class LoxInterpreter(val runtimeErrorReporter: KFunction1<LoxRuntimeError, Unit>) : Interpreter<Any?> {
    private val globalScope: Scope = Scope()

    // Define builtin functions
    init {
        globalScope.define("clock", LoxClockFunction)
        globalScope.define("readline", LoxReadLineFunction)
        globalScope.define("str", LoxStrFunction)
        globalScope.define("type", LoxTypeFunction)
    }

    // Initialize the interpreter's first scope to be the global scope
    private var currentScope: Scope = globalScope

    // Mapping of expressions to scope depths (how many jumps to do in scope stack to find the intended expression)
    private val locals: MutableMap<Expression, Int> = HashMap()

    /**
     * This method is used by the resolver. When the interpreter starts to interpret, (after the resolver finishes), the 'locals' hashmap is filled with values for every expression
     */
    fun resolve(expr: Expression, scopeDepth: Int) {
        locals[expr] = scopeDepth
    }

    /**
     * Interpret a list of statements
     */
    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements)
                execute(statement)
        } catch (e: LoxRuntimeError) {
            runtimeErrorReporter(e)
        }
    }

    /**
     * Execute a statement
     */
    private fun execute(statement: Stmt) {
        statement.interpretBy(this)
    }

    /**
     * Evaluate an expression
     */
    private fun eval(expr: Expression): Any? = expr.interpretBy(this)


    override fun interpretUnaryExpr(expr: UnaryExpression): Any? {
        val rightValue: Any? = eval(expr.rightExpr)
        return when (expr.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, rightValue)
                return -(rightValue as Double)
            }
            TokenType.BANG -> !isTruthy(rightValue)
            else -> null
        }
    }


    override fun interpretBinaryExpr(expr: BinaryExpression): Any? {
        val leftValue: Any? = eval(expr.leftExpr)
        val rightValue: Any? = eval(expr.rightExpr)

        return when (expr.operator.type) {
            // Arithmetic (and string concatenation)
            TokenType.PLUS -> {
                if (leftValue is Double && rightValue is Double)
                    leftValue + rightValue
                else if (leftValue is String && rightValue is String)
                    leftValue + rightValue
                else
                    throw LoxRuntimeError(expr.operator,
                        "Operator + not used correctly(can only add two numbers, or two strings): {$leftValue} + {$rightValue}")
            }
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                leftValue as Double - rightValue as Double
            }
            TokenType.STAR -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                leftValue as Double * rightValue as Double
            }
            TokenType.SLASH -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                if (rightValue as Double == 0.0)
                    throw LoxRuntimeError(expr.operator, "Division by zero!")
                leftValue as Double / rightValue
            }

            // Logic expressions
            TokenType.EQUAL_EQUAL -> isEqual(leftValue, rightValue)
            TokenType.BANG_EQUAL -> !isEqual(leftValue, rightValue)

            TokenType.GREATER -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                leftValue as Double > rightValue as Double
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                leftValue as Double >= rightValue as Double
            }
            TokenType.LESS -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                (leftValue as Double) < rightValue as Double
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                leftValue as Double <= rightValue as Double
            }

            else -> error("Something unexpected happened.")
        }

    }

    override fun interpretGroupingExpr(expr: GroupingExpression): Any? {
        return eval(expr.expression)
    }

    override fun interpretLiteralExpr(expr: LiteralExpression): Any? {
        return expr.value
    }

    override fun interpretPrintStmt(stmt: PrintStatement): Any? {
        val value = eval(stmt.expression)
        println(stringify(value))
        return Unit
    }

    override fun interpretFunStmt(stmt: FunStatement): Any? {
        val functions = LoxFunction(stmt, currentScope)
        currentScope.define(stmt.name.lexeme, functions)
        return Unit
    }

    override fun interpretExpressionStmt(stmt: ExpressionStatement): Any? {
        eval(stmt.expression)
        // TODO if this is a repl session, println(stringify(eval(stmt.expression)))
        return Unit
    }

    override fun interpretVarStmt(stmt: VarStatement): Any? {
        var value: Any? = null
        if (stmt.initializer != null)
            value = eval(stmt.initializer)

        currentScope.define(stmt.name.lexeme, value)
        return Unit
    }

    override fun interpretVariableExpression(expr: VariableExpression): Any? {
        return lookupVariable(expr.name, expr)
    }

    private fun lookupVariable(name: Token, expr: Expression): Any? {
        val depth = locals[expr]
        return if (depth == null)
            globalScope.get(name)
        else
            currentScope.getAt(depth, name.lexeme)
    }

    override fun interpretReturnStmt(stmt: ReturnStatement): Any? {
        val retValue = when (stmt.value) {
            null -> null
            else -> eval(stmt.value)
        }

        // Return the value to the calling function
        throw LoxReturnValue(retValue)
    }

    override fun interpretAssignmentExpression(expr: AssignmentExpression): Any? {
        val value = eval(expr.value)

        val depth = locals[expr]
        if (depth == null)
            globalScope.assign(expr.name, value)
        else
            currentScope.assignAt(depth, expr.name, value)

        currentScope.assign(expr.name, value)
        return value
    }

    override fun interpretLogicalExpr(expr: LogicalExpression): Any? {
        val leftValue = eval(expr.leftExpr)
        when (expr.operator.type) {
            TokenType.OR ->
                if (isTruthy(leftValue))
                    return leftValue
            TokenType.AND ->
                if (!isTruthy(leftValue))
                    return leftValue
            else -> error("Unknown logical expression")
        }

        return eval(expr.rightExpr)
    }

    override fun interpretBlockStmt(stmt: BlockStatement): Any? {
        executeBlock(stmt.statements, Scope(currentScope))
        return Unit
    }

    override fun executeBlock(statements: List<Stmt>, scope: Scope) {
        val previous = currentScope
        try {
            currentScope = scope
            for (statement in statements)
                execute(statement)
        } finally {
            this.currentScope = previous
        }
    }

    override fun interpretIfStmt(stmt: IfStatement): Any? {
        if (isTruthy(eval(stmt.condition)))
            execute(stmt.thenBranch)
        else if (stmt.elseBranch != null)
            execute(stmt.elseBranch)
        return Unit
    }

    override fun interpretCallExpression(expr: CallExpression): Any? {
        val callee = eval(expr.callee)
        val arguments: MutableList<Any?> = ArrayList()
        for (arg in expr.arguments)
            arguments += eval(arg)

        if (callee !is LoxCallable)
            throw LoxRuntimeError(expr.paren, "Can only call functions and classes.")
        if (arguments.size != callee.arity)
            throw LoxRuntimeError(expr.paren, "Expected ${callee.arity} arguments but got ${arguments.size} .")
        return callee.call(this, arguments)
    }

    override fun interpretWhileStmt(stmt: WhileStatement): Any? {
        while (isTruthy(eval(stmt.condition)))
            execute(stmt.body)
        return Unit
    }

    override fun interpretClassStmt(stmt: ClassStatement): Any? {
        var superclass: Any? = null
        if (stmt.superclass != null) {
            superclass = eval(stmt.superclass)
            if (superclass !is LoxClass)
                throw LoxRuntimeError(stmt.superclass.name, "Superclass must be a class.")
        }

        currentScope.define(stmt.name.lexeme, null)

        if (stmt.superclass != null) {
            currentScope = Scope(currentScope)
            currentScope.define("super", superclass)
        }

        val methods: MutableMap<String, LoxFunction> = HashMap()
        for (method in stmt.methods) {
            val function = LoxFunction(method, currentScope, method.name.lexeme == "init")
            methods[method.name.lexeme] = function
        }

        val loxClass = LoxClass(stmt.name.lexeme, superclass as LoxClass?, methods)

        if (superclass != null)
            currentScope = currentScope.enclosingScope ?: error("Unexpected. This should not ever be null.")

        currentScope.assign(stmt.name, loxClass)
        return Unit
    }

    override fun interpretGetExpression(expr: GetExpression): Any? {
        val loxObject = eval(expr.loxObject)
        if (loxObject is LoxInstance)
            return loxObject.get(expr.name)
        throw LoxRuntimeError(expr.name, "Only instances have properties.")
    }

    override fun interpretSetExpression(expr: SetExpression): Any? {
        val loxObject = eval(expr.loxObject)
        if (loxObject !is LoxInstance)
            throw LoxRuntimeError(expr.name, "Only instances have fields.")
        val value = eval(expr.value)
        loxObject.set(expr.name, value)
        return value
    }

    override fun interpretThisExpression(expr: ThisExpression): Any? = lookupVariable(expr.keyword, expr)

    override fun interpretSuperExpression(expr: SuperExpression): Any? {
        val depth = locals.getValue(expr)

        val superclass = currentScope.getAt(depth, "super") as LoxClass
        val loxInstance = currentScope.getAt(depth - 1, "this") as LoxInstance
        val method = superclass.findMethod(expr.method.lexeme) ?: throw LoxRuntimeError(expr.method,
            "Undefined property '${expr.method.lexeme}'.")

        return method.bind(loxInstance)
    }
}