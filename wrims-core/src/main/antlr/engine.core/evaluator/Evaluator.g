grammar Evaluator;

options {
  language = Java;
}

@header {
  package wrims.engine.core.evaluator;
    
  import org.antlr.runtime.ANTLRFileStream;
  import org.antlr.runtime.CharStream;
  import org.antlr.runtime.CommonTokenStream;
  import org.antlr.runtime.RecognitionException;
  import org.antlr.runtime.TokenStream;
  
  import java.util.HashMap;
  import wrims.engine.core.components.Error;
  import wrims.engine.core.components.IntDouble;
  import wrims.engine.core.parallel.ParallelVars; 
}

@lexer::header {
  package wrims.engine.core.evaluator;
}

@members {
  public IntDouble evalValue;
  public EvalExpression evalExpression;
  public EvalConstraint evalConstraint;
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
	:	 goalInput|
	expressionInput |
	softConstraint|
	conditionInput 
	;

///////////////////
/// input rules ///
///////////////////

goalInput: 'g:' constraintStatement {evalConstraint = $constraintStatement.ec;};
expressionInput: 'v:' expressionCollection{evalValue=Evaluation.expressionInput($expressionCollection.ee);};
softConstraint: 's:' expressionCollection{evalExpression=$expressionCollection.ee;};
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
	
expressionCollection returns [EvalExpression ee]
	:	(expression{ee=$expression.ee;})
	|(tableSQL){ee=$tableSQL.ee;}
	|(timeseriesWithUnits)
	|((timeseries){ee=$timeseries.ee;})
	| sumExpression {ee=$sumExpression.ee;}
	|(UPPERUNBOUNDED{ee=new EvalExpression(new IntDouble(1e38,true));})
	|(LOWERUNBOUNDED{ee=new EvalExpression(new IntDouble(-1e38,true));})
	;

func returns[EvalExpression ee]: 
  (max_func{ee=$max_func.ee;})|
  (min_func{ee=$min_func.ee;})|
  (int_func{ee=$int_func.ee;})|
  (real_func{ee=$real_func.ee;})|
  (abs_func{ee=$abs_func.ee;})|
  (exp_func{ee=$exp_func.ee;})|
  (log_func{ee=$log_func.ee;})|
  (log10_func{ee=$log10_func.ee;})|
  (pow_func{ee=$pow_func.ee;})|
  (mod_func{ee=$mod_func.ee;})|
  (round_func{ee=$round_func.ee;})|
  (sin_func{ee=$sin_func.ee;})|
  (cos_func{ee=$cos_func.ee;})|
  (tan_func{ee=$tan_func.ee;})|
  (cot_func{ee=$cot_func.ee;})|
  (asin_func{ee=$asin_func.ee;})|
  (acos_func{ee=$acos_func.ee;})|
  (atan_func{ee=$atan_func.ee;})|
  (acot_func{ee=$acot_func.ee;})|
  (exceedFunc{ee=$exceedFunc.ee;})|
  (exceedtsiFunc{ee=$exceedtsiFunc.ee;}) 
  ;

round_func returns[EvalExpression ee]
  : ROUND '(' (e1=expression)')'{
     ee=Evaluation.round($e1.ee);
  }
  ;

mod_func returns[EvalExpression ee]
  : MOD '(' (e1=expression) (';' (e2=expression)) ')'{
     ee=Evaluation.mod($e1.ee, $e2.ee);
  }
  ;

max_func returns[EvalExpression ee] 
	: MAX '(' (e1=expression){ee=$e1.ee;}(';' (e2=expression{
     ee=Evaluation.max(ee, $e2.ee);
  }))+ ')'
	;

min_func returns[EvalExpression ee]
	: MIN '(' (e1=expression){ee=$e1.ee;}(';' (e2=expression{
     ee=Evaluation.min(ee, $e2.ee);
  }))+ ')'
	;
	
int_func returns[EvalExpression ee]
  : INT '(' (e=expression) ')'{
     ee=Evaluation.intFunc($e.ee);
  }
  ;

real_func returns[EvalExpression ee]
  : REAL '(' (e=expression) ')'{
    ee=Evaluation.realFunc($e.ee);
  }
  ;
  
abs_func returns[EvalExpression ee]
  : ABS '(' (e=expression) ')'{
     ee=Evaluation.abs($e.ee);
  }
  ;

exp_func returns[EvalExpression ee]
  : EXP '(' (e=expression) ')'{
     ee=Evaluation.exp($e.ee);
  }
  ;

log_func returns[EvalExpression ee]
  : LOG '(' (e=expression) ')'{
     ee=Evaluation.log($e.ee);
  }
  ;

log10_func returns[EvalExpression ee]
  : LOG10 '(' (e=expression) ')'{
    ee=Evaluation.log10($e.ee);
  }
  ;
  
pow_func returns[EvalExpression ee]
  : POW '(' (e1=expression) (';' (e2=expression)) ')'{
     ee=Evaluation.pow($e1.ee, $e2.ee);
  }
  ;

sin_func returns[EvalExpression ee]
  : SIN '(' (e=expression) ')'{
    ee=Evaluation.sin($e.ee);
  }
  ;

cos_func returns[EvalExpression ee]
  : COS '(' (e=expression) ')'{
    ee=Evaluation.cos($e.ee);
  }
  ;
  
tan_func returns[EvalExpression ee]
  : TAN '(' (e=expression) ')'{
    ee=Evaluation.tan($e.ee);
  }
  ;
  
cot_func returns[EvalExpression ee]
  : COT '(' (e=expression) ')'{
    ee=Evaluation.cot($e.ee);
  }
  ;

asin_func returns[EvalExpression ee]
  : ASIN '(' (e=expression) ')'{
    ee=Evaluation.asin($e.ee);
  }
  ;

acos_func returns[EvalExpression ee]
  : ACOS '(' (e=expression) ')'{
    ee=Evaluation.acos($e.ee);
  }
  ;
  
atan_func returns[EvalExpression ee]
  : ATAN '(' (e=expression) ')'{
    ee=Evaluation.atan($e.ee);
  }
  ;
  
acot_func returns[EvalExpression ee]
  : ACOT '(' (e=expression) ')'{
    ee=Evaluation.acot($e.ee);
  }
  ;

exceedFunc returns[EvalExpression ee]
  : EXCEEDANCE '(' var=IDENT ';' exc=term ';' (mon=MONTH_CONST|mon=MONTH_RANGE|mon=ALL) ';' sy=INTEGER ';' sm=MONTH_CONST ';' sd=INTEGER ';' ey=INTEGER ';' em=MONTH_CONST ';' ed=INTEGER ')' {
    ee=Evaluation.exceedance($var.text, $exc.ee, $mon.text, $sy.text, $sm.text, $sd.text, $ey.text, $em.text, $ed.text);
  }  
  ;
  
exceedtsiFunc returns[EvalExpression ee]
  : EXCEEDANCE_TSI '(' var=IDENT ';' exc=term ';' (mon=MONTH_CONST|mon=MONTH_RANGE|mon=ALL) ';' sy=INTEGER ';' sm=MONTH_CONST ';' sd=INTEGER ';' ey=INTEGER ';' em=MONTH_CONST ';' ed=INTEGER ')' {
    ee=Evaluation.exceedance_tsi($var.text, $exc.ee, $mon.text, $sy.text, $sm.text, $sd.text, $ey.text, $em.text, $ed.text);
  }  
  ;
  
range_func returns [boolean result]
  : RANGE '(' MONTH ';' m1=MONTH_CONST ';' m2=MONTH_CONST ')' {Evaluation.range($m1.text, $m2.text);};

timeseriesWithUnits 
	: 'timeseries' 'kind' '=' partC 'units' '=' IDENT 
	;

timeseries returns [EvalExpression ee]
	: 'timeseries' {ee=Evaluation.timeseries();}
	;
	

	
partC: 	(IDENT|IDENT1|usedKeywords) ('-' (IDENT|IDENT1|usedKeywords))*;
  
usedKeywords: YEAR|MONTH|MONTH_CONST|MONTH_RANGE|DAY|PASTMONTH|RANGE|TAFCFS|DAYSIN|DAYSINTIMESTEP|SUM|MAX|MIN|INT|REAL|ABS|EXP|LOG|LOG10|POW|MOD|ROUND|SELECT|FROM|GIVEN|USE|WHERE
|CONSTRAIN|ALWAYS|NAME|DVAR|CYCLE|FILE|  CONDITION|INCLUDE|LOWERBOUND|UPPERBOUND|INTEGERTYPE|UNITS|CONVERTUNITS|TYPE|OUTPUT
|CASE|ORDER|EXPRESSION|LHSGTRHS|LHSLTRHS|WEIGHT|FUNCTION|FROM_WRESL_FILE|UPPERUNBOUNDED|LOWERUNBOUNDED|AND|OR|NOT
|SIN|COS|TAN|COT|ASIN|ACOS|ATAN|ACOT|EXCEEDANCE|EXCEEDANCE_TSI|ALL;

tableSQL returns [EvalExpression ee] @init{String table=null; String select=null; String use=null; HashMap<String, Number> given=null; HashMap<String, Number> where=null;}
	: SELECT ((i1=IDENT{select=$i1.text;})|(u1=usedKeywords{select=$u1.text;})) FROM i2=IDENT{table=$i2.text;} 
	  (GIVEN a=assignStatement{given=new HashMap<String, Number>(); given.put($a.assignIdent, $a.value);})? (USE i3=IDENT{use=$i3.text;})? 
	  (where_items{where=$where_items.where;})? {ee=Evaluation.tableSQL(table, select, where, given, use);}	  
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

sumExpression returns [EvalExpression ee] @init{String s="";}
  : SUM '(' IDENT{Evaluation.sumExpression_IDENT($IDENT.text, sumIndex);} '=' e1=expression ';' e2=expression (';' (('-'{s=s+"-";})? INTEGER {s=s+$INTEGER.text;}))? ')' {Evaluation.initSumExpression($e1.ee, $e2.ee, s, sumIndex);} e3=expression{ee=Evaluation.sumExpression($e3.ee, $e3.text, sumIndex);}
  ;

term returns [EvalExpression ee]
	:	(IDENT {ee=Evaluation.term_IDENT($IDENT.text, sumIndex);})
	| (FLOAT {ee=Evaluation.term_FLOAT($FLOAT.text);}) 
	| ('(' (e=expression) ')' {ee=$e.ee;})
	| pastCycleValue{ee=Evaluation.term_knownTS($pastCycleValue.result);}
	| function{ee=$function.ee;}
	| func{ee=$func.ee;}
	| (INTEGER {ee=Evaluation.term_INTEGER($INTEGER.text);}) 
	| tafcfs_term{ee=$tafcfs_term.ee;}
	| YEAR{ee=Evaluation.term_YEAR();}
	| MONTH{ee=Evaluation.term_MONTH();}
	| DAY{ee=Evaluation.term_DAY();}
	| MONTH_CONST{ee=Evaluation.term_MONTH_CONST($MONTH_CONST.text);}
	| PASTMONTH{ee=Evaluation.term_PASTMONTH($PASTMONTH.text);}
	| DAYSIN{ee=Evaluation.daysIn();}
	| DAYSINTIMESTEP{ee=Evaluation.daysInTimeStep();}
	| (SVAR{ee=Evaluation.term_SVAR($SVAR.text.replace("{","").replace("}",""));}) 
	| ARRAY_ITERATOR{ee=Evaluation.term_ARRAY_ITERATOR(prvs);}
	| '(' sumExpression ')'{ee=$sumExpression.ee;}
	;
	
tafcfs_term returns [EvalExpression ee]: TAFCFS ('(' expression ')')? {
    ee=Evaluation.tafcfs_term($TAFCFS.text, $expression.ee, sumIndex);
};
	  
pastCycleValue returns[IntDouble result]
  : (p1=pastCycleNoTimeArray{return $p1.result;})|(p2=pastCycleTimeArray{return $p2.result;})|(p3=pastCycleIndexNoTimeArray{return $p3.result;})|(p4=pastCycleIndexTimeArray{return $p4.result;})
  ;

pastCycleNoTimeArray returns [IntDouble result]
  : i1=IDENT '[' i2=IDENT ']'{result=Evaluation.pastCycleNoTimeArray($i1.text,$i2.text);}
  ; 
  
pastCycleTimeArray returns [IntDouble result]
  : i1=IDENT '[' i2=IDENT ']' '(' e1=expression ')' {result=Evaluation.pastCycleTimeArray($i1.text,$i2.text, $e1.ee);}
  ; 
  
pastCycleIndexNoTimeArray returns [IntDouble result]
  : i1=IDENT '[' '-' index=INTEGER ']'{result=Evaluation.pastCycleIndexNoTimeArray($i1.text,-Integer.parseInt($index.text));}
  ; 
  
pastCycleIndexTimeArray returns [IntDouble result]
  : i1=IDENT '[' '-' index=INTEGER ']' '(' e1=expression ')' {result=Evaluation.pastCycleIndexTimeArray($i1.text,-Integer.parseInt($index.text), $e1.ee);}
  ; 

function returns [EvalExpression ee]
  : (n=noArgFunction{ee=$n.ee;})|(a=argFunction{ee=$a.ee;})
  ;

noArgFunction returns [EvalExpression ee]
  : IDENT '(' ')' {ee=Evaluation.noArgFunction($IDENT.text);};

argFunction returns [EvalExpression ee] @init{ArrayList<ArrayList<EvalExpression>> eeArray = new ArrayList<ArrayList<EvalExpression>>(); ArrayList<EvalExpression> ee0Array=new ArrayList<EvalExpression>();}
  : IDENT '(' (e1=expression {ArrayList<EvalExpression> eeArray1=new ArrayList<EvalExpression>(); eeArray1.add($e1.ee); eeArray.add(eeArray1);}
    |t1=trunk_timeArray{eeArray.add($t1.eeArray);}) 
    (';' (e2=expression{ArrayList<EvalExpression> eeArray1=new ArrayList<EvalExpression>(); eeArray1.add($e2.ee); eeArray.add(eeArray1);}
    |t2=trunk_timeArray{eeArray.add($t2.eeArray);}))* ')' ('(' e0=expression {ee0Array.add($e0.ee);} ')')?
    {
        if (ee0Array.size()==0){
          ee=Evaluation.argFunction($IDENT.text,eeArray,sumIndex);   
        }else{
          ee=Evaluation.pastTSFV($IDENT.text, $e0.ee, eeArray, prvs);
        }
    };

trunk_timeArray returns[ArrayList<EvalExpression> eeArray] @init{eeArray = new ArrayList<EvalExpression>(); IntDouble start=new IntDouble(1, true);  IntDouble end=new IntDouble(1, true);}
  : i0=IDENT '(' (n1=integer{start=ValueEvaluation.term_INTEGER($n1.text);}|i1=IDENT{start=ValueEvaluation.term_IDENT($i1.text, sumIndex);}) ':' (n2=integer{end=ValueEvaluation.term_INTEGER($n2.text);}|i2=IDENT{end=ValueEvaluation.term_IDENT($i2.text, sumIndex);}) ')' 
  {
    eeArray=Evaluation.trunk_timeArray($i0.text, start, end);
  }
  ;
  	
unary returns [EvalExpression ee] 
	:	(s=('+'|'-'))? term{ee=Evaluation.unary($s.text, $term.ee);
	};
	
allnumber 
	:	('-')? number;

mult returns [EvalExpression ee]  
	:	(u1=unary {ee=$u1.ee;}) (s=('*'| '/') (u2=unary){
	   if ($s.text.equals("*")){
	     ee=Evaluation.mult(ee, $u2.ee);
	   }else{
	     ee=Evaluation.divide(ee, $u2.ee);
	   }
  })*
	;
	
add  returns [EvalExpression ee]
	:	(m1=mult {ee=$m1.ee;}) ((s=('+'|'-')) (m2=mult){
     if ($s.text.equals("+")){
       ee=Evaluation.add(ee, $m2.ee);
     }else{
       ee=Evaluation.substract(ee, $m2.ee);
     }
	})*
	;

expression returns [EvalExpression ee]  
	:	i=add {$ee=$add.ee;} 
	;

relation
	: '=='
	| '<'
	| '>'
	| '>='
	| '<='
	;	

whereStatement returns [String whereIdent, Number value]
  : ((i=IDENT{$whereIdent=$i.text;})|(u=usedKeywords{$whereIdent=$u.text;})) '=' expression{$value=Evaluation.assignWhereStatement($expression.ee);} 
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
    (s=OR r2=relationAnd {result=Evaluation.relationStatementSeries(result, $r2.result, $s.text);})* ;

relationAnd returns [boolean result] 
  : r1=relationRangeStatement {result=$r1.result;} 
    (s=AND r2=relationRangeStatement {result=Evaluation.relationStatementSeries(result, $r2.result, $s.text);})* ;

relationRangeStatement returns [boolean result]
  : (r1=relationStatement{result=$r1.result;})|(r2=range_func{result=$r2.result;})
  ;  

relationStatement returns [boolean result] 
	: (	( expression relation expression ) => (e1=expression) relation (e2=expression) {result=Evaluation.relationStatement($e1.ee, $e2.ee, $relation.text);} )
	| ( ( '('relationUnary')'  ) => '('r2=relationUnary')' {result=$r2.result;} )
	;

constraintStatement returns [EvalConstraint ec]
  : e1=expression ((s='=')|(s='>')|(s='<')) e2=expression{ec=Evaluation.constraintStatement($e1.ee, $e2.ee, $s.text);}
  ;

assignStatement returns [String assignIdent, Number value]  
  : IDENT '=' expression {$assignIdent=$IDENT.text; $value=Evaluation.assignWhereStatement($expression.ee);} 
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


	
