package com.sxtanna.odin.interop

import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory

class OdinScriptEngineFactory : ScriptEngineFactory
{
	
	override fun getLanguageName(): String
	{
		return "Odin"
	}
	
	override fun getLanguageVersion(): String
	{
		return "1.0"
	}
	
	
	override fun getEngineName(): String
	{
		return "Odin ScriptEngine"
	}
	
	override fun getEngineVersion(): String
	{
		return "1.0"
	}
	
	
	override fun getNames(): List<String>
	{
		return listOf("Odin", "odin")
	}
	
	override fun getExtensions(): List<String>
	{
		return listOf("o", "odin")
	}
	
	override fun getMimeTypes(): List<String>
	{
		return listOf("text/odin")
	}
	
	
	override fun getScriptEngine(): ScriptEngine
	{
		return OdinScriptEngine()
	}
	
	
	
	override fun getParameter(key: String?): Any?
	{
		return when (key)
		{
			ScriptEngine.ENGINE           -> engineName
			ScriptEngine.ENGINE_VERSION   -> engineVersion
			ScriptEngine.LANGUAGE         -> languageName
			ScriptEngine.LANGUAGE_VERSION -> languageVersion
			ScriptEngine.NAME             -> names[0]
			else                          -> null
		}
	}
	
	override fun getOutputStatement(toDisplay: String): String
	{
		return "push $toDisplay"
	}
	
	override fun getMethodCallSyntax(caller: String, method: String, vararg params: String): String
	{
		return "$caller.$method(${params.joinToString(", ")})"
	}
	
	override fun getProgram(vararg statements: String): String
	{
		return statements.joinToString("\n")
	}
	
}