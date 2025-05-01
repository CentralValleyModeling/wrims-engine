package wrimsv2.wreslparser.elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.antlr.runtime.RecognitionException;

import wrimsv2.commondata.wresldata.Alias;
import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.Goal;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.Param;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.commondata.wresldata.Svar;
import wrimsv2.commondata.wresldata.Timeseries;
import wrimsv2.components.ControlData;
import wrimsv2.components.IntDouble;
import wrimsv2.wreslparser.grammar.WreslTreeWalker;
import wrimsv2.wreslplus.elements.VarCycleIndex;

public class StudyParser{
	
  public static int total_errors = 0;
  public static ArrayList<String> error_summary = new ArrayList<String>();
  public static int total_warnings = 0;
  private static Map<String, Map<String, String>> cycleVarTypeMap;
  private static Set<String> allValidCycleNames;
  private static Map<String, String> cycleNameConditionMap;
  private static Map<String, String> cycleNameTimeStepMap;
  private static int n_buffer = 3;


  public static void reset(){  
	  total_errors = 0;
	  error_summary = new ArrayList<String>();
	  total_warnings = 0;
	  cycleVarTypeMap = null;
	  allValidCycleNames = null; 
	  cycleNameConditionMap = null;
	  cycleNameTimeStepMap = null;
  }
  
  public static StudyDataSet writeWreslData(StudyConfig sc, TempData td){
    StudyDataSet studyDataSet = new StudyDataSet();

    studyDataSet.setModelList(sc.modelList);
    studyDataSet.setModelConditionList(sc.modelConditionList);
    studyDataSet.setModelTimeStepList(sc.modelTimeStepList);

    Map<String, ModelDataSet> modelDataSetMap = new HashMap<String, ModelDataSet>();
    Map<String, Timeseries> timeseriesMap = new HashMap<String, Timeseries>();
    Map<String, ArrayList<String>> timeseriesTimeStepMap = new HashMap<String, ArrayList<String>>();

    for (String modelName : studyDataSet.getModelList())
    {
      SimulationDataSet ds = (SimulationDataSet)td.model_dataset_map.get(modelName);
      ModelDataSet thisModelDataSet = new ModelDataSet();

      thisModelDataSet.dvList = ds.dvList;
      Collections.sort(thisModelDataSet.dvList,String.CASE_INSENSITIVE_ORDER);
      thisModelDataSet.dvList_global = ds.dvList_global;
      thisModelDataSet.dvList_local = ds.dvList_local;
      
      for (String d: ds.dvList){
    	  if (ds.dvMap.get(d).condition.equalsIgnoreCase(Param.conditional)){
    		  thisModelDataSet.dvSlackSurplusList.add(d);
    		  thisModelDataSet.dvSlackSurplusMap.put(d, ds.dvMap.get(d));
    		  ds.dvMap.remove(d);
    	  }	  
      }
      
      thisModelDataSet.dvList.removeAll(thisModelDataSet.dvSlackSurplusList);
      thisModelDataSet.dvList_global.removeAll(thisModelDataSet.dvSlackSurplusList);
      thisModelDataSet.dvList_local.removeAll(thisModelDataSet.dvSlackSurplusList);
      thisModelDataSet.dvMap = ds.dvMap;

      thisModelDataSet.tsList = ds.tsList;
      thisModelDataSet.tsList_global = ds.tsList_global;
      thisModelDataSet.tsList_local = ds.tsList_local;
      thisModelDataSet.tsMap = ds.tsMap;

      thisModelDataSet.svSet_unknown = ds.svSet_unknown;
      thisModelDataSet.svList = ds.svList;
      thisModelDataSet.svList_global = ds.svList_global;
      thisModelDataSet.svList_local = ds.svList_local;
      thisModelDataSet.svMap = ds.svMap;

      thisModelDataSet.gList = ds.gList;
      Collections.sort(thisModelDataSet.gList,String.CASE_INSENSITIVE_ORDER);
      thisModelDataSet.gList_global = ds.gList_global;
      thisModelDataSet.gList_local = ds.gList_local;
      thisModelDataSet.gMap = ds.gMap;

      thisModelDataSet.asSet_unknown = ds.asSet_unknown;
      thisModelDataSet.asList = ds.asList;
      thisModelDataSet.asList_global = ds.asList_global;
      thisModelDataSet.asList_local = ds.asList_local;
      thisModelDataSet.asMap = ds.asMap;

      thisModelDataSet.exList = ds.exList;
      thisModelDataSet.exList_global = ds.exList_global;
      thisModelDataSet.exList_local = ds.exList_local;
      thisModelDataSet.exMap = ds.exMap;


      thisModelDataSet.wtList = ds.wtList;
      Collections.sort(thisModelDataSet.wtList,String.CASE_INSENSITIVE_ORDER);
      
      for (String w: ds.wtList){
    	  if (ds.wtMap.get(w).condition.equalsIgnoreCase(Param.conditional)){
    		  thisModelDataSet.wtSlackSurplusList.add(w); 
    		  thisModelDataSet.wtSlackSurplusMap.put(w, ds.wtMap.get(w));
    		  ds.wtMap.remove(w);
    	  }	  
      }
      
      thisModelDataSet.wtList.removeAll(thisModelDataSet.wtSlackSurplusList);
      //System.out.println("thisModelDataSet.wtList : "+thisModelDataSet.wtList);
      thisModelDataSet.wtMap = ds.wtMap;
      

      

      thisModelDataSet.incFileList = ds.incFileList;
      thisModelDataSet.incFileList_global = ds.incFileList_global;
      thisModelDataSet.incFileList_local = ds.incFileList_local;
      
      modelDataSetMap.put(modelName, thisModelDataSet);
      timeseriesMap.putAll(ds.tsMap);
      int modelIndex=studyDataSet.getModelList().indexOf(modelName);
      String timeStep=sc.modelTimeStepList.get(modelIndex);
      String definedTimeStep;
      if (timeStep.equals(Param.undefined)){
    	  definedTimeStep=ControlData.defaultTimeStep;
      }else{
    	  definedTimeStep=timeStep;
      }
      for (String timeseriesName:ds.tsMap.keySet()){
    	  if (timeseriesTimeStepMap.containsKey(timeseriesName)){
    		  ArrayList<String> timeStepList=timeseriesTimeStepMap.get(timeseriesName);
    		  if (!timeStepList.contains(definedTimeStep)){
    			  timeStepList.add(definedTimeStep);
    		  }
    	  }else{
    		  timeseriesTimeStepMap.put(timeseriesName, new ArrayList<String>(Arrays.asList(definedTimeStep)));
    	  }
      }
    }

    studyDataSet.setModelDataSetMap(modelDataSetMap);
    studyDataSet.setTimeseriesMap(timeseriesMap);
    studyDataSet.setTimeseriesTimeStepMap(timeseriesTimeStepMap);
    studyDataSet.setVarCycleIndexList(VarCycleIndex.varCycleIndexList);
    
    return studyDataSet;
  }
  
  public static StudyConfig processMainFileIntoStudyConfig(String relativeMainFilePath) throws RecognitionException, IOException {
	    /// reset total errors
	    total_errors = 0;
    return processMainFileIntoStudyConfig(relativeMainFilePath, false);
  }

  public static StudyConfig processMainFileIntoStudyConfig(String relativeMainFilePath, boolean showTree) throws RecognitionException, IOException
  {
    File absMainFile = new File(relativeMainFilePath).getAbsoluteFile();
    String absMainFilePath = absMainFile.getCanonicalPath().toLowerCase();

    LogUtils.importantMsg("Parsing study main file: " + absMainFilePath);

    WreslTreeWalker walker = FileParser.parseMainFile(absMainFilePath, showTree);

    StudyConfig sc = new StudyConfig();

    sc.sequenceMap = walker.thisFileDataSet.seqMap;
    
	/// Sort sequence order
	
	for ( Integer i : sc.sequenceMap.keySet()){ sc.sequenceOrder.add(i); }
	Collections.sort(sc.sequenceOrder);

	cycleNameConditionMap = new HashMap<String, String>();
	cycleNameTimeStepMap = new HashMap<String, String>();
	
    for (Integer i : sc.sequenceOrder) {
      sc.sequenceList.add(sc.sequenceMap.get(i).sequenceName);
      sc.modelList.add(sc.sequenceMap.get(i).modelName);

      sc.modelConditionList.add(sc.sequenceMap.get(i).condition);
      sc.modelTimeStepList.add(sc.sequenceMap.get(i).timeStep);
      cycleNameConditionMap.put(sc.sequenceMap.get(i).modelName, sc.sequenceMap.get(i).condition );
      cycleNameTimeStepMap.put(sc.sequenceMap.get(i).modelName, sc.sequenceMap.get(i).timeStep );
    }

    sc.absMainFilePath = absMainFilePath;
    sc.modelDataMap = walker.modelDataMap;

    for (Integer i : sc.sequenceOrder) {
      LogUtils.importantMsg("Sequence: " + i + " : " + ((Sequence)sc.sequenceMap.get(i)).sequenceName + "   Model: " + ((Sequence)sc.sequenceMap.get(i)).modelName);
    }

    return sc;
  }

  public static Map<String, SimulationDataSet> parseModels(StudyConfig sc, TempData td) throws RecognitionException, IOException
  {
    return parseModels(sc, td, false, false);
  }

  public static Map<String, SimulationDataSet> parseModels(StudyConfig sc, TempData td, boolean rewrite_list_based_on_dependency, boolean send_all_alias_to_dvar) throws RecognitionException, IOException
  {
    Map<String, SimulationDataSet> model_dataset_map = new HashMap<String, SimulationDataSet>();

    for (Integer iSequence : sc.sequenceOrder)
    {
      String modelName = sc.sequenceMap.get(iSequence).modelName;

      LogUtils.importantMsg("Processing sequence: " + iSequence + ", model: " + modelName);

      SimulationDataSet adhoc = sc.modelDataMap.get(modelName);

      Map<String, SimulationDataSet> fileDataMap_new = getNewDataSet(adhoc.incFileSet, td.fileDataMap_wholeStudy.keySet());

      td.fileDataMap_wholeStudy.putAll(fileDataMap_new);

      td.t1Map_wholeStudy.putAll(Tools.getType1Map(fileDataMap_new));
      td.fileScopeMap_wholeStudy.putAll(Tools.getFileScopeMap(fileDataMap_new));

      Map<String, SimulationDataSet> fileDataMap_thisModel = copyDataSetToThisModel(adhoc.incFileSet, td.fileDataMap_wholeStudy);

      ArrayList<String> ordered_list_for_all_vars = getOrderedList(fileDataMap_thisModel, adhoc);
      

      Set<String> duplicates = Tools.removeDuplicates(ordered_list_for_all_vars);
      

      if (duplicates.size() > 0) {
        LogUtils.errMsg("Variables redefined in model " + modelName + ": " + duplicates);
      }

      
      SimulationDataSet model_dataset = correctScopeAndPrioritize(modelName, 
        adhoc, 
        sc.absMainFilePath, 
        fileDataMap_thisModel, 
        td.fileDataMap_wholeStudy, 
        td.t1Map_wholeStudy, 
        td.fileScopeMap_wholeStudy);

      model_dataset.svSet = Tools.restoreOrder(model_dataset.svList, ordered_list_for_all_vars, model_dataset.svSet);
      model_dataset.asSet = Tools.restoreOrder(model_dataset.asList, ordered_list_for_all_vars, model_dataset.asSet);

      td.t1Map_wholeStudy.put(sc.absMainFilePath, adhoc.incFileSet);
      @SuppressWarnings("unused")
	Map<String, Set<String>> t1ReverseMap_wholeStudy = Tools.getReverseMap(td.t1Map_wholeStudy);

      
      ///////// add previous globals
      //LogUtils.importantMsg("Adding previous global vars into model: "+modelName);

      /// check for redefined weight // TODO: what to do?
      Set<String> redefined_globals_wt = new LinkedHashSet<String>(td.cumulative_global_complete.wtSet);
      redefined_globals_wt.retainAll(model_dataset.wtSet_global);
      if (redefined_globals_wt.size()>0) LogUtils.errMsg("Global weights redefined: "+ redefined_globals_wt); 
      
      /// check for redefined file
      Set<String> redefined_global_file = new LinkedHashSet<String>(td.cumulative_global_complete.incFileSet);
      redefined_global_file.retainAll(model_dataset.incFileSet_global);
      if (redefined_global_file.size()>0) LogUtils.errMsg("Global include files redefined: "+ redefined_global_file); 
      
      /// check for redefined globals       
      Set<String> redefined_globals = new LinkedHashSet<String>(td.cumulative_global_complete.getAllVarsSet_except_file_and_weight());
      Set<String> redefined_globals_as_locals = new LinkedHashSet<String>(td.cumulative_global_complete.getAllVarsSet_except_file_and_weight());
      
      
      redefined_globals.retainAll(model_dataset.getAllGlobalVarsSet_except_file_and_weight());
      
      redefined_globals_as_locals.retainAll(model_dataset.getAllLocalVarsSet_except_file_and_weight());
      
      if (redefined_globals.size()>0) LogUtils.errMsg("Global variables redefined: "+ redefined_globals);
      if (redefined_globals_as_locals.size()>0) LogUtils.warningMsg("Global variables redefined as local: "+ redefined_globals_as_locals);
      
     
      
      /// add previous globals
      
     
     
//      // option 1 : remove redefined
//      SimulationDataSet cumulative_global_remove_redefined = new SimulationDataSet(td.cumulative_global_complete);
//      cumulative_global_remove_redefined.remove_set(redefined_globals);
//      cumulative_global_remove_redefined.remove_set(redefined_globals_as_locals); 
      
      
      // option 2 : replace redefined
      SimulationDataSet cumulative_global_replace_redefined = new SimulationDataSet(td.cumulative_global_complete);
      

	 // comment out. redefining globals is not allowed	
     // cumulative_global_replace_redefined.replaceGlobalWithDuplicateGlobal(redefined_globals,model_dataset );


      
    try {
		cumulative_global_replace_redefined.replaceGlobalWithDuplicateLocal(redefined_globals_as_locals,model_dataset);
	}
	catch (TypeRedefinedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 
   
      
      // put all previous global vars into current cycle 
      model_dataset.overwrite_set(cumulative_global_replace_redefined);   
      
      //LogUtils.importantMsg("Finish adding previous global vars into model: "+modelName);
      /// finish adding globals
  
     
      model_dataset.lousyConvert();
		      
      
      // check if dependency is unknown TODO: sorted list will not have the same sequence as user's define in wresl 
      sortDependency(model_dataset, rewrite_list_based_on_dependency);  

      //// check if vars are used before being defined
      Set<String> svar_alias_extern_set = new HashSet<String>();
      svar_alias_extern_set.addAll(model_dataset.svSet);
      svar_alias_extern_set.addAll(model_dataset.asSet);
      svar_alias_extern_set.addAll(model_dataset.exSet);
      
      ArrayList<String> ordered_list_svar_alias_extern = new ArrayList<String>(ordered_list_for_all_vars);
      ordered_list_svar_alias_extern.retainAll(svar_alias_extern_set);
      
      Set<String> previous_global_svar_alias_extern_timeseries = new LinkedHashSet<String>(td.cumulative_global_complete.getSvarAliasExternTimeseries());

      checkUsedBeforeDefined(new SimulationDataSet(model_dataset), ordered_list_svar_alias_extern, previous_global_svar_alias_extern_timeseries);
      

      /// check if goal dependents are defined 
      // 1. get union of previous global vars and current model vars
      Set<String> allVars = new HashSet<String>();
      allVars.addAll(td.cumulative_global_complete.dvSet); allVars.addAll(model_dataset.dvSet); 
      allVars.addAll(td.cumulative_global_complete.svSet); allVars.addAll(model_dataset.svSet);
      allVars.addAll(td.cumulative_global_complete.tsSet); allVars.addAll(model_dataset.tsSet);
      allVars.addAll(td.cumulative_global_complete.exSet); allVars.addAll(model_dataset.exSet);    
      allVars.addAll(td.cumulative_global_complete.asSet); allVars.addAll(model_dataset.asSet); 
      
      // 2. find undefined
      for (String key: model_dataset.gSet){
    	  
          Set<String> goal_dependents_undefined = new HashSet<String>();
          goal_dependents_undefined.addAll(model_dataset.gMap.get(key).expressionDependants);
          goal_dependents_undefined.removeAll(allVars);
          goal_dependents_undefined.removeAll(Param.reservedSet);
          
          if (goal_dependents_undefined.size()>0) {
        	  LogUtils.errMsg(model_dataset.gMap.get(key).fromWresl +" variable names used before definition: "+goal_dependents_undefined+" in Goal named ["+key+"]");
          }         
      }

      /// check if weight var is defined in dvar set
      // 1. get union of previous global dvar and current model dvar
      Set<String> allDvars = new HashSet<String>();
      allDvars.addAll(td.cumulative_global_complete.dvSet);
      allDvars.addAll(model_dataset.dvSet);
      // 2. find undefined
      Set<String> weightElement_undefined = new HashSet<String>();
      weightElement_undefined.addAll(model_dataset.wtSet);
      
      weightElement_undefined.removeAll(allDvars);
      if (weightElement_undefined.size()>0) LogUtils.errMsg("Weight var not found in dvar: "+ weightElement_undefined);
      
      
      
      
      model_dataset_map.put(modelName, model_dataset);
		
      td.cumulative_global_adhocs.overwrittenWith_set(sc.modelDataMap.get(modelName).getGlobalVars_set());
      td.cumulative_global_complete.overwrittenWith_set(model_dataset.getGlobalVars_set());
      
      td.cumulative_global_adhocs.lousyConvert();
      td.cumulative_global_complete.lousyConvert();
    }
    
    ///  check if alias is used in goal's constraint expression.
    //   if yes, then move that alias to dvar and add additional goal
    if(send_all_alias_to_dvar) {
    	LogUtils.importantMsg("Converting all aliases into dvars and constraints....");
    } else {   
    	LogUtils.importantMsg("Converting some aliases into dvars and constraints....");
    }
    
    for (String key : model_dataset_map.keySet()){
    	SimulationDataSet ds = model_dataset_map.get(key);
    	
    	Set<String> newGoals = checkAliasInGoalExpression(ds, new HashSet<String>(ds.gSet));
    	
    	// check for alias embedded in another alias
    	while (newGoals.size()>0){
    		newGoals = checkAliasInGoalExpression(ds, newGoals);
    	}

        if(send_all_alias_to_dvar) {
        	convertRestAliasInGoalExpression(ds);
        }
        
        
    	/// check if var size > 100
    	Set<String> allVar = new HashSet<String>();
    	allVar.addAll(ds.dvSet);
    	allVar.addAll(ds.asSet);
    	allVar.addAll(ds.svSet);
    	allVar.addAll(ds.gSet);
    	allVar.addAll(ds.exSet);
    	
    	for (String e: allVar) { if (e.length()>99)  LogUtils.errMsg("Variable name is longer than 99 chars: "+ e);};

    }

    LogUtils.importantMsg("Finished converting aliases.");
    LogUtils.criticalMsg("============================================");    
    LogUtils.criticalMsg("Wresl Parsing complete. ");
    if (total_warnings>0) LogUtils.criticalMsg("Total warnings in the study: "+ total_warnings);
    if (total_errors>0) LogUtils.criticalMsg("Total errors in the study: "+ total_errors);
    LogUtils.criticalMsg("============================================");  
    
    return model_dataset_map;
  }

  
	public static void analyzeVarNeededFromCycles(StudyConfig sc, StudyDataSet sd) {
		
		Map<String, ModelDataSet> mdsm = sd.getModelDataSetMap();
		cycleVarTypeMap = new HashMap<String, Map<String,String>>();
		allValidCycleNames = mdsm.keySet();
		String SvarClassName  = new Svar().getClass().getName();
		String AliasClassName = new Alias().getClass().getName();
		String DvarClassName = new Dvar().getClass().getName();
		
		// create map for valid var name and var type
		
		for (String key: mdsm.keySet()){
			
			ModelDataSet ds = mdsm.get(key);
		
	    	/// create map for var and varType
			Map<String,String> varTypeMap = new HashMap<String, String>();
				
	    	for (String v: ds.svList){
	    		varTypeMap.put(v, SvarClassName);
	    	}    	
	    	for (String v: ds.asList){
	    		varTypeMap.put(v, AliasClassName);
	    	}
	    	for (String v: ds.dvList){
	    		varTypeMap.put(v, DvarClassName);
	    	} 
	    	cycleVarTypeMap.put(key, varTypeMap);	
		}
		
		
		for (Integer iSequence : sc.sequenceOrder) {

			String cycleKey = sc.sequenceMap.get(iSequence).modelName;

			ModelDataSet ds = mdsm.get(cycleKey);

			// / 1. check needVarFromEarlierCycle in goal, svar and alias;
			// / 2. create space in td.varCycleValueMap
			// / 3. set usedInLaterCycle for dvar, svar and alias;

			for (String varName : ds.gList) {
				
				Goal someVar = ds.gMap.get(varName);
				
				boolean needVarFromEarlierCycle = someVar.needVarFromEarlierCycle;
				Set<String> neededVarInCycle = someVar.neededVarInCycleSet;
				String fromWresl = someVar.fromWresl;

				if (needVarFromEarlierCycle) {
					analyzeVarNeededFromCycle_subfunction(varName, neededVarInCycle, fromWresl, sd);
			
				}
			}			
			
			for (String varName : ds.svList) {
				
				Svar someVar = ds.svMap.get(varName);
				
				boolean needVarFromEarlierCycle = someVar.needVarFromEarlierCycle;
				Set<String> neededVarInCycle = someVar.neededVarInCycleSet;
				String fromWresl = someVar.fromWresl;

				if (needVarFromEarlierCycle) {
					analyzeVarNeededFromCycle_subfunction(varName, neededVarInCycle, fromWresl, sd);
			
				}
			}
			
			for (String varName : ds.asList) {
				
				Alias someVar = ds.asMap.get(varName);
				
				boolean needVarFromEarlierCycle = someVar.needVarFromEarlierCycle;
				Set<String> neededVarInCycle = someVar.neededVarInCycleSet;
				String fromWresl = someVar.fromWresl;

				if (needVarFromEarlierCycle) {
					analyzeVarNeededFromCycle_subfunction(varName, neededVarInCycle, fromWresl, sd);
			
				}
			}
		}
		
		
		for (String cycleName : allValidCycleNames ){
			
			ModelDataSet md = mdsm.get(cycleName);
			
			md.svarUsedByLaterCycle = new HashSet<String>(md.varUsedByLaterCycle);
			md.svarUsedByLaterCycle.retainAll(md.svList);

			md.dvarUsedByLaterCycle = new HashSet<String>(md.varUsedByLaterCycle);
			md.dvarUsedByLaterCycle.retainAll(md.dvList);
			
			md.aliasUsedByLaterCycle = new HashSet<String>(md.varUsedByLaterCycle);
			md.aliasUsedByLaterCycle.retainAll(md.asList);	
			
		}
		
		
	    LogUtils.importantMsg("==================================================");    
	    LogUtils.importantMsg("VariableName[CycleName] checking complete. ");
	    LogUtils.importantMsg("Total errors in the study: "+ total_errors);
	    LogUtils.importantMsg("==================================================");  

	}
	
	
	private static void analyzeVarNeededFromCycle_subfunction(String varName, Set<String> neededVarInCycle, String fromWresl, StudyDataSet sd) {

		Map<String, Set<String>> neededCycleVarMap = Tools.getCycleVarMap(neededVarInCycle);

		Map<String, ModelDataSet> mdsm = sd.getModelDataSetMap();
		
		for (String neededCycle : neededCycleVarMap.keySet()) {

			Set<String> neededVarSet = neededCycleVarMap.get(neededCycle);

			if (!allValidCycleNames.contains(neededCycle)) {

				for (String s : neededVarSet) {
					LogUtils.errMsg("In file: " + fromWresl + "\n" + "Variable [" + varName + "] has an item with undefined cycle: "+ s + "[" + neededCycle + "]");
				}
				continue;
				
			} else if ( cycleNameConditionMap.get(neededCycle).length()>0 && !cycleNameConditionMap.get(neededCycle).equalsIgnoreCase("always")){
				
				for (String s : neededVarSet) {
					//LogUtils.warningMsg("In file: " + fromWresl + "\n" + "Variable [" + varName + "] has an item " + s + "[" + neededCycle + "]"+" depending on cycle condition: "+ cycleNameConditionMap.get(neededCycle));
					//LogUtils.warningMsg("Variable [" + varName + "] has an item " + s + "[" + neededCycle + "]"+" depending on cycle condition: "+ cycleNameConditionMap.get(neededCycle));
				}				
			}

			for (String neededVar : neededVarSet) {

				// / check if exist
				Set<String> validVarNames = cycleVarTypeMap.get(neededCycle).keySet();

				if (!validVarNames.contains(neededVar)) {
					
					if (sd.getModelDataSetMap().get(neededCycle.toLowerCase()).tsList.contains(neededVar.toLowerCase())){
					
						LogUtils.errMsg( fromWresl + " "+neededVar + "[" + neededCycle + "] cannot be of Timeseries type." );
						
					} else {

						LogUtils.errMsg( fromWresl + " variable named [" + varName + "] has an undefined variable: "+ neededVar + "[" + neededCycle + "]");						
					}

					continue;
				}

				// / create space in varCycleValue map
				Map<String, Map<String, IntDouble>> vcv = sd.getVarCycleValueMap();
				
				// / create space in varCycleValue map where cycles contains neededCycle and past cycles
				
				ArrayList<String> cycleListBase = new ArrayList<String>(sd.getModelList());
				ArrayList<String> cycleList = new ArrayList<String>(sd.getModelList());
				
				int indexOfNeededCycle = cycleListBase.indexOf(neededCycle);
				
				
//				for (int i=1 ; i <= n_buffer; i++){
//					cycleList.add(0, cycleListBase.get(cycleListBase.size()-i));
//				}
//				indexOfNeededCycle = indexOfNeededCycle + n_buffer ;

				int beginIndex = Math.max( indexOfNeededCycle-n_buffer, 0);				
				
				for (int i= beginIndex ; i <= indexOfNeededCycle; i++){
					String cycleN = cycleList.get(i);
				
					//===================
					if (vcv.keySet().contains(neededVar)) {
						vcv.get(neededVar).put(cycleN, null);
					} else {
						Map<String, IntDouble> t = new HashMap<String, IntDouble>();
						t.put(cycleN, null);
						vcv.put(neededVar, t);
					}
	
					// / add to set varUsedByLaterCycle
					mdsm.get(cycleN).varUsedByLaterCycle.add(neededVar);
					//===================
				}
				
			}
		}

	}
  
  private static void checkUsedBeforeDefined(SimulationDataSet ds, ArrayList<String> ordered_list_sv_as_ex, Set<String> previous_global_sv_as_ex_ts) {
	
	  Set<String> dep;
	  
	  for (int i=0; i< ds.svList.size(); i++ ){
		  
		  String svName = ds.svList.get(i);
		 // TODO: serious bug. Map object is not reinitialized
		 dep = new HashSet<String>(ds.svMap.get(svName).dependants);
		 //dep = ds.svMap.get(svName).dependants; // this will cause problem in shallow copy
		  dep.removeAll(ds.tsSet);	
		  dep.removeAll(previous_global_sv_as_ex_ts);	
		  
		  
		  //dep.removeAll(ds.svList.subList(0, i)); 
		  if (ordered_list_sv_as_ex.contains(svName)) { 
			  int index_sv = ordered_list_sv_as_ex.indexOf(svName);
			  dep.removeAll(ordered_list_sv_as_ex.subList(0, index_sv));
		  }
		  
		  if (dep.size()>0){
			  
			  //Set<String> undefinedVar = new HashSet<String>(dep);
			  //undefinedVar.removeAll(ds.asSet);
			  
			  //allows recursive svar definition for future array
			  LogUtils.warningMsg(ds.svMap.get(svName).fromWresl + " variable names used before definition: "+dep+" in Svar named ["+svName+"]"   );
			  //LogUtils.errMsg(ds.svMap.get(svName).fromWresl + " variable names used before definition: "+dep+" in Svar named ["+svName+"]"   );
			  
		  }
	  }
	  
	  for (int i=0; i< ds.asList.size(); i++ ){
		  
		  String asName = ds.asList.get(i);

		  dep = new HashSet<String>(ds.asMap.get(asName).dependants);

		  dep.removeAll(ds.tsSet);		  
		  dep.removeAll(previous_global_sv_as_ex_ts);
		  
		  //dep.removeAll(ds.asList.subList(0, i));
		  if (ordered_list_sv_as_ex.contains(asName)) { 
			  int index_as = ordered_list_sv_as_ex.indexOf(asName);
			  dep.removeAll(ordered_list_sv_as_ex.subList(0, index_as));
		  }
		  
		  if (dep.size()>0){
			  
			  LogUtils.errMsg( ds.asMap.get(asName).fromWresl + " variable names used before definition: " + dep + " in Alias named ["+asName +"]"  );
			  
		  }
	  }
  }

	private static void convertRestAliasInGoalExpression(SimulationDataSet ds) {

		Set<String> asS = new LinkedHashSet<String>(ds.asSet);

		for (String e : asS) {
					
			String goalName = e+"__alias";
			convertAlias(ds, e, goalName);

		}
	}

  private static Set<String> checkAliasInGoalExpression(SimulationDataSet ds, Set<String> goalSetToCheck) {
	
	  Set<String> out_newGoalSet = new LinkedHashSet<String>();
	  ArrayList<String> dep;
	  Set<String> asSet = new LinkedHashSet<String>(ds.asSet);
	  
		for (String gName: goalSetToCheck) {

			dep = new ArrayList<String>(ds.gMap.get(gName).expressionDependants);

			dep.removeAll(ds.tsSet);
			dep.removeAll(ds.svSet);			
			dep.removeAll(ds.dvSet);
			
			Collections.sort(dep);
			for (String e : dep) {
				
				if ( asSet.contains(e) ){
					
					String goalName = e+"__alias";
					convertAlias(ds, e, goalName);
					
					out_newGoalSet.add(goalName);
				}
			}
		}
		return out_newGoalSet;
  }  
  
private static Map<String, SimulationDataSet> getNewDataSet(Set<String> adhoc_incFileSet, Set<String> fileDataMap_wholeStudy_keySet) throws RecognitionException, IOException
  {
    Map<String, SimulationDataSet> fileDataMap_new = new HashMap<String, SimulationDataSet>();

    for (String f : adhoc_incFileSet)
    {
      if (fileDataMap_wholeStudy_keySet.contains(f))
      {
        continue;
      }

      Map<String, SimulationDataSet> each = FileParser.processNestedFileExceptFor(f, fileDataMap_wholeStudy_keySet);
      fileDataMap_new.putAll(each);
    }

    return fileDataMap_new;
  }

  private static Map<String, SimulationDataSet> copyDataSetToThisModel(Set<String> incFileSet, Map<String, SimulationDataSet> fileDataMap_wholeStudy)
  {
    Map<String, SimulationDataSet> fileDataMap_thisModel = new HashMap<String, SimulationDataSet>();

    for (String f : incFileSet)
    {
      fileDataMap_thisModel.putAll(Tools.putDataFileMapFromWholeStudy(f, fileDataMap_wholeStudy));
    }

    return fileDataMap_thisModel;
  }

  private static SimulationDataSet correctScopeAndPrioritize(String modelName, SimulationDataSet adhoc, String absMainFilePath, Map<String, SimulationDataSet> fileDataMap_thisModel, Map<String, SimulationDataSet> fileDataMap_wholeStudy, Map<String, Set<String>> t1Map_wholeStudy, Map<String, String> fileScopeMap_wholeStudy)
    throws RecognitionException, IOException
  {
    Map<String, String> fileScopeMap = new HashMap<String, String>(fileScopeMap_wholeStudy);
    fileScopeMap.putAll(Tools.getScopeMap(adhoc.incFileSet, adhoc.incFileSet_local));

    Map<String, Set<String>> t1Map = new HashMap<String, Set<String>>(t1Map_wholeStudy);
    t1Map.put(absMainFilePath, adhoc.incFileSet);
    Map<String, Set<String>> t1ReverseMap = Tools.getReverseMap(t1Map);

    Map<String, SimulationDataSet> fileDataMap_corrected = new HashMap<String, SimulationDataSet>(fileDataMap_thisModel);
    SimulationDataSet ds;
    for (String f : fileDataMap_thisModel.keySet())
    {
      ds = Tools.correctDataScope_deep(f, fileDataMap_thisModel.get(f), fileScopeMap, t1ReverseMap);
      fileDataMap_corrected.put(f, ds);
    }

    SimulationDataSet model_dataset = new SimulationDataSet();

    for (String f : adhoc.incFileList)
    {
      SimulationDataSet temp = new SimulationDataSet();

      temp.addChildren(f, t1Map, fileDataMap_corrected);

      temp.overwrittenWith_set(fileDataMap_corrected.get(f));

      model_dataset.overwrittenWith_set(temp);
    }

    model_dataset.overwrittenWith_set(adhoc);

    return model_dataset;
  }

	private static boolean sortDependency(SimulationDataSet model_dataset, boolean rewrite_list_based_on_dependency) {
		boolean OK = true;


		// sort Svar + Alias
		Sort sort_Sv_As = new Sort(model_dataset.svMap, model_dataset.asMap, model_dataset.dvSet, model_dataset.tsSet, model_dataset.exSet);
				//model_dataset.exSet);

		ArrayList<String> sortedSvAsList = new ArrayList<String>();

		Set<String> var_with_unknown = sort_Sv_As.sort(sortedSvAsList);
		if (var_with_unknown.size() > 0) OK = false;

		ArrayList<String> sortedSvList = new ArrayList<String>(sortedSvAsList);
		ArrayList<String> sortedAsList = new ArrayList<String>(sortedSvAsList);
		
		sortedAsList.retainAll(model_dataset.asSet);
		sortedSvList.retainAll(model_dataset.svSet);

		model_dataset.asSet_unknown = new HashSet<String>(var_with_unknown);
		model_dataset.asSet_unknown.retainAll(model_dataset.asSet);

		model_dataset.svSet_unknown = new HashSet<String>(var_with_unknown);
		model_dataset.svSet_unknown.retainAll(model_dataset.svSet);

		if (rewrite_list_based_on_dependency) {
			model_dataset.asList = sortedAsList;
			model_dataset.asList.addAll(model_dataset.asSet_unknown);
			model_dataset.asSet = new LinkedHashSet<String>(model_dataset.asList);

			model_dataset.svList = sortedSvList;
			model_dataset.svList.addAll(model_dataset.svSet_unknown);
			model_dataset.svSet = new LinkedHashSet<String>(model_dataset.svList);
		}

		return OK;
	}

  private static ArrayList<String> getOrderedList(Map<String, SimulationDataSet> fileDataMap_thisModel, SimulationDataSet adhoc)
  {
    Map<String, ArrayList<String>> orderListMap = new HashMap<String, ArrayList<String>>();

    ArrayList<String> complete_ordered_list = new ArrayList<String>();

    Sort sortFile = new Sort(fileDataMap_thisModel);

    ArrayList<String> sortedFileList = new ArrayList<String>();

    sortFile.sort(sortedFileList);

    //------------------------------------------------
    // initialize
    for (String f: sortedFileList){
    	ArrayList<String> toBeAdded = new ArrayList<String>(fileDataMap_thisModel.get(f).ordered_list_including_files);
    	orderListMap.put(f, toBeAdded);
    }
    
    // iterate

			for (String f : sortedFileList) {
				SimulationDataSet ds = fileDataMap_thisModel.get(f);

				ArrayList<String> new_ordered_list = new ArrayList<String>(ds.ordered_list_including_files);

				// System.out.println(" new ordered_list_include_files: " +
				// new_ordered_list);

				for (String inf : ds.incFileList) {
					ArrayList<String> toBeAdded = orderListMap.get(inf);

					// System.out.println(" new ordered_list before: " +
					// new_ordered_list);
					// System.out.println(" toBeAdded: " + toBeAdded);

					int pos = new_ordered_list.indexOf(inf);
					new_ordered_list.addAll(pos, toBeAdded);
					new_ordered_list.remove(inf);
				}
				orderListMap.put(f, new_ordered_list);
				//System.out.println(" order list Map: " + orderListMap);
			}



    //--------------------------------------------------

    complete_ordered_list = new ArrayList<String>(adhoc.ordered_list_including_files);

    for (String inf : adhoc.incFileList)
    {
      ArrayList<String> toBeAdded = orderListMap.get(inf);

      int pos = complete_ordered_list.indexOf(inf);
      complete_ordered_list.addAll(pos, toBeAdded);
      complete_ordered_list.remove(inf);
    }
    return complete_ordered_list;
  }

	private static void convertAlias(SimulationDataSet ds, String aliasName, String goalName) {

		Alias as = ds.asMap.get(aliasName);

		// add e into dvar
		Dvar dv = new Dvar();

		dv.kind = as.kind;
		dv.units = as.units;
		dv.lowerBound = Param.lower_unbounded;
		dv.upperBound = Param.upper_unbounded;
		dv.fromWresl = as.fromWresl;
		dv.expression = as.expression;
		dv.dependants = as.dependants;
		dv.timeArraySize = as.timeArraySize;

		ds.dvMap.put(aliasName, dv);
		ds.dvList.add(aliasName);
		ds.dvSet.add(aliasName);

		if (as.scope.equalsIgnoreCase(Param.global)) {
			ds.dvList_global.add(aliasName);
			ds.dvSet_global.add(aliasName);
		}
		else if (as.scope.equalsIgnoreCase(Param.local)) {
			ds.dvList_local.add(aliasName);
			ds.dvSet_local.add(aliasName);
		}
		else {
			LogUtils.errMsg("Scope error when converting alias to dvar: " + aliasName, as.fromWresl);
		}

		// add additional goal

		Goal gl = new Goal();
		gl.caseCondition.add(Param.always);
		gl.caseName.add(Param.defaultCaseName);
		if (as.timeArraySize.equals("0")){
			gl.caseExpression.add(aliasName + "=" + as.expression);
		}else{
			gl.caseExpression.add(aliasName + "($m)=" + as.expression);
		}
		gl.expressionDependants = as.dependants;
		gl.fromWresl = as.fromWresl;
		gl.timeArraySize=as.timeArraySize;
	    gl.needVarFromEarlierCycle = as.needVarFromEarlierCycle;
	    gl.neededVarInCycleSet = new HashSet<String>(as.neededVarInCycleSet);


		ds.gMap.put(goalName, gl);
		ds.gList.add(goalName);
		ds.gSet.add(goalName);

		if (as.scope.equalsIgnoreCase(Param.global)) {
			ds.gList_global.add(goalName);
			ds.gSet_global.add(goalName);
		}
		else if (as.scope.equalsIgnoreCase(Param.local)) {
			ds.gList_local.add(goalName);
			ds.gSet_local.add(goalName);
		}
		else {
			LogUtils.errMsg("Scope error when adding constraint for alias: " + aliasName, as.fromWresl);
		}

		// remove e from alias
		ds.asList.remove(aliasName);
		ds.asList_global.remove(aliasName);
		ds.asList_local.remove(aliasName);
		ds.asSet.remove(aliasName);
		ds.asSet_global.remove(aliasName);
		ds.asSet_local.remove(aliasName);
		ds.asMap.remove(aliasName);

	}
}