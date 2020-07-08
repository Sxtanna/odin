package com.sxtanna.odin

import org.junit.jupiter.api.Test

object Tests
{
	
	init
	{
		Odin
	}
	
	private fun readTextFromFile(name: String): String
	{
		return requireNotNull(ClassLoader.getSystemClassLoader().getResourceAsStream(name)?.bufferedReader()?.readText())
		{
			"Could not read code from file: $name"
		}
	}
	
	private fun odinEvalFromFile(name: String)
	{
		Odin.evaluate(Odin.assemble(readTextFromFile(name)) ?: return)
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
	internal fun `test tuple types`()
	{
		odinEvalFromFile("test13.o")
	}
	
	@Test
	internal fun `test brainfuck`()
	{
		odinEvalFromFile("brainfuck.o")
	}
	
	@Test
	internal fun `test create byte array`()
	{
		odinEvalFromFile("test5.o")
	}
	
	@Test
	internal fun `test use builtins`()
	{
		odinEvalFromFile("test16.o")
	}
}