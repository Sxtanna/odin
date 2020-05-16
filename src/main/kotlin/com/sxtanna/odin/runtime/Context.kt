package com.sxtanna.odin.runtime

import com.sxtanna.odin.runtime.base.Scope
import com.sxtanna.odin.runtime.base.Stack
import com.sxtanna.odin.runtime.data.Type
import com.sxtanna.odin.runtime.data.Prop

class Context
{
	val stack = Stack()
	val globe = Scope("global")
	
	var scope = globe
	var redos = 0
	
	init
	{
		Type.builtIns.forEach()
		{
			globe.types[it.name] = it
		}
	}
	
	
	fun findProp(name: String): Prop?
	{
		return scope.props[name] ?: globe.props[name]
	}
	
	fun findType(name: String): Type?
	{
		return scope.types[name] ?: globe.types[name]
	}
	
	
	fun defineProp(prop: Prop)
	{
		scope.props[prop.name] = prop
	}
	
	fun defineType(type: Type)
	{
		scope.types[type.name] = type
	}
	
	
	fun joinScope(join: Scope)
	{
	
	}
	
	fun quitScope()
	{
	
	}
	
	
	override fun toString(): String
	{
		val text =
			"""
				Stack: $stack
				Scope: $scope
			""".trimIndent()
		
		return text
	}
	
}