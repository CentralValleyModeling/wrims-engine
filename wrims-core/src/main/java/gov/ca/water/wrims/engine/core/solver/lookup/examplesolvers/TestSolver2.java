package gov.ca.water.wrims.engine.core.solver.lookup.examplesolvers;

import gov.ca.water.wrims.engine.core.solver.lookup.AbstractSolver;
import gov.ca.water.wrims.engine.core.solver.lookup.ISolver;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceProviders(value = {
		@ServiceProvider(service = ISolver.class),
		@ServiceProvider(service = ISolver.class, position = 0, path = ISolver.LOOKUP_PATH + "test2")
})
public final class TestSolver2 extends AbstractSolver implements ISolver
{
	private static final Logger LOGGER = LoggerFactory.getLogger(TestSolver2.class);
	private Integer x = null;
	private Integer y = null;

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
	public void getSolverInformation()
	{
		String solverInfo = this.getClass().getName();

		LOGGER.atInfo().log(solverInfo);
	}

	@Override
	public boolean isValid(SolverType type)
	{
		return type.equals(SolverType.CBC);
	}
}
