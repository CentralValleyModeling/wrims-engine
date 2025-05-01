tree grammar ValueEvaluatorTreeWalker;

options {
  language = Java;
  tokenVocab=ValueEvaluatorTree;
  ASTLabelType=CommonTree;
}

@header {
  package wrimsv2.evaluator;
    
  import org.antlr.runtime.ANTLRFileStream;
  import org.antlr.runtime.CharStream;
  import org.antlr.runtime.CommonTokenStream;
  import org.antlr.runtime.RecognitionException;
  import org.antlr.runtime.TokenStream;
  
  import java.util.HashMap;
  import wrimsv2.components.Error;
  import wrimsv2.components.IntDouble;
}

@members {
  public static IntDouble evalValue;
  public static boolean evalCondition;
    public Stack<LoopIndex> sumIndex= new Stack <LoopIndex>();
  
  @Override
  public void reportError(RecognitionException e) {
       Error.addEvaluationError(getErrorMessage(e, tokenNames));
  }
}

evaluator 
  : expressionInput |
    conditionInput 
  ;

///////////////////
/// input rules ///
///////////////////

expressionInput: ^('v:' expressionCollection){evalValue=$expressionCollection.id;};
conditionInput: ^('c:' conditionStatement) {evalCondition=$conditionStatement.result;};

///////////////////
/// basic rules ///
///////////////////
lhsrhs: term|CONSTRAIN;

//weight	:	(allnumber|(allnumber '*' TAFCFS)) (('+' allnumber)|('-' allnumber))?;

units: IDENT|(IDENT '/' IDENT);

fileName
  : (':'|';'|'.'|'|'|SYMBOLS|'-'|'+'|BACKSLASH|IDENT|IDENT1|IDENT2|INTEGER|FLOAT|usedKeywords)+{
  }
  ;
  
externalFile
  : (';'|'.'|'|'|SYMBOLS|'-'|'+'|INTEGER|FLOAT|IDENT|usedKeywords)+
  ;
	
text	:	LETTER (LETTER | DIGIT )*;

conditionStatement returns [boolean result]
  : (r=relationStatementTerm{result=$r.result;})|ALWAYS{result=true;}
  ;
  
relationStatementTerm returns [boolean result]
  : r0=relationRangeStatement{result=$r0.result;}|r1=relationStatementSeries{result=$r1.result;}
  ;
 
relationStatementSeries returns [boolean result]
  : (^((s=('.and.'|'.or.')) r1=relationStatementTerm {result=$r1.result;} 
    (r2=relationStatementTerm {result=ValueEvaluation.relationStatementSeries(result, $r2.result, $s.text);}))) ;

relationRangeStatement returns [boolean result]
  : (r1=relationStatement{result=$r1.result;})|(r2=range_func{result=$r2.result;})
  ;

relationStatement returns [boolean result] 
  : ^(s=relation e1=term e2=term){result=ValueEvaluation.relationStatement($e1.id, $e2.id, $s.text);}
  ;

	
expressionCollection returns [IntDouble id]
	:((term{id=$term.id;})
	|(tableSQL){id=$tableSQL.id;}
	|(timeseriesWithUnits)
	|((timeseries){id=$timeseries.id;})
	|(sumExpression{id=$sumExpression.id;}))
	|(UPPERUNBOUNDED{id=new IntDouble(1e38,true);})
	|(LOWERUNBOUNDED{id=new IntDouble(-1e38,true);})
	;

func returns[IntDouble id]: 
  (max_func{id=$max_func.id;})|
  (min_func{id=$min_func.id;})|
  (int_func{id=$int_func.id;})|
  (real_func{id=$real_func.id;})|
  (abs_func{id=$abs_func.id;})|
  (exp_func{id=$exp_func.id;})|
  (log_func{id=$log_func.id;})|
  (log10_func{id=$log10_func.id;})|
  (pow_func{id=$pow_func.id;});

max_func returns[IntDouble id] 
	: ^(MAX (t1=term){id=$t1.id;} (t2=term{
     id=ValueEvaluation.max(id, $t2.id);
  }))
	;

min_func returns[IntDouble id]
	: ^(MIN (t1=term){id=$t1.id;} (t2=term{
     id=ValueEvaluation.min(id, $t2.id);
  }))
	;
	
int_func returns[IntDouble id]
  : ^(INT (t=term) {
     id=ValueEvaluation.intFunc($t.id);
  })
  ;
  
real_func returns[IntDouble id]
  : ^(REAL (t=term) {
     id=ValueEvaluation.realFunc($t.id);
  })
  ;
  
abs_func returns[IntDouble id]
  : ^(ABS (t=term) {
     id=ValueEvaluation.abs($t.id);
  })
  ;

exp_func returns[IntDouble id]
  : ^(EXP (t=term) {
     id=ValueEvaluation.exp($t.id);
  })
  ;

log_func returns[IntDouble id]
  : ^(LOG (t=term) {
     id=ValueEvaluation.log($t.id);
  })
  ;

log10_func returns[IntDouble id]
  : ^(LOG10 (t=term) {
    id=ValueEvaluation.log10($t.id);
  })
  ;
  
pow_func returns[IntDouble id]
  : ^(POW (t1=term) (t2=term) {
     id=ValueEvaluation.pow($t1.id, $t2.id);
  })
  ;
  
range_func returns [boolean result]
  : ^(RANGE  MONTH m1=MONTH_CONST m2=MONTH_CONST ){result=ValueEvaluation.range($m1.text, $m2.text);};

timeseriesWithUnits 
	: TIMESERIES 'kind' '=' partC 'units' '=' IDENT 
	;

timeseries returns [IntDouble id]
	: TIMESERIES {id=ValueEvaluation.timeseries();}
	;
	

	
partC: 	(IDENT|IDENT1|usedKeywords) ('-' (IDENT|IDENT1|usedKeywords))*;
  
usedKeywords: YEAR|MONTH|MONTH_CONST|PASTMONTH|RANGE|TAFCFS|DAYSIN|SUM|MAX|MIN|INT|ABS|LOG|LOG10|POW|MOD|SELECT|FROM|GIVEN|USE|WHERE
|CONSTRAIN|ALWAYS|NAME|DVAR|CYCLE|FILE|CONDITION|INCLUDE|LOWERBOUND|UPPERBOUND|INTEGERTYPE|UNITS|CONVERTUNITS|TYPE|OUTPUT
|CASE|ORDER|EXPRESSION|LHSGTRHS|LHSLTRHS|WEIGHT|FUNCTION|FROM_WRESL_FILE|UPPERUNBOUNDED|LOWERUNBOUNDED;

tableSQL returns [IntDouble id] @init{String table=null; String select=null; String use=null; HashMap<String, Number> given=null; HashMap<String, Number> where=null;}
	: ^(SELECT (selectName{select=$selectName.text;}) i1=IDENT{table=$i1.text;} 
	  (a=assignStatement{given=new HashMap<String, Number>(); given.put($a.assignIdent, $a.value);})? (i2=IDENT{use=$i2.text;})? 
	  (where_items{where=$where_items.where;})?) {id=ValueEvaluation.tableSQL(table, select, where, given, use);}	  
	;
	
selectName: (IDENT |usedKeywords);

where_items returns [HashMap<String, Number> where]
	:	 ^(WHERE  (r1=whereStatement{where=new HashMap<String, Number>(); where.put($r1.whereIdent, $r1.value);})
	        (r=whereStatement{where.put($r.whereIdent, $r.value);})* )
	;


//upperbound:	IDENT|allnumber|^('*' allnumber TAFCFS);

//lowerbound:	IDENT|allnumber|^('*' allnumber TAFCFS);

//sumExpression 
//  : SUM '(' I '=' e1=expression_sum ';' e2=expression_sum (';' (s='-')? INTEGER )? ')' e3=expression
//  ;
//term_sum: (MONTH|MONTH_CONST|PASTMONTH|I|INTEGER|'(' expression_sum ')');

//unary_sum : ('-')? term_sum ;
//add_sum  :  unary_sum(('+' | '-') unary_sum)* ;
//expression_sum: add_sum ;

//sumExpression was redesign. If not work, switch back to the original design above

sumExpression returns [IntDouble id] @init{String s="";}
  : ^(SUM IDENT{$id=new IntDouble(0, false);} t1=term t2=term ((('-'{s=s+"-";})? INTEGER {s=s+$INTEGER.text;}))? ({$id=new IntDouble(0, false);})  t3=term{$id=new IntDouble(0, false);})
  ;

term returns [IntDouble id]
	:	(IDENT {$id=new IntDouble(0, false);})
	| (FLOAT {$id=ValueEvaluation.term_FLOAT($FLOAT.text);}) 
	| ( t=expression  {$id=$t.id;})
	| ((knownTS{$id=ValueEvaluation.term_knownTS($knownTS.result);}) 
	| func{$id=$func.id;}
	| (INTEGER {$id=ValueEvaluation.term_INTEGER($INTEGER.text);})
	| tafcfs_term{$id=$tafcfs_term.id;}
	| YEAR{$id=ValueEvaluation.term_YEAR();}
	| MONTH{$id=ValueEvaluation.term_MONTH();}
	| MONTH_CONST{$id=ValueEvaluation.term_MONTH_CONST($MONTH_CONST.text);}
	| PASTMONTH{$id=ValueEvaluation.term_PASTMONTH($PASTMONTH.text);}
	| DAYSIN{$id=ValueEvaluation.daysIn();})
	| (SVAR{$id=ValueEvaluation.term_SVAR($SVAR.text.replace("{","").replace("}",""));}) 
	;
	
tafcfs_term returns [IntDouble id]: ^(TAFCFS term?) {
    id=ValueEvaluation.tafcfs_term($TAFCFS.text, $term.id);
};
	
knownTS returns [IntDouble result]  
  : (f=function{result=$f.result;})|(p=pastCycleDV {result=$p.result;}) 
  ;
  
//pastMonthTS  
//  : ((i1=IDENT)|TAFCFS) '(' ((p=PASTMONTH)|(i=I)|(pm=(MONTH_CONST '-' MONTH (('+'|'-') INTEGER)? ))|(mp=(MONTH '-' MONTH_CONST (('+'|'-') INTEGER)?))) ')'
//  ;
  
//preMonthTS 
//  : IDENT '(' (s='-')? INTEGER ')'  
//  ;
  
pastCycleDV returns [IntDouble result]
  : ^(i1=IDENT '[' i2=IDENT ']'{result=ValueEvaluation.pastCycleNoTimeArray($i1.text,$i2.text);})
  ; 

function returns [IntDouble result]
  : (n=noArgFunction{result=$n.result;})|(a=argFunction{result=$a.result;})
  ;

noArgFunction returns [IntDouble result]
  : ^(IDENT '(' ')' {result=ValueEvaluation.noArgFunction($IDENT.text);});

argFunction returns [IntDouble result] @init{ArrayList<ArrayList<IntDouble>> idArray = new ArrayList<ArrayList<IntDouble>>();}
  : ^(IDENT '(' (t1=term {ArrayList<IntDouble> idArray1=new ArrayList<IntDouble>();idArray1.add($t1.id);idArray.add(idArray1);}) ((t2=term{ArrayList<IntDouble> idArray1=new ArrayList<IntDouble>();idArray1.add($t2.id);idArray.add(idArray1);}))* ')'{result=ValueEvaluation.argFunction($IDENT.text,idArray);});
  	
expression returns [IntDouble id]  
  : (^('+' t1=term t2=term{$id=ValueEvaluation.add($t1.id, $t2.id);})
  |^('-' t3=term t4=term{$id=ValueEvaluation.substract($t3.id, $t4.id);})
  |^('*' t5=term t6=term{$id=ValueEvaluation.mult($t5.id, $t6.id);})
  |^('/' t7=term t8=term{$id=ValueEvaluation.divide($t7.id, $t8.id);})
  |^(MOD t9=term t10=term{$id=ValueEvaluation.mod($t9.id, $t10.id);})
  |^(UNARY t11=term{$id=ValueEvaluation.unary("-", $t11.id);}))
  ;

relation returns [String text]
	: '=='{text="==";}
	| '<'{text="<";}
	| '>'{text=">";}
	| '>='{text=">=";}
	| '<='{text="<=";}
	;	

whereStatement returns [String whereIdent, Number value]
  : ^(WHERE ((i=IDENT{$whereIdent=$i.text;})|(u=usedKeywords{$whereIdent=$u.text;})) term{$value=ValueEvaluation.assignWhereStatement($term.id);}) 
  ;

assignStatement returns [String assignIdent, Number value]  
  : ^('=' IDENT  term {$assignIdent=$IDENT.text; $value=ValueEvaluation.assignWhereStatement($term.id);}) 
  ;

number
	: INTEGER 
	| FLOAT
	;



	
