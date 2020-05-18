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
import java.util.Scanner
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

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
		
		when(val value = stack.pull())
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
						"expected type ${property.type}, got type ${value.type.back}"
					}
				}
				
				property.data = value
			}
			else ->
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
			"property $name has not been defined"
		}
		
		val value = requireNotNull(property.data)
		{
			"property $name has not been assigned"
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

data class CommandConsolePush(val newline: Boolean)
	: Command()
{
	
	override fun eval(stack: Stack, context: Context)
	{
		var value = stack.pull()
		if (value is Value)
		{
			value = value.toPushString()
		}
		
		if (!newline)
		{
			print(value)
		}
		else
		{
			println(value)
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
		
		index = requireNotNull((index as? Long)?.toInt())
		{
			"index value must be a long"
		}
		
		var access = stack.pull()
		if (access is Value)
		{
			access = access.data
		}
		
		val data = when(access)
		{
			is Map<*, *> ->
			{
				access[index]
			}
			is List<*> ->
			{
				access[index]
			}
			is CharSequence ->
			{
				access[index]
			}
			else ->
			{
				throw UnsupportedOperationException("invalid accessor")
			}
		}
		
		val type = when(data)
		{
			is Value ->
			{
				data.type
			}
			else ->
			{
				Type.ALL
			}
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

data class CommandRoute(val route: Route)
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

data class CommandLoop(val expr: Route, val body: Route)
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
			if (result is None)
			{
				if (result.info is CommandStop.StopException)
				{
					context.quitScope()
					break
				}
				
				context.quitScope()
				throw result.info
			}
			
			context.quitScope()
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
		
		//println("found function: $func")
		//println("stack: $stack")
		// println("body: ${func.body}")
		
		val pull = func.pull
		val push = func.push
		val body = func.body
		
		val funcStack = Stack()
		
		for (entry in pull)
		{
			//println("pulling: ${entry.key}")
			funcStack.push(stack.pull())
			//println("stack: $stack")
		}
		
		//println("func stack0: $funcStack")
		
		
		val result = if (body == null)
		{
			null
		}
		else
		{
			Odin.eval(context, body, funcStack)
		}
		
		//println("func stack1: $funcStack")
		
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

data class CommandInstanceFunctionAccess(val name: String, val size: Int)
	: Command()
{
	@ExperimentalTime
	override fun eval(stack: Stack, context: Context)
	{
		var type = stack.pull()
		if (type is Value)
		{
			type = type.data
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
		
		// println("looking for $name")
		
		val (pair, findTime) = measureTimedValue()
		{
			requireNotNull(resolveMethodCall(type::class, name, args))
			{
				"could not resolve method $name"
			}
		}
		//println("Find Time: $findType")
		
		val (method, params) = pair
		method.isAccessible = true
		
		val (result, callTime) = measureTimedValue()
		{
			method.call(type, *params.toTypedArray())
		}
		//println("Call Time: $callTime")
		
		stack.push(Value(Type.ALL, result ?: Unit))
	}
	
	
	private fun resolveMethodCall(type: KClass<*>, name: String, args: List<Any>): Pair<KCallable<*>, List<Any>>?
	{
		//println(System.currentTimeMillis())
		var funcs = type.members.filter { it.name == name }
		if (funcs.isEmpty())
		{
			println("no named")
			return null
		}
		
		// println(funcs)
		
		//println(System.currentTimeMillis())
		funcs = funcs.filter { it.valueParameters.size == args.size }
		if (funcs.isEmpty())
		{
			// println("no sized")
			return null
		}
		
		if (funcs.size == 1 && args.isEmpty())
		{
			return funcs.single() to args
		}
		
		//println(System.currentTimeMillis())
		funcs.forEach()
		match@ { method ->
			val prms = method.valueParameters.map { it.type.jvmErasure }
			val args = args.toMutableList()
			
			prms.indices.forEach()
			{ index ->
				
				val thisType = args[index]::class
				val thatType = prms[index]
				
				if (thisType != thatType)
				{
					if (thisType == Long::class && thatType == Int::class)
					{
						args[index] = (args[index] as Long).toInt()
					}
				}
			}
			
			prms.indices.forEach()
			{ index ->
				val thisType = args[index]::class
				val thatType = prms[index]
				
				if (thisType != thatType && thatType != Any::class)
				{
					return@match
				}
			}
			
			return method to args
		}
		
		return null
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

