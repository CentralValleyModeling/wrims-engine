grammar ValueEvaluator;

options {
  language = Java;
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
  import wrimsv2.parallel.ParallelVars; 
}

@lexer::header {
  package wrimsv2.evaluator;
}

@members {
  public IntDouble evalValue;
  public boolean evalCondition;
  public ParallelVars prvs;
  public Stack<LoopIndex> sumIndex= new Stack <LoopIndex>();
  
  @Override
  public void reportError(RecognitionException e) {
       Error.addEvaluationError(getErrorMessage(e, tokenNames));
  }
  
  public void setParallelVars (ParallelVars prvs1) {
       prvs=prvs1;
  }
  
  public void setSumIndex(Stack<LoopIndex> sumIndex){
      this.sumIndex=sumIndex;
  }
}

evaluator returns [String result]
	:	expressionInput |
		conditionInput 
	;

///////////////////
/// input rules ///
///////////////////

expressionInput: 'v:' expressionCollection{evalValue=$expressionCollection.id;};
conditionInput: 'c:' conditionStatement {evalCondition=$conditionStatement.result;};

///////////////////
/// basic rules ///
///////////////////
lhsrhs: expression|CONSTRAIN;

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
	
expressionCollection returns [IntDouble id]
	:(expression{id=$expression.id;})
	|(tableSQL){id=$tableSQL.id;}
	|(timeseriesWithUnits)
	|((timeseries){id=$timeseries.id;})
	| sumExpression {id=$sumExpression.id;}
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
  (pow_func{id=$pow_func.id;})|
  (mod_func{id=$mod_func.id;})|
  (round_func{id=$round_func.id;})|
  (sin_func{id=$sin_func.id;})|
  (cos_func{id=$cos_func.id;})|
  (tan_func{id=$tan_func.id;})|
  (cot_func{id=$cot_func.id;})|
  (asin_func{id=$asin_func.id;})|
  (acos_func{id=$acos_func.id;})|
  (atan_func{id=$atan_func.id;})|
  (acot_func{id=$acot_func.id;})|
  (exceedFunc{id=$exceedFunc.id;})|
  (exceedtsiFunc{id=$exceedtsiFunc.id;});

round_func returns[IntDouble id]
  : ROUND '(' (e1=expression) ')'{
     id=ValueEvaluation.round($e1.id);
  }
  ;

mod_func returns[IntDouble id]
  : MOD '(' (e1=expression) (';' (e2=expression)) ')'{
     id=ValueEvaluation.mod($e1.id, $e2.id);
  }
  ;

max_func returns[IntDouble id] 
	: MAX '(' (e1=expression){id=$e1.id;}(';' (e2=expression{
     id=ValueEvaluation.max(id, $e2.id);
  }))+ ')'
	;

min_func returns[IntDouble id]
	: MIN '(' (e1=expression){id=$e1.id;}(';' (e2=expression{
     id=ValueEvaluation.min(id, $e2.id);
  }))+ ')'
	;
	
int_func returns[IntDouble id]
  : INT '(' (e=expression) ')'{
     id=ValueEvaluation.intFunc($e.id);
  }
  ;
  
real_func returns[IntDouble id]
  : REAL '(' (e=expression) ')'{
    id=ValueEvaluation.realFunc($e.id);
  }
  ;
  
abs_func returns[IntDouble id]
  : ABS '(' (e=expression) ')'{
     id=ValueEvaluation.abs($e.id);
  }
  ;

exp_func returns[IntDouble id]
  : EXP '(' (e=expression) ')'{
     id=ValueEvaluation.exp($e.id);
  }
  ;
  
log_func returns[IntDouble id]
  : LOG '(' (e=expression) ')'{
     id=ValueEvaluation.log($e.id);
  }
  ;

log10_func returns[IntDouble id]
  : LOG10 '(' (e=expression) ')'{
    id=ValueEvaluation.log10($e.id);
  }
  ;
  
pow_func returns[IntDouble id]
  : POW '(' (e1=expression) (';' (e2=expression)) ')'{
     id=ValueEvaluation.pow($e1.id, $e2.id);
  }
  ;

sin_func returns[IntDouble id]
  : SIN '(' (e=expression) ')'{
    id=ValueEvaluation.sin($e.id);
  }
  ;

cos_func returns[IntDouble id]
  : COS '(' (e=expression) ')'{
    id=ValueEvaluation.cos($e.id);
  }
  ;
  
tan_func returns[IntDouble id]
  : TAN '(' (e=expression) ')'{
    id=ValueEvaluation.tan($e.id);
  }
  ;
  
cot_func returns[IntDouble id]
  : COT '(' (e=expression) ')'{
    id=ValueEvaluation.cot($e.id);
  }
  ;

asin_func returns[IntDouble id]
  : ASIN '(' (e=expression) ')'{
    id=ValueEvaluation.asin($e.id);
  }
  ;

acos_func returns[IntDouble id]
  : ACOS '(' (e=expression) ')'{
    id=ValueEvaluation.acos($e.id);
  }
  ;
  
atan_func returns[IntDouble id]
  : ATAN '(' (e=expression) ')'{
    id=ValueEvaluation.atan($e.id);
  }
  ;
  
acot_func returns[IntDouble id]
  : ACOT '(' (e=expression) ')'{
    id=ValueEvaluation.acot($e.id);
  }
  ;
  
exceedFunc returns[IntDouble id]
  : EXCEEDANCE '(' var=IDENT ';' exc=term ';' (mon=MONTH_CONST|mon=MONTH_RANGE|mon=ALL) ';' sy=INTEGER ';' sm=MONTH_CONST ';' sd=INTEGER ';' ey=INTEGER ';' em=MONTH_CONST ';' ed=INTEGER ')' {
    id=ValueEvaluation.exceedance($var.text, $exc.id, $mon.text, $sy.text, $sm.text, $sd.text, $ey.text, $em.text, $ed.text);
  }  
  ;
  
exceedtsiFunc returns[IntDouble id]
  : EXCEEDANCE_TSI '(' var=IDENT ';' exc=term ';' (mon=MONTH_CONST|mon=MONTH_RANGE|mon=ALL) ';' sy=INTEGER ';' sm=MONTH_CONST ';' sd=INTEGER ';' ey=INTEGER ';' em=MONTH_CONST ';' ed=INTEGER ')' {
    id=ValueEvaluation.exceedance_tsi($var.text, $exc.id, $mon.text, $sy.text, $sm.text, $sd.text, $ey.text, $em.text, $ed.text);
  }  
  ;
  
range_func returns [boolean result]
  : RANGE '(' MONTH ';' m1=MONTH_CONST ';' m2=MONTH_CONST ')' {result=ValueEvaluation.range($m1.text, $m2.text);};

timeseriesWithUnits 
	: 'timeseries' 'kind' '=' partC 'units' '=' IDENT 
	;

timeseries returns [IntDouble id]
	: 'timeseries' {id=ValueEvaluation.timeseries();}
	;
	

	
partC: 	(IDENT|IDENT1|usedKeywords) ('-' (IDENT|IDENT1|usedKeywords))*;
  
usedKeywords: YEAR|MONTH|MONTH_CONST|MONTH_RANGE|DAY|PASTMONTH|RANGE|TAFCFS|DAYSIN|DAYSINTIMESTEP|SUM|MAX|MIN|INT|REAL|ABS|EXP|LOG|LOG10|POW|MOD|ROUND|SELECT|FROM|GIVEN|USE|WHERE
|CONSTRAIN|ALWAYS|NAME|DVAR|CYCLE|FILE|CONDITION|INCLUDE|LOWERBOUND|UPPERBOUND|INTEGERTYPE|UNITS|CONVERTUNITS|TYPE|OUTPUT
|CASE|ORDER|EXPRESSION|LHSGTRHS|LHSLTRHS|WEIGHT|FUNCTION|FROM_WRESL_FILE|UPPERUNBOUNDED|LOWERUNBOUNDED|AND|OR|NOT
|SIN|COS|TAN|COT|ASIN|ACOS|ATAN|ACOT|EXCEEDANCE|EXCEEDANCE_TSI|ALL;

tableSQL returns [IntDouble id] @init{String table=null; String select=null; String use=null; HashMap<String, Number> given=null; HashMap<String, Number> where=null;}
	: SELECT ((i1=IDENT{select=$i1.text;})|(u1=usedKeywords{select=$u1.text;})) FROM i2=IDENT{table=$i2.text;} 
	  (GIVEN a=assignStatement{given=new HashMap<String, Number>(); given.put($a.assignIdent, $a.value);})? (USE i3=IDENT{use=$i3.text;})? 
	  (where_items{where=$where_items.where;})? {id=ValueEvaluation.tableSQL(table, select, where, given, use);}	  
	;

where_items returns [HashMap<String, Number> where]
	:	 WHERE  (r1=whereStatement{where=new HashMap<String, Number>(); where.put($r1.whereIdent, $r1.value);})
	        (';' r=whereStatement{where.put($r.whereIdent, $r.value);})*
	;


upperbound:	IDENT|allnumber|(allnumber '*' TAFCFS);

lowerbound:	IDENT|allnumber|(allnumber '*' TAFCFS);

//sumExpression 
//  : SUM '(' I '=' e1=expression_sum ';' e2=expression_sum (';' (s='-')? INTEGER )? ')' e3=expression
//  ;
//term_sum: (MONTH|MONTH_CONST|PASTMONTH|I|INTEGER|'(' expression_sum ')');

//unary_sum : ('-')? term_sum ;
//add_sum  :  unary_sum(('+' | '-') unary_sum)* ;
//expression_sum: add_sum ;

//sumExpression was redesign. If not work, switch back to the original design above

sumExpression returns [IntDouble id] @init{String s="";}
  : SUM '(' IDENT{ValueEvaluation.sumExpression_IDENT($IDENT.text, sumIndex);} '=' e1=expression ';' e2=expression (';' (('-'{s=s+"-";})? INTEGER {s=s+$INTEGER.text;}))? (')'{ValueEvaluation.initSumExpression($e1.id, $e2.id, s, sumIndex);})  e3=expression{id=ValueEvaluation.sumExpression($e3.id, $e3.text, sumIndex);}
  ;

term returns [IntDouble id]
	:	(IDENT {id=ValueEvaluation.term_IDENT($IDENT.text, sumIndex);})
	| (FLOAT {id=ValueEvaluation.term_FLOAT($FLOAT.text);}) 
	| ('(' (e=expression) ')' {id=$e.id;})
	| (knownTS{id=ValueEvaluation.term_knownTS($knownTS.result);}) 
	| func{id=$func.id;}
	| (INTEGER {id=ValueEvaluation.term_INTEGER($INTEGER.text);})
	| tafcfs_term{id=$tafcfs_term.id;}
	| YEAR{id=ValueEvaluation.term_YEAR();}
	| MONTH{id=ValueEvaluation.term_MONTH();}
	| DAY {id=ValueEvaluation.term_DAY();}
	| MONTH_CONST{id=ValueEvaluation.term_MONTH_CONST($MONTH_CONST.text);}
	| PASTMONTH{id=ValueEvaluation.term_PASTMONTH($PASTMONTH.text);}
	| DAYSIN{id=ValueEvaluation.daysIn();}
	| DAYSINTIMESTEP{id=ValueEvaluation.daysInTimeStep();}
	| (SVAR{id=ValueEvaluation.term_SVAR($SVAR.text.replace("{","").replace("}",""));})
	| ARRAY_ITERATOR{id=ValueEvaluation.term_ARRAY_ITERATOR(prvs);} 
	| '(' sumExpression ')' {id=$sumExpression.id;}
	;
	
tafcfs_term returns [IntDouble id]: TAFCFS ('(' expression ')')? {
    id=ValueEvaluation.tafcfs_term($TAFCFS.text, $expression.id);
};
	
knownTS returns [IntDouble result]  
  : (f=function{result=$f.result;})|(p=pastCycleValue {result=$p.result;}) 
  ;
    
pastCycleValue returns [IntDouble result]
  : (p1=pastCycleNoTimeArray{return $p1.result;})|(p2=pastCycleTimeArray{return $p2.result;})|(p3=pastCycleIndexNoTimeArray{return $p3.result;})|(p4=pastCycleIndexTimeArray{return $p4.result;})
  ;

pastCycleNoTimeArray returns [IntDouble result]
  : i1=IDENT '[' i2=IDENT ']'{result=ValueEvaluation.pastCycleNoTimeArray($i1.text,$i2.text);}
  ; 
  
pastCycleTimeArray returns [IntDouble result]
  : i1=IDENT '[' i2=IDENT ']' '(' e1=expression ')' {result=ValueEvaluation.pastCycleTimeArray($i1.text,$i2.text, $e1.id);}
  ; 
  
pastCycleIndexNoTimeArray returns [IntDouble result]
  : i1=IDENT '[' ('-' index=INTEGER) ']'{result=ValueEvaluation.pastCycleIndexNoTimeArray($i1.text, -Integer.parseInt($index.text));}
  ; 
  
pastCycleIndexTimeArray returns [IntDouble result]
  : i1=IDENT '[' '-' index=INTEGER ']' '(' e1=expression ')' {result=ValueEvaluation.pastCycleIndexTimeArray($i1.text,-Integer.parseInt($index.text), $e1.id);}
  ;

function returns [IntDouble result]
  : (n=noArgFunction{result=$n.result;})|(a=argFunction{result=$a.result;})
  ;

noArgFunction returns [IntDouble result]
  : IDENT '(' ')' {result=ValueEvaluation.noArgFunction($IDENT.text);};

argFunction returns [IntDouble result] @init{ArrayList<ArrayList<IntDouble>> idArray = new ArrayList<ArrayList<IntDouble>>(); ArrayList<IntDouble> id0Array=new ArrayList<IntDouble>();}
  : IDENT '(' (e1=expression {ArrayList<IntDouble> idArray1=new ArrayList<IntDouble>(); idArray1.add($e1.id); idArray.add(idArray1);} 
  | t1=trunk_timeArray{idArray.add($t1.idArray);}) 
  (';' (e2=expression{ArrayList<IntDouble> idArray1=new ArrayList<IntDouble>(); idArray1.add($e2.id); idArray.add(idArray1);}
  |t2=trunk_timeArray{idArray.add($t2.idArray);}))* ')' ('(' e0=expression {id0Array.add($e0.id);} ')')?
  {
    if (id0Array.size()==0) {
      result=ValueEvaluation.argFunction($IDENT.text,idArray);
    }else{
      result=ValueEvaluation.pastTSFV($IDENT.text, $e0.id, idArray, prvs);
    }
  };

trunk_timeArray returns[ArrayList<IntDouble> idArray] @init{idArray = new ArrayList<IntDouble>(); IntDouble start=new IntDouble(1, true);  IntDouble end=new IntDouble(1, true);}
  : i0=IDENT '(' (n1=integer{start=ValueEvaluation.term_INTEGER($n1.text);}|i1=IDENT{start=ValueEvaluation.term_IDENT($i1.text, sumIndex);}) ':' (n2=integer{end=ValueEvaluation.term_INTEGER($n2.text);}|i2=IDENT{end=ValueEvaluation.term_IDENT($i2.text, sumIndex);}) ')' 
  {
    idArray=ValueEvaluation.trunk_timeArray($i0.text, start, end);
  }
  ;
  	
unary returns [IntDouble id] 
	:	(s=('+'|'-'))? term{id=ValueEvaluation.unary($s.text, $term.id);
	};
	
allnumber 
	:	('-')? number;

mult returns [IntDouble id]  
	:	(u1=unary {id=$u1.id;}) (s=('*'| '/') (u2=unary){
	   if ($s.text.equals("*")){
	     id=ValueEvaluation.mult(id, $u2.id);
	   }else{
	     id=ValueEvaluation.divide(id, $u2.id);
	   }
  })*
	;
	
add  returns [IntDouble id]
	:	(m1=mult {id=$m1.id;}) ((s=('+'|'-')) (m2=mult){
     if ($s.text.equals("+")){
       id=ValueEvaluation.add(id, $m2.id);
     }else{
       id=ValueEvaluation.substract(id, $m2.id);
     }
	})*
	;

expression returns [IntDouble id]  
	:	i=add {$id=$add.id;} 
	;

relation
	: '=='
	| '<'
	| '>'
	| '>='
	| '<='
	;	

whereStatement returns [String whereIdent, Number value]
  : ((i=IDENT{$whereIdent=$i.text;})|(u=usedKeywords{$whereIdent=$u.text;})) '=' expression{$value=ValueEvaluation.assignWhereStatement($expression.id);} 
  ;

conditionStatement returns [boolean result]
	:	((r=relationUnary{result=$r.result;})|ALWAYS{result=true;})
	;

relationUnary returns [boolean result]
  : (n=NOT)? r=relationOr{
      if ($n==null){
        return $r.result;
      }else{
        if ($r.result){
          return false;
        }else{
          return true;
        }
      }
  }
  ; 
	
relationOr returns [boolean result] 
  : r1=relationAnd {result=$r1.result;} 
    (s=OR r2=relationAnd {result=ValueEvaluation.relationStatementSeries(result, $r2.result, $s.text);})* ;

relationAnd returns [boolean result] 
  : r1=relationRangeStatement {result=$r1.result;} 
    (s=AND r2=relationRangeStatement {result=ValueEvaluation.relationStatementSeries(result, $r2.result, $s.text);})* ;

relationRangeStatement returns [boolean result]
  : (r1=relationStatement{result=$r1.result;})|(r2=range_func{result=$r2.result;})
  ;

relationStatement returns [boolean result] 
	: (	( expression relation expression) => e1=expression relation e2=expression {result=ValueEvaluation.relationStatement($e1.id, $e2.id, $relation.text);} )
	| ( ( '('relationUnary')'  ) => '('r2=relationUnary')' {result=$r2.result;} )
	;

assignStatement returns [String assignIdent, Number value]  
  : IDENT '=' expression {$assignIdent=$IDENT.text; $value=ValueEvaluation.assignWhereStatement($expression.id);} 
  ;

number
	: INTEGER 
	| FLOAT
	;

integer : integer_p|integer_n ;
integer_p : INTEGER ;
integer_n : '-' INTEGER ;

MULTILINE_COMMENT : '/*' .* '*/' {$channel = HIDDEN;} ;

fragment LETTER : ('a'..'z' | 'A'..'Z') ;
fragment DIGIT : '0'..'9';
fragment SYMBOLS : '_';	

BACKSLASH : '\\';	 	 

INTEGER : DIGIT+ ;
FLOAT : INTEGER? '.' INTEGER 
	  | INTEGER '.' 
	  ;

//I: 'i';
YEAR: 'wateryear';
MONTH: 'month';
DAY: 'day';
MONTH_CONST: 'jan'|'feb'|'mar'|'apr'|'may'|'jun'|'jul'|'aug'|'sep'|'oct'|'nov'|'dec';
MONTH_RANGE: MONTH_CONST MONTH_CONST;
ALL: 'all';
PASTMONTH: 'prevjan'|'prevfeb'|'prevmar'|'prevapr'|'prevmay'|'prevjun'|'prevjul'|'prevaug'|'prevsep'|'prevoct'|'prevnov'|'prevdec';
RANGE: 'range';

TAFCFS: 'taf_cfs'|'cfs_taf'|'cfs_af'|'af_cfs';
DAYSIN: 'daysin'|'daysinmonth';
DAYSINTIMESTEP: 'daysintimestep';

ARRAY_ITERATOR : '$m' ;

AND: '.and.';
OR: '.or.';
NOT: '.not.';

SUM: 'sum';
MAX : 'max';
MIN : 'min';
INT : 'int';
REAL: 'real';
ABS: 'abs';
EXP: 'exp';
LOG: 'log';
LOG10: 'log10';
POW: 'pow';
MOD: 'mod';
ROUND: 'round';
SIN : 'sin';
COS : 'cos';
TAN : 'tan';
COT : 'cot';
ASIN : 'asin';
ACOS : 'acos';
ATAN : 'atan';
ACOT : 'acot';
EXCEEDANCE : 'exceedance';
EXCEEDANCE_TSI : 'exceedance_tsi';

SELECT: 'select';
FROM: 'from';
GIVEN: 'given';
USE: 'use';
WHERE : 'where';

CONSTRAIN: 'constrain';
ALWAYS: 'always';

NAME: 'name';
DVAR: 'dvar';
CYCLE: 'cycle';
FILE: 'file';
CONDITION: 'condition';
INCLUDE: 'include';
LOWERBOUND: 'lower_bound';
UPPERBOUND: 'upper_bound';
INTEGERTYPE: 'integer';
UNITS: 'units';
CONVERTUNITS: 'convert_to_units';
TYPE: 'type';
OUTPUT: 'output';
CASE: 'case';
ORDER: 'order';
EXPRESSION: 'expression';
LHSGTRHS: 'lhs_gt_rhs';
LHSLTRHS: 'lhs_lt_rhs';
WEIGHT: 'weight';
FUNCTION: 'function';
FROM_WRESL_FILE: 'from_wresl_file';
UPPERUNBOUNDED: 'upper_unbounded';
LOWERUNBOUNDED: 'lower_unbounded';

SVAR: '{' IDENT '}';
IDENT : LETTER (LETTER | DIGIT | SYMBOLS )*;
IDENT1 : DIGIT (LETTER | DIGIT | SYMBOLS )*;
IDENT2 : SYMBOLS (LETTER | DIGIT | SYMBOLS )*; 

WS : (' ' | '\t' | '\n' | '\r' | '\f')+ {$channel = HIDDEN;};
COMMENT : '!' .* ('\n'|'\r') {$channel = HIDDEN;};


	
