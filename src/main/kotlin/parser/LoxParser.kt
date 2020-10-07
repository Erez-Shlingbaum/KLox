package parser

import lexer.Token
import lexer.TokenType
import java.util.*


/*

"Expressions produce values. lox.Lox has a number of unary and binary operators with different levels of precedence.
Some grammars for languages do not directly encode the precedence relationships and specify that elsewhere.
Here, we use a separate rule for each precedence level to make it explicit."

program        → declaration* EOF ;
///////////////
declaration    → classDecl
               | funDecl
               | varDecl
               | statement ;

classDecl      → "class" IDENTIFIER ( "<" IDENTIFIER )?
                 "{" function* "}" ;
funDecl        → "fun" function ;
varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
///////////////
statement      → exprStmt
               | forStmt
               | ifStmt
               | returnStmt
               | whileStmt
               | block ;

exprStmt       → expression ";" ;
forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
                           expression? ";"
                           expression? ")" statement ;
ifStmt         → "if" "(" expression ")" statement
                 ( "else" statement )? ;
returnStmt     → "return" expression? ";" ;
whileStmt      → "while" "(" expression ")" statement ;
block          → "{" declaration* "}" ;

////////////////
expression     → assignment ;

assignment     → ( call "." )? IDENTIFIER ("=" | "+=" | "-=" | "*=" | "/=" | "%=" | "**=" | "|=" | "^=" | "&=" | "<<=" | ">>=") assignment
               | logic_or ;

logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → bit_or ( ( ">" | ">=" | "<" | "<=" ) bit_or )* ;

bit_or         → bit_xor    ("|" bit_xor)*  ;
bit_xor        → bit_and    ("^" bit_and)*  ;
bit_and        → bit_shift  ("&" bit_shift)*;
bit_shift      → addition   (("<<" | ">>") bit_xor)*  ;

addition       → multiplication ( ( "-" | "+" ) multiplication )* ;
multiplication → power ( ( "/" | "*" ) power )* ;
unary          → ( "!" | "-"  | "~") unary | call ;
power          → call ( "**" unary )* ;
call           → primary ( "(" arguments? ")" | "[" arguments? "]" | "." IDENTIFIER )* ;
primary        → "true" | "false" | "nil" | "this"
               | NUMBER | STRING | IDENTIFIER | "(" expression ")" | "[" arguments? "]"
               | "super" "." IDENTIFIER ;


function       → IDENTIFIER "(" parameters? ")" block ;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
arguments      → expression ( "," expression )* ;

*/


class LoxParser(private val tokens: List<Token>, private val reportError: (token: Token, msg: String) -> Unit) {
    private var current: Int = 0

    fun parse(): List<Stmt> {
        val statements: MutableList<Stmt> = ArrayList()
        while (!isEOF()) {
            val statement = parseDeclaration() ?: return statements
            statements += statement
        }
        return statements
    }

    private fun parseDeclaration(): Stmt? {
        try {
            if (match(TokenType.VAR))
                return parseVarDeclaration()
            return parseStatement()
        } catch (err: LoxParseError) {
            synchronize()
            return null
        }
    }

    private fun synchronize() {
        advance()
        while (!isEOF()) {
            if (previous().type == TokenType.SEMICOLON)
                return

            when (peek().type) {
                TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR, TokenType.IF, TokenType.WHILE, TokenType.RETURN,
                -> return
                else -> advance()
            }
        }
    }

    private fun parseVarDeclaration(): Stmt {
        val name: Token = consume(TokenType.IDENTIFIER, "Expect variable name.")

        var initializer: Expression? = null
        if (match(TokenType.EQUAL))
            initializer = parseExpression()

        consume(TokenType.SEMICOLON, "Expect ';' after var declaration.")
        return VarStatement(name, initializer)
    }

    private fun parseStatement(): Stmt {
        return when {
            match(TokenType.OPEN_BRACE) -> BlockStatement(parseBlock())
            match(TokenType.IF) -> parseIfStatement()
            match(TokenType.WHILE) -> parseWhileStatement()
            match(TokenType.FOR) -> parseForStatement()
            match(TokenType.FUN) -> parseFunStatement("function")
            match(TokenType.RETURN) -> parseReturnStatement()
            match(TokenType.CLASS) -> parseClassStatement()
            else -> parseExpressionStatement()
        }

    }

    private fun parseClassStatement(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect class name.")

        val superclass = when {
            match(TokenType.LESS) -> {
                consume(TokenType.IDENTIFIER, "Expect superclass name.")
                VariableExpression(previous())
            }
            else -> null
        }

        consume(TokenType.OPEN_BRACE, "Expect '{' before class body.")

        val methods: MutableList<FunStatement> = ArrayList()
        while (!check(TokenType.CLOSE_BRACE) && !isEOF())
            methods.add(parseFunStatement("method") as FunStatement)

        consume(TokenType.CLOSE_BRACE, "Expect '}' after class body.")

        return ClassStatement(name, superclass, methods)
    }

    private fun parseReturnStatement(): Stmt {
        val keyword = previous()
        val value: Expression? = when {
            check(TokenType.SEMICOLON) -> null
            else -> parseExpression()
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return value.")
        return ReturnStatement(keyword, value)
    }

    /**
     * @param kind should be "function" or "method"
     */
    private fun parseFunStatement(kind: String): Stmt {
        assert(kind == "function" || kind == "method")
        val name: Token = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        consume(TokenType.OPEN_PAREN, "Expect '(' after $kind name.")
        val parameters: MutableList<Token> = ArrayList()
        if (!check(TokenType.CLOSE_PAREN)) do {
            if (parameters.size >= 255)
                reportError(peek(), "Cannot have more than 255 parameters.")

            parameters += consume(TokenType.IDENTIFIER, "Expect parameter name.")
        } while (match(TokenType.COMMA))

        consume(TokenType.CLOSE_PAREN, "Expect ')' after parameters.")
        consume(TokenType.OPEN_BRACE, "Expect '{' before $kind body.")
        return FunStatement(name, parameters, parseBlock())
    }

    private fun parseForStatement(): Stmt {
        consume(TokenType.OPEN_PAREN, "Expect '(' after 'for'.")

        // Parse initializer
        val initializer: Stmt? = when {
            match(TokenType.SEMICOLON) -> null
            match(TokenType.VAR) -> parseVarDeclaration()
            else -> parseExpressionStatement()
        }

        // Parse condition
        val condition: Expression = when {
            check(TokenType.SEMICOLON) -> LiteralExpression(true)
            else -> parseExpression()
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.")

        // Parse for increment expression
        val increment: Expression? = when {
            check(TokenType.CLOSE_PAREN) -> null
            else -> parseExpression()
        }
        consume(TokenType.CLOSE_PAREN, "Expect ')' after for clauses.")

        // Parse body
        var body: Stmt = parseStatement()

        // Add increment statement to end of body
        if (increment != null)
            body = BlockStatement(listOf(body, ExpressionStatement(increment)))

        // Desugar 'for' statement into a while statement
        body = WhileStatement(condition, body)

        // Add initializer statement to start of the body
        if (initializer != null)
            body = BlockStatement(listOf(initializer, body))
        return body
    }

    private fun parseWhileStatement(): Stmt {
        consume(TokenType.OPEN_PAREN, "Expect '(' after while.")
        val condition = parseExpression()
        consume(TokenType.CLOSE_PAREN, "Expect ')' after while condition.")
        val body = parseStatement()
        return WhileStatement(condition, body)
    }

    private fun parseIfStatement(): Stmt {
        consume(TokenType.OPEN_PAREN, "Expect '(' after if.")
        val condition = parseExpression()
        consume(TokenType.CLOSE_PAREN, "Expect ')' after if condition.")

        val thenBranch = parseStatement()
        val elseBranch: Stmt? = when {
            match(TokenType.ELSE) -> parseStatement()
            else -> null
        }
        return IfStatement(condition, thenBranch, elseBranch)
    }

    private fun parseBlock(): List<Stmt> {
        val statements: MutableList<Stmt> = ArrayList()

        while (!check(TokenType.CLOSE_BRACE) && !isEOF())
            statements += parseDeclaration() ?: error("unexpected: parsed declaration is null")

        consume(TokenType.CLOSE_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun parseExpression(): Expression = parseAssignmentExpression()

    private fun parseExpressionStatement(): Stmt {
        val expr: Expression = parseExpression()
        consume(TokenType.SEMICOLON, "Expect ; after value.")
        return ExpressionStatement(expr)
    }

    private fun parseAssignmentExpression(): Expression {
        val expr = parseOrExpression()
        if (match(
                TokenType.EQUAL,
                TokenType.PLUS_EQUAL,
                TokenType.MINUS_EQUAL,
                TokenType.STAR_EQUAL,
                TokenType.SLASH_EQUAL,
                TokenType.DOUBLE_STAR_EQUAL,
                TokenType.PERCENT_EQUAL,
                TokenType.BIT_OR_EQUAL,
                TokenType.BIT_XOR_EQUAL,
                TokenType.BIT_AND_EQUAL,
                TokenType.BIT_SHIFT_LEFT_EQUAL,
                TokenType.BIT_SHIFT_RIGHT_EQUAL
            )
        ) {
            val prev = previous()
            val value = parseAssignmentExpression()

            if (expr is GetExpression)
                return SetExpression(expr.loxObject, expr.name, value, prev.type)
            if (expr is VariableExpression)
                return AssignmentExpression(expr.name, value, prev.type)
            if (expr is CallExpression && expr.paren.type == TokenType.CLOSE_SQUARE_BRACKET)
                return SetSquareBracketsExpression(expr.callee, expr.arguments, value, prev)
            throw parsingError(prev, "Invalid assignment target.")
        }
        return expr
    }

    private fun parseOrExpression(): Expression {
        var expr = parseAndExpression()

        while (match(TokenType.OR)) {
            val operator = previous()
            val right = parseAndExpression()
            expr = LogicalExpression(expr, operator, right)
        }

        return expr
    }

    private fun parseAndExpression(): Expression {
        var expr = parseEqualityExpression()

        while (match(TokenType.AND)) {
            val operator = previous()
            val right = parseEqualityExpression()
            expr = LogicalExpression(expr, operator, right)
        }

        return expr
    }

    private fun parseEqualityExpression(): Expression {
        var expr = parseComparison()
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator = previous()
            val rightExpr = parseComparison()
            expr = BinaryExpression(expr, operator, rightExpr)
        }
        return expr
    }

    private fun parseComparison(): Expression {
        var expr = parseBitOr()
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val operator = previous()
            val rightExpr = parseBitOr()
            expr = BinaryExpression(expr, operator, rightExpr)
        }
        return expr
    }

    private fun parseBitOr(): Expression {
        var expr = parseBitXor()
        while (match(TokenType.BIT_OR)) {
            val operator = previous()
            val rightExpr = parseBitXor()
            expr = BinaryExpression(expr, operator, rightExpr)
        }
        return expr
    }

    private fun parseBitXor(): Expression {
        var expr = parseBitAnd()
        while (match(TokenType.BIT_XOR)) {
            val operator = previous()
            val rightExpr = parseBitAnd()
            expr = BinaryExpression(expr, operator, rightExpr)
        }
        return expr
    }

    private fun parseBitAnd(): Expression {
        var expr = parseBitShift()
        while (match(TokenType.BIT_AND)) {
            val operator = previous()
            val rightExpr = parseBitShift()
            expr = BinaryExpression(expr, operator, rightExpr)
        }
        return expr
    }

    private fun parseBitShift(): Expression {
        var expr = parseAddition()
        while (match(TokenType.BIT_SHIFT_LEFT, TokenType.BIT_SHIFT_RIGHT)) {
            val operator = previous()
            val rightExpr = parseAddition()
            expr = BinaryExpression(expr, operator, rightExpr)
        }
        return expr
    }

    private fun parseAddition(): Expression {
        var expr = parseMultiplication()
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val operator = previous()
            val rightExpr = parseMultiplication()
            expr = BinaryExpression(expr, operator, rightExpr)
        }
        return expr
    }

    private fun parseMultiplication(): Expression {
        var expr: Expression = parseUnary()
        while (match(TokenType.SLASH, TokenType.STAR, TokenType.PERCENT)) {
            val operator = previous()
            val rightExpr = parseUnary()
            expr = BinaryExpression(expr, operator, rightExpr)
        }
        return expr
    }

    private fun parseUnary(): Expression {
        if (match(TokenType.BANG, TokenType.MINUS, TokenType.BIT_NOT)) {
            val operator = previous()
            val rightExpr = parseUnary()
            return UnaryExpression(operator, rightExpr)
        }
        return parsePower()
    }

    private fun parsePower(): Expression {
        var expr: Expression = parseCall()
        while (match(TokenType.DOUBLE_STAR)) {
            val operator = previous()
            val rightExpr = parseUnary()
            expr = BinaryExpression(expr, operator, rightExpr)
        }
        return expr
    }

    private fun parseCall(): Expression {
        var expr = parsePrimary()

        loop@ while (true) expr = when {
            match(TokenType.OPEN_PAREN) -> parseOneCall(expr)
            match(TokenType.OPEN_SQUARE_BRACKET) -> parseSquareBracketsCallExpression(expr)
            match(TokenType.DOT) -> {
                val name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.")
                GetExpression(expr, name)
            }
            else -> {
                break@loop
            }
        }

        return expr
    }

    // Helper function: Parse one call expression
    private fun parseOneCall(callee: Expression): Expression {
        val (arguments, paren) = parseArguments(TokenType.CLOSE_PAREN, ")", 255)
        return CallExpression(callee, paren, arguments)
    }

    private fun parseSquareBracketsCallExpression(callee: Expression): Expression {
        val (arguments, paren) = parseArguments(TokenType.CLOSE_SQUARE_BRACKET, "]", 1)
        return CallExpression(callee, paren, arguments)
    }

    private fun parseArguments(
        closingToken: TokenType,
        closingTokenAsStr: String,
        upperArgSizeLimit: Int?
    ): Pair<MutableList<Expression>, Token> {
        val arguments: MutableList<Expression> = ArrayList()
        if (!check(closingToken)) do {
            // return value is intentionally not thrown
            if (upperArgSizeLimit != null)
                if (arguments.size == upperArgSizeLimit)
                    parsingError(peek(), "Cannot have more than $upperArgSizeLimit arguments.")

            arguments += parseExpression()
        } while (match(TokenType.COMMA))
        val paren = consume(closingToken, "Expect '$closingTokenAsStr' after arguments.")
        return Pair(arguments, paren)
    }

    private fun parsePrimary(): Expression {
        return when {
            match(TokenType.FALSE) -> LiteralExpression(false)
            match(TokenType.TRUE) -> LiteralExpression(true)
            match(TokenType.NIL) -> LiteralExpression(null)
            match(
                TokenType.NUMBER_FLOAT,
                TokenType.NUMBER_INT,
                TokenType.STRING
            ) -> LiteralExpression(previous().literal)
            match(TokenType.IDENTIFIER) -> VariableExpression(previous())
            match(TokenType.OPEN_PAREN) -> {
                val expr = parseExpression()
                consume(TokenType.CLOSE_PAREN, "No ')' after expression")
                GroupingExpression(expr)
            }
            match(TokenType.OPEN_SQUARE_BRACKET) -> {
                val (arguments: MutableList<Expression>, paren) = parseArguments(
                    TokenType.CLOSE_SQUARE_BRACKET,
                    "]",
                    null
                )
                SquareBracketsExpression(paren, arguments)
            }

            match(TokenType.THIS) -> ThisExpression(previous())
            match(TokenType.SUPER) -> {
                val keyword = previous()
                consume(TokenType.DOT, "Expect '.' after 'super'.")
                val method = consume(TokenType.IDENTIFIER, "Expect superclass method name.")
                SuperExpression(keyword, method)
            }
            else -> throw parsingError(peek(), "Unexpected token")
        }
    }


    /****** Utility functions ******/
    private fun isEOF(): Boolean = peek().type == TokenType.EOF
    private fun previous() = tokens[current - 1]
    private fun peek(): Token = tokens[current]

    private fun advance(): Token {
        if (!isEOF())
            current++
        return previous()
    }

    /**
     * If current token is equal to one of the tokens this functions gets, advance the parser
     */
    private fun match(vararg tokenTypes: TokenType): Boolean {

        for (tokenType: TokenType in tokenTypes)
            if (check(tokenType)) {
                advance()
                return true
            }
        return false
    }

    private fun consume(type: TokenType, msg: String): Token {
        if (check(type))
            return advance()
        throw parsingError(peek(), msg)
    }

    private fun parsingError(token: Token, msg: String): Throwable {
        reportError(token, msg)
        return LoxParseError()
    }

    class LoxParseError : Throwable()

    private fun check(tokenType: TokenType): Boolean {
        if (isEOF())
            return false
        return peek().type == tokenType
    }

}