package wrimsv2.wreslplus.elements;

import java.io.Serializable;
import java.util.ArrayList;

import wrimsv2.commondata.wresldata.Param;

public class WeightSubgroup implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public String id;
	public ArrayList<String> varList;
	public String deviationPenalty;
	public String deviationTolerance;
	//public ArrayList<WeightSubgroup> subgroup;
	
	public WeightSubgroup(){
		varList = new ArrayList<String>();
		deviationPenalty = Param.zero; // default is zero
		deviationTolerance = Param.zero;
		//subgroup = new ArrayList<WeightSubgroup>();

	}
	
}
	
