package com.sxtanna.odin.runtime

import com.sxtanna.odin.runtime.base.Scope
import com.sxtanna.odin.runtime.base.Stack
import com.sxtanna.odin.runtime.data.Func
import com.sxtanna.odin.runtime.data.Type
import com.sxtanna.odin.runtime.data.Prop

class Context
{
	val stack = Stack()
	val globe = Scope("global")
	
	var redos = 0
	val scope = ArrayDeque<Scope>()
	
	init
	{
		Type.builtIns.forEach()
		{
			globe.types[it.name] = it
		}
		
		scope.add(globe)
	}
	
	
	fun findProp(name: String, depth: Int = -1): Prop?
	{
		var depth = depth
		
		for (scope in scope)
		{
			val prop = scope.props[name]
			if (prop != null)
			{
				return prop
			}
			
			if (depth != -1 && --depth <= 0)
			{
				break
			}
		}
		
		return null
	}
	
	fun findFunc(name: String): Func?
	{
		for (scope in scope)
		{
			return scope.funcs[name] ?: continue
		}
		
		return null
	}
	
	fun findType(name: String): Type?
	{
		for (scope in scope)
		{
			return scope.types[name] ?: continue
		}
		
		return null
	}
	
	
	fun defineProp(prop: Prop)
	{
		scope[0].props[prop.name] = prop
	}
	
	fun defineFunc(func: Func)
	{
		scope[0].funcs[func.name] = func
	}
	
	fun defineType(type: Type)
	{
		scope[0].types[type.name] = type
	}
	
	
	fun joinScope(join: Scope)
	{
		scope.addFirst(join)
	}
	
	fun quitScope()
	{
		scope.removeFirst()
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