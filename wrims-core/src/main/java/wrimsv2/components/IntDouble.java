package wrimsv2.components;

import java.io.Serializable;

public class IntDouble{
	private Number data;
	private boolean isInteger;
	private String argName="";
	private int index;
	
	public IntDouble() {
	}
	
	public IntDouble(Number value, boolean isInt) {
		data=value;
		isInteger=isInt;
		argName="";
	}
	
	public IntDouble(Number value, boolean isInt, String name, int index) {
		data=value;
		isInteger=isInt;
		argName=name;
		this.index=index;
	}
	
	public Number getData(){
		return data;
	}
	
	public void setData (Number value){
		data=value;
	}
	
	public boolean isInt(){
		return isInteger;
	}
	
	public void setInt(boolean isInt){
		isInteger=isInt;
	}
	
	public IntDouble copyOf(){
		IntDouble newIntDouble;
		if (isInteger){
			newIntDouble= new IntDouble(data.intValue(), isInteger);
		}else{
			newIntDouble= new IntDouble(data.doubleValue(), isInteger);
		}
		return newIntDouble;
	}
	
	public String getName(){
		return argName;
	}
	
	public void setName(String argName){
		this.argName=argName;
	}
	
	public int getIndex(){
		return index;
	}
	
	public void setIndex(int index){
		this.index=index;
	}
}
