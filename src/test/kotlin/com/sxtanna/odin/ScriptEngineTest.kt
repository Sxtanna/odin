package com.sxtanna.odin

import com.sxtanna.odin.interop.OdinScriptEngineFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import javax.script.ScriptEngineManager

object ScriptEngineTest
{
	
	@Test
	internal fun `test basic eval math`()
	{
		assertEquals(2, OdinScriptEngineFactory().scriptEngine.eval("1 + 1"))
	}
	
	@Test
	internal fun `test engine is resolvable`()
	{
		val manager = ScriptEngineManager()
		
		assertNotNull(manager.getEngineByExtension("odin"))
	}
}