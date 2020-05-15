package com.sxtanna.odin.compile

import com.sxtanna.odin.runtime.Command

object Trans : (List<Command>) -> String
{
	
	override fun invoke(cmds: List<Command>): String
	{
		println(cmds.joinToString("\n"))
		return ""
	}
	
}