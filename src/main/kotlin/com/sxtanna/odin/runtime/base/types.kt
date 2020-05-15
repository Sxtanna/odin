package com.sxtanna.odin.runtime.base

import com.sxtanna.odin.runtime.data.Type

sealed class Types
{
	abstract val name: String
	
	
	open fun matches(type: Type): Boolean
	{
		return matches(type.back)
	}
	
	open fun matches(type: Types): Boolean
	{
		return false
	}
	
	
	companion object
	{
		fun none(): Types = None
	}
}

// a single type
// Txt
data class Basic(override val name: String)
	: Types()
{
	override fun matches(type: Types): Boolean
	{
		return this == type
	}
	
	
	override fun toString(): String
	{
		return name
	}
}

// a single type that represents a list
// (Txt, Int)
data class Tuple(val part: List<Types>)
	: Types()
{
	override val name = toString()
	
	
	override fun matches(type: Types): Boolean
	{
		if (type !is Tuple || type.part.size != this.part.size)
		{
			return false
		}
		
		this.part.indices.forEach()
		{ index ->
			val thisPart = this.part[index]
			val thatPart = type.part[index]
			
			if (thisPart.matches(thatPart))
			{
				return false
			}
		}
		
		return true
	}
	
	override fun toString(): String
	{
		return part.joinToString(prefix = "(", postfix = ")")
	}
}

data class Trait(override val name: String)
	: Types()
{
	val scope = Scope(name)
	val types = mutableListOf<Types>()
	
	var route: Route? = null
	
	override fun toString(): String
	{
		return "Trait[$name]::$types"
	}
}

data class Clazz(override val name: String)
	: Types()
{
	val scope = Scope(name)
	val types = mutableListOf<Types>()
}

private object None : Types()
{
	override val name = "none"
}