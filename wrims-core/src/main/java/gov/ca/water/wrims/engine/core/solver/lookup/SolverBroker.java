package gov.ca.water.wrims.engine.core.solver.lookup;


import java.util.Collection;

import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class SolverBroker
{
	private SolverBroker()
	{
		throw new UnsupportedOperationException( "Cannot instantiate a utility class.");
	}

	public static ISolver findSolver(String solverName, ISolver.SolverType solverType)
	{
		String lookupPath = ISolver.LOOKUP_PATH + solverName;
		Lookup lookup = Lookups.forPath(lookupPath);
		ISolver retVal = null;
		Collection<? extends ISolver> solvers = lookup.lookupAll(ISolver.class);
		for (ISolver solver : solvers)
		{
			if(solver.isValid(solverType))
			{
				retVal = solver;
				break;
			}
		}
		return retVal;
	}
}
