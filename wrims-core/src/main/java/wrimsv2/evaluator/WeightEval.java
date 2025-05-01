package wrimsv2.evaluator;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.antlr.runtime.RecognitionException;

import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.commondata.wresldata.WeightElement;
import wrimsv2.components.ControlData;
import wrimsv2.components.Error;
import wrimsv2.components.FilePaths;
import wrimsv2.components.IntDouble;
import wrimsv2.ilp.ILP;
import wrimsv2.parallel.ParallelVars;

public class WeightEval {
	private static double minw=1.0;
	private static double maxw=1.0;
	private static ArrayList<String> minwva=new ArrayList<String>();
	private static ArrayList<String> maxwva=new ArrayList<String>();
	private static ArrayList<String> minPa=new ArrayList<String>();
	private static ArrayList<String> maxPa=new ArrayList<String>();
	private static ArrayList<Integer> minCa=new ArrayList<Integer>();
	private static ArrayList<Integer> maxCa=new ArrayList<Integer>();
	private static String fstr = "WeightTable.csv";
	private static String farstr = "WeightTableAR.csv";
	
	private static ArrayList<String> wList=new ArrayList<String>();
	private static HashMap<String, LinkedHashMap<Integer, ArrayList<Double>>> wMap= new HashMap<String, LinkedHashMap<Integer, ArrayList<Double>>>();
	private static ArrayList<String> variedwList=new ArrayList<String>();
	
	private static CopyOnWriteArrayList<String> wListAR=new CopyOnWriteArrayList<String>();
	private static ConcurrentHashMap<String, WeightElement> wMapAR=new ConcurrentHashMap<String, WeightElement>(); 
	
	public static void procWt(StudyDataSet sds){
		ControlData.currStudyDataSet=sds;
		ControlData.currYear=ControlData.startYear;
		ControlData.currMonth=ControlData.startMonth;
		ControlData.currDay=ControlData.startDay;
		ArrayList<String> modelList = sds.getModelList();
		Map<String, ModelDataSet> modelDataSetMap = sds.getModelDataSetMap();
		new PreEvaluator(sds);
		//ControlData.solverType = Param.SOLVER_CBC;
		//CbcSolver.init(false, sds);
		//new InitialXASolver();
		for (int i=0; i<modelList.size(); i++){ 
			ControlData.currCycleIndex=i;
			String model=modelList.get(i);
			ModelDataSet mds=modelDataSetMap.get(model);
			ControlData.currModelDataSet=mds;
			mds.resetSlackSurplusWeight(); // this clears slack and surplus vars
			//mds.processTimeseries();
			//mds.processSvar();
			//mds.processDvar();	
			//mds.processWeight();
			//mds.processWeightSlackSurplus();
			//CbcSolver.newProblem();
			//new XASolver(); 
			//mds.processAlias();
			collectWt(mds, i+1);
			collectWtSlackSurplus(mds, i+1);
		}
		outputWtTable();
		outputExtreme();
	}
	
	public static void collectWt(ModelDataSet mds, int ci){
		ArrayList<String> wtList = mds.wtList;
		Map<String, WeightElement> wtMap =mds.wtMap;
		ControlData.currEvalTypeIndex=7;
		for (String wtName: wtList){
			ControlData.currEvalName=wtName;
			WeightElement wt=wtMap.get(wtName);
			ValueEvaluatorParser evaluator=wt.weightParser;
			ParallelVars prvs = new ParallelVars();
			evaluator.setParallelVars(prvs);
			prvs.timeArrayIndex=0;
			try {
				evaluator.evaluator();
				double wtv = evaluator.evalValue.getData().doubleValue();
				if (!wList.contains(wtName)){
					wList.add(wtName);
				}
				if (wMap.containsKey(wtName)){
					LinkedHashMap<Integer, ArrayList<Double>> cw = wMap.get(wtName);
					if (cw.containsKey(ci)){
						ArrayList<Double> wlc = cw.get(ci);
						wlc.add(wtv);
					}else{
						ArrayList<Double> wlc = new ArrayList<Double> ();
						wlc.add(wtv);
						cw.put(ci, wlc);
					}
				}else{
					LinkedHashMap<Integer, ArrayList<Double>> cw = new LinkedHashMap<Integer, ArrayList<Double>>();
					ArrayList<Double> wlc = new ArrayList<Double> ();
					wlc.add(wtv);
					cw.put(ci, wlc);
					wMap.put(wtName, cw);
				}
				if (wtv !=0.0){
					double awtv = Math.abs(wtv);
					if (awtv<minw){
						minwva=new ArrayList<String>();
						minPa=new ArrayList<String>();
						minCa=new ArrayList<Integer>();
						minw=awtv;
						minwva.add(wtName);
						minPa.add("("+wt.fromWresl+":"+wt.line+")");
						minCa.add(ci);
					}else if (awtv==minw){
						minwva.add(wtName);
						minPa.add("("+wt.fromWresl+":"+wt.line+")");
						minCa.add(ci);
					}else if (awtv>maxw){
						maxwva=new ArrayList<String>();
						maxPa=new ArrayList<String>();
						maxCa=new ArrayList<Integer>();
						maxw=awtv;
						maxwva.add(wtName);
						maxPa.add("("+wt.fromWresl+":"+wt.line+")");
						maxCa.add(ci);
					}else if (awtv==maxw){
						maxwva.add(wtName);
						maxPa.add("("+wt.fromWresl+":"+wt.line+")");
						maxCa.add(ci);
					}
				}
			} catch (RecognitionException e) {
				variedwList.add(wtName);
			}
			evaluator.reset();
			
			int timeArraySize=getTimeArraySize(wt.timeArraySizeParser);
			for (prvs.timeArrayIndex=1; prvs.timeArrayIndex<=timeArraySize; prvs.timeArrayIndex++){
				WeightElement newWt=new WeightElement();
				String newWtName=wtName+"__fut__"+prvs.timeArrayIndex;
				try {
					evaluator.evaluator();
					double wtv = evaluator.evalValue.getData().doubleValue();
					if (!wList.contains(newWtName)){
						wList.add(newWtName);
					}
					if (wMap.containsKey(newWtName)){
						LinkedHashMap<Integer, ArrayList<Double>> cw = wMap.get(newWtName);
						if (cw.containsKey(ci)){
							ArrayList<Double> wlc = cw.get(ci);
							wlc.add(wtv);
						}else{
							ArrayList<Double> wlc = new ArrayList<Double> ();
							wlc.add(wtv);
							cw.put(ci, wlc);
						}
					}else{
						LinkedHashMap<Integer, ArrayList<Double>> cw = new LinkedHashMap<Integer, ArrayList<Double>>();
						ArrayList<Double> wlc = new ArrayList<Double> ();
						wlc.add(wtv);
						cw.put(ci, wlc);
						wMap.put(newWtName, cw);
					}
					if (wtv !=0.0){
						double awtv = Math.abs(wtv);
						if (awtv<minw){
							minwva=new ArrayList<String>();
							minPa=new ArrayList<String>();
							minCa=new ArrayList<Integer>();
							minw=awtv;
							minwva.add(newWtName);
							minPa.add("("+wt.fromWresl+":"+wt.line+")");
							minCa.add(ci);
						}else if (awtv==minw){
							minwva.add(newWtName);
							minPa.add("("+wt.fromWresl+":"+wt.line+")");
							minCa.add(ci);
						}else if (awtv>maxw){
							maxwva=new ArrayList<String>();
							maxPa=new ArrayList<String>();
							maxCa=new ArrayList<Integer>();
							maxw=awtv;
							maxwva.add(newWtName);
							maxPa.add("("+wt.fromWresl+":"+wt.line+")");
							maxCa.add(ci);
						}else if (awtv==maxw){
							maxwva.add(newWtName);
							maxPa.add("("+wt.fromWresl+":"+wt.line+")");
							maxCa.add(ci);
						}
					}
				} catch (RecognitionException e) {
					variedwList.add(wtName);
				}
				evaluator.reset();
			}
		}
	}
	
	public static void collectWtSlackSurplus(ModelDataSet mds, int ci){
		ArrayList<String> wtssList = mds.wtSlackSurplusList;
		Map<String, WeightElement> wtssMap =mds.wtSlackSurplusMap;
		ControlData.currEvalTypeIndex=7;
		for (String wtssName: wtssList){
			ControlData.currEvalName=wtssName;
			WeightElement wt=wtssMap.get(wtssName);
			ValueEvaluatorParser evaluator=wt.weightParser;
			ParallelVars prvs = new ParallelVars();
			evaluator.setParallelVars(prvs);
			prvs.timeArrayIndex=0;
			try {
				evaluator.evaluator();
				double wtv = evaluator.evalValue.getData().doubleValue();
				if (!wList.contains(wtssName)){
					wList.add(wtssName);
				}
				if (wMap.containsKey(wtssName)){
					LinkedHashMap<Integer, ArrayList<Double>> cw = wMap.get(wtssName);
					if (cw.containsKey(ci)){
						ArrayList<Double> wlc = cw.get(ci);
						wlc.add(wtv);
					}else{
						ArrayList<Double> wlc = new ArrayList<Double> ();
						wlc.add(wtv);
						cw.put(ci, wlc);
					}
				}else{
					LinkedHashMap<Integer, ArrayList<Double>> cw = new LinkedHashMap<Integer, ArrayList<Double>>();
					ArrayList<Double> wlc = new ArrayList<Double> ();
					wlc.add(wtv);
					cw.put(ci, wlc);
					wMap.put(wtssName, cw);
				}
				if (wtv !=0.0){
					double awtv = Math.abs(wtv);
					if (awtv<minw){
						minwva=new ArrayList<String>();
						minPa=new ArrayList<String>();
						minCa=new ArrayList<Integer>();
						minw=awtv;
						minwva.add(wtssName);
						minPa.add("("+wt.fromWresl+":"+wt.line+")");
						minCa.add(ci);
					}else if (awtv==minw){
						minwva.add(wtssName);
						minPa.add("("+wt.fromWresl+":"+wt.line+")");
						minCa.add(ci);
					}else if (awtv>maxw){
						maxwva=new ArrayList<String>();
						maxPa=new ArrayList<String>();
						maxCa=new ArrayList<Integer>();
						maxw=awtv;
						maxwva.add(wtssName);
						maxPa.add("("+wt.fromWresl+":"+wt.line+")");
						maxCa.add(ci);
					}else if (awtv==maxw){
						maxwva.add(wtssName);
						maxPa.add("("+wt.fromWresl+":"+wt.line+")");
						maxCa.add(ci);
					}
				}
			} catch (RecognitionException e) {
				variedwList.add(wtssName);
			}
			evaluator.reset();
			
			int timeArraySize=getTimeArraySize(wt.timeArraySizeParser);
			for (prvs.timeArrayIndex=1; prvs.timeArrayIndex<=timeArraySize; prvs.timeArrayIndex++){
				WeightElement newWt=new WeightElement();
				String newWtName=wtssName+"__fut__"+prvs.timeArrayIndex;
				try {
					evaluator.evaluator();
					double wtv = evaluator.evalValue.getData().doubleValue();
					if (!wList.contains(newWtName)){
						wList.add(newWtName);
					}
					if (wMap.containsKey(newWtName)){
						LinkedHashMap<Integer, ArrayList<Double>> cw = wMap.get(newWtName);
						if (cw.containsKey(ci)){
							ArrayList<Double> wlc = cw.get(ci);
							wlc.add(wtv);
						}else{
							ArrayList<Double> wlc = new ArrayList<Double> ();
							wlc.add(wtv);
							cw.put(ci, wlc);
						}
					}else{
						LinkedHashMap<Integer, ArrayList<Double>> cw = new LinkedHashMap<Integer, ArrayList<Double>>();
						ArrayList<Double> wlc = new ArrayList<Double> ();
						wlc.add(wtv);
						cw.put(ci, wlc);
						wMap.put(newWtName, cw);
					}
					if (wtv !=0.0){
						double awtv = Math.abs(wtv);
						if (awtv<minw){
							minwva=new ArrayList<String>();
							minPa=new ArrayList<String>();
							minCa=new ArrayList<Integer>();
							minw=awtv;
							minwva.add(newWtName);
							minPa.add("("+wt.fromWresl+":"+wt.line+")");
							minCa.add(ci);
						}else if (awtv==minw){
							minwva.add(newWtName);
							minPa.add("("+wt.fromWresl+":"+wt.line+")");
							minCa.add(ci);
						}else if (awtv>maxw){
							maxwva=new ArrayList<String>();
							maxPa=new ArrayList<String>();
							maxCa=new ArrayList<Integer>();
							maxw=awtv;
							maxwva.add(newWtName);
							maxPa.add("("+wt.fromWresl+":"+wt.line+")");
							maxCa.add(ci);
						}else if (awtv==maxw){
							maxwva.add(newWtName);
							maxPa.add("("+wt.fromWresl+":"+wt.line+")");
							maxCa.add(ci);
						}
					}
				} catch (RecognitionException e) {
					variedwList.add(newWtName);
				}
				evaluator.reset();
			}
		}
	}
	
	public static int getTimeArraySize(ValueEvaluatorParser timeArraySizeParser){
		int timeArraySize;
		try{
			timeArraySizeParser.evaluator();
			IntDouble timeArrayEvalValue=timeArraySizeParser.evalValue;
			timeArraySizeParser.reset();
			if (!timeArrayEvalValue.isInt()){
				Error.addEvaluationError("the time array size is not an integer.");
			}
			timeArraySize=timeArrayEvalValue.getData().intValue();
		}catch(RecognitionException e) {
			Error.addEvaluationError("weight time array definition has error");
			timeArraySize=0;
		}
		return timeArraySize;
	}
	
	public static void outputWtTable(){
		try {
			File file = new File(FilePaths.mainDirectory, fstr);
			if (!file.exists()){
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsolutePath());
			PrintWriter out = new PrintWriter(fw);
		
			Collections.sort(wList);
		
			out.println("Name"+","+"Cycle"+","+"Weight");
			for (int i=0; i<wList.size(); i++){
				String wtName = wList.get(i);
				LinkedHashMap<Integer, ArrayList<Double>> cwMap = wMap.get(wtName);
				Set<Integer> ks = cwMap.keySet();
				Iterator<Integer> it = ks.iterator();
				while (it.hasNext()){
					int ci=it.next();
					ArrayList<Double> wtList = cwMap.get(ci);
					for (int j=0; j<wtList.size(); j++){
						out.println(wtName+","+ci+","+wtList.get(j));
					}
				}
			}
			out.close();
			fw.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public static void outputExtreme(){
		Collections.sort(variedwList);
		int size = variedwList.size();
		for (int i=0; i<size-1; i++){
			System.out.print(variedwList.get(i)+", ");
		}
		if (size>0)	{
			System.out.print(variedwList.get(size-1)+" ");
			System.out.println("have variable(s) in the weight and were excluded from the evaluation of the maximum and minimum weights.");
		}
		for (int i=0; i<maxwva.size(); i++){
			System.out.println("The maximum weight is "+maxwva.get(i)+" with a value of "+maxw+" and appear in Cycle "+maxCa.get(i)+".");
			System.out.println("The maximum weight source code location is at "+maxPa.get(i)+" .");
		}
		for (int i=0; i<minwva.size(); i++){
			System.out.println("The minimum weight is "+minwva.get(i)+" with a value of "+minw+" and appear in Cycle "+minCa.get(i)+".");
			System.out.println("The minimum weight source code location is at "+minPa.get(i)+" .");
		}
	}
	
	public static void collectWtRT(String name, WeightElement wt){
		if (!wListAR.contains(name)){
			wListAR.add(name);
			wMapAR.put(name, wt);
		}
		double aValue=Math.abs(wt.getValue());
		if (wt.max<0){
			int ci=ControlData.currCycleIndex+1;
			wt.max=aValue;
			wt.maxTC=ControlData.currYear+"-"+ControlData.currMonth+"-"+ControlData.currDay+"-C"+ci;
		}else{
			if (wt.max<aValue){
				int ci=ControlData.currCycleIndex+1;
				wt.max=aValue;
				wt.maxTC=ControlData.currYear+"-"+ControlData.currMonth+"-"+ControlData.currDay+"-C"+ci;
			}
		}
		if (wt.min<0){
			int ci=ControlData.currCycleIndex+1;
			wt.min=aValue;
			wt.minTC=ControlData.currYear+"-"+ControlData.currMonth+"-"+ControlData.currDay+"-C"+ci;
		}else{
			if (wt.min>aValue){
				int ci=ControlData.currCycleIndex+1;
				wt.min=aValue;
				wt.minTC=ControlData.currYear+"-"+ControlData.currMonth+"-"+ControlData.currDay+"-C"+ci;
			}
		}
	}
	
	public static void outputWtTableAR(){
		try {
			File file = new File(ILP.getIlpDir(), farstr);
			if (!file.exists()){
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsolutePath());
			PrintWriter out = new PrintWriter(fw);
		
			Collections.sort(wListAR);
		
			out.println("Name"+","+"Min-Weight"+","+"Timestep-Cycle"+","+"Max-Weight"+","+"Timestep-Cycle");
			for (int i=0; i<wListAR.size(); i++){
				String wtName = wListAR.get(i);
				if (wMapAR.containsKey(wtName)){
					WeightElement wt = wMapAR.get(wtName);
					out.println(wtName+","+wt.min+","+wt.minTC+","+wt.max+","+wt.maxTC);
				}
			}
			out.close();
			fw.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}
