package com.sxtanna.odin.results

sealed class Result<T : Any>
{
	companion object
	{
		fun <T : Any> some(data: T): Result<T>
		{
			return Some(data)
		}
		
		fun <T : Any> none(): Result<T>
		{
			return None(NoSuchElementException())
		}
		
		inline fun <T : Any> of(function: () -> T?): Result<T>
		{
			return try
			{
				val data = requireNotNull(function.invoke())
				{
					"value is null"
				}
				
				Some(data)
			}
			catch (info: Throwable)
			{
				None(info)
			}
		}
	}
}

data class Some<T : Any>(val data: T)
	: Result<T>()

data class None<T : Any>(val info: Throwable)
	: Result<T>()


fun <I : Any, O : Any> Result<I>.map(function: (I) -> O?): Result<O>
{
	return when (this)
	{
		is None ->
		{
			None(this.info)
		}
		is Some ->
		{
			Result.of { function.invoke(this.data) }
		}
	}
}