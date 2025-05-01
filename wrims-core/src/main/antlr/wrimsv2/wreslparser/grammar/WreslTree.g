
grammar WreslTree;

options {
  language = Java;
  output=AST;
  ASTLabelType=CommonTree;
}
tokens {
	NEGATION;
	NEW_LINE; Op; Separator; Slack_Surplus; Sign; Constrain; Free; Simple; One; Two;
	Weight_table; Assignment; External;
	Local; Global; Scope;
	Value; Case ; Dependants; VarInCycle;
	Alias; Expression;
	Dvar; Dvar_std; Dvar_nonStd; Dvar_std; Dvar_nonStd_local; Dvar_integer;
	DvarTimeArray_std; DvarTimeArray_nonStd;  
	TimeArraySize; 
	SvarTimeArray_const; SvarTimeArray_case;
	Svar_case; Svar_dss; Svar_const; Svar_sum; Sum_hdr; B_part;
	Svar_table; Select; From; Where_content; Where_item_number; Given; Use;
	Goal_simple; Goal_no_case; Goal_case ; Lhs_gt_rhs; Lhs_lt_rhs; Never; Penalty;
	Lhs; Rhs; Weight;
	Constraint_content;
	Model;
	Sequence;
	Condition;
	Order;
	Kind; Units;
	Lower='Lower'; Upper='Upper';
	Std='Std'; Unbounded='Unbounded';	
	Exp='Exp';
	Include;
	//Or='.Or.'; And='.And.'; Not='.Not.';
	Always;
	LimitType;
}
@header {
  package wrimsv2.wreslparser.grammar;
  import wrimsv2.wreslparser.elements.LogUtils; 
  import wrimsv2.wreslparser.elements.Tools;
  import wrimsv2.wreslplus.elements.VarCycleIndex; 
  import wrimsv2.commondata.wresldata.Param;
  import java.util.Set;
  import java.util.HashSet;
  import java.util.Arrays;
}
@lexer::header {
  package wrimsv2.wreslparser.grammar;
  import wrimsv2.wreslparser.elements.LogUtils; 
  
}
@members {
	
	public String wresl_version = null;
    public ArrayList<String> model_in_sequence = new ArrayList<String>();
    public ArrayList<String> model_list = new ArrayList<String>();
    
    public CommonTree commonTree;
  	public String currentAbsolutePath;
  	public String currentAbsoluteParent;
  		public boolean sometest(String name) {
		
			return true;
			}
  	
	/// error message	
    public void displayRecognitionError(String[] tokenNames,
                                        RecognitionException e) {
        String hdr = getErrorHeader(e);
        String msg = getErrorMessage(e, tokenNames);
        LogUtils.errMsg(hdr + " " + msg);
    }	
}
@lexer::members {
	List tokens = new ArrayList();
	public void emit(Token token) {
	        state.token = token;
	    	tokens.add(token);
	}
	public Token nextToken() {
	    	super.nextToken();
	        if ( tokens.size()==0 ) {
	            return getEOFToken();
	        }
	        return (Token)tokens.remove(0);
	}
	
	/// error message	
    public void displayRecognitionError(String[] tokenNames,
                                        RecognitionException e) {
        String hdr = getErrorHeader(e);
        String msg = getErrorMessage(e, tokenNames);
        LogUtils.errMsg(hdr + " " + msg);
    }
}

mainFile
	: sequence+ model+
	  EOF! 
	;

version_tag : '[' 'wresl_version' vn=version_number ']' {wresl_version=$vn.text;} ;
	
version_number : FLOAT;

evaluator
	: pattern+ 
	  EOF!
	;

pattern
	: dvar | svar | goal | includeFile | alias | weight_table | external | integer
	| svar_timeArray
	;

integer
	: integer_std | integer_nonStd | integer_timeArray_std | integer_timeArray_nonStd 
	;

// this is actually a binary
integer_std
	: DEFINE ( '[' sc=LOCAL? ']' )? i=IDENT '{' INTEGER_WORD STD KIND k=STRING UNITS u=STRING '}'
		->  ^(Dvar_integer  Scope[$sc.text] $i Kind[$k.text] Units[$u.text]) 
	;

integer_nonStd
	: DEFINE ( '[' sc=LOCAL? ']' )? i=IDENT '{' INTEGER_WORD lower_and_or_upper KIND k=STRING UNITS u=STRING '}'
		->  ^(Dvar_integer  Scope[$sc.text] $i lower_and_or_upper Kind[$k.text] Units[$u.text]) 
	;
	
integer_timeArray_std
  : DEFINE '(' ta=timeArraySize ')' ( '[' sc=LOCAL? ']' )? i=IDENT '{' INTEGER_WORD STD KIND k=STRING UNITS u=STRING '}'
    ->  ^(Dvar_integer TimeArraySize[$ta.text] Scope[$sc.text] $i Kind[$k.text] Units[$u.text]) 
  ;

integer_timeArray_nonStd
  : DEFINE '(' ta=timeArraySize ')' ( '[' sc=LOCAL? ']' )? i=IDENT '{' INTEGER_WORD lower_and_or_upper KIND k=STRING UNITS u=STRING '}'
    ->  ^(Dvar_integer TimeArraySize[$ta.text] Scope[$sc.text] $i lower_and_or_upper Kind[$k.text] Units[$u.text]) 
  ;

external : DEFINE ( '[' sc=LOCAL? ']' )? i=IDENT '{' EXTERNAL (e=DLL|e=F90) '}'
-> ^(External Scope[$sc.text] $i Expression[$e.text]   )
;

	
weight_table
	: OBJECTIVE ( '[' sc=LOCAL? ']' )? IDENT '=' '{'  w+=weightItem+ '}'
	-> ^(Weight_table Scope[$sc.text] $w+  ) 
	;	

weightItem
@init{String r="0";}
	: '['  IDENT ('(' ta=timeArraySize ')' { r = $ta.text;} )? ',' e=expression ']' (',')? 
	   ->  ^(IDENT TimeArraySize[r] Expression[$e.text])
	;
		
model
@init { String id = null;}
	: MODEL i=IDENT '{' (pattern )+  '}' 
	   {id = $i.text.toLowerCase();  
	    model_list.add(id);}
	-> {model_in_sequence.contains(id)}?  ^(Model IDENT[id]  (pattern )+  ) 
	->   // ignore
	;
sequence
@init { String id = null;String timeStep=Param.undefined;} 
	: SEQUENCE s=IDENT '{' MODEL m=IDENT ( c=condition)? ORDER INTEGER (TIMESTEP t=TIMESTEPVALUE{timeStep=$t.text.toUpperCase();})? '}' 
	  {id = $m.text.toLowerCase();
	  	model_in_sequence.add(id);}
	->  ^(Sequence $s Model IDENT[id] Order INTEGER Condition[$c.text] TIMESTEP[timeStep])	 
	;
	
condition returns[String text, String dependants, String varInCycle]
	: CONDITION 
	( e=logical_expr {$text=$e.text; $dependants=$e.dependants; $varInCycle=$e.strVarInCycle;}
	| ALWAYS   {$text = Param.always; }
	)
	;	

includeFile
	:	 INCLUDE ( '[' sc=LOCAL? ']' )? FILE_PATH 
	->   ^(Include Scope[$sc.text] FILE_PATH)
	;
	
alias: DEFINE! (alias_simple|alias_timeArray_simple);

alias_simple : ( '[' sc=LOCAL? ']' )? i=IDENT '{' ALIAS e=expression (KIND k=STRING)? (UNITS u=STRING)? '}'
	->  ^(Alias Scope[$sc.text] $i Expression[$e.text] Kind[$k.text] Units[$u.text] Dependants[$e.dependants] VarInCycle[$e.strVarInCycle])
	;	

alias_timeArray_simple : '(' ta=timeArraySize ')' ( '[' sc=LOCAL? ']' )? i=IDENT '{' ALIAS e=expression (KIND k=STRING)? (UNITS u=STRING)? '}'
  ->  ^(Alias TimeArraySize[$ta.text] Scope[$sc.text] $i Expression[$e.text] Kind[$k.text] Units[$u.text] Dependants[$e.dependants] VarInCycle[$e.strVarInCycle])
  ; 

goal
scope { String goalName; String caseName; int caseNumber;} 
@init{ $goal::caseNumber = 0; }
	: GOAL! (goal_simple | goal_case_or_nocase | goal_timeArray_simple | goal_timeArray_case_or_nocase  )	
	;

goal_simple
	:  ( '[' sc=LOCAL? ']' )? i=IDENT '{' v=constraint_statement '}'	
->  ^(Goal_simple Scope[$sc.text] $i Dependants[$v.dependants] Constraint_content[Tools.replace_ignoreChar($v.text)] VarInCycle[$v.varInCycle])  		
	;
	
goal_timeArray_simple
  :  '(' ta=timeArraySize ')' ( '[' sc=LOCAL? ']' )? i=IDENT '{' v=constraint_statement '}'  
->  ^(Goal_simple TimeArraySize[$ta.text] Scope[$sc.text] $i Dependants[$v.dependants+" "+$ta.dependant] Constraint_content[Tools.replace_ignoreChar($v.text)] VarInCycle[$v.varInCycle])     
  ;

goal_case_or_nocase 
	:  ( '[' s=LOCAL? ']' )? i=IDENT { $goal::goalName = $i.text;  } 
	'{' LHS l=expression 
	( 
	  ( goal_no_case_content[$l.text, $l.dependants, $l.strVarInCycle] ->  ^( Goal_no_case Scope[$s.text] $i goal_no_case_content )  ) 	
    | ( goal_case_content[$l.text, $l.dependants, $l.strVarInCycle]+   ->  ^( Goal_case    Scope[$s.text] $i goal_case_content+ )   )
    ) '}' 
	;
	
goal_timeArray_case_or_nocase 
  :  '(' ta=timeArraySize ')' ( '[' s=LOCAL? ']' )? i=IDENT { $goal::goalName = $i.text;  } 
  '{' LHS l=expression 
  ( 
    ( goal_no_case_content[$l.text, $ta.dependant+" "+$l.dependants, $l.strVarInCycle] ->  ^( Goal_no_case TimeArraySize[$ta.text] Scope[$s.text] $i goal_no_case_content )  )  
    | ( goal_case_content[$l.text, $ta.dependant+" "+$l.dependants, $l.strVarInCycle]+   ->  ^( Goal_case  TimeArraySize[$ta.text] Scope[$s.text] $i goal_case_content+ )   )
    ) '}' 
  ;

goal_case_content[String l, String d, String vc] 
@init{ String varInCycle_nullsRemoved = null; String dependants_nullsRemoved = null; }
	: CASE i=IDENT { $goal::caseName = $i.text;  $goal::caseNumber++; } 
	'{' c=condition RHS r=expression (s=sub_content[$l,$r.text])? '}'
	{ varInCycle_nullsRemoved =   Tools.remove_nulls($vc+" "+$r.strVarInCycle+" "+$c.varInCycle);
	  dependants_nullsRemoved =   Tools.remove_nulls($d+" "+$r.dependants+" "+$c.dependants);   
	}
	-> {s!=null}? ^( Case $i Condition[$c.text] Dependants[dependants_nullsRemoved] VarInCycle[varInCycle_nullsRemoved] $s )
	->            ^( Case $i Condition[$c.text] Dependants[dependants_nullsRemoved] VarInCycle[varInCycle_nullsRemoved] Simple Lhs[$l] Op["="] Rhs[$r.text] )
	;

goal_no_case_content[String l, String d, String vc] 
@init{ $goal::caseName = "default";   $goal::caseNumber=1;}
	: RHS r=expression (s=sub_content[$l,$r.text])?
       -> {s!=null}? Dependants[$d+" "+$r.dependants] VarInCycle[$vc+" "+$r.strVarInCycle] $s 
       ->            Dependants[$d+" "+$r.dependants] VarInCycle[$vc+" "+$r.strVarInCycle] Simple Lhs[$l] Op["="] Rhs[$r.text] 
       ;
	
sub_content[String l, String r] 
	: ( a=lhs_gt_rhs[$l,$r] ( b=lhs_lt_rhs[$l,$r])? 
		-> {b==null}? One[$a.type] $a
		->            Two[$a.type+$b.type] $a $b
		) 
		
	| ( c=lhs_lt_rhs[$l,$r] ( d=lhs_gt_rhs[$l,$r] -> Two[$c.type+$d.type] $c $d )? 
		-> {d==null}? One[$c.type] $c
		->            Two[$c.type+$d.type] $c $d	
		) 
	; 
	
lhs_gt_rhs[String l, String r] returns[String type] 
@init{ String penalty2Weight = "";}
	: 
LHS '>' RHS 
	( ( CONSTRAIN  { $type = "c"; } -> Constrain Lhs[$l] Op["<"] Rhs[$r] )
	| ( p=penalty { 
					if      ($p.isZero)       { $type = "f";   }
					else if ($p.isNegative)   { $type = "p"; penalty2Weight = $p.w; }
					else 		              { $type = "p"; penalty2Weight = "-"+$p.w; }
				  }							 
		-> {$p.isZero}? Free Lhs[$l] Op[">"] Rhs[$r]  
		-> 	  		    Penalty Lhs[$l] Op["="] Rhs[$r] Sign["-"] Kind["surplus"] Slack_Surplus["surplus__"+$goal::goalName+"_"+Integer.toString($goal::caseNumber)] Weight[penalty2Weight] 
		//-> 	  		    Penalty Lhs[$l] Op["="] Rhs[$r] Sign["-"] Kind["surplus"] Slack_Surplus["surplus_"+$goal::goalName+"_"+$goal::caseName] Weight["-"+$p.w] 
		)
	);

lhs_lt_rhs[String l, String r]  returns[String type]
@init{ String penalty2Weight = "";}
	: 
LHS '<' RHS 
	( ( CONSTRAIN  { $type = "c"; } -> Constrain Lhs[$l] Op[">"] Rhs[$r])
	| ( p=penalty { 
					if      ($p.isZero)       { $type = "f";   }
					else if ($p.isNegative)   { $type = "p"; penalty2Weight = $p.w; }
					else 		              { $type = "p"; penalty2Weight = "-"+$p.w; }					
				  }
		-> {$p.isZero}? Free Lhs[$l] Op["<"] Rhs[$r] 
		->              Penalty Lhs[$l] Op["="] Rhs[$r] Sign["+"] Kind["slack"] Slack_Surplus["slack__"+$goal::goalName+"_"+Integer.toString($goal::caseNumber)]  Weight[penalty2Weight]
		//->              Penalty Lhs[$l] Op["="] Rhs[$r] Sign["+"] Kind["slack"] Slack_Surplus["slack_"+$goal::goalName+"_"+$goal::caseName]  Weight["-"+$p.w]		
		)
	);

penalty returns[String w, boolean isZero, boolean isNegative]
	: PENALTY n=expression 
		{	$w=$n.text; 
			$isZero = false;
			$isNegative = false;
			
			try { 
				double p = Double.parseDouble($n.text); 
				if      ( p == 0 ) { $isZero = true;     $w = "0";}
				else if ( p < 0  ) { $isNegative = true; $w = $n.text.replace("-","");} 
				else               { $w = $n.text;} 
			}
			catch (Exception e) { 
			
				$w = "("+$n.text+")";
//				String ptext=$n.text;
//				Integer lineNumber=$pt.getLine();
//				Integer columnNumber=$pt.getCharPositionInLine();
//				if (ptext.substring(0,1).equals("-")) {
//					$isNegative = true;
//				    $w = ptext.substring(1,ptext.length());
//				}
			}
		} ;

svar : DEFINE! (svar_dss | svar_expr | svar_sum | svar_table | svar_case ) ;

svar_timeArray :  DEFINE! ( svar_timeArray_expr | svar_timeArray_case| svar_timeArray_table | svar_timeArray_sum ) ;
		
dvar : DEFINE! (dvar_std | dvar_nonStd ) ;	

svar_case : ( '[' sc=LOCAL? ']' )? i=IDENT '{' case_content+ '}'
->     ^(Svar_case Scope[$sc.text] $i  case_content+ )  ;

svar_timeArray_case : '(' ta=timeArraySize ')' ( '[' sc=LOCAL? ']' )? i=IDENT '{' case_content+ '}'
->     ^(SvarTimeArray_case TimeArraySize[$ta.text] Dependants[$ta.dependant] Scope[$sc.text] $i  case_content+ )  ;

case_content 
@init{ String dependants_nullsRemoved = ""; String varInCycle_nullsRemoved = ""; }	
	: CASE i=IDENT '{' c=condition 
	{   varInCycle_nullsRemoved =   Tools.remove_nulls($c.varInCycle);
		dependants_nullsRemoved =   Tools.remove_nulls($c.dependants); }

    ( table_content 
	-> ^(Case $i Condition[$c.text] Dependants[dependants_nullsRemoved] VarInCycle[varInCycle_nullsRemoved] table_content  )

	| value_content 
	-> ^(Case $i Condition[$c.text] Dependants[dependants_nullsRemoved] VarInCycle[varInCycle_nullsRemoved] value_content  )
	
	| sum_content
	-> ^(Case $i Condition[$c.text] Dependants[dependants_nullsRemoved] VarInCycle[varInCycle_nullsRemoved] sum_content  )
	
) '}' 
;

value_content : VALUE e=expression -> Value[$e.text] Dependants[$e.dependants]  VarInCycle[$e.strVarInCycle];

svar_table :
	( '[' sc=LOCAL? ']' )?  i=IDENT '{' table_content '}'
->  ^(Svar_table Scope[$sc.text] $i table_content )  	
	;
	
svar_timeArray_table :
  '(' ta=timeArraySize ')' ( '[' sc=LOCAL? ']' )? i=IDENT '{' table_content '}'
->  ^(Svar_table TimeArraySize[$ta.text] Scope[$sc.text] $i table_content )   
  ;

table_content : SELECT s=IDENT FROM f=IDENT 
				(GIVEN g=assignment USE u=IDENT)? 
				(WHERE w=where_items)? 
				
->  ^( SELECT $s FROM $f (GIVEN $g Dependants[$g.dependants] VarInCycle[$g.varInCycle]  USE $u)? (WHERE $w Dependants[$w.dependants])? )
;

where_items returns[String dependants]
	: ai=assignment (',' a=assignment {$dependants =$dependants + " " + $a.dependants; } )*
	 {$dependants =$dependants + " " + $ai.dependants; }
	;

svar_expr : 
	( '[' sc=LOCAL ']' )? IDENT '{' VALUE  e=expression'}'	
	->  ^(Svar_const Scope[$sc.text] IDENT Expression[$e.text] Dependants[$e.dependants] VarInCycle[$e.strVarInCycle])  
	;	

svar_timeArray_expr :
	'(' ta=timeArraySize ')' ( '[' sc=LOCAL ']' )? IDENT '{' VALUE  e=expression'}'	
	->  ^(SvarTimeArray_const TimeArraySize[$ta.text] Scope[$sc.text] IDENT Expression[$e.text] Dependants[$e.dependants+" "+$ta.dependant] VarInCycle[$e.strVarInCycle])  
	;

svar_sum : ( '[' sc=LOCAL ']' )? IDENT '{' sum_content '}' 
  ->  ^(Svar_sum  Scope[$sc.text] IDENT sum_content )  
  ;

svar_timeArray_sum : '(' ta=timeArraySize ')' ( '[' sc=LOCAL ']' )? IDENT '{' sum_content '}' 
	->  ^(Svar_sum  TimeArraySize[$ta.text] Scope[$sc.text] IDENT sum_content )  
	;

sum_content returns[Set<String> setVarInCycle] :SUM hdr=sum_header e=expression 
{
  $setVarInCycle = $e.setVarInCycle;
}
-> ^( Sum_hdr[$hdr.text] Expression[$e.text] Dependants[$hdr.dependants+" "+$e.dependants] VarInCycle[$e.strVarInCycle])
;

sum_header returns[String dependants]
	: ( '(' 'i=' e1=expression ',' e2=expression (',' '-'? INTEGER )? ')' )
	{$dependants = $e1.dependants + " " +$e2.dependants;} 
	;

svar_dss : 
	( '[' sc=LOCAL ']' )? IDENT '{' TIMESERIES b=STRING? KIND k=STRING UNITS u=STRING (CONVERT c=STRING)? '}'
	-> ^(Svar_dss  Scope[$sc.text]  IDENT B_part[$b.text] Kind $k Units $u CONVERT[$c.text]) 
	;		
	
timeArraySize 
returns[String dependant] 
@init { String dependant = ""; }
	: INTEGER  
	| ( i=IDENT { $dependant=$i.text; } )    
	;	
	
dvar_std :
	( '(' ta=timeArraySize ')')? ( '[' sc=LOCAL ']' )? IDENT '{' STD KIND k=STRING UNITS u=STRING '}' 	
	->  {ta==null}? ^(Dvar_std                                  Scope[$sc.text] IDENT Kind $k Units $u) 
	->	            ^(DvarTimeArray_std TimeArraySize[$ta.text] Scope[$sc.text] IDENT Kind $k Units $u)
	; 	

dvar_nonStd :
	( '(' ta=timeArraySize ')')? ( '[' sc=LOCAL ']' )? IDENT '{' lower_and_or_upper KIND k=STRING UNITS u=STRING '}' 
	->  {ta==null}? ^(Dvar_nonStd                                  Scope[$sc.text] IDENT lower_and_or_upper Kind $k Units $u) 
	->              ^(DvarTimeArray_nonStd TimeArraySize[$ta.text] Scope[$sc.text] IDENT lower_and_or_upper Kind $k Units $u) 
	;	

lower_and_or_upper : lower_upper
				   | upper_lower ;
				   
lower_upper : lower (u=upper)?
				-> {u==null}? lower Upper LimitType[Param.upper_unbounded]
				->            lower $u
				 ;
upper_lower : upper (l=lower)? 
                -> {l==null}? Lower LimitType["0"] upper
                ->            $l upper
   				 ;				   

lower: LOWER ( UNBOUNDED -> Lower LimitType[Param.lower_unbounded] | e=expression -> Lower LimitType[$e.tree.toStringTree().replaceAll("\\s+", "")] ) ;
upper: UPPER ( UNBOUNDED -> Upper LimitType[Param.upper_unbounded] | e=expression -> Upper LimitType[$e.tree.toStringTree().replaceAll("\\s+", "")] ) ;


/// IDENT =, <, > ///


constraint_statement returns[String text, String dependants, String varInCycle]
	: c=constraint_statement_preprocessed
	 
	 {  $text = Tools.replace_ignoreChar($c.text); 
	    $text = Tools.replace_seperator($text); 
	    $dependants = $c.dependants;
	    $varInCycle = $c.varInCycle;
	 };	

constraint_statement_preprocessed returns[String dependants, String varInCycle]
	: c=expression ( '<' | '>' | '='  ) d=expression
	{ $dependants = $c.dependants + " " + $d.dependants;
	  $varInCycle = $c.strVarInCycle + " " + $d.strVarInCycle;
	}
	
	;


assignment returns[String dependants, String varInCycle] 
	:  t=term_simple  '=' e=expression 
		{ $dependants = Tools.remove_nulls($e.dependants); 
		  $varInCycle = Tools.remove_nulls($e.strVarInCycle);}  
	-> Assignment[$t.text+"="+$e.text] 
	;
	
lt_or_gt :              term_simple ( '<'  | '>'  ) expression ;
le_or_ge :              term_simple ( '<=' | '>=' ) expression ;
equal_statement :       term_simple '==' expression ;

/// Expression ///
number : INTEGER | FLOAT ;

term_simple : ident | number | function  ;

ident: IDENT ;

array_iterator : '$m' ;

term :	      i=ident {$expression::SV.add($i.text.toLowerCase());} 
	 |        array_iterator
	 |         number 
	 |        function 
	 |       '(' e=expression ')' {$expression::SV.addAll($e.members);$expression::varInCycle.addAll($e.setVarInCycle);}
	 |       '(' s=sum_content ')'  {$expression::varInCycle.addAll($s.setVarInCycle);}
	 ;
	
unary :	('+'! | negation)? term 	;

negation :	'-' -> NEGATION["-"]	;

mult :	unary (('*' | '/' ) unary)* 	;
	
add :	mult (('+' | '-') mult)*	;

logical_expr returns[Set<String> members, String dependants, Set<String> setVarInCycle, String strVarInCycle] 
scope { Set<String> SV; Set<String> varInCycle } 
@init { $logical_expr::SV = new HashSet<String>(); 
        $logical_expr::varInCycle = new HashSet<String>(); 
        String dependants = null; 
        String strVarInCycle = null; } 
	:  c_unary ( bin c_unary )* 	
	
	{  
	   //$logical_expr::SV.removeAll(reservedSet);
	   $members = $logical_expr::SV;
       for (String s : $logical_expr::SV) {
       
	   	$dependants = $dependants +" "+s;
	   }
	   
	   $setVarInCycle = $logical_expr::varInCycle;
	   for (String s : $logical_expr::varInCycle) {
       
	   	$strVarInCycle = $strVarInCycle +" "+s;
	   } 	   
	};
	
expression returns[String text, Set<String> members, String dependants, Set<String> setVarInCycle, String strVarInCycle] 
scope { Set<String> SV; Set<String> varInCycle } 
@init { $expression::SV = new HashSet<String>(); 
		$expression::varInCycle = new HashSet<String>(); 
		String dependants = "";
		String strVarInCycle = ""; } 
	:
	add 
	{  $text = Tools.replace_ignoreChar($add.text); 
	   $text = Tools.replace_seperator($text);
	   
	   //$expression::SV.removeAll(reservedSet);
	   $members = $expression::SV;
       for (String s : $expression::SV) {
       
	   	$dependants = $dependants +" "+s;
	   }
	   
	   $setVarInCycle = $expression::varInCycle;
	   for (String s : $expression::varInCycle) {
       
	   	$strVarInCycle = $strVarInCycle +" "+s;
	   }    
	};	
	
c_term
	: ( expression relation expression ) => e1=expression relation e2=expression 
		{
			$logical_expr::SV.addAll($e1.members); 
			$logical_expr::SV.addAll($e2.members);
			$logical_expr::varInCycle.addAll($e1.setVarInCycle);
			$logical_expr::varInCycle.addAll($e2.setVarInCycle);
		} 
	| function_logical
	| ( '(' logical_expr ')' ) => '(' e=logical_expr ')' 
		{
			$logical_expr::SV.addAll($e.members);
			$logical_expr::varInCycle.addAll($e.setVarInCycle);
		} 
	;	

c_unary :	(c_negation)? c_term  	;

c_negation :	NOT -> NOT[".NOT."]	;

 
	
relation : '>' | '<' | '>=' | '<=' | '==' | '/=' ;	

bin : OR -> OR[".OR."] | AND -> AND[".AND."] ;	
	
/// End Expression /// 	

/// Intrinsic functions ///

//range_func
//	: RANGE '(' MONTH ',' MONTH_CONST ',' MONTH_CONST ')' ;

function : external_func | max_func | min_func | int_func | round_func | var_model ;

function_logical : range_func ;

var_model: var_model_noTimeArray|var_model_timeArray|var_modelindex_noTimeArray|var_modelindex_TimeArray
           ;

var_model_noTimeArray 
	: varName=IDENT '[' cycleName = IDENT ']' 
	  {  $expression::varInCycle.add($varName.text+'['+$cycleName.text+']' );} ; //{ $expression::DV.add($i.text.toLowerCase());} ;	

var_model_timeArray 
  : varName=IDENT '[' cycleName = IDENT ']' '(' e=expression ')'
    {  $expression::varInCycle.add($varName.text+'['+$cycleName.text+']'+'(' + $e.text +')' );} ; //{ $expression::DV.add($i.text.toLowerCase());} ; 

var_modelindex_noTimeArray 
  : varName=IDENT '[' cycleIndex=('-' INTEGER) ']' {
      if (!VarCycleIndex.varCycleIndexList.contains(varName)){
        VarCycleIndex.varCycleIndexList.add($varName.text.toLowerCase());
      }
    };  
  
var_modelindex_TimeArray 
  : varName=IDENT '[' cycleIndex=('-' INTEGER) ']' '(' e=expression ')' {
      if (!VarCycleIndex.varCycleIndexList.contains(varName)){
        VarCycleIndex.varCycleIndexList.add($varName.text.toLowerCase());
      }
  }; 

external_func // this could be timeseries function
	: i=IDENT {$expression::SV.add($i.text);} '('  ie=expression (',' e=expression  {$expression::SV.addAll($e.members);$expression::varInCycle.addAll($e.setVarInCycle);}  )*  ')' 
	  {  $expression::SV.addAll($ie.members);$expression::varInCycle.addAll($ie.setVarInCycle);}  // $expression::EX.add($i.text.toLowerCase()); 
	     
	;

range_func : RANGE '(' IDENT ',' ( IDENT | number ) ',' ( IDENT | number ) ')' ;

max_func
	: MAX '(' ie=expression (',' e=expression  
				{
					$expression::SV.addAll($e.members);
					$expression::varInCycle.addAll($e.setVarInCycle);
				}  
				 
			)+ ')' 
	        	
	        	{
	        		$expression::SV.addAll($ie.members);
	        		$expression::varInCycle.addAll($ie.setVarInCycle);
	        	}
	;

min_func
	: MIN '(' ie=expression (',' e=expression  
				{
					$expression::SV.addAll($e.members);
					$expression::varInCycle.addAll($e.setVarInCycle);
				}
				    
			)+ ')' 
	        	
	        	{	
	        		$expression::SV.addAll($ie.members);
	        		$expression::varInCycle.addAll($ie.setVarInCycle);
	        	}	
	;

int_func 
	: INT '(' e=expression ')' 
	{
		$expression::SV.addAll($e.members);
		$expression::varInCycle.addAll($e.setVarInCycle);
	} 
	;

round_func 
  : ROUND '(' e=expression ')' 
  {
    $expression::SV.addAll($e.members);
    $expression::varInCycle.addAll($e.setVarInCycle);
  } 
  ;
	
/// End Intrinsic functions ///	

COMMENT : ('!'|'#') .* ('\n'|'\r') {skip();}; //{$channel = HIDDEN;}; 
MULTILINE_COMMENT : '/*' .* '*/' {skip();}; //{$channel = HIDDEN;};

fragment LETTER : ('a'..'z' | 'A'..'Z') ;
fragment DIGIT : '0'..'9';
INTEGER : DIGIT+ ;
FLOAT : (INTEGER? '.' INTEGER)  |  (INTEGER '.') ;


/// logical ///
AND : '.and.' | '.AND.' ;
OR  : '.or.'  | '.OR.'  ;
NOT : '.not.' | '.NOT.' ;

/// reserved keywords ///
PENALTY : 'penalty' | 'PENALTY' | 'Penalty';
CONSTRAIN : 'constrain' | 'CONSTRAIN' | 'Constrain';
INT : 'int' | 'INT' | 'Int' ;
ROUND : 'round' | 'ROUND' | 'Round' ;
SUM :  'sum' | 'SUM' | 'Sum' ;
RANGE : 'range' | 'RANGE' | 'Range' ;
MAX :   'max' | 'MAX' | 'Max' ;
MIN :   'min' | 'MIN' | 'Min' ;
VALUE : 'value' | 'VALUE' | 'Value' ;
LOCAL : 'local'| 'LOCAL' | 'Local' ;
OBJECTIVE: 'objective' | 'Objective' | 'OBJECTIVE';
TIMESERIES: 'timeseries' | 'TIMESERIES' | 'Timeseries';
SELECT :  'select' | 'Select' | 'SELECT' ;
FROM:     'from' | 'From' | 'FROM' ;
WHERE : 'where' | 'Where' | 'WHERE';
GIVEN:    'given' | 'Given' | 'GIVEN' ;
USE:      'use' | 'Use' | 'USE' ;
CASE : 'case' | 'Case' | 'CASE' ;
LHS: 'lhs' | 'LHS' ;
RHS: 'rhs' | 'RHS' ;
EXTERNAL : 'EXTERNAL' | 'external' | 'External' ;
F90 : 'f90' | 'F90' ;
DLL :  IDENT ('.dll' | '.DLL' );
INTEGER_WORD: 'integer' | 'INTEGER' | 'Integer' ;
STD : 'std' | 'STD' ;
UNITS : 'units' | 'UNITS' | 'Units' ;
CONVERT : 'convert' | 'CONVERT' | 'Convert';
ALIAS : 'alias' | 'ALIAS' | 'Alias';
KIND : 'kind' | 'KIND' | 'Kind';
GOAL : 'goal' | 'GOAL' | 'Goal';
DEFINE :'define' | 'DEFINE' | 'Define' ;
ALWAYS :'always' | 'ALWAYS' | 'Always' ;
CONDITION : 'condition' | 'CONDITION' | 'Condition' ;
SEQUENCE  : 'sequence' | 'SEQUENCE' | 'Sequence';
MODEL     : 'model' | 'MODEL' | 'Model';
ORDER     : 'order' | 'ORDER' | 'Order';
TIMESTEP  : 'timestep'|'TIMESTEP'|'TimeStep';
TIMESTEPVALUE: '1mon'|'1MON'|'1day'|'1DAY';
INCLUDE   : 'include' | 'INCLUDE' | 'Include';
LOWER     : 'lower' | 'LOWER' | 'Lower' ;
UPPER     : 'upper' | 'UPPER' | 'Upper' ;
UNBOUNDED : 'unbounded' | 'Unbounded' | 'UNBOUNDED';

/// include file path ///
FILE_PATH :  '\''   DIR_SPLIT? (DIR_ELEMENT | DIR_UP)*   WRESL_FILE  '\''  ;
fragment WRESL_EXT :   '.wresl' | '.WRESL' ;
fragment WRESL_FILE :  (LETTER | DIGIT | '_' |'-'  )+ WRESL_EXT ;
fragment DIR_ELEMENT : (LETTER | DIGIT | '_' | '-' )+  '\\' ;
fragment DIR_UP :                                   ('..') '\\' ;
fragment DIR_SPLIT : '\\' ;


STRING : '\''  IDENT( '-' | '/' | IDENT )*  '\'';

IDENT_FOLLOWED_BY_LOGICAL 
	: i=IDENT{$i.setType(IDENT); emit($i);}
	( a=AND { $a.setType(AND); emit($a);}
	| a=OR  { $a.setType(OR); emit($a);}
	| a=NOT { $a.setType(NOT); emit($a);}
	);

INTEGER_FOLLOWED_BY_LOGICAL 
	: i=INTEGER{$i.setType(INTEGER); emit($i);}
	( a=AND { $a.setType(AND); emit($a);}
	| a=OR  { $a.setType(OR); emit($a);}
	| a=NOT { $a.setType(NOT); emit($a);}
	);
	
IDENT : LETTER (LETTER | DIGIT | '_')*;

WS : (' ' | '\t' | '\n' | '\r' | '\f')+ {$channel = HIDDEN;};

COMMENT_LAST_LINE : ('!'|'#') (~('\n' | '\r'))* {skip();};
//IGNORE : . {skip();};
