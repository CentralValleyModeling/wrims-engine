package wrimsv2.evaluator;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;

import wrimsv2.commondata.solverdata.SolverData;
import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.Param;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.components.ControlData;
import wrimsv2.components.Error;
import wrimsv2.components.IntDouble;
import wrimsv2.external.*;
import wrimsv2.hdf5.HDF5Reader;
import wrimsv2.parallel.ParallelVars;
import wrimsv2.solver.CbcSolver;

import java.time.Duration;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;
import java.util.Date;

public class Evaluation {
	public static double convertStringToDouble(String text){
		return Double.valueOf(text);
	}
	
	public static int convertStringToInt(String text){
		return Integer.valueOf(text);
	}
	
	public static String convertDoubleToString(double value){
		return Double.toString(value);
	}
	
	public static String convertIntToString(int value){
		return Integer.toString(value);
	}
	
	public static boolean relationStatement(EvalExpression ee1, EvalExpression ee2, String relation){
		if (!ee1.isNumeric() || !ee2.isNumeric()){
			Error.addEvaluationError("Decision variable can't be used in define condition");
		}
		
		double value1=ee1.getValue().getData().doubleValue();
		double value2=ee2.getValue().getData().doubleValue();
		
		if (relation.equals("==")) {
			if (ControlData.solverName.equalsIgnoreCase("Cbc") && CbcSolver.cbcSolutionRounding){
				if (value1>=value2-ControlData.relationTolerance && value1<=value2+ControlData.relationTolerance){
					return true;
				}else{
					return false;
				}
			}else{
				if (value1==value2){
					return true;
				}else{
					return false;
				}
			}
		}else if (relation.equals(">")){
			if (value1>value2){
				return true;
			}else{
				return false;
			}
		}else if (relation.equals("<")){
			if (value1<value2){
				return true;
			}else{
				return false;
			}
		}else if (relation.equals(">=")){
			if (ControlData.solverName.equalsIgnoreCase("Cbc") && CbcSolver.cbcSolutionRounding){
				if (value1>=value2-ControlData.relationTolerance){
					return true;
				}else{
					return false;
				}
			}else{
				if (value1>=value2){
					return true;
				}else{
					return false;
				}
			}
		}else{
			if (ControlData.solverName.equalsIgnoreCase("Cbc") && CbcSolver.cbcSolutionRounding){
				if (value1<=value2+ControlData.relationTolerance){
					return true;
				}else{
					return false;
				}
			}else{
				if (value1<=value2){
					return true;
				}else{
					return false;
				}
			}
		}
	}
	
	public static boolean range(String m1, String m2){
		return TimeOperation.range(ControlData.currMonth, m1, m2);
	}
	
	public static boolean relationStatementSeries(boolean r1, boolean r2, String s){
		if (s.equals(".and.")) {
			return (r1 && r2);
		}else{
			return (r1 || r2);
		}
	}
	
	public static EvalConstraint constraintStatement (EvalExpression ee1, EvalExpression ee2, String s) {
		EvalExpression ee=substract(ee1, ee2);
		EvalConstraint ec=new EvalConstraint();
		ec.setEvalExpression(ee);
		ec.setSign(s);
		return ec;
	}
	
	public static EvalExpression term_knownTS(IntDouble result){
		return new EvalExpression(result);
	}
	
	public static EvalExpression term_IDENT (String ident, Stack<LoopIndex> sumIndex){
		if (sumIndex.size()>0){
			LoopIndex li=sumIndex.pop();
			if (li.getName().equals(ident) && li.getIndexStart()){
				sumIndex.push(li);
				EvalExpression ee = new EvalExpression();
				IntDouble id = new IntDouble(li.getValue(),true, ident, 0);
				ee.setValue(id);
				return ee;
			}
			sumIndex.push(li);
		}
		if (ControlData.currSvMap.containsKey(ident)){
			EvalExpression ee=new EvalExpression();
			IntDouble id0 = ControlData.currSvMap.get(ident).getData();
			IntDouble id1 = new IntDouble(id0.getData(), id0.isInt(), ident, 0);
			ee.setValue(id1);
			return ee;
		}else if (ControlData.currTsMap.containsKey(ident)){
			EvalExpression ee=new EvalExpression();
			IntDouble id0 = ControlData.currTsMap.get(ident).getData();
			IntDouble id1 = new IntDouble(id0.getData(), id0.isInt(), ident, 0);
			ee.setValue(id1);
			return ee;
		}else if (ControlData.isPostProcessing && ControlData.currDvMap.containsKey(ident)){
			EvalExpression ee=new EvalExpression();
			IntDouble id0 = ControlData.currDvMap.get(ident).getData();
			IntDouble id1 = new IntDouble(id0.getData(), id0.isInt(), ident, 0);
			ee.setValue(id1);
			return ee;
		}else if (ControlData.isPostProcessing && ControlData.currAliasMap.containsKey(ident)){
			EvalExpression ee=new EvalExpression();
			IntDouble id0 = ControlData.currAliasMap.get(ident).getData();
			if (id0==null) {
				Error.addEvaluationError(ident+" is not defined before it is used.");
				IntDouble id1=new IntDouble(1.0, false, ident, 0);
				ee.setValue(id1);
				return ee;
			}
			IntDouble id1 = new IntDouble(id0.getData(), id0.isInt(), ident, 0);
			ee.setValue(id1);
			return ee;
		}else if (!ControlData.isPostProcessing && ControlData.currAliasMap.containsKey(ident) && !ControlData.currDvMap.containsKey(ident)){
			EvalExpression ee=new EvalExpression();
			IntDouble id0=new IntDouble(1.0, false, ident, 0);
			ee.setValue(id0);
			Error.addEvaluationError("Alias "+ident+" should not appear in constraint. ");
			return ee;
		} else if (ControlData.parameterMap.containsKey(ident)){
			EvalExpression ee=new EvalExpression();
			IntDouble id0=ControlData.parameterMap.get(ident).getData();
			ee.setValue(id0);
			return ee;	
		} else if (!ControlData.isPostProcessing && !ControlData.currAliasMap.containsKey(ident) && !ControlData.currDvMap.containsKey(ident) && !ControlData.currDvSlackSurplusMap.containsKey(ident)){
			EvalExpression ee=new EvalExpression();
			IntDouble id0=new IntDouble(1.0, false, ident, 0);
			ee.setValue(id0);
			Error.addEvaluationError(ident+" is not defined before used.");
			return ee;				
		}
		
		EvalExpression ee=new EvalExpression();
		IntDouble id0 = new IntDouble (0, true, ident, 0);
		ee.setValue(id0);
		HashMap<String, IntDouble> multiplier=ee.getMultiplier();
		IntDouble id;
		if (ControlData.currDvMap.containsKey(ident)){
			Dvar dv = ControlData.currDvMap.get(ident);
			if (dv.integer.equals(Param.yes)){
				id= new IntDouble(1,true, ident, 0);
			}else{
				id= new IntDouble(1.0,false, ident, 0);
			}
		}else{
			id= new IntDouble(1.0,false, ident, 0);
		}
		multiplier.put(ident, id);
		return ee;
	}
	
	public static EvalExpression term_SVAR (String ident){
		IntDouble data;
		if (!ControlData.currSvMap.containsKey(ident)){
			if (!ControlData.currTsMap.containsKey(ident)){
				Error.addEvaluationError("State variable "+ident+" is not defined before used.");
				IntDouble id=new IntDouble(1.0, false, ident, 0);
				return new EvalExpression(id);
			}else{
				data=ControlData.currTsMap.get(ident).getData();
			}
		}else{
			data=ControlData.currSvMap.get(ident).getData();
		}
		
		if (data == null){
			Error.addEvaluationError("The value of state variable "+ident+" is not defined before used.");
			IntDouble id=new IntDouble(1.0, false, ident, 0);
			return new EvalExpression(id);
		}
		return new EvalExpression(new IntDouble(data.getData(), data.isInt(), ident, 0));
	}
	
	public static EvalExpression term_INTEGER (String integer){
		IntDouble id = new IntDouble(convertStringToInt(integer), true);
		return new EvalExpression(id);
	}
	
	public static EvalExpression term_FLOAT (String floatValue){
		IntDouble id = new IntDouble(convertStringToDouble(floatValue), false);
		return new EvalExpression(id);
	}
	
	public static EvalExpression unary (String s, EvalExpression ee){
		if (s !=null && s.equals("-")){
			if (ee.getValue().isInt()){
				int value=-ee.getValue().getData().intValue();
				ee.getValue().setData(value);
			}else{
				double value=-ee.getValue().getData().doubleValue();
				ee.getValue().setData(value);
			}
			Map<String, IntDouble> multiplier=ee.getMultiplier();
			for (String dvar : multiplier.keySet()) {
				IntDouble id=multiplier.get(dvar);
				if (id.isInt()){
					id.setData(-id.getData().intValue());
				}else{
					id.setData(-id.getData().doubleValue());
				}
				multiplier.put(dvar, id);				
			}
		}
		return ee;
	}
	
	public static EvalExpression mult(EvalExpression ee1, EvalExpression ee2){
		if (ee1.isNumeric()){
			IntDouble id1=ee1.getValue();
			IntDouble id2=ee2.getValue();
			ee2.setValue(multiplyOperation(id1,id2));
			Map<String, IntDouble> multiplier=ee2.getMultiplier();
			Set<String> keySet=multiplier.keySet();
			Iterator iterator=(Iterator) keySet.iterator();
			while (iterator.hasNext()) {
				String dvar=(String) iterator.next();
				IntDouble id3=multiplyOperation(id1,multiplier.get(dvar));
				if (id3.getData().doubleValue()==0.0){
					iterator.remove();
				}else{
					multiplier.put(dvar, id3);
				}
			}
			return ee2;
		}else{
			if (ee2.isNumeric()){
				IntDouble id2=ee2.getValue();
				IntDouble id1=ee1.getValue();
				ee1.setValue(multiplyOperation(id2,id1));
				Map<String, IntDouble> multiplier=ee1.getMultiplier();
				Set<String> keySet=multiplier.keySet();
				Iterator iterator=(Iterator) keySet.iterator();
				while (iterator.hasNext()) {
					String dvar=(String) iterator.next();
					IntDouble id3=multiplyOperation(id2,multiplier.get(dvar));
					if (id3.getData().doubleValue()==0.0){
						iterator.remove();
					}else{
						multiplier.put(dvar, id3);
					}			
				}
				return ee1;
			}else{
				Error.addEvaluationError("Decision variable multiply decision variable appears. The problem is not linear.");
				return ee1;
			}
		}
	}
	
	public static IntDouble multiplyOperation(IntDouble id1, IntDouble id2){
		IntDouble id;
		if (id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().intValue()*id2.getData().intValue(), true);
		}else if (id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().intValue()*id2.getData().doubleValue(), false);
		}else if (!id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()*id2.getData().intValue(), false);
		}else{
			id=new IntDouble(id1.getData().doubleValue()*id2.getData().doubleValue(), false);
		}
		return id;
	}
	
	public static EvalExpression divide(EvalExpression ee1, EvalExpression ee2){
		if (ee2.isNumeric()){
			IntDouble id2=ee2.getValue();
			IntDouble id1=ee1.getValue();
			if (id2.getData().doubleValue() ==0.0){
				if (id1.getData().doubleValue()==0.0 && ee1.isNumeric()){
					return ee1;
				}else{
					Error.addEvaluationError("0.0 appears in divisor");
				}
				return ee1;
			}
			ee1.setValue(divideOperation(id1,id2));
			Map<String, IntDouble> multiplier=ee1.getMultiplier();
			for (String dvar : multiplier.keySet()) {
				multiplier.put(dvar, divideOperation(multiplier.get(dvar),id2));				
			}
			return ee1;
		}else{
			Error.addEvaluationError("Decision variable appears in divisor. The problem is not linear.");
			return ee1;
		}
	}
	
	public static IntDouble divideOperation(IntDouble id1, IntDouble id2){
		IntDouble id;
		if (id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().intValue()/id2.getData().intValue(), true);
		}else if (id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().intValue()/id2.getData().doubleValue(), false);
		}else if (!id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()/id2.getData().intValue(), false);
		}else{
			id=new IntDouble(id1.getData().doubleValue()/id2.getData().doubleValue(), false);
		}
		return id;		
	}
	
	public static EvalExpression mod(EvalExpression ee1, EvalExpression ee2){
		if (ee1.isNumeric() && ee2.isNumeric()){
			IntDouble id1=ee1.getValue();
			IntDouble id2=ee2.getValue();
			ee1.setValue(modOperation(id1,id2));			
			return ee1;
		}else{
			Error.addEvaluationError("Decision variable appears in MOD calcualtion. The problem is not linear.");
			return ee1;
		}
	}
	
	public static IntDouble modOperation(IntDouble id1, IntDouble id2){
		IntDouble id;
		if (id2.getData().doubleValue()==0.0){
			Error.addEvaluationError("Mod function uses 0 as divider.");
			return new IntDouble(1.0, false);
		}
		if (id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().intValue()%id2.getData().intValue(), true);
		}else if (id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().intValue()%id2.getData().doubleValue(), false);
		}else if (!id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()%id2.getData().intValue(), false);
		}else{
			id=new IntDouble(id1.getData().doubleValue()%id2.getData().doubleValue(), false);
		}
		return id;		
	}
	
	public static EvalExpression round(EvalExpression ee1){
		if (ee1.isNumeric()){
			IntDouble id1=ee1.getValue();
			ee1.setValue(ValueEvaluation.round(id1));			
			return ee1;
		}else{
			Error.addEvaluationError("The arguement for the rounded variable is not numeric.");
			return ee1;
		}
	}
	
	public static EvalExpression add(EvalExpression ee1, EvalExpression ee2){
		IntDouble id1=ee1.getValue();
		IntDouble id2=ee2.getValue();
		ee1.setValue(addOperation(id1,id2));
		Map<String, IntDouble> multiplier1=ee1.getMultiplier();
		Map<String, IntDouble> multiplier2=ee2.getMultiplier();
		for (String dvar : multiplier2.keySet()) {
			if (multiplier1.containsKey(dvar)){
				IntDouble id3=addOperation(multiplier1.get(dvar),multiplier2.get(dvar));
				if (id3.getData().doubleValue()==0.0){
					multiplier1.remove(dvar);
				}else{
					multiplier1.put(dvar, id3);
				}
			}else{
				multiplier1.put(dvar, multiplier2.get(dvar));
			}
		}
		return ee1;
	}
	
	public static IntDouble addOperation(IntDouble id1, IntDouble id2){
		IntDouble id;
		if (id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().intValue()+id2.getData().intValue(), true);
		}else if (id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().intValue()+id2.getData().doubleValue(), false);
		}else if (!id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()+id2.getData().intValue(), false);
		}else{
			id=new IntDouble(id1.getData().doubleValue()+id2.getData().doubleValue(), false);
		}
		return id;		
	}
	
	public static EvalExpression substract(EvalExpression ee1, EvalExpression ee2){
		IntDouble id1=ee1.getValue();
		IntDouble id2=ee2.getValue();
		ee1.setValue(substractOperation(id1,id2));
		Map<String, IntDouble> multiplier1=ee1.getMultiplier();
		Map<String, IntDouble> multiplier2=ee2.getMultiplier();
		for (String dvar : multiplier2.keySet()) {
			if (multiplier1.containsKey(dvar)){
				IntDouble id3=substractOperation(multiplier1.get(dvar),multiplier2.get(dvar));
				if (id3.getData().doubleValue()==0.0){
					multiplier1.remove(dvar);
				}else{
					multiplier1.put(dvar, id3);
				}
			}else{
				IntDouble id0=new IntDouble(0,true);
				multiplier1.put(dvar, substractOperation(id0,multiplier2.get(dvar)));
			}
		}
		return ee1;
	}
	
	public static IntDouble substractOperation(IntDouble id1, IntDouble id2){
		IntDouble id;
		if (id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().intValue()-id2.getData().intValue(), true);
		}else if (id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().intValue()-id2.getData().doubleValue(), false);
		}else if (!id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()-id2.getData().intValue(), false);
		}else{
			id=new IntDouble(id1.getData().doubleValue()-id2.getData().doubleValue(), false);
		}
		return id;		
	}
	
	public static EvalExpression noArgFunction(String ident){
		Class function;
		IntDouble result;
		try {
			function = Class.forName("wrimsv2.external.Function"+ident);
		
			Stack stack = new Stack();

			ExternalFunction ef;
			if (ControlData.allExternalFunctionMap.containsKey(ident)){
				ef=ControlData.allExternalFunctionMap.get(ident);
			}else{
				ef = (ExternalFunction)function.newInstance();
				ControlData.allExternalFunctionMap.put(ident, ef);
			}
			ef.execute(stack);
			String valueString=stack.pop().toString();
			
			if (valueString.contains(".")){       
				result = new IntDouble(Double.parseDouble(valueString), false);
				return new EvalExpression(result);
			}else{
				result = new IntDouble(Integer.parseInt(valueString), true);
				return new EvalExpression(result);
			}
			
		} catch (Exception e) {
			Error.addEvaluationError("The function " +ident+" has an error.");
			e.printStackTrace();
			result=new IntDouble (1.0,false);
			return new EvalExpression(result);
		}
	}
	
	public static EvalExpression argFunction(String ident, ArrayList<ArrayList<EvalExpression>> eeArray, Stack<LoopIndex> sumIndex){
		IntDouble result;
		if (eeArray.size()==1){
			if (ControlData.currSvMap.containsKey(ident)||ControlData.currTsMap.containsKey(ident)||ControlData.currDvMap.containsKey(ident)||ControlData.currAliasMap.containsKey(ident)||ControlData.currDvSlackSurplusMap.containsKey(ident)) {
				ArrayList<EvalExpression> eeArray1 = eeArray.get(0);
				if (eeArray1.size()==1){
					String idName = eeArray1.get(0).getValue().getName();
					for (int k=0; k<12; k++){
						if (idName.equals(TimeOperation.month_const[k])){
							Error.addEvaluationError(idName+" can't be used in "+ident+"("+idName+")");
							return new EvalExpression(new IntDouble (1.0, false));
						}
					}
					return getTimeSeries(ident, eeArray1, sumIndex);
				}else{
					Error.addEvaluationError("Variable "+ident+" has number of indexes different from 1.");
					return new EvalExpression(new IntDouble (1.0, false));
				}
			}
		}
			
		Class function;
		try {
			function = Class.forName("wrimsv2.external.Function"+ident);
		
			Stack stack = new Stack();
			for (int i=0; i<eeArray.size(); i++){
				ArrayList<EvalExpression> eeArray1=eeArray.get(i);
				int size=eeArray1.size();
				if (size ==1){
					EvalExpression ee = eeArray1.get(0);
					if (!ee.isNumeric()){
						int ai=i+1;
						Error.addEvaluationError("The function " +ident+" has an unkown argument at argument index of "+ai+".");
						result=new IntDouble (0.0,false);
						return new EvalExpression(result);
					}
			
					IntDouble id=ee.getValue();
					if (id.isInt()){
						int value=id.getData().intValue();
						stack.push(value);
					}else{
						double value=id.getData().doubleValue();
						stack.push(value);
					}
				}else if (size>1){
					Number[] valueArray = new Number[size];
					for (int j=0; j<size; j++){
						EvalExpression ee = eeArray1.get(j);
						if (!ee.isNumeric()){
							int ai=i+1;
							Error.addEvaluationError("The function " +ident+" has an unkown argument at argument index of "+ai+".");
							return new EvalExpression(new IntDouble (0.0,false));
						}
						valueArray[j]=ee.getValue().getData();
					}
					stack.push(valueArray);
				}else{
					int ai=i+1;
					Error.addEvaluationError("The No. "+ai+" argument of function "+ident+" has no data.");	
					return new EvalExpression(new IntDouble (0.0,false));
				}
			}

			ExternalFunction ef;
			if (ControlData.allExternalFunctionMap.containsKey(ident)){
				ef=ControlData.allExternalFunctionMap.get(ident);
			}else{
				ef = (ExternalFunction)function.newInstance();
				ControlData.allExternalFunctionMap.put(ident, ef);
			}
			ef.execute(stack);

			if (stack.size()>1){
				for (int i=0; i<eeArray.size(); i++){
					ArrayList<EvalExpression> eeArray1=eeArray.get(i);
					int size=eeArray1.size();
					if (size ==1){
						EvalExpression ee = eeArray1.get(0);
						if (!ee.isNumeric()){
							int ai=i+1;
							Error.addEvaluationError("The function " +ident+" has an unkown argument at argument index of "+ai+".");
							result=new IntDouble (0.0,false);
							return new EvalExpression(result);
						}
				
						IntDouble id=ee.getValue();
						if (id.isInt()){
							int value=(Integer) stack.pop();
							ValueEvaluation.setSvarIntValue(id, value);
						}else{
							double value=(Double) stack.pop();
							ValueEvaluation.setSvarDoubleValue(id, value);
						}
					}else if (size>1){
						IntDouble id=eeArray1.get(0).getValue();
						if (id.isInt()){
							int[] valueArray = new int[size];
							valueArray=(int[])stack.pop();
							for (int j=0; j<size; j++){
								EvalExpression ee = eeArray1.get(j);
								ValueEvaluation.setSvarIntValue(ee.getValue(), valueArray[j]);
							}
						}else{
							double[] valueArray = new double[size];
							valueArray=(double[])stack.pop();
							for (int j=0; j<size; j++){
								EvalExpression ee = eeArray1.get(j);
								ValueEvaluation.setSvarDoubleValue(ee.getValue(), valueArray[j]);
							}
						}
					}
				}			
			}
			
			String valueString=stack.pop().toString();
			
			if (valueString.contains(".")){       
				result=new IntDouble(Double.parseDouble(valueString), false);
				return new EvalExpression(result);
			}else{
				result=new IntDouble(Integer.parseInt(valueString), true);
				return new EvalExpression(result);
			}
			
		} catch (Exception e) {
			Error.addEvaluationError("The function " +ident+" has an error.");
			e.printStackTrace();
			result=new IntDouble (1.0,false);
			return new EvalExpression(result);
		}
	}
	
	public static EvalExpression getTimeSeries(String ident, ArrayList<EvalExpression> eeArray, Stack<LoopIndex> sumIndex){
		IntDouble result;
		boolean isSumIndex=false;
		int indexValue=0;
		boolean isIndexStart=true;
		ParallelVars prvs;
		
		EvalExpression ee=eeArray.get(0);	
		
		if (!ee.isNumeric()){
			HashMap<String, IntDouble> multiplier=ee.getMultiplier();
			if (multiplier.size()==1){
				LoopIndex li=sumIndex.pop();
				String indexName=li.getName();
				indexValue=li.getValue();
				isIndexStart=li.getIndexStart();
				sumIndex.push(li);
				if (!(multiplier.containsKey(indexName) && multiplier.get(indexName).getData().doubleValue()==1.0 && ee.getValue().getData().doubleValue()==0.0)){
					Error.addEvaluationError("The index of "+ident+" contains decision variable.");
					result=new IntDouble (0.0,false);
					return new EvalExpression(result);
				}else{
					isSumIndex=true;
				}
			}else{
				Error.addEvaluationError("The index of "+ident+" contains decision variable.");
				result=new IntDouble (0.0,false);
				return new EvalExpression(result);
			}
		}
		
		IntDouble id=ee.getValue();
		if (!id.isInt()){
			Error.addEvaluationError("The index of "+ident+" should be integer.");
			result=new IntDouble (0.0,false);
			return new EvalExpression(result);
		}

		if (isSumIndex){
			if (isIndexStart){
				prvs=TimeOperation.findTime(indexValue);
			}else{
				result=new IntDouble (1.0,false);
				return new EvalExpression(result);
			}
		}else{
			prvs=TimeOperation.findTime(id.getData().intValue());
		}
		
		double value;
		int idValue=id.getData().intValue();
		if (ControlData.currDvMap.containsKey(ident)){
			if (idValue<0){
				value=dvarAliasTimeSeries(ident,idValue, prvs);
			}else if (idValue==0){
				LinkedHashMap<String, IntDouble> multiplier = new LinkedHashMap<String, IntDouble>();
				multiplier.put(ident, new IntDouble(1, true));
				EvalExpression eeResult=new EvalExpression();
				eeResult.setMultiplier(multiplier);
				return eeResult;
			}else{
				String futDvName=ident+"__fut__"+idValue;
				if (SolverData.getDvarMap().containsKey(futDvName)){
					LinkedHashMap<String, IntDouble> multiplier = new LinkedHashMap<String, IntDouble>();
					multiplier.put(futDvName, new IntDouble(1, true));
					EvalExpression eeResult=new EvalExpression();
					eeResult.setMultiplier(multiplier);
					return eeResult;
				}else{
					Error.addEvaluationError("Time Array Dvar "+futDvName+" is used without definition.");
					result=new IntDouble (1.0, false);
					return new EvalExpression(result);
				}
			}
		}else if (ControlData.currDvSlackSurplusMap.containsKey(ident)){
			if (idValue==0){
				LinkedHashMap<String, IntDouble> multiplier = new LinkedHashMap<String, IntDouble>();
				multiplier.put(ident, new IntDouble(1, true));
				EvalExpression eeResult=new EvalExpression();
				eeResult.setMultiplier(multiplier);
				return eeResult;
			}else if (idValue>0){
				String futDvSlackSurplusName=ident+"__fut__"+idValue;
				LinkedHashMap<String, IntDouble> multiplier = new LinkedHashMap<String, IntDouble>();
				multiplier.put(futDvSlackSurplusName, new IntDouble(1, true));
				EvalExpression eeResult=new EvalExpression();
				eeResult.setMultiplier(multiplier);
				return eeResult;
			}else{
				Error.addEvaluationError("Slack surplus index of "+ident+", "+idValue+"<0. THe index has to be larger than 0.");
				result=new IntDouble (1.0,false);
				return new EvalExpression(result);
			}
		}else if (ControlData.currAliasMap.containsKey(ident)){
			value=dvarAliasTimeSeries(ident,idValue, prvs);
		}else{
			if (ControlData.currSvMap.containsKey(ident)){ 
				if (idValue==0)	{
					result = ControlData.currSvMap.get(ident).getData();
					return new EvalExpression(result.copyOf());
				}else if(idValue>0){
					String futSvName=ident+"__fut__"+idValue;
					if (ControlData.currSvFutMap.containsKey(futSvName)){
						result=ControlData.currSvFutMap.get(futSvName).getData();
						return new EvalExpression(result.copyOf());
					}else{
						if (!ControlData.ignoreError) Error.addEvaluationError(futSvName+", the future value of "+ident+" is used before defined.");
						result=new IntDouble (1.0,false);
						return new EvalExpression(result);
					}
				}
			}
			value=svarTimeSeries(ident, idValue, prvs);
		}
		
		result=new IntDouble (value, false);
		return new EvalExpression(result);
	}
	
	public static double svarTimeSeries(String ident, int idValue, ParallelVars prvs){
		int index;
		String entryNameTS=DssOperation.entryNameTS(ident, ControlData.timeStep);
		if (DataTimeSeries.svTS.containsKey(entryNameTS)){
			DssDataSet dds=DataTimeSeries.svTS.get(entryNameTS);
			index =timeSeriesIndex(dds, prvs);
			ArrayList<Double> data=dds.getData();
			if (index>=0 && index<data.size() && index>=dds.getStudyStartIndex()){
				double value=data.get(index);
				if (dds.fromDssFile()){
					if (value != -901.0 && value != -902.0){
						return value;
					}
				}else{
					return value;
				}
			}
		}
		if (DataTimeSeries.svInit.containsKey(entryNameTS)){
			DssDataSet dds=DataTimeSeries.svInit.get(entryNameTS);
			index =timeSeriesIndex(dds, prvs);
			ArrayList<Double> data=dds.getData();
			if (index>=0 && index<data.size()){
				double value=data.get(index);
				if (value !=-901.0){
					return value;
				}
			}
		}else{
			DataTimeSeries.lookInitDss.add(entryNameTS);
			if (getSVInitTimeseries(ident)){
				DssDataSet dds=DataTimeSeries.svInit.get(entryNameTS);
				prvs=TimeOperation.findTime(idValue);
				index =timeSeriesIndex(dds, prvs);
				ArrayList<Double> data=dds.getData();
				if (index>=0 && index<data.size()){
					double value=data.get(index);
					if (value !=-901.0){
						return value;
					}
				}
			}
		}
		if (ControlData.allowSvTsInit && DataTimeSeries.svTS.containsKey(entryNameTS)){
			DssDataSet dds=DataTimeSeries.svTS.get(entryNameTS);
			index =timeSeriesIndex(dds, prvs);
			ArrayList<Double> data=dds.getData();
			if (index>=0 && index<data.size() && index<dds.getStudyStartIndex()){
				double value=data.get(index);
				if (dds.fromDssFile()){
					if (value != -901.0 && value != -902.0){
						return value;
					}
				}else{
					return value;
				}
			}
		}
		Error.addEvaluationError("The data requested for timeseries "+ident+" does not match the entries in the dss file. Please check the name, kind, unit, and requested time period.");
		return 1.0;
	}
	
	public static boolean getSVInitTimeseries(String ident){
		if (ControlData.initHDF5){
			return HDF5Reader.getSVInitTimeseries(ident);
		}else{
			return DssOperation.getSVInitTimeseries(ident);
		}
	}
	
	public static double dvarAliasTimeSeries(String ident, ParallelVars prvs){
		String entryNameTS=DssOperation.entryNameTS(ident, ControlData.timeStep);
		int index;
		long dataTime;
		long startTime;
		long currTime;
		if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
			dataTime=new Date(prvs.dataYear-1900, prvs.dataMonth-1, 1).getTime();
			startTime=new Date(ControlData.startYear-1900, ControlData.startMonth-1, 1).getTime();
			currTime=new Date(ControlData.currYear-1900, ControlData.currMonth-1, 1).getTime();
		}else{
			dataTime=new Date(prvs.dataYear-1900, prvs.dataMonth-1, prvs.dataDay).getTime();
			startTime=new Date(ControlData.startYear-1900, ControlData.startMonth-1, ControlData.startDay).getTime();
			currTime=new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay).getTime();
		}
		
		if (dataTime>=currTime){
			Error.addEvaluationError("The timeseries data for decision variable/alias "+ident+" is not available at or after current simulation period.");
			return 1.0;
		}else if(dataTime>=startTime && dataTime<currTime){
			DssDataSetFixLength dds=DataTimeSeries.dvAliasTS.get(entryNameTS);
			index=timeSeriesIndex(dds, prvs);
			double[] data=dds.getData();
			return data[index];
		}
		
		if (!DataTimeSeries.dvAliasInit.containsKey(entryNameTS)){
			if (!getDVAliasInitTimeseries(ident)){
				Error.addEvaluationError("Initial file doesn't have data for decision vairiable/alias " +ident);
				return 1.0;
			}
		}
		
		DssDataSet dds=DataTimeSeries.dvAliasInit.get(entryNameTS);
		index=timeSeriesIndex(dds, prvs);
		ArrayList<Double> data=dds.getData();
		if (index>=0 && index<data.size()){
			double result=data.get(index);
			if (result==-901.0 || result==-902.0){
				Error.addEvaluationError("Initial file doesn't have data for decision vairiable/alias " +ident);
				return 1.0;
			}
			return result;
		}
		
		Error.addEvaluationError("The data requested for timeseries "+ident+" does not match the entries in the dss file. Please check the name, kind, unit, and requested time period.");
		return 1.0;
	}
	
	public static double dvarAliasTimeSeries(String ident, int indexValue, ParallelVars prvs){
		String entryNameTS=DssOperation.entryNameTS(ident, ControlData.timeStep);
		if (indexValue>0){
			Error.addEvaluationError("Can't access decision variable after the current time step.");
		}
		
		/*
		int index=indexValue+ControlData.currTimeStep.get(ControlData.currCycleIndex);
		if (index>=0){
			if (indexValue>=0 && (ControlData.currEvalTypeIndex==0 || ControlData.currEvalTypeIndex==7)){
				Error.addEvaluationError(ident + " at timestep " +indexValue+" doesn't have value");
				return 1.0;
			}else{
				DssDataSetFixLength dds=DataTimeSeries.dvAliasTS.get(entryNameTS);
				if (dds==null){
					Error.addEvaluationError(ident + " at timestep " +indexValue+" doesn't have value");
					return 1.0;
				}
				double[] data=dds.getData();
				return data[index];
			}
		}
		*/
		
		DssDataSetFixLength ddsfl=DataTimeSeries.dvAliasTS.get(entryNameTS);
		if (ddsfl!=null){
			Date memStartDate = ddsfl.getStartTime();
			Date currDate =  new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
			int index=TimeOperation.getNumberOfTimestep(memStartDate, currDate, ddsfl.getTimeStep())+indexValue-1;
			double[] datafl=ddsfl.getData();
			if (index>=datafl.length){
				Error.addEvaluationError(ident + " at timestep " +indexValue+" doesn't have value");
				return 1.0;
			}else if (index>=0){
				return datafl[index];
			}
		}
		
		if (!DataTimeSeries.dvAliasInit.containsKey(entryNameTS)){
			if (!getDVAliasInitTimeseries(ident)){
				Error.addEvaluationError("Initial file doesn't have data for decision vairiable/alias " +ident);
				return 1.0;
			}
		}

		DssDataSet dds=DataTimeSeries.dvAliasInit.get(entryNameTS);
		int index=timeSeriesIndex(dds, prvs);
		ArrayList<Double> data=dds.getData();
		if (index>=0 && index<data.size()){
			double result=data.get(index);
			if (result==-901.0 || result==-902.0){
				Error.addEvaluationError("Initial file doesn't have data for decision vairiable/alias " +ident);
				return 1.0;
			}
			return result;
		}
		
		Error.addEvaluationError("The data requested for timeseries "+ident+" does not match the entries in the dss file. Please check the name, kind, unit, and requested time period.");
		return 1.0;
	}
	
	public static double dvarAliasCycleTimeSeries(String ident, int indexValue, int ci, ParallelVars prvs){
		String entryNameTS=DssOperation.entryNameTS(ident, ControlData.timeStep);
		if (indexValue>0){
			Error.addEvaluationError("Can't access decision variable after the current time step.");
		}
		
		/*
		int index=indexValue+ControlData.currTimeStep.get(ControlData.currCycleIndex);
		if (index>=0){
			DssDataSetFixLength dds=DataTimeSeries.dvAliasTSCycles.get(ci).get(entryNameTS);
			if (dds==null){
				Error.addEvaluationError(ident + " at timestep " +indexValue+" for the No. "+ci+ " cycle doesn't have value");
				return 1.0;
			}
			double[] data=dds.getData();
			return data[index];
		}
		*/
		
		DssDataSetFixLength ddsfl=DataTimeSeries.dvAliasTSCycles.get(ci).get(entryNameTS);
		if (ddsfl!=null){
			Date memStartDate = ddsfl.getStartTime();
			Date currDate =  new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
			int index=TimeOperation.getNumberOfTimestep(memStartDate, currDate, ddsfl.getTimeStep())+indexValue-1;
			double[] datafl=ddsfl.getData();
			if (index>=datafl.length){
				Error.addEvaluationError(ident + " at timestep " +indexValue+" doesn't have value");
				return 1.0;
			}else if (index>=0){
				return datafl[index];
			}
		}
		
		if (!DataTimeSeries.dvAliasInit.containsKey(entryNameTS)){
			if (!getDVAliasInitTimeseries(ident)){
				Error.addEvaluationError("Initial file doesn't have data for decision vairiable/alias " +ident);
				return 1.0;
			}
		}

		DssDataSet dds=DataTimeSeries.dvAliasInit.get(entryNameTS);
		int index=timeSeriesIndex(dds, prvs);
		ArrayList<Double> data=dds.getData();
		if (index>=0 && index<data.size()){
			double result=data.get(index);
			if (result==-901.0 || result==-902.0){
				Error.addEvaluationError("Initial file doesn't have data for decision vairiable/alias " +ident);
				return 1.0;
			}
			return result;
		}
		
		Error.addEvaluationError("The data requested for timeseries "+ident+" does not match the entries in the dss file. Please check the name, kind, unit, and requested time period.");
		return 1.0;
	}
	
	public static boolean getDVAliasInitTimeseries(String ident){	
		if (ControlData.initHDF5){
			return HDF5Reader.getDVAliasInitTimeseries(ident);
		}else{
			return DssOperation.getDVAliasInitTimeseries(ident);
		}
	}
	
	public static int timeSeriesIndex(DssDataSet dds, ParallelVars prvs){
		Date st=dds.getStartTime();
		long sTime=st.getTime();
		int sYear=st.getYear()+1900;
		int sMonth=st.getMonth()+1; //HEC DSS7 uses getMonth()+1. However, Vista/HecDSS6 uses getMonth() because dss data store at 24:00 Jan31, 1921 is considered to store at 0:00 Feb 1, 1921
		Date dataDate=new Date(prvs.dataYear-1900, prvs.dataMonth-1, prvs.dataDay);
		int index;
		if (TimeOperation.isMonthlyInterval(dds.getTimeStep())){
			index=prvs.dataYear*12+prvs.dataMonth-(sYear*12+sMonth);
		}else{
			//double indexValue=(dataTime-sTime)/(1000*60*60*24);
			Calendar c1=Calendar.getInstance();
			c1.setTime(st);
			Calendar c2=Calendar.getInstance();
			c2.setTime(dataDate);
			long indexValue = Duration.between(c1.toInstant(), c2.toInstant()).toDays();
			index=(int)indexValue+1;  //HEC DSS7 uses indexValue+1; Vista/Hec DSS6 uses indexValue+2
		}
		return index;
	}
	
	public static int timeSeriesIndex(DssDataSetFixLength dds, ParallelVars prvs){
		Date st=dds.getStartTime();
		long sTime=st.getTime();
		int sYear=st.getYear()+1900;
		int sMonth=st.getMonth()+1; //HEC DSS7 uses getMonth()+1. However, Vista/HecDSS6 uses getMonth() because dss data store at 24:00 Jan31, 1921 is considered to store at 0:00 Feb 1, 1921
		Date dataDate=new Date(prvs.dataYear-1900, prvs.dataMonth-1, prvs.dataDay);
		int index;
		if (TimeOperation.isMonthlyInterval(dds.getTimeStep())){
			index=prvs.dataYear*12+prvs.dataMonth-(sYear*12+sMonth);
		}else{
			//double indexValue=(dataTime-sTime)/(1000*60*60*24);
			Calendar c1=Calendar.getInstance();
			c1.setTime(st);
			Calendar c2=Calendar.getInstance();
			c2.setTime(dataDate);
			long indexValue = Duration.between(c1.toInstant(), c2.toInstant()).toDays();
			index=(int)indexValue+1; //HEC DSS7 uses indexValue+1; Vista/Hec DSS6 uses indexValue+2
		}
		return index;
	}
	
	public static EvalExpression timeseries(){
		String svName=ControlData.currEvalName;
		ParallelVars prvs=TimeOperation.findTime(0);
		double value=svarTimeSeries(svName, 0, prvs);
		IntDouble id=new IntDouble(value,false);
		return new EvalExpression(id);
	}
	
	public static double timeseries(String tsName){
		ParallelVars prvs=TimeOperation.findTime(0);
		return svarTimeSeries(tsName, 0, prvs);
	}
	
	public static EvalExpression pastTSFV(String ident, EvalExpression ee1, ArrayList<ArrayList<EvalExpression>> eeArray,  ParallelVars prvs){
		//turn off when multi-dimensional array is used
		if (eeArray.size()!=1){
			Error.addEvaluationError("The future index of array variable "+ident+" has to be a single integer.");
			return new EvalExpression(new IntDouble(1.0,false));
		}
		//
		
		ArrayList<EvalExpression> ee2Array = eeArray.get(0);
		if (ee2Array.size() !=1){
			Error.addEvaluationError("The future index of array variable "+ident+" has to be a single integer but not a range.");
			return new EvalExpression(new IntDouble(1.0,false));
		}
		EvalExpression ee2 = ee2Array.get(0);
		
		if (!ee1.isNumeric() || !ee2.isNumeric()){
			Error.addEvaluationError("The index of array variable "+ident+" has to be an integer.");
			return new EvalExpression(new IntDouble(1.0,false));
		}
		IntDouble id1=ee1.getValue();
		IntDouble id2=ee2.getValue();
		if (!id1.isInt() || !id2.isInt()){
			Error.addEvaluationError("The index of array variable "+ident+" has to be an integer.");
			return new EvalExpression(new IntDouble(1.0,false));
		}
		int i1 = id1.getData().intValue();
		int i2 = id2.getData().intValue();
		
		if (i1==0){
			EvalExpression ee = new EvalExpression();
			LinkedHashMap<String, IntDouble> multiplier = ee.getMultiplier();
			if (i2==0){
				multiplier.put(ident, new IntDouble(1, true));
				return ee;
			}else if (i2>0){
				String vn = ident+"__fut__"+id2;
				multiplier.put(vn, new IntDouble(1, true));
				return ee;
			}
		}
		
		if (i1>0){
			Error.addEvaluationError("The second index of array variable "+ident+" has to be less than or equal to 0 in constraint statements.");
			return new EvalExpression(new IntDouble(1.0,false));
		}
		if (i2<0){
			Error.addEvaluationError("The first index of array variable "+ident+" has to be larger than or equal to 0.");
			return new EvalExpression(new IntDouble(1.0,false));
		}
		if (!ControlData.currDvMap.containsKey(ident) && !ControlData.currAliasMap.containsKey(ident)){
			Error.addEvaluationError("The array variable "+ident+" is not a dvar or alias. The value from the past time step could not be retrieved.");
			return new EvalExpression(new IntDouble(1.0,false));
		}
		String vn=ident+"__fut__"+i2;
		String entryNameTS=DssOperation.entryNameTS(vn, ControlData.timeStep);
		if (DataTimeSeries.dvAliasTS.containsKey(entryNameTS)){
			DssDataSetFixLength ddsfl = DataTimeSeries.dvAliasTS.get(entryNameTS);
			if (ddsfl!=null){
				Date memStartDate = ddsfl.getStartTime();
				Date currDate =  new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
				int index=TimeOperation.getNumberOfTimestep(memStartDate, currDate, ddsfl.getTimeStep())+i1-1;
				double[] datafl=ddsfl.getData();
				if (index>=datafl.length){
					Error.addEvaluationError(vn + " at timestep " +i1+" doesn't have value.");
					return new EvalExpression(new IntDouble(1.0,false));
				}else if (index>=0){
					return new EvalExpression(new IntDouble(datafl[index], false));
				}else{
					Error.addEvaluationError(vn + " at timestep " +i1+" doesn't have value.");
					return new EvalExpression(new IntDouble(1.0,false));
				}
			}
			
			if (DataTimeSeries.dvAliasInit.containsKey(entryNameTS)){
				if (!getDVAliasInitTimeseries(ident)){
					Error.addEvaluationError("Initial file doesn't have data for decision vairiable/alias " +vn);
					return new EvalExpression(new IntDouble(1.0,false));
				}
				
				DssDataSet dds=DataTimeSeries.dvAliasInit.get(entryNameTS);
				int index = timeSeriesIndex(dds, prvs);
				ArrayList<Double> data=dds.getData();
				if (index>=0 && index<data.size()){
					double result=data.get(index);
					if (result==-901.0 || result==-902.0){
						Error.addEvaluationError("Initial file doesn't have data for decision vairiable/alias " + vn + " at time step "+i1+".");
						return new EvalExpression(new IntDouble(1.0,false));
					}
					return new EvalExpression(new IntDouble(result, false));
				}else{
					Error.addEvaluationError("Initial file doesn't have data for decision vairiable/alias " + vn + " at time step "+i1+".");
					return new EvalExpression(new IntDouble(1.0,false));
				}
			}else{
				Error.addEvaluationError(vn + " doesn't have value.");
				return new EvalExpression(new IntDouble(1.0,false));
			}
		}else{
			Error.addEvaluationError(vn + "doesn't exist.");
			return new EvalExpression(new IntDouble(1.0,false));
		}
	}
	
	public static IntDouble pastCycleNoTimeArray(String ident, String cycle){
		Map<String, Map<String, IntDouble>> varCycleValueMap=ControlData.currStudyDataSet.getVarCycleValueMap();
		IntDouble data=new IntDouble(1.0,false);
		if (varCycleValueMap.containsKey(ident)){
			Map<String, IntDouble> var= varCycleValueMap.get(ident);
			if (var.containsKey(cycle)){
				data=var.get(cycle);
			}else{
				Error.addEvaluationError("The variable "+ident+" is not defined in the past cycle of "+cycle+".");
				return data;
			}
		}else{
			Error.addEvaluationError("The variable "+ident+" is not defined in the past cycle of "+cycle+".");
			return data;
		}
		if (data==null){
			Error.addEvaluationError("The variable "+ident+" is not defined in the past cycle of "+cycle+".");
			return new IntDouble(1.0,false);
		}
		return new IntDouble(data.getData(),data.isInt());
	}
	
	public static IntDouble pastCycleIndexNoTimeArray(String ident, int index){
		int ci=ControlData.currCycleIndex+index;
		if (ci<0){
			Error.addEvaluationError("The "+ci+" cycle from the current cycle is unvailable.");
			return new IntDouble(1.0,false);
		}
		StudyDataSet sds = ControlData.currStudyDataSet;
		String cycle=sds.getModelList().get(ci);
		Map<String, Map<String, IntDouble>> varCycleIndexValueMap = sds.getVarCycleIndexValueMap();		
		if (varCycleIndexValueMap.containsKey(ident)){
			Map<String, IntDouble> valueMap = varCycleIndexValueMap.get(ident);
			if (valueMap.containsKey(cycle)){
				return valueMap.get(cycle);
			}
		}
		Error.addEvaluationError("Variable "+ident+" is not in "+ index+" from the current cycle - "+cycle);
		return new IntDouble(1.0,false);
	}
	
	public static IntDouble pastCycleTimeArray(String ident, String cycle, EvalExpression ee){
		IntDouble data=new IntDouble(1.0,false);
		if (!ee.isNumeric()){
			Error.addEvaluationError("Time array index of "+ident+" contains decision variable.");
			return data;
		}
		IntDouble id=ee.getValue();
		if (!id.isInt()){
			Error.addEvaluationError("Time array index of "+ident+" is not an integer.");
			return data;
		}
		int index=id.getData().intValue();
		if (index<0){
			ArrayList<EvalExpression> eeArray=new ArrayList<EvalExpression>();
			eeArray.add(ee);
			ModelDataSet mds=ControlData.currStudyDataSet.getModelDataSetMap().get(cycle);
			EvalExpression ee1;
			if (mds.dvMap.containsKey(ident) || mds.asMap.containsKey(ident)){
				ParallelVars prvs=TimeOperation.findTime(index);
				//if (ControlData.outputCycleToDss){
				ArrayList<String> ml = ControlData.currStudyDataSet.getModelList();
				int ci=-1;
				for (int k=0; k<ml.size(); k++){
					if (cycle.equalsIgnoreCase(ml.get(k))){
						ci=k;
					}
				}
				if (ci==-1) {
					Error.addEvaluationError("The cycle of "+cycle+" of the variable "+ident+ "  is not in the model.");
					return data;
				}
				return new IntDouble(dvarAliasCycleTimeSeries(ident, index, ci, prvs), false);
				//}else{
				//	return new IntDouble(dvarAliasTimeSeries(ident, index), false);
				//}
			}else{
				Error.addEvaluationError(ident+" is not a dvar/alias in the cycle of "+cycle+". Only dvar/alias in the past time step of "+index+" and past cycle of "+cycle+" can be used from previous cycles");
				return new IntDouble(1.0, false);
			}
		}else if(index==0){
			return pastCycleNoTimeArray(ident, cycle);
		}
		Map<String, Map<String, IntDouble>> varTimeArrayCycleValueMap=ControlData.currStudyDataSet.getVarTimeArrayCycleValueMap();
	    String varTimeArrayName=ident+"__fut__"+index;
		if (varTimeArrayCycleValueMap.containsKey(varTimeArrayName)){
			Map<String, IntDouble> var= varTimeArrayCycleValueMap.get(varTimeArrayName);
			if (var.containsKey(cycle)){
				data=var.get(cycle);
			}else{
				Error.addEvaluationError("The variable "+ident+" is not defined in the past cycle of "+cycle+".");
				return data;
			}
		}else{
			Error.addEvaluationError("The variable "+ident+" is not defined in the past cycle of "+cycle+".");
			return data;
		}
		if (data==null){
			Error.addEvaluationError("The variable "+ident+" is not defined in the past cycle of "+cycle+".");
			return new IntDouble(1.0,false);
		}
		return new IntDouble(data.getData(),data.isInt());
	}
	
	public static IntDouble pastCycleIndexTimeArray(String ident, int pci, EvalExpression ee){
		IntDouble data=new IntDouble(1.0,false);
		int ci=ControlData.currCycleIndex+pci;
		if (ci<0){
			Error.addEvaluationError("The "+ci+" cycle from the current cycle is unvailable.");
			return new IntDouble(1.0,false);
		}
		if (!ee.isNumeric()){
			Error.addEvaluationError("Time array index of "+ident+" contains decision variable.");
			return data;
		}
		IntDouble id=ee.getValue();
		if (!id.isInt()){
			Error.addEvaluationError("Time array index of "+ident+" is not an integer.");
			return data;
		}
		int index=id.getData().intValue();
		StudyDataSet sds = ControlData.currStudyDataSet;
		String cycle=sds.getModelList().get(ci);
		if (index<0){
			ArrayList<EvalExpression> eeArray=new ArrayList<EvalExpression>();
			eeArray.add(ee);
			ModelDataSet mds=ControlData.currStudyDataSet.getModelDataSetMap().get(cycle);
			EvalExpression ee1;
			if (mds.dvMap.containsKey(ident) || mds.asMap.containsKey(ident)){
				ParallelVars prvs=TimeOperation.findTime(index);
				//if (ControlData.outputCycleToDss){
				return new IntDouble(dvarAliasCycleTimeSeries(ident, index, ci, prvs), false);
				//}else{
				//	return new IntDouble(dvarAliasTimeSeries(ident, index), false);
				//}
			}else{
				Error.addEvaluationError(ident+" is not a dvar/alias in the cycle of "+cycle+". Only dvar/alias in the past time step of "+index+" and past cycle of "+cycle+" can be used from previous cycles");
				return new IntDouble(1.0, false);
			}
		}else if(index==0){
			return pastCycleIndexNoTimeArray(ident, pci);
		}
		Map<String, Map<String, IntDouble>> varCycleIndexValueMap=sds.getVarCycleIndexValueMap();
	    String varTimeArrayName=ident+"__fut__"+index;
		if (varCycleIndexValueMap.containsKey(varTimeArrayName)){
			Map<String, IntDouble> var= varCycleIndexValueMap.get(varTimeArrayName);
			if (var.containsKey(cycle)){
				data=var.get(cycle);
			}else{
				Error.addEvaluationError("The variable "+ident+" is not defined in the past cycle of "+cycle+".");
				return data;
			}
		}else{
			Error.addEvaluationError("The variable "+ident+" is not defined in the past cycle of "+cycle+".");
			return data;
		}
		if (data==null){
			Error.addEvaluationError("The variable "+ident+" is not defined in the past cycle of "+cycle+".");
			return new IntDouble(1.0,false);
		}
		return new IntDouble(data.getData(),data.isInt());
	}
	
	public static ArrayList<EvalExpression> trunk_timeArray(String ident, IntDouble start, IntDouble end){
		ArrayList<EvalExpression> eeArray=new ArrayList<EvalExpression>();
		if (!start.isInt()){
			Error.addEvaluationError("The starting index of trunk data for variable " + ident + " is not an integer.");
			EvalExpression ee=new EvalExpression(new IntDouble(1.0, false));
			eeArray.add(ee);
			return eeArray;
		}else if (!end.isInt()){
			Error.addEvaluationError("The ending index of trunk data for variable " + ident + " is not an integer.");
			EvalExpression ee=new EvalExpression(new IntDouble(1.0, false));
			eeArray.add(ee);
			return eeArray;
		}
		int si=start.getData().intValue();
		int ei=end.getData().intValue();
		
		if (si>ei){
			for (int i=si; i>=ei; i--){
				ArrayList<IntDouble> indexArray1=new ArrayList<IntDouble> ();
				IntDouble index = new IntDouble(i, true);
				indexArray1.add(index);
				ArrayList<ArrayList<IntDouble>> indexArray=new ArrayList<ArrayList<IntDouble>> ();
				indexArray.add(indexArray1);
				IntDouble id=ValueEvaluation.argFunction(ident, indexArray);
				id.setIndex(i);
				id.setName(ident);
				eeArray.add(new EvalExpression(id));
			}
		}else{
			for (int i=si; i<=ei; i++){
				ArrayList<IntDouble> indexArray1=new ArrayList<IntDouble> ();
				IntDouble index = new IntDouble(i, true);
				indexArray1.add(index);
				ArrayList<ArrayList<IntDouble>> indexArray=new ArrayList<ArrayList<IntDouble>> ();
				indexArray.add(indexArray1);
				IntDouble id=ValueEvaluation.argFunction(ident, indexArray);
				id.setIndex(i);
				id.setName(ident);
				eeArray.add(new EvalExpression(id));
			}
		}
		return eeArray;
	}
	
	public static EvalExpression max(EvalExpression ee1, EvalExpression ee2){
		if (!ee1.isNumeric() || !ee2.isNumeric()){
			Error.addEvaluationError("variable inside max function should not contain decision variable.");
		}
		return new EvalExpression(maxOperation(ee1.getValue(), ee2.getValue()));
	}
	
	public static IntDouble maxOperation(IntDouble id1, IntDouble id2){
		IntDouble id;
		if (id1.isInt() && id2.isInt()){
			id=new IntDouble(Math.max(id1.getData().intValue(),id2.getData().intValue()), true);
		}else if (id1.isInt() && !id2.isInt()){
			id=new IntDouble(Math.max(id1.getData().doubleValue(),id2.getData().doubleValue()), false);
		}else if (!id1.isInt() && id2.isInt()){
			id=new IntDouble(Math.max(id1.getData().doubleValue(),id2.getData().doubleValue()), false);
		}else{
			id=new IntDouble(Math.max(id1.getData().doubleValue(),id2.getData().doubleValue()), false);
		}
		return id;
	}
	
	public static EvalExpression min(EvalExpression ee1, EvalExpression ee2){
		if (!ee1.isNumeric() || !ee2.isNumeric()){
			Error.addEvaluationError("variable inside min function should not contain decision variable.");
		}
		return new EvalExpression((minOperation(ee1.getValue(), ee2.getValue())));
	}
	
	public static IntDouble minOperation(IntDouble id1, IntDouble id2){
		IntDouble id;
		if (id1.isInt() && id2.isInt()){
			id=new IntDouble(Math.min(id1.getData().intValue(),id2.getData().intValue()), true);
		}else if (id1.isInt() && !id2.isInt()){
			id=new IntDouble(Math.min(id1.getData().doubleValue(),id2.getData().doubleValue()), false);
		}else if (!id1.isInt() && id2.isInt()){
			id=new IntDouble(Math.min(id1.getData().doubleValue(),id2.getData().doubleValue()), false);
		}else{
			id=new IntDouble(Math.min(id1.getData().doubleValue(),id2.getData().doubleValue()), false);
		}
		return id;
	}
	
	public static EvalExpression intFunc(EvalExpression ee1){
		if (!ee1.isNumeric()){
			Error.addEvaluationError("variable inside int function should not contain decision variable.");
		}
		return new EvalExpression(intOperation(ee1.getValue()));
	}
	
	public static IntDouble intOperation(IntDouble id1){
		IntDouble id;
		if (!id1.isInt()){
			id=new IntDouble(((int)id1.getData().doubleValue()), true);
			return id;
		}
		return id1;
	}
	
	public static EvalExpression realFunc(EvalExpression ee1){
		if (!ee1.isNumeric()){
			Error.addEvaluationError("variable inside int function should not contain decision variable.");
		}
		return new EvalExpression(realOperation(ee1.getValue()));
	}
	
	public static IntDouble realOperation(IntDouble id1){
		IntDouble id;
		if (id1.isInt()){
			id=new IntDouble((id1.getData().doubleValue()), false);
			return id;
		}
		return id1;
	}
	
	public static EvalExpression abs(EvalExpression ee1){
		if (!ee1.isNumeric()){
			Error.addEvaluationError("variable inside abs function should not contain decision variable.");
		}
		return new EvalExpression(absOperation(ee1.getValue()));
	}
	
	public static IntDouble absOperation(IntDouble id1){
		IntDouble id;
		if (id1.isInt()){
			id=new IntDouble(Math.abs(id1.getData().intValue()), true);
		}else{
			id=new IntDouble(Math.abs(id1.getData().doubleValue()), false);
		}
		return id;
	}
	
	public static EvalExpression exp(EvalExpression ee1){
		if (!ee1.isNumeric()){
			Error.addEvaluationError("variable inside log function should not contain decision variable.");
		}
		return new EvalExpression(expOperation(ee1.getValue()));
	}
	
	public static IntDouble expOperation(IntDouble id1){
		IntDouble id;
		if (id1.isInt()){
			id=new IntDouble(Math.exp(id1.getData().intValue()), false);
		}else{
			id=new IntDouble(Math.exp(id1.getData().doubleValue()), false);
		}
		return id;
	}
	
	public static EvalExpression log(EvalExpression ee1){
		if (!ee1.isNumeric()){
			Error.addEvaluationError("variable inside log function should not contain decision variable.");
		}
		return new EvalExpression(logOperation(ee1.getValue()));
	}
	
	public static IntDouble logOperation(IntDouble id1){
		IntDouble id;
		if (id1.isInt()){
			id=new IntDouble(Math.log(id1.getData().intValue()), false);
		}else{
			id=new IntDouble(Math.log(id1.getData().doubleValue()), false);
		}
		return id;
	}
	
	public static EvalExpression log10(EvalExpression ee1){
		if (!ee1.isNumeric()){
			Error.addEvaluationError("variable inside log10 function should not contain decision variable.");
		}
		return new EvalExpression(log10Operation(ee1.getValue()));
	}
	
	public static IntDouble log10Operation(IntDouble id1){
		IntDouble id;
		if (id1.isInt()){
			id=new IntDouble(Math.log10(id1.getData().intValue()), false);
		}else{
			id=new IntDouble(Math.log10(id1.getData().doubleValue()), false);
		}
		return id;
	}
	
	public static EvalExpression pow(EvalExpression ee1, EvalExpression ee2){
		if (!ee1.isNumeric() || !ee2.isNumeric()){
			Error.addEvaluationError("variable inside pow function should not contain decision variable.");
		}
		return new EvalExpression(powOperation(ee1.getValue(), ee2.getValue()));
	}
	
	public static IntDouble powOperation(IntDouble id1, IntDouble id2){
		IntDouble id;
		if (id1.isInt() && id2.isInt()){
			id=new IntDouble(Math.pow(id1.getData().intValue(),id2.getData().intValue()), false);
		}else if (id1.isInt() && !id2.isInt()){
			id=new IntDouble(Math.pow(id1.getData().doubleValue(),id2.getData().doubleValue()), false);
		}else if (!id1.isInt() && id2.isInt()){
			id=new IntDouble(Math.pow(id1.getData().doubleValue(),id2.getData().doubleValue()), false);
		}else{
			id=new IntDouble(Math.pow(id1.getData().doubleValue(),id2.getData().doubleValue()), false);
		}
		return id;
	}
	
	public static EvalExpression sin(EvalExpression ee1){
		if (!ee1.isNumeric()){
			Error.addEvaluationError("variable inside sin function should not contain decision variable.");
		}
		
		double degrees = ee1.getValue().getData().doubleValue();
		double radians = Math.toRadians(degrees);
		return new EvalExpression(new IntDouble(Math.sin(radians), false));		
	}
	
	public static EvalExpression cos(EvalExpression ee1){
		if (!ee1.isNumeric()){
			Error.addEvaluationError("variable inside cos function should not contain decision variable.");
		}
		
		double degrees = ee1.getValue().getData().doubleValue();
		double radians = Math.toRadians(degrees);
		return new EvalExpression(new IntDouble(Math.cos(radians), false));		
	}
	
	public static EvalExpression tan(EvalExpression ee1){
		if (!ee1.isNumeric()){
			Error.addEvaluationError("variable inside tan function should not contain decision variable.");
		}
		
		double degrees = ee1.getValue().getData().doubleValue();
		double radians = Math.toRadians(degrees);
		return new EvalExpression(new IntDouble(Math.tan(radians), false));		
	}
	
	public static EvalExpression cot(EvalExpression ee1){
		if (!ee1.isNumeric()){
			Error.addEvaluationError("variable inside cot function should not contain decision variable.");
		}
		
		double degrees = ee1.getValue().getData().doubleValue();
		if (degrees==90.0){
			return new EvalExpression(new IntDouble(0.0, false));
		}
		double radians = Math.toRadians(degrees);
		return new EvalExpression(new IntDouble(1.0/Math.tan(radians), false));		
	}
	
	public static EvalExpression asin(EvalExpression ee1){
		if (!ee1.isNumeric()){
			Error.addEvaluationError("variable inside asin function should not contain decision variable.");
		}
		
		double value = ee1.getValue().getData().doubleValue();
		double radians = Math.asin(value);
		return new EvalExpression(new IntDouble(Math.toDegrees(radians), false));		
	}
	
	public static EvalExpression acos(EvalExpression ee1){
		if (!ee1.isNumeric()){
			Error.addEvaluationError("variable inside acos function should not contain decision variable.");
		}
		
		double value = ee1.getValue().getData().doubleValue();
		double radians = Math.acos(value);
		return new EvalExpression(new IntDouble(Math.toDegrees(radians), false));		
	}
	
	public static EvalExpression atan(EvalExpression ee1){
		if (!ee1.isNumeric()){
			Error.addEvaluationError("variable inside atan function should not contain decision variable.");
		}
		
		double value = ee1.getValue().getData().doubleValue();
		double radians = Math.atan(value);
		return new EvalExpression(new IntDouble(Math.toDegrees(radians), false));		
	}
	
	public static EvalExpression acot(EvalExpression ee1){
		if (!ee1.isNumeric()){
			Error.addEvaluationError("variable inside acot function should not contain decision variable.");
		}
		
		double value = ee1.getValue().getData().doubleValue();
		if (value == 0.){
			return new EvalExpression(new IntDouble(90.0, false));
		}
		double radians = Math.atan(1.0/value);
		return new EvalExpression(new IntDouble(Math.toDegrees(radians), false));		
	}
	
	public static EvalExpression exceedance(String tsName, EvalExpression exc_ee, String selMon, String syStr, String smStr, String sdStr, String eyStr, String emStr, String edStr){
		String entryNameTS=DssOperation.entryNameTS(tsName, ControlData.timeStep);
		if (DataTimeSeries.svTS.containsKey(entryNameTS)){
			DssDataSet dds = DataTimeSeries.svTS.get(entryNameTS);
			int sy = Integer.parseInt(syStr);
			int sd = Integer.parseInt(sdStr);
			int ey = Integer.parseInt(eyStr);
			int ed = Integer.parseInt(edStr);
			int sm = TimeOperation.monthValue(smStr);
			int em = TimeOperation.monthValue(emStr);
			
			double exc=1.0;
			if (exc_ee.isNumeric()){
				exc=exc_ee.getValue().getData().doubleValue();
			}else{
				Error.addEvaluationError("Exceedance level has unknown variable.");
				return new EvalExpression(new IntDouble (1.0, false));
			}
			if (exc<=0.0 || exc>1.0){
				Error.addEvaluationError("Exceedance level must be >0.0 and <=1.0");
				return new EvalExpression(new IntDouble (1.0, false));
			}
			
			Date selSd;
			Date selEd;
			if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
				selSd=new Date(sy-1900, sm-1, TimeOperation.numberOfDays(sm, sy));
				selEd=new Date(ey-1900, em-1, TimeOperation.numberOfDays(em, ey));
			}else{
				selSd=new Date(sy-1900, sm-1, sd);
				selEd=new Date(ey-1900, em-1, ed);
			}
			
			ArrayList<Double> optedData=dds.getTimeseriesDataWithOptions(selMon, selSd, selEd);
			double value=DssDataSet.getExceedance(optedData, exc);
			return new EvalExpression(new IntDouble (value, false));
		}else{
			Error.addEvaluationError(tsName+" is not a timeseries variable used in the Exceendance funciton for the time step of "+ControlData.timeStep+".");
			return new EvalExpression(new IntDouble (1.0, false));
		}	
	}
	
	public static EvalExpression exceedance_tsi(String tsName, EvalExpression exc_ee, String selMon, String syStr, String smStr, String sdStr, String eyStr, String emStr, String edStr){
		String entryNameTS=DssOperation.entryNameTS(tsName, ControlData.timeStep);
		if (DataTimeSeries.svTS.containsKey(entryNameTS)){
			DssDataSet dds = DataTimeSeries.svTS.get(entryNameTS);
			int sy = Integer.parseInt(syStr);
			int sd = Integer.parseInt(sdStr);
			int ey = Integer.parseInt(eyStr);
			int ed = Integer.parseInt(edStr);
			int sm = TimeOperation.monthValue(smStr);
			int em = TimeOperation.monthValue(emStr);
			
			double exc=1.0;
			if (exc_ee.isNumeric()){
				exc=exc_ee.getValue().getData().doubleValue();
			}else{
				Error.addEvaluationError("Exceedance level has unknown variable.");
				return new EvalExpression(new IntDouble (0, true));
			}
			if (exc<=0.0 || exc>1.0){
				Error.addEvaluationError("Exceedance level must be >0.0 and <=1.0");
				return new EvalExpression(new IntDouble (0, true));
			}
			
			Date selSd;
			Date selEd;
			if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
				selSd=new Date(sy-1900, sm-1, TimeOperation.numberOfDays(sm, sy));
				selEd=new Date(ey-1900, em-1, TimeOperation.numberOfDays(em, ey));
			}else{
				selSd=new Date(sy-1900, sm-1, sd);
				selEd=new Date(ey-1900, em-1, ed);
			}
			
			ArrayList<Double> optedData=dds.getTimeseriesDataWithOptions(selMon, selSd, selEd);
			int tsi=DssDataSet.getExceedance_tsi(optedData, exc);
			return new EvalExpression(new IntDouble (tsi, true));
		}else{
			Error.addEvaluationError(tsName+" is not a timeseries variable used in the Exceendance_TSI funciton for the time step of "+ControlData.timeStep+".");
			return new EvalExpression(new IntDouble (0, true));
		}	
	}
	
	public static EvalExpression daysIn(){
		int days=TimeOperation.numberOfDays(ControlData.currMonth, ControlData.currYear);
		IntDouble id=new IntDouble(days, true);
		return new EvalExpression(id);
	}
	
	public static EvalExpression daysInTimeStep(){
		if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
			return daysIn();
		}else{
			IntDouble id=new IntDouble(1, true);
			return new EvalExpression(id);
		}
	}
	
	public static EvalExpression tafcfs_term(String ident, EvalExpression ee, Stack<LoopIndex> sumIndex){
		ParallelVars prvs = new ParallelVars();
		if (ee==null){
			prvs.dataMonth=ControlData.currMonth;
			prvs.dataYear=ControlData.currYear;
		}else{
			IntDouble id=new IntDouble(0,true);
			if (!ee.isNumeric()){
				HashMap<String, IntDouble> multiplier=ee.getMultiplier();
				if (multiplier.size()==1){
					if (sumIndex.size()>0){
						LoopIndex li=sumIndex.pop();
						String indexName=li.getName();
						int indexValue=li.getValue();
						sumIndex.push(li);
						id=new IntDouble(indexValue, true);
					}else{
						Error.addEvaluationError("The index of "+ident+" should not contain decision variable.");
					}
				}else{
					Error.addEvaluationError("The index of "+ident+" should not contain decision variable.");
				}
			}else{
				id=ee.getValue();
			}
			if (!id.isInt()){
				Error.addEvaluationError("The index of "+ident+" should be integer.");
			}
			prvs=TimeOperation.findTime(id.getData().intValue());
		}
		double convert = tafcfs(ident, prvs);
		IntDouble id1=new IntDouble(convert, false);
		return new EvalExpression(id1);
	}
	
	public static double tafcfs(String ident, ParallelVars prvs){
		double convert;
		int days=TimeOperation.numberOfDays(prvs.dataMonth, prvs.dataYear);
		if (ident.equals("taf_cfs")){
			if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
				return 504.1666667 / days;
			}else{
				return 504.1666667;
			}
		}else if (ident.equals("cfs_taf")){
			if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
				return days / 504.1666667;
			}else{
				return 1 / 504.1666667;
			}
		}else if (ident.equals("af_cfs")){
			if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
				return 504.1666667 / days / 1000.;
			}else{
				return 504.1666667 / 1000.;
			}
		}else{
			if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
				return days / 504.1666667 * 1000.;
			}else{
				return 1 / 504.1666667 * 1000.;
			}
		}
	}
	
	public static EvalExpression term_YEAR(){
		IntDouble id=new IntDouble(TimeOperation.waterYearValue(), true);
		return new EvalExpression(id);
	}
	
	public static EvalExpression term_MONTH(){
		IntDouble id=new IntDouble(TimeOperation.waterMonthValue(ControlData.currMonth), true);
		return new EvalExpression(id);
	}
	
	public static EvalExpression term_DAY(){
		IntDouble id=new IntDouble(ControlData.currDay, true);
		return new EvalExpression(id);
	}
	
	public static EvalExpression term_MONTH_CONST(String month){
		int monthValue=TimeOperation.monthValue(month);
		IntDouble id=new IntDouble(TimeOperation.waterMonthValue(monthValue), true, month, 0);
		return new EvalExpression(id);
	}
	
	public static EvalExpression term_PASTMONTH(String pastMonth){
		pastMonth=pastMonth.substring(4);
		int pastMonthValue=TimeOperation.monthValue(pastMonth);
		int index=pastMonthValue-ControlData.currMonth;
		if (index>=0){
			index=index-12;
		}
		IntDouble id=new IntDouble(index,true);
		return new EvalExpression(id);
	}
	
	public static EvalExpression term_ARRAY_ITERATOR(ParallelVars prvs){
		IntDouble id=new IntDouble(prvs.timeArrayIndex, true);
		return new EvalExpression(id);
	}
	
	public static void sumExpression_IDENT(String ident, Stack<LoopIndex> sumIndex){
		//To Do: check if svar, dvar, alias contains ident
		LoopIndex li=new LoopIndex(ident, 0, false, 0, 0, 0);
		sumIndex.push(li);
	}
	
	public static void initSumExpression(EvalExpression ee1, EvalExpression ee2, String s, Stack<LoopIndex> sumIndex){
		LoopIndex li = sumIndex.pop();
		li.step=1;
		if (!s.equals("")){
			li.step=convertStringToInt(s);
		}
		if (!ee1.isNumeric() || !ee1.getValue().isInt()){
			Error.addEvaluationError("the starting index should be integer");
		}
		if (!ee2.isNumeric() || !ee2.getValue().isInt()){
			Error.addEvaluationError("the ending index should be integer");
		}
		li.start=ee1.getValue().getData().intValue();
		li.end=ee2.getValue().getData().intValue();
		li.setValue(li.start);
		li.setIndexStart(true);
		sumIndex.push(li);
		if (li.start>li.end && li.step>0){
			ControlData.ignoreError=true;
		}else if (li.start<li.end && li.step<0){
			ControlData.ignoreError=true;
		}
	}
	
	public static EvalExpression sumExpression(EvalExpression ee, String expression, Stack<LoopIndex> sumIndex){	
		ControlData.ignoreError=false;
		LoopIndex li=sumIndex.pop();
		if (li.step>=0){
			if (li.start>li.end) {
				return new EvalExpression(new IntDouble(0.0, false));
			}
			li.start=li.start+li.step;
			if (li.start>li.end) return ee;
			sumIndex.push(li);
			for (int i=li.start; i<=li.end; i=i+li.step){
				li=sumIndex.pop();
				li.setValue(i);
				li.setIndexStart(true);
				sumIndex.push(li);
				ANTLRStringStream stream = new ANTLRStringStream("s: "+expression); 
				EvaluatorLexer lexer = new EvaluatorLexer(stream);
				TokenStream tokenStream = new CommonTokenStream(lexer);
				EvaluatorParser evaluator = new EvaluatorParser(tokenStream);
				evaluator.setSumIndex(sumIndex);
				try{
					evaluator.evaluator();
				}catch (RecognitionException e){
					Error.addEvaluationError(e.toString());
				}
			
				EvalExpression ee0=evaluator.evalExpression;
				ee=add(ee, ee0);
			}
		}else{
			if (li.start<li.end) return new EvalExpression(new IntDouble(0.0, false));
			li.start=li.start+li.step;
			if (li.start<li.end) return ee;
			sumIndex.push(li);
			for (int i=li.start; i>=li.end; i=i+li.step){
				li=sumIndex.pop();
				li.setValue(i);
				li.setIndexStart(true);
				sumIndex.push(li);
				ANTLRStringStream stream = new ANTLRStringStream("s: "+expression); 
				EvaluatorLexer lexer = new EvaluatorLexer(stream);
				TokenStream tokenStream = new CommonTokenStream(lexer);
				EvaluatorParser evaluator = new EvaluatorParser(tokenStream);
				evaluator.setSumIndex(sumIndex);
				try{
					evaluator.evaluator();
				}catch (RecognitionException e){
					Error.addEvaluationError(e.toString());
				}
			
				EvalExpression ee0=evaluator.evalExpression;
				ee=add(ee, ee0);
			}
		}
		sumIndex.pop();
		return ee;
	}
	
	public static Number assignWhereStatement(EvalExpression ee){
		if (ee.isNumeric()){
			return ee.getValue().getData();
		}else{
			return 1.0;
		}
	}
	
	public static EvalExpression tableSQL(String table, String select, HashMap<String, Number> where, HashMap<String, Number> given, String use){
		IntDouble id;
		if (where==null){	
			id=TableOperation.findData(table, select, given, use);
		}else{
			id=TableOperation.findData(table, select, where, given, use);
		}
		return new EvalExpression(id);
	}
	
	public static IntDouble expressionInput(EvalExpression ee){
		if (!ee.isNumeric()){
			Error.addEvaluationError("the value is not numeric and contains decision variable");
		}
		return ee.getValue();
	}
}
