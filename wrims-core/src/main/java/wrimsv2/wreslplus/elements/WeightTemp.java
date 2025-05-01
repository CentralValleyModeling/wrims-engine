package wrimsv2.wreslplus.elements;

import java.io.Serializable;

import wrimsv2.commondata.wresldata.Param;

public class WeightTemp implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public String id;
	public String weight;
	public String condition;
	public String fromWresl;
	public int line=1;
	
	public String timeArraySize;
	public String arraySize;
	
	public WeightTemp(){
		weight = Param.undefined;
		condition = Param.always;
		fromWresl = Param.undefined;	
		
		timeArraySize="0";
		arraySize="0";

	}
	
}
	
