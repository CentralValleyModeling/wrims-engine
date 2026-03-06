package gov.ca.water.wrims.engine.core.solver.service;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.ca.water.wrims.engine.core.solver.solvers.Solver;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class SolverBroker
{
	private static final Map<String, Solver> solverCache = new HashMap<>();

	private SolverBroker()
	{
		throw new UnsupportedOperationException( "Cannot instantiate a utility class.");
	}

	public enum CacheMode
	{
		CACHE_ONLY,
		BYPASS,
		CACHE
	}

	public static void clearCache()
	{
		solverCache.clear();
	}

	public static boolean inCache(String solverName)
	{
		return solverCache.containsKey(solverName);
	}

	public static Solver findSolver(String solverName)
	{
		return findSolver(solverName, CacheMode.CACHE);
	}

	public static Solver findSolver(String solverName, CacheMode mode)
	{
		Solver retVal = null;
		if (mode != CacheMode.BYPASS && solverCache.isEmpty())
		{
			getAllSolvers(mode);
		}
		if (mode != CacheMode.BYPASS && solverCache.containsKey(solverName))
		{
			return solverCache.get(solverName);
		}
		else if (mode != CacheMode.CACHE_ONLY)
		{
			String lookupPath = Solver.LOOKUP_PATH + solverName;
			Lookup lookup = Lookups.forPath(lookupPath);
			Collection<? extends Solver> solvers = lookup.lookupAll(Solver.class);
			for(Solver solver : solvers)
			{
				if(solver.getSolverInformation().getLookupName().equals(solverName))
				{
					retVal = solver;
					if (mode != CacheMode.BYPASS)
					{
						solverCache.put(solverName, solver);
					}
					break;
				}
			}
		}
		return retVal;
	}

	public static List<Solver> getAllSolvers()
	{
		return getAllSolvers(CacheMode.CACHE);
	}

	public static List<Solver> getAllSolvers(CacheMode mode)
	{
		Collection<? extends Solver> solvers = new ArrayList<>();
		if (mode != CacheMode.BYPASS && !solverCache.isEmpty())
		{
			return solverCache.values().stream().toList();
		}
		else if (mode != CacheMode.CACHE_ONLY)
		{
			Lookup lookup = Lookup.getDefault();
			solvers = lookup.lookupAll(Solver.class);
			solvers.forEach(solver -> solverCache.put(solver.getSolverInformation().getLookupName(), solver));
		}
		return new ArrayList<>(solvers);
	}
}
