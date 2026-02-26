package gov.ca.water.wrims.engine.core.solver.lookup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

final class TestServiceLookup
{
	@Test
	void testSolverLookup()
	{
		ISolver solver = SolverBroker.findSolver("test1", ISolver.SolverType.GUROBI);
		assertNotNull(solver);
		solver.init();
		solver.setLP("test.lp");
		solver.solve();
		solver.getSolverInformation();

		solver = SolverBroker.findSolver("test2", ISolver.SolverType.CBC);
		assertNotNull(solver);
		solver.init();
		solver.setLP("test.lp");
		solver.solve();
		solver.getSolverInformation();

		solver = SolverBroker.findSolver("test2", ISolver.SolverType.LPSOLVE);
		assertNull(solver);

		solver = SolverBroker.findSolver("test3", ISolver.SolverType.GUROBI);
		assertNull(solver);
	}
}
