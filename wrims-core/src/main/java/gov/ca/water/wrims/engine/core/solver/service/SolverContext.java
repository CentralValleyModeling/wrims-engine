package gov.ca.water.wrims.engine.core.solver.service;

import java.nio.file.Path;

// context provided by model process containing input configuration for the solvers
// The solver context is available for the lifetime of the solver process
public final class SolverContext
{
	private Path lpFile;

	public Path getLP()
	{
		return lpFile;
	}

	// Mutable state should be explicitly scoped to not allow mutation within the solver
	void setLP(Path lpFile)
	{
		this.lpFile = lpFile;
	}
}
