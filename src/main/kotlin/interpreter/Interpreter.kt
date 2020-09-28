package interpreter

import parser.*

/**
 * Visitor design pattern: Interpreter is a visitor, and the AST is visited
 * */
interface Interpreter<R> {
    // Expressions
    fun interpretUnaryExpr(expr: UnaryExpression): R
    fun interpretBinaryExpr(expr: BinaryExpression): R
    fun interpretGroupingExpr(expr: GroupingExpression): R
    fun interpretLiteralExpr(expr: LiteralExpression): R
    fun interpretVariableExpression(expr: VariableExpression): R
    fun interpretAssignmentExpression(expr: AssignmentExpression): R
    fun interpretLogicalExpr(expr: LogicalExpression): R
    fun interpretCallExpression(expr: CallExpression): R
    fun interpretGetExpression(expr: GetExpression): R
    fun interpretSetExpression(expr: SetExpression): R
    fun interpretThisExpression(expr: ThisExpression): R
    fun interpretSuperExpression(expr: SuperExpression): R

    // Statements
    fun interpretExpressionStmt(stmt: ExpressionStatement): R
    fun interpretPrintStmt(stmt: PrintStatement): R
    fun interpretVarStmt(stmt: VarStatement): R
    fun interpretBlockStmt(stmt: BlockStatement): R
    fun interpretIfStmt(stmt: IfStatement): R
    fun interpretWhileStmt(stmt: WhileStatement): R
    fun interpretFunStmt(stmt: FunStatement): R
    fun interpretReturnStmt(stmt: ReturnStatement): R
    fun interpretClassStmt(stmt: ClassStatement): R

    // Execute a block of statements with the given scope
    fun executeBlock(statements: List<Stmt>, scope: Scope)
}