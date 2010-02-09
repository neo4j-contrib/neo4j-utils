package org.neo4j.util;

import org.neo4j.kernel.impl.transaction.DeadlockDetectedException;

/**
 * Encapsulates a block of code which is, sort of, expected to throw Neo4j
 * {@link DeadlockDetectedException} and provides a means of performing that
 * code block a number of times in order to succeed.
 * @author mattias
 *
 * @param <T> The result type of the result from {@link #run()}, if any.
 */
public abstract class DeadlockCapsule<T>
{
	private int maxNumberOfTries;
	private long millisToSleepBetweenTries;
	
	public DeadlockCapsule( String name )
	{
		this( name, 5 );
	}
	
	public DeadlockCapsule( String name, int maxNumberOfTries )
	{
		this( name, maxNumberOfTries, 20 );
	}
	
	public DeadlockCapsule( String name, int maxNumberOfTries,
		long millisToSleepBetweenTries )
	{
		this.maxNumberOfTries = maxNumberOfTries;
		this.millisToSleepBetweenTries = millisToSleepBetweenTries;
	}
	
	public abstract T tryOnce();
	
	public final T run()
	{
		int tries = 0;
		do
		{
			tries++;
			try
			{
				return tryOnce();
			}
			catch ( DeadlockDetectedException e )
			{
				try
				{
					Thread.sleep( millisToSleepBetweenTries );
				}
				catch ( InterruptedException ie )
				{
					Thread.interrupted();
					ie.printStackTrace();
				}
			}
		}
		while ( tries < maxNumberOfTries );
		return null;
	}
}
