package wrimsv2.wreslplus.elements;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import wrimsv2.commondata.wresldata.Param;



public class DvarTemp implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public String fromWresl; // for test only
	public int line=1;
	public String id;
	public boolean isInteger;
	public String format;
	public String kind;
	public String units;
	public String lowerBound;
	public String upperBound;
	public String condition;
	public String expression;
	public Set<String> dependants;
	public boolean isFromAlias;
	
	// default is 0
	public String timeArraySize;
	public String arraySize;
	
	public DvarTemp(){
		
		isInteger=false;
		isFromAlias=false;
		format=Param.undefined;
		kind=Param.undefined;
		units=Param.undefined;
		lowerBound=Param.dv_std_lowerBound;
		upperBound=Param.dv_std_upperBound;
		condition = Param.always;
		expression=Param.undefined;
		dependants = new HashSet<String>();	
		
		timeArraySize="0";
		arraySize="0";
	}
	
}
	
