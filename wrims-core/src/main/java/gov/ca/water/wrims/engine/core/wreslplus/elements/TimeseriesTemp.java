package gov.ca.water.wrims.engine.core.wreslplus.elements;

import java.io.Serializable;

import gov.ca.water.wrims.engine.core.commondata.wresldata.Param;


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
	
