package wrimsv2.commondata.tabledata;

import java.util.ArrayList;

public class Svar {

	private String type;
	private String units;
	private String convertUnits;
	private ArrayList<String> caseCondition;
	private ArrayList<ArrayList<String>> caseExpression;

	public Svar(){
		type="undefined";
		units="undefined";
		caseCondition = new ArrayList<String>();
		caseExpression = new ArrayList<ArrayList<String>> ();
	}

	public void setType(String type){
		this.type=type;
	}

	public void setUnits(String units){
		this.units=units;
	}
	
	public void setConvertUnits(String convertUnits){
		this.convertUnits=convertUnits;
	}

	public void setCaseCondition(ArrayList<String> caseCondition){
		this.caseCondition=caseCondition;
	}

	public void setCaseExpression(ArrayList<ArrayList<String>> caseExpression){
		this.caseExpression=caseExpression;
	}

	public ArrayList<String> getCaseCondition(){
		return caseCondition;
	}

	public ArrayList<ArrayList<String>> getCaseExpression(){
		return caseExpression;
	}
	
	public String getType(){
		return type;
	}

	public String getUnits(){
		return units;
	}
	
	public String getConvertUnits(){
		return convertUnits;
	}
}

