package com.sxtanna.odin.compile.base

data class TokenData(val line: Int, val char: Int, val type: TokenType, val data: String)
{
	constructor(line: Int, char: Int, type: TokenType, data: Char): this(line, char, type, data.toString())
}