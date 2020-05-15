package com.sxtanna.odin

import com.sxtanna.odin.results.None
import com.sxtanna.odin.results.Some

fun main()
{
	val code = ClassLoader.getSystemClassLoader().getResourceAsStream("test1.o")?.bufferedReader()?.readText()?.replace("\r", "") ?: return println("Could not read code from file!")
	// println(code)
	
	when (val result = Odin.proc(code))
	{
		is None ->
		{
			throw result.info
		}
		is Some ->
		{
			val data = Odin.pull(result.data.stack.peek() ?: Unit)
			if (data == Unit || data == "Unit")
			{
				return
			}
			
			println("==")
			println(" Eval >> $data")
			println("==")
			//
			// println()
			// println(result.data.scope.props.values.joinToString("\n"))
		}
	}
}


