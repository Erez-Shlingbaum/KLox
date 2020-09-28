package lox

import lexer.Token

class LoxRuntimeError(val token: Token, val msg: String) : RuntimeException(msg)