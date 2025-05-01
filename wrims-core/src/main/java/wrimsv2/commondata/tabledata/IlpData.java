package wrimsv2.commondata.tabledata;

import java.util.ArrayList;
import java.util.Map;

public class IlpData {
	private static ArrayList<Map<String, ArrayList<String>>>  nodeArray = new ArrayList<Map<String, ArrayList<String>>>();
	private static ArrayList<Map<String, Dvar>>  dvarArray = new ArrayList<Map<String, Dvar>>();
    private static ArrayList<Map<String, Svar>>  svarArray = new ArrayList<Map<String, Svar>>();
    private static ArrayList<ArrayList<String>>  outputSvarArray = new ArrayList<ArrayList<String>>();
    private static ArrayList<Map<String, String>>  weightArray = new ArrayList<Map<String, String>>();
    private static ArrayList<ArrayList<String>>  fileArray = new ArrayList<ArrayList<String>>();
    private static ArrayList<Map<String, Constraint>>  constraintArray = new ArrayList<Map<String, Constraint>> ();
    private static ArrayList<Map<String, LRWeight>>  lgrArray = new ArrayList<Map<String, LRWeight>>();
    private static ArrayList<Map<String, LRWeight>>  rglArray = new ArrayList<Map<String, LRWeight>>();
    private static ArrayList<Map<String, String>>  functionArray = new ArrayList<Map<String, String>>();
    private static ArrayList<Map<String, Alias>>  aliasArray = new ArrayList<Map<String, Alias>>();
    
    public static ArrayList<Map<String, Dvar>> getDvarArray(){
    	return dvarArray;
    }
    
    public static void addDvarToArray(Map<String, Dvar> dvar){
    	dvarArray.add(dvar);
    }
    
    public static ArrayList<Map<String, Svar>> getSvarArray(){
    	return svarArray;
    }
    
    public static void addSvarToArray(Map<String, Svar> svar){
    	svarArray.add(svar);
    }
    
    public static ArrayList<ArrayList<String>> getOutputSvarArray(){
    	return outputSvarArray;
    }
    
    public static void addOutputSvarToArray(ArrayList<String> outputSvar){
    	outputSvarArray.add(outputSvar);
    }
    
    public static ArrayList<Map<String, String>>  getWeightArray(){
    	return weightArray;
    }
    
    public static void addWeightToArray(Map<String, String> weight){
    	weightArray.add(weight);
    }
    
    public static ArrayList<Map<String, Constraint>>  getConstraintArray(){
    	return constraintArray;
    }
    
    public static void addConstraintToArray(Map<String, Constraint> constraint){
    	constraintArray.add(constraint);
    }
    
    public static ArrayList<Map<String, LRWeight>>  getLgrArray(){
    	return lgrArray;
    }
    
    public static void addLgrToArray(Map<String, LRWeight> lgr){
    	lgrArray.add(lgr);
    }
    
    public static ArrayList<Map<String, LRWeight>>  getRglArray(){
    	return rglArray;
    }
    
    public static void addRglToArray(Map<String, LRWeight> rgl){
    	rglArray.add(rgl);
    }
    
    public static ArrayList<Map<String, String>>  getFunctionArray (){
    	return functionArray;
    }
    
    public static void addFunctionToArray (Map<String, String> function){
    	functionArray.add(function);
    }
    
    public static ArrayList<Map<String, Alias>>  getAliasArray(){
    	return aliasArray;
    }
    
    public static void addAliasToArray(Map<String, Alias> alias){
    	aliasArray.add(alias);
    }
}