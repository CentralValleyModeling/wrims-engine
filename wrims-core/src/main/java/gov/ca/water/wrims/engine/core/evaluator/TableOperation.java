package gov.ca.water.wrims.engine.core.evaluator;

import gov.ca.water.wrims.engine.core.components.FilePaths;
import gov.ca.water.wrims.engine.core.components.Error;
import gov.ca.water.wrims.engine.core.components.IntDouble;

import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class TableOperation {
	public static boolean retrieveLookUpData (String name){
		String tableFullPath=FilePaths.mainDirectory+"lookup"+File.separator+FilePaths.lookupSubDirectory+File.separator+name+".table";
		try{
				File f = new File(tableFullPath);
				if(!f.exists()) {
					Error.addEvaluationError("Table "+name+" could not be found.");
					return false;
				}
			
				FileInputStream fstream = new FileInputStream(tableFullPath);
			    DataInputStream in = new DataInputStream(fstream);
			    BufferedReader br = new BufferedReader(new InputStreamReader(in));
			    
			    String strLine;
			    boolean isComment =true;
			    boolean isEnd=false;
			    int line=0;
			    strLine="";
			    
			    while (!isEnd && isComment){
			    	strLine=br.readLine();
			    	strLine=removeLeadingTailingSpace(strLine);
			    	line=line+1;
			    	if (strLine==null){
			    		isEnd=true;
			    	}else{
			    		if (!strLine.startsWith("!")) isComment=false;
			    	}
			    }	
			    if (strLine==null){
			    	Error.addEvaluationError("No data exists in the table "+name);
			    	in.close();
			    	return false;
			    }
			    
			    if (strLine.contains("!")) strLine=removeComment(strLine);
			    strLine=removeLeadingTailingSpace(strLine);
			    if (!(strLine.toLowerCase().equals(name))){
			    	Error.addEvaluationError("The first line after comments in the table "+name+".table should be the file name without extension: "+name);
			    }
			    
			    if ((strLine=br.readLine())==null){
			    	Error.addEvaluationError("No data exists in the table "+name);
			    	in.close();
			    	return false;
			    }
			    line=line+1;
			    LookUpTable lut=new LookUpTable();
			    if (strLine.contains("!")) strLine=removeComment(strLine);
		    	strLine=removeLeadingTailingSpace(strLine);
			    String[] fieldNames=strLine.toLowerCase().split("\\s+");
			    int fieldSize=fieldNames.length;
			    for (int i=0; i<fieldSize; i++){
			    	if (!isFieldNameRight(fieldNames[i])){
			    		Error.addEvaluationError("The No. " +(i+1)+" field name in the table "+name+" line"+line+" has a wrong format");
				    	in.close();
				    	return false;
			    	}
			    	lut.getField().put(fieldNames[i], i);
			    }
			    
			    while ((strLine=br.readLine())!=null){
			    	line=line+1;
			    	if (strLine.contains("!")) strLine=removeComment(strLine);
			    	strLine=removeLeadingTailingSpace(strLine);
			    	String[] values=strLine.split("\\s+");
			    	if (values.length!=fieldSize){
			    		if (values[0].equals("") && lut.getData().size()>0) {
			    			TableSeries.tableSeries.put(name,lut);
			    			in.close();
			    			return true;
			    		}
				    	Error.addEvaluationError("The number of data in the table "+name+" line "+line+" doesn't agree with the number of the field");
				    	in.close();
				    	return false;
			    	}
			    	
			    	Number[] dataLine=new Number[fieldSize];
			    	for (int i=0; i<fieldSize; i++){
			    		try{        
			    			dataLine[i]=Double.parseDouble(values[i]);
			    		} catch(NumberFormatException nfe1) {        
			    	        Error.addEvaluationError("The No. " +(i+1)+" data in the table "+name+" line"+line+" is not numeric");
						    in.close();
						    return false;
			    		}   
			    	}
			    	lut.getData().add(dataLine);
			    }

			    if (lut.getData().size()<1){
			    	Error.addEvaluationError("No data exists in the table "+name);
			    	in.close();
			    	return false;
			    }else{
			    	TableSeries.tableSeries.put(name,lut);
			    }
			    
			    in.close();
		 }catch (Exception e){
			    Error.addEvaluationError(e.getMessage());
		 }
		 return true;
	}
	
	public static String removeComment(String text){
		int index=text.indexOf("!");
		return text.substring(0,index);
	}
	
	public static String removeLeadingTailingSpace(String text){
	    text=text.replaceAll("^\\s+", "");
	    text=text.replaceAll("\\s+$", "");
	    return text;
	}
	
	public static boolean isFieldNameRight(String fieldName){
		if (Character.isDigit(fieldName.charAt(0))){
			return false;
		}
		Pattern alphaNumberic = Pattern.compile("[A-Za-z0-9_]+");
		Matcher m = alphaNumberic.matcher(fieldName);
		return m.matches();	
	}
	
	public static IntDouble findData(String table, String select, HashMap<String, Number> where, HashMap<String, Number> given, String use){
		if (!TableSeries.tableSeries.containsKey(table)){
			if (!retrieveLookUpData(table)){
				return new IntDouble(1.0,false);
			}
		}
		
		LookUpTable lut=TableSeries.tableSeries.get(table);
		ArrayList<Number[]> data=lut.getData();
		HashMap<String, Integer> field= lut.getField();
		int fieldSize=field.size();
		
		int selectIndex;
		if (field.containsKey(select)){
			selectIndex=field.get(select);
		}else{
			Error.addEvaluationError(select+" in the select statement is not a field name in Table "+table);
			return new IntDouble(1.0,false);
		}
		
		Set whereSet=where.keySet();
		Iterator iterator=whereSet.iterator();
		int whereSize=where.size();
		int[] whereIndex=new int[whereSize];
		Number[] whereValue=new Number[whereSize];
		int k=0;		
		while(iterator.hasNext()){
			String whereName=(String)iterator.next();
			if (field.containsKey(whereName)){
				whereIndex[k]=field.get(whereName);
			}else{
				Error.addEvaluationError(whereName+" in the where statement is not a field name in Table "+table);
				return new IntDouble(1.0,false);
			}
			whereValue[k]=(Number)where.get(whereName);
			k=k+1;
		}
			
		boolean whereTrue;
		if (whereSize==0){
			whereTrue=true;
		}else{
			whereTrue=false;
		}
		
		Number[] values=new Number[fieldSize];
		int i=-1;
		while (i<data.size()-1 && !whereTrue){
			i++;
			values=data.get(i);
			boolean eachWhereTrue=true;
			k=-1;
			while (k<whereSize-1 && eachWhereTrue){
				k++;
				if (values[whereIndex[k]].doubleValue()!=whereValue[k].doubleValue()){
					eachWhereTrue=false;
				}
			}
			if (eachWhereTrue) whereTrue=true;
		}
		
		if (!whereTrue){
			String whereError="";
			for (String key: where.keySet()){
				whereError=whereError+"("+key+": "+where.get(key)+")";
			}
			Error.addEvaluationError("Under those where statements"+whereError+", data could not be found in Table "+table);
			return new IntDouble(1.0,false);
		}
		
		Number value=values[selectIndex];
		if (given==null){
			String valueString=value.toString();
			return generateIntDouble(valueString);
		}
		
		int givenIndex;
		Set givenSet=given.keySet();
		iterator=givenSet.iterator();
		String givenName=(String)iterator.next();
		String valueString;
		if (field.containsKey(givenName)){
			givenIndex=field.get(givenName);
		}else{
			Error.addEvaluationError(givenName+" in the given statement is not a field name in Table "+table);
			return new IntDouble(1.0,false);
		}
		Number givenValue=(Number)given.get(givenName);
		
		ArrayList<Number> gVList=new ArrayList<Number>();
		Map<Number, Number> gVMap=new HashMap<Number, Number>();
		gVList.add(values[givenIndex]);
		gVMap.put(values[givenIndex], values[selectIndex]);
			
		while (i<data.size()-1){
			i++;
			values=data.get(i);
			boolean eachWhereTrue=true;
			k=-1;
			while (k<whereSize-1 && eachWhereTrue){
				k++;
				if (values[whereIndex[k]].doubleValue()!=whereValue[k].doubleValue()){
					eachWhereTrue=false;
				}
			}
			if (eachWhereTrue) {
				if (gVList.contains(values[givenIndex])){
					Error.addEvaluationError("Given value "+values[givenIndex]+" in the given statement is duplicated in Table "+table);
					return new IntDouble(1.0,false);
				}else{
					gVList.add(values[givenIndex]);
					gVMap.put(values[givenIndex], values[selectIndex]);
				}
			}else{
				eachWhereTrue=true;
			}
		}
		
		String givenError="";
		for (String key: given.keySet()){
			givenError=givenError+"("+key+": "+given.get(key)+")";
		}
		
		return calculateValue(givenValue, gVList, gVMap, use, table, givenError);
	}
	
	public static IntDouble findData(String table, String select, HashMap<String, Number> given, String use){
		if (!TableSeries.tableSeries.containsKey(table)){
			if (!retrieveLookUpData(table)){
				return new IntDouble(1.0,false);
			}
		}
		
		LookUpTable lut=TableSeries.tableSeries.get(table);
		ArrayList<Number[]> data=lut.getData();
		HashMap<String, Integer> field= lut.getField();
		int fieldSize=field.size();
		
		int selectIndex;
		if (field.containsKey(select)){
			selectIndex=field.get(select);
		}else{
			Error.addEvaluationError(select+" in the select statement is not a field name in Table "+table);
			return new IntDouble(1.0,false);
		}
				
		if (given==null){
			Error.addEvaluationError("select data from table needs either where statement or given statement.");
			return new IntDouble(1.0,false);
		}
		
		Number[] values=new Number[fieldSize];

		int givenIndex;
		Set givenSet=given.keySet();
		Iterator iterator=givenSet.iterator();
		String givenName=(String)iterator.next();
		String valueString;
		
		if (field.containsKey(givenName)){
			givenIndex=field.get(givenName);
		}else{
			Error.addEvaluationError(givenName+" in the given statement is not a field name in Table "+table);
			return new IntDouble(1.0,false);
		}
		Number givenValue=(Number)given.get(givenName);
		
		ArrayList<Number> gVList=new ArrayList<Number>();
		Map<Number, Number> gVMap=new HashMap<Number, Number>();
		
		for (int i=0; i<data.size(); i++){
			values=data.get(i);
			if (gVList.contains(values[givenIndex])){
				Error.addEvaluationError("Given value "+values[givenIndex]+" in the given statement is duplicated in Table "+table);
				return new IntDouble(1.0,false);
			}else{
				gVList.add(values[givenIndex]);
				gVMap.put(values[givenIndex], values[selectIndex]);
			}
		}
		
		String givenError="";
		for (String key: given.keySet()){
			givenError=givenError+"("+key+": "+given.get(key)+")";
		}
		return calculateValue(givenValue, gVList, gVMap, use, table, givenError);
	}	
		
	public static IntDouble calculateValue(Number given, ArrayList<Number> gVList, Map<Number, Number> gVMap, String use, String table, String givenError){
		double givenValue=given.doubleValue();
		if (gVList.size()==0){
			Error.addEvaluationError("Under the given conditon of "+givenError+" Data not found in Table "+table);
			return new IntDouble(1.0,false);
		}else if (gVList.size()==1 && use.equals("linear")){
			Number gV=gVList.get(0);
			if (givenValue==gV.doubleValue()){
				return new IntDouble(gVMap.get(gV).doubleValue(),false);
			}else{
				Error.addEvaluationError("Under the given conditon of "+givenError+" only one value for interpolation in Table "+table);
				return new IntDouble(1.0,false);
			}
		}
		
		gVList=sortNumberArray(gVList, givenError, table);
		
		for (int i=0; i<gVList.size()-1; i++){
			int j=i+1;
			Number first=gVList.get(i);
			Number second=gVList.get(j);
			double firstValue=first.doubleValue();
			double secondValue=second.doubleValue();
			if (firstValue<=givenValue && secondValue>=givenValue){
				if (use.equals("minimum")){
					return new IntDouble(gVMap.get(gVList.get(i)).doubleValue(), false);
				}else if (use.equals("maximum")){
					return new IntDouble(gVMap.get(gVList.get(j)).doubleValue(), false);
				}else if (use.equals("linear")){
					double value=(givenValue-firstValue)/(secondValue-firstValue)
					*(gVMap.get(second).doubleValue()-gVMap.get(first).doubleValue())+gVMap.get(first).doubleValue();
					return new IntDouble(value,false);
				}else{
					Error.addEvaluationError("Use statement can only be maximum, minimum, or linear in Table"+table);
					return new IntDouble(1.0,false);
				}
			}
		}

		if (givenValue<gVList.get(0).doubleValue()){ 
			if (use.equals("minimum")){
				Error.addEvaluationError("Under the given conditon of "+givenError+" Data not found in Table "+table);
				return new IntDouble(1.0,false);
			}else if (use.equals("linear")){
				Number first=gVList.get(0);
				Number second=gVList.get(1);
				double firstValue=first.doubleValue();
				double secondValue=second.doubleValue();
				double value=(givenValue-firstValue)/(secondValue-firstValue)
				*(gVMap.get(second).doubleValue()-gVMap.get(first).doubleValue())+gVMap.get(first).doubleValue();
				return new IntDouble(value,false);	
			}else if (use.equals("maximum")){
				return new IntDouble(gVMap.get(gVList.get(0)).doubleValue(),false);
			}else{
				Error.addEvaluationError("Use statement can only be maximum, minimum, or linear in Table"+table);
				return new IntDouble(1.0,false);
			}
		}else if (givenValue>gVList.get(gVList.size()-1).doubleValue()){
			if (use.equals("maximum")){
				Error.addEvaluationError("Under the given conditon of "+givenError+" Data not found in Table "+table);
				return new IntDouble(1.0,false);
			}else if (use.equals("linear")){
				int size=gVList.size();
				Number first=gVList.get(size-2);
				Number second=gVList.get(size-1);
				double firstValue=first.doubleValue();
				double secondValue=second.doubleValue();
				double value=(givenValue-firstValue)/(secondValue-firstValue)
				*(gVMap.get(second).doubleValue()-gVMap.get(first).doubleValue())+gVMap.get(first).doubleValue();
				return new IntDouble(value,false);	
			}else if (use.equals("minimum")){
				return new IntDouble(gVMap.get(gVList.get(gVList.size()-1)).doubleValue(),false);
			}else{
				Error.addEvaluationError("Use statement can only be maximum, minimum, or linear in Table"+table);
				return new IntDouble(1.0,false);
			}
		}
		
		Error.addEvaluationError("Under the given conditon of "+givenError+" only one value for interpolation in Table "+table);
		return new IntDouble(1.0,false);
	}
	
	public static IntDouble generateIntDouble(String valueString){    
		double doubleValue=Double.parseDouble(valueString);
		return new IntDouble(doubleValue, false);
	}
	
	public static ArrayList<Number> sortNumberArray(ArrayList<Number> al, String givenError, String table){
		for (int i=0; i<al.size(); i++){
			for (int j=i+1; j<al.size(); j++){
				Number first=al.get(i);
				Number second=al.get(j);
				if (first.doubleValue()>second.doubleValue()){
					al.set(i, second);
					al.set(j, first);
				}else if (first.doubleValue()==second.doubleValue()){
					Error.addEvaluationError("Under the given conditon of "+givenError+" two data in given column "+first+" , "+second+" has the same value in Table "+table);
				}
			}
		}
		return al;
	}
}
