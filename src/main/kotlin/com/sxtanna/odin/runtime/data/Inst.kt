package com.sxtanna.odin.runtime.data

import com.sxtanna.odin.runtime.Context

data class Inst(val type: Type, val insts: MutableMap<Type, Inst> = mutableMapOf()) : Context()
{
	override fun findProp(name: String, depth: Int): Prop?
	{
		var prop = super.findProp(name, -1)
		if (prop != null)
		{
			return prop
		}
		
		for (inst in insts.values)
		{
			prop = inst.findProp(name)
			if (prop != null)
			{
				break
			}
		}
		
		return prop
	}
	
	override fun findFunc(name: String): Func?
	{
		var func = super.findFunc(name)
		if (func != null)
		{
			return func
		}
		
		for (inst in insts.values)
		{
			func = inst.findFunc(name)
			if (func != null)
			{
				break
			}
		}
		
		return func
	}
	
	
	override fun equals(other: Any?): Boolean
	{
		if (this === other) return true
		if (other !is Inst) return false
		if (!super.equals(other)) return false
		
		if (type != other.type) return false
		if (insts != other.insts) return false
		
		return true
	}
	
	override fun hashCode(): Int
	{
		var result = super.hashCode()
		result = 31 * result + type.hashCode()
		result = 31 * result + insts.hashCode()
		return result
	}
	
}