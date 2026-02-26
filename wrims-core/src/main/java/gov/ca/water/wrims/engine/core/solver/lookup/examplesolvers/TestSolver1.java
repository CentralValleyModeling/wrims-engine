package gov.ca.water.wrims.engine.core.solver.lookup.examplesolvers;

import gov.ca.water.wrims.engine.core.solver.lookup.AbstractSolver;
import gov.ca.water.wrims.engine.core.solver.lookup.ISolver;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders(value = {
		@ServiceProvider(service = ISolver.class),
		@ServiceProvider(service = ISolver.class, position = 0, path = ISolver.LOOKUP_PATH + "test1")
})
public final class TestSolver1 extends AbstractSolver implements ISolver
{
	private Integer x = null;
	private Integer y = null;
	private Integer z = null;

	@Override
	public void init()
	{
		x = 1;
		y = 2;
	}

	@Override
	public void setLP(String filePath)
	{
		System.out.println("Solver 1: " + filePath);
	}

	@Override
	public void solve()
	{
		System.out.println(x + " * " + y + " * " + z + " = " + (x * y * z));
	}

	@Override
	public void getSolverInformation()
	{
		String solverInfo = this.getClass().getName();

		System.out.println(solverInfo);
	}

	@Override
	public boolean isValid(SolverType type)
	{
		return type.equals(SolverType.GUROBI);
	}
}
