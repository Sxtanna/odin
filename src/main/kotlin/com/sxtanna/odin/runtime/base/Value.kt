package com.sxtanna.odin.runtime.base

import com.sxtanna.odin.runtime.data.Type

data class Value(val type: Type, val data: Any)
{
	fun toPushString(): String
	{
		return when (data)
		{
			is Value         ->
			{
				data.toPushString()
				
			}
			is Collection<*> ->
			{
				val prefix = if (type.back is Tuple) "(" else "["
				val suffix = if (type.back is Tuple) ")" else "]"
				
				data.joinToString(prefix = prefix, postfix = suffix)
				{
					if (it !is Value) it.toString() else it.toPushString()
				}
			}
			is ByteArray     -> data.contentToString()
			is ShortArray    -> data.contentToString()
			is IntArray      -> data.contentToString()
			is LongArray     -> data.contentToString()
			is FloatArray    -> data.contentToString()
			is DoubleArray   -> data.contentToString()
			is CharArray     -> data.contentToString()
			is BooleanArray  -> data.contentToString()
			else             ->
			{
				data.toString()
			}
		}
	}
}