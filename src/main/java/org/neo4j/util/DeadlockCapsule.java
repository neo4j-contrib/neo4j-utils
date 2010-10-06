/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.util;

import org.neo4j.kernel.DeadlockDetectedException;

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
