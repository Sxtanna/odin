package com.sxtanna.odin.runtime

import com.sxtanna.odin.Odin
import com.sxtanna.odin.compile.data.Oper
import com.sxtanna.odin.results.None
import com.sxtanna.odin.results.Some
import com.sxtanna.odin.runtime.base.Basic
import com.sxtanna.odin.runtime.base.Clazz
import com.sxtanna.odin.runtime.base.Route
import com.sxtanna.odin.runtime.base.Scope
import com.sxtanna.odin.runtime.base.Stack
import com.sxtanna.odin.runtime.base.Trait
import com.sxtanna.odin.runtime.base.Tuple
import com.sxtanna.odin.runtime.base.Types
import com.sxtanna.odin.runtime.base.Value
import com.sxtanna.odin.runtime.data.Func
import com.sxtanna.odin.runtime.data.Prop
import com.sxtanna.odin.runtime.data.Type
import java.lang.reflect.Method
import java.util.Scanner

sealed class Command
{
	abstract fun eval(stack: Stack, context: Context)
	
	
	override fun toString(): String
	{
		return this::class.simpleName ?: "Unknown"
	}
}

object CommandMain : Command()
{
	override fun eval(stack: Stack, context: Context) = Unit
}

object CommandDone : Command()
{
	override fun eval(stack: Stack, context: Context) = Unit
}

object CommandStop : Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		throw StopException
	}
	
	object StopException : Exception()
}

object CommandHead : Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		stack.push(this)
	}
}

object CommandTail : Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		val data = mutableListOf<Any>()
		
		while (true)
		{
			var value = stack.pull()
			if (value is CommandHead)
			{
				break
			}
			
			if (value is Value)
			{
				value = value.data
			}
			
			data += value
		}
		
		stack.push(data)
	}
}

data class CommandRedo(val count: Int)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		stack.push(this)
	}
}

data class CommandPropertyDefine(val prop: Prop, val depth: Int = -1)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		require(context.findProp(prop.name, depth) == null)
		{
			"property ${prop.name} already defined in scope ${context.scope[0].name}"
		}
		
		context.defineProp(prop)
	}
}

data class CommandPropertyAssign(val name: String)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		val property = requireNotNull(context.findProp(name))
		{
			"property $name has not been defined"
		}
		
		when (val value = stack.pull())
		{
			is Value ->
			{
				if (property.type == Types.none())
				{
					property.type = value.type.back
				}
				else
				{
					require(property.type.matches(value.type))
					{
						"property ${property.name} expected type ${property.type}, got type ${value.type.back}"
					}
				}
				
				property.data = value
			}
			else     ->
			{
				throw UnsupportedOperationException("Cannot assign $property a value without a type")
			}
		}
	}
}

data class CommandPropertyResets(val name: String)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		val property = requireNotNull(context.findProp(name))
		{
			"property $name has not been defined"
		}
		
		property.data = null
	}
}

data class CommandPropertyAccess(val name: String)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		val property = requireNotNull(context.findProp(name))
		{
			"property '$name' has not been defined"
		}
		
		val value = requireNotNull(property.data)
		{
			"property '$name' has not been assigned"
		}
		
		stack.push(value)
	}
}

data class CommandLiteral(val type: String, val data: Any)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		val type = requireNotNull(context.findType(type))
		{
			"could not resolve type $type"
		}
		
		stack.push(Value(type, data))
	}
}

data class CommandOperate(val oper: Oper)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		oper.eval(stack)
	}
}

data class CommandTraitDefine(val trait: Trait)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		context.defineType(Type(trait.name, trait))
	}
}

data class CommandClazzDefine(val clazz: Clazz)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		context.defineType(Type(clazz.name, clazz))
	}
}

data class CommandTraitCreate(val traitName: String)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		TODO("not implemented")
	}
}

data class CommandClazzCreate(val clazzName: String)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		val type = requireNotNull(context.findType(clazzName))
		{
			"class $clazzName undefined in scope"
		}
		
		val clazz = (type.back as Clazz).copy()
		
		repeat(clazz.types.size)
		{
			val trait = stack.peek()
		}
	}
}

data class CommandStackPush(var expr: Route)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		val value = Odin.eval(context, expr, stack)
		if (value is None)
		{
			throw value.info
		}
	}
}

data class CommandStackPull(val pull: Boolean)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		requireNotNull(stack.peek())
		{
			"stack is empty"
		}
		
		if (pull)
		{
			stack.pull()
		}
	}
}

data class CommandConsolePush(val newline: Boolean, val returnToStack: Boolean = false)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		val value = stack.pull()
		
		var output = value
		if (output is Value)
		{
			output = output.toPushString()
		}
		
		if (!newline)
		{
			print(output)
		}
		else
		{
			println(output)
		}
		
		if (returnToStack)
		{
			stack.push(value)
		}
	}
}

data class CommandConsolePull(val type: Types, val prompt: String?)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		if (prompt != null)
		{
			println(prompt)
		}
		
		val scan = Scanner(System.`in`)
		
		var data = null as? Any?
		
		while (data == null)
		{
			data = try
			{
				when (type)
				{
					Type.TXT.back -> scan.nextLine()
					Type.INT.back -> scan.nextLong()
					Type.DEC.back -> scan.nextDouble()
					Type.BIT.back -> scan.nextBoolean()
					Type.NIL.back -> scan.nextLine()
					else          ->
					{
						throw UnsupportedOperationException("unable to pull value of type: $type")
					}
				}
			}
			catch (ex: Exception)
			{
				println("Invalid value for ${type.name}")
				scan.nextLine()
				null
			}
		}
		
		stack.push(Value(context.findType(type.name) ?: Type.ALL, data))
	}
}

data class CommandWhen(val expr: Route, val conditionPass: Route, val conditionFail: Route?)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		// evaluate condition
		val evalExpr = Odin.eval(context, expr, stack)
		if (evalExpr is None)
		{
			throw evalExpr.info
		}
		
		// resolve result of condition
		var pass = stack.pull()
		if (pass is Value)
		{
			pass = pass.data
		}
		
		pass = requireNotNull(pass as? Boolean)
		{
			"condition result was not a bit"
		}
		
		// evaluate branches of conditions
		val result = when
		{
			pass                  ->
			{
				Odin.eval(context, conditionPass, stack)
			}
			conditionFail != null ->
			{
				Odin.eval(context, conditionFail, stack)
			}
			else                  ->
			{
				null
			}
		}
		
		if (result != null && result is None)
		{
			throw result.info
		}
	}
}

data class CommandCase(val expr: Route, val conditions: Map<Route, Route>)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		// evaluate expression
		val evalExpr = Odin.eval(context, expr, stack)
		if (evalExpr is None)
		{
			throw evalExpr.info
		}
		
		// resolve result of expression
		val target = stack.pull()
		
		for ((cond, case) in conditions)
		{
			val evalCond = Odin.eval(context, cond, stack)
			if (evalCond is None)
			{
				throw evalCond.info
			}
			
			val value = stack.pull()
			
			if (target != value)
			{
				continue
			}
			
			val evalCase = Odin.eval(context, case, stack)
			if (evalCase is None)
			{
				throw evalCase.info
			}
			
			break
		}
	}
}

data class CommandTuple(val size: Int)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		val data = (1..size).map { stack.pull() }.filterIsInstance<Value>()
		require(data.size == size)
		{
			"tuple could not be created: size mismatch"
		}
		
		val tuple = Tuple(data.map { it.type.back })
		
		val type = context.findType(tuple.name) ?: Type(tuple.name, tuple)
		context.defineType(type)
		
		stack.push(Value(type, data))
	}
}

data class CommandTypeQuery(val expr: Route)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		val evalExpr = Odin.eval(context, expr, stack)
		if (evalExpr is None)
		{
			throw evalExpr.info
		}
		
		val value = stack.pull()
		if (value !is Value)
		{
			throw UnsupportedOperationException("Result of expression does not hold a type!")
		}
		
		stack.push(Value(Type.TXT, value.type.name))
	}
}

data class CommandGet(val indexExpr: Route)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		val indexExpr = Odin.eval(context, indexExpr, stack)
		if (indexExpr is None)
		{
			throw indexExpr.info
		}
		
		var index = stack.pull()
		if (index is Value)
		{
			index = index.data
		}
		
		var access = stack.pull()
		if (access is Value)
		{
			access = access.data
		}
		
		val data = when (access)
		{
			is Map<*, *>    ->
			{
				access[index]
			}
			is List<*>      ->
			{
				access[index.toString().toInt()]
			}
			is CharSequence ->
			{
				access[index.toString().toInt()]
			}
			else            ->
			{
				throw UnsupportedOperationException("invalid accessor")
			}
		}
		
		val type = when (data)
		{
			is Value ->
			{
				data.type
			}
			is Long -> Type.INT
			is Double -> Type.DEC
			is String -> Type.TXT
			is Char -> Type.LET
			is Boolean -> Type.BIT
			else -> Type.ALL
		}
		
		stack.push(Value(type, data ?: Unit))
	}
}

data class CommandSet(val index: Route)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
	
	}
}

data class CommandRoute(var route: Route)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		// println("route0: $stack")
		
		val result = Odin.eval(context, route, stack)
		if (result is None)
		{
			throw result.info
		}
		
		// println("route1: $stack")
	}
}

data class CommandLoop(val expr: Route, var body: Route)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		while (true)
		{
			val condition = Odin.eval(context, expr, stack)
			if (condition is None)
			{
				if (condition.info is CommandStop.StopException)
				{
					break
				}
				
				throw condition.info
			}
			
			var pass = stack.pull()
			if (pass is Value)
			{
				pass = pass.data
			}
			
			pass = requireNotNull(pass as? Boolean)
			{
				"condition result was not a bit"
			}
			
			if (!pass)
			{
				break
			}
			
			context.joinScope(Scope("loop"))
			
			val result = Odin.eval(context, body, stack)
			
			context.quitScope()
			
			if (result is None)
			{
				if (result.info is CommandStop.StopException)
				{
					break
				}
				
				throw result.info
			}
		}
	}
}

data class CommandFunctionDefine(val func: Func)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		context.defineFunc(func)
	}
}

data class CommandFunctionAccess(val name: String)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		val func = requireNotNull(context.findFunc(name))
		{
			"function $name not defined"
		}
		
		context.joinScope(Scope(func.name))
		
		val pull = func.pull
		val push = func.push
		val body = func.body
		
		var funcStack = Stack()
		
		for (entry in pull)
		{
			funcStack.push(stack.pull())
		}
		
		funcStack = funcStack.flip()
		
		val result = if (body == null)
		{
			null
		}
		else
		{
			Odin.eval(context, body, funcStack)
		}
		
		if (result != null && result is Some)
		{
			for (entry in push)
			{
				stack.push(funcStack.pull())
			}
		}
		
		context.quitScope()
		
		if (result is None)
		{
			throw result.info
		}
	}
}

data class CommandInstanceDefine(val name: String)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
	
	}
}

data class CommandInstanceFunctionAccess(val name: String, val size: Int)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		var receiver = stack.pull()
		if (receiver is Value)
		{
			receiver = receiver.data
		}
		
		val args = mutableListOf<Any>()
		repeat(size)
		{
			var data = stack.pull()
			if (data is Value)
			{
				data = data.data
			}
			
			args += data
		}
		
		val (method, params) = requireNotNull(resolveMethodCallJ(receiver.javaClass, name, args))
		{
			"could not resolve method $name"
		}
		
		method.isAccessible = true
		
		var result = method.invoke(receiver, *params)
		
		when (result)
		{
			is Int   ->
			{
				result = result.toLong()
			}
			is Float ->
			{
				result = result.toDouble()
			}
		}
		
		val type = when (result)
		{
			is Long -> Type.INT
			is Double -> Type.DEC
			is String -> Type.TXT
			is Char -> Type.LET
			is Boolean -> Type.BIT
			else -> Type.ALL
		}
		
		stack.push(Value(type, result ?: Unit))
	}
	
	private fun resolveMethodCallJ(type: Class<*>, name: String, args: List<Any>): Pair<Method, Array<Any>>?
	{
		var funcs = type.declaredMethods.filter { it.name == name }
		if (funcs.isEmpty())
		{
			return null
		}
		
		funcs = funcs.filter { it.parameterCount == args.size }
		if (funcs.isEmpty())
		{
			return null
		}
		
		if (funcs.size == 1)
		{
			val func = funcs.single()
			
			if (func.parameterCount == 0 && args.isEmpty())
			{
				return func to args.toTypedArray()
			}
			
			return resolveMatching(func, args)
		}
		
		for (func in funcs)
		{
			return resolveMatching(func, args) ?: continue
		}
		
		return null
	}
	
	private fun resolveMatching(func: Method, args: List<Any>): Pair<Method, Array<Any>>?
	{
		val meth = func.parameterTypes
		val pass = args.toTypedArray()
		
		for (index in meth.indices)
		{
			val methType = meth[index]
			
			val passData = pass[index]
			val passType = passData.javaClass
			
			if (methType != passType)
			{
				if (passData !is Number)
				{
					return null
				}
				
				pass[index] = when (methType)
				{
					Any::class.java    -> passData
					Byte::class.java   -> passData.toByte()
					Short::class.java  -> passData.toShort()
					Int::class.java    -> passData.toInt()
					Long::class.java   -> passData.toLong()
					Float::class.java  -> passData.toFloat()
					Double::class.java -> passData.toDouble()
					else               ->
					{
						return null
					}
				}
			}
		}
		
		return func to pass
	}
}

data class CommandJavaTypeDefine(val clazz: Class<*>)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		stack.push(Value(Type(clazz.name, Basic(clazz.name)), clazz.getDeclaredConstructor().newInstance()))
	}
}

