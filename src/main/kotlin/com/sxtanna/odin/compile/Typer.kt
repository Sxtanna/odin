@file:Suppress("JoinDeclarationAndAssignment", "unused", "DuplicatedCode")

package com.sxtanna.odin.compile

import com.sxtanna.odin.compile.base.TokenData
import com.sxtanna.odin.compile.base.TokenType.*
import com.sxtanna.odin.compile.data.Oper
import com.sxtanna.odin.compile.data.OperatorAdd
import com.sxtanna.odin.compile.data.OperatorBoth
import com.sxtanna.odin.compile.data.OperatorDiff
import com.sxtanna.odin.compile.data.OperatorDiv
import com.sxtanna.odin.compile.data.OperatorElse
import com.sxtanna.odin.compile.data.OperatorLess
import com.sxtanna.odin.compile.data.OperatorLessOrSame
import com.sxtanna.odin.compile.data.OperatorLvl
import com.sxtanna.odin.compile.data.OperatorMore
import com.sxtanna.odin.compile.data.OperatorMoreOrSame
import com.sxtanna.odin.compile.data.OperatorMul
import com.sxtanna.odin.compile.data.OperatorNot
import com.sxtanna.odin.compile.data.OperatorSame
import com.sxtanna.odin.compile.data.OperatorSub
import com.sxtanna.odin.compile.data.Word
import com.sxtanna.odin.compile.util.PeekIterator
import com.sxtanna.odin.runtime.Command
import com.sxtanna.odin.runtime.CommandClazzDefine
import com.sxtanna.odin.runtime.CommandWhen
import com.sxtanna.odin.runtime.CommandConsolePull
import com.sxtanna.odin.runtime.CommandConsolePush
import com.sxtanna.odin.runtime.CommandFunctionAccess
import com.sxtanna.odin.runtime.CommandFunctionDefine
import com.sxtanna.odin.runtime.CommandGet
import com.sxtanna.odin.runtime.CommandLiteral
import com.sxtanna.odin.runtime.CommandLoop
import com.sxtanna.odin.runtime.CommandOperate
import com.sxtanna.odin.runtime.CommandPropertyAccess
import com.sxtanna.odin.runtime.CommandPropertyAssign
import com.sxtanna.odin.runtime.CommandPropertyDefine
import com.sxtanna.odin.runtime.CommandPropertyResets
import com.sxtanna.odin.runtime.CommandRedo
import com.sxtanna.odin.runtime.CommandRoute
import com.sxtanna.odin.runtime.CommandStop
import com.sxtanna.odin.runtime.CommandTraitDefine
import com.sxtanna.odin.runtime.CommandTuple
import com.sxtanna.odin.runtime.CommandTypeQuery
import com.sxtanna.odin.runtime.base.Basic
import com.sxtanna.odin.runtime.base.Clazz
import com.sxtanna.odin.runtime.base.Route
import com.sxtanna.odin.runtime.base.Trait
import com.sxtanna.odin.runtime.base.Tuple
import com.sxtanna.odin.runtime.base.Types
import com.sxtanna.odin.runtime.data.Func
import com.sxtanna.odin.runtime.data.Prop
import java.util.ArrayDeque

object Typer
{
	
	fun pass0(toks: List<TokenData>): List<Command>
	{
		val cmds = mutableListOf<Command>()
		val iter = PeekIterator(toks)
		
		while (!iter.empty)
		{
			iter.parseMain(cmds)
		}
		
		return cmds
	}
	
	fun pass1(prev: List<Command>): List<Command>
	{
		val cmds = mutableListOf<Command>()
		val iter = PeekIterator(prev)
		
		iter.each()
		{
			cmds += it
			println(it)
		}
		
		return cmds
	}
	
	
	private fun PeekIterator<TokenData>.parseMain(cmds: MutableList<Command>)
	{
		val next = next()
		
		when (next.type)
		{
			NEWLINE ->
			{
				return
			}
			WORD    ->
			{
				parseWord(cmds, next)
			}
			NAME    ->
			{
				parseName(cmds, next)
			}
			NUM     ->
			{
				move(-1)
				parseShuntedExpression(cmds)
			}
			else    ->
			{
				throw IllegalStateException("token out of place: $next")
			}
		}
	}
	
	
	private fun PeekIterator<TokenData>.parseName(cmds: MutableList<Command>, data: TokenData)
	{
		var next: TokenData
		
		next = next()
		
		when (next.type)
		{
			OPER   ->
			{
				move(-2)
				val expr = mutableListOf<Command>()
				parseShuntedExpression(expr)
				
				cmds += CommandRoute(Route.of(expr))
			}
			PAREN_L ->
			{
				move(-1)
				val expr = mutableListOf<Command>()
				parseShuntedExpression(expr)
				
				cmds += CommandRoute(Route.of(expr))
			}
			ASSIGN ->
			{
				val expr = mutableListOf<Command>()
				parseShuntedExpression(expr)
				
				cmds += CommandRoute(Route.of(expr))
				cmds += CommandPropertyAssign(data.data)
			}
		}
	}
	
	private fun PeekIterator<TokenData>.parseWord(cmds: MutableList<Command>, data: TokenData)
	{
		val word = requireNotNull(Word.find(data.data))
		{
			"word ${data.data} not found!"
		}
		
		when (word)
		{
			Word.VAL,
			Word.VAR   ->
				parseProp(cmds, word == Word.VAR, true)
			Word.FUN   ->
				parseFunc(cmds)
			Word.TRAIT ->
				parseTrait(cmds)
			Word.CLASS ->
				parseClazz(cmds)
			Word.PUSH  ->
				parsePush(cmds)
			Word.PULL  ->
				parsePull(cmds)
			Word.WHEN  ->
				parseWhen(cmds)
			Word.TYPE ->
				parseTypeQuery(cmds)
			Word.LOOP ->
				parseLoop(cmds)
			Word.STOP ->
				cmds += CommandStop
			Word.REDO  ->
			{
				val peek = peek()
				require(peek == null || peek.type == NUM)
				{
					"peek must be followed by nothing or a number"
				}
				
				if (peek != null)
				{
					move(1)
				}
				
				val numb = peek?.data?.toIntOrNull() ?: -1
				
				cmds += CommandRedo(numb)
			}
			else  ->
			{
				throw UnsupportedOperationException("handling for word $word not found")
			}
		}
	}
	
	private fun PeekIterator<TokenData>.parseProp(cmds: MutableList<Command>, mutable: Boolean, assignment: Boolean)
	{
		var next: TokenData
		
		next = next()
		require(next.type == NAME)
		{
			"property missing name"
		}
		
		
		val prop = Prop(next.data, mutable)
		cmds += CommandPropertyDefine(prop)
		
		if (peek()?.type == TYPED)
		{
			next()
			prop.type = parseType()
		}
		
		
		if (!assignment)
		{
			return
		}
		
		
		next = next()
		require(next.type == ASSIGN)
		{
			"must be assignment: $next"
		}
		
		parseShuntedExpression(cmds)
		
		cmds += CommandPropertyAssign(prop.name)
	}
	
	private fun PeekIterator<TokenData>.parseFunc(cmds: MutableList<Command>)
	{
		var next: TokenData
		
		next = next()
		require(next.type == NAME)
		{
			"function missing name"
		}
		
		val func = Func(next.data)
		
		if (peek()?.type == PAREN_L)
		{
			next = next()
			require(next.type == PAREN_L)
			{
				"function parameters must be surrounded in parentheses"
			}
			
			val names = mutableSetOf<String>()
			
			while (!empty)
			{
				next = next()
				
				if (next.type == PAREN_R)
				{
					require(func.pull.isNotEmpty())
					{
						"function parentheses must contain parameters"
					}
					
					move(-1)
					
					break
				}
				
				if (next.type == COMMA)
				{
					require(names.isNotEmpty() || func.pull.isNotEmpty())
					{
						"first value must be a parameter"
					}
					
					continue
				}
				
				if (next.type == NAME)
				{
					require(names.add(next.data))
					{
						"parameter ${next.data} already defined for function ${func.name}"
					}
					
					continue
				}
				
				if (next.type == TYPED)
				{
					val type = parseType()
					
					for (name in names)
					{
						func.pull[name] = type
					}
					
					names.clear()
					continue
				}
				
				throw UnsupportedOperationException("Token out of place: $next")
			}
			
			next = next()
			require(next.type == PAREN_R)
			{
				"function parameters must be surrounded in parentheses"
			}
			
			// println("func ${func.name} pull: ${func.pull}")
		}
		
		if (peek()?.type == TYPED)
		{
			next = next()
			require(next.type == TYPED)
			{
				"function return type must be specified with typed symbol"
			}
			
			func.push["ret0"] = parseType()
			
			// println("func ${func.name} push: ${func.push}")
		}
		
		ignoreNewLines()
		
		next = next()
		require(next.type == BRACE_L)
		{
			"function body must be surrounded with braces"
		}
		
		ignoreNewLines()
		
		val body = mutableListOf<Command>()
		
		func.pull.forEach()
		{ (name, type) ->
			
			val prop = Prop(name, false)
			prop.type = type
			
			body += CommandPropertyDefine(prop, depth = 1)
			body += CommandPropertyAssign(name)
		}
		
		
		while (!empty)
		{
			next = peek() ?: break
			
			if (next.type == BRACE_R)
			{
				break
			}
			if (next.type == NEWLINE)
			{
				move(1)
				continue
			}
			if (next.type == RETURN)
			{
				move(1)
				
				parseShuntedExpression(body)
				break
			}
			
			parseMain(body)
		}
		
		ignoreNewLines()
		
		next = next()
		require(next.type == BRACE_R)
		{
			"function body must be surrounded with braces"
		}
		
		func.pull.forEach()
		{ (name, _) ->
			body += CommandPropertyResets(name)
		}
		
		func.body = Route.of(body)
		
		cmds += CommandFunctionDefine(func)
	}
	
	private fun PeekIterator<TokenData>.parsePush(cmds: MutableList<Command>)
	{
		parseShuntedExpression(cmds)
		cmds += CommandConsolePush(true)
	}
	
	private fun PeekIterator<TokenData>.parsePull(cmds: MutableList<Command>)
	{
		var type = Types.none()
		var prompt = null as? String?
		
		if (!empty && peek()?.type == BOUND || peek()?.type == PAREN_L)
		{
			var next: TokenData
			
			next = next()
			if (next.type == BOUND)
			{
				type = requireNotNull(parseBound().singleOrNull())
				{
					"pull bounds can be only 1 type"
				}
				
				if (!empty && peek()?.type == PAREN_L)
				{
					next = next()
				}
			}
			
			if (next.type == PAREN_L)
			{
				next = next()
				require(next.type == TXT)
				{
					"pull parens must contain a txt prompt"
				}
				
				prompt = next.data
				
				next = next()
				require(next.type == PAREN_R)
				{
					"pull parens missing close"
				}
			}
		}
		
		cmds += CommandConsolePull(type, prompt)
	}
	
	private fun PeekIterator<TokenData>.parseWhen(cmds: MutableList<Command>)
	{
		var next: TokenData
		
		next = next()
		require(next.type == PAREN_L)
		{
			"when condition must be in parentheses"
		}
		
		val expr = mutableListOf<Command>()
		parseShuntedExpression(expr)
		
		next = next()
		require(next.type == PAREN_R)
		{
			"when condition must be in parentheses"
		}
		
		ignoreNewLines()
		
		next = next()
		require(next.type == BRACE_L)
		{
			"when condition pass must be in braces"
		}
		
		ignoreNewLines()
		
		val pass = mutableListOf<Command>()
		
		while (!empty)
		{
			next = peek() ?: break
			
			if (next.type == BRACE_R)
			{
				break
			}
			if (next.type == NEWLINE)
			{
				move(1)
				continue
			}
			
			parseMain(pass)
		}
		
		ignoreNewLines()
		
		next = next()
		require(next.type == BRACE_R)
		{
			"when condition pass must be in braces"
		}
		
		ignoreNewLines()
		
		val peek = peek()
		if (peek == null || peek.type != WORD || Word.find(peek.data) != Word.ELSE)
		{
			cmds += CommandWhen(Route.of(expr), Route.of(pass), null)
			return
		}
		
		move(1) // skip else word
		
		ignoreNewLines()
		
		next = next()
		require(next.type == BRACE_L)
		{
			"when condition fail must be in braces"
		}
		
		ignoreNewLines()
		
		val fail = mutableListOf<Command>()
		
		while (!empty)
		{
			next = peek() ?: break
			
			if (next.type == BRACE_R)
			{
				break
			}
			if (next.type == NEWLINE)
			{
				move(1)
				continue
			}
			
			parseMain(fail)
		}
		
		ignoreNewLines()
		
		next = next()
		require(next.type == BRACE_R)
		{
			"when condition pass must be in braces"
		}
		
		cmds += CommandWhen(Route.of(expr), Route.of(pass), Route.of(fail))
	}
	
	private fun PeekIterator<TokenData>.parseLoop(cmds: MutableList<Command>)
	{
		var next: TokenData
		
		next = next()
		require(next.type == PAREN_L)
		{
			"loop condition must be in parentheses"
		}
		
		val expr = mutableListOf<Command>()
		parseShuntedExpression(expr)
		
		next = next()
		require(next.type == PAREN_R)
		{
			"loop condition must be in parentheses"
		}
		
		ignoreNewLines()
		
		next = next()
		require(next.type == BRACE_L)
		{
			"loop body pass must be in braces"
		}
		
		ignoreNewLines()
		
		val body = mutableListOf<Command>()
		
		while (!empty)
		{
			next = peek() ?: break
			
			if (next.type == BRACE_R)
			{
				break
			}
			if (next.type == NEWLINE)
			{
				move(1)
				continue
			}
			
			parseMain(body)
		}
		
		ignoreNewLines()
		
		next = next()
		require(next.type == BRACE_R)
		{
			"loop body pass must be in braces"
		}
		
		cmds += CommandLoop(Route.of(expr), Route.of(body))
	}
	
	private fun PeekIterator<TokenData>.parseBound(): List<Types>
	{
		var next: TokenData
		
		next = next()
		require(next.type == BRACK_L)
		{
			"bounds missing brack l"
		}
		
		val types = mutableListOf<Types>()
		
		while (!empty)
		{
			if (next.type == NEWLINE)
			{
				continue
			}
			
			if (next.type == COMMA)
			{
				require(types.isNotEmpty())
				{
					"first value must be a type"
				}
				
				continue
			}
			
			if (next.type == BRACK_R)
			{
				require(types.isNotEmpty())
				{
					"bounds must not be empty"
				}
				
				break
			}
			
			types += parseType()
			
			next = next()
		}
		
		return types
	}
	
	private fun PeekIterator<TokenData>.parseTrait(cmds: MutableList<Command>)
	{
		var next: TokenData
		
		next = next()
		require(next.type == TYPE)
		{
			"trait missing type name"
		}
		
		val name = next.data
		
		val trait = Trait(name)
		
		cmds += CommandTraitDefine(trait)
		
		next = next()
		require(next.type == PAREN_L || next.type == BRACE_L || next.type == BOUND)
		{
			"trait missing props or body"
		}
		
		if (next.type == PAREN_L) // parse props
		{
			val props = mutableListOf<Command>()
			
			while (!empty)
			{
				next = next()
				
				if (next.type == PAREN_R)
				{
					require(props.isNotEmpty())
					{
						"trait parentheses must contain properties"
					}
					
					break
				}
				
				if (next.type == COMMA)
				{
					require(props.isNotEmpty())
					{
						"first value must be a property"
					}
					
					continue
				}
				
				if (next.type == WORD)
				{
					val word = requireNotNull(Word.find(next.data))
					{
						"word ${next.data} not found!"
					}
					
					when (word)
					{
						Word.VAL,
						Word.VAR  ->
							parseProp(props, word == Word.VAR, false)
						else ->
						{
							throw UnsupportedOperationException("word out of place: $next")
						}
					}
					
					continue
				}
				
				throw UnsupportedOperationException("token out of place: $next")
			}
			
			val route = Route.of(props)
			trait.route = route
			
			if (!empty)
			{
				next = next()
			}
		}
		
		if (next.type == BOUND)
		{
			trait.types += parseBound()
		}
		
		if (next.type == BRACE_L) // parse body
		{
			// trouble time
		}
	}
	
	private fun PeekIterator<TokenData>.parseClazz(cmds: MutableList<Command>)
	{
		var next: TokenData
		
		next = next()
		require(next.type == TYPE)
		{
			"class missing type name"
		}
		
		val name = next.data
		
		val clazz = Clazz(name)
		
		cmds += CommandClazzDefine(clazz)
		
		next = next()
		require(next.type == BRACE_L || next.type == BOUND)
		{
			"class missing bounds or body"
		}
		
		if (next.type == BOUND)
		{
			clazz.types += parseBound()
		}
		
		if (next.type == BRACE_L) // parse body
		{
			// trouble time
		}
	}
	
	
	private fun PeekIterator<TokenData>.parseShuntedExpression(cmds: MutableList<Command>)
	{
		cmds += shuntingYard(parseExpr())
	}
	
	private fun PeekIterator<TokenData>.parseExpr(breakOnComma: Boolean = false): List<Command>
	{
		val expr = mutableListOf<Command>()
		
		var open = 0
		val head = index
		
		var next: TokenData
		
		loop@ while (!empty)
		{
			next = next()
			
			if (next.type == NEWLINE)
			{
				break
			}
			if (next.type == COMMA && breakOnComma)
			{
				move(-1)
				break
			}
			if (next.type == BRACE_R || next.type == BRACK_R)
			{
				move(-1)
				break
			}
			
			when (next.type)
			{
				NUM,
				LET,
				TXT,
				BIT     ->
					parseLit(expr, next)
				NAME    ->
				{
					if (peek()?.type != ASSIGN)
					{
						parseRef(expr, next)
					}
					else
					{
						parseName(expr, next)
						expr += CommandPropertyAccess(next.data)
					}
				}
				TYPE    ->
					parseNew(expr, next)
				OPER    ->
				{
					val oper = when (next.data)
					{
						"+"  ->
							OperatorAdd
						"-"  ->
							OperatorSub
						"*"  ->
							OperatorMul
						"/"  ->
							OperatorDiv
						"!"  ->
							OperatorNot
						"==" ->
							OperatorSame
						"!=" ->
							OperatorDiff
						"||" ->
							OperatorElse
						"&&" ->
							OperatorBoth
						">"  ->
							OperatorMore
						"<"  ->
							OperatorLess
						">=" ->
							OperatorMoreOrSame
						"<=" ->
							OperatorLessOrSame
						else ->
						{
							throw UnsupportedOperationException("unknown operator: $next")
						}
					}
					
					expr += CommandOperate(oper)
				}
				COMMA   ->
				{
					require(open > 0)
					{
						"comma out of position: $next"
					}
					
					var erased = 0
					while (peek()?.type != PAREN_L || index > head)
					{
						erased++
						move(-1)
					}
					
					while (erased-- > 0)
					{
						expr.removeLastOrNull() ?: break
					}
					
					val cmds = parseTup()
					
					expr += CommandRoute(Route.of(cmds.flatten()))
					expr += CommandTuple(cmds.size)
				}
				PAREN_L ->
				{
					open++
					expr += CommandOperate(Oper.SOS)
				}
				PAREN_R ->
				{
					if (open == 0)
					{
						move(-1)
						break@loop
					}
					
					open--
					expr += CommandOperate(Oper.EOS)
				}
				BRACK_L ->
				{
					move(-1)
					parseInd(expr)
				}
				WORD    ->
				{
					when (Word.find(next.data))
					{
						Word.PULL ->
							parsePull(expr)
						Word.PUSH ->
							parsePush(expr)
						Word.TYPE ->
							parseTypeQuery(expr)
						Word.WHEN ->
							parseWhen(expr)
						else ->
						{
							throw UnsupportedOperationException("only the pull word is usable in expressions: $next")
						}
					}
				}
				else    ->
				{
					println(expr)
					throw UnsupportedOperationException("token out of place $next")
				}
			}
		}
		
		return expr
	}
	
	private fun PeekIterator<TokenData>.parseType(): Types
	{
		var next: TokenData
		
		next = next()
		require(next.type == TYPE || next.type == PAREN_L)
		{
			"type must be TYPE or PAREN_L | $next"
		}
		
		if (next.type == TYPE)
		{
			return Basic(next.data)
		}
		
		val types = mutableListOf<Types>()
		
		while (!empty)
		{
			next = peek() ?: break
			
			if (next.type == TYPE || next.type == PAREN_L)
			{
				types += parseType()
				continue
			}
			
			if (next.type == COMMA)
			{
				require(types.isNotEmpty())
				{
					"first value must be a type"
				}
				
				next() // skip comma
				continue
			}
			
			if (next.type == PAREN_R)
			{
				require(types.isNotEmpty())
				{
					"tuple must contain types"
				}
				
				next() // skip paren r
				break
			}
		}
		
		return Tuple(types)
	}
	
	private fun PeekIterator<TokenData>.parseTypeQuery(cmds: MutableList<Command>)
	{
		var next: TokenData
		
		next = next()
		require(next.type == PAREN_L)
		{
			"type query must have a value in parentheses"
		}
		
		val expr = mutableListOf<Command>()
		parseShuntedExpression(expr)
		
		next = next()
		require(next.type == PAREN_R)
		{
			"type query must have a value in parentheses"
		}
		
		cmds += CommandTypeQuery(Route.of(expr))
	}
	
	private fun PeekIterator<TokenData>.parseLit(cmds: MutableList<Command>, token: TokenData)
	{
		val data = when (token.type)
		{
			NUM  ->
			{
				if ('.' !in token.data)
					token.data.toLong()
				else
					token.data.toDouble()
			}
			LET  ->
			{
				token.data.single()
			}
			TXT  ->
			{
				token.data
			}
			BIT  ->
			{
				when (token.data)
				{
					"true"  ->
						true
					"false" ->
						false
					else    ->
					{
						throw IllegalStateException("Invalid bit literal: $token")
					}
				}
			}
			else ->
			{
				throw IllegalStateException("Invalid literal: $token")
			}
		}
		
		val type = when (data)
		{
			is Long   ->
				"Int"
			is Double ->
				"Dec"
			else      ->
			{
				token.type.name.toLowerCase().capitalize()
			}
		}
		
		cmds += CommandLiteral(type, data)
	}
	
	private fun PeekIterator<TokenData>.parseRef(cmds: MutableList<Command>, token: TokenData)
	{
		when (peek()?.type)
		{
			PAREN_L -> // function call
			{
				val params = parseTup(funcParams = true)
				
				params.forEach()
				{ expr ->
					cmds += CommandRoute(Route.of(expr))
				}
				
				cmds += CommandFunctionAccess(token.data)
			}
			else    ->
			{
				cmds += CommandPropertyAccess(token.data)
			}
		}
	}
	
	private fun PeekIterator<TokenData>.parseNew(cmds: MutableList<Command>, token: TokenData)
	{
		println("parsing new ${token.data}")
		ignoreNewLines()
		
		var next: TokenData
		
		next = next()
		require(next.type == BRACE_L)
		{
			"type must be followed by brace l"
		}
		
		ignoreNewLines()
		
		while (!empty)
		{
			next = next()
			
			if (next.type == NAME)
			{
				println("parsing property $next")
			}
			
		}
		
		
	}
	
	private fun PeekIterator<TokenData>.parseTup(funcParams: Boolean = false): List<List<Command>>
	{
		val cmds = mutableListOf<List<Command>>()
		
		var next: TokenData
		
		next = next()
		require(next.type == PAREN_L)
		{
			"tuple must start with paren l"
		}
		
		while (!empty)
		{
			next = next()
			
			if (next.type == COMMA)
			{
				require(cmds.isNotEmpty())
				{
					"first value required before comma"
				}
				
				continue
			}
			
			if (next.type == PAREN_R)
			{
				require(funcParams || cmds.isNotEmpty())
				{
					"tuple must contain data"
				}
				
				break
			}
			if (next.type == PAREN_L)
			{
				move(-1)
				
				val nest = parseTup()
				cmds += nest.flatten() + CommandTuple(nest.size)
				
				continue
			}
			
			move(-1)
			cmds += shuntingYard(parseExpr(breakOnComma = true))
		}
		
		return cmds.reversed()
	}
	
	private fun PeekIterator<TokenData>.parseInd(cmds: MutableList<Command>)
	{
		var next: TokenData
		
		next = next()
		require(next.type == BRACK_L)
		{
			"index access requires brack l"
		}
		
		val expr = mutableListOf<Command>()
		parseShuntedExpression(expr)
		
		next = next()
		require(next.type == BRACK_R)
		{
			"index access requires brack r"
		}
		
		cmds += CommandGet(Route.of(expr))
	}
	
	
	private fun shuntingYard(cmds: List<Command>): List<Command>
	{
		val temp = mutableListOf<Command>()
		val deck = ArrayDeque<CommandOperate>()
		
		cmds.forEach()
		{ cmd ->
			when (cmd)
			{
				is CommandOperate ->
				{
					when (cmd.oper)
					{
						is Oper.SOS ->
						{
							deck.push(cmd)
						}
						is Oper.EOS ->
						{
							while (deck.isNotEmpty() && deck.peek().oper !is Oper.SOS)
							{
								temp += deck.pop()
							}
							
							checkNotNull(deck.pollFirst())
							{
								"missing closing paren"
							}
						}
						else        ->
						{
							while (deck.isNotEmpty())
							{
								val thisOp = cmd.oper as OperatorLvl
								val nextOp = deck.peek().oper
								
								if (nextOp !is Oper.SOS)
								{
									val nextLvl = nextOp as OperatorLvl
									if (nextLvl.lvl > thisOp.lvl || (nextLvl.dir == Oper.Dir.L && nextLvl.lvl == thisOp.lvl))
									{
										temp += deck.pop()
										continue
									}
								}
								
								break
							}
							
							deck.push(cmd)
						}
					}
				}
				else              ->
				{
					temp += cmd
				}
			}
		}
		
		while (deck.isNotEmpty())
		{
			temp += deck.pop()
		}
		
		return temp
	}
	
	private fun PeekIterator<TokenData>.ignoreNewLines()
	{
		while (!empty)
		{
			if (peek()?.type != NEWLINE)
			{
				break
			}
			
			move(1)
		}
	}
	
}