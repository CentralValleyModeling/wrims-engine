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
public class SolverA_Rev2 extends SolverA implements ISolver
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SolverA_Rev2.class);

	public SolverA_Rev2()
	{
		super();
	}

	@Override
	public void setLP(String filePath)
	{
		LOGGER.atInfo().log("Solver A Revision 2: " + filePath);
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
		return new SolverInfo(ISolver.LOOKUP_PATH + SolverTypes.CBC, 500, SolverTypes.CBC, getIdentifier());
	}
}
