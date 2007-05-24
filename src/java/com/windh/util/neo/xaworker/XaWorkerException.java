package com.windh.util.neo.xaworker;

public class XaWorkerException extends Exception
{
	public XaWorkerException()
	{
	}

	public XaWorkerException( String message )
	{
		super( message );
	}

	public XaWorkerException( Throwable cause )
	{
		super( cause );
	}

	public XaWorkerException( String message, Throwable cause )
	{
		super( message, cause );
	}
}
