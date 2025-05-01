package wrimsv2.commondata.wresldata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import wrimsv2.components.IntDouble;
import wrimsv2.evaluator.ValueEvaluatorParser;


public class StudyDataSet implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private String absMainFilePath="";
	
	private ArrayList<String> parameterList = new ArrayList<String>();
	private LinkedHashMap<String, Svar> parameterMap = new LinkedHashMap<String, Svar>();
	public LinkedHashMap<String, Svar> controlDataParameterMap = new LinkedHashMap<String, Svar>();
	
	private ArrayList<String> modelList = new ArrayList<String>();
	private ArrayList<String> modelConditionList = new ArrayList<String>();
	private ArrayList<String> modelTimeStepList = new ArrayList<String>();
	private ArrayList<ValueEvaluatorParser> modelConditionParsers=new ArrayList<ValueEvaluatorParser>();
	
	///  < timeseries name, timeseries object > 
	private Map<String, Timeseries> timeseriesMap = new HashMap<String, Timeseries>();
	private Map<String, ArrayList<String>> timeseriesTimeStepMap = new HashMap<String, ArrayList<String>>();
	
	///  < modelName, modelDataSet > 		
	private Map<String, ModelDataSet> modelDataSetMap = new HashMap<String, ModelDataSet>();

	/// this map contains value of vars needed for WRESL syntax: varName[cycleName] 
	/// < VarName, < CycleName, Value >>		
	private Map<String, Map<String, IntDouble>> varCycleValueMap = new HashMap<String, Map<String, IntDouble>>();
	private Map<String, Map<String, IntDouble>> varTimeArrayCycleValueMap = new HashMap<String, Map<String, IntDouble>>();
	private Map<String, Map<String, IntDouble>> varCycleIndexValueMap = new HashMap<String, Map<String, IntDouble>>();
	private ArrayList<String> varCycleIndexList = new ArrayList<String>();
	private ArrayList<String> dvarTimeArrayCycleIndexList = new ArrayList<String>();
	
	public LinkedHashSet<String> allIntDv=new LinkedHashSet<String>();
	public Map<Integer,LinkedHashSet<String>> cycIntDvMap=new HashMap<Integer,LinkedHashSet<String>>();
	
	public ArrayList<String> getParameterList() {
		return new ArrayList<String>(parameterList);
	}

	public void setParameterList(ArrayList<String> parameterList) {
		this.parameterList = parameterList;
	}
	
	public LinkedHashMap<String, Svar> getParameterMap() {
		return new LinkedHashMap<String, Svar>(parameterMap);
	}
	
	public void setParameterMap(LinkedHashMap<String, Svar> parameterMap) {
		this.parameterMap = parameterMap;
	}
	
	public Map<String, Timeseries> getTimeseriesMap() {
		return new HashMap<String, Timeseries>(timeseriesMap);
	}

	public void setTimeseriesMap(Map<String, Timeseries> timeseriesMap) {
		this.timeseriesMap = timeseriesMap;
	}
	
	public Map<String, ArrayList<String>> getTimeseriesTimeStepMap() {
		return new HashMap<String, ArrayList<String>>(timeseriesTimeStepMap);
	}
	
	public void setTimeseriesTimeStepMap(Map<String, ArrayList<String>> timeseriesTimeStepMap) {
		this.timeseriesTimeStepMap = timeseriesTimeStepMap;
	}
	
	public String getAbsMainFilePath() {
		return new String(absMainFilePath);
	}

	public void setAbsMainFilePath(String absMainFilePath) {
		this.absMainFilePath = absMainFilePath;
	}

	public ArrayList<String> getModelList() {
		return new ArrayList<String>(modelList);
	}

	public void setModelList(ArrayList<String> modelList) {
		this.modelList = modelList;
	}

	public ArrayList<String> getModelConditionList() {
		return new ArrayList<String>(modelConditionList);
	}

	public ArrayList<String> getModelTimeStepList() {
		return modelTimeStepList;
	}
	
	public void setModelConditionList(ArrayList<String> modelConditionList) {
		this.modelConditionList = modelConditionList;
	}
	
	public void setModelTimeStepList(ArrayList<String> modelTimeStepList) {
		this.modelTimeStepList = modelTimeStepList;
	}
	
	public ArrayList<ValueEvaluatorParser> getModelConditionParsers() {
		return modelConditionParsers;
	}
	
	public void setModelConditionParsers(ArrayList<ValueEvaluatorParser> modelConditionParsers) {
		this.modelConditionParsers=modelConditionParsers;
	}

	public Map<String, ModelDataSet> getModelDataSetMap() {
		return new HashMap<String, ModelDataSet>(modelDataSetMap);
	}

	public void setModelDataSetMap(Map<String, ModelDataSet> modelDataSetMap) {
		this.modelDataSetMap = modelDataSetMap;
	}

	public Map<String, Map<String, IntDouble>> getVarCycleValueMap() {
		return this.varCycleValueMap;
	}
	
	public void setVarCycleValueMap(Map<String, Map<String, IntDouble>> varCycleValueMap) {
		this.varCycleValueMap = varCycleValueMap;
	}
	
	public Map<String, Map<String, IntDouble>> getVarTimeArrayCycleValueMap() {
		return this.varTimeArrayCycleValueMap;
	}
	
	public Map<String, Map<String, IntDouble>> getVarCycleIndexValueMap() {
		return this.varCycleIndexValueMap;
	}
	
	public void setVarCycleIndexValueMap(Map<String, Map<String, IntDouble>> varCycleIndexValueMap) {
		this.varCycleIndexValueMap = varCycleIndexValueMap;
	}
	
	public void clearVarTimeArrayCycleValueMap(){
		varTimeArrayCycleValueMap=new HashMap<String, Map<String, IntDouble>>();
	}
	
	public void clearVarCycleIndexByTimeStep(){
		varCycleIndexValueMap = new HashMap<String, Map<String, IntDouble>>();
		dvarTimeArrayCycleIndexList = new ArrayList<String> ();
	}
	
	public ArrayList<String> getVarCycleIndexList(){
		return varCycleIndexList;
	}
	
	public void setVarCycleIndexList(ArrayList<String> varCycleIndexList){
		this.varCycleIndexList=new ArrayList<String>();
		for (int i=0; i<varCycleIndexList.size(); i++){
			this.varCycleIndexList.add(varCycleIndexList.get(i));
		}
	}

	public ArrayList<String> getDvarTimeArrayCycleIndexList(){
		return dvarTimeArrayCycleIndexList;
	}
}
