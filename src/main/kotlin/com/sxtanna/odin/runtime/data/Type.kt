package com.sxtanna.odin.runtime.data

import com.sxtanna.odin.runtime.base.Basic
import com.sxtanna.odin.runtime.base.Types

data class Type(val name: String, val back: Types)
{
	companion object
	{
		val NIL = Type("Nil", Types.NONE)
		val ALL = Type("All", Basic("All"))
		
		val BYT = Type("Byt", Basic("Byt"))
		val INT = Type("Int", Basic("Int"))
		val LNG = Type("Lng", Basic("Lng"))
		val FLT = Type("Flt", Basic("Flt"))
		val DEC = Type("Dec", Basic("Dec"))
		val BIT = Type("Bit", Basic("Bit"))
		val LET = Type("Let", Basic("Let"))
		val TXT = Type("Txt", Basic("Txt"))
		
		val builtIns = arrayOf(ALL,
		                       NIL,
		                       BYT,
		                       INT,
		                       LNG,
		                       FLT,
		                       DEC,
		                       BIT,
		                       LET,
		                       TXT)
	}
	
	override fun equals(other: Any?): Boolean
	{
		if (this === other) return true
		if (other !is Type) return false
		
		if (back != other.back)
		{
			return false
		}
		
		return true
	}
	
	override fun hashCode(): Int
	{
		return back.hashCode()
	}
	
}