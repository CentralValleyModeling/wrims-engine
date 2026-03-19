package gov.ca.water.wrims.engine.core.solver.solvers.cbc;

import java.io.File;

import gov.ca.water.wrims.engine.core.solver.service.Solver;
import gov.ca.water.wrims.engine.core.solver.service.SolverContext;
import gov.ca.water.wrims.engine.core.solver.solvers.WrimsSolver;
import org.apache.commons.io.FilenameUtils;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

public class CBCSolver extends WrimsSolver implements Solver
{
	public static final String SOLVER_TYPE = "CBC";
	public static final String SOLVER_TYPE_V1 = "CBC_V1";
	private String soluPath;

	public CBCSolver(SolverContext context)
	{
		super(context);
		// Solver configuration initialization done using the parameterized context
		soluPath = FilenameUtils.removeExtension(context.getLP().toString())+".sn";
		File soluF = new File(soluPath);
		if (soluF.exists()){
			soluF.delete();
		}
	}

	@Override
	public void solve()
	{
		// Performing solve operation using CBC
	}

	@Override
	public void close()
	{
		// Cleaning up resources for CBC solver
	}

	// Provide both new and old solver paths, allowing this version to be used explicitly
	@ServiceProviders(value = {
			@ServiceProvider(service = SolverFactory.class, position = 1000),
			@ServiceProvider(service = SolverFactory.class, position = 1000, path = Solver.LOOKUP_PATH + SOLVER_TYPE_V1),
			@ServiceProvider(service = SolverFactory.class, position = 1000, path = Solver.LOOKUP_PATH + SOLVER_TYPE)
	})
	public static class CBCSolverFactory implements SolverFactory
	{
		@Override
		public Solver create(SolverContext context)
		{
			return new CBCSolver(context);
		}
	}
}
