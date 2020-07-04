package com.sxtanna.odin.runtime.data

import com.sxtanna.odin.runtime.base.Types
import com.sxtanna.odin.runtime.base.Value

data class Prop(val name: String, val mutable: Boolean)
{
	
	var type = Types.none()
	
	var data = null as? Value?
		set(value)
		{
			check(mutable || field == null || value == null)
			{
				"property reassignment: ${toString()} to `$value`"
			}
			
			field = value
		}
	
	fun copyProp(): Prop
	{
		val prop = copy()
		prop.type = type
		
		return prop
	}
	
	override fun toString(): String
	{
		return "Prop(${if (mutable) "var" else "val"} $name: ${type.name} = ${data?.data})"
	}
	
	override fun equals(other: Any?): Boolean
	{
		if (this === other) return true
		if (other !is Prop) return false
		
		if (name != other.name) return false
		if (mutable != other.mutable) return false
		if (type != other.type) return false
		if (data != other.data) return false
		
		return true
	}
	
	override fun hashCode(): Int
	{
		var result = name.hashCode()
		result = 31 * result + mutable.hashCode()
		result = 31 * result + type.hashCode()
		result = 31 * result + (data?.hashCode() ?: 0)
		return result
	}
	
	
}