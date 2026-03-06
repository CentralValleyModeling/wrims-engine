package gov.ca.water.wrims.engine.core.solver.lookup.examplesolvers;

import gov.ca.water.wrims.engine.core.solver.lookup.ISolver;
import gov.ca.water.wrims.engine.core.solver.lookup.SolverTypes;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceProviders(value = {
		@ServiceProvider(service = ISolver.class),
		@ServiceProvider(service = ISolver.class, position = 1000, path = ISolver.LOOKUP_PATH + SolverTypes.GUROBI)
})
public final class SolverB implements ISolver
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SolverB.class);
	private static Long loadedId;
	private Integer x = null;
	private Integer y = null;

	public SolverB()
	{
		load();
	}

	private static void load()
	{
		// imitate static library loading procedure
		loadedId = System.currentTimeMillis();
	}

	@Override
	public void init()
	{
		x = 1;
		y = 2;
	}

	@Override
	public void setLP(String filePath)
	{
		LOGGER.atInfo().log("Solver 2: " + filePath);
	}

	@Override
	public void solve()
	{
		if (x != null && y != null)
		{
			LOGGER.atInfo().log(x + " + " + y + " = " + (x + y));
		}
		else
		{
			throw new IllegalStateException("Solver not initialized.");
		}
	}

	@Override
	public SolverInfo getSolverInformation()
	{
		return new SolverInfo(ISolver.LOOKUP_PATH + SolverTypes.GUROBI, 1000, SolverTypes.GUROBI, loadedId);
	}
}
