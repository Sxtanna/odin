package com.sxtanna.odin.compile.util

object Interpolator
{
	
	sealed class Interpolation
	{
		abstract val text: String
		
		data class Expr(override val text: String) : Interpolation()
		
		data class Text(override val text: String) : Interpolation()
	}
	
	
	private val regex = Regex("\\$\\{?")
	
	private val breakNone = Regex("\\W")
	private val breakStop = Regex("}")
	
	
	fun hasInterpolation(text: String): Boolean
	{
		return text.contains(regex)
	}
	
	fun getInterpolation(text: String): List<Interpolation>
	{
		val interps = mutableListOf<Interpolation>()
		
		val matches = regex.findAll(text).iterator()
		if (!matches.hasNext())
		{
			interps += Interpolation.Text(text)
			
			return interps
		}
		
		var current = matches.next()
		val builder = StringBuilder()
		
		val peeking = PeekIterator(text.toList())
		
		
		while (!peeking.empty)
		{
			val char = peeking.next
			
			if (peeking.index <= current.range.first)
			{
				builder.append(char)
				continue
			}
			
			if (builder.isNotEmpty())
			{
				interps += Interpolation.Text(builder.toString())
				builder.setLength(0)
			}
			
			
			val breakOn = if (!current.value.endsWith('{')) breakNone else breakStop
			
			val expr = buildString()
			{
				if (peeking.peek == '{')
				{
					peeking.move(amount = 1)
				}
				
				while (!peeking.empty)
				{
					if (!breakOn.matches(peeking.peek.toString()))
					{
						append(peeking.next)
						continue
					}
					
					if (breakOn === breakStop)
					{
						peeking.move(amount = 1)
					}
					break
				}
			}
			
			interps += Interpolation.Expr(expr.replace("\\", ""))
			
			if (matches.hasNext())
			{
				current = matches.next()
				
				while (matches.hasNext() && current.range.first < peeking.index)
				{
					current = matches.next()
				}
			}
			
			if (current.range.first < peeking.index)
			{
				break
			}
		}
		
		if (builder.isNotEmpty())
		{
			interps += Interpolation.Text(builder.toString())
			builder.setLength(0)
		}
		
		if (!peeking.empty)
		{
			interps += Interpolation.Text(text.substring(peeking.index))
		}
		
		return interps
	}
	
}