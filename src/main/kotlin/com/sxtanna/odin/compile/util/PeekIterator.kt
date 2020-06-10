package com.sxtanna.odin.compile.util

data class PeekIterator<T : Any>(val data: List<T>)
{
	
	var index = 0
		private set
	
	val empty: Boolean
		get() = index > data.lastIndex
	
	
	val peek: T?
		get() = peek(amount = 0)
	
	val next: T
		get() = next(amount = 1)
	
	
	fun peek(amount: Int): T?
	{
		return data.getOrNull(index + amount)
	}
	
	fun next(amount: Int): T
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
			function.invoke(next)
		}
	}
	
}