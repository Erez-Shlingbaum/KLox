package interpreter

import lexer.Token
import parser.*
import java.util.*
import kotlin.collections.HashMap

/**
 * This class is responsible for resolving expressions, and detecting static errors, such as using "this" outside of a class method
 */
class Resolver(
    private val loxInterpreter: LoxInterpreter,
    private val reportError: (token: Token, msg: String) -> Unit,
) : Interpreter<Unit> {

    private enum class FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD,
    }

    private enum class ClassType {
        NONE,
        CLASS,
        SUBCLASS,
    }

    private var currentFunction = FunctionType.NONE
    private var currentClass = ClassType.NONE

    private val scopes = Stack<MutableMap<String, Boolean>>()

    private fun beginScope() {
        scopes.push(HashMap())
    }

    private fun endScope() {
        scopes.pop()
    }

    fun resolve(statements: List<Stmt>) {
        for (statement in statements)
            resolve(statement)
    }

    private fun resolve(statement: Stmt) {
        statement.interpretBy(this)

    }

    private fun resolve(expression: Expression) {
        expression.interpretBy(this)
    }

    private fun resolveLocal(expr: Expression, name: Token) {
        for (i in scopes.indices.reversed())
            if (name.lexeme in scopes[i]) {
                loxInterpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty())
            return
        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme))
            reportError(name, "Variable with this name already declared in this scope.")

        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty())
            return
        scopes.peek()[name.lexeme] = true
    }

    override fun interpretBlockStmt(stmt: BlockStatement) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    private fun resolveFunction(function: FunStatement, functionType: FunctionType) {
        // Switch current function type
        val enclosingFunction = currentFunction
        currentFunction = functionType

        beginScope()
        for (param in function.parameters) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()

        // Switch to original function type
        currentFunction = enclosingFunction
    }

    override fun interpretUnaryExpr(expr: UnaryExpression) {
        resolve(expr.rightExpr)
    }

    override fun interpretBinaryExpr(expr: BinaryExpression) {
        resolve(expr.leftExpr)
        resolve(expr.rightExpr)
    }

    override fun interpretGroupingExpr(expr: GroupingExpression) {
        resolve(expr.expression)
    }

    override fun interpretSquareBracketsExpr(expr: SquareBracketsExpression) {
        for (argument in expr.arguments)
            resolve(argument)
    }

    override fun interpretLiteralExpr(expr: LiteralExpression) = Unit

    override fun interpretVariableExpression(expr: VariableExpression) {
        if (!scopes.isEmpty() && scopes.peek()[expr.name.lexeme] == false)
            reportError(expr.name, "Cannot read local variable in its own initializer.")
        resolveLocal(expr, expr.name)
    }

    override fun interpretAssignmentExpression(expr: AssignmentExpression) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun interpretLogicalExpr(expr: LogicalExpression) {
        resolve(expr.leftExpr)
        resolve(expr.rightExpr)
    }

    override fun interpretCallExpression(expr: CallExpression) {
        resolve(expr.callee)
        for (argument in expr.arguments)
            resolve(argument)
    }

    override fun interpretExpressionStmt(stmt: ExpressionStatement) {
        resolve(stmt.expression)
    }

    override fun interpretVarStmt(stmt: VarStatement) {
        declare(stmt.name)
        if (stmt.initializer != null)
            resolve(stmt.initializer)

        define(stmt.name)
    }

    override fun interpretSetSquareBracketsExpression(expression: SetSquareBracketsExpression) {
        resolve(expression.callee)
        for (argument in expression.arguments)
            resolve(argument)
        resolve(expression.value)
    }

    override fun interpretIfStmt(stmt: IfStatement) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != null)
            resolve(stmt.elseBranch)
    }

    override fun interpretWhileStmt(stmt: WhileStatement) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }


    override fun interpretFunStmt(stmt: FunStatement) {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun interpretReturnStmt(stmt: ReturnStatement) {
        if (currentFunction == FunctionType.NONE)
            reportError(stmt.keyword, "Cannot return from top-level code.")
        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER)
                reportError(stmt.keyword, "Cannot return a value from an initializer.")
            resolve(stmt.value)
        }
    }

    override fun executeBlock(statements: List<Stmt>, scope: Scope): Unit =
        error("This method should not be used inside the resolver")

    override fun interpretClassStmt(stmt: ClassStatement) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS

        declare(stmt.name)
        define(stmt.name)

        if (stmt.superclass != null) {
            if (stmt.name.lexeme == stmt.superclass.name.lexeme)
                reportError(stmt.superclass.name, "A class cannot inherit from itself.")
            currentClass = ClassType.SUBCLASS
            resolve(stmt.superclass)
            beginScope()
            scopes.peek()["super"] = true
        }

        beginScope()
        scopes.peek()["this"] = true
        for (method in stmt.methods) {
            val declaration = when (method.name.lexeme) {
                "init" -> FunctionType.INITIALIZER
                else -> FunctionType.METHOD
            }
            resolveFunction(method, declaration)
        }

        endScope()
        if (stmt.superclass != null)
            endScope()

        currentClass = enclosingClass
    }

    override fun interpretSuperExpression(expr: SuperExpression) {
        if (currentClass == ClassType.NONE)
            reportError(expr.keyword, "Cannot use 'super' outside of a class.")
        else if (currentClass != ClassType.SUBCLASS)
            reportError(expr.keyword, "Cannot use 'super' outside of a class.")
        resolveLocal(expr, expr.keyword)
    }

    override fun interpretThisExpression(expr: ThisExpression) {
        if (currentClass == ClassType.NONE) {
            reportError(expr.keyword, "Cannot use 'this' outside of a class.")
            return
        }
        resolveLocal(expr, expr.keyword)
    }

    override fun interpretGetExpression(expr: GetExpression) {
        resolve(expr.loxObject)
    }

    override fun interpretSetExpression(expr: SetExpression) {
        resolve(expr.value)
        resolve(expr.loxObject)
    }
}
