package com.sxtanna.odin.runtime.data

import com.sxtanna.odin.runtime.base.Basic
import com.sxtanna.odin.runtime.base.Types

data class Type(val name: String, val back: Types)
{
	companion object
	{
		val NIL = Type("Nil", Types.NONE)
		val ALL = Type("All", Basic("All"))
		
		val INT = Type("Int", Basic("Int"))
		val DEC = Type("Dec", Basic("Dec"))
		val BIT = Type("Bit", Basic("Bit"))
		val LET = Type("Let", Basic("Let"))
		val TXT = Type("Txt", Basic("Txt"))
		val ARR = Type("Arr", Basic("Arr"))
		
		val builtIns = arrayOf(ALL,
		                       NIL,
		                       INT,
		                       DEC,
		                       BIT,
		                       LET,
		                       TXT,
		                       ARR)
	}
}