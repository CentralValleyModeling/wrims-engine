package gov.ca.water.wrims.engine.core.solver.service;

import java.util.UUID;

public interface Solver
{
	// The base path to use for the solver lookup
	String LOOKUP_PATH = "wrims/solver/";

	/**
	 * Initialize the solver's state
	 */
	void init();

	/**
	 * Set the path to the LP file and solution file
	 *
	 * @param filePath path to the LP file
	 */
	void setLP(String filePath);

	/**
	 * run the solver and produce the solution file
	 */
	void solve();

	/**
	 * Close the solver and release any resources. Log statistics for the solver.
	 */
	void close();

	/**
	 * Get information about the solver
	 *
	 * @return SolverInfo the solver information
	 */
	SolverInfo getSolverInformation();

	/**
	 * Information about the solver
	 */
	class SolverInfo
	{
		private final String path;
		private final int position;
		private final String lookupName;
		private final UUID identifier;

		public SolverInfo(String path, int position, String lookupName, UUID identifier)
		{
			this.path = path;
			this.position = position;
			this.lookupName = lookupName;
			this.identifier = identifier;
		}

		public String getPath()
		{
			return path;
		}

		public int getPosition()
		{
			return position;
		}

		public String getLookupName()
		{
			return lookupName;
		}

		public UUID getIdentifier()
		{
			return identifier;
		}
	}
}
