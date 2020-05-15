package com.sxtanna.odin.compile.util

data class PeekIterator<T : Any>(val data: List<T>)
{
	
	var index = 0
		private set
	
	val empty: Boolean
		get() = index > data.lastIndex
	
	
	fun peek(amount: Int = 0): T?
	{
		return data.getOrNull(index + amount)
	}
	
	fun next(amount: Int = 1): T
	{
		val value = data[index]
		move(amount)
		
		return value
	}
	
	fun move(amount: Int)
	{
		index += amount
	}
	
	
	inline fun each(function: (T) -> Unit)
	{
		while (!empty)
		{
			function.invoke(next())
		}
	}
	
}