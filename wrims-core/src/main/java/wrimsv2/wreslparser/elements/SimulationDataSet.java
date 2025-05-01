package wrimsv2.wreslparser.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import wrimsv2.commondata.wresldata.Alias;
import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.External;
import wrimsv2.commondata.wresldata.Goal;
import wrimsv2.commondata.wresldata.Svar;
import wrimsv2.commondata.wresldata.Timeseries;
import wrimsv2.commondata.wresldata.WeightElement;

public class SimulationDataSet
{
  public String currentAbsolutePath;
  public String currentAbsoluteParent;

  public ArrayList<String> model_list = new ArrayList<String>();
  public ArrayList<String> error_model_redefined = new ArrayList<String>();

  public ArrayList<String> error_sequence_redefined = new ArrayList<String>();
  public ArrayList<Integer> error_sequence_order_redefined = new ArrayList<Integer>();

  public Map<Integer, Sequence> seqMap = new HashMap<Integer, Sequence>();
  public ArrayList<String> seqList = new ArrayList<String>();

  public Set<String> wtSet = new LinkedHashSet<String>();
  public Set<String> wtSet_global = new LinkedHashSet<String>();
  public Set<String> wtSet_local = new LinkedHashSet<String>();
  public ArrayList<String> wtList = new ArrayList<String>();
  public ArrayList<String> wtList_global = new ArrayList<String>();
  public ArrayList<String> wtList_local = new ArrayList<String>();
  public Map<String, WeightElement> wtMap = new HashMap<String, WeightElement>();
  public Map<String, String> error_weightVar_redefined = new HashMap<String, String>();

  public Set<String> incFileSet = new LinkedHashSet<String>();
  public Set<String> incFileSet_global = new LinkedHashSet<String>();
  public Set<String> incFileSet_local = new LinkedHashSet<String>();
  public ArrayList<String> incFileList = new ArrayList<String>();
  public ArrayList<String> incFileList_global = new ArrayList<String>();
  public ArrayList<String> incFileList_local = new ArrayList<String>();
  public Map<String, IncludeFile> incFileMap = new HashMap<String, IncludeFile>();
  public ArrayList<String> error_includeFile_redefined = new ArrayList<String>();

  public ArrayList<String> ordered_list_including_files = new ArrayList<String>();
  public ArrayList<String> ordered_list = new ArrayList<String>();

  public ArrayList<String> ordered_list_including_files_global = new ArrayList<String>();
  public ArrayList<String> ordered_list_global = new ArrayList<String>();

  public Map<String, String> var_all = new HashMap<String, String>();
  public Map<String, String> error_var_redefined = new HashMap<String, String>();

  public Set<String> exSet = new LinkedHashSet<String>();
  public Set<String> exSet_global = new LinkedHashSet<String>();
  public Set<String> exSet_local = new LinkedHashSet<String>();
  public ArrayList<String> exList = new ArrayList<String>();
  public ArrayList<String> exList_global = new ArrayList<String>();
  public ArrayList<String> exList_local = new ArrayList<String>();
  public Map<String, External> exMap = new HashMap<String, External>();

  public Set<String> tsSet = new LinkedHashSet<String>();
  public Set<String> tsSet_global = new LinkedHashSet<String>();
  public Set<String> tsSet_local = new LinkedHashSet<String>();
  public ArrayList<String> tsList = new ArrayList<String>();
  public ArrayList<String> tsList_global = new ArrayList<String>();
  public ArrayList<String> tsList_local = new ArrayList<String>();
  public Map<String, Timeseries> tsMap = new HashMap<String, Timeseries>();

  public Set<String> svSet_unknown = new LinkedHashSet<String>();
  public Set<String> svSet = new LinkedHashSet<String>();
  public Set<String> svSet_global = new LinkedHashSet<String>();
  public Set<String> svSet_local = new LinkedHashSet<String>();
  public ArrayList<String> svList = new ArrayList<String>();
  public ArrayList<String> svList_global = new ArrayList<String>();
  public ArrayList<String> svList_local = new ArrayList<String>();
  public Map<String, Svar> svMap = new HashMap<String, Svar>();

  public Set<String> dvSet = new LinkedHashSet<String>();
  public Set<String> dvSet_global = new LinkedHashSet<String>();
  public Set<String> dvSet_local = new LinkedHashSet<String>();
  public ArrayList<String> dvList = new ArrayList<String>();
  public ArrayList<String> dvList_global = new ArrayList<String>();
  public ArrayList<String> dvList_local = new ArrayList<String>();
  public Map<String, Dvar> dvMap = new HashMap<String, Dvar>();

  public Set<String> asSet_unknown = new LinkedHashSet<String>();
  public Set<String> asSet = new LinkedHashSet<String>();
  public Set<String> asSet_global = new LinkedHashSet<String>();
  public Set<String> asSet_local = new LinkedHashSet<String>();
  public ArrayList<String> asList = new ArrayList<String>();
  public ArrayList<String> asList_global = new ArrayList<String>();
  public ArrayList<String> asList_local = new ArrayList<String>();
  public Map<String, Alias> asMap = new HashMap<String, Alias>();

  public Set<String> gSet = new LinkedHashSet<String>();
  public Set<String> gSet_global = new LinkedHashSet<String>();
  public Set<String> gSet_local = new LinkedHashSet<String>();
  public ArrayList<String> gList = new ArrayList<String>();
  public ArrayList<String> gList_global = new ArrayList<String>();
  public ArrayList<String> gList_local = new ArrayList<String>();
  public Map<String, Goal> gMap = new HashMap<String, Goal>();

  public Set<String> varNeedEarlierCycle = new LinkedHashSet<String>();
  public Map<String, Set<String>> var_neededVarCycle_map = new HashMap<String, Set<String>>();

  
  public SimulationDataSet()
  {
	  
  }

  /// warning: this is partial copy
  public SimulationDataSet(SimulationDataSet s)
  {
    this.overwrittenWith_set(s);

  }

  public SimulationDataSet overwrittenWith(SimulationDataSet s)
  {
    remove(s);
    add(s);
    return this;
  }

  public SimulationDataSet overwrite(SimulationDataSet s)
  {
    SimulationDataSet p = new SimulationDataSet(s);
    p.remove(this);
    add(p);
    return this;
  }

  public boolean hasDuplicateIn(SimulationDataSet s, String filePath, Map<String, Set<String>> reverseMap)
  {
    boolean b = false;

    if (s == null) System.out.println("Fatal error!!! SimulationDataSet is null in file: " + filePath);

    for (String e : s.wtList) {
      if (this.wtList.contains(e)) {
        String f1 = ((WeightElement)s.wtMap.get(e)).fromWresl;
        String f2 = ((WeightElement)this.wtMap.get(e)).fromWresl;
        LogUtils.errMsg("Weight table variable redefined: " + e, f1, f2, reverseMap);
        b = true;
      }
    }

    for (String e : s.incFileList) {
      if (!this.incFileList.contains(e)) {
        continue;
      }
      String f1 = ((IncludeFile)s.incFileMap.get(e)).fromWresl;
      String f2 = ((IncludeFile)this.incFileMap.get(e)).fromWresl;
      LogUtils.errMsg("Include file redefined: " + e, f1, f2, reverseMap);
      b = true;
    }

    for (String e : s.tsList) {
      if (this.tsList.contains(e)) {
        String f1 = ((Timeseries)s.tsMap.get(e)).fromWresl;
        String f2 = ((Timeseries)this.tsMap.get(e)).fromWresl;
        LogUtils.errMsg("State variable redefined: " + e, f1, f2, reverseMap);
        b = true;
      }
    }

    for (String e : s.svList) {
      if (this.svList.contains(e)) {
        String f1 = ((Svar)s.svMap.get(e)).fromWresl;
        String f2 = ((Svar)this.svMap.get(e)).fromWresl;
        LogUtils.errMsg("State variable redefined: " + e, f1, f2, reverseMap);
        b = true;
      }
    }

    for (String e : s.dvList) {
      if (this.dvList.contains(e)) {
        String f1 = ((Dvar)s.dvMap.get(e)).fromWresl;
        String f2 = ((Dvar)this.dvMap.get(e)).fromWresl;
        LogUtils.errMsg("Decision varriable redefined: " + e, f1, f2, reverseMap);
        b = true;
      }
    }

    for (String e : s.gList) {
      if (this.gList.contains(e)) {
        String f1 = ((Goal)s.gMap.get(e)).fromWresl;
        String f2 = ((Goal)this.gMap.get(e)).fromWresl;
        LogUtils.errMsg("Goal redefined: " + e, f1, f2, reverseMap);
        b = true;
      }
    }

    for (String e : s.asList) {
      if (this.asList.contains(e)) {
        String f1 = ((Alias)s.asMap.get(e)).fromWresl;
        String f2 = ((Alias)this.asMap.get(e)).fromWresl;
        LogUtils.errMsg("Alias redefined: " + e, f1, f2, reverseMap);
        b = true;
      }
    }

    for (String e : s.model_list) {
      if (!this.model_list.contains(e))
        continue;
      LogUtils.errMsg("Error!!! Model redefined: " + e + " in file: " + filePath);
      b = true;
    }

    for (String e : s.seqList) {
      if (!this.seqList.contains(e))
        continue;
      LogUtils.errMsg("Error!!! Sequence redefined: " + e + " in file: " + filePath);
      b = true;
    }

    return b;
  }

  public SimulationDataSet convertToLocal()
  {
    if (!this.wtList.isEmpty()) {
      this.wtList_local = new ArrayList<String>();
      this.wtList_local.addAll(this.wtList);
      this.wtList_global = new ArrayList<String>();
    }

    if (!this.incFileList.isEmpty()) {
      this.incFileList_local = new ArrayList<String>();
      this.incFileList_local.addAll(this.incFileList);
      this.incFileList_global = new ArrayList<String>();
    }

    if (!this.tsList.isEmpty()) {
      this.tsList_local = new ArrayList<String>();
      this.tsList_local.addAll(this.tsList);
      this.tsList_global = new ArrayList<String>();
    }

    if (!this.svList.isEmpty()) {
      this.svList_local = new ArrayList<String>();
      this.svList_local.addAll(this.svList);
      this.svList_global = new ArrayList<String>();
    }

    if (!this.dvList.isEmpty()) {
      this.dvList_local = new ArrayList<String>();
      this.dvList_local.addAll(this.dvList);
      this.dvList_global = new ArrayList<String>();
    }
    if (!this.asList.isEmpty()) {
      this.asList_local = new ArrayList<String>();
      this.asList_local.addAll(this.asList);
      this.asList_global = new ArrayList<String>();
    }

    if (!this.gList.isEmpty()) {
      this.gList_local = new ArrayList<String>();
      this.gList_local.addAll(this.gList);
      this.gList_global = new ArrayList<String>();
    }

    return this;
  }

  public SimulationDataSet getGlobalVars()
  {
    SimulationDataSet out = new SimulationDataSet();

    if (!this.wtList_global.isEmpty()) {
      out.wtList.addAll(this.wtList_global);
      out.wtList_global.addAll(this.wtList_global);

      for (String key : this.wtList_global) {
        out.wtMap.put(key, (WeightElement)this.wtMap.get(key));
      }
    }

    if (!this.incFileList_global.isEmpty()) {
      out.incFileList.addAll(this.incFileList_global);
      out.incFileList_global.addAll(this.incFileList_global);

      for (String key : this.incFileList_global) {
        out.incFileMap.put(key, (IncludeFile)this.incFileMap.get(key));
      }
    }

    if (!this.exList_global.isEmpty()) {
      out.exList.addAll(this.exList_global);
      out.exList_global.addAll(this.exList_global);

      for (String key : this.exList_global) {
        out.exMap.put(key, (External)this.exMap.get(key));
      }
    }

    if (!this.tsList_global.isEmpty()) {
      out.tsList.addAll(this.tsList_global);
      out.tsList_global.addAll(this.tsList_global);

      for (String key : this.tsList_global) {
        out.tsMap.put(key, (Timeseries)this.tsMap.get(key));
      }
    }

    if (!this.svList_global.isEmpty()) {
      out.svList.addAll(this.svList_global);
      out.svList_global.addAll(this.svList_global);

      for (String key : this.svList_global) {
        out.svMap.put(key, (Svar)this.svMap.get(key));
      }
    }

    if (!this.dvList_global.isEmpty()) {
      out.dvList.addAll(this.dvList_global);
      out.dvList_global.addAll(this.dvList_global);

      for (String key : this.dvList_global) {
        out.dvMap.put(key, (Dvar)this.dvMap.get(key));
      }
    }

    if (!this.gList_global.isEmpty()) {
      out.gList.addAll(this.gList_global);
      out.gList_global.addAll(this.gList_global);

      for (String key : this.gList_global) {
        out.gMap.put(key, (Goal)this.gMap.get(key));
      }

    }

    if (!this.varNeedEarlierCycle.isEmpty()) {
        out.varNeedEarlierCycle.addAll(this.varNeedEarlierCycle);
        out.varNeedEarlierCycle.retainAll(out.svList_global);
        out.varNeedEarlierCycle.retainAll(out.asList_global);

        for (String key : out.varNeedEarlierCycle) {
          out.var_neededVarCycle_map.put(key, this.var_neededVarCycle_map.get(key));
        }

     }
    
    return out;
  }

  public SimulationDataSet remove(SimulationDataSet s)
  {
    if (!s.wtList.isEmpty()) {
      this.wtList.removeAll(s.wtList);
      this.wtList_global.removeAll(s.wtList);
      this.wtList_local.removeAll(s.wtList);

      Tools.mapRemoveAll(this.wtMap, s.wtList);
    }

    if (!s.incFileList.isEmpty()) {
      this.incFileList.removeAll(s.incFileList);
      this.incFileList_global.removeAll(s.incFileList);
      this.incFileList_local.removeAll(s.incFileList);

      Tools.mapRemoveAll(this.incFileMap, s.incFileList);
    }

    if (!s.exList.isEmpty()) {
      this.exList.removeAll(s.exList);
      if (!s.exList_global.isEmpty()) this.exList_global.removeAll(s.exList);
      if (!s.exList_local.isEmpty()) this.exList_local.removeAll(s.exList);

      Tools.mapRemoveAll(this.exMap, s.exList);
    }

    if (!s.tsList.isEmpty()) {
      this.tsList.removeAll(s.tsList);
      if (!s.tsList_global.isEmpty()) this.tsList_global.removeAll(s.tsList);
      if (!s.tsList_local.isEmpty()) this.tsList_local.removeAll(s.tsList);

      Tools.mapRemoveAll(this.tsMap, s.tsList);
    }

    if (!s.svList.isEmpty()) {
      this.svList.removeAll(s.svList);
      if (!s.svList_global.isEmpty()) this.svList_global.removeAll(s.svList);
      if (!s.svList_local.isEmpty()) this.svList_local.removeAll(s.svList);

      Tools.mapRemoveAll(this.svMap, s.svList);
    }

    if (!s.dvList.isEmpty()) {
      this.dvList.removeAll(s.dvList);
      if (!s.dvList_global.isEmpty()) this.dvList_global.removeAll(s.dvList);
      if (!s.dvList_local.isEmpty()) this.dvList_local.removeAll(s.dvList);

      Tools.mapRemoveAll(this.dvMap, s.dvList);
    }
    if (!s.asList.isEmpty()) {
      this.asList.removeAll(s.gList);
      if (!s.asList_global.isEmpty()) this.asList_global.removeAll(s.asList);
      if (!s.asList_local.isEmpty()) this.asList_local.removeAll(s.asList);

      Tools.mapRemoveAll(this.asMap, s.asList);
    }

    if (!s.gList.isEmpty()) {
      this.gList.removeAll(s.gList);
      if (!s.gList_global.isEmpty()) this.gList_global.removeAll(s.gList);
      if (!s.gList_local.isEmpty()) this.gList_local.removeAll(s.gList);

      Tools.mapRemoveAll(this.gMap, s.gList);
    }

    if (!s.varNeedEarlierCycle.isEmpty()) {
        this.varNeedEarlierCycle.removeAll(s.varNeedEarlierCycle);

        Tools.mapRemoveAll(this.var_neededVarCycle_map, s.varNeedEarlierCycle);
      }
    
    if (!s.model_list.isEmpty()) {
      this.model_list.removeAll(s.model_list);
    }

    return this;
  }

  public SimulationDataSet add(SimulationDataSet s)
  {
    if (!s.wtList.isEmpty()) {
      this.wtList.addAll(s.wtList);
      if (!s.wtList_global.isEmpty()) this.wtList_global.addAll(s.wtList_global);
      if (!s.wtList_local.isEmpty()) this.wtList_local.addAll(s.wtList_local);
      this.wtMap.putAll(s.wtMap);
    }

    if (!s.incFileList.isEmpty()) {
      this.incFileList.addAll(s.incFileList);
      if (!s.incFileList_global.isEmpty()) this.incFileList_global.addAll(s.incFileList_global);
      if (!s.incFileList_local.isEmpty()) this.incFileList_local.addAll(s.incFileList_local);
      this.incFileMap.putAll(s.incFileMap);
    }

    if (!s.exList.isEmpty()) {
      this.exList.addAll(s.exList);
      if (!s.exList_global.isEmpty()) this.exList_global.addAll(s.exList_global);
      if (!s.exList_local.isEmpty()) this.exList_local.addAll(s.exList_local);
      this.exMap.putAll(s.exMap);
    }

    if (!s.tsList.isEmpty()) {
      this.tsList.addAll(0, s.tsList);
      if (!s.tsList_global.isEmpty()) this.tsList_global.addAll(s.tsList_global);
      if (!s.tsList_local.isEmpty()) this.tsList_local.addAll(s.tsList_local);
      this.tsMap.putAll(s.tsMap);
    }

    if (!s.svList.isEmpty()) {
      this.svList.addAll(s.svList);
      if (!s.svList_global.isEmpty()) this.svList_global.addAll(s.svList_global);
      if (!s.svList_local.isEmpty()) this.svList_local.addAll(s.svList_local);
      this.svMap.putAll(s.svMap);
    }

    if (!s.dvList.isEmpty()) {
      this.dvList.addAll(s.dvList);
      if (!s.dvList_global.isEmpty()) this.dvList_global.addAll(s.dvList_global);
      if (!s.dvList_local.isEmpty()) this.dvList_local.addAll(s.dvList_local);
      this.dvMap.putAll(s.dvMap);
    }
    if (!s.asList.isEmpty()) {
      this.asList.addAll(s.asList);
      if (!s.asList_global.isEmpty()) this.asList_global.addAll(s.asList_global);
      if (!s.asList_local.isEmpty()) this.asList_local.addAll(s.asList_local);
      this.asMap.putAll(s.asMap);
    }

    if (!s.gList.isEmpty()) {
      this.gList.addAll(s.gList);
      if (!s.gList_global.isEmpty()) this.gList_global.addAll(s.gList_global);
      if (!s.gList_local.isEmpty()) this.gList_local.addAll(s.gList_local);
      this.gMap.putAll(s.gMap);
    }

    if (!s.varNeedEarlierCycle.isEmpty()) {
        this.varNeedEarlierCycle.addAll(s.varNeedEarlierCycle);
        this.var_neededVarCycle_map.putAll(s.var_neededVarCycle_map);
    }
    
    if (!s.model_list.isEmpty()) {
      this.model_list.addAll(s.model_list);
    }

    if (!s.seqList.isEmpty()) {
      this.seqList.addAll(s.seqList);
      this.seqMap.putAll(s.seqMap);
    }

    return this;
  }

  public SimulationDataSet addNonDuplicate(SimulationDataSet s)
  {
    s.remove(this);

    return add(s);
  }

  public SimulationDataSet dePrioritize(SimulationDataSet laterFileData, String filePath_forErrorMessage, Map<String, Set<String>> reverseMap)
  {
    if (laterFileData == null) System.out.println("Fatal error!!! SimulationDataSet is null in file: " + filePath_forErrorMessage);

    if (hasDuplicateIn(laterFileData, filePath_forErrorMessage, reverseMap))
      overwrite(laterFileData);
    else {
      add(laterFileData);
    }

    return this;
  }

  public SimulationDataSet prioritize_append(SimulationDataSet laterFileData, String filePath_forErrorMessage, Map<String, Set<String>> reverseMap)
  {
    if (laterFileData == null) System.out.println("Fatal error!!! SimulationDataSet is null in file: " + filePath_forErrorMessage);

    if (hasDuplicateIn(laterFileData, filePath_forErrorMessage, reverseMap)) {
      remove(laterFileData);
    }

    return add(laterFileData);
  }

  public SimulationDataSet prioritize_prepend(SimulationDataSet laterFileData, String filePath_forErrorMessage, Map<String, Set<String>> reverseMap)
  {
    if (laterFileData == null) System.out.println("Fatal error!!! SimulationDataSet is null in file: " + filePath_forErrorMessage);

    if (hasDuplicateIn(laterFileData, filePath_forErrorMessage, reverseMap)) {
      remove(laterFileData);
    }

    return insert(laterFileData);
  }

  public SimulationDataSet addChildren(String nodeFile, Map<String, Set<String>> t1Map, Map<String, SimulationDataSet> fileDataMap)
  {
    for (String childFile : t1Map.get(nodeFile))
    {
      if (t1Map.get(childFile) != null) addChildren(childFile, t1Map, fileDataMap);

      overwrittenWith_set((SimulationDataSet)fileDataMap.get(childFile));
    }

    return this;
  }

  public SimulationDataSet insert(SimulationDataSet s)
  {
    if (!s.wtList.isEmpty()) {
      this.wtList.addAll(0, s.wtList);
      if (!s.wtList_global.isEmpty()) this.wtList_global.addAll(0, s.wtList_global);
      if (!s.wtList_local.isEmpty()) this.wtList_local.addAll(0, s.wtList_local);
      this.wtMap.putAll(s.wtMap);
    }

    if (!s.incFileList.isEmpty()) {
      this.incFileList.addAll(0, s.incFileList);
      if (!s.incFileList_global.isEmpty()) this.incFileList_global.addAll(0, s.incFileList_global);
      if (!s.incFileList_local.isEmpty()) this.incFileList_local.addAll(0, s.incFileList_local);
      this.incFileMap.putAll(s.incFileMap);
    }

    if (!s.exList.isEmpty()) {
      this.exList.addAll(0, s.exList);
      if (!s.exList_global.isEmpty()) this.exList_global.addAll(0, s.exList_global);
      if (!s.exList_local.isEmpty()) this.exList_local.addAll(0, s.exList_local);
      this.exMap.putAll(s.exMap);
    }

    if (!s.tsList.isEmpty()) {
      this.tsList.addAll(0, s.tsList);
      if (!s.tsList_global.isEmpty()) this.tsList_global.addAll(0, s.tsList_global);
      if (!s.tsList_local.isEmpty()) this.tsList_local.addAll(0, s.tsList_local);
      this.tsMap.putAll(s.tsMap);
    }

    if (!s.svList.isEmpty()) {
      this.svList.addAll(0, s.svList);
      if (!s.svList_global.isEmpty()) this.svList_global.addAll(0, s.svList_global);
      if (!s.svList_local.isEmpty()) this.svList_local.addAll(0, s.svList_local);
      this.svMap.putAll(s.svMap);
    }

    if (!s.dvList.isEmpty()) {
      this.dvList.addAll(0, s.dvList);
      if (!s.dvList_global.isEmpty()) this.dvList_global.addAll(0, s.dvList_global);
      if (!s.dvList_local.isEmpty()) this.dvList_local.addAll(0, s.dvList_local);
      this.dvMap.putAll(s.dvMap);
    }
    if (!s.asList.isEmpty()) {
      this.asList.addAll(0, s.asList);
      if (!s.asList_global.isEmpty()) this.asList_global.addAll(0, s.asList_global);
      if (!s.asList_local.isEmpty()) this.asList_local.addAll(0, s.asList_local);
      this.asMap.putAll(s.asMap);
    }

    if (!s.gList.isEmpty()) {
      this.gList.addAll(0, s.gList);
      if (!s.gList_global.isEmpty()) this.gList_global.addAll(0, s.gList_global);
      if (!s.gList_local.isEmpty()) this.gList_local.addAll(0, s.gList_local);
      this.gMap.putAll(s.gMap);
    }

    if (!s.varNeedEarlierCycle.isEmpty()) {
    	// TODO: find a way to insert linkedHashSet
        this.varNeedEarlierCycle.addAll(s.varNeedEarlierCycle);
        this.var_neededVarCycle_map.putAll(s.var_neededVarCycle_map);
      }
    
    if (!s.model_list.isEmpty()) {
      this.model_list.addAll(0, s.model_list);
    }

    if (!s.seqList.isEmpty()) {
      this.seqList.addAll(0, s.seqList);
      this.seqMap.putAll(s.seqMap);
    }

    return this;
  }

  public SimulationDataSet overwrite_set(SimulationDataSet s) { 
	  return overwrite_set(s, false);
  }
  public SimulationDataSet overwrite_set(SimulationDataSet s, boolean errMessage) {
	  
//		this.ordered_list_including_files.addAll(0,s.ordered_list_including_files);  
//		this.ordered_list.addAll(0,s.ordered_list);   
	  
    SimulationDataSet x = new SimulationDataSet(s);
    //x.overwrittenWith_set(s);
    
    Tools.mapRemoveAll(x.dvMap, this.dvSet);
    Tools.mapRemoveAll(x.svMap, this.svSet);
    Tools.mapRemoveAll(x.asMap, this.asSet);
    Tools.mapRemoveAll(x.wtMap, this.wtSet);
    Tools.mapRemoveAll(x.tsMap, this.tsSet);
    Tools.mapRemoveAll(x.gMap, this.gSet);
    Tools.mapRemoveAll(x.exMap, this.exSet);
    Tools.mapRemoveAll(x.incFileMap, this.incFileSet);
    Tools.mapRemoveAll(x.var_neededVarCycle_map, this.varNeedEarlierCycle);

    x.dvSet.addAll(this.dvSet);
    x.dvSet_global.addAll(this.dvSet_global);
    x.dvSet_local.addAll(this.dvSet_local);
    this.dvSet = x.dvSet;
    this.dvSet_global = x.dvSet_global;
    this.dvSet_local = x.dvSet_local;
    this.dvMap.putAll(x.dvMap);

    x.svSet.addAll(this.svSet);
    x.svSet_global.addAll(this.svSet_global);
    x.svSet_local.addAll(this.svSet_local);  
    this.svSet = x.svSet;
    this.svSet_global = x.svSet_global;
    this.svSet_local = x.svSet_local;
    this.svMap.putAll(x.svMap);

    x.tsSet.addAll(this.tsSet);
    x.tsSet_global.addAll(this.tsSet_global);
    x.tsSet_local.addAll(this.tsSet_local);
    this.tsSet = x.tsSet;
    this.tsSet_global = x.tsSet_global;
    this.tsSet_local = x.tsSet_local;
    this.tsMap.putAll(x.tsMap);

    x.asSet.addAll(this.asSet);
    x.asSet_global.addAll(this.asSet_global);
    x.asSet_local.addAll(this.asSet_local);
    this.asSet = x.asSet;
    this.asSet_global = x.asSet_global;
    this.asSet_local = x.asSet_local;
    this.asMap.putAll(x.asMap);

    x.gSet.addAll(this.gSet);
    x.gSet_global.addAll(this.gSet_global);
    x.gSet_local.addAll(this.gSet_local);
    this.gSet = x.gSet;
    this.gSet_global = x.gSet_global;
    this.gSet_local = x.gSet_local;
    this.gMap.putAll(x.gMap);

    x.wtSet.addAll(this.wtSet);
    x.wtSet_global.addAll(this.wtSet_global);
    x.wtSet_local.addAll(this.wtSet_local);
    this.wtSet = x.wtSet;
    this.wtSet_global = x.wtSet_global;
    this.wtSet_local = x.wtSet_local;
    this.wtMap.putAll(x.wtMap);

    x.exSet.addAll(this.exSet);
    x.exSet_global.addAll(this.exSet_global);
    x.exSet_local.addAll(this.exSet_local);
    this.exSet = x.exSet;
    this.exSet_global = x.exSet_global;
    this.exSet_local = x.exSet_local;
    this.exMap.putAll(x.exMap);

    x.incFileSet.addAll(this.incFileSet);
    x.incFileSet_global.addAll(this.incFileSet_global);
    x.incFileSet_local.addAll(this.incFileSet_local);
    this.incFileSet = x.incFileSet;
    this.incFileSet_global = x.incFileSet_global;
    this.incFileSet_local = x.incFileSet_local;
    this.incFileMap.putAll(x.incFileMap);

    x.varNeedEarlierCycle.addAll(this.varNeedEarlierCycle);
    this.varNeedEarlierCycle = x.varNeedEarlierCycle;
    this.var_neededVarCycle_map.putAll(x.var_neededVarCycle_map);
    
    return this;
  }

	public SimulationDataSet replaceGlobalWithDuplicateGlobal(Set<String> keys, SimulationDataSet s) {

		for (String k : keys) {

			if (this.dvSet.contains(k)){
//				this.dvSet_global.remove(k);
//				this.dvSet_local.add(k);
				this.dvMap.put(k,s.dvMap.get(k));
			}
			if (this.svSet.contains(k)){
//				this.svSet_global.remove(k);
//				this.svSet_local.add(k);
				this.svMap.put(k,s.svMap.get(k));
			}

			if (this.tsSet.contains(k)){
//				this.tsSet_global.remove(k);
//				this.tsSet_local.add(k);
				this.tsMap.put(k,s.tsMap.get(k));
			}
			if (this.asSet.contains(k)){
//				this.asSet_global.remove(k);
//				this.asSet_local.add(k);
				this.asMap.put(k,s.asMap.get(k));
			}
			if (this.gSet.contains(k)){
//				this.gSet_global.remove(k);
//				this.gSet_local.add(k);
				this.gMap.put(k,s.gMap.get(k));
			}
			if (this.exSet.contains(k)){
//				this.exSet_global.remove(k);
//				this.exSet_local.add(k);
				this.exMap.put(k,s.exMap.get(k));
			}
			if (this.varNeedEarlierCycle.contains(k)){
				this.var_neededVarCycle_map.put(k,s.var_neededVarCycle_map.get(k));
			}
		}
		
		return this;
	}

	public SimulationDataSet replaceGlobalWithDuplicateLocal(Set<String> keys, SimulationDataSet s) throws TypeRedefinedException {

		for (String k : keys) {

			if (this.dvSet.contains(k)){
				
				if( !s.dvSet.contains(k)) LogUtils.typeRedefinedErrMsg("Variable is redefined as different type: "+k);
				
				this.dvSet_global.remove(k);
				this.dvSet_local.add(k);
				this.dvMap.put(k,s.dvMap.get(k));
			}
			if (this.svSet.contains(k)){
				if( !s.svSet.contains(k)) LogUtils.typeRedefinedErrMsg("Variable is redefined as different type: "+k);
				this.svSet_global.remove(k);
				this.svSet_local.add(k);
				this.svMap.put(k,s.svMap.get(k));
			}

			if (this.tsSet.contains(k)){
				if( !s.tsSet.contains(k)) LogUtils.typeRedefinedErrMsg("Variable is redefined as different type: "+k);
				this.tsSet_global.remove(k);
				this.tsSet_local.add(k);
				this.tsMap.put(k,s.tsMap.get(k));
			}
			if (this.asSet.contains(k)){
				if( !s.asSet.contains(k)) LogUtils.typeRedefinedErrMsg("Variable is redefined as different type: "+k);
				this.asSet_global.remove(k);
				this.asSet_local.add(k);
				this.asMap.put(k,s.asMap.get(k));
			}
			if (this.gSet.contains(k)){
				if( !s.gSet.contains(k)) LogUtils.typeRedefinedErrMsg("Variable is redefined as different type: "+k);
				this.gSet_global.remove(k);
				this.gSet_local.add(k);
				this.gMap.put(k,s.gMap.get(k));
			}
			if (this.exSet.contains(k)){
				if( !s.exSet.contains(k)) LogUtils.typeRedefinedErrMsg("Variable is redefined as different type: "+k);
				this.exSet_global.remove(k);
				this.exSet_local.add(k);
				this.exMap.put(k,s.exMap.get(k));
			}
			if (this.varNeedEarlierCycle.contains(k)){
				this.var_neededVarCycle_map.put(k,s.var_neededVarCycle_map.get(k));
			}
		}
		
		return this;
	}
	
	public SimulationDataSet getGlobalVars_set()
  {
    SimulationDataSet out = new SimulationDataSet();

    Set<String> t;
    t= new LinkedHashSet<String>(this.asSet);
    t.retainAll(this.asSet_global);
    out.asSet.addAll(t);
    out.asSet_global.addAll(t);

    for (String key : this.asSet_global) {
      out.asMap.put(key, (Alias)this.asMap.get(key));
    }

    t = new LinkedHashSet<String>(this.wtSet);
    t.retainAll(this.wtSet_global);
    out.wtSet.addAll(t);
    out.wtSet_global.addAll(t);

    for (String key : this.wtSet_global) {
      out.wtMap.put(key, (WeightElement)this.wtMap.get(key));
    }

    t = new LinkedHashSet<String>(this.incFileSet);
    t.retainAll(this.incFileSet_global);
    out.incFileSet.addAll(t);
    out.incFileSet_global.addAll(t);

    for (String key : this.incFileSet_global) {
      out.incFileMap.put(key, (IncludeFile)this.incFileMap.get(key));
    }

    t = new LinkedHashSet<String>(this.exSet);
    t.retainAll(this.exSet_global);
    out.exSet.addAll(t);
    out.exSet_global.addAll(t);

    for (String key : this.exSet_global) {
      out.exMap.put(key, (External)this.exMap.get(key));
    }

    t = new LinkedHashSet<String>(this.svSet);
    t.retainAll(this.svSet_global);
    
    out.svSet.addAll(t);
    out.svSet_global.addAll(t);

    for (String key : this.svSet_global) {
      out.svMap.put(key, (Svar)this.svMap.get(key));
    }

    t = new LinkedHashSet<String>(this.tsSet);
    t.retainAll(this.tsSet_global);
    out.tsSet.addAll(t);
    out.tsSet_global.addAll(t);

    for (String key : this.tsSet_global) {
      out.tsMap.put(key, (Timeseries)this.tsMap.get(key));
    }

    t = new LinkedHashSet<String>(this.dvSet);
    t.retainAll(this.dvSet_global);
    out.dvSet.addAll(t);
    out.dvSet_global.addAll(t);

    for (String key : this.dvSet_global) {
      out.dvMap.put(key, (Dvar)this.dvMap.get(key));
    }

    t = new LinkedHashSet<String>(this.gSet);
    t.retainAll(this.gSet_global);
    out.gSet.addAll(t);
    out.gSet_global.addAll(t);

    for (String key : this.gSet_global) {
      out.gMap.put(key, (Goal)this.gMap.get(key));
    }

    ////////
    t = new LinkedHashSet<String>(this.varNeedEarlierCycle);
    t.retainAll(this.svSet_global);
    t.retainAll(this.asSet_global);
    out.varNeedEarlierCycle.addAll(t);
    for (String key : out.varNeedEarlierCycle) {
        out.var_neededVarCycle_map.put(key, this.var_neededVarCycle_map.get(key));
    }
    
    return out;
  }

  public SimulationDataSet convertToLocal_set(){
	  
    this.asSet_local.addAll(this.asSet);
    this.asSet_global.clear();

    this.wtSet_local.addAll(this.wtSet);
    this.wtSet_global.clear();

    this.tsSet_local.addAll(this.tsSet);
    this.tsSet_global.clear();

    this.dvSet_local.addAll(this.dvSet);
    this.dvSet_global.clear();

    this.svSet_local.addAll(this.svSet);
    this.svSet_global.clear();

    this.exSet_local.addAll(this.exSet);
    this.exSet_global.clear();

    this.gSet_local.addAll(this.gSet);
    this.gSet_global.clear();

    this.incFileSet_local.addAll(this.incFileSet);
    this.incFileSet_global.clear();

    return this;
  }

public Set<String> getAllGlobalVarsSet_except_file_and_weight(){
		
		Set<String> out = new HashSet<String>();
		
		out.addAll(this.asSet_global);
		out.addAll(this.dvSet_global);
		out.addAll(this.svSet_global);
		out.addAll(this.gSet_global);
		out.addAll(this.exSet_global);

	    return out;
	  }  

public Set<String> getAllLocalVarsSet_except_file_and_weight(){
	
	Set<String> out = new HashSet<String>();
	
	out.addAll(this.asSet_local);
	out.addAll(this.dvSet_local);
	out.addAll(this.svSet_local);
	out.addAll(this.gSet_local);
	out.addAll(this.exSet_local);

    return out;
  } 

public Set<String> getAllVarsSet_except_file_and_weight(){
	
	Set<String> out = new HashSet<String>();
	
	out.addAll(this.asSet);
	out.addAll(this.dvSet);
	out.addAll(this.svSet);
	out.addAll(this.gSet);
	out.addAll(this.exSet);

    return out;
  }

public Set<String> getSvarAliasExternTimeseries(){
	
	Set<String> out = new HashSet<String>();
	
	out.addAll(this.asSet);
	out.addAll(this.svSet);
	out.addAll(this.tsSet);
	out.addAll(this.exSet);

    return out;
  }

public SimulationDataSet remove_set(Set<String> s)
  {

//      this.wtSet.removeAll(s);
//      this.wtSet_global.removeAll(s);
//      this.wtSet_local.removeAll(s);
//
//      Tools.mapRemoveAll(this.wtMap, s);
    

      this.incFileSet.removeAll(s);
      this.incFileSet_global.removeAll(s);
      this.incFileSet_local.removeAll(s);

      Tools.mapRemoveAll(this.incFileMap, s);
      

      this.exSet.removeAll(s);
      this.exSet_global.removeAll(s);
      this.exSet_local.removeAll(s);

      Tools.mapRemoveAll(this.exMap, s);


      this.tsSet.removeAll(s);
      this.tsSet_global.removeAll(s);
      this.tsSet_local.removeAll(s);

      Tools.mapRemoveAll(this.tsMap, s);


      this.svSet.removeAll(s);
      this.svSet_global.removeAll(s);
      this.svSet_local.removeAll(s);

      Tools.mapRemoveAll(this.svMap, s);


      this.dvSet.removeAll(s);
      this.dvSet_global.removeAll(s);
      this.dvSet_local.removeAll(s);

      Tools.mapRemoveAll(this.dvMap, s);


      this.asSet.removeAll(s);
      this.asSet_global.removeAll(s);
      this.asSet_local.removeAll(s);

      Tools.mapRemoveAll(this.asMap, s);


      this.gSet.removeAll(s);
      this.gSet_global.removeAll(s);
      this.gSet_local.removeAll(s);

      Tools.mapRemoveAll(this.gMap, s);

      
      this.varNeedEarlierCycle.removeAll(s);

      Tools.mapRemoveAll(this.var_neededVarCycle_map, s);
      

    return this;
  }

public SimulationDataSet overwrittenWith_set(SimulationDataSet s) {
	 
	//this.ordered_list_including_files.addAll(s.ordered_list_including_files);  
	//this.ordered_list.addAll(s.ordered_list);  
	  
    this.dvSet.addAll(s.dvSet);
    this.dvSet_global.addAll(s.dvSet_global);
    this.dvSet_local.addAll(s.dvSet_local);
    this.dvMap.putAll(s.dvMap);

	this.svSet.addAll(s.svSet);
    this.svSet_global.addAll(s.svSet_global);
    this.svSet_local.addAll(s.svSet_local);
    
//    /// experiment with deep copy
//    Map<String, Svar> t = new HashMap<String, Svar>();
//    for (Map.Entry<String, Svar> e : s.svMap.entrySet()){
//    	t.put(e.getKey(), new Svar(e.getValue()));
//    }
    
    this.svMap.putAll(s.svMap);

    this.tsSet.addAll(s.tsSet);
    this.tsSet_global.addAll(s.tsSet_global);
    this.tsSet_local.addAll(s.tsSet_local);
    this.tsMap.putAll(s.tsMap);

    this.asSet.addAll(s.asSet);
    this.asSet_global.addAll(s.asSet_global);
    this.asSet_local.addAll(s.asSet_local);
    this.asMap.putAll(s.asMap);

    this.gSet.addAll(s.gSet);
    this.gSet_global.addAll(s.gSet_global);
    this.gSet_local.addAll(s.gSet_local);
    this.gMap.putAll(s.gMap);

    this.wtSet.addAll(s.wtSet);
    this.wtSet_global.addAll(s.wtSet_global);
    this.wtSet_local.addAll(s.wtSet_local);
    this.wtMap.putAll(s.wtMap);

    this.exSet.addAll(s.exSet);
    this.exSet_global.addAll(s.exSet_global);
    this.exSet_local.addAll(s.exSet_local);
    this.exMap.putAll(s.exMap);

    this.incFileSet.addAll(s.incFileSet);
    this.incFileSet_global.addAll(s.incFileSet_global);
    this.incFileSet_local.addAll(s.incFileSet_local);
    this.incFileMap.putAll(s.incFileMap);

    this.varNeedEarlierCycle.addAll(s.varNeedEarlierCycle);
    this.var_neededVarCycle_map.putAll(s.var_neededVarCycle_map);
    
    this.lousyConvert();
    
    return this;
  }

public void lousyConvert() {

	this.asList = new ArrayList<String>(this.asSet);
	this.asList_global = new ArrayList<String>(this.asSet_global);
	this.asList_local = new ArrayList<String>(this.asSet_local);

	this.wtList = new ArrayList<String>(this.wtSet);
	this.wtList_global = new ArrayList<String>(this.wtSet_global);
	this.wtList_local = new ArrayList<String>(this.wtSet_local);

	this.svList = new ArrayList<String>(this.svSet);
	this.svList_global = new ArrayList<String>(this.svSet_global);
	this.svList_local = new ArrayList<String>(this.svSet_local);

	this.dvList = new ArrayList<String>(this.dvSet);
	this.dvList_global = new ArrayList<String>(this.dvSet_global);
	this.dvList_local = new ArrayList<String>(this.dvSet_local);

	this.tsList = new ArrayList<String>(this.tsSet);
	this.tsList_global = new ArrayList<String>(this.tsSet_global);
	this.tsList_local = new ArrayList<String>(this.tsSet_local);

	this.exList = new ArrayList<String>(this.exSet);
	this.exList_global = new ArrayList<String>(this.exSet_global);
	this.exList_local = new ArrayList<String>(this.exSet_local);

	this.gList = new ArrayList<String>(this.gSet);
	this.gList_global = new ArrayList<String>(this.gSet_global);
	this.gList_local = new ArrayList<String>(this.gSet_local);

	this.incFileList = new ArrayList<String>(this.incFileSet);
	this.incFileList_global = new ArrayList<String>(this.incFileSet_global);
	this.incFileList_local = new ArrayList<String>(this.incFileSet_local);

}
}