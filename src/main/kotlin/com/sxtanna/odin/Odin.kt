package com.sxtanna.odin

import com.sxtanna.odin.compile.Lexer
import com.sxtanna.odin.compile.Typer
import com.sxtanna.odin.compile.base.TokenData
import com.sxtanna.odin.results.Result
import com.sxtanna.odin.runtime.Command
import com.sxtanna.odin.runtime.Context
import com.sxtanna.odin.runtime.base.Route
import com.sxtanna.odin.runtime.base.Stack

object Odin
{
	
	@JvmStatic
	fun assemble(source: String): OdinScript?
	{
		val lexed: List<TokenData>
		try
		{
			lexed = Lexer(source)
		}
		catch (ex: Throwable)
		{
			ex.printStackTrace()
			return null
		}
		
		val typed: List<Command>
		
		try
		{
			typed = Typer(lexed)
		}
		catch (ex: Throwable)
		{
			ex.printStackTrace()
			return null
		}
		
		return OdinScript(source, Route.of(typed))
	}
	
	@JvmStatic
	fun evaluate(script: OdinScript): Stack
	{
		val stack = Stack()
		val context = Context()
		
		eval(context, script.script, stack)
		
		return stack
	}
	
	@JvmSynthetic
	internal fun eval(context: Context, route: Route, stack: Stack): Result<Unit>
	{
		return Result.of()
		{
			var route: Route? = route
			
			while (route != null)
			{
				while (route != null)
				{
					try
					{
						route.eval(stack, context)
					}
					catch (ex: Throwable)
					{
						ex.printStackTrace()
						break
					}
					
					route = route.next
				}
			}
		}
	}
	
}