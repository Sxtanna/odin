package com.sxtanna.odin.compile

import com.sxtanna.odin.compile.base.TokenData
import com.sxtanna.odin.compile.base.TokenType
import com.sxtanna.odin.compile.data.Word
import com.sxtanna.odin.compile.util.PeekIterator

object Lexer : (String) -> List<TokenData>
{
	
	private val collapses = listOf(Collapse(TokenData(TokenType.STACK_PULL, "<|"),
	                                        hereMatch = { it.type == TokenType.OPER && it.data == "<" },
	                                        nextMatch = { it.type == TokenType.OPER && it.data == "|" }),
	                               Collapse(TokenData(TokenType.STACK_PUSH, "|>"),
	                                        hereMatch = { it.type == TokenType.OPER && it.data == "|" },
	                                        nextMatch = { it.type == TokenType.OPER && it.data == ">" }),
	                               Collapse(TokenData(TokenType.OPER, "++"),
	                                        hereMatch = { it.type == TokenType.OPER && it.data == "+" },
	                                        prevMatch = { it.type == TokenType.NAME },
	                                        nextMatch = { it.type == TokenType.OPER && it.data == "+" }),
	                               Collapse(TokenData(TokenType.OPER, "--"),
	                                        hereMatch = { it.type == TokenType.OPER && it.data == "-" },
	                                        prevMatch = { it.type == TokenType.NAME },
	                                        nextMatch = { it.type == TokenType.OPER && it.data == "-" }),
	                               Collapse(TokenData(TokenType.OPER, "+="),
	                                        hereMatch = { it.type == TokenType.OPER && it.data == "+" },
	                                        nextMatch = { it.type == TokenType.ASSIGN }),
	                               Collapse(TokenData(TokenType.OPER, "-="),
	                                        hereMatch = { it.type == TokenType.OPER && it.data == "-" },
	                                        nextMatch = { it.type == TokenType.ASSIGN }),
	                               Collapse(TokenData(TokenType.OPER, "*="),
	                                        hereMatch = { it.type == TokenType.OPER && it.data == "*" },
	                                        nextMatch = { it.type == TokenType.ASSIGN }),
	                               Collapse(TokenData(TokenType.OPER, "/="),
	                                        hereMatch = { it.type == TokenType.OPER && it.data == "/" },
	                                        nextMatch = { it.type == TokenType.ASSIGN }),
	                               Collapse(TokenData(TokenType.OPER, "=="),
	                                        hereMatch = { it.type == TokenType.ASSIGN },
	                                        nextMatch = { it.type == TokenType.ASSIGN }),
	                               Collapse(TokenData(TokenType.OPER, "!="),
	                                        hereMatch = { it.type == TokenType.OPER && it.data == "!" },
	                                        nextMatch = { it.type == TokenType.ASSIGN }),
	                               Collapse(TokenData(TokenType.OPER, ">="),
	                                        hereMatch = { it.type == TokenType.OPER && it.data == ">" },
	                                        nextMatch = { it.type == TokenType.ASSIGN }),
	                               Collapse(TokenData(TokenType.OPER, "<="),
	                                        hereMatch = { it.type == TokenType.OPER && it.data == "<" },
	                                        nextMatch = { it.type == TokenType.ASSIGN }),
	                               Collapse(TokenData(TokenType.OPER, "&&"),
	                                        hereMatch = { it.type == TokenType.OPER && it.data == "&" },
	                                        nextMatch = { it.type == TokenType.OPER && it.data == "&" }),
	                               Collapse(TokenData(TokenType.OPER, "||"),
	                                        hereMatch = { it.type == TokenType.OPER && it.data == "|" },
	                                        nextMatch = { it.type == TokenType.OPER && it.data == "|" }),
	                               Collapse(TokenData(TokenType.BOUND, "::"),
	                                        hereMatch = { it.type == TokenType.TYPED },
	                                        nextMatch = { it.type == TokenType.TYPED }),
	                               Collapse(TokenData(TokenType.RETURN, "=>"),
	                                        hereMatch = { it.type == TokenType.ASSIGN },
	                                        nextMatch = { it.type == TokenType.OPER && it.data == ">" }))
	
	
	private fun pass0(text: String): List<TokenData>
	{
		var char = 0
		var line = 1
		
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
				'\n'              ->
				{
					add(TokenType.NEWLINE, "")
					
					line++
					char = 0
				}
				' ',
				'\r'              ->
					add(TokenType.SPACE, c)
				','               ->
					add(TokenType.COMMA, c)
				'{'               ->
					add(TokenType.BRACE_L, c)
				'}'               ->
					add(TokenType.BRACE_R, c)
				'['               ->
					add(TokenType.BRACK_L, c)
				']'               ->
					add(TokenType.BRACK_R, c)
				'('               ->
					add(TokenType.PAREN_L, c)
				')'               ->
					add(TokenType.PAREN_R, c)
				':'               ->
					add(TokenType.TYPED, c)
				'='               ->
					add(TokenType.ASSIGN, c)
				'\'',
				'\"'              ->
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
								if (next == c && iter.peek(amount = -2) != '\\')
								{
									break
								}
								
								if (next == '\\')
								{
									if (iter.peek == 'n')
									{
										appendln()
										iter.move(amount = 1)
										continue
									}
									
									if (iter.peek == '\\')
									{
										iter.move(amount = 1)
									}
								}
								
								append(next)
							}
						}
					}
					
					add(type, data)
				}
				'.',
				in Char::isDigit  ->
				{
					if (c == '.' && iter.peek !in Char::isDigit)
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
							if (value !in Char::isDigit && value != '.')
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
						toks.removeAt(toks.size - 1)
						
						value = prev.data + value
					}
					
					add(TokenType.NUM, value)
				}
				in Char::isLetter ->
				{
					val value = buildString()
					{
						append(c)
						
						while (!iter.empty)
						{
							val value = iter.peek
							if (value !in Char::isLetterOrDigit && value != '_')
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
				'+',
				'-',
				'/',
				'*',
				'<',
				'>',
				'!',
				'&',
				'|'               ->
				{
					if (c == '/')
					{
						
						if (iter.peek == '/')
						{
							while (iter.peek != '\n')
							{
								iter.move(amount = 1)
							}
							return@each
						}
						
						if (iter.peek == '*')
						{
							while (iter.peek(amount = 0) != '*' || iter.peek(amount = 1) != '/')
							{
								if (iter.peek(amount = 0) == '\n')
								{
									line++
								}
								
								iter.move(amount = 1)
							}
							
							iter.move(amount = 2)
							return@each
						}
					}
					
					add(TokenType.OPER, c)
				}
			}
		}
		
		
		// remove repeating new lines
		val iterator = toks.iterator()
		
		while (iterator.hasNext())
		{
			if (iterator.next().type == TokenType.NEWLINE)
			{
				while (iterator.hasNext() && iterator.next().type == TokenType.NEWLINE)
				{
					iterator.remove()
				}
			}
		}
		
		return toks
	}
	
	private fun pass1(data: List<TokenData>): List<TokenData>
	{
		val toks = mutableListOf<TokenData>()
		val iter = PeekIterator(data)
		
		iter.each()
		{ here ->
			
			val prev = iter.peek(amount = -2)
			val next = iter.peek(amount = +0)
			
			val collapse = collapses.firstOrNull { it.matches(here, prev, next) }
			
			if (collapse == null)
			{
				toks += here
			}
			else
			{
				toks += collapse.intoToken
				
				iter.move(collapse.skipCount)
			}
		}
		
		toks.removeIf { it.type == TokenType.SPACE }
		
		return toks
	}
	
	
	override fun invoke(text: String): List<TokenData>
	{
		val pass0 = pass0(text)
		val pass1 = pass1(pass0)
		
		return pass1
	}
	
	
	private operator fun ((Char) -> Boolean).contains(char: Char?): Boolean
	{
		return char != null && this.invoke(char)
	}
	
	private data class Collapse(val intoToken: TokenData,
	                            val hereMatch: ((TokenData) -> Boolean),
	                            val prevMatch: ((TokenData) -> Boolean)? = null,
	                            val nextMatch: ((TokenData) -> Boolean)? = null,
	                            val skipCount: Int = 1)
	{
		fun matches(here: TokenData, prev: TokenData?, next: TokenData?): Boolean
		{
			if (!hereMatch.invoke(here))
			{
				return false
			}
			
			
			val prevMatches = if (prevMatch == null)
			{
				true
			}
			else
			{
				if (prev == null)
				{
					false
				}
				else
				{
					prevMatch.invoke(prev)
				}
			}
			
			val nextMatches = if (nextMatch == null)
			{
				true
			}
			else
			{
				if (next == null)
				{
					false
				}
				else
				{
					nextMatch.invoke(next)
				}
			}
			
			return prevMatches && nextMatches
		}
	}
	
}