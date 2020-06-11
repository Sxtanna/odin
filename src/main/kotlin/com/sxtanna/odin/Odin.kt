package com.sxtanna.odin

import com.sxtanna.korm.Korm
import com.sxtanna.korm.data.option.Options
import com.sxtanna.korm.writer.KormWriter
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
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

object Odin
{
	
	private val korm = Korm(writer = KormWriter(1, Options.max()))
	
	init
	{
		korm.pushWith<Class<*>> { writer, data ->
			writer.writeBase(data?.simpleName ?: "unknown")
		}
	}
	
	fun proc(code: String, cont: Context? = null): Result<Context>
	{
		val cont = cont ?: Context()
		
		when (val read = read(code, printTime = true, printInfo = true))
		{
			is None ->
			{
				return Result.of { throw read.info }
			}
			is Some ->
			{
				// println(korm.push(read.data))
				
				when (val data = eval(cont, read.data, printTime = true))
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
	
	fun read(code: String, printTime: Boolean = false, printInfo: Boolean = false): Result<Route>
	{
		val (lexed, lexerTime) = measureTimedValue()
		{
			Result.of { Lexer.invoke(code) }
		}
		
		if (printTime)
		{
			println("Lexing took: $lexerTime")
		}
		if (printInfo && lexed is Some)
		{
			println(lexed.data.joinToString("\n"))
		}
		
		val (typed, typerTime) = measureTimedValue()
		{
			lexed.map(Typer::invoke)
		}
		
		if (printTime)
		{
			println("Typing took: $typerTime")
		}
		if (printInfo && typed is Some)
		{
			println()
			println(typed.data.joinToString("\n"))
			println()
		}
		
		return typed.map { Route.of(it) }
	}
	
	fun eval(cont: Context, route: Route, stack: Stack? = null, printTime: Boolean = false): Result<Unit>
	{
		return Result.of()
		{
			val evalTime = measureTime()
			{
				var route: Route? = route
				
				while (route != null)
				{
					route.eval(stack ?: cont.stack, cont)
					
					route = route.next
				}
			}
			
			
			if (printTime)
			{
				println("Evaluation took: $evalTime")
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