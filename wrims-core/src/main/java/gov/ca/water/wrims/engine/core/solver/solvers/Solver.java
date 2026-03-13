package gov.ca.water.wrims.engine.core.solver.solvers;

import java.util.UUID;

public interface Solver
{
	String LOOKUP_PATH = "wrims/solver/";

	void init();

	void setLP(String filePath);

	void solve();

	SolverInfo getSolverInformation();

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
