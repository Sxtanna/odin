package com.sxtanna.odin.runtime.base

import com.sxtanna.odin.runtime.Command
import com.sxtanna.odin.runtime.CommandDone
import com.sxtanna.odin.runtime.CommandMain
import com.sxtanna.odin.runtime.Context

data class Route(val command: Command)
{
	var prev: Route? = null
	var next: Route? = null
	
	
	fun eval(stack: Stack, context: Context)
	{
		//println("Evaluating $command")
		command.eval(stack, context)
		//println("$stack\n")
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
	
	companion object
	{
		fun of(commands: Collection<Command>): Route
		{
			val routes = listOf(CommandMain, *commands.toTypedArray(), CommandDone).map(::Route)
			
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