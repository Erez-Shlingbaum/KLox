package parser

import interpreter.Interpreter
import lexer.Token

interface Stmt {
    fun <R> interpretBy(interpreter: Interpreter<R>): R
}

class ExpressionStatement(val expression: Expression) : Stmt {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretExpressionStmt(this)
}

class VarStatement(val name: Token, val initializer: Expression?) : Stmt {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretVarStmt(this)
}


class BlockStatement(val statements: List<Stmt>) : Stmt {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretBlockStmt(this)
}

class IfStatement(val condition: Expression, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretIfStmt(this)
}

class WhileStatement(val condition: Expression, val body: Stmt) : Stmt {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretWhileStmt(this)
}

class FunStatement(val name: Token, val parameters: List<Token>, val body: List<Stmt>) : Stmt {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretFunStmt(this)
}

class ReturnStatement(val keyword: Token, val value: Expression?) : Stmt {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretReturnStmt(this)
}

class ClassStatement(val name: Token, val superclass: VariableExpression?, val methods: List<FunStatement>) : Stmt {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretClassStmt(this)
}
