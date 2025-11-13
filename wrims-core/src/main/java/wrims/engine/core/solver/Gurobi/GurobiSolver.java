package wrims.engine.core.solver.Gurobi;

import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

import wrims.engine.core.commondata.solverdata.SolverData;
import wrims.engine.core.commondata.wresldata.Dvar;
import wrims.engine.core.commondata.wresldata.StudyDataSet;
import wrims.engine.core.commondata.wresldata.WeightElement;
import wrims.engine.core.commondata.solverdata.*;
import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.FilePaths;
import wrims.engine.core.components.IntDouble;
import wrims.engine.core.components.Error;
import wrims.engine.core.evaluator.DataTimeSeries;
import wrims.engine.core.evaluator.DssOperation;
import wrims.engine.core.evaluator.EvalConstraint;

public class GurobiSolver {
	
	int modelStatus;
	static GRBEnv    env;
	static GRBModel  model;
	public static Map <String, Double> varDoubleMap;
	
	Map<String, GRBVar> varMap = new HashMap <String, GRBVar> ();
	
	public static void initialize(){
		
	    try {
	    	//env   = new GRBEnv("TestGurobi.log");
	    	env   = new GRBEnv();
	    	env.set(GRB.IntParam.LogToConsole, 0);
	    	//env.set(GRB.IntParam.Presolve, 0);
		}
		catch (GRBException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void setLp(String CplexLpFilePath) {
		
	    try {
	    	//env   = new GRBEnv("TestGurobi.log");
	    	//env   = new GRBEnv();
	    	//env.set(GRB.IntParam.LogToConsole, 0);
		    model = new GRBModel(env, CplexLpFilePath);
		}
		catch (GRBException e) {
			Error.addSolvingError("File not found: "+CplexLpFilePath);
			//e.printStackTrace();
		}
		
	}
	
	public static void dispose(){
		
	    try {
			model.dispose();
			//env.release(); // release license
			env.dispose();
		}
		catch (GRBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public GurobiSolver() throws GRBException{	
		
		setDVars();
		setConstraints();
		model.optimize();	
		assignDvar();
		Output();
	
	}
	
	public static void solve() {

		LpResult result = new LpResult();
		
	    try {

	      model.optimize();

	      int optimstatus = model.get(GRB.IntAttr.Status);
	      result.status = model.get(GRB.IntAttr.Status);
	      
	      if (optimstatus == GRB.Status.INF_OR_UNBD) {
	        model.getEnv().set(GRB.IntParam.Presolve, 0);
		//model.getEnv().set(GRB.FloatParam.FeasibilityTol, 0.001);
	        model.optimize();
	        optimstatus = model.get(GRB.IntAttr.Status);
	        result.status = model.get(GRB.IntAttr.Status);
	        //reset env
	    	env   = new GRBEnv();
	    	env.set(GRB.IntParam.LogToConsole, 0);
	      }

	      if (optimstatus == GRB.Status.OPTIMAL) {
	    	  
	        double objval = model.get(GRB.DoubleAttr.ObjVal);
	        //System.out.println("Optimal objective: " + objval);
	        
	        
	        GRBVar[] allVars = model.getVars();
	      //  String[] allVarNames = model.get(GRB.StringAttr.VarName,allVars);
	      //  double[] allValues = model.get(GRB.DoubleAttr.X,allVars);

	        result.varNames = model.get(GRB.StringAttr.VarName,allVars);
	        result.varValues = model.get(GRB.DoubleAttr.X,allVars);

			ControlData.gurobi_objective = objval;
	        collectDvar(result);
	        assignDvar();
	              
	      } else if (optimstatus == GRB.Status.INFEASIBLE) {
	    	  
	    	  Error.addSolvingError("Model is infeasible"); 
	          //System.out.println("Model is infeasible");

	        // Compute and write out IIS
	    	  String IISFilePath = new File(FilePaths.mainDirectory, "Gurobi_infeasible.ilp").getAbsolutePath();
	    	  model.computeIIS();
	    	  model.write(IISFilePath);
	      } else if (optimstatus == GRB.Status.UNBOUNDED) {
	    	  Error.addSolvingError("Model is unbounded"); 
	          //System.out.println("Model is unbounded");
	      } else {
	    	  Error.addSolvingError("Optimization was stopped with status = "
                    + optimstatus);
	      }

	      // Dispose of model 
	      model.dispose();

	    } catch (GRBException e) {

	      	Error.addSolvingError("Error code: " + e.getErrorCode() + ". " +
		          e.getMessage());
	    }
		//return result;
	  }
	
	public void setDVars() throws GRBException{
		Map<String, Dvar> DvarMap = SolverData.getDvarMap();
		Set DvarCollection = DvarMap.keySet();
		Iterator dvarIterator=DvarCollection.iterator();
		
		while(dvarIterator.hasNext()){                          
			String dvarName=(String)dvarIterator.next();
			Dvar dvar=DvarMap.get(dvarName);
			double testWeight=0;
			
			Map<String, WeightElement> weightMap = SolverData.getWeightMap();
			Set weightCollection = weightMap.keySet();
			Iterator weightIterator = weightCollection.iterator();
			String weightName=(String)weightIterator.next();
			
			double lb = dvar.lowerBoundValue.doubleValue();
			double ub = dvar.upperBoundValue.doubleValue();
			
			if (weightMap.containsKey(dvarName)) {
				double weight=-weightMap.get(dvarName).getValue();
				GRBVar gv = model.addVar(lb, ub, weight, GRB.CONTINUOUS, dvarName);
				varMap.put(dvarName, gv);
				
				testWeight = weight;
			}else{
				GRBVar VarName = model.addVar(lb, ub, 0, GRB.CONTINUOUS, dvarName);
				varMap.put(dvarName, VarName);
			}
			model.update();
		}
	}

	private void setConstraints() throws GRBException {
		Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
		Set constraintCollection = constraintMap.keySet();
		Iterator constraintIterator = constraintCollection.iterator();
		
		while(constraintIterator.hasNext()){                          
			String constraintName=(String)constraintIterator.next();
			EvalConstraint ec=constraintMap.get(constraintName);
			HashMap<String, IntDouble> multMap = ec.getEvalExpression().getMultiplier();
			Set multCollection = multMap.keySet();
			Iterator multIterator = multCollection.iterator();
			GRBLinExpr expr = new GRBLinExpr();
			double [] jack = new double[multCollection.size()+1];
			int counter = 1;
			
			while(multIterator.hasNext()){
				String multName=(String)multIterator.next();
				double coef=multMap.get(multName).getData().doubleValue();
				GRBVar var=varMap.get(multName);
				expr.addTerm(coef, var);
				jack[counter] = coef;
				counter++;
			}
			if (ec.getSign().equals("=")) {
				model.addConstr(expr, GRB.EQUAL, -ec.getEvalExpression().getValue().getData().doubleValue(), constraintName); 			}
			else if (ec.getSign().equals("<") || ec.getSign().equals("<=")){
				model.addConstr(expr, GRB.LESS_EQUAL, -ec.getEvalExpression().getValue().getData().doubleValue(), constraintName); 
			}
			else if (ec.getSign().equals(">") || ec.getSign().equals(">=")){
				model.addConstr(expr, GRB.GREATER_EQUAL, -ec.getEvalExpression().getValue().getData().doubleValue(), constraintName); 
			}
		}
	}
	
	public static void assignDvar() {
		Map<String, Map<String, IntDouble>> varCycleValueMap=ControlData.currStudyDataSet.getVarCycleValueMap();
		Map<String, Map<String, IntDouble>> varTimeArrayCycleValueMap=ControlData.currStudyDataSet.getVarTimeArrayCycleValueMap();
		Set<String> dvarUsedByLaterCycle = ControlData.currModelDataSet.dvarUsedByLaterCycle;
		Set<String> dvarTimeArrayUsedByLaterCycle = ControlData.currModelDataSet.dvarTimeArrayUsedByLaterCycle;
		ArrayList<String> timeArrayDvList = ControlData.currModelDataSet.timeArrayDvList;
		String model=ControlData.currCycleName;
		
		StudyDataSet sds = ControlData.currStudyDataSet;
		ArrayList<String> varCycleIndexList = sds.getVarCycleIndexList();
		ArrayList<String> dvarTimeArrayCycleIndexList = sds.getDvarTimeArrayCycleIndexList();
		Map<String, Map<String, IntDouble>> varCycleIndexValueMap = sds.getVarCycleIndexValueMap();
				
		Map<String, Dvar> dvarMap = SolverData.getDvarMap();
		Set dvarCollection = dvarMap.keySet();
		Iterator dvarIterator = dvarCollection.iterator();
			
		while(dvarIterator.hasNext()){ 
			String dvName=(String)dvarIterator.next();
			Dvar dvar=dvarMap.get(dvName);
			
			double value = -77777777;
			try {
				value=varDoubleMap.get(dvName);
			} catch (Exception e) {
				//value = 0;  // TODO: warning!! needs work here!!
				
				//System.out.println(" This dvName not found: "+ dvName);
				//continue;
				try {
					value = (Double) dvar.getData().getData(); // use whatever is in the container.
				} catch (Exception e2) {
					value=-77777777; // TODO: if this value is used, then this is probably an error in the wresl code. need to give warning.
				}
			}
			IntDouble id=new IntDouble(value,false);
			dvar.setData(id);
			if(dvarUsedByLaterCycle.contains(dvName)){
				varCycleValueMap.get(dvName).put(model, id);
			}else if (dvarTimeArrayUsedByLaterCycle.contains(dvName)){
				if (varTimeArrayCycleValueMap.containsKey(dvName)){
					varTimeArrayCycleValueMap.get(dvName).put(model, dvar.data);
				}else{
					Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
					cycleValue.put(model, dvar.data);
					varTimeArrayCycleValueMap.put(dvName, cycleValue);
				}
			}
			if (varCycleIndexList.contains(dvName) || dvarTimeArrayCycleIndexList.contains(dvName)){
				if (varCycleIndexValueMap.containsKey(dvName)){
					varCycleIndexValueMap.get(dvName).put(model, dvar.data);
				}else{
					Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
					cycleValue.put(model, dvar.data);
					varCycleIndexValueMap.put(dvName, cycleValue);
				}
			}
			String entryNameTS=DssOperation.entryNameTS(dvName, ControlData.timeStep);
			DataTimeSeries.saveDataToTimeSeries(dvName, entryNameTS, value, dvar);
			if (timeArrayDvList.contains(dvName)){
				entryNameTS=DssOperation.entryNameTS(dvName+"__fut__0", ControlData.timeStep);
				DataTimeSeries.saveDataToTimeSeries(entryNameTS, value, dvar, 0);
			}
		}
		
		if (ControlData.showRunTimeMessage) {
			System.out.println("Objective Value: "+ControlData.lpsolve_objective);
			System.out.println("Assign Dvar Done.");
		}
	}

	private static void collectDvar(LpResult lpResult){
		
		//Map<String, Dvar> dvarMap=SolverData.getDvarMap();
		varDoubleMap = new HashMap<String, Double>();
		
		for (int i = 0; i < lpResult.varValues.length; i++) {
			
			//System.out.println(lpResult.varNames[i]+":"+lpResult.varValues[i]);
			varDoubleMap.put(lpResult.varNames[i], lpResult.varValues[i]);
			
			// TODO: add the following line before sending the problem to the solver using direct link. 
			// it's too late here. need to assign value.
			//if (!dvarMap.containsKey(lpResult.varNames[i])) addConditionalSlackSurplusToDvarMap(dvarMap, lpResult.varNames[i]);
			
	    }		
	
	}

	private void checkStatus() throws GRBException {
	      int status = model.get(GRB.IntAttr.Status);
	      if (status == GRB.Status.UNBOUNDED) {
	        System.out.println("The model cannot be solved "
	            + "because it is unbounded");
	        return;
	      }
	      if (status == GRB.Status.OPTIMAL) {
	        System.out.println("The optimal objective is " +
	            model.get(GRB.DoubleAttr.ObjVal));
	        return;
	      }
	      if (status != GRB.Status.INF_OR_UNBD && 
	          status != GRB.Status.INFEASIBLE    ) {
	        System.out.println("Optimization was stopped with status " + status);
	        return;
	      }

	      // do IIS
	      System.out.println("The model is infeasible; computing IIS");
	      LinkedList<String> removed = new LinkedList<String>();

	      // Loop until we reduce to a model that can be solved
	      while (true) {
	        model.computeIIS();
	        System.out.println("\nThe following constraint cannot be satisfied:");
	        for (GRBConstr c : model.getConstrs()) {
	          if (c.get(GRB.IntAttr.IISConstr) == 1) {
	            System.out.println(c.get(GRB.StringAttr.ConstrName));
	            // Remove a single constraint from the model
	            removed.add(c.get(GRB.StringAttr.ConstrName));
	            model.remove(c);
	            break;
	          }
	        }

	        System.out.println();
	        model.optimize();
	        status = model.get(GRB.IntAttr.Status);

	        if (status == GRB.Status.UNBOUNDED) {
	          System.out.println("The model cannot be solved "
	              + "because it is unbounded");
	          return;
	        }
	        if (status == GRB.Status.OPTIMAL) {
	          break;
	        }
	        if (status != GRB.Status.INF_OR_UNBD &&
	            status != GRB.Status.INFEASIBLE    ) {
	          System.out.println("Optimization was stopped with status " +
	              status);
	          return;
	        }
	      }

	      System.out.println("\nThe following constraints were removed "
	          + "to get a feasible LP:");
	      for (String s : removed) {
	        System.out.print(s + " ");
	      }
	      System.out.println();
		}
	
	private void Output() throws GRBException {
		Map<String, Dvar> dvarMap = ControlData.currDvMap;
		Set dvarCollection = dvarMap.keySet();
		Iterator dvarIterator = dvarCollection.iterator();
		
		while(dvarIterator.hasNext()){ 
			String dvName=(String)dvarIterator.next();
			Dvar dvar=dvarMap.get(dvName);
			System.out.print(dvName + ": " + dvar.getData().getData() +"\n");
		}
		
		System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));
	}
	public static void addConditionalSlackSurplusToDvarMap(Map<String, Dvar> dvarMap, String multName){
		Dvar dvar=new Dvar();
		dvar.upperBoundValue=1.0e23;
		dvar.lowerBoundValue=0.0;
		dvarMap.put(multName, dvar);
	}
}
