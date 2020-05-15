package com.sxtanna.odin.runtime

import com.sxtanna.odin.Odin
import com.sxtanna.odin.compile.data.Oper
import com.sxtanna.odin.results.None
import com.sxtanna.odin.runtime.base.Clazz
import com.sxtanna.odin.runtime.base.Route
import com.sxtanna.odin.runtime.base.Stack
import com.sxtanna.odin.runtime.base.Trait
import com.sxtanna.odin.runtime.base.Tuple
import com.sxtanna.odin.runtime.base.Types
import com.sxtanna.odin.runtime.base.Value
import com.sxtanna.odin.runtime.data.Prop
import com.sxtanna.odin.runtime.data.Type
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

data class CommandRedo(val count: Int)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		stack.push(this)
	}
}

data class CommandPropertyDefine(val prop: Prop)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		require(context.scope.props[prop.name] == null)
		{
			"property ${prop.name} already defined"
		}
		
		context.scope.props[prop.name] = prop
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
		context.scope.types[trait.name] = Type(trait.name, trait)
	}
}

data class CommandClazzDefine(val clazz: Clazz)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		context.scope.types[clazz.name] = Type(clazz.name, clazz)
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

data class CommandConditional(val expr: Route, val conditionPass: Route, val conditionFail: Route?)
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
		if (pass)
		{
			Odin.eval(context, conditionPass, stack)
		}
		else if (conditionFail != null)
		{
			Odin.eval(context, conditionFail, stack)
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
		context.scope.types[tuple.name] = type
		
		
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
		val result = Odin.eval(context, route, stack)
		if (result is None)
		{
			throw result.info
		}
	}
}