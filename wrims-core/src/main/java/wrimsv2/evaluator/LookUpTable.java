package wrimsv2.evaluator;

import java.util.HashMap;
import java.util.ArrayList;

public class LookUpTable {
	private String name=null;
	private HashMap<String, Integer> field = new HashMap<String, Integer>();
	private ArrayList<Number[]> data=new ArrayList<Number[]>();
	
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name=name;
	}
	
	public HashMap<String, Integer> getField(){
		return field;
	}
	
	public void setField(HashMap<String, Integer> field){
		this.field=field;
	}
	
	public ArrayList<Number[]> getData(){
		return data;
	}
}
