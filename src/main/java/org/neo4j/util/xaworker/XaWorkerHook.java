package org.neo4j.util.xaworker;

import org.neo4j.util.xaworker.XaWorkerLog.LogDisposal;

public abstract class XaWorkerHook
{
	public XaWorker newXaWorker()
	{
		return new XaWorker( 10 );
	}
	
	protected abstract int getEntrySize();
	
	protected abstract XaWorkerLogEntry newLogEntry();
	
	public final XaWorkerLogEntry newPreparedLogEntry()
	{
		XaWorkerLogEntry result = this.newLogEntry();
		result.setHook( this );
		return result;
	}
	
	public XaWorkerEntry newEntry()
	{
		return new XaWorkerEntry();
	}
	
	public LogDisposal getWorkerLogDisposal()
	{
		return LogDisposal.DELETE;
	}
}
