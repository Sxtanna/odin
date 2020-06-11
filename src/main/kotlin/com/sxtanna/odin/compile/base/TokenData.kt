package com.sxtanna.odin.compile.base

data class TokenData(val line: Int, val char: Int, val type: TokenType, val data: String)
{
	constructor(type: TokenType, data: String)
			: this(0, 0, type, data)
	
	constructor(line: Int, char: Int, type: TokenType, data: Char)
			: this(line, char, type, data.toString())
	
	
	override fun toString(): String
	{
		return "TokenData(L$line, C$char, $type: '$data')"
	}
	
}