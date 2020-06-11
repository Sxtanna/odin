package com.sxtanna.odin

import com.sxtanna.odin.results.None
import com.sxtanna.odin.results.Some


fun readCodeFromFile(name: String): String
{
	return requireNotNull(ClassLoader.getSystemClassLoader().getResourceAsStream(name)?.bufferedReader()?.readText()?.replace("\r", ""))
	{
		"Could not read code from file: $name"
	}
}


fun main()
{
	val code = readCodeFromFile("test10.o")
	
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


