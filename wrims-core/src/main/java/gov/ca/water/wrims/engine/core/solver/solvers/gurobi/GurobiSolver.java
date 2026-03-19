package gov.ca.water.wrims.engine.core.solver.solvers.gurobi;

import gov.ca.water.wrims.engine.core.solver.service.Solver;
import gov.ca.water.wrims.engine.core.solver.service.SolverContext;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

public final class GurobiSolver implements Solver
{
	public static final String SOLVER_TYPE = "GUROBI";

	public GurobiSolver(SolverContext context)
	{
		// Initialize solver based on context
	}

	@Override
	public void solve()
	{
		// Performing solve operation using Gurobi
	}

	@Override
	public void close()
	{
		// Cleaning up resources for Gurobi solver
	}

	@ServiceProviders(value = {
			@ServiceProvider(service = SolverFactory.class, position = 100),
			@ServiceProvider(service = SolverFactory.class, position = 100, path = Solver.LOOKUP_PATH + GurobiSolver.SOLVER_TYPE)
	})
	public static final class GurobiSolverFactory implements SolverFactory
	{
		public GurobiSolverFactory()
		{
			// Given service loading design, this is only executed once
			// It is an implementation detail whether to load the library here or in a static initializer block
			// The determination of where it is placed should be based on code readability and maintainability

			// System.loadLibrary("gurobi_java");
		}

		@Override
		public Solver create(SolverContext context)
		{
			return new GurobiSolver(context);
		}
	}
}
