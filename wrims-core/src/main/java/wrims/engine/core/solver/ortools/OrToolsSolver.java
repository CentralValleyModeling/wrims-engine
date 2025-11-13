package wrims.engine.core.solver.ortools;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import wrims.engine.core.commondata.wresldata.Param;
import wrims.engine.core.solver.mpmodel.MPModel;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

public class OrToolsSolver {

	// private static String solverType = "CLP_LINEAR_PROGRAMMING";
	// private static String mpSolverType = "CBC_MIXED_INTEGER_PROGRAMMING";
	// private static String mpSolverType = "GLPK_MIXED_INTEGER_PROGRAMMING";
	public MPSolver solver;
	public MPModel model;
	private Map<String, MPVariable> _solverVarMap = null;
	private Map<String, MPConstraint> _solverConstraintMap = null;
	public LinkedHashMap<String, Double> solution = null;
	private int verbosity = 0; // 0,1,2,3,4
	
	
	public OrToolsSolver(String mpSolverType) {

		//System.loadLibrary("jnilinearsolver");

		try {
			solver = new MPSolver("testExample", MPSolver.getSolverEnum(mpSolverType));
			verbosity = 0;

		} catch (java.lang.ClassNotFoundException e) {
			throw new java.lang.Error(e);
		} catch (java.lang.NoSuchFieldException e) {
			System.err.println("Could not create solver " + mpSolverType);
			solver = null;
		} catch (java.lang.IllegalAccessException e) {
			throw new java.lang.Error(e);
		}

	}

	public static void initialize() {
		
		System.loadLibrary("jnilinearsolver");
		
	}
	
	public void setModel(MPModel m) {

		model = m;

		solver.clear();

		// add var into solver
		_solverVarMap = new LinkedHashMap<String, MPVariable>();

		for (String varName : m.varMap_number.keySet()) {
			double lb = m.varMap_number.get(varName)[0];
			double ub = m.varMap_number.get(varName)[1];
			_solverVarMap.put(varName, solver.makeNumVar(lb, ub, varName));
		}
		for (String varName : m.varMap_integer.keySet()) {
			double lb = m.varMap_integer.get(varName)[0];
			double ub = m.varMap_integer.get(varName)[1];
			_solverVarMap.put(varName, solver.makeIntVar(lb, ub, varName));
		}

		// add obj into solver
		for (String key : m.objFunction.keySet()) {
			
			if (!_solverVarMap.keySet().contains(key)) {
				_solverVarMap.put(key, solver.makeNumVar(0, Param.inf, key));
			}
			solver.setObjectiveCoefficient(_solverVarMap.get(key), m.objFunction.get(key));
		}

		// add constraints into solver
		_solverConstraintMap = new LinkedHashMap<String, MPConstraint>();

		for (String constraintName : m.constraintRhs.keySet()) {

			double lb = m.constraintRhs.get(constraintName)[0];
			double ub = m.constraintRhs.get(constraintName)[1];

			_solverConstraintMap.put(constraintName, solver.makeConstraint(lb, ub, constraintName));

			for (String vName : m.constraintLhs.get(constraintName).keySet()) {

				_solverConstraintMap.get(constraintName).setCoefficient(_solverVarMap.get(vName), m.constraintLhs.get(constraintName).get(vName));

			}
		}
	}

	public void refreshObjFunc(LinkedHashMap<String, Double> newObjFunc) {

		//model.objFunction = newObjFunc;

		solver.clearObjective();

		// add obj into solver
		for (String key : newObjFunc.keySet()) {
			solver.setObjectiveCoefficient(_solverVarMap.get(key), newObjFunc.get(key));
		}

	}

	public void setVerbose(int verbosity) {

		this.verbosity = verbosity;
	}

	public int solve(MPModel m) {

		setModel(m);

		return solve();

	}

	public int solve() {

		solver.setMaximization();

		int resultStatus = solver.solve();

		// Check that the problem has an optimal solution.
		if (resultStatus != MPSolver.OPTIMAL) {
			System.err.println("# Error: The problem does not have an optimal solution!");
			return resultStatus;
		}

		// objective value.
		if (verbosity > 0)
			System.out.println("Optimal objective value = " + solver.objectiveValue());

		// post solution.
		solution = new LinkedHashMap<String, Double>();

		SortedSet<String> sortedkeys = new TreeSet<String>(_solverVarMap.keySet());

		for (String varName : sortedkeys) {

			double v = _solverVarMap.get(varName).solutionValue();
			if (verbosity > 1)
				System.out.println(varName + " = " + v);
			solution.put(varName, v);
		}

		return resultStatus;

	}

	public void delete() {

		solver.delete();

	}

	// for wrims only
	public MPModel createModel() {
		
		model = new MPModel("wrims");
		// create model
		Misc.setDVars(model);
		Misc.setConstraints(model);
		Misc.setWeights(model);
		
		return model;	

	}
	// for wrims only
	public void run() {
		
		int modelStatus = solve(model);
		
		if (modelStatus != MPSolver.OPTIMAL) {
			Misc.getSolverInformation(modelStatus);
		}
		else {			
			// post solution.
			solution = new LinkedHashMap<String, Double>();
			SortedSet<String> sortedkeys = new TreeSet<String>(_solverVarMap.keySet());
			for (String varName : sortedkeys) {
				double v = _solverVarMap.get(varName).solutionValue();
				if (verbosity > 1)
					System.out.println(varName + " = " + v);
				solution.put(varName, v);
			}
			
			// assign dvar
			Misc.assignDvar(solution); 
		}
		
	}
	
}
