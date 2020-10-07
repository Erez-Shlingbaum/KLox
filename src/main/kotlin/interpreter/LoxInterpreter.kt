package interpreter

import lexer.Token
import lexer.TokenType
import lox.LoxCallError
import lox.LoxRuntimeError
import lox.isEqual
import lox.isTruthy
import parser.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set
import kotlin.math.pow
import kotlin.reflect.KFunction1

private fun checkIntsOperands(operator: Token, vararg operands: Any?) {
    for (operand in operands)
        if (operand !is Int)
            throw LoxRuntimeError(operator, "Operands must be ints.")
}

private fun checkNumberOperands(operator: Token, vararg operands: Any?) {
    for (operand in operands)
        if (operand !is Double && operand !is Int)
            throw LoxRuntimeError(operator, "Operands must be ints or floats.")
}


// This exception is used when returning from a function in lox
data class LoxReturnValue(val value: Any?) : RuntimeException()


class LoxInterpreter(val interpreterErrorReporter: KFunction1<LoxRuntimeError, Unit>) :
    Interpreter<Any?> {

    private val globalScope: Scope = Scope()

    // Define builtin functions
    init {
        globalScope.define("print", LoxPrintFunction)
        globalScope.define("clock", LoxClockFunction)
        globalScope.define("readline", LoxReadLineFunction)
        globalScope.define("int", LoxIntFunction)
        globalScope.define("float", LoxFloatFunction)
        globalScope.define("str", LoxStrFunction)
        globalScope.define("type", LoxTypeFunction)
        globalScope.define("list", LoxListInstance.NewList)
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
            interpreterErrorReporter(e)
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
                checkNumberOperands(expr.operator, rightValue)
                when (rightValue) {
                    is Int -> -rightValue
                    is Double -> -rightValue
                    else -> error("checkNumberOperand should have thrown a runtime exception...")
                }
            }
            TokenType.BANG -> !isTruthy(rightValue)
            TokenType.BIT_NOT -> {
                checkIntsOperands(expr.operator, rightValue)
                (rightValue as Int).inv()
            }
            else -> null // TODO i am pretty sure we need to error() here
        }
    }


    override fun interpretBinaryExpr(expr: BinaryExpression): Any? {
        val leftValue: Any? = eval(expr.leftExpr)
        val rightValue: Any? = eval(expr.rightExpr)

        return when (expr.operator.type) {
            // Arithmetic (and string concatenation)
            TokenType.PLUS -> when {
                // String concatenation
                leftValue is String && rightValue is String -> leftValue + rightValue

                // Ints / Doubles
                else -> {
                    // Make sure we are dealing with Ints / Doubles
                    try {
                        checkNumberOperands(expr.operator, leftValue, rightValue)
                    } catch (e: LoxRuntimeError) {
                        throw LoxRuntimeError(
                            expr.operator,
                            "Operator + not used correctly(can only add two numbers, or two strings): {$leftValue} + {$rightValue}"
                        )
                    }
                    when {
                        // If one is of the operands is double, cast result to double
                        leftValue is Double || rightValue is Double -> (leftValue as Number).toDouble() + (rightValue as Number).toDouble()
                        // In any other case, both are int, so cast the result to int
                        else -> leftValue as Int + rightValue as Int
                    }
                }
            }
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                when {
                    leftValue is Double || rightValue is Double -> (leftValue as Number).toDouble() - (rightValue as Number).toDouble()
                    else -> leftValue as Int - rightValue as Int
                }
            }
            TokenType.STAR -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                when {
                    leftValue is Double || rightValue is Double -> (leftValue as Number).toDouble() * (rightValue as Number).toDouble()
                    else -> leftValue as Int * rightValue as Int
                }
            }
            TokenType.PERCENT -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                when {
                    leftValue is Double || rightValue is Double -> (leftValue as Number).toDouble() % (rightValue as Number).toDouble()
                    else -> leftValue as Int % rightValue as Int
                }
            }

            TokenType.SLASH -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                if (rightValue as Number == 0.0)
                    throw LoxRuntimeError(expr.operator, "Division by zero!")
                when {
                    leftValue is Double || rightValue is Double -> (leftValue as Number).toDouble() / rightValue.toDouble()
                    else -> leftValue as Int / rightValue as Int
                }
            }
            TokenType.DOUBLE_STAR -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                when {
                    leftValue is Double || rightValue is Double || (rightValue as Int) < 0 -> (leftValue as Number).toDouble()
                        .pow((rightValue as Number).toDouble())
                    else -> ((leftValue as Int).toDouble()).pow(rightValue.toDouble()).toInt()
                }
            }
            // Bitwise expressions
            TokenType.BIT_OR -> {
                checkIntsOperands(expr.operator, leftValue, rightValue)
                (leftValue as Int) or (rightValue as Int)
            }
            TokenType.BIT_XOR -> {
                checkIntsOperands(expr.operator, leftValue, rightValue)
                (leftValue as Int) xor (rightValue as Int)
            }
            TokenType.BIT_AND -> {
                checkIntsOperands(expr.operator, leftValue, rightValue)
                (leftValue as Int) and (rightValue as Int)
            }
            TokenType.BIT_SHIFT_LEFT -> {
                checkIntsOperands(expr.operator, leftValue, rightValue)
                (leftValue as Int) shl (rightValue as Int)
            }
            TokenType.BIT_SHIFT_RIGHT -> {
                checkIntsOperands(expr.operator, leftValue, rightValue)
                (leftValue as Int) shr (rightValue as Int)
            }
            // Logic expressions
            TokenType.EQUAL_EQUAL -> isEqual(leftValue, rightValue)
            TokenType.BANG_EQUAL -> !isEqual(leftValue, rightValue)

            TokenType.GREATER -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                when {
                    leftValue is Double || rightValue is Double -> (leftValue as Number).toDouble() > (rightValue as Number).toDouble()
                    else -> leftValue as Int > rightValue as Int
                }
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                when {
                    leftValue is Double || rightValue is Double -> (leftValue as Number).toDouble() >= (rightValue as Number).toDouble()
                    else -> leftValue as Int >= rightValue as Int
                }
            }
            TokenType.LESS -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                when {
                    leftValue is Double || rightValue is Double -> (leftValue as Number).toDouble() < (rightValue as Number).toDouble()
                    else -> (leftValue as Int) < rightValue as Int
                }
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, leftValue, rightValue)
                when {
                    leftValue is Double || rightValue is Double -> (leftValue as Number).toDouble() <= (rightValue as Number).toDouble()
                    else -> leftValue as Int <= rightValue as Int
                }
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

    override fun interpretFunStmt(stmt: FunStatement): Any? {
        val functions = LoxFunction(stmt, currentScope)
        currentScope.define(stmt.name.lexeme, functions)
        return Unit
    }

    override fun interpretExpressionStmt(stmt: ExpressionStatement): Any? {
        eval(stmt.expression)
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
        val rightValue = eval(expr.value)

        return when (val depth = locals[expr]) {
            null -> when (expr.type) {
                TokenType.EQUAL -> {
                    globalScope.assign(expr.name, rightValue)
                    rightValue
                }

                TokenType.PLUS_EQUAL, TokenType.MINUS_EQUAL, TokenType.STAR_EQUAL,
                TokenType.SLASH_EQUAL, TokenType.DOUBLE_STAR_EQUAL, TokenType.PERCENT_EQUAL -> {
                    val currentVal = globalScope.get(expr.name)

                    // Make sure we are dealing with numbers
                    checkNumberOperands(expr.name, currentVal, rightValue)

                    val newVal = newValNumberOperation(expr.type, currentVal as Number, rightValue as Number)
                    globalScope.assign(expr.name, newVal)
                    newVal
                }
                // Bit operations
                TokenType.BIT_OR_EQUAL, TokenType.BIT_XOR_EQUAL, TokenType.BIT_AND_EQUAL, TokenType.BIT_SHIFT_LEFT_EQUAL, TokenType.BIT_SHIFT_RIGHT_EQUAL -> {
                    val currentVal = globalScope.get(expr.name)

                    // For bits operations, make sure we are dealing with ints
                    checkIntsOperands(expr.name, currentVal, rightValue)

                    val newVal = newValBitOperation(expr.type, currentVal as Int, rightValue as Int)
                    globalScope.assign(expr.name, newVal)
                    newVal
                }
                else -> error("Unexpected operation")
            }
            // depth != null
            else -> when (expr.type) {
                TokenType.EQUAL -> {
                    currentScope.assignAt(depth, expr.name, rightValue)
                    rightValue
                }
                TokenType.PLUS_EQUAL, TokenType.MINUS_EQUAL, TokenType.STAR_EQUAL,
                TokenType.SLASH_EQUAL, TokenType.DOUBLE_STAR_EQUAL, TokenType.PERCENT_EQUAL -> {
                    val currentVal = globalScope.getAt(depth, expr.name.lexeme)

                    // Make sure we are dealing with numbers
                    checkNumberOperands(expr.name, currentVal, rightValue)

                    val newVal = newValNumberOperation(expr.type, currentVal as Number, rightValue as Number)
                    globalScope.assignAt(depth, expr.name, newVal)
                    newVal
                }
                TokenType.BIT_OR_EQUAL, TokenType.BIT_XOR_EQUAL, TokenType.BIT_AND_EQUAL, TokenType.BIT_SHIFT_LEFT_EQUAL, TokenType.BIT_SHIFT_RIGHT_EQUAL -> {
                    // Bit operations
                    val currentVal = globalScope.getAt(depth, expr.name.lexeme)

                    // For bits operations, make sure we are dealing with ints
                    checkIntsOperands(expr.name, currentVal, rightValue)

                    val newVal = newValBitOperation(expr.type, currentVal as Int, rightValue as Int)
                    globalScope.assignAt(depth, expr.name, newVal)
                    newVal
                }
                else -> error("Unexpected operation")
            }
        }
    }

    private fun newValNumberOperation(type: TokenType, currentVal: Number, rightValue: Number): Number {
        return when (type) {
            TokenType.PLUS_EQUAL -> if (currentVal is Int && rightValue is Int) currentVal + rightValue else currentVal.toDouble() + rightValue.toDouble()
            TokenType.MINUS_EQUAL -> if (currentVal is Int && rightValue is Int) currentVal - rightValue else currentVal.toDouble() - rightValue.toDouble()
            TokenType.STAR_EQUAL -> if (currentVal is Int && rightValue is Int) currentVal * rightValue else currentVal.toDouble() * rightValue.toDouble()
            TokenType.SLASH_EQUAL -> if (currentVal is Int && rightValue is Int) currentVal / rightValue else currentVal.toDouble() / rightValue.toDouble()
            TokenType.PERCENT_EQUAL -> if (currentVal is Int && rightValue is Int) currentVal % rightValue else currentVal.toDouble() % rightValue.toDouble()
            TokenType.DOUBLE_STAR_EQUAL -> currentVal.toDouble().pow(rightValue.toDouble())
            else -> error("Unexpected operation")
        }
    }

    private fun newValBitOperation(
        type: TokenType,
        currentVal: Int,
        rightValue: Int
    ): Int {
        return when (type) {
            TokenType.BIT_OR_EQUAL -> currentVal or rightValue
            TokenType.BIT_XOR_EQUAL -> currentVal xor rightValue
            TokenType.BIT_AND_EQUAL -> currentVal and rightValue
            TokenType.BIT_SHIFT_LEFT_EQUAL -> currentVal shl rightValue
            TokenType.BIT_SHIFT_RIGHT_EQUAL -> currentVal shr rightValue
            else -> error("Unexpected operation")
        }
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
        try {
            return callee.call(this, arguments)
        } catch (e: LoxCallError) {
            throw LoxRuntimeError(expr.paren, e.msg)
        }
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
        if (loxObject is LoxInstanceBase)
            return loxObject.get(expr.name)
        throw LoxRuntimeError(expr.name, "Only instances have properties.")
    }

    override fun interpretSetExpression(expr: SetExpression): Any? {
        val loxObject = eval(expr.loxObject)
        if (loxObject !is LoxInstanceBase)
            throw LoxRuntimeError(expr.name, "Only instances have fields.")

        val rightValue = eval(expr.value)

        val currentVal = if (expr.type != TokenType.EQUAL) loxObject.get(expr.name) else null

        val newVal = when (expr.type) {
            TokenType.EQUAL -> rightValue

            TokenType.PLUS_EQUAL, TokenType.MINUS_EQUAL, TokenType.STAR_EQUAL,
            TokenType.SLASH_EQUAL, TokenType.DOUBLE_STAR_EQUAL, TokenType.PERCENT_EQUAL -> {
                // Make sure we are dealing with numbers
                checkNumberOperands(expr.name, currentVal, rightValue)
                newValNumberOperation(expr.type, currentVal as Number, rightValue as Number)
            }
            // Bit operations
            TokenType.BIT_OR_EQUAL, TokenType.BIT_XOR_EQUAL, TokenType.BIT_AND_EQUAL, TokenType.BIT_SHIFT_LEFT_EQUAL, TokenType.BIT_SHIFT_RIGHT_EQUAL -> {
                // For bits operations, make sure we are dealing with ints
                checkIntsOperands(expr.name, currentVal, rightValue)
                newValBitOperation(expr.type, currentVal as Int, rightValue as Int)
            }
            else -> error("Unexpected operation")
        }

        loxObject.set(expr.name, newVal)
        return newVal
    }

    override fun interpretThisExpression(expr: ThisExpression): Any? = lookupVariable(expr.keyword, expr)

    override fun interpretSuperExpression(expr: SuperExpression): Any? {
        val depth = locals.getValue(expr)

        val superclass = currentScope.getAt(depth, "super") as LoxClass
        val loxInstance = currentScope.getAt(depth - 1, "this") as LoxInstance
        val method = superclass.findMethod(expr.method.lexeme) ?: throw LoxRuntimeError(
            expr.method,
            "Undefined property '${expr.method.lexeme}'."
        )

        return method.bind(loxInstance)
    }
}