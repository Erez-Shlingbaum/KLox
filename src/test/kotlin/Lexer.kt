import lexer.Lexer
import lexer.Token
import lexer.TokenType
import java.io.File
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class LexerTest : LoxTest() {
    private fun reporter(i: Int, s: String) {}

    @Test
    fun testArithmetic() {
        val expectedResult: List<Token> = listOf(
            Token(TokenType.PLUS, "+", null, 1),
            Token(TokenType.MINUS, "-", null, 1),
            Token(TokenType.SLASH, "/", null, 1),
            Token(TokenType.STAR, "*", null, 1),
            Token(TokenType.NUMBER_FLOAT, "123.321", 123.321, 1),
            Token(TokenType.NUMBER_INT, "999", 999, 2),
            Token(TokenType.IDENTIFIER, "abc122", null, 2),
            Token(TokenType.STRING, "\"this is a string\nok?\"", "this is a string\nok?", 3),
            Token(TokenType.EOF, "", null, 3),
        )

        val tokens = Lexer("+-/*123.321 \n 999 abc122 \"this is a string\nok?\"", this::reporter).scan()
        assertEquals(
            tokens,
            expectedResult,
            "Lexer not working"
        )
    }

    @Test
    fun testStrings() {
        val script = File("$resourceDir/string.lox")
        lox.execute(script.readText())

        val source = outContent.toString()
        val scanner = Scanner(source)

        assert(scanner.nextLine() == "this is a string")
        assert(scanner.nextLine() == "with lines")
    }
}