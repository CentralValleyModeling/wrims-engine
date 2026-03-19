grammar ValueEvaluatorTree;

options {
  language = Java;
  output=AST;
  ASTLabelType=CommonTree;
}

@header {
  package gov.ca.water.wrims.engine.core.evaluator;
    
  import org.antlr.runtime.ANTLRFileStream;
  import org.antlr.runtime.CharStream;
  import org.antlr.runtime.CommonTokenStream;
  import org.antlr.runtime.RecognitionException;
  import org.antlr.runtime.TokenStream;
  
  import java.util.HashMap;
  import gov.ca.water.wrims.engine.core.components.Error;
  import gov.ca.water.wrims.engine.core.components.IntDouble;
}

@lexer::header {
  package gov.ca.water.wrims.engine.core.evaluator;
}

@members {
 
  @Override
  public void reportError(RecognitionException e) {
       Error.addEvaluationError(getErrorMessage(e, tokenNames));
  }
}

evaluator 
	:	expressionInput |
		conditionInput 
	;

///////////////////
/// input rules ///
///////////////////

expressionInput: 'v:' expressionCollection-> ^('v:' expressionCollection);
conditionInput: 'c:' conditionStatement-> ^('c:' conditionStatement);

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

conditionStatement 
  : ((r=relationStatementSeries->$r) |(ALWAYS->ALWAYS))
  ;

relationStatementSeries  
  : r1=relationRangeStatement  
    (((s=('.and.'^|'.or.'^))) r2=relationRangeStatement)* ;

relationRangeStatement
  : (relationStatement -> relationStatement)|(range_func -> range_func)
  ;

relationStatement  
  : (e1=expression) relation (e2=expression) -> ^(relation $e1 $e2)
  ;
	
expressionCollection
	:((expression->expression)
	|(tableSQL->tableSQL)
	|(timeseries->timeseries)
	|(sumExpression->sumExpression)
	|(UPPERUNBOUNDED ->UPPERUNBOUNDED)
	|(LOWERUNBOUNDED ->LOWERUNBOUNDED))
	;

func: 
  (max_func|
  min_func|
  int_func|
  real_func|
  abs_func|
  exp_func|
  log_func|
  log10_func|
  pow_func);

max_func 
	: MAX '(' (e1=expression) (';' (e2=expression))+ ')' -> ^(MAX $e1 $e2+) 
	;

min_func
	: MIN '(' (e1=expression)(';' (e2=expression))+ ')' -> ^(MIN $e1 $e2+)
	;
	
int_func
  : INT '(' (e=expression) ')' -> ^(INT $e)
  ;
  
real_func
  : REAL '(' (e=expression) ')' -> ^(REAL $e)
  ;
  
abs_func 
  : ABS '(' (e=expression) ')'-> ^(ABS $e)
  ;

exp_func 
  : EXP '(' (e=expression) ')' -> ^(EXP $e)
  ;

log_func 
  : LOG '(' (e=expression) ')' -> ^(LOG $e)
  ;

log10_func 
  : LOG10 '(' (e=expression) ')'-> ^(LOG10 $e)
  ;
  
pow_func 
  : POW '(' (e1=expression) (';' (e2=expression)) ')'-> ^(POW $e1 $e2)
  ;
  
range_func 
  : RANGE '(' MONTH ';' m1=MONTH_CONST ';' m2=MONTH_CONST ')' -> ^(RANGE MONTH $m1 $m2) 
  ;

timeseries 
	: TIMESERIES ->TIMESERIES
	;
	

	
partC: 	(IDENT|IDENT1|usedKeywords) ('-' (IDENT|IDENT1|usedKeywords))*;
  
usedKeywords: YEAR|MONTH|MONTH_CONST|PASTMONTH|RANGE|TAFCFS|DAYSIN|SUM|MAX|MIN|INT|REAL|ABS|EXP|LOG|LOG10|POW|MOD|UNARY|SELECT|FROM|GIVEN|USE|WHERE
|TIMESERIES|CONSTRAIN|ALWAYS|NAME|DVAR|CYCLE|FILE|CONDITION|INCLUDE|LOWERBOUND|UPPERBOUND|INTEGERTYPE|UNITS|CONVERTUNITS|TYPE|OUTPUT
|CASE|ORDER|EXPRESSION|LHSGTRHS|LHSLTRHS|WEIGHT|FUNCTION|FROM_WRESL_FILE|UPPERUNBOUNDED|LOWERUNBOUNDED;

tableSQL	: SELECT selectName FROM i1=IDENT (GIVEN a=assignStatement)? (USE i2=IDENT)? (where_items)? 	  -> ^(SELECT selectName $i1 assignStatement? $i2? where_items?)
	;
	
selectName: (IDENT -> IDENT)|(usedKeywords -> usedKeywords);

where_items
	:	 WHERE  (w1=whereStatement)
	        (addWhereStatement)* ->^(WHERE $w1 addWhereStatement*)
	;

addWhereStatement
  : ';' whereStatement -> whereStatement
  ;

//upperbound:	(IDENT -> IDENT)|(allnumber->allnumber)|(((allnumber '*' TAFCFS) ->^('*' allnumber TAFCFS)));

//lowerbound:	(IDENT -> IDENT)|(allnumber->allnumber)|(((allnumber '*' TAFCFS) ->^('*' allnumber TAFCFS)));

//sumExpression 
//  : SUM '(' I '=' e1=expression_sum ';' e2=expression_sum (';' (s='-')? INTEGER )? ')' e3=expression
//  ;
//term_sum: (MONTH|MONTH_CONST|PASTMONTH|I|INTEGER|'(' expression_sum ')');

//unary_sum : ('-')? term_sum ;
//add_sum  :  unary_sum(('+' | '-') unary_sum)* ;
//expression_sum: add_sum ;

//sumExpression was redesign. If not work, switch back to the original design above

sumExpression 
  : SUM '(' IDENT '=' e1=expression ';' e2=expression (';' (('-')? INTEGER ))? ')'  e3=expression 
    -> ^(SUM IDENT $e1 $e2 '-'? INTEGER? $e3)
  ;

term 
	:	(IDENT -> IDENT)
	| (FLOAT -> FLOAT) 
	| ('(' (expression) ')' -> expression)
	| (knownTS -> knownTS) 
	| (func -> func)
	| (INTEGER -> INTEGER)
	| (tafcfs_term -> tafcfs_term)
	| (YEAR ->YEAR)
	| (MONTH ->MONTH)
	| (MONTH_CONST ->MONTH_CONST)
	| (PASTMONTH -> PASTMONTH)
	| (DAYSIN -> DAYSIN)
	| (SVAR -> SVAR)
	;
	
tafcfs_term : TAFCFS ('(' expression ')')? -> ^(TAFCFS expression?) 
  ;
	
knownTS 
  : (f=function -> $f)|(p=pastCycleDV ->$p) 
  ;
  
//pastMonthTS  
//  : ((i1=IDENT)|TAFCFS) '(' ((p=PASTMONTH)|(i=I)|(pm=(MONTH_CONST '-' MONTH (('+'|'-') INTEGER)? ))|(mp=(MONTH '-' MONTH_CONST (('+'|'-') INTEGER)?))) ')'
//  ;
  
//preMonthTS 
//  : IDENT '(' (s='-')? INTEGER ')'  
//  ;
  
pastCycleDV 
  : i1=IDENT '[' i2=IDENT ']' -> ^($i1 '[' $i2 ']')
  ; 

function 
  : (n=noArgFunction-> $n)|(a=argFunction->$a)
  ;

noArgFunction 
  : IDENT '(' ')' -> ^(IDENT '(' ')');

argFunction 
  : IDENT '(' e1=expression  addarg* ')' -> ^(IDENT '(' $e1 addarg* ')' ) 
  ;
  
addarg
  : ';' expression -> expression
  ;
  	
unary 
	:	(s='-')? term -> {s==null}? term
	                -> ^(UNARY term)
	;
	
//allnumber 
//	:	(s='-')? number -> {s==null}? number
//                  -> ^('-' number)
//  ;

mult   
	:	(u1=unary) (s=('*'^| '/'^| MOD^) (u2=unary))*
	;
	
add  
	:	(m1=mult) ((s=('+'^|'-'^)) (m2=mult))*
	;

expression 
	:	add  -> add
	;

relation
	: '=='
	| '<'
	| '>'
	| '>='
	| '<='
	;	

whereStatement
  : whereName '=' expression -> ^(WHERE whereName expression)
  ;
  
whereName: (IDENT -> IDENT)|(usedKeywords -> usedKeywords);

assignStatement   
  : IDENT '=' expression -> ^('=' IDENT expression) 
  ;

number
	: (INTEGER->INTEGER) 
	| (FLOAT->FLOAT)
	;

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
MONTH_CONST: 'jan'|'feb'|'mar'|'apr'|'may'|'jun'|'jul'|'aug'|'sep'|'oct'|'nov'|'dec';
PASTMONTH: 'prevjan'|'prevfeb'|'prevmar'|'prevapr'|'prevmay'|'prevjun'|'prevjul'|'prevaug'|'prevsep'|'prevoct'|'prevnov'|'prevdec';
RANGE: 'range';

TAFCFS: 'taf_cfs'|'cfs_taf'|'cfs_af'|'af_cfs';
DAYSIN: 'daysin'|'daysinmonth';

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
UNARY: 'unary';

SELECT: 'select';
FROM: 'from';
GIVEN: 'given';
USE: 'use';
WHERE : 'where';
TIMESERIES: 'timeseries';

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


	
