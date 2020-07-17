package com.sxtanna.odin

import com.sxtanna.odin.compile.Lexer
import com.sxtanna.odin.compile.Typer
import com.sxtanna.odin.compile.base.TokenData
import com.sxtanna.odin.runtime.Command
import com.sxtanna.odin.runtime.Context
import com.sxtanna.odin.runtime.base.Route
import com.sxtanna.odin.runtime.base.Stack

object Odin
{
	
	@JvmStatic
	@JvmOverloads
	fun assemble(source: String, superScript: OdinScript? = null): OdinScript?
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
		
		val script = if (superScript == null)
		{
			Route.of(typed)
		}
		else
		{
			Route.of(superScript.script.unwrap() + typed)
		}
		
		return OdinScript(source, script)
	}
	
	@JvmStatic
	fun evaluate(script: OdinScript): Stack
	{
		val stack = Stack()
		val context = Context("global")
		
		eval(context, script.script, stack)
		
		return stack
	}
	
	
	@JvmSynthetic
	internal fun eval(context: Context, route: Route, stack: Stack)
	{
		var route: Route? = route
		
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