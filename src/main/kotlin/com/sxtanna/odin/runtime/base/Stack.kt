package com.sxtanna.odin.runtime.base

import java.util.ArrayDeque
import java.util.Deque

class Stack private constructor(private val stack: Deque<Any>)
{
	constructor() : this(ArrayDeque())
	
	
	fun peek(): Any?
	{
		return stack.peek()
	}
	
	
	fun pull(): Any
	{
		return stack.poll()
	}
	
	fun push(data: Any)
	{
		stack.push(data)
	}
	
	fun flip(): Stack
	{
		val stack = ArrayDeque<Any>()
		
		this.stack.forEach(stack::push)
		
		return Stack(stack)
	}
	
	override fun toString(): String
	{
		return stack.toString()
	}
	
}