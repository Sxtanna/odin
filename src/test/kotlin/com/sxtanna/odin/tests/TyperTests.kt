package com.sxtanna.odin.tests

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isSameAs
import assertk.assertions.isSuccess
import assertk.assertions.key
import assertk.assertions.prop
import assertk.assertions.size
import com.sxtanna.odin.OdinTests
import com.sxtanna.odin.compile.Lexer
import com.sxtanna.odin.compile.Typer
import com.sxtanna.odin.compile.data.OperatorAdd
import com.sxtanna.odin.runtime.Command
import com.sxtanna.odin.runtime.CommandFunctionDefine
import com.sxtanna.odin.runtime.CommandLiteral
import com.sxtanna.odin.runtime.CommandOperate
import com.sxtanna.odin.runtime.CommandPropertyAccess
import com.sxtanna.odin.runtime.CommandPropertyAssign
import com.sxtanna.odin.runtime.CommandPropertyDefine
import com.sxtanna.odin.runtime.CommandPropertyResets
import com.sxtanna.odin.runtime.base.Route
import com.sxtanna.odin.runtime.base.Types
import com.sxtanna.odin.runtime.data.Func
import com.sxtanna.odin.runtime.data.Prop
import com.sxtanna.odin.runtime.data.Type
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(OdinTests::class)
object TyperTests
{
	
	private fun assertTypesSuccessfully(text: String): Assert<List<Command>>
	{
		return assertThat { Typer(Lexer(text)) }.isSuccess()
	}
	
	@Test
	internal fun `test property without type`()
	{
		val data = 10
		val name = "number"
		
		assertTypesSuccessfully("val $name = $data").all()
		{
			size().isEqualTo(3)
			
			index(0).isInstanceOf(CommandPropertyDefine::class).prop(CommandPropertyDefine::prop).all()
			{
				prop(Prop::mutable).isFalse()
				
				prop(Prop::name).isEqualTo(name)
				
				prop(Prop::type).isEqualTo(Types.none())
			}
			
			index(1).isInstanceOf(CommandLiteral::class).all()
			{
				prop(CommandLiteral::type).isEqualTo("Int")
				prop(CommandLiteral::data).isEqualTo(data)
			}
			
			index(2).isInstanceOf(CommandPropertyAssign::class).prop(CommandPropertyAssign::name).isEqualTo(name)
		}
	}
	
	
	@Test
	internal fun `test function with repeated type arguments`()
	{
		assertTypesSuccessfully("fun add(arg0, arg1: Int): Int { => arg0 + arg1 }").all()
		{
			size().isEqualTo(1)
			
			index(0).isInstanceOf(CommandFunctionDefine::class).prop(CommandFunctionDefine::func).all()
			{
				prop(Func::pull).all()
				{
					size().isEqualTo(2)
					
					key("arg0").isEqualTo(Type.INT.back)
					key("arg1").isEqualTo(Type.INT.back)
				}
				
				prop(Func::push).all()
				{
					size().isEqualTo(1)
					
					key("ret0").isEqualTo(Type.INT.back)
				}
				
				prop(Func::body).isNotNull().transform("unwrapped route", Route::unwrap).all()
				{
					size().isEqualTo(9)
					
					index(0).isInstanceOf(CommandPropertyDefine::class).prop(CommandPropertyDefine::prop).all()
					{
						prop(Prop::mutable).isFalse()
						
						prop(Prop::name).isEqualTo("arg0")
						
						prop(Prop::type).isEqualTo(Type.INT.back)
					}
					index(1).isInstanceOf(CommandPropertyAssign::class).prop(CommandPropertyAssign::name).isEqualTo("arg0")
					
					
					index(2).isInstanceOf(CommandPropertyDefine::class).prop(CommandPropertyDefine::prop).all()
					{
						prop(Prop::mutable).isFalse()
						
						prop(Prop::name).isEqualTo("arg1")
						
						prop(Prop::type).isEqualTo(Type.INT.back)
					}
					index(3).isInstanceOf(CommandPropertyAssign::class).prop(CommandPropertyAssign::name).isEqualTo("arg1")
					
					index(4).isInstanceOf(CommandPropertyAccess::class).prop(CommandPropertyAccess::name).isEqualTo("arg0")
					index(5).isInstanceOf(CommandPropertyAccess::class).prop(CommandPropertyAccess::name).isEqualTo("arg1")
					
					index(6).isInstanceOf(CommandOperate::class).prop(CommandOperate::oper).isSameAs(OperatorAdd)
					
					index(7).isInstanceOf(CommandPropertyResets::class).prop(CommandPropertyResets::name).isEqualTo("arg0")
					index(8).isInstanceOf(CommandPropertyResets::class).prop(CommandPropertyResets::name).isEqualTo("arg1")
				}
			}
		}
	}
	
}