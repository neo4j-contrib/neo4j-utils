package com.windh.util.neo.xaworker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public abstract class XaWorkerLogEntry
{
	private XaWorkerEntry entry;
	private long position;
	private int transactionId;
	private XaWorkerHook hook;
	
	XaWorkerLogEntry()
	{
	}
	
	void setHook( XaWorkerHook hook )
	{
		this.hook = hook;
	}
	
	public void setEntry( XaWorkerEntry entry )
	{
		this.entry = entry;
	}
	
	public void setTransactionId( int transactionId )
	{
		this.transactionId = transactionId;
	}
	
	public XaWorkerEntry getEntry()
	{
		return entry;
	}
	
	public int getTransactionId()
	{
		return transactionId;
	}
	
	long position()
	{
		return position;
	}
	
	static int getCommandSize( int entrySize )
	{
		// The entry plus the transaction id (int)
		return entrySize + 4;
	}
	
	private int getCommandSize()
	{
		return getCommandSize( this.hook.getEntrySize() );
	}

	public void writeToFile( FileChannel channel, ByteBuffer buffer )
		throws IOException
	{
		buffer.clear();
		buffer.limit( this.getCommandSize() );
		this.writeEntry( buffer );
		buffer.putInt( this.transactionId );
		buffer.flip();
		channel.write( buffer );
	}
	
	protected abstract void writeEntry( ByteBuffer buffer );
	
	void readFromFile( FileChannel channel, ByteBuffer buffer )
		throws IOException
	{
		// -1 since the beginning of the entry starts one byte back where the
		// COMPLETE/INCOMPLETE flag is
		this.position = channel.position() - 1;
		buffer.clear();
		buffer.limit( this.getCommandSize() );
		channel.read( buffer );
		buffer.flip();
		this.entry = this.hook.newEntry();
		this.entry.setValues( this.readEntry( buffer ) );
		this.transactionId = buffer.getInt();
	}
	
	protected abstract Object[] readEntry( ByteBuffer buffer );
	
	@Override
	public String toString()
	{
		return "SearchCommandEntry[" + getEntry() + ":" +
			"txId=" + getTransactionId() + "]";
	}
}
