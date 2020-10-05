package parser

import interpreter.Interpreter
import lexer.Token
import lexer.TokenType


interface Expression {
    fun <R> interpretBy(interpreter: Interpreter<R>): R
}

class UnaryExpression(val operator: Token, val rightExpr: Expression) : Expression {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretUnaryExpr(this)
}

class BinaryExpression(val leftExpr: Expression, val operator: Token, val rightExpr: Expression) : Expression {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretBinaryExpr(this)
}

class LogicalExpression(val leftExpr: Expression, val operator: Token, val rightExpr: Expression) : Expression {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretLogicalExpr(this)
}

class GroupingExpression(val expression: Expression) : Expression {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretGroupingExpr(this)
}

class LiteralExpression(val value: Any?) : Expression {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretLiteralExpr(this)
}

class VariableExpression(val name: Token) : Expression {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretVariableExpression(this)
}

class AssignmentExpression(val name: Token, val value: Expression, val type: TokenType) : Expression {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretAssignmentExpression(this)
}

class CallExpression(val callee: Expression, val paren: Token, val arguments: List<Expression>) : Expression {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretCallExpression(this)
}

class GetExpression(val loxObject: Expression, val name: Token) : Expression {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretGetExpression(this)
}

class SetExpression(val loxObject: Expression, val name: Token, val value: Expression, val type: TokenType) : Expression {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretSetExpression(this)
}

class ThisExpression(val keyword: Token) : Expression {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretThisExpression(this)
}

class SuperExpression(val keyword: Token, val method: Token) : Expression {
    override fun <R> interpretBy(interpreter: Interpreter<R>): R = interpreter.interpretSuperExpression(this)
}