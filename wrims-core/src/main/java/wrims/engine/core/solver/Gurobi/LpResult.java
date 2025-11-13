package wrims.engine.core.solver.Gurobi;

public class LpResult {
	
	public int status = 0;
	
	public String[] varNames = null;
	public double[] varValues = null;

	public LpResult(){
		
		this.status = 0;
		this.varNames = null;
		this.varValues = null;
		
	}
	
}
