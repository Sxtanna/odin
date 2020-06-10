package com.sxtanna.odin.compile

import com.sxtanna.odin.compile.base.TokenData
import com.sxtanna.odin.compile.base.TokenType
import com.sxtanna.odin.compile.data.Word
import com.sxtanna.odin.compile.util.PeekIterator

object Lexer
{
	
	private val digit = '0'..'9'
	private val lower = 'a'..'z'
	private val upper = 'A'..'Z'
	
	private val symbol = setOf('+', '-', '/', '*', '<', '>', '!', '&')
	
	
	fun pass0(text: String): List<TokenData>
	{
		var char = 0
		var line = 0
		
		val toks = mutableListOf<TokenData>()
		val iter = PeekIterator(text.toList())
		
		
		fun add(type: TokenType, value: Char)
		{
			toks += TokenData(line, char, type, value)
			char += 1
		}
		
		fun add(type: TokenType, value: String)
		{
			toks += TokenData(line, char, type, value)
			char += value.length + (if (type == TokenType.LET || type == TokenType.TXT) 2 else 0)
		}
		
		iter.each()
		{ c ->
			when (c)
			{
				'\n'      ->
				{
					add(TokenType.NEWLINE, "")
					
					line++
					char = 0
				}
				' ',
				'\r'      ->
					add(TokenType.SPACE, c)
				','       ->
					add(TokenType.COMMA, c)
				'{'       ->
					add(TokenType.BRACE_L, c)
				'}'       ->
					add(TokenType.BRACE_R, c)
				'['       ->
					add(TokenType.BRACK_L, c)
				']'       ->
					add(TokenType.BRACK_R, c)
				'('       ->
					add(TokenType.PAREN_L, c)
				')'       ->
					add(TokenType.PAREN_R, c)
				':'       ->
				{
					when (iter.peek)
					{
						':'  ->
						{
							iter.move(amount = 1)
							add(TokenType.BOUND, "::")
						}
						else ->
						{
							add(TokenType.TYPED, c)
						}
					}
				}
				'='       ->
				{
					when (iter.peek)
					{
						'='  ->
						{
							iter.move(amount = 1)
							add(TokenType.OPER, "==")
						}
						'>'  ->
						{
							iter.move(amount = 1)
							add(TokenType.RETURN, "=>")
						}
						else ->
						{
							add(TokenType.ASSIGN, c)
						}
					}
				}
				'\'',
				'\"'      ->
				{
					val type = if (c == '\'') TokenType.LET else TokenType.TXT
					
					val data = if (iter.peek == c)
					{
						iter.move(amount = 1)
						""
					}
					else
					{
						buildString()
						{
							while (!iter.empty)
							{
								val next = iter.next
								if (next == c)
								{
									break
								}
								if (next == '\\')
								{
									if (iter.peek == 'n')
									{
										iter.move(amount = 1)
										appendln()
										continue
									}
								}
								
								append(next)
							}
						}
					}
					
					add(type, data)
				}
				'.',
				in digit  ->
				{
					if (c == '.' && iter.peek !in digit)
					{
						return@each add(TokenType.POINT, c)
					}
					
					var point = c == '.'
					var value = buildString()
					{
						if (point)
						{
							append('0')
						}
						
						append(c)
						
						while (!iter.empty)
						{
							val value = iter.peek
							if (value !in digit && value != '.')
							{
								break
							}
							
							if (value == '.')
							{
								check(!point)
								{
									"decimal out of place!"
								}
								
								point = true
							}
							
							append(iter.next)
						}
					}
					
					val prev = toks.lastOrNull()
					if (prev != null && prev.type == TokenType.OPER && (prev.data == "-" || prev.data == "+"))
					{
						char--
						toks.removeAt(toks.lastIndex)
						
						value = prev.data + value
					}
					
					add(TokenType.NUM, value)
				}
				in lower,
				in upper  ->
				{
					val value = buildString()
					{
						append(c)
						
						while (!iter.empty)
						{
							val value = iter.peek
							if (value !in lower && value !in upper && value !in digit && value != '_')
							{
								break
							}
							
							append(iter.next)
						}
					}
					
					val type = when (value)
					{
						"true",
						"false"        ->
						{
							TokenType.BIT
						}
						in Word.asName ->
						{
							TokenType.WORD
						}
						else           ->
						{
							if (value.first().isLowerCase()) TokenType.NAME else TokenType.TYPE
						}
					}
					
					add(type, value)
				}
				in symbol ->
				{
					if (c == '&' && iter.peek == '&')
					{
						iter.move(amount = 1)
						add(TokenType.OPER, "&&")
						
						return@each
					}
					if (c == '|' && iter.peek == '|')
					{
						iter.move(amount = 1)
						add(TokenType.OPER, "||")
						
						return@each
					}
					
					if (c == '!' && iter.peek == '=')
					{
						iter.move(amount = 1)
						add(TokenType.OPER, "!=")
						
						return@each
					}
					if (c == '>' && iter.peek == '=')
					{
						iter.move(amount = 1)
						add(TokenType.OPER, ">=")
						
						return@each
					}
					if (c == '<' && iter.peek == '=')
					{
						iter.move(amount = 1)
						add(TokenType.OPER, "<=")
						
						return@each
					}
					
					add(TokenType.OPER, c)
				}
			}
		}
		
		toks.removeIf { it.type == TokenType.SPACE }
		// remove all spaces and repeating new lines
		val iterMut = toks.iterator()
		
		while (iterMut.hasNext())
		{
			val here = iterMut.next()
			if (here.type != TokenType.NEWLINE || !iterMut.hasNext())
			{
				continue
			}
			
			while (iterMut.hasNext())
			{
				val next = iterMut.next()
				if (next.type != TokenType.NEWLINE)
				{
					break
				}
				
				iterMut.remove()
			}
		}
		
		return toks
	}
	
}