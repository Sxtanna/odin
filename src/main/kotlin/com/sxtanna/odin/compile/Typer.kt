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
import com.sxtanna.odin.compile.util.Interpolator
import com.sxtanna.odin.compile.util.PeekIterator
import com.sxtanna.odin.results.None
import com.sxtanna.odin.results.Result
import com.sxtanna.odin.results.Some
import com.sxtanna.odin.results.map
import com.sxtanna.odin.runtime.Command
import com.sxtanna.odin.runtime.CommandClazzDefine
import com.sxtanna.odin.runtime.CommandConsolePull
import com.sxtanna.odin.runtime.CommandConsolePush
import com.sxtanna.odin.runtime.CommandFunctionAccess
import com.sxtanna.odin.runtime.CommandFunctionDefine
import com.sxtanna.odin.runtime.CommandGet
import com.sxtanna.odin.runtime.CommandHead
import com.sxtanna.odin.runtime.CommandInstanceFunctionAccess
import com.sxtanna.odin.runtime.CommandJavaTypeDefine
import com.sxtanna.odin.runtime.CommandLiteral
import com.sxtanna.odin.runtime.CommandLoop
import com.sxtanna.odin.runtime.CommandOperate
import com.sxtanna.odin.runtime.CommandPropertyAccess
import com.sxtanna.odin.runtime.CommandPropertyAssign
import com.sxtanna.odin.runtime.CommandPropertyDefine
import com.sxtanna.odin.runtime.CommandPropertyResets
import com.sxtanna.odin.runtime.CommandRedo
import com.sxtanna.odin.runtime.CommandRoute
import com.sxtanna.odin.runtime.CommandStackPull
import com.sxtanna.odin.runtime.CommandStackPush
import com.sxtanna.odin.runtime.CommandStop
import com.sxtanna.odin.runtime.CommandTail
import com.sxtanna.odin.runtime.CommandTraitDefine
import com.sxtanna.odin.runtime.CommandTuple
import com.sxtanna.odin.runtime.CommandTypeQuery
import com.sxtanna.odin.runtime.CommandWhen
import com.sxtanna.odin.runtime.base.Basic
import com.sxtanna.odin.runtime.base.Clazz
import com.sxtanna.odin.runtime.base.Route
import com.sxtanna.odin.runtime.base.Trait
import com.sxtanna.odin.runtime.base.Tuple
import com.sxtanna.odin.runtime.base.Types
import com.sxtanna.odin.runtime.data.Func
import com.sxtanna.odin.runtime.data.Prop
import java.util.ArrayDeque

object Typer : (List<TokenData>) -> List<Command>
{
	
	private val expanders = mutableListOf<Expander>()
	
	
	init
	{
		expanders += Expander(
			skipCount = 0,
			hereMatch = { it.type == TXT && Interpolator.hasInterpolation(it.data) },
			intoToken = {
				
				val outputs = mutableListOf<TokenData>()
				val interps = Interpolator.getInterpolation(it.data)
				
				for ((i, interp) in interps.withIndex())
				{
					when (interp)
					{
						is Interpolator.Interpolation.Text ->
						{
							outputs += TokenData(TXT, interp.text)
							
							if (i < interps.lastIndex)
							{
								outputs += TokenData(OPER, "+")
							}
						}
						is Interpolator.Interpolation.Expr ->
						{
							
							when (val lexed = Result.of { Lexer.invoke(interp.text) }.map(this::pass0))
							{
								is None -> throw lexed.info
								is Some ->
								{
									outputs += TokenData(PAREN_L, "(")
									outputs += lexed.data
									outputs += TokenData(PAREN_R, ")")
									
									if (i != interps.lastIndex)
									{
										outputs += TokenData(OPER, "+")
									}
								}
							}
						}
					}
				}
				
				outputs
			})
		
		expanders += Expander(
			skipCount = 1,
			intoToken = { listOf(TokenData(it.type, it.data), TokenData(ASSIGN, "="), TokenData(it.type, it.data), TokenData(OPER, "+")) },
			hereMatch = { it.type == NAME },
			nextMatch = { it.type == OPER && it.data == "+=" })
		
		expanders += Expander(
			skipCount = 1,
			intoToken = { listOf(TokenData(PAREN_L, "("), TokenData(it.type, it.data), TokenData(ASSIGN, "="), TokenData(it.type, it.data), TokenData(OPER, "+"), TokenData(NUM, "1"), TokenData(PAREN_R, ")")) },
			hereMatch = { it.type == NAME },
			nextMatch = { it.type == OPER && it.data == "++" })
		
		expanders += Expander(
			skipCount = 1,
			intoToken = { listOf(TokenData(PAREN_L, "("), TokenData(it.type, it.data), TokenData(ASSIGN, "="), TokenData(it.type, it.data), TokenData(OPER, "-"), TokenData(NUM, "1"), TokenData(PAREN_R, ")")) },
			hereMatch = { it.type == NAME },
			nextMatch = { it.type == OPER && it.data == "--" })
	}
	
	
	private fun pass0(data: List<TokenData>): List<TokenData>
	{
		val toks = mutableListOf<TokenData>()
		val iter = PeekIterator(data)
		
		iter.each()
		{ here ->
			
			val prev = iter.peek(amount = -2)
			val next = iter.peek(amount = +0)
			
			val expanse = expanders.firstOrNull { it.matches(here, prev, next) }
			
			if (expanse == null)
			{
				toks += here
			}
			else
			{
				toks += expanse.intoToken.invoke(here)
				
				iter.move(expanse.skipCount)
			}
		}
		
		return toks
	}
	
	private fun pass1(toks: List<TokenData>): List<Command>
	{
		val cmds = mutableListOf<Command>()
		val iter = PeekIterator(toks)
		
		while (!iter.empty)
		{
			iter.parseMain(cmds)
		}
		
		return cmds
	}
	
	private fun pass2(data: List<Command>): List<Command>
	{
		val cmds = mutableListOf<Command>()
		val iter = PeekIterator(data)
		
		
		while (!iter.empty)
		{
			val here = iter.next
			val prev = iter.peek(amount = -2)
			val next = iter.peek(amount = +0)
			
			if (here is CommandFunctionDefine)
			{
				here.func.body = Route.of(pass2(here.func.body?.unwrap() ?: continue))
			}
			
			if (here is CommandRoute && prev is CommandPropertyAssign && next is CommandPropertyAssign)
			{
				val body = here.route.unwrap().toMutableList()
				body += next
				
				iter.move(amount = 1)
				
				here.route = Route.of(pass2(body))
			}
			
			if (next is CommandHead)
			{
				val loop = iter.peek(amount = 1)
				require(loop is CommandLoop)
				{
					"target was not loop: $loop"
				}
				
				val body = loop.body.unwrap().toMutableList()
				
				val last = body.removeLast()
				require(last is CommandStackPush)
				{
					"last statement was not a stack push: $last"
				}
				
				val lastExpr = last.expr.unwrap().toMutableList()
				
				iter.move(amount = 2) // skip over body
				
				val tail = iter.peek
				require(tail is CommandTail)
				{
					"target was not tail: $tail"
				}
				
				iter.move(amount = 1) // skip over tail
				
				body.add(0, here)
				body.addAll(1, lastExpr)
				body.add(iter.next)
				body.add(iter.next)
				
				loop.body = Route.of(body)
				
				cmds += loop
				
				continue
			}
			
			cmds += here
		}
		
		return cmds
	}
	
	
	override fun invoke(data: List<TokenData>): List<Command>
	{
		val pass0 = pass0(data)
		val pass1 = pass1(pass0)
		val pass2 = pass2(pass1)
		
		return pass2
	}
	
	
	private fun PeekIterator<TokenData>.parseMain(cmds: MutableList<Command>)
	{
		ignoreNewLines()
		val token = next
		
		when (token.type)
		{
			WORD       ->
			{
				parseWord(cmds, token)
			}
			NAME       ->
			{
				parseName(cmds, token)
			}
			NUM        ->
			{
				move(amount = -1)
				parseShuntedExpression(cmds)
			}
			PAREN_L    ->
			{
				parseShuntedExpression(cmds)
				
				require(peek?.type == PAREN_R)
				
				move(amount = 1)
			}
			STACK_PUSH ->
			{
				val expr = mutableListOf<Command>()
				parseShuntedExpression(expr)
				
				cmds += CommandStackPush(Route.of(expr))
			}
			STACK_PULL ->
			{
				cmds += CommandStackPull(true)
			}
			else       ->
			{
				throw IllegalStateException("token out of place: $token")
			}
		}
	}
	
	
	private fun PeekIterator<TokenData>.parseName(cmds: MutableList<Command>, data: TokenData)
	{
		var token: TokenData
		
		token = next
		
		when (token.type)
		{
			OPER    ->
			{
				move(amount = -2)
				
				val expr = mutableListOf<Command>()
				parseShuntedExpression(expr)
				
				cmds += CommandRoute(Route.of(expr))
			}
			PAREN_L ->
			{
				move(amount = -1)
				
				parseRef(cmds, data)
			}
			ASSIGN  ->
			{
				val expr = mutableListOf<Command>()
				parseShuntedExpression(expr)
				
				cmds += CommandRoute(Route.of(expr))
				cmds += CommandPropertyAssign(data.data)
			}
			POINT   ->
			{
				move(amount = -1)
				
				parseRef(cmds, data)
			}
			else    ->
			{
				throw UnsupportedOperationException("token out of place: $token")
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
			Word.TYPE  ->
				parseTypeQuery(cmds)
			Word.LOOP  ->
				parseLoop(cmds)
			Word.STOP  ->
				cmds += CommandStop
			Word.JAVA  ->
				parseJava(cmds)
			Word.REDO  ->
			{
				val check = peek
				require(check == null || check.type == NUM)
				{
					"peek must be followed by nothing or a number"
				}
				
				if (check != null)
				{
					move(amount = 1)
				}
				
				val numb = check?.data?.toIntOrNull() ?: -1
				
				cmds += CommandRedo(numb)
			}
			else       ->
			{
				throw UnsupportedOperationException("handling for word $word not found")
			}
		}
	}
	
	private fun PeekIterator<TokenData>.parseProp(cmds: MutableList<Command>, mutable: Boolean, assignment: Boolean)
	{
		var token: TokenData
		
		token = next
		require(token.type == NAME)
		{
			"property missing name"
		}
		
		
		val prop = Prop(token.data, mutable)
		cmds += CommandPropertyDefine(prop)
		
		if (peek?.type == TYPED)
		{
			move(amount = 1)
			prop.type = parseType()
		}
		
		
		if (!assignment)
		{
			return
		}
		
		
		token = next
		require(token.type == ASSIGN)
		{
			"must be assignment: $token"
		}
		
		parseShuntedExpression(cmds)
		
		cmds += CommandPropertyAssign(prop.name)
	}
	
	private fun PeekIterator<TokenData>.parseFunc(cmds: MutableList<Command>)
	{
		var token: TokenData
		
		token = next
		require(token.type == NAME)
		{
			"function missing name"
		}
		
		val func = Func(token.data)
		
		if (peek?.type == PAREN_L)
		{
			token = next
			require(token.type == PAREN_L)
			{
				"function parameters must be surrounded in parentheses"
			}
			
			val names = mutableSetOf<String>()
			
			while (!empty)
			{
				token = next
				
				if (token.type == PAREN_R)
				{
					require(func.pull.isNotEmpty())
					{
						"function parentheses must contain parameters"
					}
					
					move(amount = -1)
					
					break
				}
				
				if (token.type == COMMA)
				{
					require(names.isNotEmpty() || func.pull.isNotEmpty())
					{
						"first value must be a parameter"
					}
					
					continue
				}
				
				if (token.type == NAME)
				{
					require(names.add(token.data))
					{
						"parameter ${token.data} already defined for function ${func.name}"
					}
					
					continue
				}
				
				if (token.type == TYPED)
				{
					val type = parseType()
					
					for (name in names)
					{
						func.pull[name] = type
					}
					
					names.clear()
					continue
				}
				
				throw UnsupportedOperationException("Token out of place: $token")
			}
			
			token = next
			require(token.type == PAREN_R)
			{
				"function parameters must be surrounded in parentheses"
			}
			
			// println("func ${func.name} pull: ${func.pull}")
		}
		
		if (peek?.type == TYPED)
		{
			token = next
			require(token.type == TYPED)
			{
				"function return type must be specified with typed symbol"
			}
			
			func.push["ret0"] = parseType()
			
			// println("func ${func.name} push: ${func.push}")
		}
		
		ignoreNewLines()
		
		token = next
		require(token.type == BRACE_L)
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
			token = peek ?: break
			
			if (token.type == BRACE_R)
			{
				break
			}
			if (token.type == NEWLINE)
			{
				move(amount = 1)
				continue
			}
			if (token.type == RETURN)
			{
				move(amount = 1)
				
				parseShuntedExpression(body)
				break
			}
			
			parseMain(body)
		}
		
		ignoreNewLines()
		
		token = next
		require(token.type == BRACE_R)
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
	
	private fun PeekIterator<TokenData>.parsePush(cmds: MutableList<Command>, returnToStack: Boolean = false)
	{
		parseShuntedExpression(cmds)
		cmds += CommandConsolePush(true, returnToStack)
	}
	
	private fun PeekIterator<TokenData>.parsePull(cmds: MutableList<Command>)
	{
		var type = Types.none()
		var text = null as? String?
		
		if (!empty && peek?.type == BOUND || peek?.type == PAREN_L)
		{
			var token: TokenData
			
			token = next
			if (token.type == BOUND)
			{
				type = requireNotNull(parseBound().singleOrNull())
				{
					"pull bounds can be only 1 type"
				}
				
				if (!empty && peek?.type == PAREN_L)
				{
					token = next
				}
			}
			
			if (token.type == PAREN_L)
			{
				token = next
				require(token.type == TXT)
				{
					"pull parens must contain a txt prompt"
				}
				
				text = token.data
				
				token = next
				require(token.type == PAREN_R)
				{
					"pull parens missing close"
				}
			}
		}
		
		cmds += CommandConsolePull(type, text)
	}
	
	private fun PeekIterator<TokenData>.parseWhen(cmds: MutableList<Command>)
	{
		var token: TokenData
		
		token = next
		require(token.type == PAREN_L)
		{
			"when condition must be in parentheses"
		}
		
		val expr = mutableListOf<Command>()
		parseShuntedExpression(expr)
		
		token = next
		require(token.type == PAREN_R)
		{
			"when condition must be in parentheses"
		}
		
		ignoreNewLines()
		
		token = next
		require(token.type == BRACE_L)
		{
			"when condition pass must be in braces"
		}
		
		ignoreNewLines()
		
		val pass = mutableListOf<Command>()
		
		while (!empty)
		{
			token = peek ?: break
			
			if (token.type == BRACE_R)
			{
				break
			}
			if (token.type == NEWLINE)
			{
				move(amount = 1)
				continue
			}
			
			parseMain(pass)
		}
		
		ignoreNewLines()
		
		token = next
		require(token.type == BRACE_R)
		{
			"when condition pass must be in braces"
		}
		
		ignoreNewLines()
		
		val check = peek
		if (check == null || check.type != WORD || Word.find(check.data) != Word.ELSE)
		{
			cmds += CommandWhen(Route.of(expr), Route.of(pass), null)
			return
		}
		
		move(amount = 1) // skip else word
		
		ignoreNewLines()
		
		token = next
		require(token.type == BRACE_L)
		{
			"when condition fail must be in braces"
		}
		
		ignoreNewLines()
		
		val fail = mutableListOf<Command>()
		
		while (!empty)
		{
			token = peek ?: break
			
			if (token.type == BRACE_R)
			{
				break
			}
			if (token.type == NEWLINE)
			{
				move(amount = 1)
				continue
			}
			
			parseMain(fail)
		}
		
		ignoreNewLines()
		
		token = next
		require(token.type == BRACE_R)
		{
			"when condition pass must be in braces"
		}
		
		cmds += CommandWhen(Route.of(expr), Route.of(pass), Route.of(fail))
	}
	
	private fun PeekIterator<TokenData>.parseLoop(cmds: MutableList<Command>)
	{
		var token: TokenData
		
		token = next
		require(token.type == PAREN_L)
		{
			"loop condition must be in parentheses"
		}
		
		val expr = mutableListOf<Command>()
		parseShuntedExpression(expr)
		
		token = next
		require(token.type == PAREN_R)
		{
			"loop condition must be in parentheses"
		}
		
		ignoreNewLines()
		
		token = next
		require(token.type == BRACE_L)
		{
			"loop body pass must be in braces"
		}
		
		ignoreNewLines()
		
		val body = mutableListOf<Command>()
		
		while (!empty)
		{
			token = peek ?: break
			
			if (token.type == BRACE_R)
			{
				break
			}
			if (token.type == NEWLINE)
			{
				move(amount = 1)
				continue
			}
			
			parseMain(body)
		}
		
		ignoreNewLines()
		
		token = next
		require(token.type == BRACE_R)
		{
			"loop body pass must be in braces"
		}
		
		cmds += CommandLoop(Route.of(expr), Route.of(body))
	}
	
	private fun PeekIterator<TokenData>.parseJava(cmds: MutableList<Command>)
	{
		var token: TokenData
		
		token = next
		require(token.type == BOUND)
		{
			"java reference must be bound!"
		}
		
		token = next
		require(token.type == BRACK_L)
		{
			"java bounds must be a txt in bracks"
		}
		
		token = next
		require(token.type == TXT)
		{
			"java bounds must be a FQN class"
		}
		
		val name = token.data
		
		token = next
		require(token.type == BRACK_R)
		{
			"java bounds must be a txt in bracks"
		}
		
		cmds += CommandJavaTypeDefine(Class.forName(name))
	}
	
	private fun PeekIterator<TokenData>.parseBound(): List<Types>
	{
		var token: TokenData
		
		token = next
		require(token.type == BRACK_L)
		{
			"bounds missing brack l"
		}
		
		val types = mutableListOf<Types>()
		
		while (!empty)
		{
			if (token.type == NEWLINE)
			{
				continue
			}
			
			if (token.type == COMMA)
			{
				require(types.isNotEmpty())
				{
					"first value must be a type"
				}
				
				continue
			}
			
			if (token.type == BRACK_R)
			{
				require(types.isNotEmpty())
				{
					"bounds must not be empty"
				}
				
				break
			}
			
			types += parseType()
			
			token = next
		}
		
		return types
	}
	
	private fun PeekIterator<TokenData>.parseTrait(cmds: MutableList<Command>)
	{
		var token: TokenData
		
		token = next
		require(token.type == TYPE)
		{
			"trait missing type name"
		}
		
		val name = token.data
		
		val trait = Trait(name)
		
		cmds += CommandTraitDefine(trait)
		
		token = next
		require(token.type == PAREN_L || token.type == BRACE_L || token.type == BOUND)
		{
			"trait missing props or body"
		}
		
		if (token.type == PAREN_L) // parse props
		{
			val props = mutableListOf<Command>()
			
			while (!empty)
			{
				token = next
				
				if (token.type == PAREN_R)
				{
					require(props.isNotEmpty())
					{
						"trait parentheses must contain properties"
					}
					
					break
				}
				
				if (token.type == COMMA)
				{
					require(props.isNotEmpty())
					{
						"first value must be a property"
					}
					
					continue
				}
				
				if (token.type == WORD)
				{
					val word = requireNotNull(Word.find(token.data))
					{
						"word ${token.data} not found!"
					}
					
					when (word)
					{
						Word.VAL,
						Word.VAR ->
							parseProp(props, word == Word.VAR, false)
						else     ->
						{
							throw UnsupportedOperationException("word out of place: $token")
						}
					}
					
					continue
				}
				
				throw UnsupportedOperationException("token out of place: $token")
			}
			
			val route = Route.of(props)
			trait.route = route
			
			if (!empty)
			{
				token = next
			}
		}
		
		if (token.type == BOUND)
		{
			trait.types += parseBound()
		}
		
		if (token.type == BRACE_L) // parse body
		{
			// trouble time
		}
	}
	
	private fun PeekIterator<TokenData>.parseClazz(cmds: MutableList<Command>)
	{
		var token: TokenData
		
		token = next
		require(token.type == TYPE)
		{
			"class missing type name"
		}
		
		val name = token.data
		
		val clazz = Clazz(name)
		
		cmds += CommandClazzDefine(clazz)
		
		token = next
		require(token.type == BRACE_L || token.type == BOUND)
		{
			"class missing bounds or body"
		}
		
		if (token.type == BOUND)
		{
			clazz.types += parseBound()
		}
		
		if (token.type == BRACE_L) // parse body
		{
			// trouble time
		}
	}
	
	
	private fun PeekIterator<TokenData>.parseShuntedExpression(cmds: MutableList<Command>)
	{
		val expr = parseExpr()
		
		cmds += shuntingYard(expr)
	}
	
	private fun PeekIterator<TokenData>.parseExpr(breakOnComma: Boolean = false): List<Command>
	{
		val expr = mutableListOf<Command>()
		
		var open = 0
		val head = index
		
		var token: TokenData
		
		loop@ while (!empty)
		{
			token = next
			
			if (token.type == NEWLINE)
			{
				break
			}
			if (token.type == COMMA && breakOnComma)
			{
				move(amount = -1)
				break
			}
			if (token.type == BRACE_R || token.type == BRACK_R)
			{
				move(amount = -1)
				break
			}
			
			when (token.type)
			{
				NUM,
				LET,
				TXT,
				BIT     ->
					parseLit(expr, token)
				NAME    ->
				{
					if (peek?.type != ASSIGN)
					{
						parseRef(expr, token)
					}
					else
					{
						parseName(expr, token)
						expr += CommandPropertyAccess(token.data)
					}
				}
				TYPE    ->
					parseNew(expr, token)
				OPER    ->
				{
					val oper = when (token.data)
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
							throw UnsupportedOperationException("unknown operator: $token")
						}
					}
					
					expr += CommandOperate(oper)
				}
				COMMA   ->
				{
					require(open > 0)
					{
						"comma out of position: $token"
					}
					
					var erased = 0
					while (peek?.type != PAREN_L || index > head)
					{
						erased++
						move(amount = -1)
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
						move(amount = -1)
						break@loop
					}
					
					open--
					expr += CommandOperate(Oper.EOS)
				}
				BRACK_L ->
				{
					move(amount = -1)
					parseInd(expr)
				}
				WORD    ->
				{
					when (Word.find(token.data))
					{
						Word.PULL ->
							parsePull(expr)
						Word.PUSH ->
							parsePush(expr, returnToStack = true)
						Word.TYPE ->
							parseTypeQuery(expr)
						Word.WHEN ->
							parseWhen(expr)
						Word.JAVA ->
							parseJava(expr)
						Word.LOOP ->
						{
							expr += CommandHead
							parseLoop(expr)
							expr += CommandTail
						}
						else      ->
						{
							throw UnsupportedOperationException("word isn't usable in expressions: $token")
						}
					}
				}
				STACK_PULL ->
				{
					expr += CommandStackPull(false)
				}
				else    ->
				{
					println(expr)
					throw UnsupportedOperationException("token out of place $token")
				}
			}
		}
		
		return expr
	}
	
	private fun PeekIterator<TokenData>.parseType(): Types
	{
		var token: TokenData
		
		var tuple = false
		val types = mutableListOf<Types>()
		
		fun internalParseType(): List<Types>
		{
			token = next
			require(token.type == TYPE || token.type == PAREN_L)
			{
				"type must be TYPE or PAREN_L | $token"
			}
			
			if (token.type == TYPE)
			{
				val basic = Basic(token.data)
				
				if (peek?.type != BOUND)
				{
					return listOf(basic)
				}
				
				check(tuple)
				{
					"shorthand tuple syntax must be surrounded by parens"
				}
				
				move(amount = 1)
				
				val value = resolveValue(next)
				require(value.first == "Int")
				{
					"shorthand tuple syntax must have a whole number"
				}
				
				repeat((value.second as Long).toInt())
				{
					types += basic
				}
			}
			
			return emptyList()
		}
		
		types += internalParseType()
		if (types.size == 1)
		{
			return types.single()
		}
		
		while (!empty)
		{
			token = peek ?: break
			
			if (token.type == TYPE || token.type == PAREN_L)
			{
				tuple = true
				types += internalParseType()
				tuple = false
				continue
			}
			
			if (token.type == COMMA)
			{
				require(types.isNotEmpty())
				{
					"first value must be a type"
				}
				
				move(amount = 1) // skip comma
				continue
			}
			
			if (token.type == PAREN_R)
			{
				require(types.isNotEmpty())
				{
					"tuple must contain types"
				}
				
				move(amount = 1) // skip paren r
				break
			}
			
			throw IllegalStateException("token out of place: $token")
		}
		
		return Tuple(types)
	}
	
	private fun PeekIterator<TokenData>.parseTypeQuery(cmds: MutableList<Command>)
	{
		var token: TokenData
		
		token = next
		require(token.type == PAREN_L)
		{
			"type query must have a value in parentheses"
		}
		
		val expr = mutableListOf<Command>()
		parseShuntedExpression(expr)
		
		token = next
		require(token.type == PAREN_R)
		{
			"type query must have a value in parentheses"
		}
		
		cmds += CommandTypeQuery(Route.of(expr))
	}
	
	private fun PeekIterator<TokenData>.parseLit(cmds: MutableList<Command>, token: TokenData)
	{
		val value = resolveValue(token)
		
		cmds += CommandLiteral(value.first, value.second)
	}
	
	private fun PeekIterator<TokenData>.parseRef(cmds: MutableList<Command>, token: TokenData, assigned: Boolean = false)
	{
		when (peek?.type)
		{
			PAREN_L -> // function call
			{
				parseFuncCall(cmds, token)
			}
			POINT   -> // instance function call
			{
				parseFuncCall(cmds, token, instance = true)
			}
			else    ->
			{
				cmds += CommandPropertyAccess(token.data)
			}
		}
	}
	
	private fun PeekIterator<TokenData>.parseNew(cmds: MutableList<Command>, data: TokenData)
	{
		println("parsing new ${data.data}")
		ignoreNewLines()
		
		var token: TokenData
		
		token = next
		require(token.type == BRACE_L)
		{
			"type must be followed by brace l"
		}
		
		ignoreNewLines()
		
		while (!empty)
		{
			token = next
			
			if (token.type == NAME)
			{
				val name = token.data
				
				token = next
				require(token.type == TYPED)
				{
					"new class properties assign with :"
				}
				
				val expr = mutableListOf<Command>()
				parseShuntedExpression(expr)
				
				println("$name: $expr")
			}
			
		}
		
	}
	
	private fun PeekIterator<TokenData>.parseTup(funcParams: Boolean = false): List<List<Command>>
	{
		val cmds = mutableListOf<List<Command>>()
		
		var token: TokenData
		
		token = next
		require(token.type == PAREN_L)
		{
			"tuple must start with paren l"
		}
		
		while (!empty)
		{
			token = next
			
			if (token.type == COMMA)
			{
				require(cmds.isNotEmpty())
				{
					"first value required before comma"
				}
				
				continue
			}
			
			if (token.type == PAREN_R)
			{
				require(funcParams || cmds.isNotEmpty())
				{
					"tuple must contain data"
				}
				
				break
			}
			if (token.type == PAREN_L)
			{
				move(amount = -1)
				
				val nest = parseTup()
				cmds += nest.flatten() + CommandTuple(nest.size)
				
				continue
			}
			
			move(amount = -1)
			cmds += shuntingYard(parseExpr(breakOnComma = true))
		}
		
		return cmds.reversed()
	}
	
	private fun PeekIterator<TokenData>.parseInd(cmds: MutableList<Command>)
	{
		var token: TokenData
		
		token = next
		require(token.type == BRACK_L)
		{
			"index access requires brack l"
		}
		
		val expr = mutableListOf<Command>()
		parseShuntedExpression(expr)
		
		token = next
		require(token.type == BRACK_R)
		{
			"index access requires brack r"
		}
		
		cmds += CommandGet(Route.of(expr))
	}
	
	
	private fun PeekIterator<TokenData>.parseFuncCall(cmds: MutableList<Command>, data: TokenData, instance: Boolean = false)
	{
		if (!instance)
		{
			val params = parseTup(funcParams = true)
			
			params.forEach()
			{ expr ->
				cmds += CommandRoute(Route.of(expr))
			}
			
			cmds += CommandFunctionAccess(data.data)
			return
		}
		
		var token: TokenData
		
		token = next
		require(token.type == POINT)
		{
			"instance function call must be with a point"
		}
		
		token = next
		require(token.type == WORD || token.type == NAME || token.type == TYPE)
		{
			"instance function call must be one of [WORD, NAME, TYPE]"
		}
		
		val funcName = token.data
		
		val params = parseTup(funcParams = true)
		
		params.forEach()
		{ expr ->
			cmds += CommandRoute(Route.of(expr))
		}
		
		cmds += CommandPropertyAccess(data.data)
		cmds += CommandInstanceFunctionAccess(funcName, params.size)
	}
	
	private fun resolveValue(token: TokenData): Pair<String, Any>
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
		
		return type to data
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
			if (peek?.type != NEWLINE)
			{
				break
			}
			
			move(amount = 1)
		}
	}
	
	
	private data class Expander(val skipCount: Int,
	                            val intoToken: (TokenData) -> List<TokenData>,
	                            val hereMatch: ((TokenData) -> Boolean),
	                            val prevMatch: ((TokenData) -> Boolean)? = null,
	                            val nextMatch: ((TokenData) -> Boolean)? = null)
	{
		fun matches(here: TokenData, prev: TokenData?, next: TokenData?): Boolean
		{
			if (!hereMatch.invoke(here))
			{
				return false
			}
			
			val prevMatches = if (prevMatch != null)
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
			else
			{
				true
			}
			
			val nextMatches = if (nextMatch != null)
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
			else
			{
				true
			}
			
			return prevMatches && nextMatches
		}
	}
	
}