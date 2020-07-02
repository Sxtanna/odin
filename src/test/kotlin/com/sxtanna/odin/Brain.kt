package com.sxtanna.odin

class Brain
{
	
	private val values = ByteArray(5000)
	
	
	fun get(index: Int): Int
	{
		return values[index].toInt()
	}
	
	fun set(index: Int, value: Int)
	{
		values[index] = value.toByte()
	}
	
	fun getSize(): Int
	{
		return values.size
	}
	
	fun getChar(index: Int): Char
	{
		return values[index].toChar()
	}
	
}