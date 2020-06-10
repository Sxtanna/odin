package com.sxtanna.odin;

public final class Person
{

	private final String name;

	public Person()
	{
		this.name = "Sxtanna";
	}


	public String getNameWith(final String other, final int amount)
	{
		return this.name + ":" + new String(new char[amount]).replace("\0", other);
	}

	public String getNameWith(final int amount, final String other)
	{
		return getNameWith(other, amount);
	}

}
