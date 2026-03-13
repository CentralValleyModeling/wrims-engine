package gov.ca.water.wrims.engine.core.solver.solvers;

import java.nio.file.Path;
import java.util.Objects;

import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceProviders(value = {
		@ServiceProvider(service = Solver.class),
		@ServiceProvider(service = Solver.class, position = 500, path = Solver.LOOKUP_PATH + ImprovedSolverA.SOLVER_TYPE),
})
public final class ImprovedSolverA extends SolverA implements Solver
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ImprovedSolverA.class);
	private static final long versionId = 2L;
	public static final String SOLVER_TYPE = "CBC_IMPROVED";

	public ImprovedSolverA()
	{
		super();
	}

	@Override
	public void setLP(String filePath)
	{
		filePath = Path.of("/solve/", filePath).toAbsolutePath().toString();
		LOGGER.atInfo().log("Improved Solver A: " + filePath);
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
			LOGGER.atInfo().log(x + " * " + y + " * " + z + "^2 = " + (x * y * (Math.pow(z, 2))));
		}
	}

	@Override
	public Solver.SolverInfo getSolverInformation()
	{
		return new SolverInfo(Solver.LOOKUP_PATH + SOLVER_TYPE, 500, SOLVER_TYPE, getIdentifier());
	}

	@Override
	public boolean equals(Object o)
	{
		if(o == null || getClass() != o.getClass())
		{
			return false;
		}
		final ImprovedSolverA improvedSolverA = (ImprovedSolverA) o;
		return Objects.equals(x, improvedSolverA.x) && Objects.equals(y, improvedSolverA.y) && Objects.equals(z, improvedSolverA.z);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(x, y, z, versionId);
	}
}
