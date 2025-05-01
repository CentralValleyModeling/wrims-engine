package wrimsv2.wreslplus.elements;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import wrimsv2.commondata.wresldata.Param;
import wrimsv2.components.IntDouble;



public class TimeseriesTemp implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public String id;
	public String dssBPart;
	public String kind;
	public String units;
	public String convertToUnits;
	public String fromWresl;
	public int line=1;
	
	public TimeseriesTemp(){

		dssBPart=Param.undefined;
		kind=Param.undefined;
		units=Param.undefined;
		convertToUnits =Param.undefined;
		fromWresl = Param.undefined;
	}
	
}
	
