grammar IncFileFinder;
//import CommonLexer;

options {
  language = Java;
  //output=AST;
  //ASTLabelType=CommonTree;
}

@header {
  package wrimsv2.wreslplus.grammar;
  import wrimsv2.wreslparser.elements.LogUtils; 
  import wrimsv2.wreslplus.elements.IncFileSimple; 
  import wrimsv2.wreslplus.elements.LookupTableSimple; 
  import java.util.HashMap;
  import java.util.Set;
  import java.util.HashSet;
  import java.util.LinkedHashSet;
  import java.io.File;
}
@lexer::header {
  package wrimsv2.wreslplus.grammar;
  import wrimsv2.wreslparser.elements.LogUtils; 
}

@members {
    //public ArrayList<String> incFileList;
    public ArrayList<IncFileSimple> incFileSimpleList;
    public ArrayList<LookupTableSimple> lookupTableSimpleList;
	  //public CommonTree commonTree;
	  public String currentAbsolutePath;
  	public String currentAbsoluteParent;
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
	
	public String currentAbsolutePath;
	public int number_of_errors = 0;
	/// error message	
    public void displayRecognitionError(String[] tokenNames,
                                        RecognitionException e) {
        String hdr = getErrorHeader(e);
        String msg = getErrorMessage(e, tokenNames);
        LogUtils.errMsgLocation(currentAbsolutePath, e.line, msg);
        number_of_errors++;
    }
}


wreslFile 
@init{ //incFileList = new ArrayList<String>(); 
       incFileSimpleList = new ArrayList<IncFileSimple>();
       lookupTableSimpleList = new ArrayList<LookupTableSimple>(); }
  : .*  (  
      ( f=include_file 
        { //incFileList.add(new File(currentAbsoluteParent, $f.fp_string).toString());
          $f.incFileObj.absPath=new File(currentAbsoluteParent, $f.fp_string).toString();
          incFileSimpleList.add($f.incFileObj);
        }
      | include_group  
      | include_model
      | l=lookup 
        { lookupTableSimpleList.add($l.lookupTableObj);
        }) 
      .*  ) * ;

include_group : INCLUDE GROUP ID ;
include_model : INCLUDE MODEL ID ;
include_file returns[String fp_string, IncFileSimple incFileObj]
@init {$incFileObj = new IncFileSimple();
       $incFileObj.fromWresl = this.currentAbsolutePath; 
       }
       
@after{$incFileObj.line=line;
       } 
      : i=INCLUDE{line=$i.line;}(local_deprecated)? fp=file_path {$fp_string = $fp.text.substring(1,$fp.text.length()-1);}   
      ;

file_path : QUOTE  ;

local_deprecated
  : '[' LOCAL ']' ;


lookup returns[String tp_string, LookupTableSimple lookupTableObj]
@init {$lookupTableObj = new LookupTableSimple();
       $lookupTableObj.fromWresl = this.currentAbsolutePath; 
       }
       
@after{$lookupTableObj.line=line;
       } 
: SELECT ID i=FROM {line=$i.line;} ft=fromTable {$lookupTableObj.tableName=$ft.text;} ;

fromTable :
ID ;

QUOTE : '\''  (Letter | Digit | '\\' | '_' | '.' | '-' | '/' )+  '\'' ;

ML_COMMENT : '/*' .* '*/' {skip();}; 

SL_COMMENT : ('#'|'!') ~('\r'|'\n')*  '\r'? ( '\n' | EOF ) {skip();};  //{$channel=HIDDEN;} ;

INCLUDE :   'include' | 'INCLUDE' | 'Include' ;
LOCAL : 'local' | 'LOCAL' | 'Local' ; 
MODEL : 'model' | 'MODEL' | 'Model' ;
GROUP : 'group' | 'GROUP' | 'Group' ;
SELECT : 'select'|'SELECT'|'Select';
FROM : 'from'|'FROM'|'From';
WHERE: 'where'|'WHERE'|'Where';

WS : (' ' | '\t' | '\n' | '\r' | '\f')+ {skip();}; //{$channel = HIDDEN;};

ID : Letter ( Letter | Digit | '_' )* ;
N : Digit+ {skip();};

fragment Letter : 'a'..'z' | 'A'..'Z';

fragment Digit : '0'..'9';

Others : '{'|'}'|'\\'|'.'|'='|'('|')'|'\r'|'\n'|'<'|'>'|'+'|'-'|'*'|'/'|','|'$' {skip();};
