package com.sxtanna.odin.compile.data

enum class Word
{
	WHEN,
	CASE,
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
	
	JAVA,
	CAST,
	
	CLASS,
	TRAIT;
	
	companion object
	{
		val values = values()
		val asName = values.associateBy { it.name.toLowerCase() }
		
		
		fun find(name: String): Word?
		{
			return asName[name.toLowerCase()]
		}
	}
}