package com.sxtanna.odin.runtime

import com.sxtanna.odin.Odin
import com.sxtanna.odin.compile.data.Oper
import com.sxtanna.odin.runtime.base.Basic
import com.sxtanna.odin.runtime.base.Clazz
import com.sxtanna.odin.runtime.base.Route
import com.sxtanna.odin.runtime.base.Scope
import com.sxtanna.odin.runtime.base.Stack
import com.sxtanna.odin.runtime.base.Trait
import com.sxtanna.odin.runtime.base.Tuple
import com.sxtanna.odin.runtime.base.Types
import com.sxtanna.odin.runtime.base.Value
import com.sxtanna.odin.runtime.base.Wraps
import com.sxtanna.odin.runtime.data.Func
import com.sxtanna.odin.runtime.data.Inst
import com.sxtanna.odin.runtime.data.Prop
import com.sxtanna.odin.runtime.data.Type
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Field
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

object CommandNone : Command()
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
			
			while (value is Value)
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
			"property ${prop.name}|${depth} already defined in scope ${context.scopes[0].name}"
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
				
				if (property.type.matches(value.type))
				{
					property.data = value
					return
				}
				
				var data = value.data
				var type = value.type
				
				if (data is Number)
				{
					when (property.type)
					{
						Type.BYT.back ->
						{
							data = data.toByte()
							type = Type.BYT
						}
						Type.INT.back ->
						{
							data = data.toInt()
							type = Type.INT
						}
						Type.LNG.back ->
						{
							data = data.toLong()
							type = Type.LNG
						}
						Type.FLT.back ->
						{
							data = data.toFloat()
							type = Type.FLT
						}
						Type.DEC.back ->
						{
							data = data.toDouble()
							type = Type.DEC
						}
					}
				}
				
				require(property.type.matches(type))
				{
					"property ${property.name} expected type ${property.type}, got type ${value.type.back}"
				}
				
				property.data = Value(type, data)
			}
			else     ->
			{
				throw UnsupportedOperationException("Cannot assign $property a value without a type `$value`")
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

data class CommandClazzCreate(val clazzName: String, val init: Route)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		val instance = makeInstance(clazzName, stack, context, init)
		
		stack.push(Value(instance.type, instance))
	}
}

data class CommandStackPush(var expr: Route)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		Odin.eval(context, expr, stack)
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
					Type.BYT.back -> scan.nextByte()
					Type.INT.back -> scan.nextInt()
					Type.LNG.back -> scan.nextLong()
					Type.FLT.back -> scan.nextFloat()
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
		Odin.eval(context, expr, stack)
		
		// resolve result of condition
		var pass = stack.pull()
		while (pass is Value)
		{
			pass = pass.data
		}
		
		pass = requireNotNull(pass as? Boolean)
		{
			"condition result was not a bit"
		}
		
		// evaluate branches of conditions
		when
		{
			pass                  ->
			{
				Odin.eval(context, conditionPass, stack)
			}
			conditionFail != null ->
			{
				Odin.eval(context, conditionFail, stack)
			}
		}
	}
}

data class CommandCase(val expr: Route, val conditions: Map<Route, Route>)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		// evaluate expression
		Odin.eval(context, expr, stack)
		
		// resolve result of expression
		val target = stack.pull()
		
		for ((cond, case) in conditions)
		{
			Odin.eval(context, cond, stack)
			
			val value = stack.pull()
			
			if (target != value)
			{
				continue
			}
			
			Odin.eval(context, case, stack)
			
			break
		}
	}
}

data class CommandCast(val expr: Route, val type: Types)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		Odin.eval(context, expr, stack)
		
		var value = stack.pull()
		
		while (value is Value)
		{
			value = value.data
		}
		
		value = when (type)
		{
			is Wraps ->
			{
				type.clazz.cast(value)
			}
			is Basic ->
			{
				when (type)
				{
					Type.BYT.back -> (value as Number).toByte()
					Type.INT.back -> (value as Number).toInt()
					Type.LNG.back -> (value as Number).toLong()
					Type.FLT.back -> (value as Number).toFloat()
					Type.DEC.back -> (value as Number).toDouble()
					
					
					Type.TXT.back -> (value as? String) ?: value.toString()
					Type.LET.back -> (value as? Char) ?: (value as? Number)?.toChar() ?: value.toString().first()
					
					Type.BIT.back ->
					{
						(value as? Boolean) ?: (value as? Number)?.let { it.toDouble() > 0 } ?: false
					}
					else          ->
					{
						value
					}
				}
			}
			else     ->
			{
				value
			}
		}
		
		val type = requireNotNull(context.findType(type.name))
		
		stack.push(Value(type, value))
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
		Odin.eval(context, expr, stack)
		
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
		Odin.eval(context, indexExpr, stack)
		
		var index = stack.pull()
		while (index is Value)
		{
			index = index.data
		}
		
		var access = stack.pull()
		while (access is Value)
		{
			access = access.data
		}
		
		val data = if (access.javaClass.isArray)
		{
			val index = requireNotNull((index as? Number)?.toInt())
			{
				"array index must be an int"
			}
			
			java.lang.reflect.Array.get(access, index)
		}
		else
		{
			when (access)
			{
				is Map<*, *>    ->
				{
					access[index]
				}
				is List<*>      ->
				{
					access[(index as Number).toInt()]
				}
				is CharSequence ->
				{
					access[(index as Number).toInt()]
				}
				else            ->
				{
					throw UnsupportedOperationException("invalid accessor")
				}
			}
		}
		
		val type = when (data)
		{
			is Value   -> data.type
			is Byte    -> Type.BYT
			is Int     -> Type.INT
			is Long    -> Type.LNG
			is Float   -> Type.FLT
			is Double  -> Type.DEC
			is String  -> Type.TXT
			is Char    -> Type.LET
			is Boolean -> Type.BIT
			else       -> Type.NIL
		}
		
		stack.push(Value(type, data ?: Unit))
	}
}

data class CommandSet(val indexExpr: Route, val valueExpr: Route)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		Odin.eval(context, indexExpr, stack)
		
		var index = stack.pull()
		while (index is Value)
		{
			index = index.data
		}
		
		Odin.eval(context, valueExpr, stack)
		
		var value = stack.pull()
		while (value is Value)
		{
			value = value.data
		}
		
		var access = stack.pull()
		while (access is Value)
		{
			access = access.data
		}
		
		if (access.javaClass.isArray)
		{
			val index = requireNotNull((index as? Number)?.toInt())
			{
				"array index must be an int"
			}
			
			val value = when (access.javaClass.componentType)
			{
				Byte::class.java   -> (value as Number).toByte()
				Short::class.java  -> (value as Number).toShort()
				Int::class.java    -> (value as Number).toInt()
				Long::class.java   -> (value as Number).toLong()
				Float::class.java  -> (value as Number).toFloat()
				Double::class.java -> (value as Number).toDouble()
				else               -> value
			}
			
			java.lang.reflect.Array.set(access, index, value)
			return
		}
		
		when (access)
		{
			is MutableMap<*, *> ->
			{
				(access as MutableMap<Any, Any>)[index] = value
			}
			is MutableList<*>   ->
			{
				val index = requireNotNull((index as? Number)?.toInt())
				{
					"list index must be an int"
				}
				
				(access as MutableList<Any>)[index] = value
			}
			else                ->
			{
				throw UnsupportedOperationException("invalid accessor")
			}
		}
	}
}

data class CommandRoute(var route: Route)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		Odin.eval(context, route, stack)
	}
}

data class CommandLoop(val expr: Route, var body: Route)
	: Command()
{
	override fun eval(stack: Stack, context: Context)
	{
		while (true)
		{
			try
			{
				Odin.eval(context, expr, stack)
			}
			catch (ex: CommandStop.StopException)
			{
				break
			}
			
			var pass = stack.pull()
			while (pass is Value)
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
			
			context.enterScope(Scope("loop"))
			
			try
			{
				Odin.eval(context, body, stack)
			}
			catch (ex: CommandStop.StopException)
			{
				break
			}
			finally
			{
				context.leaveScope()
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
		
		val pull = func.pull
		val push = func.push
		val body = func.body
		
		var funcStack = Stack()
		
		for (entry in pull)
		{
			funcStack.push(stack.pull())
		}
		
		funcStack = funcStack.flip()
		
		if (body != null)
		{
			context.enterScope(Scope(func.name))
			
			Odin.eval(context, body, funcStack)
			
			for (entry in push)
			{
				stack.push(funcStack.pull())
			}
			
			context.leaveScope()
		}
	}
}

data class CommandInstanceFunctionAccess(val name: String, val size: Int)
	: Command()
{
	
	private var method: Method? = null
	
	override fun eval(stack: Stack, context: Context)
	{
		var receiver = stack.pull()
		while (receiver is Value)
		{
			receiver = receiver.data
		}
		
		val args = mutableListOf<Any>()
		repeat(size)
		{
			var data = stack.pull()
			if (receiver !is Inst)
			{
				while (data is Value)
				{
					data = data
				}
			}
			
			args += data
		}
		
		if (receiver is Inst)
		{
			callFunctionInst(receiver, args, stack)
		}
		else
		{
			callFunctionJava(receiver, args, stack)
		}
	}
	
	private fun callFunctionInst(receiver: Inst, args: List<Any>, stack: Stack)
	{
		val func = requireNotNull(receiver.findFunc(name))
		{
			"function $name not defined in ${receiver.type}"
		}
		
		val push = func.push
		val body = func.body
		
		
		var funcStack = Stack()
		
		args.forEach(funcStack::push)
		
		funcStack = funcStack.flip()
		
		receiver.enterScope(Scope(func.name))
		
		if (body != null)
		{
			Odin.eval(receiver, body, funcStack)
			
			for (entry in push)
			{
				stack.push(funcStack.pull())
			}
		}
		
		receiver.leaveScope()
	}
	
	private fun callFunctionJava(receiver: Any, args: List<Any>, stack: Stack)
	{
		val target: Method
		val params: Array<Any>
		
		val cached = this.method
		
		if (cached != null)
		{
			target = cached
			params = requireNotNull(resolveMatching(cached, args)?.second) {
				"could not resolve params"
			}
		}
		else
		{
			val (found, match) = requireNotNull(resolve(receiver.javaClass.declaredMethods.filter { it.name == name }, args))
			{
				"could not resolve method $name"
			}
			
			require(found is Method)
			{
				"resolved method was not an actual method"
			}
			
			target = found
			params = match
			
			target.isAccessible = true
			
			this.method = target
		}
		
		val result = target.invoke(receiver, *params)
		
		val type = when (result)
		{
			is Value   -> result.type
			is Byte    -> Type.BYT
			is Int     -> Type.INT
			is Long    -> Type.LNG
			is Float   -> Type.FLT
			is Double  -> Type.DEC
			is String  -> Type.TXT
			is Char    -> Type.LET
			is Boolean -> Type.BIT
			else       -> Type.ALL
		}
		
		stack.push(Value(type, result ?: Unit))
	}
}

data class CommandInstancePropertyAccess(val name: String)
	: Command()
{
	
	private var field: Field? = null
	
	override fun eval(stack: Stack, context: Context)
	{
		var receiver = stack.pull()
		while (receiver is Value)
		{
			receiver = receiver.data
		}
		
		if (receiver is Inst)
		{
			callPropertyInst(receiver, stack)
		}
		else
		{
			callPropertyJava(receiver, stack)
		}
		
	}
	
	
	private fun callPropertyInst(receiver: Inst, stack: Stack)
	{
		val prop = requireNotNull(receiver.findProp(name))
		{
			"property $name not defined in ${receiver.type}"
		}
		
		val value = requireNotNull(prop.data)
		{
			"property '$name' has not been assigned"
		}
		
		stack.push(value)
	}
	
	private fun callPropertyJava(receiver: Any, stack: Stack)
	{
		if (receiver.javaClass.isArray)
		{
			require(name == "length")
			{
				"array classes only define a length property"
			}
			
			return stack.push(Value(Type.INT, java.lang.reflect.Array.getLength(receiver).toLong()))
		}
		
		val target: Field
		
		val cached = this.field
		
		if (cached != null)
		{
			target = cached
		}
		else
		{
			val found = receiver.javaClass.getDeclaredField(name)
			found.isAccessible = true
			
			this.field = found
			
			target = found
		}
		
		val result = target.get(receiver)
		
		val type = when (result)
		{
			is Value   -> result.type
			is Byte    -> Type.BYT
			is Int     -> Type.INT
			is Long    -> Type.LNG
			is Float   -> Type.FLT
			is Double  -> Type.DEC
			is String  -> Type.TXT
			is Char    -> Type.LET
			is Boolean -> Type.BIT
			else       -> Type.ALL
		}
		
		stack.push(Value(type, result ?: Unit))
	}
}

data class CommandJavaTypeDefine(val clazz: Class<*>, val size: Int)
	: Command()
{
	
	private var constructor: Constructor<*>? = null
	
	override fun eval(stack: Stack, context: Context)
	{
		var type = context.findType(clazz.name)
		if (type == null)
		{
			type = Type(clazz.name, Wraps(clazz))
			
			context.defineType(type)
		}
		
		
		val args = mutableListOf<Any>()
		repeat(size)
		{
			var data = stack.pull()
			while (data is Value)
			{
				data = data.data
			}
			
			args += data
		}
		
		
		if (clazz.isArray)
		{
			require(args.size <= 1)
			{
				"array creation params must be a single value"
			}
			
			val length = requireNotNull((args.single() as? Number)?.toInt())
			{
				"array creation param must be a single int: `${args.single()}`"
			}
			
			return stack.push(Value(type, java.lang.reflect.Array.newInstance(clazz.componentType, length)))
		}
		
		val target: Constructor<*>
		val params: Array<Any>
		
		val cached = this.constructor
		
		if (cached != null)
		{
			target = cached
			params = requireNotNull(resolveMatching(cached, args)?.second) {
				"could not resolve params"
			}
		}
		else
		{
			val (found, match) = requireNotNull(resolve(clazz.declaredConstructors.toList(), args))
			{
				"could not resolve constructor of ${clazz.name} for args ${args.joinToString { it.javaClass.name }}"
			}
			
			require(found is Constructor<*>)
			{
				"resolved constructor was not an actual constructor"
			}
			
			target = found
			params = match
			
			target.isAccessible = true
			
			this.constructor = target
		}
		
		stack.push(Value(type, target.newInstance(*params)))
	}
}


private fun resolve(candidates: List<Executable>, args: List<Any>): Pair<Executable, Array<Any>>?
{
	var funcs = candidates
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

private fun resolveMatching(func: Executable, args: List<Any>): Pair<Executable, Array<Any>>?
{
	val meth = func.parameterTypes
	val pass = args.toTypedArray()
	
	for (index in meth.indices)
	{
		val methType = meth[index]
		
		if (methType == Any::class.java)
		{
			continue
		}
		
		val passData = pass[index]
		val passType = passData.javaClass
		
		if (methType != passType)
		{
			if (methType.isAssignableFrom(passType))
			{
				continue
			}
			
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


private fun makeInstance(name: String, stack: Stack, context: Context, body: Route?): Inst
{
	val clazz = requireNotNull(context.findType(name))
	{
		"trait or class $name undefined in context ${context.name}"
	}
	
	require(clazz.back is Clazz)
	{
		"only classes can be initialized, $name is a ${clazz.back::class}"
	}
	
	val supes = clazz.back.supes
	
	val instance = Inst(clazz)
	
	
	fun definePropsFrom(sourceName: String, props: Map<String, Prop>)
	{
		props.forEach()
		{ (name, newProp) ->
			
			val oldProp = instance.findProp(name) ?: return@forEach instance.defineProp(newProp)
			
			require(oldProp.type.matches(newProp.type))
			{
				"property $newProp in $sourceName conflicts with existing property $oldProp"
			}
		}
	}
	
	fun defineFuncsFrom(sourceName: String, funcs: Map<String, Func>)
	{
		funcs.forEach()
		{ (name, newFunc) ->
			
			val oldFunc = instance.findFunc(name) ?: return@forEach instance.defineFunc(newFunc)
			
			require(oldFunc.pull == newFunc.pull && oldFunc.push == newFunc.push)
			{
				"function $newFunc in $sourceName conflicts with existing function $oldFunc"
			}
			
			if (oldFunc.body == null)
			{
				oldFunc.body = newFunc.body
			}
		}
	}
	
	
	supes.forEach()
	{ superTrait ->
		val trait = requireNotNull(context.findType(superTrait))
		{
			"trait $superTrait undefined in context ${context.name}"
		}
		
		require(trait.back is Trait)
		{
			"classes can only inherit from traits, $name is a ${trait.back::class}"
		}
		
		val traitProps = trait.back.props
		val traitFuncs = trait.back.funcs
		
		definePropsFrom(trait.back.name, traitProps)
		defineFuncsFrom(trait.back.name, traitFuncs)
	}
	
	
	val classProps = clazz.back.props
	val classFuncs = clazz.back.funcs
	
	definePropsFrom(clazz.back.name, classProps)
	defineFuncsFrom(clazz.back.name, classFuncs)
	
	if (body != null)
	{
		instance.enterScope(context.scopes[0])
		Odin.eval(instance, body, stack)
		instance.leaveScope()
	}
	
	val route = clazz.back.route
	if (route != null)
	{
		Odin.eval(instance, route, stack)
	}
	
	return instance
}
