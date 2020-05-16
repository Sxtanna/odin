package com.sxtanna.odin.runtime.data

import com.sxtanna.odin.runtime.base.Route
import com.sxtanna.odin.runtime.base.Types

data class Func(val name: String)
{
	
	var body = null as? Route?
	
	val pull = mutableMapOf<String, Types>()
	val push = mutableMapOf<String, Types>() // I will probably never make this fully operational
	
}