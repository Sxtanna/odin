package com.sxtanna.odin.compile.data

enum class Word
{
	WHEN,
	ELSE,
	
	VAL,
	VAR,
	FUN,
	
	PUSH,
	PULL,
	
	REDO,
	TYPE,
	
	LOOP,
	STOP,
	
	CLASS,
	TRAIT;
	
	companion object
	{
		val values = values().toSet()
		val asName = values.map { it.name.toLowerCase() }
		
		
		fun find(name: String): Word?
		{
			return values.find { it.name.equals(name, true) }
		}
	}
}