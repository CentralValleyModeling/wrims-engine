tree grammar WreslTreeWalker;
options {
  language = Java;
  tokenVocab = WreslTree;
  ASTLabelType = CommonTree;
}
@header {
  package wrimsv2.wreslparser.grammar;
  import java.util.Map;
  import java.util.HashMap;
  import wrimsv2.wreslparser.elements.StructTree;
  import wrimsv2.wreslparser.elements.SimulationDataSet;
  import wrimsv2.wreslparser.elements.Tools;
  import wrimsv2.wreslparser.elements.LogUtils; 
  import wrimsv2.commondata.wresldata.Param;
  import wrimsv2.commondata.wresldata.Goal;
  import wrimsv2.commondata.wresldata.Svar;
}
@members {

  public int result;
  public CommonTree commonTree;
  public String currentAbsolutePath;
  public String currentAbsoluteParent;
  
  	public StructTree F = new StructTree();	
  	public SimulationDataSet thisFileDataSet = new SimulationDataSet();
  	private SimulationDataSet S;
  	  	
  	public Map<String, SimulationDataSet> modelDataMap = new HashMap<String, SimulationDataSet>();  	
  		/// error message	
    public void displayRecognitionError(String[] tokenNames,
                                        RecognitionException e) {
        String hdr = getErrorHeader(e);
        String msg = getErrorMessage(e, tokenNames);
        LogUtils.errMsg(hdr + " " + msg);
    }
}
evaluator
@init { 
		F.S = thisFileDataSet;
		F.S.currentAbsolutePath = currentAbsolutePath;
		F.S.currentAbsoluteParent = currentAbsoluteParent;
	  }
	:	
	(       pattern+ 
	|     ( sequence+  model+ ) 
	)
	     EOF
	;

pattern
	: dvar | svar | goal | includeFile | alias | weight_table | external | integer
	| svar_timeArray
	;

integer
	: integer_std | integer_nonStd | integer_timeArray_std | integer_timeArray_nonStd 
	; 

integer_std
	:  ^(Dvar_integer sc=Scope i=IDENT k=Kind u=Units )
	    {F.dvarStd($i.text, $sc.text, "integer", Tools.strip($k.text), Tools.strip($u.text)); }
	; 
	
integer_nonStd
	:  ^(Dvar_integer sc=Scope i=IDENT Lower lowerbound=LimitType Upper upperbound=LimitType k=Kind u=Units )
	    {F.dvarNonStd($i.text, $sc.text, "integer", Tools.strip($k.text), Tools.strip($u.text),  $lowerbound.text, $upperbound.text); }
	; 	

integer_timeArray_std
  :  ^(Dvar_integer ta=TimeArraySize sc=Scope i=IDENT k=Kind u=Units )
      {F.dvarStd($i.text, $sc.text, "integer", Tools.strip($k.text), Tools.strip($u.text), $ta.text); }
  ; 
  
integer_timeArray_nonStd
  :  ^(Dvar_integer ta=TimeArraySize sc=Scope i=IDENT Lower lowerbound=LimitType Upper upperbound=LimitType k=Kind u=Units )
      {F.dvarNonStd($i.text, $sc.text, "integer", Tools.strip($k.text), Tools.strip($u.text),  $lowerbound.text, $upperbound.text, $ta.text); }
  ; 

external 
	:  ^(External sc=Scope i=IDENT e=Expression )
		{F.external($i.text, $sc.text, $e.text);}
	;

sequence 
	:  ^(Sequence s=IDENT Model m=IDENT Order i=INTEGER c=Condition t=TIMESTEP) 
		{
			F.sequenceOrder($s.text, $i.text, $m.text, $c.text, $t.text);
			
			SimulationDataSet M = new SimulationDataSet();
			M.currentAbsolutePath=currentAbsolutePath;
			M.currentAbsoluteParent=currentAbsoluteParent;
			modelDataMap.put($m.text.toLowerCase(), M);
		}


	;

model
@after{ F.S = thisFileDataSet; }
	: ^(Model i=IDENT  			
				{   
					F.S = thisFileDataSet; F.modelList($i.text.toLowerCase()); 
					
				    F.S = modelDataMap.get($i.text.toLowerCase());
				} 
				
				(pattern )+ ) 
	;	

includeFile
	:	 ^(Include sc=Scope f=FILE_PATH) {F.includeFile(Tools.strip($f.text), $sc.text);}
	;
	
weight_table
	:  ^(Weight_table sc=Scope ( ^(i=IDENT ta=TimeArraySize e=Expression    
	{ 	     
	     F.mergeWeightTable($i.text, $e.text, $sc.text, $ta.text);
	    }
	) )+  ) 
	;	

	
//assignment
//	:	^(':=' IDENT e=expression)
//			{ variables.put($IDENT.text, e); }
//	;
goal 
scope { String ta; String scop ; Goal gl; String case_condition; int caseNumber; Map<String, String> dvarWeightMap; ArrayList<String> dvarSlackSurplus;} 
@init { $goal::ta=null; $goal::scop = null; $goal::gl = new Goal(); $goal::caseNumber=0; $goal::case_condition="always";} 
: goal_simple | goal_nocase | goal_case ;

dvar : dvar_std | dvar_nonStd | dvar_timeArray_std | dvar_timeArray_nonStd   ;

svar : svar_dss | svar_expr | svar_sum | svar_table | svar_case;

svar_timeArray : svar_timeArray_expr | svar_timeArray_case |svar_timeArray_table | svar_timeArray_sum;

svar_timeArray_case
@init { Svar sv = new Svar(); String dependants=null; String varInCycle=null;}  
	: ^(SvarTimeArray_case ta=TimeArraySize tad=Dependants sc=Scope i=IDENT  ( c=case_content 
	{	
				sv.caseName.add($c.name.toLowerCase());
				sv.caseCondition.add( Tools.add_space_between_logical( $c.condition.toLowerCase() ) );
				sv.caseExpression.add($c.expression.toLowerCase());
				dependants = dependants + " " + $c.dependants;
				varInCycle = varInCycle + " " + $c.varInCycle;
			}
	
	)+ 
	) 
			 {
			 	F.svarCase($i.text, $sc.text, sv, dependants+" "+$tad.text, varInCycle, $ta.text); }
;

svar_case 	
@init { Svar sv = new Svar(); String dependants=null; String varInCycle=null;}  
	: ^(Svar_case sc=Scope i=IDENT  ( c=case_content 
	{	
				sv.caseName.add($c.name.toLowerCase());
				sv.caseCondition.add( Tools.add_space_between_logical( $c.condition.toLowerCase() ) );
				sv.caseExpression.add($c.expression.toLowerCase());
				dependants = dependants + " " + $c.dependants;
				varInCycle = varInCycle + " " + $c.varInCycle;
			}
	
	)+ 
	) 
			 {
			 	F.svarCase($i.text, $sc.text, sv, dependants, varInCycle); }
;


case_content returns[String name, String condition, String expression, String dependants, String varInCycle]  
@init{ String expr = null;} :
^(Case i=IDENT c=Condition d=Dependants cvc=VarInCycle
 
 ( t=table_content {expr =$t.text;}  // todo: add dependants
 | v=Value  vd=Dependants vvc=VarInCycle {expr =$v.text; }
 | sum=sum_content {expr =$sum.hdr+" "+$sum.expr; } 
 ) )
 
{ $name = $i.text; $condition =$c.text; $expression = expr; 
  $dependants = $d.text + " " + $t.dependants + " " + $vd.text + " " + $sum.dependants; 
  $varInCycle = $cvc.text + " "+ $t.varInCycle + " " + $sum.varInCycle + " " + $vvc.text;

}


;


//table_content returns[String text] :  
//^( s=Select f=From (g=Given u=Use)? wc=Where_content )
//{ $text = "SELECT "+$s.text+" FROM "+$f.text+" GIVEN "+$g.text+" USE "+$u.text+
//   " WHERE "+ Tools.replace_ignoreChar(Tools.replace_seperator($wc.text)); }
//;

table_content returns[String text, String dependants, String varInCycle] 
	: 
 ^( SELECT s=IDENT FROM f=IDENT   {$text = "select "+$s.text+" from "+$f.text; }
  (GIVEN g=Assignment d=Dependants vc=VarInCycle USE u=IDENT {$text = $text+" given "+$g.text+" use "+$u.text;} )? 
  (WHERE w=where_items wd=Dependants            {$text = $text+" where "+ Tools.replace_ignoreChar(Tools.replace_seperator($w.text)); } )?
  )
  {$dependants = $d.text +" " + $wd.text;  $varInCycle = $vc.text;    }
	;

where_items returns[String text]
	: a=Assignment {$text = $a.text;} (',' b=Assignment {$text = $text+","+$b.text;})*
	;


alias  : (alias_simple|alias_timeArray_simple);

alias_simple: ^(Alias sc=Scope i=IDENT e=Expression k=Kind u=Units d=Dependants vc=VarInCycle)
       { F.alias($i.text, $sc.text, Tools.strip($k.text), Tools.strip($u.text), $e.text, $d.text, $vc.text  ); }
	;

alias_timeArray_simple: ^(Alias ta=TimeArraySize sc=Scope i=IDENT e=Expression k=Kind u=Units d=Dependants vc=VarInCycle)
       { F.alias($i.text, $sc.text, Tools.strip($k.text), Tools.strip($u.text), $e.text, $d.text, $vc.text, $ta.text); }
  ;
	
goal_simple 
  :  ^(Goal_simple (ta=TimeArraySize{$goal::ta=$ta.text;})? sc=Scope {$goal::scop = $sc.text;} i=IDENT d=Dependants v=Constraint_content vc=VarInCycle) 
    { 
      if ($ta==null){
        F.goalSimple($i.text, $sc.text, $v.text, $d.text, $vc.text);
      }else{
        F.goalSimple($i.text, $sc.text, $v.text, $d.text, $vc.text, $ta.text);
      }
     } 
  ;

goal_nocase
  @init { $goal::gl = new Goal(); $goal::caseNumber=0; $goal::case_condition="always";} 
  :  ^( Goal_no_case (ta=TimeArraySize{$goal::ta=$ta.text;})? sc=Scope {$goal::scop = $sc.text;} i=IDENT  d=Dependants vc=VarInCycle 
      {   
        if ($ta==null){
          $goal::gl = F.goalSimple($i.text, $sc.text, "", $d.text, $vc.text);
        }else{
          $goal::gl = F.goalSimple($i.text, $sc.text, "", $d.text, $vc.text, $ta.text);
        }
        $goal::gl.dvarWeightMapList.add(null);
        $goal::gl.dvarSlackSurplusList.add(null);
        $goal::dvarWeightMap = new HashMap<String, String>(); 
        $goal::dvarSlackSurplus = new ArrayList<String>();         
        //$goal::gl.dvarName.add(""); $goal::gl.dvarWeight.add("");
      }
      c=goal_contents  )
        
    { 
       $goal::gl.caseExpression.set(0, $c.str.toLowerCase());             
    } 
;

goal_case
	@init { $goal::gl = new Goal(); $goal::caseNumber=0; $goal::case_condition=Param.conditional; }   
	:  ^( Goal_case (ta=TimeArraySize{$goal::ta=$ta.text;})? sc=Scope {$goal::scop = $sc.text;} i=IDENT  
		( 
			{ 	$goal::gl.dvarWeightMapList.add(null);	
				$goal::gl.dvarSlackSurplusList.add(null);	
				$goal::dvarWeightMap = new HashMap<String, String>();
				$goal::dvarSlackSurplus = new ArrayList<String>();	
				//$goal::gl.dvarName.add(""); $goal::gl.dvarWeight.add(""); 
			}
			
			^( Case n=IDENT c=Condition d=Dependants vc=VarInCycle e=goal_contents 
			{	$goal::caseNumber++;
				$goal::dvarWeightMap = new HashMap<String, String>();	
				$goal::dvarSlackSurplus = new ArrayList<String>();	
				$goal::gl.caseName.add($n.text.toLowerCase());
				$goal::gl.caseCondition.add( Tools.add_space_between_logical( $c.text.toLowerCase() ) );
				$goal::gl.caseExpression.add($e.str.toLowerCase());
				if (d != null) {
					String dependants = $d.text.toLowerCase();
					$goal::gl.expressionDependants.addAll(Tools.convertStrToSet(dependants));
					$goal::gl.expressionDependants.removeAll(Param.reservedSet);
				}
				if (vc != null) {
					String varInCycle = $vc.text.toLowerCase();
					$goal::gl.neededVarInCycleSet.addAll(Tools.convertStrToSet(varInCycle));
					$goal::gl.needVarFromEarlierCycle = true;
				}
			} 
		) )+  
		)  
		{ 
			 if ($ta==null){
			   F.goalCase($i.text, $sc.text, $goal::gl);
			 }else{
			   F.goalCase($i.text, $sc.text, $goal::gl, $ta.text);  
			 }	  				
		} 
;

goal_contents returns[String str] 
		: ( g=goal_contents_process_2 |g=goal_contents_process_1 | g=goal_content_simple )
			
			{$str = $g.str;}
		;

goal_contents_process_1 returns[String str] : t=One c1=goal_content 
		{ 
				// c -> =
				// f -> f
				// p -> =
		  
		  		if ( $t.text.equals("c") ) { 
		  		
		   			$str = $c1.lhs + "=" + $c1.rhs ; 
		  		
		  		} else if ( $t.text.equals("f") ) { 
		  		
		   			$str = $c1.str ;  
		   			
		   		} else {  
		   		
		   			$str = $c1.lhs + $c1.ss + "=" + $c1.rhs ;  
		   		
		   		}		  				
			
	}
		;

goal_contents_process_2 returns[String str] : t=Two c1=goal_content c2=goal_content
		{ 
		      // p: penalty f: free c: constrain
		      // pp -> =
		      // pc or cp -> =
		      // pf or fp  -> f
		      // fc or cf  -> f (same as c)		      
		      // cc -> =  (simple string)
		      // ff -> error

		   
		  		if ( $t.text.equals("pp") ) { 
		  				
		  			$str = $c1.lhs + $c1.ss + $c2.ss + "=" + $c1.rhs ;
		  			 
		  		} else if  ( $t.text.equals("pc") ) {
		  		
		  			$str = $c1.lhs + $c1.ss + "=" + $c1.rhs ; 
		  			
		  		} else if  ( $t.text.equals("cp") ) {
		  		
		  			$str = $c2.lhs + $c2.ss + "=" + $c2.rhs; 	

		  		} else if  ( $t.text.equals("pf") ) {
		  		
		  			$str = $c1.lhs + $c1.ss + $c2.op + $c1.rhs ; 			
		  		
		  		} else if ( $t.text.equals("fp"))  {
		  		
		  			$str = $c2.lhs + $c2.ss + $c1.op + $c2.rhs; 
		  		
		  		} else if ( $t.text.equals("fc"))  {  
		  		
		  			$str = $c1.lhs + $c1.op + $c1.rhs ;
		  			
		  		} else if ( $t.text.equals("cf"))  {  
		  		
		  			$str = $c1.lhs + $c2.op + $c1.rhs ;
		  					  		
		  			
		  		} else if ( $t.text.equals("cc"))  {  
		  		
		  			$str = $c1.lhs+"="+$c1.rhs ;
		  			
		  		} else  {  // ff
		  		
		  			$str = " 1 > 0 ";  // do something
		  		}
		  

			
	}
		;

goal_content returns[boolean hasDvar, String str, String ss, String weight, String lhs, String rhs, String type, String op]
@init{$hasDvar=false; String kind=null; $type="constrain";}
	: 
		 ( Constrain | Free | Penalty )   l=Lhs o=Op r=Rhs   ( Sign  Kind  s=Slack_Surplus  w=Weight  )? 
		 
		 {  $str = $l.text + $o.text + $r.text; 
		 
		    if (s!=null) { 
		    	
		    	if ($goal::case_condition.equalsIgnoreCase(Param.conditional)){
		    		
		    		$goal::dvarWeightMap.put($s.text.toLowerCase(), $w.text.toLowerCase());
		    		$goal::dvarSlackSurplus.add($s.text.toLowerCase());
		    		$goal::gl.dvarWeightMapList.set($goal::caseNumber,$goal::dvarWeightMap);	
		    		$goal::gl.dvarSlackSurplusList.set($goal::caseNumber,$goal::dvarSlackSurplus);	
		    	}
		    	//System.out.println($s.text.toLowerCase()+": "+$w.text.toLowerCase());
		    	//$goal::gl.dvarName.set($goal::caseNumber, $s.text.toLowerCase());
		    	//$goal::gl.dvarWeight.set($goal::caseNumber, $w.text.toLowerCase());
		    	//F.dvarStd($s.text, $goal::scop, null, $Kind.text, "");  
		    	if ($goal::ta==null){ 
            $ss = $Sign.text + $s.text;
            F.dvarSlackSurplus($s.text, $goal::scop, $Kind.text, "", $goal::case_condition, "0");
            F.mergeSlackSurplusIntoWeightTable($s.text, $w.text, $goal::scop, $goal::case_condition, "0");
          }else if ($goal::ta.equals("0")){
            $ss = $Sign.text + $s.text;
            F.dvarSlackSurplus($s.text, $goal::scop, $Kind.text, "", $goal::case_condition, "0");
            F.mergeSlackSurplusIntoWeightTable($s.text, $w.text, $goal::scop, $goal::case_condition, "0");
          }else{
            $ss = $Sign.text + $s.text+"(\$m)";
            F.dvarSlackSurplus($s.text, $goal::scop, $Kind.text, "", $goal::case_condition, $goal::ta);
            F.mergeSlackSurplusIntoWeightTable($s.text, $w.text, $goal::scop, $goal::case_condition, $goal::ta);
          } 
		 		$hasDvar = true; 
		 		$weight = $w.text; }
		 	//} else {
		 	
		 	 	$lhs = $l.text; $rhs = $r.text; $op = $o.text;
		 	//}
		 } 
	;

goal_content_simple returns[String str]
	: 
		  Simple  l=Lhs o=Op r=Rhs  
		 {  $str = $l.text + $o.text + $r.text; } 
	;

svar_table :
	^( Svar_table sc=Scope i=IDENT t=table_content ) 
	 { F.svarTable($i.text, $sc.text, $t.text, $t.dependants, $t.varInCycle); } 
	;
	
svar_timeArray_table :
  ^( Svar_table ta=TimeArraySize sc=Scope i=IDENT t=table_content ) 
   { F.svarTable($i.text, $sc.text, $t.text, $t.dependants, $t.varInCycle, $ta.text); } 
  ;

svar_sum : 
    ^(Svar_sum sc=Scope i=IDENT sum=sum_content )
     { F.svarSum($i.text, $sc.text, $sum.hdr, $sum.expr, $sum.dependants, $sum.varInCycle ); }
  ;
  
svar_timeArray_sum : 
		^(Svar_sum ta=TimeArraySize sc=Scope i=IDENT sum=sum_content )
	   { F.svarSum($i.text, $sc.text, $sum.hdr, $sum.expr, $sum.dependants, $sum.varInCycle, $ta.text ); }
	;
	
sum_content returns[String hdr, String expr, String dependants, String varInCycle]: 
^(h=Sum_hdr e=Expression d=Dependants vc=VarInCycle) 
	{ 
		$hdr="SUM"+Tools.replace_ignoreChar( Tools.replace_seperator($h.text)); 
    	$expr = $e.text;
    	$dependants = $d.text;
    	$varInCycle = $vc.text;
    }
;	

svar_expr : 
	   ^(Svar_const sc=Scope i=IDENT v=Expression d=Dependants vc=VarInCycle)
	   { F.svarExpression($i.text, $sc.text, Tools.replace_seperator($v.text), $d.text, $vc.text ); }
	;

svar_timeArray_expr : 
	   ^(SvarTimeArray_const ta=TimeArraySize sc=Scope i=IDENT v=Expression d=Dependants vc=VarInCycle)
	   { F.svarExpression($i.text, $sc.text, Tools.replace_seperator($v.text), $d.text, $vc.text, $ta.text ); }
	;

svar_dss :
       ^(Svar_dss sc=Scope i=IDENT b=B_part Kind k=STRING Units u=STRING c=CONVERT )
       { F.timeseriesDss($i.text, $sc.text, Tools.strip($b.text), Tools.strip($k.text), Tools.strip($u.text),  Tools.strip($c.text)); }
	;

dvar_std  :
       ^(Dvar_std sc=Scope i=IDENT Kind k=STRING Units u=STRING)
       { F.dvarStd($i.text, $sc.text, null, Tools.strip($k.text), Tools.strip($u.text)); }
	;

dvar_nonStd : 
	   ^(Dvar_nonStd sc=Scope i=IDENT Lower lowerbound=LimitType Upper upperbound=LimitType Kind k=STRING Units u=STRING)
	   {F.dvarNonStd($i.text, $sc.text, null, Tools.strip($k.text), Tools.strip($u.text),  $lowerbound.text, $upperbound.text);}
	;

dvar_timeArray_std  :
       ^(DvarTimeArray_std ta=TimeArraySize sc=Scope i=IDENT Kind k=STRING Units u=STRING)
       { F.dvarStd($i.text, $sc.text, null, Tools.strip($k.text), Tools.strip($u.text), $ta.text ); }
	;
			
dvar_timeArray_nonStd : 
	   ^(DvarTimeArray_nonStd ta=TimeArraySize sc=Scope i=IDENT Lower lowerbound=LimitType Upper upperbound=LimitType Kind k=STRING Units u=STRING)
	   {F.dvarNonStd($i.text, $sc.text, null, Tools.strip($k.text), Tools.strip($u.text),  $lowerbound.text, $upperbound.text, $ta.text);}
	;
	
/// Expression ///
term
	:	IDENT
	|	'(' expression ')'
	|	INTEGER
	;
	
unary :	NEGATION? term 	;

mult :	unary (('*' | '/' ) unary)* 	;
	
expression :	mult (('+' | '-') mult)*	;
	
c_term
	: ( expression relation expression ) => expression relation expression
	| ( '(' logical ')' ) => '(' logical ')' 
	;	

c_unary :	NOT? c_term  	;

logical :  c_unary ( bin c_unary )* ;  
	
relation : '>' | '<' | '>=' | '<=' | '==' | '/=' ;	

bin : OR | AND ;	
	
/// End Expression /// 

