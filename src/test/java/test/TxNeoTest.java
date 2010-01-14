package test;

import org.neo4j.graphdb.Transaction;

public class TxNeoTest extends NeoTest
{
	private Transaction tx;
	
	@Override
	public void setUp() throws Exception
	{
		super.setUp();
		tx = neo().beginTx();
	}
	
	protected void newTransaction()
	{
		tx.success();
		tx.finish();
		tx = neo().beginTx();
	}

	@Override
	protected void tearDown() throws Exception
	{
		tx.success();
		tx.finish();
		super.tearDown();
	}
}
