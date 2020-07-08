package com.sxtanna.odin.runtime.data

import com.sxtanna.odin.runtime.Context

data class Inst(val type: Type) : Context(type.name)
{
	
	override fun equals(other: Any?): Boolean
	{
		if (this === other) return true
		if (other !is Inst) return false
		if (!super.equals(other)) return false
		
		if (type != other.type) return false
		
		return true
	}
	
	override fun hashCode(): Int
	{
		var result = super.hashCode()
		result = 31 * result + type.hashCode()
		return result
	}
	
}