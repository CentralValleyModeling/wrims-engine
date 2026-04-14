package gov.ca.water.wrims.engine.core.solver.solvers;

import gov.ca.water.wrims.engine.core.solver.service.SolverContext;

// Placeholder abstract class for shared implementation details
public abstract class WrimsSolver
{
	private final SolverContext context;

	protected WrimsSolver(SolverContext context)
	{
		this.context = context;
	}

	public SolverContext getContext()
	{
		return context;
	}
}
