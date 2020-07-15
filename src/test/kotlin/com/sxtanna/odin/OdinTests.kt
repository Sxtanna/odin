package com.sxtanna.odin

import com.sxtanna.odin.compile.Lexer
import com.sxtanna.odin.compile.Typer
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

object OdinTests : BeforeAllCallback
{
	
	init
	{
		Odin
		Lexer
		Typer
	}
	
	override fun beforeAll(context: ExtensionContext) = Unit
	
}