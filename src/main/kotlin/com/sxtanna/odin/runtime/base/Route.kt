package com.sxtanna.odin.runtime.base

import com.sxtanna.odin.runtime.Command
import com.sxtanna.odin.runtime.CommandNone
import com.sxtanna.odin.runtime.Context

data class Route(val command: Command)
{
	@Transient
	var prev: Route? = null
	var next: Route? = null
	
	
	fun eval(stack: Stack, context: Context)
	{
		command.eval(stack, context)
	}
	
	fun unwrap(): List<Command>
	{
		val cmds = mutableListOf<Command>()
		
		var here: Route? = this
		
		while (here != null)
		{
			cmds += here.command
			
			here = here.next
		}
		
		return cmds
	}
	
	
	override fun toString(): String
	{
		return buildString()
		{
			if (prev == null)
			{
				append("Route[")
			}
			
			appendln()
			append("=> ")
			append(command)
			
			if (next != null)
			{
				append(next.toString())
			}
			else
			{
				appendln()
				append("]")
			}
		}
	}
	
	
	override fun equals(other: Any?): Boolean
	{
		if (this === other) return true
		if (other !is Route) return false
		
		if (unwrap() != other.unwrap()) return false
		
		return true
	}
	
	override fun hashCode(): Int
	{
		return unwrap().hashCode()
	}
	
	
	companion object
	{
		private val none = Route(CommandNone)
		
		fun of(commands: Collection<Command>): Route
		{
			if (commands.isEmpty())
			{
				return none
			}
			
			val routes = listOf(*commands.toTypedArray()).map(::Route)
			
			routes.forEachIndexed()
			{ index, route ->
				val prev = routes.getOrNull(index - 1)
				val next = routes.getOrNull(index + 1)
				
				route.prev = prev
				route.next = next
			}
			
			return routes.first()
		}
	}
	
}