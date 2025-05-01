package wrimsv2.evaluator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import wrimsv2.components.ControlData;
import wrimsv2.components.IntDouble;

public class EvalExpression {
	private IntDouble intDouble;
	private LinkedHashMap<String, IntDouble> multiplier;
	
	public EvalExpression(){
		intDouble=new IntDouble(0, true);
		multiplier = new LinkedHashMap<String, IntDouble>();
	}
	
	public EvalExpression(IntDouble id){
		intDouble=id;
		multiplier = new LinkedHashMap<String, IntDouble>();
	}
	
	public IntDouble getValue(){
		return intDouble;
	}
	
	public void setValue(IntDouble intDouble){
		this.intDouble=intDouble;
	}
	
	public LinkedHashMap<String, IntDouble> getMultiplier(){
		return multiplier;
	}
	
	public void setMultiplier(LinkedHashMap<String, IntDouble> multiplier){
		this.multiplier=multiplier;
	}
	
	public boolean isNumeric(){
		if (multiplier.size()==0){
			return true;
		}else{
			return false;
		}
	}
	
	public EvalExpression copyOf(){
		EvalExpression evalExpression = new EvalExpression();
		evalExpression.setMultiplier(copyOfMultiplier());
		evalExpression.setValue(intDouble.copyOf());
		return evalExpression;
	}
	
	public LinkedHashMap<String, IntDouble> copyOfMultiplier(){
		LinkedHashMap<String, IntDouble> newMultiplier=new LinkedHashMap<String, IntDouble>();
		Set multCollection = multiplier.keySet();
		Iterator multIterator = multCollection.iterator();
		
		while(multIterator.hasNext()){
			String multName=(String)multIterator.next();
			newMultiplier.put(multName, multiplier.get(multName).copyOf());
		}
		return newMultiplier;
	}
}
