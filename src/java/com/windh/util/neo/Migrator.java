package com.windh.util.neo;

/**
 * A migrator for migration of one version.
 */
public interface Migrator
{
	/**
	 * Performs the migration for this version.
	 * @throws RuntimeException if the migration should fail, which it shouldn't
	 */
	public void performMigration();
}
