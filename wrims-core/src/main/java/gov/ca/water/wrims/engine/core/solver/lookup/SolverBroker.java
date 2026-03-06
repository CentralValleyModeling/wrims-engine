package gov.ca.water.wrims.engine.core.solver.lookup;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class SolverBroker
{
	private static final Map<String, ISolver> solverCache = new HashMap<>();

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

	public static ISolver findSolver(String solverName)
	{
		return findSolver(solverName, CacheMode.CACHE);
	}

	public static ISolver findSolver(String solverName, CacheMode mode)
	{
		ISolver retVal = null;
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
			String lookupPath = ISolver.LOOKUP_PATH + solverName;
			Lookup lookup = Lookups.forPath(lookupPath);
			Collection<? extends ISolver> solvers = lookup.lookupAll(ISolver.class);
			for(ISolver solver : solvers)
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

	public static List<ISolver> getAllSolvers()
	{
		return getAllSolvers(CacheMode.CACHE);
	}

	public static List<ISolver> getAllSolvers(CacheMode mode)
	{
		Collection<? extends ISolver> solvers = new ArrayList<>();
		if (mode != CacheMode.BYPASS && !solverCache.isEmpty())
		{
			return solverCache.values().stream().toList();
		}
		else if (mode != CacheMode.CACHE_ONLY)
		{
			Lookup lookup = Lookup.getDefault();
			solvers = lookup.lookupAll(ISolver.class);
			solvers.forEach(solver -> solverCache.put(solver.getSolverInformation().getLookupName(), solver));
		}
		return new ArrayList<>(solvers);
	}
}
