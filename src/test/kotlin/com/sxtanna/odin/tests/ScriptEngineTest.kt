package com.sxtanna.odin.tests

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import org.junit.jupiter.api.Test
import javax.script.ScriptEngineManager

object ScriptEngineTest
{
	private val manager = ScriptEngineManager()
	
	private fun eval(text: String): Assert<Any>
	{
		return assertThat { manager.getEngineByExtension("odin") }.isSuccess().isNotNull().transform("eval result")
		{
			it.eval(text)
		}
	}
	
	
	@Test
	internal fun `test basic eval math`()
	{
		eval("1 + 1").isEqualTo(2)
	}
}