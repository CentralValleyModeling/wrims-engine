package gov.ca.water.wrims.engine.core.solver.solvers.cbc;

import java.util.Objects;
import java.util.UUID;

import gov.ca.water.wrims.engine.core.solver.service.Solver;
import gov.ca.water.wrims.engine.core.solver.solvers.shared.SolverIdentifier;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceProviders(value = {
		@ServiceProvider(service = Solver.class),
		@ServiceProvider(service = Solver.class, position = 1000, path = Solver.LOOKUP_PATH + SolverA.SOLVER_TYPE)
})
public class SolverA extends SolverIdentifier implements Solver
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SolverA.class);
	private static final long versionId = 0L;
	private static UUID identifier;
	public static final String SOLVER_TYPE = "CBC";
	Integer x = null;
	Integer y = null;
	Integer z = null;

	public SolverA()
	{
		load();
	}

	private static void load()
	{
		// imitate static library loading procedure
		identifier = UUID.randomUUID();
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
		LOGGER.atInfo().log("Solver A: " + filePath);
	}

	@Override
	public void solve()
	{
		if (x == null || y == null || z == null)
		{
			throw new IllegalStateException("Solver not initialized.");
		}
		else
		{
			LOGGER.atInfo().log(x + " * " + y + " * " + z + " = " + (x * y * z));
		}
	}

	@Override
	public void close()
	{
		LOGGER.atDebug().log("Closing CBC solver.");
	}

	@Override
	public SolverInfo getSolverInformation()
	{
		return new SolverInfo(Solver.LOOKUP_PATH + SOLVER_TYPE, 1000, SOLVER_TYPE, getIdentifier());
	}

	public UUID getIdentifier()
	{
		return identifier;
	}

	@Override
	public boolean equals(Object o)
	{
		if(o == null || getClass() != o.getClass())
		{
			return false;
		}
		final SolverA solverA = (SolverA) o;
		return Objects.equals(x, solverA.x) && Objects.equals(y, solverA.y) && Objects.equals(z, solverA.z);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(x, y, z, versionId);
	}
}
