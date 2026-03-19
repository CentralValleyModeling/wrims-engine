package gov.ca.water.wrims.engine.core.solver.service;


import java.util.ArrayList;
import java.util.List;

import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import gov.ca.water.wrims.engine.core.solver.service.Solver.SolverFactory;

public final class SolverBroker
{
	private SolverBroker()
	{
		throw new UnsupportedOperationException("Cannot instantiate a utility class.");
	}

	public static Solver findSolver(String solverName, SolverContext context) throws IllegalArgumentException
	{
		String lookupPath = Solver.LOOKUP_PATH + solverName;
		Lookup lookup = Lookups.forPath(lookupPath);
		// We are looking up factories instead of solver implementations because service
		// implementations are global singletons, solvers themselves carry state
		// It is up to implementations how to load JNI libraries
		SolverFactory solver = lookup.lookup(SolverFactory.class);
		if (solver == null)
		{
			throw new IllegalArgumentException("Solver not found: " + solverName);
		}
		return solver.create(context);
	}

	public static List<SolverFactory> findAllSolvers()
	{
		Lookup lookup = Lookup.getDefault();
		return new ArrayList<>(lookup.lookupAll(SolverFactory.class));
	}
}
