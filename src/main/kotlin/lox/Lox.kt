package lox

import interpreter.LoxInterpreter
import interpreter.Resolver
import lexer.Lexer
import lexer.Token
import lexer.TokenType
import parser.LoxParser


class Lox {
    private val interpreter = LoxInterpreter(this::interpreterRuntimeErrorReporter)

    var hadError = false
    var hadRuntimeError = false

    private fun report(line: Int, msg: String, where: String = "") {
        // Prints to stdout on purpose.
        // TODO do this only if this a repl session. print to stderr if not
        println("Error on line $line:$where ==> $msg")
        hadError = true
    }

    private fun parserErrorReporter(token: Token, msg: String) {
        if (token.type == TokenType.EOF)
            report(token.line, msg, " at end")
        else
            report(token.line, msg, " at '${token.lexeme}'")
    }

    private fun interpreterRuntimeErrorReporter(err: LoxRuntimeError) {
        report(err.token.line, err.msg)
    }

    /**
     * Execute lox code
     */
    fun execute(str: String) {
        val tokens = Lexer(str, this::report).scan()
        if (hadError || tokens.size == 1) return

        val statements = LoxParser(tokens, this::parserErrorReporter).parse()
        if (hadError) return

        val resolver = Resolver(interpreter, this::parserErrorReporter)
        resolver.resolve(statements)
        if (hadError) return

        interpreter.interpret(statements)
    }

    // Read, Eval, Print, Loop
    fun repl() {
        while (true) {
            print("> ")
            val code = readReplLine() ?: return
            if (code.isEmpty())
                continue
            execute(code)
            hadError = false
        }
    }

    // TODO add support for multiline strings in repl
    private fun readReplLine(): String? {
        var result = ""
        var scopeStack = 0
        do {
            if (scopeStack > 0)
                print("...")

            // Read line, if EOF then return result
            val line = readLine() ?: return null
            if (line.isEmpty())
                continue

            scopeStack += line.count { c -> c == '{' }
            scopeStack -= line.count { c -> c == '}' }
            if (scopeStack < 0) {
                println("There are more '}' than '{.")
                return ""
            }
            result += line
        } while (scopeStack > 0)
        return result
    }
}