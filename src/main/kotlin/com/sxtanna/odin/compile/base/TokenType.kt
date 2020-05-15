package com.sxtanna.odin.compile.base

enum class TokenType
{
	
	NUM, // number
	LET, // char
	TXT, // string
	BIT, // boolean
	
	WORD, // keyword
	OPER, // operator
	NAME, // prop/func name
	TYPE, // trait/class name
	
	COMMA, // ','
	POINT, // '.'
	SPACE, // ' '
	TYPED, // ':'
	BOUND, // '::'
	
	ASSIGN, // '='
	RETURN, // '=>'
	
	NEWLINE, // '\n'
	BRACE_L, // '{'
	BRACE_R, // '}'
	BRACK_L, // '['
	BRACK_R, // ']'
	PAREN_L, // '('
	PAREN_R, // ')'
	
}