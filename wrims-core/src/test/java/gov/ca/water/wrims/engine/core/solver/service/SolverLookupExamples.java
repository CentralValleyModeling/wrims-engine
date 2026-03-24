package gov.ca.water.wrims.engine.core.solver.service;

import java.nio.file.Path;
import java.util.List;
import gov.ca.water.wrims.engine.core.solver.service.Solver.SolverFactory;

final class SolverLookupExamples
{
	void useCBCSolver() throws Exception
	{
		SolverContext context = new SolverContext();
		// Providing all context needed for solvers using LP file as an example
		context.setLP(Path.of("test.solve"));
		// This will get the newest version of the CBC solver, given the supersedes configuration for CBC solver version 2
		try(Solver cbc = SolverBroker.findSolver("CBC", context))
		{
			cbc.solve();
			// Given the solver is in the local context, all computation state is locally scoped,
			// which makes this threadsafe and deterministic.
		}
		// CBC solver is auto-closable, so resources will be closed when out of scope
	}

	void useOlderVersionOfSolver() throws Exception
	{
		SolverContext context = new SolverContext();
		context.setLP(Path.of("test.solve"));
		// This will get the version 1 of the CBC solver, given the multiple service provider definitions
		try(Solver cbc = SolverBroker.findSolver("CBC_V1", context))
		{
			cbc.solve();
		}
	}

	void findNonExistentSolver() throws Exception
	{
		SolverContext context = new SolverContext();
		context.setLP(Path.of("test.solve"));
		// This throws an exception, given this LP does not exist and thus is a configuration issue
		try(Solver cbc = SolverBroker.findSolver("MACHINE_LEARNING_LP", context))
		{
			cbc.solve();
		}
	}

	void resolveSolverFactoryOrderBasedOnServicePosition()
	{
		List<SolverFactory> solvers = SolverBroker.findAllSolvers();

		// This is Gurobi because it is the lowest position
		SolverFactory gurobi = solvers.getFirst();

		// This is CBC_V2 because the position is greater than Gurobi, but explicitly supersedes V1
		SolverFactory cbcV2 = solvers.get(1);

		// This is CBC_V1, as it has the highest position and is explicitly superseded by V2
		SolverFactory cbcV1 = solvers.get(2);
	}
}
