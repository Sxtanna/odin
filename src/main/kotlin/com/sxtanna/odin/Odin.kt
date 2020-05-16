package com.sxtanna.odin

import com.sxtanna.odin.compile.Lexer
import com.sxtanna.odin.compile.Typer
import com.sxtanna.odin.results.None
import com.sxtanna.odin.results.Result
import com.sxtanna.odin.results.Some
import com.sxtanna.odin.results.map
import com.sxtanna.odin.runtime.CommandRedo
import com.sxtanna.odin.runtime.Context
import com.sxtanna.odin.runtime.base.Route
import com.sxtanna.odin.runtime.base.Stack
import com.sxtanna.odin.runtime.base.Value

object Odin
{
	
	fun proc(code: String, cont: Context? = null): Result<Context>
	{
		val cont = cont ?: Context()
		
		when (val read = read(code))
		{
			is None ->
			{
				return Result.of { throw read.info }
			}
			is Some ->
			{
				when (val data = eval(cont, read.data))
				{
					is None ->
					{
						return Result.of { throw data.info }
					}
				}
			}
		}
		
		val next = cont.stack.peek()
		if (next is CommandRedo)
		{
			if (next.count > 0)
			{
				if (cont.redos++ == next.count)
				{
					while (cont.stack.peek() is CommandRedo)
					{
						cont.stack.pull()
					}
					return Result.some(cont)
				}
			}
			
			println()
			return proc(code, cont)
		}
		
		return Result.some(cont)
	}
	
	fun read(code: String): Result<Route>
	{
		val lexed = Result.of()
		{
			Lexer.pass0(code)
		}
		
		if (lexed is Some)
		{
			// println(lexed.data.joinToString("\n"))
		}
		
		val typed = lexed.map()
		{
			Typer.pass0(it)
		}
		
		if (typed is Some)
		{
			// println()
			// println(typed.data.joinToString("\n"))
			// println()
		}
		
		return typed.map { Route.of(it) }
	}
	
	fun eval(cont: Context, route: Route, stack: Stack? = null): Result<Unit>
	{
		return Result.of()
		{
			var route: Route? = route
			
			while (route != null)
			{
				route.eval(stack ?: cont.stack, cont)
				
				route = route.next
			}
		}
	}
	
	
	fun pull(data: Any): Any
	{
		when (data)
		{
			is Some<*> ->
			{
				return pull(data.data)
			}
			is None<*> ->
			{
				throw data.info
			}
			is Value   ->
			{
				return data.data
			}
			is Unit    ->
			{
				return "Unit"
			}
			else       -> return data
		}
		
	}
	
}