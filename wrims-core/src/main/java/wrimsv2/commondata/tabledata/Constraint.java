package wrimsv2.commondata.tabledata;

import java.util.ArrayList;

public class Constraint {

	private String constraintName;
	private ArrayList<String> caseCondition;
	private ArrayList<String> caseExpression;


	public Constraint(){
		caseCondition  = new ArrayList<String>();
		caseExpression = new ArrayList<String>();
	}

	public void setCaseCondition(ArrayList<String> caseCondition){
		this.caseCondition=caseCondition;
	}

	public void setCaseExpression(ArrayList<String> caseExpression){
		this.caseExpression=caseExpression;
	}

	public ArrayList<String> getCaseCondition(){
		return caseCondition;
	}

	public ArrayList<String> getCaseExpression(){
		return caseExpression;
	}
}

