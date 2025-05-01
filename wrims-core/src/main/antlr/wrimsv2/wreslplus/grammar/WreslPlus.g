grammar WreslPlus;
//import CommonLexer;

options {
  language = Java;
  output=AST;
  ASTLabelType=CommonTree;
}

@header {
  package wrimsv2.wreslplus.grammar;
  import wrimsv2.wreslparser.elements.LogUtils; 
  import java.util.HashMap;
  import java.util.LinkedHashMap;
  import java.util.Set;
  import java.util.HashSet;
  import java.util.LinkedHashSet;
  import wrimsv2.wreslplus.elements.Tools;
  import wrimsv2.wreslplus.elements.IncFileTemp;
  import wrimsv2.wreslplus.elements.IfIncItemGroup;
  import wrimsv2.wreslplus.elements.TimeseriesTemp;
  import wrimsv2.wreslplus.elements.ExternalTemp;
  import wrimsv2.wreslplus.elements.DvarTemp;
  import wrimsv2.wreslplus.elements.SvarTemp;
  import wrimsv2.wreslplus.elements.ParamTemp;
  import wrimsv2.wreslplus.elements.WeightTable;
  import wrimsv2.wreslplus.elements.WeightSubgroup;
  import wrimsv2.wreslplus.elements.AliasTemp;
  import wrimsv2.wreslplus.elements.GoalTemp;
  import wrimsv2.wreslplus.elements.GoalHS;
  import wrimsv2.wreslplus.elements.GoalCase;
  import wrimsv2.wreslplus.elements.ModelTemp;
  import wrimsv2.wreslplus.elements.SequenceTemp;
  import wrimsv2.wreslplus.elements.StudyTemp;
  import wrimsv2.wreslplus.elements.VarCycleIndex;
  import wrimsv2.commondata.wresldata.Param;
}
@lexer::header {
  package wrimsv2.wreslplus.grammar;
  import wrimsv2.wreslparser.elements.LogUtils; 
}

@members {
	public CommonTree commonTree;
	public String currentAbsolutePath;
  	public String currentAbsoluteParent;
  	public String pathRelativeToRunDir;
  	public StudyTemp styObj;
  	public ModelTemp mObj;
  	public Set<String> dependants;
  	public Set<Integer> dependantTypes; 
  	public Set<String> dependants_notAllowed;
  	public Set<String> varInCycle;
  	public Map<String, HashSet<String>> neededCycleVarMap;
  	boolean addDep = true;
  	boolean isParameter = false;
  	public int number_of_errors = 0;
  	public int line=1;
 	
  		/// error message	
    public void displayRecognitionError(String[] tokenNames,
                                        RecognitionException e) {
        String hdr = getErrorHeader(e);
        String msg = getErrorMessage(e, tokenNames);
        //LogUtils.errMsg("@parser "+ hdr + " " + msg);
        LogUtils.errMsgLocation(currentAbsolutePath, e.line, msg);
        number_of_errors++;
    }
}

@lexer::members {
	
	public int number_of_errors = 0;
	/// error message	
    public void displayRecognitionError(String[] tokenNames,
                                        RecognitionException e) {
        String hdr = getErrorHeader(e);
        String msg = getErrorMessage(e, tokenNames);
        LogUtils.errMsg("@lexer "+ hdr + " " + msg);
        number_of_errors++;
    }
}
//wreslPlusMain : study  template* sequence+ model+;


wreslFile
@after{ mObj = $t.modelObj;}
	: ('{' t=mt '}' EOF ) | ( t=mt EOF ); 

wreslMain
scope { StudyTemp sty;} 
@init {  $wreslMain::sty = new StudyTemp();}
@after{ styObj = $wreslMain::sty; }

	: 
   initial?
	( seq=sequence { $wreslMain::sty.seqList.add($seq.id); $wreslMain::sty.seqMap.put($seq.id,$seq.seqObj);  }    )+ 
	( g=group      { $wreslMain::sty.modelList.add($g.id); $wreslMain::sty.modelMap.put($g.id, $g.modelObj);  }    )*
	( m=model      { $wreslMain::sty.modelList.add($m.id); $wreslMain::sty.modelMap.put($m.id, $m.modelObj);  }    )+ 
	EOF ;

local_deprecated
  : '[' LOCAL ']' ;
    //{LogUtils.warningMsg("\"[local]\" is deprecated. All variables are by default local.");};

initial 
@init {  isParameter=true; }
@after{  isParameter=false; }
  : 
  Initial '{' 
    //config*
    (
        ( c=constant  {$wreslMain::sty.parameterList.add($c.id); $wreslMain::sty.parameterConstList.add($c.id); $wreslMain::sty.parameterMap.put($c.id, $c.ptObj);} ) 
      | ( s=svar_initial {$wreslMain::sty.parameterList.add($s.id); $wreslMain::sty.parameterMap.put($s.id, $s.svObj);} )
    )+ 
  '}';

//config : CONFIG SORTING '{' VALUE (TRUE | (FALSE {$wreslMain::sty.isSort=false;}))'}' ;

constant returns[String id, SvarTemp ptObj]
@init{  $ptObj = new SvarTemp();
        dependants = new LinkedHashSet<String>(); }
  : Const i=ID '{' n=number '}'   //n=expr_add_simple

          { 
            $id = $i.text;
            $ptObj.id = $i.text;
            $ptObj.caseName.add(Param.defaultCaseName);
            $ptObj.caseCondition.add(Param.always);
            $ptObj.caseExpression.add($n.text);
            $ptObj.dependants = dependants;

          };

svar_initial returns[String id, SvarTemp svObj]
@init{  dependants = new LinkedHashSet<String>(); 
        dependantTypes = new LinkedHashSet<Integer>();
        dependants_notAllowed = new LinkedHashSet<String>(); }
  :  SVAR  svar_g { $id=$svar_g.id;  $svObj=$svar_g.svObj; $svObj.dependants_notAllowed=dependants_notAllowed;  } ;           


          
expression_simple
@init{ dependants = new LinkedHashSet<String>(); }
  : expr_add_simple ;
          
          

//study : 'study' ID '{' include_template* include_sequence+  '}' ;
//
//
//include_template : INCLUDE 'template' ( ID | include_file )  ;
//include_sequence : INCLUDE SEQUENCE ID  ;



template : TEMPLATE ID '{' ( template_svar | template_dvar | template_dvar_array )*  '}' ;

template_dvar : '%dvar' varID '{' dvar_trunk '}' ;

template_dvar_array : '%dvar' dimension varID '{' dvar_trunk '}' ;

template_svar : '%svar' varID  svar_trunk ;

sequence returns[String id, SequenceTemp seqObj]
@init {$seqObj = new SequenceTemp();
       dependants = new LinkedHashSet<String>();
       $seqObj.fromWresl = this.currentAbsolutePath; 
       
       // for condition expression if previous cycle var is used
       neededCycleVarMap = new HashMap<String, HashSet<String>>();
       }
       
@after{$seqObj.model=$m.text; $seqObj.order=$o.text;
       $seqObj.dependants= dependants; $seqObj.line=line;
       $seqObj.neededCycleVarMap = neededCycleVarMap;
       }  
       
	: SEQUENCE i=ID {$id=$i.text; $seqObj.id=$i.text;} 
		'{' MODEL m=ID 
		   (( CONDITION cc=logical_main {$seqObj.condition=$cc.text;} ORDER o=INT    )| 
		    ( ORDER o=INT (CONDITION cc=logical_main {$seqObj.condition=$cc.text;})? ) )
		   ( TIMESTEP t=TIMESTEPVALUE {$seqObj.timeStep=$t.text.toUpperCase();} )? 
		    '}' 
	;



group returns[String id, ModelTemp modelObj]
: GROUP i=ID {$id=$i.text;} '{' t=mt '}' {$modelObj =$t.modelObj; $modelObj.id=$id;} ;



model_standalone : model ;

model returns[String id, ModelTemp modelObj]
: MODEL i=modelName {$id=$i.text;} 
	   '{' t=mt '}' 
	   {$modelObj =$t.modelObj; $modelObj.id=$id;}
	   ;

modelName: ID;

//model trunk
mt returns[ModelTemp modelObj]

scope { ModelTemp m_;} 
@init{ neededCycleVarMap = new HashMap<String, HashSet<String>>();
       $mt::m_ = new ModelTemp(); 
	   $mt::m_.absPath = currentAbsolutePath; 
	   $mt::m_.parentAbsPath = currentAbsoluteParent; }
@after{$modelObj =$mt::m_; 
       $modelObj.neededCycleVarMap = neededCycleVarMap;
       $modelObj.id=Param.legacywresl;}	    
:      
	   ( fi=include_file {$mt::m_.itemTypeList.add(Param.incFileType); $mt::m_.itemList.add($fi.id); $mt::m_.incFileIDList.add($fi.id); $mt::m_.incFileMap.put($fi.id, $fi.incFileObj); }
	   | ts=timeseries   {$mt::m_.itemTypeList.add(Param.tsType);$mt::m_.itemList.add($ts.id); $mt::m_.tsList.add($ts.id); $mt::m_.tsMap.put($ts.id, $ts.tsObj); }
	   | sv=svar_group   {$mt::m_.itemTypeList.add(Param.svType);$mt::m_.itemList.add($sv.id); $mt::m_.svList.add($sv.id); $mt::m_.svMap.put($sv.id, $sv.svObj); } 
	   | dv=dvar_g       {$mt::m_.itemTypeList.add(Param.dvType);$mt::m_.itemList.add($dv.id); $mt::m_.dvList.add($dv.id); $mt::m_.dvMap.put($dv.id, $dv.dvObj); } 
	   | ex=ex_g         {$mt::m_.itemTypeList.add(Param.exType);$mt::m_.itemList.add($ex.id); $mt::m_.exList.add($ex.id); $mt::m_.exMap.put($ex.id, $ex.exObj); }
	   | as=alias        {$mt::m_.itemTypeList.add(Param.asType);$mt::m_.itemList.add($as.id); $mt::m_.asList.add($as.id); $mt::m_.asMap.put($as.id, $as.asObj); }
	   | gl1=goal_s      {$mt::m_.itemTypeList.add(Param.gl1Type);$mt::m_.itemList.add($gl1.id);$mt::m_.glList.add($gl1.id);$mt::m_.glMap.put($gl1.id, $gl1.glObj); }
	   | gl2=goal_hs     {$mt::m_.itemTypeList.add(Param.gl2Type);$mt::m_.itemList.add($gl2.id);$mt::m_.glList.add($gl2.id);$mt::m_.glMap.put($gl2.id, $gl2.glObj); $mt::m_.gl2List.add($gl2.id); }
	   | network  
	   | wt=weight       {$mt::m_.wTableObjList.add($wt.wtObj);}
	   | im=include_model {$mt::m_.itemTypeList.add(Param.incModelType);$mt::m_.itemList.add(Param.model_label+$im.id); $mt::m_.incModelList.add($im.id);
	                       $mt::m_.incFileIDList.add($im.id); $mt::m_.incFileMap.put($im.id, null);
	                       }
	   | ifig=if_inc_items  {$mt::m_.itemTypeList.add(Param.ifIncItemGroupType); $mt::m_.itemList.add($ifig.id); $mt::m_.ifIncItemGroupIDList.add($ifig.id); $mt::m_.ifIncItemGroupMap.put($ifig.id, $ifig.ifIncItemGroupObj); }                  
	   )+
	   ;

if_inc_items returns[String id, IfIncItemGroup ifIncItemGroupObj]
scope { IfIncItemGroup incg_; 
        ArrayList<String> _arr; 
        HashMap<String, IncFileTemp> _incfmap;
        HashMap<String, SvarTemp> _incSvarMap;
        HashMap<String, DvarTemp> _incDvarMap;
        LinkedHashMap<String, AliasTemp> _incAliasMap;
        HashMap<String, TimeseriesTemp> _incTimeseriesMap;
        HashMap<String, GoalTemp> _incGoalSimpleMap;
        HashMap<String, GoalTemp> _incGoalComplexMap;
        HashMap<String, WeightTable> _incWeightTableMap;
        } 
@init{ $if_inc_items::incg_ = new IfIncItemGroup();
       $if_inc_items::incg_.id = "__item__"+Integer.toString($mt::m_.ifIncItemGroupIDList.size());
       $id = $if_inc_items::incg_.id;
       $ifIncItemGroupObj = $if_inc_items::incg_;
       //dependants = new LinkedHashSet<String>();
       $if_inc_items::incg_.dependants = new HashSet<String>();
      // $incg_.id = "__incfilegroup__"+Integer.toString($mt::m_.incFileGroupIDList.size()); 

       }
@after{ 
        // reserve space
        $mt::m_.svList.add($if_inc_items::incg_.id); 
        $mt::m_.dvList.add($if_inc_items::incg_.id);
        $mt::m_.asList.add($if_inc_items::incg_.id); 
        $mt::m_.tsList.add($if_inc_items::incg_.id); 
        $mt::m_.glList.add($if_inc_items::incg_.id); 
        $mt::m_.gl2List.add($if_inc_items::incg_.id); 
        //$if_inc_items::incg_.dependants = dependants;
        $if_inc_items::incg_.fromWresl = this.currentAbsolutePath;
        $if_inc_items::incg_.line=line; 
       // $mt::m_.wTableObjList.add($if_inc_items::incg_.id);         

}
  : if_  elseif_* else_?  ;

if_ 
@init{ dependants = new LinkedHashSet<String>(); }
  :
  If{line=$If.line;} e=logical_main {$if_inc_items::incg_.dependants.addAll(dependants);}
  '{' include_item_group '}' 
  {$if_inc_items::incg_.conditionList.add($e.text);};
  
elseif_ 
@init{ dependants = new LinkedHashSet<String>(); } 
  :
  Elseif e=logical_main {$if_inc_items::incg_.dependants.addAll(dependants);}
  '{' include_item_group '}'
  {$if_inc_items::incg_.conditionList.add($e.text);};
  
else_  : 
  Else '{' include_item_group '}'
  {$if_inc_items::incg_.conditionList.add(Param.always);};

include_item_group
@init{ $if_inc_items::_arr = new ArrayList<String>(); 
       $if_inc_items::_incfmap = new HashMap<String, IncFileTemp>();
       $if_inc_items::_incSvarMap = new HashMap<String, SvarTemp>();
       $if_inc_items::_incDvarMap = new HashMap<String, DvarTemp>();
       $if_inc_items::_incAliasMap = new LinkedHashMap<String, AliasTemp>();
       $if_inc_items::_incTimeseriesMap = new HashMap<String, TimeseriesTemp>(); 
       $if_inc_items::_incGoalSimpleMap = new HashMap<String, GoalTemp>();              
       $if_inc_items::_incGoalComplexMap = new HashMap<String, GoalTemp>(); 
       $if_inc_items::_incWeightTableMap = new HashMap<String, WeightTable>(); 
}
@after {
        $if_inc_items::incg_.inc_item_list.add($if_inc_items::_arr); 
        $if_inc_items::incg_.inc_files_map_list.add($if_inc_items::_incfmap);
        $if_inc_items::incg_.inc_svar_map_list.add($if_inc_items::_incSvarMap);
        $if_inc_items::incg_.inc_dvar_map_list.add($if_inc_items::_incDvarMap);
        $if_inc_items::incg_.inc_alias_map_list.add($if_inc_items::_incAliasMap);
        $if_inc_items::incg_.inc_timeseries_map_list.add($if_inc_items::_incTimeseriesMap);
        $if_inc_items::incg_.inc_goalSimple_map_list.add($if_inc_items::_incGoalSimpleMap);
        $if_inc_items::incg_.inc_goalComplex_map_list.add($if_inc_items::_incGoalComplexMap);        
        $if_inc_items::incg_.inc_weightTable_map_list.add($if_inc_items::_incWeightTableMap); 
}
: 
   (
      ( fi=include_file { 
        $if_inc_items::_arr.add($fi.id);  
        $if_inc_items::_incfmap.put($fi.id, $fi.incFileObj);
        $mt::m_.incFileIDList.add($if_inc_items::incg_.id); 
        })
   |  ( si=svar_group { 
        $if_inc_items::_arr.add($si.id);  
        $if_inc_items::_incSvarMap.put($si.id, $si.svObj);
        })
   |  ( di=dvar_g { 
        $if_inc_items::_arr.add($di.id);  
        $if_inc_items::_incDvarMap.put($di.id, $di.dvObj);
        })
   |  ( ai=alias { 
        $if_inc_items::_arr.add($ai.id);  
        $if_inc_items::_incAliasMap.put($ai.id, $ai.asObj);
        }) 
   |  ( ti=timeseries { 
        $if_inc_items::_arr.add($ti.id);  
        $if_inc_items::_incTimeseriesMap.put($ti.id, $ti.tsObj);
        })
   |  ( gsi=goal_s { 
        $if_inc_items::_arr.add($gsi.id);  
        $if_inc_items::_incGoalSimpleMap.put($gsi.id, $gsi.glObj);
        })  
   |  ( ghi=goal_hs { 
        $if_inc_items::_arr.add($ghi.id);  
        $if_inc_items::_incGoalComplexMap.put($ghi.id, $ghi.glObj);
        })                              
   |  ( wti=weight {   
        $if_inc_items::_incWeightTableMap.put($wti.id, $wti.wtObj);
        }) 
  )+ ;


///// external
//
//external returns[String id, ExternalTemp exObj]
//scope { ExternalTemp ex_;} 
//@init{ $external::ex_ = new ExternalTemp(); 
//       $external::ex_.fromWresl = this.currentAbsolutePath; 
//       dependants = new LinkedHashSet<String>();  
//	 }
//@after{ $id = $external::ex_.id; $exObj=$external::ex_;}	 
//	: external_old
// 	;
//
//external_old : DEFINE ('[' LOCAL ']')? externalID '{' EXTERNAL external_fileName '}' ;
//
//
//external_fileName : f=fileName {$external::ex_.fileName=$f.text;} ;
//fileName : ID ('.' ID)? ;
//externalID : i=ID {$external::ex_.id=$i.text;} ; 


weight returns[String id, WeightTable wtObj]
scope { WeightTable wt_;
        String id_; 
      } 
@init{ $weight::wt_ = new WeightTable(); 
       $weight::wt_.fromWresl = this.currentAbsolutePath; 
       $weight::wt_.line = line;
       dependants = new LinkedHashSet<String>();
	 }
@after{ $id = $weight::wt_.id_lowcase; $wtObj=$weight::wt_; $wtObj.dependants= dependants;}	 
	: weight_legacy | weight_new ;

weight_legacy : OBJECTIVE{line=$OBJECTIVE.line;} (local_deprecated)? objGroupName '='? '{' weight_legacy_unit+ '}'  ;

//obj : 'obj' | 'Obj' | 'OBJ' ;
objGroupName : i=ID {$weight::wt_.id_lowcase=$i.text.toLowerCase();
                     $weight::wt_.id_raw=$i.text;} ;

weight_legacy_unit 
	: '[' i=ID{line=$i.line;} (ta=weightTimeArray {$weight::wt_.varTimeArraySizeMap.put($i.text,$ta.text);})? ',' e=expr_add ']' ','?
	{$weight::wt_.varList.add($i.text);
	$weight::wt_.varWeightMap.put($i.text,$e.text);
	$weight::wt_.varLineMap.put($i.text, line);};
  
weightTimeArray : '(' d=( INT | ID ) ')';  // this is a repetition. The time array should be known when dvar is declared.

weight_new : OBJECTIVE{line=$OBJECTIVE.line;} weightTableID '{' weight_group '}'  ;

weightTableID : i=ID {$weight::wt_.id_lowcase=$i.text.toLowerCase();
                      $weight::wt_.id_raw=$i.text;
                      $weight::wt_.line=$i.getLine();} ;
	
weight_group
@init{ $weight::wt_.isWeightGroup=true; }
	:  WEIGHT w=expr_add  {$weight::wt_.commonWeight=$w.text;} 
	   weight_trunk 
	;	

weight_trunk 
	:  ( DeviationPenalty   p=expr_add {$weight::wt_.deviationPenalty=$p.text;} )? 
	   ( DeviationTolerance t=expr_add {$weight::wt_.deviationTolerance=$t.text;} )? 
	   VARIABLE ( weight_group_unit | weight_subgroup )+
	  ;
	    
weight_group_unit : i=ID  {$weight::wt_.varList.add($i.text);} ;

weight_subgroup 
scope { WeightSubgroup sub_;} 
@init{ $weight_subgroup::sub_ = new WeightSubgroup(); }
@after{ $weight_subgroup::sub_.id=$i.text;
        $weight::wt_.subgroupMap.put($i.text,$weight_subgroup::sub_); 
        //$weight::wt_.varList.add($i.text);
        //System.out.println("subgroup: "+$weight::wt_.subgroupMap.keySet());
      }
	:  i=ID //{$weight::wt_.subgroupMap.put($i.text.toLowerCase(),$weight_subgroup::sub_);} 
	   '{' weight_subgroup_trunk '}'
	;

weight_subgroup_trunk 
	:  ( DeviationPenalty   p=expr_add {$weight_subgroup::sub_.deviationPenalty=$p.text;} )? 
	   ( DeviationTolerance t=expr_add {$weight_subgroup::sub_.deviationTolerance=$t.text;} )? 
	   VARIABLE ( weight_subgroup_unit+ ) // | weight_subgroup )+
	  ;

weight_subgroup_unit : i=ID  {$weight_subgroup::sub_.varList.add($i.text);} ;


include_model returns[String id] : INCLUDE (MODEL|GROUP) i=ID   {$id=$i.text;} ;

include_file returns[String id, IncFileTemp incFileObj]
@init{ $incFileObj = new IncFileTemp();
       $incFileObj.id = "__file__"+Integer.toString($mt::m_.incFileIDList.size()); 
       $id = $incFileObj.id;
       }
      : INCLUDE (local_deprecated)? fp=file_path {$incFileObj.rawPath=Tools.strip($fp.text);}   ;

file_path : QUOTE  ;


/// goal simple
goal_s returns[String id, GoalTemp glObj]
scope { GoalTemp gl_;
        String id_; 
      } 
@init{ $goal_s::gl_ = new GoalTemp(); 
       $goal_s::gl_.fromWresl = this.currentAbsolutePath;
       $goal_s::gl_.hasCase = false; 
       dependants = new LinkedHashSet<String>();
       varInCycle = new LinkedHashSet<String>();    
	 }
@after{ $id = $goal_s::gl_.id; 
        $glObj=$goal_s::gl_; 
        $glObj.dependants= dependants;
        $glObj.neededVarInCycleSet= varInCycle;
        $glObj.needVarFromEarlierCycle = (varInCycle!=null);
        $glObj.line=line;}	 

	: GOAL{line=$GOAL.line;} (local_deprecated)? (goalSimpleArray|goalSimpleTimeArray)? i=ID  '{' ( e=expr_constraint )'}' 
	{$goal_s::gl_.id=$i.text; 
	 $goal_s::gl_.caseCondition.add(Param.always);
	 $goal_s::gl_.caseName.add(Param.defaultCaseName);
	 $goal_s::gl_.caseExpression.add($e.text);};

goalSimpleArray :     '[' d=( INT | ID ) ']'  {$goal_s::gl_.arraySize=$d.text; };
goalSimpleTimeArray : '(' d=( INT | ID ) ')'  {$goal_s::gl_.timeArraySize=$d.text; };


/// goal hs
goal_hs returns[String id, GoalTemp glObj]
scope { GoalTemp gl_;
        String id_; 
      } 
@init{ $goal_hs::gl_ = new GoalTemp(); 
       $goal_hs::gl_.fromWresl = this.currentAbsolutePath; 
       dependants = new LinkedHashSet<String>();
       varInCycle = new LinkedHashSet<String>();  
       $goal_hs::gl_.hasLhs=true;
	 }
@after{ $id = $goal_hs::gl_.id; 
        $glObj=$goal_hs::gl_; 
        $glObj.dependants= dependants;
        $glObj.neededVarInCycleSet= varInCycle;
        $glObj.needVarFromEarlierCycle = (varInCycle!=null);
        $glObj.line=line;}	 
	: GOAL{line=$GOAL.line;} (local_deprecated)? (goalHsArray|goalHsTimeArray)? i=ID  {$goal_hs::gl_.id=$i.text;}
	  '{' lhs 
	  ( goal_hs_nocase 		
	  | goal_hs_cases 
	  ) '}';

goalHsArray :     '[' d=( INT | ID ) ']'  {$goal_hs::gl_.arraySize=$d.text; };
goalHsTimeArray : '(' d=( INT | ID ) ')'  {$goal_hs::gl_.timeArraySize=$d.text; };

goal_hs_nocase
@after{ $t.gc.id = Param.defaultCaseName;
		$goal_hs::gl_.caseName.add($t.gc.id); 
		$goal_hs::gl_.caseMap.put($t.gc.id, $t.gc);
		$goal_hs::gl_.hasCase = false;
		}
	: t=goal_hs_trunk ;

goal_hs_trunk returns[GoalCase gc]
scope { GoalCase gc_; } 
@init { $goal_hs_trunk::gc_ = new GoalCase();}
@after{ $gc=$goal_hs_trunk::gc_;}
	:  rhs ((lhs_gt_rhs lhs_lt_rhs?) | (lhs_lt_rhs lhs_gt_rhs?))? ;

goal_hs_cases : goal_hs_case+ ;

goal_hs_case
@after{ $t.gc.id = $ci.text;
		$t.gc.condition = $cc.text;
		$goal_hs::gl_.caseName.add($t.gc.id); 
		$goal_hs::gl_.caseMap.put($t.gc.id, $t.gc);
		$goal_hs::gl_.hasCase = true;}
	: CASE ci=ID '{' CONDITION cc=logical_main t=goal_hs_trunk  '}';
	

lhs : LHS e=expr_add {$goal_hs::gl_.lhs=$e.text;} ;
rhs : RHS e=expr_add {$goal_hs_trunk::gc_.rhs=$e.text;} ;
lhs_gt_rhs : LHS '>' RHS ( PENALTY p=expr_add {$goal_hs_trunk::gc_.lhs_gt_rhs=$p.text;} | constrain ) ;
lhs_lt_rhs : LHS '<' RHS ( PENALTY p=expr_add {$goal_hs_trunk::gc_.lhs_lt_rhs=$p.text;} | constrain ) ;

constrain : 'constrain' ;

/// alias

alias returns[String id, AliasTemp asObj]
scope { AliasTemp as_;} 
@init{ $alias::as_ = new AliasTemp(); 
       $alias::as_.fromWresl = this.currentAbsolutePath; 
       dependants = new LinkedHashSet<String>();
       varInCycle = new LinkedHashSet<String>();  
	 }
@after{ $id = $alias::as_.id; 
        $asObj=$alias::as_; 
        $asObj.dependants= dependants;
        $asObj.neededVarInCycleSet= varInCycle;
        $asObj.needVarFromEarlierCycle = (varInCycle!=null);
        $asObj.line=line;}	 
	: alias_new | alias_old
 	;

alias_old : DEFINE{line=$DEFINE.line;}  (local_deprecated)? (aliasArray|aliasTimeArray)? aliasID '{' ALIAS  aliasExpresion  aliasKind?  aliasUnits? aliasNoSolver? '}' ;
alias_new : ALIAS{line=$ALIAS.line;} (aliasArray|aliasTimeArray)? aliasID '{' aliasExpresion  aliasKind?  aliasUnits? aliasNoSolver? '}' ;

aliasExpresion : e=expr_add {$alias::as_.expression=$e.text;}; 
aliasID : i=ID {$alias::as_.id=$i.text;}; 
aliasUnits: UNITS s=QUOTE {$alias::as_.units=Tools.strip($s.text);};
aliasKind:  KIND s=QUOTE {$alias::as_.kind=Tools.strip($s.text);};
aliasNoSolver : NoSolver {$alias::as_.noSolver=true;};

aliasArray :     '[' d=( INT | ID ) ']'  {$alias::as_.arraySize=$d.text; };
aliasTimeArray : '(' d=( INT | ID ) ')'  {$alias::as_.timeArraySize=$d.text; };

/// svar

svar_group returns[String id, SvarTemp svObj]
  : ( SVAR{line=$SVAR.line;} | DEFINE{line=$DEFINE.line;}  (local_deprecated)? ) svar_g { $id=$svar_g.id;  $svObj=$svar_g.svObj;  $svObj.line=line;} ;


svar_g returns[String id, SvarTemp svObj]
scope { SvarTemp sv_;
        String id_; 
      } 
@init{ $svar_g::sv_ = new SvarTemp(); 
       $svar_g::sv_.fromWresl = this.currentAbsolutePath; 
       dependants = new LinkedHashSet<String>();
       varInCycle = new LinkedHashSet<String>();
       
	 }
@after{ $id = $svar_g::sv_.id; 
        $svObj=$svar_g::sv_; 
        $svObj.dependants= dependants;
        $svObj.neededVarInCycleSet= varInCycle;
        $svObj.needVarFromEarlierCycle = (varInCycle!=null);
        }	 
	:  ( svar | svar_array | svar_timeArray ) ;


svarID : i=ID  {$svar_g::sv_.id =$i.text;} ;

svar: svarID '{' svar_trunk '}' ;

svar_array: svarArray svar;

svar_timeArray: svarTimeArray svar ;

svarArray :     '[' d=( INT | ID ) ']'  {$svar_g::sv_.arraySize=$d.text; };
svarTimeArray : '(' d=( INT | ID ) ')'  {$svar_g::sv_.timeArraySize=$d.text; };

/// svar trunk

svar_trunk 
	: ( svar_noCase | svar_case+ ) ( svarKind svarUnits )?  ;

svarUnits: UNITS QUOTE ;
svarKind:  KIND QUOTE ;

svar_noCase
	@after {$svar_g::sv_.caseName.add(Param.defaultCaseName); $svar_g::sv_.caseCondition.add(Param.always);}
	: svar_value | svar_sum | svar_table 
	;

svar_case
	@after {$svar_g::sv_.caseName.add($ci.text); $svar_g::sv_.caseCondition.add($cc.text);}
	: CASE ci=ID '{' CONDITION cc=logical_main ( svar_value | svar_sum | svar_table )  '}' ;

svar_value 
	: VALUE e=expr_add
	{ $svar_g::sv_.caseExpression.add($e.text); };

svar_sum
	: h=sum_header c=sum_content
	{ $svar_g::sv_.caseExpression.add($h.text+" "+$c.text); };

sum_header 
	: SUM '(' 'i' '=' e1=expr_add ',' e2=expr_add (',' integer)?  ')' 
	;

sum_content 
	: e=expr_add ;

svar_table 
	: t1=svar_table_1 { $svar_g::sv_.caseExpression.add($t1.text); 
	  //System.out.println($t1.text);
	  } 
	| svar_table_2 ;

svar_table_1 
	: s=SELECT { $s.setText("select ");} (ID|reservedID) 
	  f=FROM   { $f.setText(" from ");} ID 
	  (g=GIVEN { $g.setText(" given ");} expr_assign 
	   u=USE   { $u.setText(" use ");}  ID)? 
	  (w=WHERE { $w.setText(" where ");} where)?
    ;

where: expr_assign (',' expr_assign)* ;

svar_table_2
	: 'table' '(' ')' ;


// common rules

dimension : '[' ( INT | ID ) ']' ;

dimension_time : '(' ( INT | ID ) ')' ;

//typeNumber: 'integer' | 'real' | 'binary' ;

//value : 'value' expr_add;

//sum: 'sum' '(' 'i' '=' INT ',' expr_add ',' INT  ')' '(' expr_add  ')';

//table : 'select' ID 'from' ID 'where' expr_assign (',' expr_assign)* ;

// end common rules



//timeseries: 'timeseries' ID '{' 'kind' '\'' .+ '\'' 'units' '\'' id_with_dash '\'' '}';
timeseries returns[String id, TimeseriesTemp tsObj]
scope { TimeseriesTemp ts_;
        String id_; 
      } 
@init{ $timeseries::ts_ = new TimeseriesTemp(); 
       $timeseries::ts_.fromWresl = this.currentAbsolutePath; 
	 }
@after{ $id = $timeseries::ts_.id; 
        $tsObj = $timeseries::ts_; 
        $tsObj.line = line;
	 }
	: timeseries_new | timeseries_old ;

timeseries_new : TIMESERIES{line=$TIMESERIES.line;} tsID      '{' (NAME      bpart_id)? tsKind tsUnits convert? '}' ;
timeseries_old : DEFINE{line=$DEFINE.line;} (local_deprecated)? tsID   '{' TIMESERIES bpart_id? tsKind tsUnits convert? '}' ;
			
tsID : i=ID {$timeseries::ts_.id=$i.text;$timeseries::ts_.dssBPart=$i.text;} ;			
tsUnits: UNITS s=QUOTE {$timeseries::ts_.units=Tools.strip($s.text);} ;
tsKind:  KIND s=QUOTE {$timeseries::ts_.kind=Tools.strip($s.text);} ;			
bpart_id : s=QUOTE {$timeseries::ts_.dssBPart=Tools.strip($s.text);};
convert : CONVERT s=QUOTE {$timeseries::ts_.convertToUnits=Tools.strip($s.text);};

/// network
network : NETWORK ID '{' node? connection+ '}';

//inlet:  'inlet'  ID ( ',' ID)* ;
//outlet: 'outlet' ID ( ',' ID)* ;
node:   NODE    ID ( ',' ID)* ;

connection:    branch | branch_short ;

inlet       :  INFLOW  element ;
outlet      :  element OUTFLOW ;
inoutlet    :  INFLOW element OUTFLOW ;

branch_short : inoutlet ;

branch : (elements | inlet) FLOW  (element FLOW)*  (elements | outlet) ;

elements     :  element ( ',' element)* ;

element      :  ID ('.' ID)?  ;


//branch:     ( id_with_port arrow_type )+ id_with_port   ;
//
//id_with_port:  inlet_port?   id  outlet_port?  ; 
//
//id_with_inlet : inlet_port?  id ;
//
//id_with_outlet :  id outlet_port? ;
//
//inlet_port : '(' ID ':)' ;
//outlet_port : '(:' ID ')' ;


/// external

ex_g returns[String id, ExternalTemp exObj]
scope { ExternalTemp ex_;
        String id_; 
      } 
@init{ $ex_g::ex_ = new ExternalTemp(); 
       $ex_g::ex_.fromWresl = this.currentAbsolutePath; 
	 }
@after{ $id = $ex_g::ex_.id; 
        $exObj = $ex_g::ex_; 
        $exObj.line=line;
	 }
	 : ex_old ; //| ex_new ;

ex_id : i=ID {$ex_g::ex_.id=$i.text;} ;

ex_old : DEFINE{line=$DEFINE.line;} (local_deprecated)? ex_id '{' EXTERNAL f=ex_fileName {$ex_g::ex_.fileName=$f.text;} '}' ;

ex_fileName : ID ('.' ID)? ;

//ex_new : EXTERNAL ex_id '{' ( ex_fortran | ex_java ) '}' ;

//ex_fortran : 'language' 'fortran' '{' fortran_var+  fortran_return '}' ;

//fortran_var : VARIABLE ID '{' 'type' 'intent' '}' ;
//fortran_return : 'return' ;

//ex_java : 'language' 'java' ;


/// dvar
dvar_g returns[String id, DvarTemp dvObj]
scope { DvarTemp dvar_;
        String id_; 
      } 
@init{ $dvar_g::dvar_ = new DvarTemp(); 
       $dvar_g::dvar_.fromWresl = this.currentAbsolutePath; 
	 }
@after{ $id= $dvar_g::id_; $dvObj= $dvar_g::dvar_; $dvObj.line=line; }
	: ( dvar_group_new | dvar_group_old ) 
	;

dvarID : i=ID { $dvar_g::dvar_.id=$i.text; $dvar_g::id_=$i.text; };

dvar_group_old: DEFINE{line=$DEFINE.line;} (local_deprecated)? dvar ;
dvar_group_new: DVAR{line=$DVAR.line;}    dvar ;

dvar: (dvarArray|dvarTimeArray)? dvarID '{' dvar_trunk '}'  ;

dvarArray :     '[' d=( INT | ID ) ']'  {$dvar_g::dvar_.arraySize=$d.text; };
dvarTimeArray : '(' d=( INT | ID ) ')'  {$dvar_g::dvar_.timeArraySize=$d.text; };

//dvar_array : dimension dvarID '{' dvar_trunk '}'  ;
//
//dvar_timeArray:  dimension_time dvarID '{' dvar_trunk  '}'  ;
 
dvar_trunk 
	: 
	// TODO: remove addDep. this is to match wreslparser result.
	( ( ( dvarIsInteger? ( std | ( {addDep = false;} lower_upper {addDep = true;} ) ) ) 
	  | dvarIsBinary )
	 dvKindUnits )
	| ('<' ID '>') ;

//dvar_array_trunk : ( index_assign? '{' dvar_trunk '}' )+ ;
//dvar_timeArray_trunk : ( timeIndex_assign? '{' dvar_trunk '}' )+ ;

dvarIsInteger : INTEGER {$dvar_g::dvar_.isInteger=true; $dvar_g::dvar_.upperBound="1";$dvar_g::dvar_.lowerBound="0";} ;
dvarIsBinary  : BINARY  {$dvar_g::dvar_.isInteger=true; $dvar_g::dvar_.upperBound="1";$dvar_g::dvar_.lowerBound="0";} ;
index_assign : '[' INT (':' INT)? ']' ;
timeIndex_assign : '(' INT (':' INT)? ')' ;

lower_upper : lower upper? | upper lower? ;
upper: UPPER ( e=expr_limited {$dvar_g::dvar_.upperBound=$e.text;} | UNBOUNDED {$dvar_g::dvar_.upperBound=Param.upper_unbounded;}) ;
lower: LOWER ( e=expr_limited {$dvar_g::dvar_.lowerBound=$e.text;} | UNBOUNDED {$dvar_g::dvar_.lowerBound=Param.lower_unbounded;}) ;

expr_limited: expr_add ; //number ( ('*' | '/') unitFunc )? ;

std: STD ;

dvKindUnits : dvKind dvUnits | dvUnits dvKind ;
dvKind:  KIND s=QUOTE  {$dvar_g::dvar_.kind=Tools.strip($s.text);};
dvUnits: UNITS s=QUOTE {$dvar_g::dvar_.units=Tools.strip($s.text);};







//================ begin logical ==============//

logical_main: logical_or | ALWAYS ;

logical_or
    :   logical_and       
        (   o=OR  {$o.setText(" .or. ");} logical_and  )*    
    ;

logical_and
    :   logical_unary       
        (   a=AND  {$a.setText(" .and. ");}  logical_unary  )*    
    ;

logical_unary : NOT? logical_term;

logical_term
    :    ( logical_relation ) => logical_relation
    |    ( logical_or_p ) => logical_or_p
    |    logical_relation_p
    |    logicalFunc    
    ;

logical_or_p : '(' logical_or ')' ;

logical_relation_p
	:  '(' logical_relation ')'
	;

logical_relation
	:  expr_add  relation_token  expr_add
	;

logicalFunc 
	: RANGE '(' expr_add ',' expr_add ',' expr_add ')' ;	
//================ end logical ==============//


/// constraint expr
constraint_token : '>' | '<' | '>=' | '<=' | '=' ;

expr_constraint
	: expr_add constraint_token expr_add
	;

// comparison expr
relation_token: '>' | '<' | '>=' | '<=' | '==' | '/=' ;

expr_relation
	:  expr_add  relation_token  expr_add
	;

// ignore dependants in the lhs
expr_assign
@init{ Set<String> backup = new LinkedHashSet<String>(dependants);}
    :   expr_add {dependants = backup;} '='  expr_add  
    ;
//================ begin simple expr_add ==============//
expr_add_simple
    :   expr_mult_simple  ( ( '+' | '-' )  expr_mult_simple   )*
    ;

expr_mult_simple 
    :    expr_unary_simple  ( ( '*' | '/' )  expr_unary_simple   )*
    ; 

expr_unary_simple: '-'? expr_term_simple ;

expr_term_simple 
    :   atom_simple
    |   '(' expr_add_simple ')'    
    ;

atom_simple
    :  number_p
    |  v=varID {dependants.add($v.text);} 
    |  intrinsicFunc_simple
    ;
    
intrinsicFunc_simple
  : mathFunc_simple 
  | multiInputFunc_simple
  | unitFunc_simple
  ;

mathFunc_simple 
  :  LOG '(' expr_add_simple ')' 
  |  INT_word '(' expr_add_simple ')' 
  |  ROUND '(' expr_add_simple ')' 
  |  MOD '(' expr_add_simple ',' expr_add_simple ')' 
  |  SIN '(' expr_add_simple ')' 
  |  COS '(' expr_add_simple ')' 
  |  TAN '(' expr_add_simple ')' 
  |  COT '(' expr_add_simple ')' 
  |  ASIN '(' expr_add_simple ')' 
  |  ACOS '(' expr_add_simple ')' 
  |  ATAN '(' expr_add_simple ')' 
  |  ACOT '(' expr_add_simple ')' 
  |  exceedFunc
  |  exceedtsiFunc  
  ;
  
unitFunc_simple
  : ( CFS_TAF | TAF_CFS ) ('(' expr_add_simple ')')? ;  
  
multiInputFunc_simple
  : ( MIN | MAX ) '(' expr_add_simple ( ',' expr_add_simple )* ')' ; 
       
//================ end simple expr_add ==============//

//================ begin expr_add ==============//
expr_add
    :   expr_mult  ( ( '+' | '-' )  expr_mult   )*
    ;

expr_mult 
    :    expr_unary  ( ( '*' | '/' )  expr_unary   )*
    ; 

expr_unary: ('-'|'+')?  expr_term ;

expr_term 
    :   atom
    |   '(' expr_add ')'    
    ;
//================ end expr_add ==============//

atom
    :  number_p
    |  v=varID {if (addDep) dependants.add($v.text);}  
    |  intrinsicFunc
   // |  r=reservedID   {if (isParameter) dependants_notAllowed.add($r.text);}  
    |  s=specialVar   {if (isParameter) dependants_notAllowed.add($s.text);}  
    |  externalFunc
    |  vf=varFunc     {if (isParameter) dependants_notAllowed.add($vf.text);}    
    |  p=preCycleVar  {if (isParameter) dependants_notAllowed.add($p.text);}   
    ;

specialVar : 'i' | '$m' | '$M';

preCycleVar
	:  p1=preCycleVar_old {varInCycle.add($p1.text);} | p2=preCycleVarIndex {varInCycle.add($p2.text);}
	;

preCycleVar_old :  var=ID '[' cycle=ID ']' ('(' e=expr_add ')')?
{ 
  // TODO: don't convert to lower case here!
  if (!isParameter){
    String cnl = $cycle.text.toLowerCase();
    String vl = $var.text.toLowerCase();
    if (neededCycleVarMap.keySet().contains(cnl)) {
      neededCycleVarMap.get(cnl).add(vl);
    } else {
      HashSet<String> t = new HashSet<String>();
      t.add(vl);
      neededCycleVarMap.put(cnl, t);
    }
  }
} ; 

preCycleVarIndex :  var=ID '[' '-' INT ']' ('(' e=expr_add ')')? {
  if (!VarCycleIndex.varCycleIndexList.contains($var.text)){
    VarCycleIndex.varCycleIndexList.add($var.text.toLowerCase());
  }
}; 

externalFunc
	:  'extern:(' ID '(' expr_add ')' ')'
	;
intrinsicFunc
	: mathFunc 
	| multiInputFunc
	| unitFunc
	| tableFunc
	| timeFunc
	| sumFunc
	; 

sumFunc: '(' sum_header sum_content ')';
	
timeFunc:  ( DAY | MONTH | WATERYEAR ) ( '(' expr_add ')' )? ;

tableFunc : 'table' '(' tableName ',' columnNumber ',' rowNumber ')' ;

tableName : ID ;
columnNumber : INT ;
rowNumber : INT ;
	
varFunc
	: v=varID '(' func_arg  (',' func_arg )*   ')'  ('(' expr_add ')')?
	{dependants.add($v.text);
	 //System.out.println(dependants);
	 }
	;

varID : ID ;

func_arg: expr_add|trunk_timeArray;

trunk_timeArray
  : v1=varID '(' (integer|v2=varID{dependants.add($v2.text);}) ':' (integer|v3=varID{dependants.add($v3.text);}) ')';  
	
number : integer | real ;
number_p : integer_p | real_p ;

integer :	integer_p|integer_n ;
real   :	real_p|real_n ;

integer_p : INT ;
integer_n : '-' INT ;
real_p : REAL ;
real_n : '-' REAL ;


id_domain : ( ID '.' )?  ID ;


	
mathFunc 
  :  LOG '(' expr_add ')' 
  |	 INT_word '(' expr_add ')' 
  |  ROUND '(' expr_add ')' 
  |  MOD '(' expr_add ',' expr_add ')' 
  |  SIN '(' expr_add ')' 
  |  COS '(' expr_add ')' 
  |  TAN '(' expr_add ')' 
  |  COT '(' expr_add ')' 
  |  ASIN '(' expr_add ')' 
  |  ACOS '(' expr_add ')' 
  |  ATAN '(' expr_add ')' 
  |  ACOT '(' expr_add ')' 
  |  exceedFunc
  |  exceedtsiFunc
  ;
  
unitFunc
  : ( CFS_TAF | TAF_CFS ) ('(' expr_add ')')? ;  
  
multiInputFunc
  : ( MIN | MAX ) '(' expr_add ( ',' expr_add )* ')' ;
  
//exceedance(FOLSM_INFLOW, 0.9,ALL/OCT,1921,OCT,31,2015,SEP,30)  
exceedFunc
  : EXCEEDANCE '(' varID ',' number_p ',' varID ',' integer_p ',' varID ',' integer ',' integer_p ',' varID ',' integer ')'   
  ;
  
exceedtsiFunc
  : EXCEEDANCE_TSI '(' varID ',' number_p ',' varID ',' integer_p ',' varID ',' integer ',' integer_p ',' varID ',' integer ')'   
  ;
  
//
//// reserved ID
//DaysIn: 'daysin';
//Int: 'int';
//Real: 'real';


reservedID :  DAY | MONTH | WATERYEAR ; //| MonthID ;

QUOTE : '\'' .*  '\'' ;

ML_COMMENT : '/*' .* '*/' {skip();}; 

SL_COMMENT : ('#'|'!') ~('\r'|'\n')*  {skip();};  //{$channel=HIDDEN;} ;

AND : '&&' | '.and.' | '.AND.' ;
OR  : '||' | '.or.' | '.OR.' ;
NOT :  '.not.' | '.NOT.' ;
NOT_EQUAL :  '.ne.' | '.NE.' ;

DAY: 'day'|'Day'|'DAY';
MONTH :   'month' | 'Month' | 'MONTH' ;
WATERYEAR : 'wateryear' | 'Wateryear' | 'WaterYear' | 'WATERYEAR'  ; 
//MonthID : 'jan'|'feb'|'mar'|'apr'|'may'|'jun'|'jul'|'aug'|'sep'|'oct'|'nov'|'dec';        


REAL :   ( Digit+ '.' Digit* ) | ( '.' Digit+ )  ;
INT :   Digit+ ;

OBJECTIVE : 'objective' | 'OBJECTIVE' | 'Objective' ;
//OBJ : 'obj' | 'Obj' | 'OBJ' ;
MODEL :     'model' | 'MODEL' | 'Model' ;
GROUP :     'group' | 'Group' | 'GROUP';
SEQUENCE :  'sequence' | 'Sequence' | 'SEQUENCE' ;

ORDER :     'order' | 'ORDER' | 'Order' ;
TIMESTEP  : 'timestep'|'TIMESTEP'|'TimeStep';
TIMESTEPVALUE: '1mon'|'1day';
INCLUDE :   'include' | 'INCLUDE' | 'Include' ;
CASE :      'case' | 'CASE' | 'Case' ;
CONDITION : 'condition' | 'CONDITION' | 'Condition' ;
GOAL :      'goal' | 'GOAL' | 'Goal' ;
VALUE :     'value' | 'VALUE' | 'Value';
PENALTY : 'penalty' | 'PENALTY' | 'Penalty' ;
DeviationPenalty : 'deviationpenalty' | 'DEVIATIONPENALTY' | 'DeviationPenalty' | 'deviationPenalty' | 'Deviationpenalty' ;
DeviationTolerance : 'deviationtolerance' | 'DEVIATIONTOLERANCE' | 'DeviationTolerance' | 'deviationTolerance' | 'Deviationtolerance' ;
WEIGHT : 'weight' | 'WEIGHT' | 'Weight' ;
//ITEM    : 'item' | 'ITEM' | 'Item' ;

CONFIG : 'config' | 'CONFIG' | 'Config';
SORTING : 'sorting' | 'SORTING' | 'Sorting' ;
TRUE : 'true' | 'TRUE' | 'True' ;
FALSE : 'false' | 'FALSE' | 'False' ; 
LABEL : 'label' ;
NAME : 'name' ;

Initial : 'initial' | 'Initial' | 'INITIAL' ;
//Parameter : 'parameter' | 'Parameter' | 'PARAMETER' ;
Const : 'const' | 'Const' | 'CONST' ;

If : 'If' | 'IF' | 'if' ;
Elseif : 'Elseif' | 'ELSEIF' | 'elseif' | 'ElseIf' ;
Else : 'Else' | 'ELSE' | 'else' ;

// deprecated keyword
DEFINE : 'define' | 'DEFINE' | 'Define' ;
LOCAL : 'local' | 'LOCAL' | 'Local' ; 
/////////////////////////////// 

STD : 'std' | 'STD' | 'Std' ;
DVAR : 'dvar' | 'DVAR' | 'Dvar' ;
SVAR : 'svar' | 'SVAR' | 'Svar' ; 
VARIABLE  : 'variable' | 'VARIABLE' | 'Variable' ;
ALIAS : 'alias' | 'ALIAS' | 'Alias';
TIMESERIES : 'timeseries' | 'TIMESERIES' | 'Timeseries' ;
EXTERNAL : 'external' | 'EXTERNAL' | 'External' ;
TEMPLATE : 'template' ;


SUM : 'sum' | 'SUM' | 'Sum';
KIND : 'kind' | 'KIND' | 'Kind';
UNITS : 'units' | 'UNITS' | 'Units' ;
NoSolver : 'NoSolver' | 'Nosolver' | 'NOSOLVER' | 'nosolver' | 'noSolver';
CONVERT : 'convert' | 'CONVERT' |'Convert' ;
UPPER : 'upper' | 'UPPER' | 'Upper';
LOWER : 'lower' | 'LOWER' | 'Lower';

UNBOUNDED : 'unbounded'|'UNBOUNDED' ;

ALWAYS: 'always'|'ALWAYS'|'Always' ;
INTEGER : 'integer'|'INTEGER'|'Integer';
BINARY : 'binary'|'BINARY'|'Binary';

SELECT : 'select' | 'SELECT'|'Select' ;
FROM   : 'from' | 'FROM'|'From' ;
WHERE  : 'where' | 'WHERE'|'Where' ;
GIVEN  : 'given' | 'GIVEN'|'Given' ;
USE    : 'use' | 'USE'|'Use' ;

LHS : 'lhs' | 'LHS' ;
RHS : 'rhs' | 'RHS' ;

// special function
RANGE : 'range' | 'RANGE' | 'Range' ;

// intrinsic function
INT_word : 'int' | 'INT' ;
ROUND : 'round' | 'ROUND' ;
LOG : 'log' | 'LOG' ;
MAX : 'max' | 'MAX' ;
MIN : 'min' | 'MIN' ;
MOD : 'mod' | 'MOD' ;
SIN : 'sin' | 'SIN' ;
COS : 'cos' | 'COS' ;
TAN : 'tan' | 'TAN' ;
COT : 'cot' | 'COT' ;
ASIN : 'asin' | 'ASIN' ;
ACOS : 'acos' | 'ACOS' ;
ATAN : 'atan' | 'ATAN' ;
ACOT : 'acot' | 'ACOT' ;
EXCEEDANCE : 'exceedance' | 'EXCEEDANCE' ;
EXCEEDANCE_TSI : 'exceedance_tsi' | 'EXCEEDANCE_TSI' ;
CFS_TAF : 'cfs_taf' | 'CFS_TAF' ;
TAF_CFS : 'taf_cfs' | 'TAF_CFS' ;

// network
NETWORK : 'network' | 'NETWORK' | 'Network' ;
NODE    : 'node' | 'NODE' | 'Node' ;
INFLOW  :  '*>>' ;
OUTFLOW :  '>>*' ;
FLOW    :  '>>'  ;

//ID  :   ('a'..'z'|'A'..'Z')('a'..'z'|'A'..'Z'|'0'..'9'|'_')* ;
ID : Letter ( Letter | Digit | '_' )*;


fragment Letter : 'a'..'z' | 'A'..'Z';

fragment Digit : '0'..'9';

WS : (' ' | '\t' | '\n' | '\r' | '\f')+ {skip();}; //{$channel = HIDDEN;};
//WS : ( '\t' | '\n' | '\r' | '\f')+ {skip();}; //{$channel = HIDDEN;};
//WS2 : ( ' ' )+ {$channel = HIDDEN;};
