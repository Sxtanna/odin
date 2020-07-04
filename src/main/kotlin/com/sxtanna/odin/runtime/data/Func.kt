package com.sxtanna.odin.runtime.data

import com.sxtanna.odin.runtime.base.Route
import com.sxtanna.odin.runtime.base.Types

data class Func(val name: String)
{
	
	var body = null as? Route?
	
	val pull = mutableMapOf<String, Types>()
	val push = mutableMapOf<String, Types>() // I will probably never make this fully operational
	
	
	override fun toString(): String
	{
		return "fun '$name'($pull): $push { \n$body \n}"
	}
	
	override fun equals(other: Any?): Boolean
	{
		if (this === other) return true
		if (other !is Func) return false
		
		if (name != other.name) return false
		if (body != other.body) return false
		if (pull != other.pull) return false
		if (push != other.push) return false
		
		return true
	}
	
	override fun hashCode(): Int
	{
		var result = name.hashCode()
		result = 31 * result + (body?.hashCode() ?: 0)
		result = 31 * result + pull.hashCode()
		result = 31 * result + push.hashCode()
		return result
	}
	
	
}