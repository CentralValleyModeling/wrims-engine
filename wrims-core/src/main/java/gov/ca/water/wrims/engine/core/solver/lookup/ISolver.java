package gov.ca.water.wrims.engine.core.solver.lookup;

public interface ISolver
{
	String LOOKUP_PATH = "wrims/solver/";

	void init();

	void setLP(String filePath);

	void solve();

	SolverInfo getSolverInformation();

	class SolverInfo
	{
		private String path;
		private int position;
		private String lookupName;
		private long identifier;

		public SolverInfo(String path, int position, String lookupName, long identifier)
		{
			this.path = path;
			this.position = position;
			this.lookupName = lookupName;
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

		public long getIdentifier()
		{
			return identifier;
		}
	}
}
