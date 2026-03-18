package gov.ca.water.wrims.engine.core.solver.service;


import java.util.ArrayList;
import java.util.List;

import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class SolverBroker
{
	private SolverBroker()
	{
		throw new UnsupportedOperationException("Cannot instantiate a utility class.");
	}

	public static Solver findSolver(String solverName)
	{
		String lookupPath = Solver.LOOKUP_PATH + solverName;
		Lookup lookup = Lookups.forPath(lookupPath);
		return lookup.lookup(Solver.class);
	}

	public static List<Solver> getAllSolvers()
	{
		Lookup lookup = Lookup.getDefault();
		return new ArrayList<>(lookup.lookupAll(Solver.class));
	}
}
