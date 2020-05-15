package com.sxtanna.odin.runtime.base

import com.sxtanna.odin.runtime.data.Func
import com.sxtanna.odin.runtime.data.Type
import com.sxtanna.odin.runtime.data.Prop

data class Scope(val name: String)
{
	val props = mutableMapOf<String, Prop>()
	val funcs = mutableMapOf<String, Func>()
	val types = mutableMapOf<String, Type>()
	
	
	override fun toString(): String
	{
		return "Scope(name='$name', props=$props, funcs=$funcs, types=$types)"
	}
	
}