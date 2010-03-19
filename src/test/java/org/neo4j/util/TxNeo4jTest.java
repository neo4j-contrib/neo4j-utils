package org.neo4j.util;

import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.Transaction;

public abstract class TxNeo4jTest extends Neo4jTest
{
	private Transaction tx;
	
	@Before
	public void setUpTx()
	{
		tx = graphDb().beginTx();
	}
	
	protected void newTransaction()
	{
		tx.success();
		tx.finish();
		tx = graphDb().beginTx();
	}

	@After
	public void tearDownTx() throws Exception
	{
		tx.success();
		tx.finish();
	}
}
