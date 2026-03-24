package gov.ca.water.wrims.engine.core.solver.service;

public interface Solver extends AutoCloseable
{
	// The base path to use for the solver lookup
	String LOOKUP_PATH = "wrims/solver/";

	/**
	 * run the solver and produce the solution file
	 */
	void solve();

	@FunctionalInterface
	interface SolverFactory
	{
		Solver create(SolverContext context);
	}
}
