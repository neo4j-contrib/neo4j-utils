package org.neo4j.util;

/**
 * Class representing one CRUD (Create, Read, Update, Delete) operation.
 * @author mattias
 */
public abstract class CrudEventData
{
	private AlterationMode mode;
	
	/**
	 * @param mode which operation mode this event stands for.
	 */
	public CrudEventData( AlterationMode mode )
	{
		this.mode = mode;
	}
	
	/**
	 * @return the operation mode from the constructor.
	 */
	public AlterationMode getAlterationMode()
	{
		return mode;
	}
	
	/**
	 * @return the associated node (node id).
	 */
	public abstract long getNodeId();
	
	/**
	 * The available operation modes.
	 */
	public static enum AlterationMode
	{
		/**
		 * An entity has been created.
		 */
		CREATED,
		
		/**
		 * An entity has been modified.
		 */
		MODIFIED,
		
		/**
		 * An entity has been deleted.
		 */
		DELETED,
	}
}
