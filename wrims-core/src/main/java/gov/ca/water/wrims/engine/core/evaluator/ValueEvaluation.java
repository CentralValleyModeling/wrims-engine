package gov.ca.water.wrims.engine.core.evaluator;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;

import gov.ca.water.wrims.engine.core.commondata.solverdata.SolverData;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Alias;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Dvar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.ModelDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.StudyDataSet;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.Error;
import gov.ca.water.wrims.engine.core.components.IntDouble;
import gov.ca.water.wrims.engine.core.external.*;
import gov.ca.water.wrims.engine.core.hdf5.HDF5Reader;
import gov.ca.water.wrims.engine.core.parallel.ParallelVars;
import gov.ca.water.wrims.engine.core.solver.CbcSolver;

import java.time.Duration;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Date;

public class ValueEvaluation {
	public static double convertStringToDouble(String text){
		return Double.parseDouble(text);
	}
	
	public static int convertStringToInt(String text){
		return Integer.parseInt(text);
	}
	
	public static String convertDoubleToString(double value){
		return Double.toString(value);
	}
	
	public static String convertIntToString(int value){
		return Integer.toString(value);
	}
	
	public static boolean relationStatement(IntDouble id1, IntDouble id2, String relation){
		double value1=id1.getData().doubleValue();
		double value2=id2.getData().doubleValue();
		
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
	
	public static IntDouble term_knownTS(IntDouble result){
		return result;
	}
	
	public static IntDouble term_IDENT (String ident, Stack<LoopIndex> sumIndex){
		if (ControlData.currSvMap.containsKey(ident)){
			IntDouble id0=ControlData.currSvMap.get(ident).getData();
			return new IntDouble(id0.getData(),id0.isInt(),ident, 0);
		}else if (ControlData.currTsMap.containsKey(ident)){
			IntDouble id0=ControlData.currTsMap.get(ident).getData();
			return new IntDouble(id0.getData(),id0.isInt(), ident, 0);
		}else if (ControlData.isPostProcessing && ControlData.currDvMap.containsKey(ident)){
			IntDouble id0=ControlData.currDvMap.get(ident).getData();
			return new IntDouble(id0.getData(),id0.isInt(), ident, 0);
		}else if (ControlData.isPostProcessing && ControlData.currAliasMap.containsKey(ident)){
			IntDouble id0=ControlData.currAliasMap.get(ident).getData();
			if (id0==null) {
				Error.addEvaluationError(ident+" is not defined before it is used.");
				return new IntDouble (1.0, false, ident, 0);
			}
			return new IntDouble(id0.getData(),id0.isInt(), ident, 0);
		}
		if (sumIndex.size()>0){
			LoopIndex li=sumIndex.pop();
			if (li.getName().equals(ident) && li.getIndexStart()){
				sumIndex.push(li);
				return new IntDouble(li.getValue(),true, ident, 0);
			}
			sumIndex.push(li);
		}
		if (ControlData.parameterMap.containsKey(ident)){
			IntDouble id0=ControlData.parameterMap.get(ident).getData();
			return new IntDouble(id0.getData(),id0.isInt(), ident, 0);					
		}
		Error.addEvaluationError(ident+" is not in svar, dvar, alias, or parameter list.");
		return new IntDouble (1.0, false, ident, 0);
	}
		
	public static IntDouble term_SVAR (String ident){
		IntDouble data;
		if (!ControlData.currSvMap.containsKey(ident)){
			if (!ControlData.currTsMap.containsKey(ident)){
				Error.addEvaluationError("State variable "+ident+" is not defined before used.");
				return new IntDouble(1.0, false, ident, 0);
			}else{
				data=ControlData.currTsMap.get(ident).getData();
			}
		}else{
			data=ControlData.currSvMap.get(ident).getData();
		}
		
		if (data == null){
			Error.addEvaluationError("The value of state variable "+ident+" is not defined before used.");
			return new IntDouble(1.0, false, ident, 0);
		}
		return new IntDouble(data.getData(), data.isInt(), ident, 0);
	}
	
	public static IntDouble term_INTEGER (String integer){
		return new IntDouble(convertStringToInt(integer), true);
	}
	
	public static IntDouble term_FLOAT (String floatValue){
		return new IntDouble(convertStringToDouble(floatValue), false);
	}
	
	public static IntDouble unary (String s, IntDouble id){
		if (s !=null && s.equals("-")){
			if (id.isInt()){
				int value=-id.getData().intValue();
				return new IntDouble(value, id.isInt());
			}else{
				double value=-id.getData().doubleValue();
				return new IntDouble(value, id.isInt());
			}
		}else{
			return id;
		}
	}
		
	public static IntDouble mult(IntDouble id1, IntDouble id2){
		IntDouble id;
		if (!id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()*id2.getData().doubleValue(), false);
		}else if (id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().intValue()*id2.getData().doubleValue(), false);
		}else if (!id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()*id2.getData().intValue(), false);
		}else{
			id=new IntDouble(id1.getData().intValue()*id2.getData().intValue(), true);
		}
		return id;
	}
	
	public static IntDouble divide(IntDouble id1, IntDouble id2){
		IntDouble id;
		if (id2.getData().doubleValue()==0.0){
			Error.addEvaluationError("divided by 0.");
			return new IntDouble(1.0, false);
		}
		if (!id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()/id2.getData().doubleValue(), false);
		}else if (id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().intValue()/id2.getData().doubleValue(), false);
		}else if (!id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()/id2.getData().intValue(), false);
		}else {
			id=new IntDouble(id1.getData().intValue()/id2.getData().intValue(), true);
		}
		return id;		
	}
		
	public static IntDouble mod(IntDouble id1, IntDouble id2){
		IntDouble id;
		if (id2.getData().doubleValue()==0.0){
			Error.addEvaluationError("Mod function uses 0 as divider.");
			return new IntDouble(1.0, false);
		}
		if (!id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()%id2.getData().doubleValue(), false);
		}else if (id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().intValue()%id2.getData().doubleValue(), false);
		}else if (!id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()%id2.getData().intValue(), false);
		}else{
			id=new IntDouble(id1.getData().intValue()%id2.getData().intValue(), true);
		}
		return id;		
	}
	
	public static IntDouble round(IntDouble id1){
		IntDouble id;
		id=new IntDouble(Math.round(id1.getData().doubleValue()), true);
		return id;		
	}
	
	public static IntDouble add(IntDouble id1, IntDouble id2){
		IntDouble id;
		if (!id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()+id2.getData().doubleValue(), false);
		}else if (id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().intValue()+id2.getData().doubleValue(), false);
		}else if (!id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()+id2.getData().intValue(), false);
		}else {
			id=new IntDouble(id1.getData().intValue()+id2.getData().intValue(), true);
		}
		return id;		
	}
	
	public static IntDouble substract(IntDouble id1, IntDouble id2){
		IntDouble id;	
		if (!id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()-id2.getData().doubleValue(), false);
		}else if (id1.isInt() && !id2.isInt()){
			id=new IntDouble(id1.getData().intValue()-id2.getData().doubleValue(), false);
		}else if (!id1.isInt() && id2.isInt()){
			id=new IntDouble(id1.getData().doubleValue()-id2.getData().intValue(), false);
		}else{
			id=new IntDouble(id1.getData().intValue()-id2.getData().intValue(), true);
		}
		return id;		
	}
	
	public static IntDouble noArgFunction(String ident){
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
				return new IntDouble(Double.parseDouble(valueString), false);
			}else{
				return new IntDouble(Integer.parseInt(valueString), true);
			}
			
		} catch (Exception e) {
			Error.addEvaluationError("The function " +ident+" has an error.");
			e.printStackTrace();
			result=new IntDouble (1.0,false);
			return result;
		}
	}
	
	public static IntDouble argFunction(String ident, ArrayList<ArrayList<IntDouble>> idArray){
		IntDouble result;
		if (idArray.size()==1){
			if (ControlData.currSvMap.containsKey(ident)||ControlData.currTsMap.containsKey(ident)||ControlData.currDvMap.containsKey(ident)||ControlData.currAliasMap.containsKey(ident)) {
				ArrayList<IntDouble> idArray1 = idArray.get(0);
				if (idArray1.size()==1){
					String idName = idArray1.get(0).getName();
					for (int k=0; k<12; k++){
						if (idName.equals(TimeOperation.month_const[k])){
							Error.addEvaluationError(idName+" can't be used in "+ident+"("+idName+")");
							return new IntDouble (1.0, false);
						}
					}
					return getTimeSeries(ident, idArray1);
				}else{
					Error.addEvaluationError("Variable "+ident+" has number of indexes different from 1.");
					return new IntDouble (1.0, false);
				}
			}
		}
			
		Class function;
		try {
			Stack stack = new Stack();
			for (int i=0; i<idArray.size(); i++){
				ArrayList<IntDouble> idArray1 = idArray.get(i);
				int size =idArray1.size(); 
				if (size==1){
					IntDouble id=idArray1.get(0);
					if (id.isInt()){
						int value=id.getData().intValue();
						stack.push(value);
					}else{
						double value=id.getData().doubleValue();
						stack.push(value);
					}      
				}else if (size>1){
					Number[] valueArray=new Number[size];
					for (int j=0; j<size; j++){
						valueArray[j]=idArray1.get(j).getData();
					}
					stack.push(valueArray);
				}else{
					int ai=i+1;
					Error.addEvaluationError("The No. "+ai+" argument of function "+ident+" has no data.");				return new IntDouble (1.0, false);
				}
			}

			ExternalFunction ef;
			if (ControlData.allExternalFunctionMap.containsKey(ident)){
				ef=ControlData.allExternalFunctionMap.get(ident);
			}else{
				function = Class.forName("wrimsv2.external.Function" + ident);
				ef = (ExternalFunction)function.newInstance();
				ControlData.allExternalFunctionMap.put(ident, ef);
			}
			ef.execute(stack);
			if (stack.size()>1){
				for (int i=0; i<idArray.size(); i++){
					ArrayList<IntDouble> idArray1 = idArray.get(i);
					int size =idArray1.size(); 
					if (size==1){
						IntDouble id=idArray1.get(0);
						if (id.isInt()){
							int value=(Integer) stack.pop();
							setSvarIntValue(id, value);
						}else{
							double value=(Double) stack.pop();
							setSvarDoubleValue(id, value);
						}      
					}else if (size>1){
						IntDouble id=idArray1.get(0);
						if (id.isInt()){
							int[] valueArray=new int[size];
							valueArray=(int[])stack.pop();
							for (int j=0; j<size; j++){
								setSvarIntValue(idArray1.get(j), valueArray[j]);
							}
						}else{
							double[] valueArray=new double[size];
							valueArray=(double[])stack.pop();
							for (int j=0; j<size; j++){
								setSvarDoubleValue(idArray1.get(j), valueArray[j]);
							}
						}
					}else{
						int ai=i+1;
						Error.addEvaluationError("The No. "+ai+" argument of function "+ident+" has no data.");				return new IntDouble (1.0, false);
					}
				}
			}

			String valueString=stack.pop().toString();
			if (valueString.contains(".")){       
				return new IntDouble(Double.parseDouble(valueString), false);
			}else{
				return new IntDouble(Integer.parseInt(valueString), true);
			}
			
		} catch (Exception e) {
			Error.addEvaluationError("The function " +ident+" has an error.");
			e.printStackTrace();
			result=new IntDouble (1.0,false);
			return result;
		}
	}
	
	public static IntDouble getTimeSeries(String ident, ArrayList<IntDouble> idArray){		
		IntDouble result;
		boolean isSumIndex=false;
		int indexValue=0;
		boolean isIndexStart=true;
		
		IntDouble id=idArray.get(0);	
		
		if (!id.isInt()){
			Error.addEvaluationError("The index of "+ident+" should be integer.");
			result=new IntDouble (1.0,false);
			return result;
		}

		int idValue=id.getData().intValue();
		ParallelVars prvs = TimeOperation.findTime(idValue);
		
		double value;
		if (ControlData.currDvMap.containsKey(ident)||ControlData.currAliasMap.containsKey(ident)){
			value=dvarAliasTimeSeries(ident,id.getData().intValue(), prvs);
		}else{
			if (ControlData.currSvMap.containsKey(ident)){ 
				if (idValue==0)	{
					return ControlData.currSvMap.get(ident).getData().copyOf();
				}else if(idValue>0){
					String futSvName=ident+"__fut__"+idValue;
					if (ControlData.currSvFutMap.containsKey(futSvName)){
						return ControlData.currSvFutMap.get(futSvName).getData().copyOf();
					}else{
						if (!ControlData.ignoreError) Error.addEvaluationError(futSvName+", the future value of "+ident+" is used before defined.");
						return new IntDouble (1.0,false);
					}
				}
			}
			value=svarTimeSeries(ident, idValue, prvs);
		}
		
		return new IntDouble (value, false);
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
				prvs = TimeOperation.findTime(idValue);
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
			if (indexValue>=0 && (ControlData.currEvalTypeIndex==0 || ControlData.currEvalTypeIndex==7)){
				Error.addEvaluationError(ident + " at timestep " +indexValue+" doesn't have value");
				return 1.0;
			}			
			String newName = ident+"__fut__"+indexValue;
			Map<String, Dvar> dvMap = SolverData.getDvarMap();
			Map<String, Alias> asFutMap = ControlData.currModelDataSet.asFutMap;
			if (dvMap.containsKey(newName)){
				return dvMap.get(newName).getData().getData().doubleValue();
			}else if(asFutMap.containsKey(newName)){
				return asFutMap.get(newName).getData().getData().doubleValue();
			}else{
				Error.addEvaluationError("Can't access decision variable after the current time step.");
				return 1.0;
			}
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
		int index = timeSeriesIndex(dds, prvs);
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
			String newName = ident+"__fut__"+indexValue;
			Map<String, Dvar> dvMap = SolverData.getDvarMap();
			Map<String, Alias> asFutMap = ControlData.currModelDataSet.asFutMap;
			if (dvMap.containsKey(newName)){
				return dvMap.get(newName).getData().getData().doubleValue();
			}else if(asFutMap.containsKey(newName)){
				return asFutMap.get(newName).getData().getData().doubleValue();
			}else{
				Error.addEvaluationError("Can't access decision variable after the current time step.");
				return 1.0;
			}
		}
		
		/*
		int index=indexValue+ControlData.currTimeStep.get(ControlData.currCycleIndex);
		if (index>=0){
			DssDataSetFixLength dds=DataTimeSeries.dvAliasTSCycles.get(ci).get(entryNameTS);
			if (dds==null){
				Error.addEvaluationError(ident + " at timestep " +indexValue+" of No. "+ci+" cycle doesn't have value.");
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
		int index = timeSeriesIndex(dds, prvs);
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
		int sMonth=st.getMonth()+1; //HEC DSS7 uses getMonth()+1. However, Vista/HecDSS6 uses getMonth()bbecause dss data store at 24:00 Jan31, 1921 is considered to store at 0:00 Feb 1, 1921
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
			index=(int)indexValue+1;  //HEC DSS7 uses indexValue+1; Vista/Hec DSS6 uses indexValue+2
		}
		return index;
	}
	
	public static IntDouble timeseries(){
		String svName=ControlData.currEvalName;
		ParallelVars prvs = TimeOperation.findTime(0);
		double value=svarTimeSeries(svName, 0, prvs);
		return new IntDouble(value,false);
	}
	
	public static double timeseries(String tsName){
		ParallelVars prvs = TimeOperation.findTime(0);
		return svarTimeSeries(tsName, 0, prvs);
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
	
	public static IntDouble pastTSFV(String ident, IntDouble id1, ArrayList<ArrayList<IntDouble>> idArray, ParallelVars prvs) {
		//turn off when multi-dimensional array is used
		if (idArray.size()!=1){
			Error.addEvaluationError("The future index of array variable "+ident+" has to be a single integer.");
			return new IntDouble(1.0,false);
		}
		//
		
		ArrayList<IntDouble> id2Array = idArray.get(0);
		if (id2Array.size() !=1){
			Error.addEvaluationError("The future index of array variable "+ident+" has to be a single integer but not a range.");
			return new IntDouble(1.0,false);
		}
		IntDouble id2 = id2Array.get(0);
		
		if (!id1.isInt() || !id2.isInt()){
			Error.addEvaluationError("The index of array variable "+ident+" has to be an integer.");
			return new IntDouble(1.0,false);
		}
		int i1 = id1.getData().intValue();
		int i2 = id2.getData().intValue();
		
		if (i1>=0){
			Error.addEvaluationError("The second index of array variable "+ident+" has to be less than 0 in non-constraint statements.");
			return new IntDouble(1.0,false);
		}
		if (i2<0){
			Error.addEvaluationError("The first index of array variable "+ident+" has to be larger than or equal to 0.");
			return new IntDouble(1.0,false);
		}
		if (!ControlData.currDvMap.containsKey(ident) && !ControlData.currAliasMap.containsKey(ident)){
			Error.addEvaluationError("The array variable "+ident+" is not a dvar or alias. The value from the past time step could not be retrieved.");
			return new IntDouble(1.0,false);
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
					return new IntDouble(1.0,false);
				}else if (index>=0){
					return new IntDouble(datafl[index], false);
				}
			}
			
			if (DataTimeSeries.dvAliasInit.containsKey(entryNameTS)){
				if (!getDVAliasInitTimeseries(ident)){
					Error.addEvaluationError("Initial file doesn't have data for decision vairiable/alias " +vn);
					return new IntDouble(1.0,false);
				}
				
				DssDataSet dds=DataTimeSeries.dvAliasInit.get(entryNameTS);
				int index = timeSeriesIndex(dds, prvs);
				ArrayList<Double> data=dds.getData();
				if (index>=0 && index<data.size()){
					double result=data.get(index);
					if (result==-901.0 || result==-902.0){
						Error.addEvaluationError("Initial file doesn't have data for decision vairiable/alias " + vn + " at time step "+i1+".");
						return new IntDouble(1.0,false);
					}
					return new IntDouble(result, false);
				}else{
					Error.addEvaluationError("Initial file doesn't have data for decision vairiable/alias " + vn + " at time step "+i1+".");
					return new IntDouble(1.0,false);
				}
			}else{
				Error.addEvaluationError(vn + " doesn't have value.");
				return new IntDouble(1.0,false);
			}
		}else{
			Error.addEvaluationError(vn + "doesn't exist.");
			return new IntDouble(1.0,false);
		}
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
	
	public static IntDouble pastCycleTimeArray(String ident, String cycle, IntDouble id){
				
		IntDouble data=new IntDouble(1.0,false);
		if (!id.isInt()){
			Error.addEvaluationError("Time array index of "+ident+" is not an integer.");
			return data;
		}
		int index=id.getData().intValue();
		if (index<0){
			ArrayList<IntDouble> idArray=new ArrayList<IntDouble>();
			idArray.add(id);
			ModelDataSet mds=ControlData.currStudyDataSet.getModelDataSetMap().get(cycle);
			if (mds.dvMap.containsKey(ident) || mds.asMap.containsKey(ident)){
				ParallelVars prvs = TimeOperation.findTime(index);
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
	
	public static IntDouble pastCycleIndexTimeArray(String ident, int pci, IntDouble id){
		IntDouble data=new IntDouble(1.0,false);
		int ci=ControlData.currCycleIndex+pci;
		if (ci<0){
			Error.addEvaluationError("The "+ci+" cycle from the current cycle is unvailable.");
			return new IntDouble(1.0,false);
		}
		if (!id.isInt()){
			Error.addEvaluationError("Time array index of "+ident+" is not an integer.");
			return data;
		}
		int index=id.getData().intValue();
		StudyDataSet sds = ControlData.currStudyDataSet;
		String cycle=sds.getModelList().get(ci);
		if (index<0){
			ArrayList<IntDouble> idArray=new ArrayList<IntDouble>();
			idArray.add(id);
			ModelDataSet mds=ControlData.currStudyDataSet.getModelDataSetMap().get(cycle);
			if (mds.dvMap.containsKey(ident) || mds.asMap.containsKey(ident)){
				ParallelVars prvs = TimeOperation.findTime(index);
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
	
	public static ArrayList<IntDouble> trunk_timeArray(String ident, IntDouble start, IntDouble end){
		ArrayList<IntDouble> idArray=new ArrayList<IntDouble>();
		if (!start.isInt()){
			Error.addEvaluationError("The starting index of trunk data for variable " + ident + " is not an integer.");
			idArray.add(new IntDouble(1.0, false));
			return idArray;
		}else if (!end.isInt()){
			Error.addEvaluationError("The ending index of trunk data for variable " + ident + " is not an integer.");
			idArray.add(new IntDouble(1.0, false));
			return idArray;
		}
		int si=start.getData().intValue();
		int ei=end.getData().intValue();
		
		if (si>ei){
			for (int i=si; i>=ei; i--){
				ArrayList<IntDouble> indexArray1=new ArrayList<IntDouble> ();
				IntDouble index = new IntDouble(i, true);
				indexArray1.add(index);
				ArrayList<ArrayList<IntDouble>> indexArray = new ArrayList<ArrayList<IntDouble>>();
				indexArray.add(indexArray1);
				IntDouble id=ValueEvaluation.argFunction(ident, indexArray);
				id.setIndex(i);
				id.setName(ident);
				idArray.add(id);
			}
		}else{
			for (int i=si; i<=ei; i++){
				ArrayList<IntDouble> indexArray1=new ArrayList<IntDouble> ();
				IntDouble index = new IntDouble(i, true);
				indexArray1.add(index);
				ArrayList<ArrayList<IntDouble>> indexArray = new ArrayList<ArrayList<IntDouble>>();
				indexArray.add(indexArray1);
				IntDouble id=ValueEvaluation.argFunction(ident, indexArray);
				id.setIndex(i);
				id.setName(ident);
				idArray.add(id);
			}
		}
		return idArray;
	}
	
	public static IntDouble max(IntDouble id1, IntDouble id2){
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
	
	public static IntDouble min(IntDouble id1, IntDouble id2){
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
	
	public static IntDouble intFunc(IntDouble id1){
		IntDouble id;
		if (!id1.isInt()){
			id=new IntDouble(((int)id1.getData().doubleValue()), true);
			return id;
		}
		return id1;
	}
	
	public static IntDouble realFunc(IntDouble id1){
		IntDouble id;
		if (id1.isInt()){
			id=new IntDouble((id1.getData().doubleValue()), false);
			return id;
		}
		return id1;
	}
	
	public static IntDouble abs(IntDouble id1){
		IntDouble id;
		if (id1.isInt()){
			id=new IntDouble(Math.abs(id1.getData().intValue()), true);
		}else{
			id=new IntDouble(Math.abs(id1.getData().doubleValue()), false);
		}
		return id;
	}
	
	public static IntDouble exp(IntDouble id1){
		IntDouble id;
		if (id1.isInt()){
			id=new IntDouble(Math.exp(id1.getData().intValue()), false);
		}else{
			id=new IntDouble(Math.exp(id1.getData().doubleValue()), false);
		}
		return id;
	}
	
	public static IntDouble log(IntDouble id1){
		IntDouble id;
		if (id1.isInt()){
			id=new IntDouble(Math.log(id1.getData().intValue()), false);
		}else{
			id=new IntDouble(Math.log(id1.getData().doubleValue()), false);
		}
		return id;
	}
	
	public static IntDouble log10(IntDouble id1){
		IntDouble id;
		if (id1.isInt()){
			id=new IntDouble(Math.log10(id1.getData().intValue()), false);
		}else{
			id=new IntDouble(Math.log10(id1.getData().doubleValue()), false);
		}
		return id;
	}
	
	public static IntDouble pow(IntDouble id1, IntDouble id2){
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
	
	public static IntDouble sin(IntDouble id1){	
		double degrees = id1.getData().doubleValue();
		double radians = Math.toRadians(degrees);
		return new IntDouble(Math.sin(radians), false);		
	}
	
	public static IntDouble cos(IntDouble id1){	
		double degrees = id1.getData().doubleValue();
		double radians = Math.toRadians(degrees);
		return new IntDouble(Math.cos(radians), false);		
	}
	
	public static IntDouble tan(IntDouble id1){	
		double degrees = id1.getData().doubleValue();
		double radians = Math.toRadians(degrees);
		return new IntDouble(Math.tan(radians), false);		
	}
	
	public static IntDouble cot(IntDouble id1){	
		double degrees = id1.getData().doubleValue();
		if (degrees==90.0){
			return new IntDouble(0.0, false);
		}
		double radians = Math.toRadians(degrees);
		return new IntDouble(1.0/Math.tan(radians), false);		
	}
	
	public static IntDouble asin(IntDouble id1){
		double value = id1.getData().doubleValue();
		double radians = Math.asin(value);
		return new IntDouble(Math.toDegrees(radians), false);		
	}
	
	public static IntDouble acos(IntDouble id1){
		double value = id1.getData().doubleValue();
		double radians = Math.acos(value);
		return new IntDouble(Math.toDegrees(radians), false);		
	}
	
	public static IntDouble atan(IntDouble id1){
		double value = id1.getData().doubleValue();
		double radians = Math.atan(value);
		return new IntDouble(Math.toDegrees(radians), false);		
	}
	
	public static IntDouble acot(IntDouble id1){
		double value = id1.getData().doubleValue();
		if (value == 0.){
			return new IntDouble(90.0, false);
		}
		double radians = Math.atan(1.0/value);
		return new IntDouble(Math.toDegrees(radians), false);		
	}
	
	public static IntDouble exceedance(String tsName, IntDouble exc_id, String selMon, String syStr, String smStr, String sdStr, String eyStr, String emStr, String edStr){
		String entryNameTS=DssOperation.entryNameTS(tsName, ControlData.timeStep);
		if (DataTimeSeries.svTS.containsKey(entryNameTS)){
			DssDataSet dds = DataTimeSeries.svTS.get(entryNameTS);
			int sy = Integer.parseInt(syStr);
			int sd = Integer.parseInt(sdStr);
			int ey = Integer.parseInt(eyStr);
			int ed = Integer.parseInt(edStr);
			int sm = TimeOperation.monthValue(smStr);
			int em = TimeOperation.monthValue(emStr);
			double exc = exc_id.getData().doubleValue();
			
			if (exc<=0.0 || exc>1.0){
				Error.addEvaluationError("Exceedance level must be >0.0 and <=1.0");
				return new IntDouble (1.0, false);
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
			return new IntDouble(value, false);
		}else{
			Error.addEvaluationError(tsName+" is not a timeseries variable used in the Exceendance funciton for the time step of "+ControlData.timeStep+".");
			return new IntDouble (1.0, false);
		}	
	}
	
	public static IntDouble exceedance_tsi(String tsName, IntDouble exc_id, String selMon, String syStr, String smStr, String sdStr, String eyStr, String emStr, String edStr){
		String entryNameTS=DssOperation.entryNameTS(tsName, ControlData.timeStep);
		if (DataTimeSeries.svTS.containsKey(entryNameTS)){
			DssDataSet dds = DataTimeSeries.svTS.get(entryNameTS);
			int sy = Integer.parseInt(syStr);
			int sd = Integer.parseInt(sdStr);
			int ey = Integer.parseInt(eyStr);
			int ed = Integer.parseInt(edStr);
			int sm = TimeOperation.monthValue(smStr);
			int em = TimeOperation.monthValue(emStr);
			double exc = exc_id.getData().doubleValue();
			
			if (exc<=0.0 || exc>1.0){
				Error.addEvaluationError("Exceedance level must be >0.0 and <=1.0");
				return new IntDouble (0, true);
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
			return new IntDouble(tsi, true);
		}else{
			Error.addEvaluationError(tsName+" is not a timeseries variable used in the Exceendance_TSI funciton for the time step of "+ControlData.timeStep+".");
			return new IntDouble (0, true);
		}	
	}
	
	public static IntDouble daysIn(){
		int days=TimeOperation.numberOfDays(ControlData.currMonth, ControlData.currYear);
		return new IntDouble(days, true);
	}
	
	public static IntDouble daysInTimeStep(){
		if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
			return daysIn();
		}else{
			IntDouble id=new IntDouble(1, true);
			return new IntDouble(1, true);
		}
	}
	
	public static IntDouble tafcfs_term(String ident, IntDouble id){
		ParallelVars prvs = new ParallelVars();
		if (id==null){
			prvs.dataMonth=ControlData.currMonth;
			prvs.dataYear=ControlData.currYear;
		}else{
			if (!id.isInt()){
				Error.addEvaluationError("The index of "+ident+" should be integer.");
			}
			prvs=TimeOperation.findTime(id.getData().intValue());
		}
		double convert = tafcfs(ident, prvs);
		return new IntDouble(convert, false);
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
	
	public static IntDouble term_YEAR(){
		return new IntDouble(TimeOperation.waterYearValue(), true);
	}
	
	public static IntDouble term_MONTH(){
		return new IntDouble(TimeOperation.waterMonthValue(ControlData.currMonth), true);
	}
	
	public static IntDouble term_DAY(){
		return new IntDouble(ControlData.currDay, true);
	}
	
	public static IntDouble  term_MONTH_CONST(String month){
		int monthValue=TimeOperation.monthValue(month);
		return new IntDouble(TimeOperation.waterMonthValue(monthValue), true, month, 0);
	}
	
	public static IntDouble term_PASTMONTH(String pastMonth){
		pastMonth=pastMonth.substring(4);
		int pastMonthValue=TimeOperation.monthValue(pastMonth);
		int index=pastMonthValue-ControlData.currMonth;
		if (index>=0){
			index=index-12;
		}
		return new IntDouble(index,true);
	}
	
	public static IntDouble term_ARRAY_ITERATOR(ParallelVars prvs){
		return new IntDouble(prvs.timeArrayIndex, true);
	}
	
	public static void sumExpression_IDENT(String ident, Stack<LoopIndex> sumIndex){
		//To Do: check if svar, dvar, alias contains ident
		LoopIndex li=new LoopIndex(ident, 0, false, 0, 0, 0);
		sumIndex.push(li);
	}
	
	public static void initSumExpression(IntDouble id1, IntDouble id2, String s, Stack<LoopIndex> sumIndex){
		LoopIndex li=sumIndex.pop();
		li.step=1;
		if (!s.equals("")){
			li.step=convertStringToInt(s);
		}
		if (!id1.isInt()){
			Error.addEvaluationError("the starting index should be integer");
		}
		if (!id2.isInt()){
			Error.addEvaluationError("the ending index should be integer");
		}
		li.start=id1.getData().intValue();
		li.end=id2.getData().intValue();
		li.setValue(li.start);
		li.setIndexStart(true);
		sumIndex.push(li);
		if (li.start>li.end && li.step>0){
			ControlData.ignoreError=true;
		}else if (li.start<li.end && li.step<0){
			ControlData.ignoreError=true;
		}
	}
	
	public static IntDouble sumExpression(IntDouble id, String expression, Stack<LoopIndex> sumIndex){	
		ControlData.ignoreError=false;
		LoopIndex li=sumIndex.pop();
		if (li.step>=0){
			if (li.start>li.end) return new IntDouble(0.0, false);
			li.start=li.start+li.step;
			if (li.start>li.end) return id;
			sumIndex.push(li);
			for (int i=li.start; i<=li.end; i=i+li.step){
				li=sumIndex.pop();
				li.setValue(i);
				li.setIndexStart(true);
				sumIndex.push(li);
				ANTLRStringStream stream = new ANTLRStringStream("v: "+expression); 
				ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
				TokenStream tokenStream = new CommonTokenStream(lexer);
				ValueEvaluatorParser evaluator = new ValueEvaluatorParser(tokenStream);
				evaluator.setSumIndex(sumIndex);
				try{
					evaluator.evaluator();
				}catch (RecognitionException e){
					Error.addEvaluationError(e.toString());
				}
			
				IntDouble id0=evaluator.evalValue;
				id=add(id, id0);
			}
		}else{
			if (li.start<li.end) return new IntDouble(0.0, false);
			li.start=li.start+li.step;
			if (li.start<li.end) return id;
			sumIndex.push(li);
			for (int i=li.start; i>=li.end; i=i+li.step){
				li=sumIndex.pop();
				li.setValue(i);
				li.setIndexStart(true);
				sumIndex.push(li);
				ANTLRStringStream stream = new ANTLRStringStream("v: "+expression); 
				ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
				TokenStream tokenStream = new CommonTokenStream(lexer);
				ValueEvaluatorParser evaluator = new ValueEvaluatorParser(tokenStream);
				evaluator.setSumIndex(sumIndex);
				try{
					evaluator.evaluator();
				}catch (RecognitionException e){
					Error.addEvaluationError(e.toString());
				}
			
				IntDouble id0=evaluator.evalValue;
				id=add(id, id0);
			}
		}
		sumIndex.pop();
		return id;
	}
	
	public static Number assignWhereStatement(IntDouble id){
		return id.getData();
	}
	
	public static IntDouble tableSQL(String table, String select, HashMap<String, Number> where, HashMap<String, Number> given, String use){
		IntDouble id;
		if (where==null){	
			return TableOperation.findData(table, select, given, use);
		}else{
			return TableOperation.findData(table, select, where, given, use);
		}
	}
	
	public static IntDouble expressionInput(IntDouble id){
		return id;
	}
	
	public static void setSvarIntValue(IntDouble id, int value){
		String name=id.getName();
		int index=id.getIndex();
		
		if (ControlData.currSvMap.containsKey(name)){ 
			if (index==0)	{
				ControlData.currSvMap.get(name).getData().setData(value);
			}else if(index>0){
				String futSvName=name+"__fut__"+index;
				if (ControlData.currSvFutMap.containsKey(futSvName)){
					ControlData.currSvFutMap.get(futSvName).getData().setData(value);
				}
			}
		}
	}
	
	public static void setSvarDoubleValue(IntDouble id, double value){
		String name=id.getName();
		int index=id.getIndex();
		
		if (ControlData.currSvMap.containsKey(name)){ 
			if (index==0)	{
				ControlData.currSvMap.get(name).getData().setData(value);
			}else if(index>0){
				String futSvName=name+"__fut__"+index;
				if (ControlData.currSvFutMap.containsKey(futSvName)){
					ControlData.currSvFutMap.get(futSvName).getData().setData(value);
				}
			}
		}
	}
}
