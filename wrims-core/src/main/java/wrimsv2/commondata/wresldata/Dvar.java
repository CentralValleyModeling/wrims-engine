package wrimsv2.commondata.wresldata;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import wrimsv2.components.IntDouble;
import wrimsv2.evaluator.ValueEvaluatorParser;


public class Dvar implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public String fromWresl;
	public int line=1;
	public String scope;
	public String integer;
	public String format;
	public String kind;
	public String units;
	public String lowerBound;
	public ValueEvaluatorParser lowerBoundParser;
	public String upperBound;
	public ValueEvaluatorParser upperBoundParser;
	public String condition;
	public Number upperBoundValue;
	public Number lowerBoundValue;
	public String expression;
	public Set<String> dependants;
	public IntDouble data;
		
	// default is zero
	public String timeArraySize;
	public ValueEvaluatorParser timeArraySizeParser;
	
	
	public Dvar(){
		scope=Param.undefined;
		integer=Param.no;
		format=Param.undefined;
		kind=Param.undefined;
		units=Param.undefined;
		lowerBound=Param.undefined;
		upperBound=Param.undefined;
		condition = Param.always;
		fromWresl=Param.undefined;
		expression=Param.undefined;
		dependants = new HashSet<String>();

		lowerBoundParser = null;
		upperBoundParser = null;
		data = null;
		upperBoundValue = null;
		lowerBoundValue = null;
		
		timeArraySize = "0"; // default has no future time array		
	}
	
	public void setData(IntDouble data){
		this.data=data;
	}
	
	public IntDouble getData(){
		return data;
	}
}
	
