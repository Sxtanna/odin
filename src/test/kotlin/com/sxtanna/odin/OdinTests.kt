package com.sxtanna.odin

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

object OdinTests : BeforeAllCallback
{
	
	init
	{
		Odin
	}
	
	override fun beforeAll(context: ExtensionContext) = Unit
	
}