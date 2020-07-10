package com.sxtanna.odin.runtime.base

import com.sxtanna.odin.runtime.data.Func
import com.sxtanna.odin.runtime.data.Prop
import com.sxtanna.odin.runtime.data.Type

sealed class Types
{
	abstract val name: String
	
	
	open fun matches(type: Type): Boolean
	{
		return matches(type.back)
	}
	
	abstract fun matches(type: Types): Boolean
	
	
	final override fun equals(other: Any?): Boolean
	{
		if (this === other) return true
		if (other !is Types) return false
		
		return matches(other)
	}
	
	final override fun hashCode(): Int
	{
		return name.hashCode()
	}
	
	
	companion object
	{
		val NONE = Basic("Nil")
		
		fun none(): Types = NONE
	}
}

// a single type
// Txt
data class Basic(override val name: String)
	: Types()
{
	override fun matches(type: Types): Boolean
	{
		return name == "All" || name == type.name
	}
	
	override fun toString(): String
	{
		return name
	}
}

data class Wraps(val clazz: Class<*>)
	: Types()
{
	override val name: String = clazz.name
	
	override fun matches(type: Types): Boolean
	{
		return type is Wraps && type.clazz.isAssignableFrom(clazz)
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
			
			if (!thisPart.matches(thatPart))
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
	// props that need to be initialized
	val props = mutableMapOf<String, Prop>()
	val funcs = mutableMapOf<String, Func>()
	
	
	fun addProp(prop: Prop)
	{
		val prev = props.put(prop.name, prop)
		require(prev == null)
		{
			"property ${prop.name} already defined as $prev"
		}
	}
	
	fun addFunc(func: Func)
	{
		val prev = funcs.put(func.name, func)
		require(prev == null)
		{
			"function ${func.name} already defined as $prev"
		}
	}
	
	
	override fun matches(type: Types): Boolean
	{
		return (type is Trait && type.name == name) || (type is Clazz && type.supes.contains(this.name))
	}
	
	override fun toString(): String
	{
		return "Trait[$name]($props){$funcs}"
	}
}

data class Clazz(override val name: String)
	: Types()
{
	var route = null as? Route?
	
	val supes = mutableListOf<String>()
	
	val props = mutableMapOf<String, Prop>()
	val funcs = mutableMapOf<String, Func>()
	
	
	fun addProp(prop: Prop)
	{
		val prev = props.put(prop.name, prop)
		require(prev == null)
		{
			"property ${prop.name} already defined as $prev"
		}
	}
	
	fun addFunc(func: Func)
	{
		val prev = funcs.put(func.name, func)
		require(prev == null)
		{
			"function ${func.name} already defined as $prev"
		}
	}
	
	
	override fun matches(type: Types): Boolean
	{
		return (type is Clazz && type.name == name)
	}
	
	override fun toString(): String
	{
		return "Class[$name]($props)::$supes"
	}
	
}