package lexer

enum class TokenType {
    // Operators, etc.
    OPEN_PAREN, CLOSE_PAREN, OPEN_BRACE, CLOSE_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,
    PERCENT, DOUBLE_STAR,

    // Logical
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // Bits
    BIT_NOT,
    BIT_AND, BIT_AND_EQUAL,
    BIT_OR, BIT_OR_EQUAL,
    BIT_XOR, BIT_XOR_EQUAL,
    BIT_SHIFT_RIGHT, BIT_SHIFT_RIGHT_EQUAL,
    BIT_SHIFT_LEFT, BIT_SHIFT_LEFT_EQUAL,


    // Literals.
    IDENTIFIER, STRING, NUMBER_FLOAT, NUMBER_INT,

    // Keywords.
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE, IN,

    EOF,
}

/**
 * Data class representing a lexical token
 * @param lexeme string representation of the token, found in the script itself
 *
 * @param type type of the token
 * */
data class Token(val type: TokenType, val lexeme: String, val literal: Any?, val line: Int)


/**
 * Lexer for lox language
 *
 * @param source expression to lex
 * @param reportError function that gets line number, msg as parameter and reports it as needed. If this function throws an exception, the lexer will pass it on..
 */
class Lexer(private val source: String, private val reportError: (line: Int, msg: String) -> Unit) {
    companion object {
        val keywords: Map<String, TokenType> = hashMapOf(
            "and" to TokenType.AND,
            "class" to TokenType.CLASS,
            "else" to TokenType.ELSE,
            "false" to TokenType.FALSE,
            "for" to TokenType.FOR,
            "fun" to TokenType.FUN,
            "if" to TokenType.IF,
            "nil" to TokenType.NIL,
            "or" to TokenType.OR,
            "print" to TokenType.PRINT,
            "return" to TokenType.RETURN,
            "super" to TokenType.SUPER,
            "this" to TokenType.THIS,
            "true" to TokenType.TRUE,
            "var" to TokenType.VAR,
            "while" to TokenType.WHILE,
            "in" to TokenType.IN,
        )
    }

    private val result: MutableList<Token> = ArrayList()

    private var start: Int = 0
    private var current: Int = 0
    private var line: Int = 1

    fun scan(): List<Token> {
        while (!isEOF()) {
            start = current
            scanToken()
        }
        result += Token(TokenType.EOF, "", null, line)
        return result
    }

    private fun scanToken() {
        when (val c: Char = advance()) {
            '(' -> addToken(TokenType.OPEN_PAREN)
            ')' -> addToken(TokenType.CLOSE_PAREN)
            '{' -> addToken(TokenType.OPEN_BRACE)
            '}' -> addToken(TokenType.CLOSE_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '%' -> addToken(TokenType.PERCENT)
            '~' -> addToken(TokenType.BIT_NOT)
            '*' -> addToken(if (match('*')) TokenType.DOUBLE_STAR else TokenType.STAR)
            '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '^' -> addToken(if (match('=')) TokenType.BIT_XOR_EQUAL else TokenType.BIT_XOR)
            '&' -> addToken(if (match('=')) TokenType.BIT_AND_EQUAL else TokenType.BIT_AND)
            '|' -> addToken(if (match('=')) TokenType.BIT_OR_EQUAL else TokenType.BIT_OR)
            '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '<' -> when {
                match('<') -> addToken(if (match('=')) TokenType.BIT_SHIFT_LEFT_EQUAL else TokenType.BIT_SHIFT_LEFT)
                else -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            }
            '>' -> when {
                match('>') -> addToken(if (match('=')) TokenType.BIT_SHIFT_RIGHT_EQUAL else TokenType.BIT_SHIFT_RIGHT)
                else -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            }
            '/' -> {
                // Comment
                if (match('/'))
                    while (peek() != '\n' && !isEOF())
                        advance()
                else
                    addToken(TokenType.SLASH)
            }
            ' ', '\r', '\t' -> Unit
            '\n' -> line++
            '"' -> scanString()
            else -> {
                when {
                    isDigit(c) -> scanNumber()
                    isAlpha(c) -> scanIdentifier()
                    else -> reportError(line, "Unexpected character")
                }
            }
        }
    }

    private fun scanIdentifier() {
        while (isAlphaNumeric(peek()))
            advance()

        // Check if identifier is a reserved word
        val identifier: String = source.substring(start until current)

        val type = if (identifier in keywords)
            keywords.getValue(identifier)
        else
            TokenType.IDENTIFIER


        addToken(type)
    }

    private fun isAlphaNumeric(c: Char): Boolean {
        return isAlpha(c) || isDigit(c)
    }

    private fun isAlpha(c: Char): Boolean {
        return c in 'a'..'z' || c in 'A'..'Z' || c == '_'
    }

    private fun scanNumber() {
        while (isDigit(peek()))
            advance()

        var isDouble = false
        if (peek() == '.' && isDigit(peekNext())) {
            isDouble = true
            // Consume '.'
            advance()

            while (isDigit(peek()))
                advance()
        }
        val substring = source.substring(start until current)

        val num: Number = when {
            isDouble -> substring.toDoubleOrNull() ?: run {
                reportError(line, "Could not convert literal number to double ==> $substring")
                return
            }
            else -> substring.toIntOrNull() ?: run {
                reportError(line, "Could not convert literal number to int ==> $substring")
                return
            }
        }

        addToken(if (num is Double) TokenType.NUMBER_FLOAT else TokenType.NUMBER_INT, num)
    }

    private fun peekNext(): Char {
        if (current + 1 >= source.length)
            return '\u0000'
        return source[current + 1]
    }

    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }

    private fun scanString() {
        val strBuilder = StringBuilder()

        while (peek() != '"' && !isEOF()) {
            when (peek()) {
                '\\' -> {
                    // Consume the \
                    advance()
                    if (isEOF())
                        return reportError(line, "String has a stray escape character: '\\'")

                    when (peek()) {
                        '\\' -> strBuilder.append("\\")
                        '"' -> strBuilder.append("\"")
                        'n' -> strBuilder.append("\n")
                        'r' -> strBuilder.append("\r")
                        't' -> strBuilder.append("\t")
                        else -> reportError(line, "String has a stray escape character: '\\'")
                    }
                }
                '\n' -> {
                    strBuilder.append(peek()) // TODO Maybe append System.lineSeparator() instead of peek
                    line++
                }
                else -> strBuilder.append(peek())
            }
            advance()
        }

        if (isEOF())
            return reportError(line, "Unterminated string")

        // consume the closing '"'
        advance()

        addToken(TokenType.STRING, strBuilder.toString())
    }

    private fun peek(): Char {
        if (isEOF())
            return '\u0000'
        return source[current]
    }

    /**
     * Advances only if the expected char is matched
     */
    private fun match(expectedChar: Char): Boolean {
        if (isEOF())
            return false
        if (source[current] != expectedChar)
            return false
        current++
        return true
    }

    private fun addToken(type: TokenType, literal: Any? = null) {
        result += Token(type, source.substring(start until current), literal, line)
    }

    private fun advance(): Char {
        current++
        return source[current - 1]
    }

    private fun isEOF(): Boolean {
        return current >= source.length
    }

}