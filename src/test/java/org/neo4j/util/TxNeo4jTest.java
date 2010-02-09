package org.neo4j.util;

import org.neo4j.graphdb.Transaction;

public abstract class TxNeo4jTest extends Neo4jTest
{
	private Transaction tx;
	
	@Override
	public void setUp() throws Exception
	{
		super.setUp();
		tx = graphDb().beginTx();
	}
	
	protected void newTransaction()
	{
		tx.success();
		tx.finish();
		tx = graphDb().beginTx();
	}

	@Override
	protected void tearDown() throws Exception
	{
		tx.success();
		tx.finish();
		super.tearDown();
	}
}
