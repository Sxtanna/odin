package com.sxtanna.odin.runtime

import com.sxtanna.odin.runtime.base.Scope
import com.sxtanna.odin.runtime.data.Func
import com.sxtanna.odin.runtime.data.Prop
import com.sxtanna.odin.runtime.data.Type

open class Context(val name: String)
{
	val scopes = ArrayDeque<Scope>()
	
	init
	{
		enterScope(Scope(name))
		
		Type.builtIns.forEach(::defineType)
	}
	
	
	open fun findProp(name: String, depth: Int = -1): Prop?
	{
		var remaining = depth
		
		for (scope in scopes)
		{
			val prop = scope.props[name]
			if (prop != null)
			{
				return prop
			}
			
			if (remaining != -1 && --remaining <= 0)
			{
				break
			}
		}
		
		return null
	}
	
	open fun findFunc(name: String): Func?
	{
		for (scope in scopes)
		{
			return scope.funcs[name] ?: continue
		}
		
		return null
	}
	
	fun findType(name: String): Type?
	{
		for (scope in scopes)
		{
			return scope.types[name] ?: continue
		}
		
		return null
	}
	
	
	fun defineProp(prop: Prop)
	{
		scopes[0].props[prop.name] = prop.copyProp()
	}
	
	fun defineFunc(func: Func)
	{
		scopes[0].funcs[func.name] = func
	}
	
	fun defineType(type: Type)
	{
		scopes[0].types[type.name] = type
	}
	
	
	fun enterScope(scope: Scope)
	{
		scopes.addFirst(scope)
	}
	
	fun leaveScope(): Scope
	{
		require(scopes.size > 1)
		{
			"context cannot leave it's primary scope"
		}
		
		return scopes.removeFirst()
	}
	
	
	override fun equals(other: Any?): Boolean
	{
		if (this === other) return true
		if (other !is Context) return false
		
		if (name != other.name) return false
		if (scopes != other.scopes) return false
		
		return true
	}
	
	override fun hashCode(): Int
	{
		var result = name.hashCode()
		result = 31 * result + scopes.hashCode()
		return result
	}
	
	override fun toString(): String
	{
		return "Context(name='$name', scopes=$scopes)"
	}
	
}