package wrimsv2.commondata.tabledata;

import java.util.ArrayList;

public class LRWeight {

	private String constraintName;
	private ArrayList<String> caseCondition;
	private ArrayList<String> caseExpression;
	private ArrayList<String> caseWeight;


	public LRWeight(){
		caseCondition  = new ArrayList<String>();
		caseExpression = new ArrayList<String>();
		caseWeight = new ArrayList<String>();
	}

	public void setCaseCondition(ArrayList<String> caseCondition){
		this.caseCondition=caseCondition;
	}

	public void setCaseExpression(ArrayList<String> caseExpression){
		this.caseExpression=caseExpression;
	}

	public void setCaseWeight(ArrayList<String> weight){
		this.caseWeight=weight;
	}

	public ArrayList<String> getCaseCondition(){
		return caseCondition;
	}

	public ArrayList<String> getCaseExpression(){
		return caseExpression;
	}

	public ArrayList<String> getCaseWeight(){
		return caseWeight;
	}
}

