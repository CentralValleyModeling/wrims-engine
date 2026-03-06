package gov.ca.water.wrims.engine.core.solver.lookup.examplesolvers;

import gov.ca.water.wrims.engine.core.solver.lookup.ISolver;
import gov.ca.water.wrims.engine.core.solver.lookup.SolverTypes;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceProviders(value = {
		@ServiceProvider(service = ISolver.class),
		@ServiceProvider(service = ISolver.class, position = 500, path = ISolver.LOOKUP_PATH + SolverTypes.CBC),
})
public class SolverA_Rev2 implements ISolver
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SolverA_Rev2.class);
	private static Long loadedId = null;
	private Integer x = null;
	private Integer y = null;
	private Integer z = null;

	public SolverA_Rev2()
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
		z = 3;
	}

	@Override
	public void setLP(String filePath)
	{
		LOGGER.atInfo().log("Solver 1: " + filePath);
	}

	@Override
	public void solve()
	{
		if(x == null || y == null || z == null)
		{
			throw new IllegalStateException("Solver not initialized.");
		}
		else
		{
			LOGGER.atInfo().log(x + " * " + y + " * " + z + " = " + (x * y * z));
		}
	}

	@Override
	public ISolver.SolverInfo getSolverInformation()
	{
		return new SolverInfo(ISolver.LOOKUP_PATH + SolverTypes.CBC, 500, SolverTypes.CBC, loadedId);
	}
}
