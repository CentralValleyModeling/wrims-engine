package gov.ca.water.wrims.engine.core.solver.lookup;

public interface ISolver
{
	enum SolverType
	{
		CBC, LPSOLVE, ORTOOLS, GUROBI
	}

	String LOOKUP_PATH = "wrims/solver/";

	void init();

	void setLP(String filePath);

	void solve();

	void getSolverInformation();

	boolean isValid(SolverType solverType);
}
