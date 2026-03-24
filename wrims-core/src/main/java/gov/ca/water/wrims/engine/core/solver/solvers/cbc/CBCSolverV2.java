package gov.ca.water.wrims.engine.core.solver.solvers.cbc;

import gov.ca.water.wrims.engine.core.solver.service.Solver;
import gov.ca.water.wrims.engine.core.solver.service.SolverContext;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

public final class CBCSolverV2 extends CBCSolver implements Solver
{
	public static final String SOLVER_TYPE_V2 = "CBC_V2";

	public CBCSolverV2(SolverContext context)
	{
		super(context);
	}

	@Override
	public void solve()
	{
		// Do something different from CBCSolver V1 here
		super.solve();
	}

	@Override
	public void close()
	{
		// Cleaning up resources for CBC solver version 2
	}

	// Provide both new and old solver paths, allowing this version to be used explicitly
	@ServiceProviders(value = {
			@ServiceProvider(service = SolverFactory.class, position = 500),
			@ServiceProvider(service = SolverFactory.class, position = 500, path = Solver.LOOKUP_PATH + SOLVER_TYPE, supersedes = {"gov.ca.water.wrims.engine.core.solver.solvers.cbc.CBCSolver"}),
			@ServiceProvider(service = SolverFactory.class, position = 500, path = Solver.LOOKUP_PATH + SOLVER_TYPE_V2, supersedes = {"gov.ca.water.wrims.engine.core.solver.solvers.cbc.CBCSolver"}),
	})
	public static class ImprovedSolverAFactory implements SolverFactory
	{
		@Override
		public Solver create(SolverContext context)
		{
			return new CBCSolverV2(context);
		}
	}
}
