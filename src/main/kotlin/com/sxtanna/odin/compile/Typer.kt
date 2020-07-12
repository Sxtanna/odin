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
import com.sxtanna.odin.runtime.Command
import com.sxtanna.odin.runtime.CommandCase
import com.sxtanna.odin.runtime.CommandCast
import com.sxtanna.odin.runtime.CommandClazzCreate
import com.sxtanna.odin.runtime.CommandClazzDefine
import com.sxtanna.odin.runtime.CommandConsolePull
import com.sxtanna.odin.runtime.CommandConsolePush
import com.sxtanna.odin.runtime.CommandFunctionAccess
import com.sxtanna.odin.runtime.CommandFunctionDefine
import com.sxtanna.odin.runtime.CommandGet
import com.sxtanna.odin.runtime.CommandHead
import com.sxtanna.odin.runtime.CommandInstanceFunctionAccess
import com.sxtanna.odin.runtime.CommandInstancePropertyAccess
import com.sxtanna.odin.runtime.CommandJavaTypeDefine
import com.sxtanna.odin.runtime.CommandLiteral
import com.sxtanna.odin.runtime.CommandLoop
import com.sxtanna.odin.runtime.CommandNone
import com.sxtanna.odin.runtime.CommandOperate
import com.sxtanna.odin.runtime.CommandPropertyAccess
import com.sxtanna.odin.runtime.CommandPropertyAssign
import com.sxtanna.odin.runtime.CommandPropertyDefine
import com.sxtanna.odin.runtime.CommandPropertyResets
import com.sxtanna.odin.runtime.CommandRoute
import com.sxtanna.odin.runtime.CommandSet
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
import java.util.UUID

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
							
							if (i < (interps.size - 1))
							{
								outputs += TokenData(OPER, "+")
							}
						}
						is Interpolator.Interpolation.Expr ->
						{
							val lexed = pass0(Lexer(interp.text))
							
							outputs += TokenData(PAREN_L, "(")
							outputs += lexed
							outputs += TokenData(PAREN_R, ")")
							
							if (i != (interps.size - 1))
							{
								outputs += TokenData(OPER, "+")
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
			intoToken = { listOf(TokenData(PAREN_L, "_"), TokenData(it.type, it.data), TokenData(ASSIGN, "="), TokenData(it.type, it.data), TokenData(OPER, "+"), TokenData(NUM, "1"), TokenData(PAREN_R, ")")) },
			hereMatch = { it.type == NAME },
			nextMatch = { it.type == OPER && it.data == "++" })
		
		expanders += Expander(
			skipCount = 1,
			intoToken = { listOf(TokenData(PAREN_L, "_"), TokenData(it.type, it.data), TokenData(ASSIGN, "="), TokenData(it.type, it.data), TokenData(OPER, "-"), TokenData(NUM, "1"), TokenData(PAREN_R, ")")) },
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
		
		iter.ignoreNewLines()
		
		while (!iter.empty)
		{
			iter.parseMain(cmds)
			iter.ignoreNewLines()
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
	
	
	private fun IterOfTokens.parseMain(cmds: CommandChain)
	{
		when (peek?.type)
		{
			WORD       ->
			{
				parseWord(cmds, next)
			}
			NAME       ->
			{
				parseName(cmds, next)
			}
			STACK_PUSH ->
			{
				move(amount = 1)
				
				val expr = mutableListOf<Command>()
				parseShuntedExpression(expr)
				
				cmds += CommandStackPush(Route.of(expr))
			}
			STACK_PULL ->
			{
				move(amount = 1)
				
				cmds += CommandStackPull(true)
			}
			PAREN_L    ->
			{
				move(amount = 1)
				
				parseShuntedExpression(cmds)
				
				require(peek?.type == PAREN_R)
				
				move(amount = 1)
			}
			NUM        ->
			{
				parseShuntedExpression(cmds)
			}
			else       ->
			{
				throw IllegalStateException("token out of place: $peek")
			}
		}
	}
	
	
	private fun IterOfTokens.parseName(cmds: CommandChain, data: TokenData)
	{
		when (peek?.type)
		{
			OPER    ->
			{
				move(amount = -1)
				
				val expr = mutableListOf<Command>()
				parseShuntedExpression(expr)
				
				cmds += CommandRoute(Route.of(expr))
			}
			ASSIGN  ->
			{
				move(amount = 1)
				
				val expr = mutableListOf<Command>()
				parseShuntedExpression(expr)
				
				cmds += CommandRoute(Route.of(expr))
				cmds += CommandPropertyAssign(data.data)
			}
			POINT,
			PAREN_L ->
			{
				parseRef(cmds, data)
			}
			BRACK_L ->
			{
				cmds += CommandPropertyAccess(data.data)
				parseInd(cmds)
			}
			else    ->
			{
				throw UnsupportedOperationException("token out of place: $peek")
			}
		}
	}
	
	private fun IterOfTokens.parseWord(cmds: CommandChain, data: TokenData)
	{
		val word = requireNotNull(Word.find(data.data))
		{
			"word ${data.data} not found!"
		}
		
		when (word)
		{
			Word.VAL,
			Word.VAR   ->
				parseProp(cmds, word == Word.VAR)
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
			Word.CASE  ->
				parseCase(cmds)
			Word.TYPE  ->
				parseTypeQuery(cmds)
			Word.LOOP  ->
				parseLoop(cmds)
			Word.STOP  ->
				cmds += CommandStop
			Word.JAVA  ->
				parseJava(cmds)
			else       ->
			{
				throw UnsupportedOperationException("handling for word $word not found")
			}
		}
	}
	
	
	private fun IterOfTokens.parseProp(cmds: CommandChain, mutable: Boolean): Prop
	{
		require(peek?.type == NAME)
		{
			"property missing name $peek"
		}
		
		val prop = Prop(next.data, mutable)
		cmds += CommandPropertyDefine(prop)
		
		if (peek?.type == TYPED)
		{
			move(amount = 1)
			prop.type = parseType()
		}
		
		require(peek?.type == ASSIGN)
		{
			"must be assignment: $peek"
		}
		
		move(amount = 1)
		
		parseShuntedExpression(cmds)
		
		cmds += CommandPropertyAssign(prop.name)
		
		return prop
	}
	
	private fun IterOfTokens.parseFunc(cmds: CommandChain): Func
	{
		require(peek?.type == NAME)
		{
			"function missing name $peek"
		}
		
		val func = Func(next.data)
		
		if (peek?.type == PAREN_L)
		{
			move(amount = 1)
			
			val names = mutableSetOf<String>()
			
			while (!empty)
			{
				if (peek?.type == PAREN_R)
				{
					require(func.pull.isNotEmpty())
					{
						"function parentheses must contain parameters"
					}
					
					break
				}
				
				if (peek?.type == COMMA)
				{
					require(names.isNotEmpty() || func.pull.isNotEmpty())
					{
						"first value must be a parameter"
					}
					
					move(amount = 1)
					
					continue
				}
				
				if (peek?.type == NAME)
				{
					val name = next.data
					
					require(names.add(name))
					{
						"parameter $name already defined for function ${func.name}"
					}
					
					continue
				}
				
				if (peek?.type == TYPED)
				{
					move(amount = 1)
					
					val type = parseType()
					
					for (name in names)
					{
						func.pull[name] = type
					}
					
					names.clear()
					continue
				}
				
				throw UnsupportedOperationException("Token out of place: $peek")
			}
			
			require(peek?.type == PAREN_R)
			{
				"function parameters must be surrounded in parentheses"
			}
			
			move(amount = 1)
		}
		
		if (peek?.type == TYPED)
		{
			require(peek?.type == TYPED)
			{
				"function return type must be specified with typed symbol"
			}
			
			move(amount = 1)
			
			func.push["ret0"] = parseType()
		}
		
		ignoreNewLines()
		
		require(peek?.type == BRACE_L)
		{
			"function body must be surrounded with braces : $peek"
		}
		
		move(amount = 1)
		
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
			val token = peek ?: break
			
			if (token.type == BRACE_R)
			{
				break
			}
			if (token.type == NEWLINE)
			{
				ignoreNewLines()
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
		
		require(peek?.type == BRACE_R)
		{
			"function body must be surrounded with braces"
		}
		
		move(amount = 1)
		
		func.pull.forEach()
		{ (name, _) ->
			body += CommandPropertyResets(name)
		}
		
		func.body = Route.of(body)
		
		cmds += CommandFunctionDefine(func)
		
		return func
	}
	
	
	private fun IterOfTokens.parsePush(cmds: CommandChain, returnToStack: Boolean = false)
	{
		parseShuntedExpression(cmds)
		cmds += CommandConsolePush(true, returnToStack)
	}
	
	private fun IterOfTokens.parsePull(cmds: CommandChain)
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
	
	private fun IterOfTokens.parseCast(cmds: CommandChain)
	{
		require(next.type == BOUND)
		{
			"cast target must be in bounds"
		}
		
		val type = requireNotNull(parseBound().singleOrNull())
		{
			"cast bounds can be only 1 type"
		}
		
		require(next.type == PAREN_L)
		{
			"cast target must be in parentheses"
		}
		
		val expr = mutableListOf<Command>()
		parseShuntedExpression(expr)
		
		require(next.type == PAREN_R)
		{
			"cast target must be in parentheses"
		}
		
		cmds += CommandCast(Route.of(expr), type)
	}
	
	private fun IterOfTokens.parseWhen(cmds: CommandChain)
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
	
	private fun IterOfTokens.parseCase(cmds: CommandChain)
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
		
		val cases = mutableMapOf<Route, Route>()
		
		while (!empty)
		{
			token = peek ?: break
			
			if (token.type == BRACE_R)
			{
				move(amount = 1)
				break
			}
			if (token.type == NEWLINE)
			{
				move(amount = 1)
				continue
			}
			
			val caseExpr = parseExpr(breakOnBraceL = true)
			
			token = next
			require(token.type == BRACE_L)
			{
				"case condition must be followed by a code block : $token"
			}
			
			val caseFunc = mutableListOf<Command>()
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
				
				parseMain(caseFunc)
			}
			
			token = next
			require(token.type == BRACE_R)
			{
				"case condition must be followed by a code block : $token"
			}
			
			cases[Route.of(caseExpr)] = Route.of(caseFunc)
		}
		
		cmds += CommandCase(Route.of(expr), cases)
	}
	
	private fun IterOfTokens.parseLoop(cmds: CommandChain)
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
	
	private fun IterOfTokens.parseJava(cmds: CommandChain)
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
		
		var constructorParams = 0
		
		if (peek?.type == PAREN_L)
		{
			val params = parseTup(funcParams = true)
			
			params.forEach()
			{ expr ->
				cmds += CommandRoute(Route.of(expr))
			}
			
			constructorParams = params.size
		}
		
		cmds += CommandJavaTypeDefine(Class.forName(name), constructorParams)
	}
	
	private fun IterOfTokens.parseBound(): List<Types>
	{
		require(peek?.type == BRACK_L)
		{
			"bounds missing brack l"
		}
		
		move(amount = 1)
		
		val types = mutableListOf<Types>()
		
		while (!empty)
		{
			if (peek?.type == NEWLINE)
			{
				ignoreNewLines()
				continue
			}
			
			if (peek?.type == COMMA)
			{
				require(types.isNotEmpty())
				{
					"first value must be a type"
				}
				
				move(amount = 1)
				
				continue
			}
			
			if (peek?.type == BRACK_R)
			{
				require(types.isNotEmpty())
				{
					"bounds must not be empty"
				}
				
				move(amount = 1)
				
				break
			}
			
			types += parseType()
		}
		
		return types
	}
	
	private fun IterOfTokens.parseTrait(cmds: CommandChain)
	{
		require(peek?.type == TYPE)
		{
			"trait missing name $peek"
		}
		
		
		val trait = Trait(next.data)
		cmds += CommandTraitDefine(trait)
		
		
		fun parseTraitProp(mutable: Boolean)
		{
			require(peek?.type == NAME)
			{
				"property missing name $peek"
			}
			
			val prop = Prop(next.data, mutable)
			
			require(peek?.type == TYPED)
			{
				"property missing type assignment $peek"
			}
			
			move(amount = 1)
			
			prop.type = parseType()
			
			trait.addProp(prop)
		}
		
		fun parseTraitFunc()
		{
			require(peek?.type == NAME)
			{
				"function missing name $peek"
			}
			
			val func = Func(next.data)
			val body = mutableListOf<Command>()
			
			if (peek?.type == PAREN_L) // function params
			{
				move(amount = 1)
				
				val names = mutableSetOf<String>()
				val combo = mutableSetOf<String>()
				
				ignoreNewLines()
				
				var token: TokenData
				
				while (!empty)
				{
					token = next
					
					if (token.type == NEWLINE)
					{
						ignoreNewLines()
						continue
					}
					
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
						require(combo.isNotEmpty() || func.pull.isNotEmpty())
						{
							"first value must be a parameter"
						}
						
						continue
					}
					
					if (token.type == NAME)
					{
						require(names.add(token.data) && combo.add(token.data))
						{
							"parameter ${token.data} already defined for function ${func.name}"
						}
						
						continue
					}
					
					if (token.type == TYPED)
					{
						val type = parseType()
						
						for (name in combo)
						{
							func.pull[name] = type
						}
						
						combo.clear()
						continue
					}
					
					throw UnsupportedOperationException("Token out of place: $token")
				}
				
				token = next
				require(token.type == PAREN_R)
				{
					"function parameters must be surrounded in parentheses"
				}
			}
			
			if (peek?.type == TYPED) // function return
			{
				move(amount = 1)
				
				func.push["ret0"] = parseType()
			}
			
			ignoreNewLines()
			
			if (peek?.type == BRACE_L) // function body
			{
				move(amount = 1)
				
				ignoreNewLines()
				
				while (!empty)
				{
					if (peek?.type == NEWLINE)
					{
						ignoreNewLines()
						continue
					}
					
					if (peek?.type == BRACE_R)
					{
						move(amount = 1)
						break
					}
					
					if (peek?.type == RETURN)
					{
						move(amount = 1)
						
						parseShuntedExpression(body)
						break
					}
					
					parseMain(body)
				}
				
				func.pull.forEach()
				{ (name, type) ->
					
					val prop = Prop(name, false)
					prop.type = type
					
					body.add(0, CommandPropertyAssign(name))
					body.add(0, CommandPropertyDefine(prop, depth = 1))
				}
			}
			
			if (body.isNotEmpty())
			{
				func.body = Route.of(body)
			}
			
			trait.addFunc(func)
		}
		
		
		ignoreNewLines()
		
		
		require(peek?.type == PAREN_L || peek?.type == BRACE_L)
		{
			"trait missing props or body $peek"
		}
		
		
		if (peek?.type == PAREN_L) // parse props
		{
			move(amount = 1)
			
			ignoreNewLines()
			
			var token: TokenData
			
			while (!empty)
			{
				token = next
				
				if (token.type == PAREN_R)
				{
					require(trait.props.isNotEmpty())
					{
						"trait parentheses must contain properties"
					}
					
					break
				}
				
				if (token.type == COMMA)
				{
					require(trait.props.isNotEmpty())
					{
						"first value must be a property"
					}
					
					continue
				}
				
				require(token.type == WORD)
				{
					"token out of place $token, must be word"
				}
				
				val word = requireNotNull(Word.find(token.data))
				{
					"word ${token.data} not found $token"
				}
				
				require(word == Word.VAR || word == Word.VAL)
				{
					"word out of place $token, must be var or val"
				}
				
				parseTraitProp(word == Word.VAR)
				
				ignoreNewLines()
			}
			
			ignoreNewLines()
		}
		
		if (peek?.type == BRACE_L) // parse body
		{
			move(amount = 1)
			
			ignoreNewLines()
			
			var token: TokenData
			
			while (!empty)
			{
				token = next
				
				if (token.type == NEWLINE)
				{
					continue
				}
				
				if (token.type == BRACE_R)
				{
					break
				}
				
				require(token.type == WORD)
				{
					"token out of place $token, must be word"
				}
				
				val word = requireNotNull(Word.find(token.data))
				{
					"word ${token.data} not found $token"
				}
				
				require(word == Word.FUN || word == Word.VAR || word == Word.VAL)
				{
					"word out of place $token, must be fun, var, or val"
				}
				
				if (word == Word.FUN)
				{
					parseTraitFunc()
				}
				else
				{
					parseTraitProp(word == Word.VAR)
				}
				
				ignoreNewLines()
			}
		}
	}
	
	private fun IterOfTokens.parseClazz(cmds: CommandChain)
	{
		require(peek?.type == TYPE)
		{
			"class missing name $peek"
		}
		
		
		val clazz = Clazz(next.data)
		cmds += CommandClazzDefine(clazz)
		
		
		fun parseClassProp(mutable: Boolean)
		{
			require(peek?.type == NAME)
			{
				"property missing name $peek"
			}
			
			val prop = Prop(next.data, mutable)
			
			require(peek?.type == TYPED)
			{
				"property missing type assignment $peek"
			}
			
			move(amount = 1)
			
			prop.type = parseType()
			
			clazz.addProp(prop)
		}
		
		fun parseClassFunc()
		{
			require(peek?.type == NAME)
			{
				"function missing name $peek"
			}
			
			val func = Func(next.data)
			val body = mutableListOf<Command>()
			
			if (peek?.type == PAREN_L) // function params
			{
				move(amount = 1)
				
				val names = mutableSetOf<String>()
				val combo = mutableSetOf<String>()
				
				ignoreNewLines()
				
				var token: TokenData
				
				while (!empty)
				{
					token = next
					
					if (token.type == NEWLINE)
					{
						ignoreNewLines()
						continue
					}
					
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
						require(combo.isNotEmpty() || func.pull.isNotEmpty())
						{
							"first value must be a parameter"
						}
						
						continue
					}
					
					if (token.type == NAME)
					{
						require(names.add(token.data) && combo.add(token.data))
						{
							"parameter ${token.data} already defined for function ${func.name}"
						}
						
						continue
					}
					
					if (token.type == TYPED)
					{
						val type = parseType()
						
						for (name in combo)
						{
							func.pull[name] = type
						}
						
						combo.clear()
						continue
					}
					
					throw UnsupportedOperationException("Token out of place: $token")
				}
				
				token = next
				require(token.type == PAREN_R)
				{
					"function parameters must be surrounded in parentheses"
				}
				
				func.pull.forEach()
				{ (name, type) ->
					
					val prop = Prop(name, false)
					prop.type = type
					
					body += CommandPropertyDefine(prop, depth = 1)
					body += CommandPropertyAssign(name)
				}
			}
			
			if (peek?.type == TYPED) // function return
			{
				move(amount = 1)
				
				func.push["ret0"] = parseType()
			}
			
			ignoreNewLines()
			
			require(peek?.type == BRACE_L)
			{
				"class functions must have implementations"
			}
			
			move(amount = 1)
			
			ignoreNewLines()
			
			while (!empty)
			{
				if (peek?.type == NEWLINE)
				{
					ignoreNewLines()
					continue
				}
				
				if (peek?.type == BRACE_R)
				{
					move(amount = 1)
					break
				}
				
				if (peek?.type == RETURN)
				{
					move(amount = 1)
					
					parseShuntedExpression(body)
					break
				}
				
				parseMain(body)
			}
			
			func.body = Route.of(body)
			
			clazz.addFunc(func)
		}
		
		ignoreNewLines()
		
		require(peek?.type == PAREN_L || peek?.type == BOUND || peek?.type == BRACE_L)
		{
			"class missing args, bounds, or body $peek"
		}
		
		if (peek?.type == PAREN_L) // parse props
		{
			move(amount = 1)
			
			ignoreNewLines()
			
			var token: TokenData
			
			while (!empty)
			{
				token = next
				
				if (token.type == PAREN_R)
				{
					require(clazz.props.isNotEmpty())
					{
						"class parentheses must contain properties"
					}
					
					break
				}
				
				if (token.type == COMMA)
				{
					require(clazz.props.isNotEmpty())
					{
						"first value must be a property"
					}
					
					continue
				}
				
				require(token.type == WORD)
				{
					"token out of place $token, must be word"
				}
				
				val word = requireNotNull(Word.find(token.data))
				{
					"word ${token.data} not found $token"
				}
				
				require(word == Word.VAR || word == Word.VAL)
				{
					"word out of place $token, must be var or val"
				}
				
				parseClassProp(word == Word.VAR)
				
				ignoreNewLines()
			}
			
			ignoreNewLines()
		}
		
		if (peek?.type == BOUND) // parse class bounds
		{
			move(amount = 1)
			
			clazz.supes += parseBound().filterIsInstance<Basic>().map(Basic::name)
			
			ignoreNewLines()
		}
		
		if (peek?.type == BRACE_L) // parse body
		{
			move(amount = 1)
			
			ignoreNewLines()
			
			val funcs = mutableListOf<Route>()
			val props = mutableListOf<Route>()
			
			var token: TokenData
			
			while (!empty)
			{
				token = next
				
				if (token.type == NEWLINE)
				{
					continue
				}
				
				if (token.type == BRACE_R)
				{
					break
				}
				
				require(token.type == WORD)
				{
					"token out of place $token, must be word"
				}
				
				val word = requireNotNull(Word.find(token.data))
				{
					"word ${token.data} not found $token"
				}
				
				require(word == Word.FUN || word == Word.VAR || word == Word.VAL)
				{
					"word out of place $token, must be fun, var, or val"
				}
				
				val route = mutableListOf<Command>()
				
				if (word == Word.FUN)
				{
					clazz.addFunc(parseFunc(route))
				}
				else
				{
					clazz.addProp(parseProp(route, word == Word.VAR))
				}
				
				route.removeIf { it is CommandPropertyDefine || it is CommandFunctionDefine }
				
				(if (word == Word.FUN) funcs else props) += Route.of(route)
				
				ignoreNewLines()
			}
			
			val route = (funcs.map(::CommandRoute) + props.map(::CommandRoute)).toMutableList()
			route.removeIf { it.route.command == CommandNone }
			
			clazz.route = Route.of(route)
		}
	}
	
	private fun IterOfTokens.parseShuntedExpression(cmds: CommandChain)
	{
		val expr = parseExpr()
		
		cmds += shuntingYard(expr)
	}
	
	private fun IterOfTokens.parseExpr(breakOnComma: Boolean = false, breakOnBraceL: Boolean = false): List<Command>
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
			if (token.type == BRACE_L && breakOnBraceL)
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
				BIT        ->
					parseLit(expr, token)
				NAME       ->
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
				POINT      ->
				{
					val temp = generateTempVariable(expr)
					
					if (peek(amount = 1)?.type == PAREN_L)
					{
						move(amount = -1)
						parseInstanceFuncCall(expr, TokenData(NAME, temp))
					}
					else
					{
						move(amount = -1)
						parseInstancePropCall(expr, TokenData(NAME, temp))
					}
				}
				TYPE       ->
					parseNew(expr, token)
				OPER       ->
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
				COMMA      ->
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
				PAREN_L    ->
				{
					open++
					expr += CommandOperate(Oper.SOS)
				}
				PAREN_R    ->
				{
					if (open == 0)
					{
						move(amount = -1)
						break@loop
					}
					
					open--
					expr += CommandOperate(Oper.EOS)
				}
				BRACK_L    ->
				{
					move(amount = -1)
					parseInd(expr)
				}
				WORD       ->
				{
					when (Word.find(token.data))
					{
						Word.CAST ->
							parseCast(expr)
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
				else       ->
				{
					throw UnsupportedOperationException("token out of place $token")
				}
			}
		}
		
		return expr
	}
	
	private fun IterOfTokens.parseType(): Types
	{
		var tuple = false
		
		fun parseBasicType(): Types
		{
			
			fun parseTupleTypeShortHand(basic: Basic): Types
			{
				check(tuple)
				{
					"shorthand tuple syntax must be surrounded by parens"
				}
				
				move(amount = 1)
				
				val types = mutableListOf<Types>()
				
				val value = resolveValue(next)
				require(value.first == "Int")
				{
					"shorthand tuple syntax must have a whole number"
				}
				
				repeat((value.second as Int))
				{
					types += basic
				}
				
				
				return Tuple(types)
			}
			
			val next = next
			
			require(next.type == TYPE)
			{
				"basic type must be a type"
			}
			
			val basic = Basic(next.data)
			
			if (peek?.type != BOUND)
			{
				return basic
			}
			
			return parseTupleTypeShortHand(basic)
		}
		
		fun parseTupleType(): Types
		{
			val types = mutableListOf<Types>()
			
			require(next.type == PAREN_L)
			{
				"tuple type must begin with paren l"
			}
			
			while (!empty)
			{
				val next = peek ?: break
				
				if (next.type == TYPE)
				{
					tuple = true
					val basic = parseBasicType()
					tuple = false
					
					if (basic is Tuple)
					{
						types += basic.part
					}
					else
					{
						types += basic
					}
					
					continue
				}
				
				if (next.type == PAREN_L)
				{
					types += parseTupleType()
					continue
				}
				
				if (next.type == COMMA)
				{
					require(types.isNotEmpty())
					{
						"first value must be a type"
					}
					
					move(amount = 1) // skip comma
					continue
				}
				
				if (next.type == PAREN_R)
				{
					require(types.isNotEmpty())
					{
						"tuple must contain types"
					}
					
					move(amount = 1) // skip paren r
					break
				}
			}
			
			
			return Tuple(types)
		}
		
		
		return when (peek?.type)
		{
			TYPE    -> parseBasicType()
			PAREN_L -> parseTupleType()
			else    ->
			{
				throw IllegalStateException("token out of place: $peek")
			}
		}
	}
	
	private fun IterOfTokens.parseTypeQuery(cmds: CommandChain)
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
	
	private fun IterOfTokens.parseLit(cmds: CommandChain, token: TokenData)
	{
		val value = resolveValue(token)
		
		cmds += CommandLiteral(value.first, value.second)
	}
	
	private fun IterOfTokens.parseRef(cmds: CommandChain, token: TokenData)
	{
		when (peek?.type)
		{
			PAREN_L -> // function call
			{
				parseFuncCall(cmds, token)
			}
			POINT   -> // instance call
			{
				var target = token
				
				while (peek?.type == POINT)
				{
					if (peek(amount = 2)?.type == PAREN_L)
					{
						parseInstanceFuncCall(cmds, target)
					}
					else
					{
						parseInstancePropCall(cmds, target)
					}
					
					if (peek?.type == POINT)
					{
						target = TokenData(NAME, generateTempVariable(cmds))
					}
				}
			}
			else    ->
			{
				parsePropCall(cmds, token)
			}
		}
	}
	
	private fun IterOfTokens.parseNew(cmds: CommandChain, data: TokenData)
	{
		ignoreNewLines()
		
		var token: TokenData
		
		token = next
		require(token.type == BRACE_L)
		{
			"type must be followed by brace l"
		}
		
		ignoreNewLines()
		
		val init = mutableListOf<Command>()
		
		while (!empty)
		{
			token = next
			
			if (token.type == NEWLINE)
			{
				ignoreNewLines()
				continue
			}
			if (token.type == COMMA)
			{
				require(init.isNotEmpty())
				{
					"comma out of position"
				}
				
				continue
			}
			if (token.type == BRACE_R)
			{
				break
			}
			
			if (token.type == NAME)
			{
				val name = token.data
				
				token = next
				require(token.type == TYPED)
				{
					"new class properties assign with :"
				}
				
				val expr = mutableListOf<Command>()
				expr += parseExpr(breakOnComma = true)
				
				init += CommandRoute(Route.of(expr))
				init += CommandPropertyAssign(name)
				continue
			}
			
			throw IllegalStateException("token out of position: $token")
		}
		
		cmds += CommandClazzCreate(data.data, Route.of(init))
	}
	
	private fun IterOfTokens.parseTup(funcParams: Boolean = false): List<List<Command>>
	{
		val cmds = mutableListOf<List<Command>>()
		
		require(peek?.type == PAREN_L)
		{
			"tuple must start with paren l"
		}
		
		move(amount = 1)
		
		while (!empty)
		{
			if (peek?.type == COMMA)
			{
				require(cmds.isNotEmpty())
				{
					"first value required before comma"
				}
				
				move(amount = 1)
				
				continue
			}
			
			if (peek?.type == PAREN_R)
			{
				require(funcParams || cmds.isNotEmpty())
				{
					"tuple must contain data"
				}
				
				break
			}
			
			if (peek?.type == PAREN_L)
			{
				val marker = peek
				val nested = parseTup()
				
				val values = nested.flatten()
				
				cmds += if (marker?.data == "_")
				{
					values
				}
				else
				{
					values + CommandTuple(nested.size)
				}
				
				continue
			}
			
			cmds += shuntingYard(parseExpr(breakOnComma = true))
		}
		
		require(peek?.type == PAREN_R)
		{
			"tuple must end with paren r"
		}
		
		move(amount = 1)
		
		return cmds.reversed()
	}
	
	private fun IterOfTokens.parseInd(cmds: CommandChain)
	{
		require(peek?.type == BRACK_L)
		{
			"index access requires brack l $peek"
		}
		
		move(amount = 1)
		
		val index = mutableListOf<Command>()
		parseShuntedExpression(index)
		
		require(peek?.type == BRACK_R)
		{
			"index access requires brack r $peek"
		}
		
		move(amount = 1)
		
		if (peek?.type != ASSIGN)
		{
			cmds += CommandGet(Route.of(index))
			return
		}
		
		move(amount = 1)
		
		val value = mutableListOf<Command>()
		parseShuntedExpression(value)
		
		cmds += CommandSet(Route.of(index), Route.of(value))
	}
	
	
	private fun IterOfTokens.parseFuncCall(cmds: CommandChain, data: TokenData)
	{
		parseTup(funcParams = true).forEach()
		{ expr ->
			cmds += CommandRoute(Route.of(expr))
		}
		
		cmds += CommandFunctionAccess(data.data)
	}
	
	private fun IterOfTokens.parseInstanceFuncCall(cmds: CommandChain, data: TokenData)
	{
		require(peek?.type == POINT)
		{
			"instance function call must be with a point $peek"
		}
		
		move(amount = 1)
		
		require(peek?.type == WORD || peek?.type == NAME || peek?.type == TYPE)
		{
			"instance function call must be one of [WORD, NAME, TYPE] $peek"
		}
		
		val name = next.data
		
		val args = parseTup(funcParams = true)
		
		args.forEach()
		{ expr ->
			cmds += CommandRoute(Route.of(expr))
		}
		
		cmds += CommandPropertyAccess(data.data)
		cmds += CommandInstanceFunctionAccess(name, args.size)
	}
	
	private fun IterOfTokens.parsePropCall(cmds: CommandChain, data: TokenData)
	{
		cmds += CommandPropertyAccess(data.data)
	}
	
	private fun IterOfTokens.parseInstancePropCall(cmds: CommandChain, data: TokenData)
	{
		require(peek?.type == POINT)
		{
			"instance property call must be with a point $peek"
		}
		
		move(amount = 1)
		
		require(peek?.type == WORD || peek?.type == NAME || peek?.type == TYPE)
		{
			"instance property call must be one of [WORD, NAME, TYPE] $peek"
		}
		
		cmds += CommandPropertyAccess(data.data)
		cmds += CommandInstancePropertyAccess(next.data)
	}
	
	
	private fun resolveValue(token: TokenData): Pair<String, Any>
	{
		val data = when (token.type)
		{
			NUM  ->
			{
				if ('.' !in token.data)
					token.data.toInt()
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
			is Int    ->
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
	
	private fun generateTempVariable(cmds: CommandChain): String
	{
		val temp = "temp${UUID.randomUUID()}"
		
		cmds += CommandPropertyDefine(Prop(temp, mutable = false))
		cmds += CommandPropertyAssign(temp)
		cmds += CommandPropertyAccess(temp)
		
		return temp
	}
	
	
	private fun IterOfTokens.ignoreNewLines()
	{
		while (peek?.type == NEWLINE)
		{
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