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
		assertLexesSuccessfully("24").all()
		{
			size().isEqualTo(1)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("24")
			}
		}
	}
	
	@Test
	internal fun `test lexer num dec`()
	{
		assertLexesSuccessfully("21.5").all()
		{
			size().isEqualTo(1)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("21.5")
			}
		}
	}
	
	@Test
	internal fun `test lexer num int negative`()
	{
		assertLexesSuccessfully("-26").all()
		{
			size().isEqualTo(1)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("-26")
			}
		}
	}
	
	@Test
	internal fun `test lexer num dec negative`()
	{
		assertLexesSuccessfully("-27.2").all()
		{
			size().isEqualTo(1)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("-27.2")
			}
		}
	}
	
	@Test
	internal fun `test lexer num int positive`()
	{
		assertLexesSuccessfully("+34").all()
		{
			size().isEqualTo(1)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("+34")
			}
		}
	}
	
	@Test
	internal fun `test lexer num dec positive`()
	{
		assertLexesSuccessfully("+36.55").all()
		{
			size().isEqualTo(1)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("+36.55")
			}
		}
	}
	
	@Test
	internal fun `test lexer num dec with leading point`()
	{
		assertLexesSuccessfully(".53").all()
		{
			size().isEqualTo(1)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("0.53")
			}
		}
	}
	
	@Test
	internal fun `test lexer num dec with leading point negative`()
	{
		assertLexesSuccessfully("-.23").all()
		{
			size().isEqualTo(1)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("-0.23")
			}
		}
	}
	
	@Test
	internal fun `test lexer num dec with leading point positive`()
	{
		assertLexesSuccessfully("+.38").all()
		{
			size().isEqualTo(1)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.NUM)
				prop("data") { it.data }.isEqualTo("+0.38")
			}
		}
	}
	
	@Test
	internal fun `test lexer let`()
	{
		assertLexesSuccessfully("'A'").all()
		{
			size().isEqualTo(1)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.LET)
				prop("data") { it.data }.isEqualTo("A")
			}
		}
	}
	
	@Test
	internal fun `test lexer txt`()
	{
		assertLexesSuccessfully(""""Hello World!"""").all()
		{
			size().isEqualTo(1)
			
			index(0).all()
			{
				prop("type") { it.type }.isEqualTo(TokenType.TXT)
				prop("data") { it.data }.isEqualTo("Hello World!")
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
}