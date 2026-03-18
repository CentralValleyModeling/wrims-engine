package gov.ca.water.wrims.engine.core.solver.solvers.gurobi;

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
		@ServiceProvider(service = Solver.class, position = 1000, path = Solver.LOOKUP_PATH + SolverB.SOLVER_TYPE)
})
public final class SolverB extends SolverIdentifier implements Solver
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SolverB.class);
	private static final long versionId = 1L;
	private static UUID identifier;
	public static final String SOLVER_TYPE = "GUROBI";
	private Integer x = null;
	private Integer y = null;

	public SolverB()
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
	}

	@Override
	public void setLP(String filePath)
	{
		LOGGER.atInfo().log("Solver B: " + filePath);
	}

	@Override
	public void solve()
	{
		if(x != null && y != null)
		{
			LOGGER.atInfo().log(x + " + " + y + " = " + (x + y));
		}
		else
		{
			throw new IllegalStateException("Solver not initialized.");
		}
	}

	@Override
	public void close()
	{
		// nothing to do
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
		final SolverB solverB = (SolverB) o;
		return Objects.equals(x, solverB.x) && Objects.equals(y, solverB.y);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(x, y, versionId);
	}
}
