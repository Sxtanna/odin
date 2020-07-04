package com.sxtanna.odin.compile.data

import com.sxtanna.odin.runtime.base.Stack
import com.sxtanna.odin.runtime.base.Value
import com.sxtanna.odin.runtime.data.Type

sealed class Oper
{
	
	enum class Dir
	{
		L,
		R,
	}
	
	enum class Lvl
	{
		T_0,
		T_1,
		T_2,
		T_3,
		T_4,
		T_5,
	}
	
	
	abstract fun eval(stack: Stack)
	
	override fun toString(): String
	{
		return (this::class.simpleName ?: "Unknown").replace("operator", "", true)
	}
	
	
	object SOS : Oper()
	{
		override fun eval(stack: Stack) = Unit
	}
	
	object EOS : Oper()
	{
		override fun eval(stack: Stack) = Unit
	}
	
}


sealed class OperatorLvl : Oper()
{
	abstract val dir: Dir
	abstract val lvl: Lvl
}

object OperatorCon : OperatorLvl()
{
	override val dir = Dir.L
	override val lvl = Lvl.T_0
	
	override fun eval(stack: Stack)
	{
		val obj1 = stack.pull().asObject(concat = true)
		val obj0 = stack.pull().asObject(concat = true)
		
		stack.push(Value(Type.TXT, "$obj1$obj0"))
	}
}

sealed class OperatorNum(final override val lvl: Lvl) : OperatorLvl()
{
	final override val dir = Dir.L
	
	final override fun eval(stack: Stack)
	{
		val var1 = stack.pull()
		val var0 = stack.pull()
		
		val num1 = var1.asNumber()
		val num0 = var0.asNumber()
		
		if (this == OperatorAdd && (num0 == Double.NEGATIVE_INFINITY || num1 == Double.NEGATIVE_INFINITY))
		{
			stack.push(var1)
			stack.push(var0)
			
			return OperatorCon.eval(stack)
		}
		
		var type = Type.ALL
		var data = evalNum(num0, num1)
		
		if (num0 is Byte || num1 is Byte)
		{
			type = Type.BYT
			data = data.toByte()
		}
		if (num0 is Int || num1 is Int)
		{
			type = Type.INT
			data = data.toInt()
		}
		if (num0 is Long || num1 is Long)
		{
			type = Type.LNG
			data = data.toLong()
		}
		if (num0 is Float || num1 is Float)
		{
			type = Type.FLT
			data = data.toFloat()
		}
		if (num0 is Double || num1 is Double)
		{
			type = Type.DEC
			data = data.toDouble()
		}
		
		require(type != Type.ALL)
		{
			"number type could not be determined"
		}
		
		stack.push(Value(type, data))
	}
	
	protected abstract fun evalNum(num0: Number, num1: Number): Number
}

object OperatorAdd : OperatorNum(Lvl.T_3)
{
	override fun evalNum(num0: Number, num1: Number): Number
	{
		return num0.toDouble() + num1.toDouble()
	}
}

object OperatorSub : OperatorNum(Lvl.T_3)
{
	override fun evalNum(num0: Number, num1: Number): Number
	{
		return num0.toDouble() - num1.toDouble()
	}
}

object OperatorMul : OperatorNum(Lvl.T_4)
{
	override fun evalNum(num0: Number, num1: Number): Number
	{
		return num0.toDouble() * num1.toDouble()
	}
}

object OperatorDiv : OperatorNum(Lvl.T_4)
{
	override fun evalNum(num0: Number, num1: Number): Number
	{
		return num0.toDouble() / num1.toDouble()
	}
}


sealed class OperatorBit(final override val lvl: Lvl) : OperatorLvl()
{
	override val dir = Dir.L
	
	final override fun eval(stack: Stack)
	{
		stack.push(Value(Type.BIT, evalBit(stack)))
	}
	
	protected abstract fun evalBit(stack: Stack): Boolean
}

object OperatorNot : OperatorBit(Lvl.T_1)
{
	override val dir = Dir.R
	
	override fun evalBit(stack: Stack): Boolean
	{
		return !stack.pull().asBoolean()
	}
}


sealed class OperatorCom(lvl: Lvl) : OperatorBit(lvl)
{
	final override fun evalBit(stack: Stack): Boolean
	{
		val bitR = stack.pull()
		val bitL = stack.pull()
		
		return evalCom(bitL, bitR)
	}
	
	protected abstract fun evalCom(bitL: Any, bitR: Any): Boolean
}

object OperatorElse : OperatorCom(Lvl.T_0)
{
	override fun evalCom(bitL: Any, bitR: Any): Boolean
	{
		return bitL.asBoolean() || bitR.asBoolean()
	}
}

object OperatorBoth : OperatorCom(Lvl.T_0)
{
	override fun evalCom(bitL: Any, bitR: Any): Boolean
	{
		return bitL.asBoolean() && bitR.asBoolean()
	}
}

object OperatorSame : OperatorCom(Lvl.T_2)
{
	override fun evalCom(bitL: Any, bitR: Any): Boolean
	{
		return bitL == bitR
	}
}

object OperatorDiff : OperatorCom(Lvl.T_2)
{
	override fun evalCom(bitL: Any, bitR: Any): Boolean
	{
		return bitL != bitR
	}
}

object OperatorMore : OperatorCom(Lvl.T_2)
{
	override fun evalCom(bitL: Any, bitR: Any): Boolean
	{
		return compare(bitL.asObject(), bitR.asObject()) > 0
	}
}

object OperatorLess : OperatorCom(Lvl.T_2)
{
	override fun evalCom(bitL: Any, bitR: Any): Boolean
	{
		return compare(bitL.asObject(), bitR.asObject()) < 0
	}
}

object OperatorMoreOrSame : OperatorCom(Lvl.T_2)
{
	override fun evalCom(bitL: Any, bitR: Any): Boolean
	{
		return compare(bitL.asObject(), bitR.asObject()) >= 0
	}
}

object OperatorLessOrSame : OperatorCom(Lvl.T_2)
{
	override fun evalCom(bitL: Any, bitR: Any): Boolean
	{
		return compare(bitL.asObject(), bitR.asObject()) <= 0
	}
}


private fun Any.asNumber(): Number
{
	return when (this)
	{
		is Number  ->
		{
			this
		}
		is Boolean ->
		{
			if (this) 1 else 0
		}
		is Value   ->
		{
			data.asNumber()
		}
		else       ->
		{
			Double.NEGATIVE_INFINITY
		}
	}
}

private fun Any.asBoolean(): Boolean
{
	return when (this)
	{
		is Boolean ->
		{
			this
		}
		is Number  ->
		{
			toInt() != 0
		}
		is Value   ->
		{
			data.asBoolean()
		}
		else       ->
		{
			throw UnsupportedOperationException("Could not convert $this to a number")
		}
	}
}

private fun Any.asObject(concat: Boolean = false): Any
{
	return when (this)
	{
		is Value ->
		{
			if (concat) toPushString() else data.asObject()
		}
		else     ->
		{
			this
		}
	}
}

@Suppress("UNCHECKED_CAST")
private fun compare(bitL: Any, bitR: Any): Int
{
	check(bitL is Comparable<*> && bitL::class.java.isAssignableFrom(bitR::class.java)) {
		"values [$bitL|${bitL::class.simpleName}] and [$bitR|${bitR::class.simpleName}] aren't comparable"
	}
	
	val comL = requireNotNull(bitL as? Comparable<Any>)
	{
		"could not assert left operand:[$bitL] as comparable"
	}
	val comR = requireNotNull(bitR as? Comparable<Any>)
	{
		"could not assert right operand:[$bitR] as comparable"
	}
	
	return comL.compareTo(comR)
}