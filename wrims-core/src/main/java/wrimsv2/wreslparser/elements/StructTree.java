package wrimsv2.wreslparser.elements;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import wrimsv2.commondata.wresldata.Alias;
import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.External;
import wrimsv2.commondata.wresldata.Goal;
import wrimsv2.commondata.wresldata.Param;
import wrimsv2.commondata.wresldata.Svar;
import wrimsv2.commondata.wresldata.Timeseries;
import wrimsv2.commondata.wresldata.WeightElement;

public class StructTree
{
  public SimulationDataSet S;
  public WeightElement wt;
  public Sequence seq;
  public IncludeFile incFile;
  public External ex;
  public Timeseries svTs;
  public Svar sv;
  public Dvar dv;
  public Alias as;
  public Goal gl;
  private static String[] keys = { "define", "goal" };
  private static String[] mons = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", 
    "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };

  public static List<String> r_keys = Arrays.asList(keys);
  public static List<String> r_mons = Arrays.asList(mons);

  public void modelList(String name)
  {
    name = name.toLowerCase();

    if (this.S.model_list.contains(name)) {
      LogUtils.errMsg("Model redefined: " + name, this.S.currentAbsolutePath);
      this.S.error_model_redefined.add(name); 
    }
    else {
      this.S.model_list.add(name);
      this.S.var_all.put(name, "model");
    }
  }

  public void mergeWeightTable(String name, String value, String scope){
	  mergeWeightTable(name, value, scope, "0");
  }
  
  public void mergeWeightTable(String name, String value, String scope, String timeArraySizeStr){
	  
	// TODO: weights redefined in different files are not checked within the same model
	  
    name = name.toLowerCase();
    value = value.toLowerCase();
    timeArraySizeStr=timeArraySizeStr.toLowerCase();

    if (this.S.wtList.contains(name)) {
      LogUtils.errMsg("Weight redefined: " + name, this.S.currentAbsolutePath);
      this.S.error_weightVar_redefined.put(name, this.S.currentAbsolutePath); 
      //this.S.wtList.remove(name); this.S.wtList_global.remove(name); this.S.wtList_local.remove(name);
    }

    this.wt = new WeightElement();
    this.wt.weight = value;
    this.wt.fromWresl = this.S.currentAbsolutePath;
    this.wt.timeArraySize=timeArraySizeStr;
    this.S.wtMap.put(name, this.wt);

    this.S.wtList.add(name);
    this.S.wtSet.add(name);

    if (scope == null) {
      this.S.wtList_global.add(name); this.S.wtSet_global.add(name);} 
    else if (scope.equalsIgnoreCase(Param.local)) {
      this.S.wtList_local.add(name); this.S.wtSet_local.add(name); } 
    else {
      LogUtils.errMsg("Weight table scope undefined: " + name, this.S.currentAbsolutePath);
    }
    
  }

  public void includeFile(String fileRelativePath, String scope)
  {
    fileRelativePath = fileRelativePath.toLowerCase();
    fileRelativePath = Tools.replace_pathseperator(fileRelativePath);

    File absIncludeFile = new File(this.S.currentAbsoluteParent, fileRelativePath).getAbsoluteFile();
    String absIncludeFilePath = "Error with include file: " + fileRelativePath;
    try {
      absIncludeFilePath = absIncludeFile.getCanonicalPath().toLowerCase();
    }
    catch (IOException e)
    {
      e.printStackTrace();
      LogUtils.errMsg("Include file IOException: " + fileRelativePath, this.S.currentAbsolutePath);
    }

    if (this.S.var_all.containsKey(absIncludeFilePath)) {
      LogUtils.errMsg("Include file redefined: " + fileRelativePath, this.S.currentAbsolutePath);
      this.S.error_var_redefined.put(absIncludeFilePath, "file"); 
      //this.S.incFileList.remove(absIncludeFilePath); this.S.incFileList_global.remove(absIncludeFilePath); this.S.incFileList_local.remove(absIncludeFilePath);
    }

    this.S.ordered_list_including_files.add(absIncludeFilePath);

    this.S.var_all.put(absIncludeFilePath, "file");

    this.incFile = new IncludeFile();

    this.incFile.fromWresl = this.S.currentAbsolutePath;

    this.S.incFileMap.put(absIncludeFilePath, this.incFile);
    this.S.incFileList.add(absIncludeFilePath);

    this.S.incFileSet.add(absIncludeFilePath);

    if (scope == null) {
      this.S.incFileList_global.add(absIncludeFilePath);
      this.S.incFileSet_global.add(absIncludeFilePath);
      this.incFile.scope = Param.global;

      this.S.ordered_list_including_files_global.add(absIncludeFilePath);
    } 
    else if (scope.equalsIgnoreCase(Param.local)) {
      this.S.incFileList_local.add(absIncludeFilePath);
      this.S.incFileSet_local.add(absIncludeFilePath);
      this.incFile.scope = Param.local; } 
    else {
      LogUtils.errMsg("Include file scope undefined: " + fileRelativePath, this.S.currentAbsolutePath);
    }
  }

  public Goal goalSimple(String name, String scope, String expression, String dependants, String varInCycle){
	  return goalSimple(name, scope, expression, dependants, varInCycle, "0");
  }
  
  public Goal goalSimple(String name, String scope, String expression, String dependants, String varInCycle, String timeArraySizeStr)
  {
	timeArraySizeStr=timeArraySizeStr.toLowerCase();
	name = name.toLowerCase();
    expression = expression.toLowerCase();
    if (dependants != null) dependants = dependants.toLowerCase();
    if (varInCycle != null) varInCycle = varInCycle.toLowerCase();

    if (this.S.var_all.containsKey(name)) {
      LogUtils.errMsg("Goal redefined: " + name, this.S.currentAbsolutePath);
      this.S.error_var_redefined.put(name, "goal"); 
     // this.S.gList.remove(name); this.S.gList_global.remove(name); this.S.gList_local.remove(name);
    }

    this.S.ordered_list.add(name);
    this.S.ordered_list_including_files.add(name);

    this.S.var_all.put(name, "goal");

    this.gl = new Goal();
    this.gl.caseCondition.add(Param.always);
    this.gl.caseName.add(Param.defaultCaseName);
    this.gl.caseExpression.add(expression);
    this.gl.fromWresl = this.S.currentAbsolutePath;
    if (dependants != null) {
    	this.gl.expressionDependants.addAll(Tools.convertStrToSet(dependants));
    	this.gl.expressionDependants.removeAll(Param.reservedSet);
    }
    if (varInCycle != null) { 
    	this.gl.neededVarInCycleSet = Tools.convertStrToSet(varInCycle);
    	this.gl.needVarFromEarlierCycle = true;
    }
    this.gl.timeArraySize=timeArraySizeStr;
    
    
    this.S.gMap.put(name, this.gl);
    this.S.gList.add(name);
    this.S.gSet.add(name);

    if (scope == null) {
      this.S.gList_global.add(name); this.S.gSet_global.add(name); this.gl.scope = Param.global;
      this.S.ordered_list_global.add(name);
      this.S.ordered_list_including_files_global.add(name);
    } else if (scope.equalsIgnoreCase(Param.local)) {
      this.S.gList_local.add(name); this.S.gSet_local.add(name); this.gl.scope = Param.local; } else {
      LogUtils.errMsg("Goal scope undefined: " + name, this.S.currentAbsolutePath);
    }
	return gl;
  }

  public void goalCase(String name, String scope, Goal gl){
	  goalCase(name, scope, gl, "0");
  }
  
  public void goalCase(String name, String scope, Goal gl, String timeArraySizeStr)
  {
    name = name.toLowerCase();
    timeArraySizeStr=timeArraySizeStr.toLowerCase();
    
    if (this.S.var_all.containsKey(name)) {
      LogUtils.errMsg("Goal redefined: " + name, this.S.currentAbsolutePath);
      this.S.error_var_redefined.put(name, "goal"); 
     // this.S.gList.remove(name); this.S.gList_global.remove(name); this.S.gList_local.remove(name);
    }

    this.S.ordered_list.add(name);
    this.S.ordered_list_including_files.add(name);

    this.S.var_all.put(name, "goal");

    gl.fromWresl = this.S.currentAbsolutePath;

    this.S.gMap.put(name, gl);
    this.S.gList.add(name);
    this.S.gSet.add(name);


    gl.timeArraySize=timeArraySizeStr;
    if (scope == null) {
      this.S.gList_global.add(name); this.S.gSet_global.add(name); gl.scope = Param.global;
      this.S.ordered_list_global.add(name);
      this.S.ordered_list_including_files_global.add(name);
    } else if (scope.equalsIgnoreCase(Param.local)) {
      this.S.gList_local.add(name); this.S.gSet_local.add(name); gl.scope = Param.local; } else {
      LogUtils.errMsg("Goal scope undefined: " + name, this.S.currentAbsolutePath);
    }
  }

  public void alias(String name, String scope, String kind, String units, String expression, String dependants, String varInCycle){
	  alias(name, scope, kind, units, expression, dependants, varInCycle,  "0");
  }
  
  public void alias(String name, String scope, String kind, String units, String expression, String dependants, String varInCycle,  String timeArraySizeStr)
  {
    name = name.toLowerCase();
    if (kind != null) kind = kind.toLowerCase();
    if (units != null) units = units.toLowerCase();
    expression = expression.toLowerCase();
    if (dependants != null) dependants = dependants.toLowerCase();
    if (varInCycle != null) varInCycle = varInCycle.toLowerCase();
    timeArraySizeStr=timeArraySizeStr.toLowerCase();

    if (this.S.var_all.containsKey(name)) {
      LogUtils.errMsg("Alias redefined: " + name, this.S.currentAbsolutePath);
      this.S.error_var_redefined.put(name, "alias"); 
      //this.S.asList.remove(name); this.S.asList_global.remove(name); this.S.asList_local.remove(name);
    }

    this.S.ordered_list.add(name);
    this.S.ordered_list_including_files.add(name);

    this.S.var_all.put(name, "alias");

    this.as = new Alias();

    if (kind != null)  this.as.kind = kind;
    if (units != null) this.as.units = units;
    this.as.expression = expression;
    this.as.fromWresl = this.S.currentAbsolutePath;
    this.as.timeArraySize=timeArraySizeStr;
    if (dependants != null) this.as.dependants = Tools.convertStrToSet(dependants);
    if (varInCycle != null) { 
    	this.as.neededVarInCycleSet = Tools.convertStrToSet(varInCycle);
    	this.as.needVarFromEarlierCycle = true;
    }

    this.S.asMap.put(name, this.as);
    this.S.asList.add(name);
    this.S.asSet.add(name);

    if (scope == null) {
      this.S.asList_global.add(name); this.S.asSet_global.add(name); this.as.scope = Param.global;
      this.S.ordered_list_global.add(name);
      this.S.ordered_list_including_files_global.add(name);
    } else if (scope.toLowerCase().equals(Param.local)) {
      this.S.asList_local.add(name); this.S.asSet_local.add(name); this.as.scope = Param.local; } else {
      LogUtils.errMsg("Alias scope undefined: " + name, this.S.currentAbsolutePath);
    }
  }

  public void svarCase(String name, String scope, Svar sv, String dependants, String varInCycle)  
  {
	  svarCase(name, scope, sv, dependants, varInCycle, "0"); 
  }
  
  public void svarCase(String name, String scope, Svar sv, String dependants, String varInCycle, String timeArraySizeStr)
  {
	timeArraySizeStr = timeArraySizeStr.toLowerCase();  
    name = name.toLowerCase();
    if (dependants != null) dependants = dependants.toLowerCase();
    if (varInCycle != null) varInCycle = varInCycle.toLowerCase();
    
    if (this.S.var_all.containsKey(name)) {
      LogUtils.errMsg("Svar redefined: " + name, this.S.currentAbsolutePath);
      this.S.error_var_redefined.put(name, "svar"); 
      //this.S.svList.remove(name); this.S.svList_global.remove(name); this.S.svList_local.remove(name);
    }

    this.S.ordered_list.add(name);
    this.S.ordered_list_including_files.add(name);

    this.S.var_all.put(name, "svar");

    sv.fromWresl = this.S.currentAbsolutePath;
    sv.timeArraySize = timeArraySizeStr;

    if (dependants != null) sv.dependants = Tools.convertStrToSet(dependants);
    if (varInCycle != null) { 
    	sv.neededVarInCycleSet = Tools.convertStrToSet(varInCycle);
    	sv.needVarFromEarlierCycle = true;
    }

    this.S.svMap.put(name, sv);
    this.S.svList.add(name);
    this.S.svSet.add(name);

    if (scope == null) {
      this.S.svList_global.add(name); this.S.svSet_global.add(name); sv.scope = Param.global;
      this.S.ordered_list_global.add(name);
      this.S.ordered_list_including_files_global.add(name);
    } else if (scope.equalsIgnoreCase(Param.local)) {
      this.S.svList_local.add(name); this.S.svSet_local.add(name); sv.scope = Param.local; } else {
      LogUtils.errMsg("Svar scope undefined: " + name, this.S.currentAbsolutePath);
    }
  }

  public void external(String name, String scope, String externalType)
  {
    name = name.toLowerCase();
    externalType = externalType.toLowerCase();

    if (this.S.var_all.containsKey(name)) {
      LogUtils.errMsg("External variable redefined: " + name, this.S.currentAbsolutePath);
      this.S.error_var_redefined.put(name, "external"); 
      //this.S.exList.remove(name); this.S.exList_global.remove(name); this.S.exList_local.remove(name);
    }

    this.S.ordered_list.add(name);
    this.S.ordered_list_including_files.add(name);
    
    this.S.var_all.put(name, "external");

    this.ex = new External();
    this.ex.type = externalType;
    this.ex.fromWresl = this.S.currentAbsolutePath;

    this.S.exMap.put(name, this.ex);
    this.S.exList.add(name);
    this.S.exSet.add(name);

    if (scope == null) {
      this.S.exList_global.add(name); this.S.exSet_global.add(name); this.ex.scope = Param.global;
      this.S.ordered_list_global.add(name);
      this.S.ordered_list_including_files_global.add(name);
    }
    else if (scope.equalsIgnoreCase(Param.local)) {
      this.S.exList_local.add(name); this.S.exSet_local.add(name); this.ex.scope = Param.local; } else {
      LogUtils.errMsg("External scope undefined: " + name, this.S.currentAbsolutePath);
    }
  }

  public void svarExpression(String name, String scope, String expression, String dependants, String varInCycle)
  {
	  svarExpression(name, scope, expression, dependants, varInCycle, "0");
  }
  
  public void svarExpression(String name, String scope, String expression, String dependants, String varInCycle, String timeArraySizeStr)
  {
	timeArraySizeStr = timeArraySizeStr.toLowerCase();
    name = name.toLowerCase();
    expression = expression.toLowerCase();
    if (dependants != null) dependants = dependants.toLowerCase();
    if (varInCycle != null) varInCycle = varInCycle.toLowerCase();
    
    if (this.S.var_all.containsKey(name)) {
      LogUtils.errMsg("State variable redefined: " + name, this.S.currentAbsolutePath);
      this.S.error_var_redefined.put(name, "svar"); 
      //this.S.svList.remove(name); this.S.svList_global.remove(name); this.S.svList_local.remove(name);
    }

    this.S.ordered_list.add(name);
    this.S.ordered_list_including_files.add(name);

    this.S.var_all.put(name, "svar");

    String caseName = Param.defaultCaseName;
    String condition = Param.always;

    this.sv = new Svar();

    this.sv.caseName.add(caseName);
    this.sv.caseCondition.add(condition);
    this.sv.caseExpression.add(expression);
    this.sv.fromWresl = this.S.currentAbsolutePath;
    this.sv.timeArraySize = timeArraySizeStr;

    if (dependants != null) this.sv.dependants = Tools.convertStrToSet(dependants);
    if (varInCycle != null) { 
    	this.sv.neededVarInCycleSet = Tools.convertStrToSet(varInCycle);
    	this.sv.needVarFromEarlierCycle = true;
    }
    
    this.S.svMap.put(name, this.sv);
    this.S.svList.add(name);
    this.S.svSet.add(name);

    if (scope == null) {
      this.S.svList_global.add(name); this.S.svSet_global.add(name); this.sv.scope = Param.global;
      this.S.ordered_list_global.add(name);
      this.S.ordered_list_including_files_global.add(name);
    } else if (scope.equalsIgnoreCase(Param.local)) {
      this.S.svList_local.add(name); this.S.svSet_local.add(name); this.sv.scope = Param.local; } else {
      LogUtils.errMsg("Svar scope undefined: " + name, this.S.currentAbsolutePath);
    }
  }

  public void svarSum(String name, String scope, String hdr, String expression, String dependants, String varInCycle)
  {
	  svarSum(name, scope, hdr, expression, dependants, varInCycle, "0");
  }
  
  public void svarSum(String name, String scope, String hdr, String expression, String dependants, String varInCycle, String timeArraySizeStr)
  {
    name = name.toLowerCase();
    hdr = hdr.toLowerCase();
    expression = expression.toLowerCase();
    if (dependants != null) dependants = dependants.toLowerCase();
    if (varInCycle != null) varInCycle = varInCycle.toLowerCase();
    timeArraySizeStr=timeArraySizeStr.toLowerCase();
    
    if (this.S.var_all.containsKey(name)) {
      LogUtils.errMsg("State variable redefined: " + name, this.S.currentAbsolutePath);
      this.S.error_var_redefined.put(name, "svar"); 
      //this.S.svList.remove(name); this.S.svList_global.remove(name); this.S.svList_local.remove(name);
    }

    this.S.ordered_list.add(name);
    this.S.ordered_list_including_files.add(name);

    this.S.var_all.put(name, "svar");

    this.sv = new Svar();

    this.sv.caseName.add(Param.defaultCaseName);
    this.sv.caseCondition.add("always");
    this.sv.caseExpression.add(hdr + " " + expression);
    this.sv.fromWresl = this.S.currentAbsolutePath;
    if (dependants != null) this.sv.dependants = Tools.convertStrToSet(dependants);
    if (varInCycle != null) { 
    	this.sv.neededVarInCycleSet = Tools.convertStrToSet(varInCycle);
    	this.sv.needVarFromEarlierCycle = true;
    }
    this.sv.timeArraySize=timeArraySizeStr;
    
    this.S.svMap.put(name, this.sv);
    this.S.svList.add(name);
    this.S.svSet.add(name);

    if (scope == null) {
      this.S.svList_global.add(name); this.S.svSet_global.add(name); this.sv.scope = Param.global;
      this.S.ordered_list_global.add(name);
      this.S.ordered_list_including_files_global.add(name);
    } else if (scope.equalsIgnoreCase(Param.local)) {
      this.S.svList_local.add(name); this.S.svSet_local.add(name); this.sv.scope = Param.local; } else {
      LogUtils.errMsg("Svar scope undefined: " + name, this.S.currentAbsolutePath);
    }
  }

  public void svarTable(String name, String scope, String sqlStr, String dependants, String varInCycle){
	  svarTable(name, scope, sqlStr, dependants, varInCycle, "0");
  }
  
  public void svarTable(String name, String scope, String sqlStr, String dependants, String varInCycle, String timeArraySizeStr)
  {
    name = name.toLowerCase();
    sqlStr = sqlStr.toLowerCase();
    timeArraySizeStr=timeArraySizeStr.toLowerCase();
    
    if (dependants != null) dependants = dependants.toLowerCase();
    if (varInCycle != null) varInCycle = varInCycle.toLowerCase();

    if (this.S.var_all.containsKey(name)) {
      LogUtils.errMsg("State variable redefined: " + name, this.S.currentAbsolutePath);
      this.S.error_var_redefined.put(name, "svar"); 
      //this.S.svList.remove(name); this.S.svList_global.remove(name); this.S.svList_local.remove(name);
    }

    this.S.ordered_list.add(name);
    this.S.ordered_list_including_files.add(name);

    this.S.var_all.put(name, "svar");

    this.sv = new Svar();

    this.sv.format = "lookup_table";
    this.sv.caseCondition.add("always");
    this.sv.caseName.add(Param.defaultCaseName);
    this.sv.caseExpression.add(sqlStr);
    this.sv.fromWresl = this.S.currentAbsolutePath;
    if (dependants != null) this.sv.dependants = Tools.convertStrToSet(dependants);
    if (varInCycle != null) { 
    	this.sv.neededVarInCycleSet = Tools.convertStrToSet(varInCycle);
    	this.sv.needVarFromEarlierCycle = true;
    }
    this.sv.timeArraySize=timeArraySizeStr;
    
    this.S.svMap.put(name, this.sv);
    this.S.svList.add(name);
    this.S.svSet.add(name);

    if (scope == null) {
      this.S.svList_global.add(name); this.S.svSet_global.add(name); this.sv.scope = Param.global;
      this.S.ordered_list_global.add(name);
      this.S.ordered_list_including_files_global.add(name);
    } else if (scope.equalsIgnoreCase(Param.local)) {
      this.S.svList_local.add(name); this.S.svSet_local.add(name); this.sv.scope = Param.local; } else {
      LogUtils.errMsg("Svar scope undefined: " + name, this.S.currentAbsolutePath);
    }
  }

  public void timeseriesDss(String name, String scope, String b_part, String kind, String units, String convertToUnits)
  {
    name = name.toLowerCase();
    if (b_part != null) b_part = b_part.toLowerCase();
    kind = kind.toLowerCase();
    units = units.toLowerCase();
    if (convertToUnits != null) convertToUnits = convertToUnits.toLowerCase();

    if (this.S.var_all.containsKey(name)) {
      LogUtils.errMsg("State variable redefined: " + name, this.S.currentAbsolutePath);
      this.S.error_var_redefined.put(name, "timeseries"); 
      //this.S.tsList.remove(name); this.S.tsList_global.remove(name); this.S.tsList_local.remove(name);
    }

    this.S.var_all.put(name, "timeseries");

    Timeseries ts = new Timeseries();

    ts.dssBPart = name;
    if (b_part != null) ts.dssBPart = b_part;
    ts.kind = kind;
    ts.units = units;
    if (convertToUnits != null) ts.convertToUnits = convertToUnits;
    ts.format = "dss";
    ts.fromWresl = this.S.currentAbsolutePath;
    this.S.tsMap.put(name, ts);
    this.S.tsList.add(name);
    this.S.tsSet.add(name);

    if (scope == null) {
      this.S.tsList_global.add(name); this.S.tsSet_global.add(name); ts.scope = Param.global;
    }
    else if (scope.equalsIgnoreCase(Param.local)) {
      this.S.tsList_local.add(name); this.S.tsSet_local.add(name); ts.scope = Param.local; } else {
      LogUtils.errMsg("Timeseries scope undefined: " + name, this.S.currentAbsolutePath);
    }
  }

  public void dvarStd(String name, String scope, String integer, String kind, String units)
  {
	  dvarStd(name, scope, integer, kind, units, "0");
  }

  public void dvarStd(String name, String scope, String integer, String kind, String units, String timeArraySizeStr)
  {
    name = name.toLowerCase();
    timeArraySizeStr = timeArraySizeStr.toLowerCase();
    if (kind.length()>0) kind = kind.toLowerCase();
    if (units.length()>0) units = units.toLowerCase();

    if (this.S.var_all.containsKey(name)) {
      LogUtils.errMsg("Dvar redefined: " + name, "\n" + this.S.currentAbsolutePath);
      this.S.error_var_redefined.put(name, "dvar"); 
     // this.S.dvList.remove(name); this.S.dvList_global.remove(name); this.S.dvList_local.remove(name);
    }

    this.S.ordered_list.add(name);
    this.S.ordered_list_including_files.add(name);

    this.S.var_all.put(name, "dvar");

    this.S.dvSet.add(name);

    this.dv = new Dvar();
    if (kind.length()>0)  this.dv.kind = kind;
    if (units.length()>0) this.dv.units = units;
    this.dv.lowerBound = Param.dv_std_lowerBound;
    this.dv.upperBound = Param.dv_std_upperBound;
    this.dv.fromWresl = this.S.currentAbsolutePath;
    this.dv.timeArraySize = timeArraySizeStr;

    if (integer != null)
    {
      this.dv.integer = Param.yes;
      this.dv.lowerBound = Param.dv_std_integer_lowerBound;
      this.dv.upperBound = Param.dv_std_integer_upperBound;
    }

    this.S.dvMap.put(name, this.dv);
    this.S.dvList.add(name);
    this.S.dvSet.add(name);

    if (scope == null) {
      this.S.dvList_global.add(name); this.S.dvSet_global.add(name); this.dv.scope = Param.global;
      this.S.ordered_list_global.add(name);
      this.S.ordered_list_including_files_global.add(name);
    } else if (scope.equalsIgnoreCase(Param.local)) {
      this.S.dvList_local.add(name); this.S.dvSet_local.add(name); this.dv.scope = Param.local; } else {
      LogUtils.errMsg("Dvar scope undefined: " + name, this.S.currentAbsolutePath);
    }
  }

  public void dvarNonStd(String name, String scope, String integer, String kind, String units, String lowerBound, String upperBound)
  {
	dvarNonStd(name, scope, integer, kind, units, lowerBound, upperBound, "0");
  }

  public void dvarNonStd(String name, String scope, String integer, String kind, String units, String lowerBound, String upperBound, String timeArraySizeStr)
  {
    name = name.toLowerCase();
    timeArraySizeStr = timeArraySizeStr.toLowerCase();
    if (kind != null) kind = kind.toLowerCase();
    if (units != null) units = units.toLowerCase();
    lowerBound = lowerBound.toLowerCase();
    upperBound = upperBound.toLowerCase();

    if (this.S.var_all.containsKey(name)) {
      LogUtils.errMsg("Dvar redefined: " + name, this.S.currentAbsolutePath);
      this.S.error_var_redefined.put(name, "dvar"); 
     // this.S.dvList.remove(name); this.S.dvList_global.remove(name); this.S.dvList_local.remove(name);
    }

    this.S.ordered_list.add(name);
    this.S.ordered_list_including_files.add(name);

    this.S.var_all.put(name, "dvar");

    this.dv = new Dvar();

    if (kind != null)  this.dv.kind = kind;
    if (units != null) this.dv.units = units;
    this.dv.lowerBound = lowerBound;
    this.dv.upperBound = upperBound;
    this.dv.fromWresl = this.S.currentAbsolutePath;
    this.dv.timeArraySize = timeArraySizeStr;
    
    if (integer != null){
      this.dv.integer = Param.yes;
    }
      
    this.S.dvMap.put(name, this.dv);
    this.S.dvList.add(name);
    this.S.dvSet.add(name);

    if (scope == null) {
      this.S.dvList_global.add(name); this.S.dvSet_global.add(name); this.dv.scope = Param.global;
      this.S.ordered_list_global.add(name);
      this.S.ordered_list_including_files_global.add(name);
    } else if (scope.equalsIgnoreCase(Param.local)) {
      this.S.dvList_local.add(name); this.S.dvSet_local.add(name); this.dv.scope = Param.local; } else {
      LogUtils.errMsg("Dvar scope undefined: " + name, this.S.currentAbsolutePath);
    }
  }

public void sequenceOrder(String sequenceName, String order, String modelName, String condition, String timeStep)
  {
    sequenceName = sequenceName.toLowerCase();
    order = order.toLowerCase();
    modelName = modelName.toLowerCase();
    if (condition != null) condition = condition.toLowerCase();

    Integer i = Integer.valueOf(Integer.parseInt(order));

    if (this.S.seqList.contains(sequenceName)) {
      this.S.error_sequence_redefined.add(sequenceName); 
    } else if (this.S.seqMap.containsKey(i)) {
      this.S.error_sequence_order_redefined.add(i); 
    }
    else {
      this.seq = new Sequence();
      this.seq.sequenceName = sequenceName;
      this.seq.condition = Param.always;
      if (condition != null) this.seq.condition = condition;
      this.seq.modelName = modelName;
      this.seq.fromWresl = this.S.currentAbsolutePath;
      this.seq.timeStep=timeStep;
      this.S.seqMap.put(i, this.seq);
      this.S.seqList.add(sequenceName);
    }
  }

public void dvarSlackSurplus(String name, String scope, String kind, String units, String conditionStr)
  {
	dvarSlackSurplus(name, scope, kind, units, conditionStr, "0");
  }

public void dvarSlackSurplus(String name, String scope, String kind, String units, String conditionStr,  String timeArraySizeStr)
  {
    name = name.toLowerCase();
    timeArraySizeStr=timeArraySizeStr.toLowerCase();
    if (kind.length()>0) kind = kind.toLowerCase();
    if (units.length()>0) units = units.toLowerCase();

    if (this.S.var_all.containsKey(name)) {
      LogUtils.errMsg("Dvar redefined: " + name, "\n" + this.S.currentAbsolutePath);
      this.S.error_var_redefined.put(name, "dvar"); 
     // this.S.dvList.remove(name); this.S.dvList_global.remove(name); this.S.dvList_local.remove(name);
    }

    this.S.ordered_list.add(name);
    this.S.ordered_list_including_files.add(name);

    this.S.var_all.put(name, "dvar");

    this.dv = new Dvar();
    if (kind.length()>0)  this.dv.kind = kind;
    if (units.length()>0) this.dv.units = units;
    this.dv.lowerBound = Param.dv_std_lowerBound;
    this.dv.upperBound = Param.dv_std_upperBound;
    this.dv.fromWresl = this.S.currentAbsolutePath;
    this.dv.condition = conditionStr;
    this.dv.timeArraySize=timeArraySizeStr;


    this.S.dvMap.put(name, this.dv);
    this.S.dvList.add(name);
    this.S.dvSet.add(name);

    if (scope == null) {
      this.S.dvList_global.add(name); this.S.dvSet_global.add(name); this.dv.scope = Param.global;
      this.S.ordered_list_global.add(name);
      this.S.ordered_list_including_files_global.add(name);
    } else if (scope.equalsIgnoreCase(Param.local)) {
      this.S.dvList_local.add(name); this.S.dvSet_local.add(name); this.dv.scope = Param.local; } else {
      LogUtils.errMsg("Dvar (Slack or Surplus) scope undefined: " + name, this.S.currentAbsolutePath);
    }
  }

public void mergeSlackSurplusIntoWeightTable(String name, String value, String scope, String conditionStr, String timeArraySizeStr){
	  
	// TODO: weights redefined in different files are not checked within the same model
	  
    name = name.toLowerCase();
    value = value.toLowerCase();
    timeArraySizeStr = timeArraySizeStr.toLowerCase();

    if (this.S.wtList.contains(name)) {
      LogUtils.errMsg("Weight redefined: " + name, this.S.currentAbsolutePath);
      this.S.error_weightVar_redefined.put(name, this.S.currentAbsolutePath); 
      //this.S.wtList.remove(name); this.S.wtList_global.remove(name); this.S.wtList_local.remove(name);
    }

    this.wt = new WeightElement();
    this.wt.weight = value;
    this.wt.fromWresl = this.S.currentAbsolutePath;
    this.wt.condition = conditionStr;
    this.wt.timeArraySize=timeArraySizeStr;
    this.S.wtMap.put(name, this.wt);

    this.S.wtList.add(name);
    this.S.wtSet.add(name);

    if (scope == null) {
      this.S.wtList_global.add(name); this.S.wtSet_global.add(name);} 
    else if (scope.equalsIgnoreCase(Param.local)) {
      this.S.wtList_local.add(name); this.S.wtSet_local.add(name); } 
    else {
      LogUtils.errMsg("Weight table (Slack or Surplus) scope undefined: " + name, this.S.currentAbsolutePath);
    }
    
  }
}