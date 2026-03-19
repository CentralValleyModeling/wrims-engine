package gov.ca.water.wrims.engine.core.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import gov.ca.water.wrims.engine.core.commondata.solverdata.SolverData;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Alias;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Dvar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.WeightElement;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.FilePaths;
import gov.ca.water.wrims.engine.core.components.IntDouble;
import gov.ca.water.wrims.engine.core.evaluator.EvalConstraint;

public class RCCComparison {
	private int cycle=1;
	private String cycleName;
	private BufferedWriter out;
	private BufferedWriter out1;
	private BufferedWriter out2;
	private String gName;
	private ArrayList<Object> gNameArrayList=new ArrayList<Object> ();
	private ArrayList<Object> wNameArrayList=new ArrayList<Object> ();
	private ArrayList<Object> dvarArrayList=new ArrayList<Object> ();
	private Map<String, WeightElement> weightMap;
	private Map<String, Dvar> dvarMap;
	private Map<String, Alias> aliasMap;
	
	public RCCComparison() {
		if (cycle<10){
			cycleName="0"+cycle;
		}else{
			cycleName=""+cycle;
		}
		
		Map<String, EvalConstraint> constraintMap=SolverData.getConstraintDataMap();
		Object[] gNameArray=constraintMap.keySet().toArray();
		Collections.addAll(gNameArrayList, gNameArray); 
		
		weightMap=SolverData.getWeightMap();
		Object[] wNameArray=weightMap.keySet().toArray();
		Collections.addAll(wNameArrayList, wNameArray);
		
		
		
		try{
			String outPath=FilePaths.mainDirectory+"comparegoal.txt";
			FileWriter outstream = new FileWriter(outPath);
			out = new BufferedWriter(outstream);
			
			String outPath1=FilePaths.mainDirectory+"compareobject.txt";
			FileWriter outstream1 = new FileWriter(outPath1);
			out1 = new BufferedWriter(outstream1);

			String filePath=FilePaths.mainDirectory+"rcc_reformatted_"+cycle+".txt";		
			FileInputStream fstream = new FileInputStream(filePath);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			    
			String strLine;
			boolean isEnd=false;
			int line=0;
			while (!isEnd){
			 	strLine=br.readLine();
			 	if (strLine==null || strLine.equals("")) {
			 		isEnd=true;
			 	}else{
			 		if (strLine.startsWith("00") || strLine.startsWith(cycleName)){
			 			if (strLine.startsWith("00"+"Objective") || strLine.startsWith(cycleName+"Objective")){
			 				compareObject(strLine);
			 			}
			 			else{
			 				compareConstraint(strLine);
			 			}
			 		}
			 	}
			}
			
			for (int i=0; i<gNameArrayList.size(); i++){	
				out.write(gNameArrayList.get(i)+" is not in WRIMS1.\n");
			}
			out.close();
			
			for (int i=0; i<wNameArrayList.size(); i++){
				//String wName=(String)wNameArrayList.get(i);
				//Dvar dvar=SolverData.getDvarMap().get(wName);
				//if (dvar.data.getData().doubleValue()!=0.0) out1.write(weightMap.get(wNameArrayList.get(i)).getValue()+"*"+wNameArrayList.get(i)+" is not in WRIMS1.\n");
				out1.write(weightMap.get(wNameArrayList.get(i)).getValue()+"*"+wNameArrayList.get(i)+" is not in WRIMS1.\n");
			}
			out1.close();
		}catch (Exception e){
			e.printStackTrace();
			System.out.println(gName);
		}
		
		dvarMap=SolverData.getDvarMap();
		Object[] dvarArray=dvarMap.keySet().toArray();
		Collections.addAll(dvarArrayList, dvarArray);
		
		aliasMap=ControlData.currAliasMap;
		
		try{
			String outPath=FilePaths.mainDirectory+"comparedvar.txt";
			FileWriter outstream = new FileWriter(outPath);
			out2 = new BufferedWriter(outstream);

			String filePath=FilePaths.mainDirectory+"rcc_dvar_"+cycle+".txt";		
			FileInputStream fstream = new FileInputStream(filePath);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			    
			String strLine;
			boolean isEnd=false;
			int line=0;
			while (!isEnd){
			 	strLine=br.readLine();
			 	if (strLine==null || strLine.equals("")) {
			 		isEnd=true;
			 	}else{
			 		compareDvar(strLine);
			 	}
			}
			
			for (int i=0; i<dvarArrayList.size(); i++){	
				Dvar dvar=dvarMap.get(dvarArrayList.get(i));
				if (dvar.lowerBoundValue.doubleValue()!=0 || dvar.upperBoundValue.doubleValue()!=1.0e38){
					out2.write(dvarArrayList.get(i)+" is not in WRIMS1.\n");
				}
			}
			out2.close();
		}catch (Exception e){
			e.printStackTrace();
			System.out.println(gName);
		}
	}
	
	public void compareDvar(String strLine) throws IOException{
		boolean isDifferent =false;
		String[] subStrs=strLine.toLowerCase().replaceAll(" ", "").split(":");
		String outLine=subStrs[1]+":";
		String[] bounds=subStrs[2].split(";");
		if (dvarMap.containsKey(subStrs[1])){
			dvarArrayList.remove(subStrs[1]);
			Dvar dvar=dvarMap.get(subStrs[1]);
			double lowBound=dvar.lowerBoundValue.doubleValue();
			if (Math.abs(Double.parseDouble(bounds[0])-lowBound)>0.1){
				outLine=outLine+"("+bounds[0]+","+lowBound+")lower:";
				isDifferent=true;
			}
			double upBound=dvar.upperBoundValue.doubleValue();
			if (Math.abs(Double.parseDouble(bounds[1])-upBound)>0.1){
				outLine=outLine+"("+bounds[1]+","+upBound+")upper";
				isDifferent=true;
			}
		}else{
			if (!aliasMap.containsKey(subStrs[1])) out2.write(subStrs[1]+" is not in WRIMS v2.\n");
		}
		if (isDifferent) out2.write(outLine+"\n");
	}
	
	public void compareObject(String strLine) throws IOException{
		String outLine="Object:";
		boolean isDifferent =false;
		strLine=strLine.substring(2, strLine.length()).toLowerCase();
		String[] subStrs=strLine.split(":");
		String[] weightVariable=subStrs[1].replaceAll(" ","").split(";");
		for (int i=1; i<weightVariable.length; i++){
			String[] multiStrs=weightVariable[i].split("\\|");
			if (weightMap.containsKey(multiStrs[0])){
				wNameArrayList.remove(multiStrs[0]);
				double coef=weightMap.get(multiStrs[0]).getValue();
				if (Math.abs(coef-Double.parseDouble(multiStrs[1]))>0.1){
					isDifferent=true;
					outLine=outLine+"("+multiStrs[1]+"|"+coef+")*"+multiStrs[0]+";";
				}
			}else{
				out1.write(multiStrs[1]+"*"+multiStrs[0]+" is not in WRIMS v2.\n");
			}
		}
		if (isDifferent) out1.write(outLine+"\n");
	}
	
	public void compareConstraint(String strLine) throws IOException{
		strLine=strLine.substring(2, strLine.length()).toLowerCase();
		String[] subStrs=strLine.split(":");
		gName=subStrs[0].substring(0, subStrs[0].lastIndexOf("/"));
		Map<String, EvalConstraint> constraintMap=SolverData.getConstraintDataMap();
		if (constraintMap.containsKey(gName)){
			EvalConstraint ec=constraintMap.get(gName);
			gNameArrayList.remove(gName);
			Map<String, IntDouble> multiplier=ec.getEvalExpression().getMultiplier();
			Object[] multiplierArray=multiplier.keySet().toArray();
			ArrayList<Object> multiplierArrayList=new ArrayList<Object> ();
			Collections.addAll(multiplierArrayList, multiplierArray);
			String outLine=gName+":";
			String[] coefVariable=subStrs[1].replaceAll(" ","").split(";");
			boolean isDifferent=false;
			for (int i=0; i<coefVariable.length; i++){
				String[] multiStrs=coefVariable[i].split("\\|");
				if (multiplier.containsKey(multiStrs[0])){
					multiplierArrayList.remove(multiStrs[0]);
					double coef=multiplier.get(multiStrs[0]).getData().doubleValue();
					if (Math.abs(coef-Double.parseDouble(multiStrs[1]))>0.1){
						isDifferent=true;
						outLine=outLine+"("+multiStrs[1]+"|"+coef+")*"+multiStrs[0]+";";
					}
				}else{
					isDifferent=true;
					outLine=outLine+"("+multiStrs[1]+"|no)*"+multiStrs[0]+";";
				}
			}
			for (int i=0; i<multiplierArrayList.size(); i++){
				isDifferent=true;
				outLine=outLine+"(no|"+multiplier.get(multiplierArrayList.get(i)).getData()+")"+multiplierArrayList.get(i)+";";
			}
			String[] signValue=subStrs[subStrs.length-1].split(";");
			if (!signValue[0].replaceAll(" ", "").equals(ec.getSign())){
				isDifferent=true;
				outLine=outLine+"("+signValue[0]+"|"+ec.getSign()+")";
			}
			double value=-ec.getEvalExpression().getValue().getData().doubleValue();
			if (Math.abs(Double.parseDouble(signValue[1])-value) >0.1){
				isDifferent=true;
				outLine=outLine+"("+signValue[1]+"|"+value+")";
			}
			if (isDifferent) out.write(outLine+"\n");
		}else{
			if (!gName.endsWith("_alias")) out.write(gName+" is not in WRIMS v2.\n");
		}
	}
}
