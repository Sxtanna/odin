package com.sxtanna.odin.interop

import com.sxtanna.odin.Odin
import com.sxtanna.odin.runtime.base.Value
import java.io.Reader
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory
import javax.script.SimpleBindings
import javax.script.SimpleScriptContext

class OdinScriptEngine : ScriptEngine
{
	
	private var context: ScriptContext = SimpleScriptContext()
	
	
	override fun getContext(): ScriptContext
	{
		return context
	}
	
	override fun setContext(context: ScriptContext)
	{
		this.context = context
	}
	
	
	
	override fun createBindings(): Bindings
	{
		return SimpleBindings()
	}
	
	
	override fun getBindings(scope: Int): Bindings
	{
		return context.getBindings(scope)
	}
	
	override fun setBindings(bindings: Bindings, scope: Int)
	{
		context.setBindings(bindings, scope)
	}
	
	
	override fun get(key: String): Any?
	{
		return getBindings(ScriptContext.ENGINE_SCOPE)[key]
	}
	
	override fun put(key: String, value: Any)
	{
		getBindings(ScriptContext.ENGINE_SCOPE)[key] = value
	}
	
	
	override fun eval(reader: Reader): Any
	{
		return eval(reader.readText())
	}
	
	override fun eval(script: String): Any
	{
		return eval(script, context)
	}
	
	override fun eval(reader: Reader, context: ScriptContext): Any
	{
		return eval(reader.readText(), context)
	}
	
	override fun eval(script: String, context: ScriptContext): Any
	{
		return eval(script, context.getBindings(ScriptContext.ENGINE_SCOPE))
	}
	
	override fun eval(reader: Reader, bindings: Bindings): Any
	{
		return eval(reader.readText(), bindings)
	}
	
	override fun eval(script: String, bindings: Bindings): Any
	{
		var value = Odin.evaluate(Odin.assemble(script) ?: return Unit).peek() ?: return Unit
		while (value is Value)
		{
			value = value.data
		}
		
		return value
	}
	
	
	override fun getFactory(): ScriptEngineFactory
	{
		return OdinScriptEngineFactory()
	}
	
}