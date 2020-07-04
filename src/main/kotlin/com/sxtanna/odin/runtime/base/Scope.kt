package com.sxtanna.odin.runtime.base

import com.sxtanna.odin.runtime.data.Func
import com.sxtanna.odin.runtime.data.Prop
import com.sxtanna.odin.runtime.data.Type

data class Scope(val name: String)
{
	val props = mutableMapOf<String, Prop>()
	val funcs = mutableMapOf<String, Func>()
	val types = mutableMapOf<String, Type>()
	
	
	override fun toString(): String
	{
		return "Scope(name='$name', props=$props, funcs=$funcs, types=$types)"
	}
	
	override fun equals(other: Any?): Boolean
	{
		if (this === other) return true
		if (other !is Scope) return false
		
		if (name != other.name) return false
		if (props != other.props) return false
		if (funcs != other.funcs) return false
		if (types != other.types) return false
		
		return true
	}
	
	override fun hashCode(): Int
	{
		var result = name.hashCode()
		result = 31 * result + props.hashCode()
		result = 31 * result + funcs.hashCode()
		result = 31 * result + types.hashCode()
		return result
	}
	
	
}