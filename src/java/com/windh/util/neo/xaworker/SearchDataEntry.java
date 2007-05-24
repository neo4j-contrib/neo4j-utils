package com.windh.util.neo.xaworker;

/**
 * Convenience class which is useful for a simple xa data source which
 * indexes search engines asynchronously
 */
public class SearchDataEntry extends XaWorkerEntry
{
	public void setSearchValues( byte type, long id, byte mode )
	{
		setValues( type, id, mode );
	}
	
	/**
	 * What type of object
	 */
	public byte getType()
	{
		return ( ( Number ) this.getValues()[ 0 ] ).byteValue();
	}
	
	/**
	 * The id of the object
	 */
	public long getId()
	{
		return ( ( Number ) this.getValues()[ 1 ] ).longValue();
	}
	
	/**
	 * The alteration mode, add, delete, update perhaps... the values of this
	 * mode is decided by the client code which adds entries to the data source
	 */
	public byte getMode()
	{
		return ( ( Number ) this.getValues()[ 2 ] ).byteValue();
	}
}
