package wrimsv2.wreslparser.elements;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import java.util.Map;

import wrimsv2.commondata.wresldata.Alias;
import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.External;
import wrimsv2.commondata.wresldata.Goal;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.Param;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.commondata.wresldata.Svar;
import wrimsv2.commondata.wresldata.Timeseries;
import wrimsv2.commondata.wresldata.WeightElement;


public class WriteCSV {
	  
	  //static Map<String, List<String>> mapStringList = new HashMap<String, List<String>>();
	  //private static PrintWriter out;
	
	  public static String sequence_header ="CYCLE,CONDITION,TIMESTEP";
	  public static String weight_header ="DVAR,TIME_ARRAY_SIZE,CONDITION,WEIGHT";
	  public static String external_header ="FUNCTION,FILE,FROM_WRESL_FILE";
	  public static String svar_header   ="NAME,TIME_ARRAY_SIZE,CASE,ORDER,CONDITION,EXPRESSION,DEPENDANT,FROM_WRESL_FILE,NEED_VAR_FROM_CYCLE,USED_IN_LATER_CYCLE";
	  public static String timeseries_header ="NAME,B_PART,TYPE,UNITS,CONVERT_TO_UNITS,FROM_WRESL_FILE";
	  public static String timeseries_timestep_header ="NAME,B_PART,TIMESTEP";
	  public static String dvar_header ="NAME,TIME_ARRAY_SIZE,CONDITION,LOWER_BOUND,UPPER_BOUND,INTEGER,UNITS,TYPE,FROM_WRESL_FILE,USED_IN_LATER_CYCLE";	  
	  public static String alias_header ="NAME,TIME_ARRAY_SIZE,TYPE,UNITS,EXPRESSION,DEPENDANT,FROM_WRESL_FILE,NEED_VAR_FROM_CYCLE,USED_IN_LATER_CYCLE";
	  public static String goal_header = "NAME,TIME_ARRAY_SIZE,CASE,ORDER,CONDITION,EXPRESSION,DEPENDANT,FROM_WRESL_FILE,NEED_VAR_FROM_CYCLE,";
	  private static ModelDataSet currentModelDataSet;
	  
	  
	public static void study(StudyDataSet sd, String outParent) {
			
			if (sd==null) return;
		
			Map<String,ModelDataSet> modelDataSetMap = sd.getModelDataSetMap();
		
			output(modelDataSetMap,outParent);
			
			try {
				PrintWriter out_seq = Tools.openFile(outParent, "SEQUENCE.csv");
				out_seq.print(WriteCSV.sequence_header + "\n");	
				sequence(sd.getModelList(), sd.getModelConditionList(), out_seq);
				out_seq.close();
				
				if (sd.getTimeseriesMap().keySet().size()>0) {
					PrintWriter out_timeseries_wholeStudy = Tools.openFile(outParent, "TIMESERIES.csv");
					out_timeseries_wholeStudy.print(WriteCSV.timeseries_header + "\n");			
					timeseries_wholeStudy( sd.getTimeseriesMap(), out_timeseries_wholeStudy);
					out_timeseries_wholeStudy.close();
					
					
					PrintWriter out_timeseriesTimeStep = Tools.openFile(outParent, "TIMESERIES_TIMESTEP.csv");
					out_timeseriesTimeStep.print(WriteCSV.timeseries_timestep_header + "\n");						
					timeseriesTimeStep( sd, out_timeseriesTimeStep);
					out_timeseriesTimeStep.close();
				}
				
			}
			catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	  
	public static void study(StudyConfig sc, Map<String, ModelDataSet> modelDataMap, String outParent) {
				
		  	Tools.deleteDir(outParent);
		
			output(modelDataMap,outParent);
			
			try {
				PrintWriter out_seq = Tools.openFile(outParent, "SEQUENCE.csv");
				out_seq.print(WriteCSV.sequence_header + "\n");			
				sequence(sc.sequenceMap, sc.sequenceOrder, out_seq);
				out_seq.close();
			}
			catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}	  
	  
	public static void output(Map<String, ModelDataSet> modelDataMap, String outParent) {

		for (String model : modelDataMap.keySet()) {

			String outFolder = outParent + File.separator + model;
			
			try {
				dataset(modelDataMap.get(model), outFolder);
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static void dataset(ModelDataSet ds, String outFolder) throws IOException {

		PrintWriter out_ex;
		PrintWriter out_svTs;
		PrintWriter out_sv;
		PrintWriter out_sv_unknown;
		PrintWriter out_dv;
		PrintWriter out_goal;
		PrintWriter out_alias;
		PrintWriter out_alias_unknown;
		PrintWriter out_wt;
		PrintWriter out_incFile;
		
		currentModelDataSet = ds;
		
		
		//outFolder = System.getProperty("user.dir")+"//"+outFolder;

		if(ds.incFileList.size()>0){
			out_incFile = Tools.openFile(outFolder, "include_file.csv");		
			//out_incFile.print(WriteCSV.incFile_header + "\n");
			incFile(ds.incFileList, out_incFile);	
			out_incFile.close();
		}
		if(ds.incFileList_global.size()>0){
			out_incFile = Tools.openFile(outFolder, "include_file_global.csv");		
			//out_incFile.print(WriteCSV.incFile_header + "\n");
			incFile(ds.incFileList_global, out_incFile);	
			out_incFile.close();
		}
		if(ds.exList.size()>0){
			out_ex = Tools.openFile(outFolder, "external.csv");		
			out_ex.print(WriteCSV.external_header + "\n");
			external(ds.exMap, ds.exList, out_ex);	
			out_ex.close();
		}
		if(ds.tsList.size()>0){
			out_svTs = Tools.openFile(outFolder, "timeseries.csv");	
			out_svTs.print(WriteCSV.timeseries_header + "\n");
			timeseries_model(ds.tsMap, ds.tsList, out_svTs);
			out_svTs.close();
		}
		if(ds.svList.size()>0){
			out_sv = Tools.openFile(outFolder, "svar.csv");	
			out_sv.print(WriteCSV.svar_header + "\n");
			svar(ds.svMap, ds.svList, out_sv);
			out_sv.close();
		}
		if(ds.svSet_unknown.size()>0){
			out_sv_unknown = Tools.openFile(outFolder, "svar_unknown.csv");	
			out_sv_unknown.print(WriteCSV.svar_header + "\n");
			svar(ds.svMap, new ArrayList<String>(ds.svSet_unknown), out_sv_unknown);
			out_sv_unknown.close();
		}
		if((ds.dvList.size()+ds.dvSlackSurplusList.size())>0){
			out_dv = Tools.openFile(outFolder, "dvar.csv");	
			out_dv.print(WriteCSV.dvar_header + "\n");
			ArrayList<String> allDvList = new ArrayList<String>(ds.dvList);
			allDvList.addAll(ds.dvSlackSurplusList);
			Map<String,Dvar> allDvMap = new HashMap<String, Dvar>(ds.dvMap);
			allDvMap.putAll(ds.dvSlackSurplusMap);
			dvar(allDvMap, allDvList, out_dv);
			out_dv.close();
		}
		if(ds.gList.size()>0){
			out_goal = Tools.openFile(outFolder, "constraint.csv");		
			out_goal.print(WriteCSV.goal_header + "\n");
			goal(ds.gMap, ds.gList, out_goal);	
			out_goal.close();
		}
		if(ds.exList.size()>0){
			out_ex = Tools.openFile(outFolder, "external.csv");		
			out_ex.print(WriteCSV.external_header + "\n");
			external(ds.exMap, ds.exList, out_ex);
			out_ex.close();
		}
		if((ds.wtList.size()+ds.wtSlackSurplusList.size())>0){
			out_wt = Tools.openFile(outFolder, "weight.csv");		
			out_wt.print(WriteCSV.weight_header + "\n");
			ArrayList<String> allWtList = new ArrayList<String>(ds.wtList);
			allWtList.addAll(ds.wtSlackSurplusList);
			Map<String,WeightElement> allWtMap = new HashMap<String, WeightElement>(ds.wtMap);
			allWtMap.putAll(ds.wtSlackSurplusMap);
			weight(allWtMap, allWtList, out_wt);	
			out_wt.close();
		}
		if(ds.asList.size()>0){
			out_alias = Tools.openFile(outFolder, "alias.csv");		
			out_alias.print(WriteCSV.alias_header + "\n");
			alias(ds.asMap, ds.asList, out_alias);	
			out_alias.close();
		}
		if(ds.asSet_unknown.size()>0){
			out_alias_unknown = Tools.openFile(outFolder, "alias_unknown.csv");	
			out_alias_unknown.print(WriteCSV.alias_header + "\n");
			alias_unknown(ds.asMap, ds.dvMap, new ArrayList<String>(ds.asSet_unknown), out_alias_unknown);
			out_alias_unknown.close();
		}



	};	

	  public static void timeseries_model(Map<String,Timeseries> sTsMap, ArrayList<String> list ,PrintWriter out) {
		    
		  List<String> keys = new ArrayList<String>(list);
			Collections.sort(keys,String.CASE_INSENSITIVE_ORDER);
			
		    for (String k: keys ){
		    	
		    	Timeseries s = sTsMap.get(k);
		    	
			    out.print(k); // for SVAR NAME
			    out.print(Param.csv_seperator+s.dssBPart); //for DSS B Part	
			    out.print(Param.csv_seperator+s.kind); //for KIND		    	
		    	out.print(Param.csv_seperator+s.units); //for UNITS
		    	out.print(Param.csv_seperator+s.convertToUnits); //for CONVERT
		    	out.print(",n"); //for OUTPUT

				out.print(Param.csv_seperator+s.fromWresl);
				out.print("\n");	
		    	
			}
	  };
	  
	  public static void svar(Map<String,Svar> sMap, ArrayList<String> list ,PrintWriter out) {
		    
		  List<String> keys = new ArrayList<String>(list);
			//Collections.sort(keys,String.CASE_INSENSITIVE_ORDER);
			
		    for (String k: keys ){
		    	
		    	//out.print(k);
		    	Svar s = sMap.get(k);
		    	
		    	for (int i=0; i<s.caseCondition.size(); i++)

		    	//	SvarProps p =sMap.get(k);
		    	{
			    	int caseOrder = i+1;
			    out.print(k); // for SVAR NAME
			    //out.print(Param.csv_seperator+s.dssBPart); //for DSS B Part	
			    //out.print(Param.csv_seperator+s.kind); //for KIND		    	
		    	//out.print(Param.csv_seperator+s.units); //for UNITS
		    	//out.print(Param.csv_seperator+s.convertToUnits); //for CONVERT
		    	//out.print(",n"); //for OUTPUT
			    out.print(Param.csv_seperator+s.timeArraySize); //for TIME ARRAY SIZE
		    	out.print(Param.csv_seperator+s.caseName.get(i)); //for CASE 
		    	out.print(Param.csv_seperator+caseOrder); //for CASE 
		    	out.print(Param.csv_seperator+s.caseCondition.get(i)); //for CONDITION
		    	out.print(Param.csv_seperator+s.caseExpression.get(i)); //for EXPRESSION
		    	
		    	out.print(Param.csv_seperator);
		    	
		    	if (s.dependants!=null){
		    		
		  		    List<String> dep = new ArrayList<String>(s.dependants);
					Collections.sort(dep,String.CASE_INSENSITIVE_ORDER);
					
			    	for (String d: dep){
			    		out.print(d+";"); //for dependants		    	
			    	}
		    	}
				
		    	out.print(Param.csv_seperator+s.fromWresl);
		    	
		    	out.print(Param.csv_seperator);
		    	
		    	if (s.neededVarInCycleSet!=null){
		    		
		  		    List<String> varInCycle = new ArrayList<String>(s.neededVarInCycleSet);
					Collections.sort(varInCycle,String.CASE_INSENSITIVE_ORDER);
		    		
			    	for (String d: varInCycle){
			    		out.print(d+";"); //for dependantName[cycleName]		    	
			    	}
		    	}
		    	
		    	out.print(Param.csv_seperator);
		    	
		    	boolean usedByLaterCycle = false;
		    	if (currentModelDataSet.svarUsedByLaterCycle.contains(k)) usedByLaterCycle = true;
		    	
		    	out.print( usedByLaterCycle);
		    	
		    	out.print(Param.csv_seperator);
		    	
				out.print("\n");	
		    	}
			}
	  };	  

	  public static void sequence(ArrayList<String> modelList, ArrayList<String> modelConditionList ,PrintWriter out) {
		    
		    for (int i = 0; i< modelList.size(); i++ ){
		    			    	
		    	out.print(modelList.get(i));
		    	out.print(Param.csv_seperator+modelConditionList.get(i));
				out.print("\n");	
		    	
			}
	  };
	
	  public static void sequence(Map<Integer,Sequence> seqMap, ArrayList<Integer> list ,PrintWriter out) {
		    
			List<Integer> keys = list;
			Collections.sort(keys);
			
		    for (Integer k: keys ){
		    			    	
		    	out.print(seqMap.get(k).modelName);
		    	out.print(Param.csv_seperator+seqMap.get(k).condition+Param.csv_seperator+seqMap.get(k).timeStep);
				out.print("\n");	
		    	
			}
	  };	
	
	  
	  public static void timeseries_wholeStudy(Map<String,Timeseries> tsMap, PrintWriter out) {
		    
			Set<String> kSet = tsMap.keySet();
			
			List<String> keys = new ArrayList<String>(kSet);
			
			Collections.sort(keys);
			
		    for (String k: keys ){
		    	
		    	Timeseries s = tsMap.get(k);
		    	
			    out.print(k); // for SVAR NAME
			    out.print(Param.csv_seperator+s.dssBPart); //for DSS B Part	
			    out.print(Param.csv_seperator+s.kind); //for KIND		    	
		    	out.print(Param.csv_seperator+s.units); //for UNITS
		    	out.print(Param.csv_seperator+s.convertToUnits); //for CONVERT
		    	out.print(",n"); //for OUTPUT

				out.print(Param.csv_seperator+s.fromWresl);
				out.print("\n");	
		    	
			}
	  };

	  public static void timeseriesTimeStep(StudyDataSet sd, PrintWriter out) {
		    
		    Map<String,Timeseries> tsMap = sd.getTimeseriesMap();
		  
			Set<String> kSet = tsMap.keySet();
			
			List<String> keys = new ArrayList<String>(kSet);
			
			Collections.sort(keys);
			
		    for (String k: keys ){
		    	
		    	Timeseries s = tsMap.get(k);
		    	
			    out.print(k); // for TIMESERIES NAME
			    out.print(Param.csv_seperator+s.dssBPart); //for DSS B Part	
			    out.print(Param.csv_seperator); 		    	

			    for (String step:sd.getTimeseriesTimeStepMap().get(k)){
			    	
			    	out.print(step+";");
			    	
			    }

				out.print("\n");	
		    	
			}
	  };
	  
	  public static void incFile(ArrayList<String> list ,PrintWriter out) {
		    
		  List<String> keys = new ArrayList<String>(list);
		  Collections.sort(keys,String.CASE_INSENSITIVE_ORDER);
			
		    for (String k: keys ){
		    	
		    	//out.print(k);
		    	

			    out.print(k); // 

				out.print("\n");	
		    	
			}
	  };
	  
	  public static void external(Map<String,External> exMap, ArrayList<String> list ,PrintWriter out) {
		    
		    List<String> keys = new ArrayList<String>(list);
			Collections.sort(keys,String.CASE_INSENSITIVE_ORDER);
			
		    for (String k: keys ){
		    	
		    	//out.print(k);
		    	External s = exMap.get(k);
		    	

			    out.print(k); // 
		    	out.print(Param.csv_seperator+s.type); 	    	

				out.print(Param.csv_seperator+s.fromWresl);
				out.print("\n");	
		    	
			}
	  };	  

	  
	  public static void weight(Map<String,WeightElement> wtMap, ArrayList<String> list ,PrintWriter out) {
		    
			List<String> keys = new ArrayList<String>(list);
			Collections.sort(keys,String.CASE_INSENSITIVE_ORDER);
			
		    for (String k: keys ){
		    	
			    
			    if (! wtMap.containsKey(k)) System.out.println("##### not exist in wtMap:"+k);
			    
			    
		    	//out.print(k);
		    	WeightElement w = wtMap.get(k);


			    out.print(k); // for DVAR NAME
			    
			    out.print(Param.csv_seperator+w.timeArraySize); //for TIME ARRAY SIZE
			    
			    out.print(Param.csv_seperator+w.condition); 
			    
			    out.print(Param.csv_seperator+w.weight); 

				out.print("\n");	

			}
	  };	  

	public static void dvar(Map<String,Dvar> dMap, ArrayList<String> list ,PrintWriter out) {
		    
			List<String> keys = new ArrayList<String>(list);
			Collections.sort(keys,String.CASE_INSENSITIVE_ORDER);
			
		    for (String k: keys ){
		    	
		    	Dvar d = dMap.get(k);		    	

			    out.print(k); // for DVAR NAME
			    out.print(Param.csv_seperator+d.timeArraySize); //for TIME ARRAY SIZE
			    out.print(Param.csv_seperator+d.condition); // for DVAR CONDITION
		    	out.print(Param.csv_seperator+d.lowerBound); //for UNITS
		    	out.print(Param.csv_seperator+d.upperBound); //for UNITS
		    	out.print(Param.csv_seperator+d.integer); //for KIND
		    	out.print(Param.csv_seperator+d.units); //for UNITS
		    	out.print(Param.csv_seperator+d.kind); //for KIND

				out.print(Param.csv_seperator+d.fromWresl);
				
		    	out.print(Param.csv_seperator);
		    	
		    	boolean usedByLaterCycle = false;
		    	if (currentModelDataSet.dvarUsedByLaterCycle.contains(k)) usedByLaterCycle = true;
		    	
		    	out.print( usedByLaterCycle);
		    	
		    	out.print(Param.csv_seperator);
		    	
				out.print("\n");	
		    	}
	  };

	public static void alias_unknown(Map<String,Alias> asMap, Map<String,Dvar> dvMap, ArrayList<String> list ,PrintWriter out) {
		    
			List<String> keys = new ArrayList<String>(list);
			Collections.sort(keys,String.CASE_INSENSITIVE_ORDER);
			
		for (String k : keys) {

			if (asMap.keySet().contains(k)) {

				Alias a = asMap.get(k);

				out.print(k); // for DVAR NAME
				out.print(Param.csv_seperator + a.timeArraySize); //for TIME ARRAY SIZE
				out.print(Param.csv_seperator + a.kind); // for KIND
				out.print(Param.csv_seperator + a.units); // for UNITS
				out.print(Param.csv_seperator + a.expression); // for expression

				out.print(Param.csv_seperator);

				if (a.dependants != null) {
					for (String d : a.dependants) {
						out.print(d + ";"); // for dependants
					}
				}

				out.print(Param.csv_seperator + a.fromWresl);
				out.print("\n");
			}
			else { // converted to dv

				Dvar a = dvMap.get(k);

				out.print(k); // for DVAR NAME

				out.print(Param.csv_seperator + a.kind); // for KIND
				out.print(Param.csv_seperator + a.units); // for UNITS
				out.print(Param.csv_seperator + a.expression); // for expression

				out.print(Param.csv_seperator);

				if (a.dependants != null) {
					for (String d : a.dependants) {
						out.print(d + ";"); // for dependants
					}
				}

				out.print(Param.csv_seperator + a.fromWresl);
				out.print("\n");				
				
			}
		}
	  };	  
	  
	public static void alias(Map<String,Alias> asMap, ArrayList<String> list ,PrintWriter out) {
		    
			List<String> keys = new ArrayList<String>(list);
			Collections.sort(keys,String.CASE_INSENSITIVE_ORDER);
			
		    for (String k: keys ){
		    	
		    	Alias a = asMap.get(k);		    	

			    out.print(k); // for DVAR NAME
			    out.print(Param.csv_seperator+a.timeArraySize); //for TIME ARRAY SIZE
		    	out.print(Param.csv_seperator+a.kind); //for KIND		    	
		    	out.print(Param.csv_seperator+a.units); //for UNITS
		    	out.print(Param.csv_seperator+a.expression); //for expression

		    	out.print(Param.csv_seperator);
		    	
		    	if (a.dependants!=null){
		    		
		  		    List<String> dep = new ArrayList<String>(a.dependants);
					Collections.sort(dep,String.CASE_INSENSITIVE_ORDER);
					
			    	for (String d: dep){
			    		out.print(d+";"); //for dependants		    	
			    	}
		    	}
		    	
				out.print(Param.csv_seperator+a.fromWresl);
				
		    	out.print(Param.csv_seperator);
		    	
		    	if (a.neededVarInCycleSet!=null){
		    		
		  		    List<String> varInCycle = new ArrayList<String>(a.neededVarInCycleSet);
					Collections.sort(varInCycle,String.CASE_INSENSITIVE_ORDER);
					
			    	for (String d: varInCycle){
			    		out.print(d+";"); //for dependantName[cycleName]		    	
			    	}
		    	}
		    	
		    	out.print(Param.csv_seperator);
		    	
		    	boolean usedByLaterCycle = false;
		    	if (currentModelDataSet.aliasUsedByLaterCycle.contains(k)) usedByLaterCycle = true;
		    	
		    	out.print( usedByLaterCycle);
				
				out.print("\n");	
		    	}
	  };		  
	  
	  public static void goal(Map<String,Goal> gMap, ArrayList<String> list ,PrintWriter out) {
		    
		    List<String> keys = new ArrayList<String>(list);
			Collections.sort(keys,String.CASE_INSENSITIVE_ORDER);
			
		    for (String k: keys ){
		    	
		    	//out.print(k);
		    	Goal g = gMap.get(k);
		    	
		    	for (int i=0; i<g.caseCondition.size(); i++)

		    	//	SvarProps p =sMap.get(k);
		    	{
			    	int caseOrder = i+1;
			    out.print(k); // for GOAL NAME
			    //out.print(Parameters.csv_seperator+g.lhs); // for LHS
		    	//out.print(Parameters.csv_seperator+p.scope);  // for SCOPE
		    	//out.print(",Y"); //for INCLUDE
		    	//out.print(Parameters.csv_seperator+p.format); //for FORMAT
		    	//out.print(Parameters.csv_seperator+s.kind); //for KIND		    	
		    	//out.print(Parameters.csv_seperator+s.units); //for UNITS
		    	//out.print(",Y"); //for OUTPUT
			    out.print(Param.csv_seperator+g.timeArraySize); //for TIME ARRAY SIZE
		    	out.print(Param.csv_seperator+g.caseName.get(i)); //for CASE 
		    	out.print(Param.csv_seperator+caseOrder); //for ORDER 
		    	out.print(Param.csv_seperator+g.caseCondition.get(i)); //for CONDITION
		    	
		    	out.print(Param.csv_seperator + g.caseExpression.get(i) ); //for EXPRESSION

		    	out.print(Param.csv_seperator);
		    	
		    	if (g.expressionDependants!=null){
		    		
		  		    List<String> dep = new ArrayList<String>(g.expressionDependants);
					Collections.sort(dep,String.CASE_INSENSITIVE_ORDER);
					
			    	for (String d: dep){
			    		out.print(d+";"); //for dependants		    	
			    	}
		    	}
		    	
//		    	out.print(Param.csv_seperator+g.case_lhs_gt_rhs.get(i)); //
//		    	out.print(Param.csv_seperator+g.case_lhs_lt_rhs.get(i)); //for EXPRESSION
		    	

				out.print(Param.csv_seperator+g.fromWresl);
		    	out.print(Param.csv_seperator);
		    	
		    	if (g.neededVarInCycleSet!=null){
		    		
		  		    List<String> varInCycle = new ArrayList<String>(g.neededVarInCycleSet);
					Collections.sort(varInCycle,String.CASE_INSENSITIVE_ORDER);
					
			    	for (String d: varInCycle){
			    		out.print(d+";"); //for dependantName[cycleName]		    	
			    	}
		    	}
				out.print("\n");	
		    	}
			}
	  };	  
	  
		public static void mapStringListMerged(Object obj, PrintWriter out) {
			
		    @SuppressWarnings("unchecked")
		    List<Map<String, List<String>>> listMapStringList = (List<Map<String, List<String>>>) obj;
		    Map<String, List<String>> mapAll = new HashMap<String, List<String>>();		    
		    
			ArrayList<String> keys = new ArrayList<String>(); 
			
			for (Map<String, List<String>> m : listMapStringList){
				
				mapAll.putAll(m);
			}

			Collections.sort(keys,String.CASE_INSENSITIVE_ORDER);
		    
		    for (String k: keys ){
		    	out.print(k);		
			
				for(String i : mapAll.get(k)){
					out.print(Param.csv_seperator+i);}
				out.print("\n");	
			}
	  };	  
	  

}
