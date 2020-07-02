package com.sxtanna.odin

import com.sxtanna.odin.results.None
import com.sxtanna.odin.results.Some
import org.junit.jupiter.api.Test

object Tests
{
	
	private fun readTextFromFile(name: String): String
	{
		return requireNotNull(ClassLoader.getSystemClassLoader().getResourceAsStream(name)?.bufferedReader()?.readText()?.replace("\r", ""))
		{
			"Could not read code from file: $name"
		}
	}
	
	private fun odinEvalFromFile(name: String)
	{
		val code = readTextFromFile(name)
		
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
			}
		}
	}
	
	
	@Test
	internal fun `test generate random decs`()
	{
		odinEvalFromFile("test14.o")
	}
	
	@Test
	internal fun `test tuple types are the same`()
	{
		odinEvalFromFile("test15.o")
	}
	
	@Test
	internal fun `test interleaved push`()
	{
		odinEvalFromFile("test16.o")
	}
	
	@Test
	internal fun `test tuple types`()
	{
		odinEvalFromFile("test13.o")
	}
	
	@Test
	internal fun `test brainfuck`()
	{
		odinEvalFromFile("brainfuck.o")
	}
	
}