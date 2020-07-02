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
	
	
	override fun toString(): String
	{
		return "Prop(${if (mutable) "var" else "val"} $name: ${type.name} = ${data?.data})"
	}
	
}