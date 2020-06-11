package com.sxtanna.odin

import com.sxtanna.odin.compile.util.Interpolator

fun main()
{
	val text = readCodeFromFile("test10.o")
	println("text is: '$text'")
	
	
	println("has: ${Interpolator.hasInterpolation(text)}")
	println("get: \n${Interpolator.getInterpolation(text).joinToString("\n")}")
}