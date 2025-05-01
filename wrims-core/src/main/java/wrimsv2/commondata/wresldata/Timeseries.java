package wrimsv2.commondata.wresldata;

import java.io.Serializable;

import wrimsv2.components.IntDouble;


public class Timeseries implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public String scope;
	public String dssBPart;
	public String format;
	public String kind;
	public String units;
	public String convertToUnits;
	public String fromWresl;
	public int line=1;
	private IntDouble data;
	
	
	public Timeseries(){
		scope=Param.undefined;
		dssBPart=Param.undefined;
		format=Param.undefined;
		kind=Param.undefined;
		units=Param.undefined;
		convertToUnits =Param.undefined;
		fromWresl = Param.undefined;
	}
	
	public void setData(IntDouble data){
		this.data=data;
	}
	
	public IntDouble getData(){
		return data;
	}
}
	
