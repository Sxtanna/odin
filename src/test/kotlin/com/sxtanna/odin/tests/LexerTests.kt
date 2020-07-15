package com.sxtanna.odin.tests

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isSuccess
import assertk.assertions.prop
import assertk.assertions.size
import com.sxtanna.odin.OdinTests
import com.sxtanna.odin.compile.Lexer
import com.sxtanna.odin.compile.base.TokenData
import com.sxtanna.odin.compile.base.TokenType
import com.sxtanna.odin.compile.data.Word
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(OdinTests::class)
object LexerTests
{
	
	private fun assertLexesSuccessfully(text: String): Assert<List<TokenData>>
	{
		return assertThat { Lexer(text) }.isSuccess()
	}
	
	
	@Test
	internal fun `test lexer num int`()
	{
		assertLexesSuccessfully("24 -26 +34").all()
		{
			size().isEqualTo(3)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("24")
			}
			
			index(1).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("-26")
			}
			
			index(2).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("+34")
			}
		}
	}
	
	@Test
	internal fun `test lexer num dec`()
	{
		assertLexesSuccessfully("21.5 -27.2 +36.55").all()
		{
			size().isEqualTo(3)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("21.5")
			}
			
			index(1).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("-27.2")
			}
			
			index(2).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("+36.55")
			}
		}
	}
	
	@Test
	internal fun `test lexer num dec with leading point`()
	{
		assertLexesSuccessfully(".53 -.23 +.38").all()
		{
			size().isEqualTo(3)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("0.53")
			}
			
			index(1).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("-0.23")
			}
			
			index(2).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("+0.38")
			}
		}
	}
	
	@Test
	internal fun `test lexer let`()
	{
		assertLexesSuccessfully("'A' ' ' ").all()
		{
			size().isEqualTo(2)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.LET)
				prop("data") { it.data }.isEqualTo("A")
			}
			
			index(1).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.LET)
				prop("data") { it.data }.isEqualTo(" ")
			}
		}
	}
	
	@Test
	internal fun `test lexer txt`()
	{
		assertLexesSuccessfully(""" "Hello World!" "" """).all()
		{
			size().isEqualTo(2)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.TXT)
				prop("data") { it.data }.isEqualTo("Hello World!")
			}
			
			index(1).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.TXT)
				prop("data") { it.data }.isEqualTo("")
			}
		}
	}
	
	@Test
	internal fun `test lexer bit`()
	{
		assertLexesSuccessfully("true false").all()
		{
			size().isEqualTo(2)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.BIT)
				prop("data") { it.data }.isEqualTo("true")
			}
			
			index(1).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.BIT)
				prop("data") { it.data }.isEqualTo("false")
			}
		}
	}
	
	@Test
	internal fun `test lexer word`()
	{
		assertLexesSuccessfully(Word.values.joinToString(" ") { it.name.toLowerCase() }).all()
		{
			size().isEqualTo(Word.values.size)
			
			Word.values.forEachIndexed()
			{ index, word ->
				index(index).all()
				{
					prop("type") { it.type }.isEqualTo(TokenType.WORD)
					prop("data") { it.data }.isEqualTo(word.name.toLowerCase())
				}
			}
		}
	}
	
	@Test
	internal fun `test symbols collapse`()
	{
		assertLexesSuccessfully("<| |> value++ value-- += -= *= /= == != >= <= && || :: =>").all()
		{
			size().isEqualTo(18)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.STACK_PULL)
				prop("data") { it.data }.isEqualTo("<|")
			}
			
			index(1).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.STACK_PUSH)
				prop("data") { it.data }.isEqualTo("|>")
			}
			
			index(2).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NAME)
				prop("data") { it.data }.isEqualTo("value")
			}
			
			index(3).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.OPER)
				prop("data") { it.data }.isEqualTo("++")
			}
			
			index(4).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NAME)
				prop("data") { it.data }.isEqualTo("value")
			}
			
			index(5).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.OPER)
				prop("data") { it.data }.isEqualTo("--")
			}
			
			index(6).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.OPER)
				prop("data") { it.data }.isEqualTo("+=")
			}
			
			index(7).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.OPER)
				prop("data") { it.data }.isEqualTo("-=")
			}
			
			index(8).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.OPER)
				prop("data") { it.data }.isEqualTo("*=")
			}
			
			index(9).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.OPER)
				prop("data") { it.data }.isEqualTo("/=")
			}
			
			index(10).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.OPER)
				prop("data") { it.data }.isEqualTo("==")
			}
			
			index(11).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.OPER)
				prop("data") { it.data }.isEqualTo("!=")
			}
			
			index(12).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.OPER)
				prop("data") { it.data }.isEqualTo(">=")
			}
			
			index(13).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.OPER)
				prop("data") { it.data }.isEqualTo("<=")
			}
			
			index(14).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.OPER)
				prop("data") { it.data }.isEqualTo("&&")
			}
			
			index(15).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.OPER)
				prop("data") { it.data }.isEqualTo("||")
			}
			
			index(16).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.BOUND)
				prop("data") { it.data }.isEqualTo("::")
			}
			
			index(17).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.RETURN)
				prop("data") { it.data }.isEqualTo("=>")
			}
		}
	}
	
	@Test
	internal fun `test lexer strips repeating new lines`()
	{
		val code =
			"""
				
				
				value
				
				
			""".trimIndent()
		
		assertLexesSuccessfully(code).all()
		{
			size().isEqualTo(3)
		}
	}
}