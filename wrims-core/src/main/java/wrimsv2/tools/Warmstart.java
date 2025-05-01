package wrimsv2.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.Param;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.components.ControlData;
import wrimsv2.wreslplus.elements.Procedures;
import wrimsv2.wreslplus.elements.SequenceTemp;
import wrimsv2.wreslplus.elements.StudyTemp;
import wrimsv2.wreslplus.elements.Tools;

public class Warmstart {

	public static void collectIntegerDV_1(StudyTemp st) {
			// TODO: put result in control data.
			ControlData.currStudyDataSet.cycIntDvMap = new HashMap<Integer,LinkedHashSet<String>>();		
			ControlData.currStudyDataSet.allIntDv = new LinkedHashSet<String>();
			
			
			//System.out.println("st.seqList: "+st.seqList);
			for (String k: st.seqList){
				SequenceTemp seq = st.seqMap.get(k);
				int cyc = st.modelList_effective.indexOf(seq.model);
				if (cyc>=0){
					LinkedHashSet<String> intDVSet = new LinkedHashSet<String>();	
					for (String dvName: seq.dvList){
						if (seq.dvMap.get(dvName).isInteger) intDVSet.add(dvName);
					}
					ControlData.currStudyDataSet.cycIntDvMap.put(cyc, intDVSet);
					ControlData.currStudyDataSet.allIntDv.addAll(intDVSet);
				}
				
				//System.out.println(st.seqMap.get(k).dvList);
			}
			//System.out.println("st.modelList_effective: "+st.modelList_effective);
			
			
			
			for (int c: ControlData.currStudyDataSet.cycIntDvMap.keySet()){
				System.out.println("cycle: "+c+"\n"+ControlData.currStudyDataSet.cycIntDvMap.get(c));
				System.out.println();
				System.out.println();
				//System.out.println(ControlData.cycIntDvMap.get(c));
			}
			
			// find first cyc with int
			
	//		Map<Integer,LinkedHashSet<String>> test = new HashMap<Integer, LinkedHashSet<String>>();
	//		
	//		LinkedHashSet<String> w0 = new LinkedHashSet(); 
	//		LinkedHashSet<String> w1 = new LinkedHashSet(Arrays.asList("a", "b", "c", "d")); 
	//		LinkedHashSet<String> w2 = new LinkedHashSet(Arrays.asList("a", "b", "c", "d")); 
	//		LinkedHashSet<String> w3 = new LinkedHashSet(Arrays.asList("a", "b", "c", "d")); 
	//		LinkedHashSet<String> w4 = new LinkedHashSet(Arrays.asList("a")); 
	//		LinkedHashSet<String> w5 = new LinkedHashSet(Arrays.asList("a", "b")); 
	//		LinkedHashSet<String> w6 = new LinkedHashSet(Arrays.asList("a", "b", "c", "d", "e")); 
	//		LinkedHashSet<String> w7 = new LinkedHashSet(Arrays.asList("a", "b", "c", "d", "e")); 
	//		LinkedHashSet<String> w8 = new LinkedHashSet(Arrays.asList("a", "b", "c", "d", "e")); 
	//		LinkedHashSet<String> w9 = new LinkedHashSet(Arrays.asList("a")); 		
	//		LinkedHashSet<String> w10 = new LinkedHashSet(Arrays.asList("a", "b", "c", "d", "e")); 
	//		LinkedHashSet<String> w11 = new LinkedHashSet(Arrays.asList("a", "b", "c", "d", "e", "f")); 
	//		LinkedHashSet<String> w12 = new LinkedHashSet(Arrays.asList("a")); 		
	//		test.put(0, w0);
	//		test.put(1, w1);
	//		test.put(2, w2);
	//		test.put(3, w3);
	//		test.put(4, w4);
	//		test.put(5, w5);
	//		test.put(6, w6);
	//		test.put(7, w7);
	//		test.put(8, w8);
	//		test.put(9, w9);
	//		test.put(10, w10);
	//		test.put(11, w11);
	//		test.put(12, w12);		
	//		ControlData.cycIntDvMap = test;
			
			int nCyc = ControlData.currStudyDataSet.cycIntDvMap.size();
			
			int firstCycWarmStart=9999;
			ArrayList<Integer> cycWarmStart = new ArrayList<Integer>();
			ArrayList<Integer> cycWarmStop = new ArrayList<Integer>();
			ArrayList<Integer> cycWarmUse = new ArrayList<Integer>();
			
			
			for (int i=0; i<nCyc-1; i++){
				if (ControlData.currStudyDataSet.cycIntDvMap.get(i).size()>Param.cbcMinIntNumber) {
					firstCycWarmStart = i;
					cycWarmStart.add(i);
					break;
				}
			}
			
			if (firstCycWarmStart < nCyc-1){
				
				int stop = firstCycWarmStart;
				int start = firstCycWarmStart; 
				
				while(stop < nCyc && start < nCyc-1){
				
					stop  = Procedures.findWarmStop(start, nCyc); 
					if ( stop > start){
						cycWarmStop.add(stop);
						
						for (int i=start+1; i<= stop; i++) cycWarmUse.add(i);
						
					} else {
						cycWarmStart.remove(cycWarmStart.size()-1);
					}
					//System.out.println("stop: "+stop);
					
					start = Procedures.findWarmStart(stop, nCyc); if (start<nCyc-1) cycWarmStart.add(start);
					//System.out.println("start: "+start);
				}
				
				
				if (cycWarmStart.size()!=cycWarmStop.size()) System.out.println("*** Error in warm setting.");
				
				ControlData.cycWarmStart = cycWarmStart;
				ControlData.cycWarmStop = cycWarmStop;
				ControlData.cycWarmUse = cycWarmUse;			
				
				
			} else {
				
				ControlData.useCbcWarmStart=false;
				
			}
			
			System.out.println("cycWarmStart: "+ControlData.cycWarmStart);
	//		System.out.println("cycWarmStop: "+ControlData.cycWarmStop);
			System.out.println("cycWarmUse: " + ControlData.cycWarmUse);
			
			String w = ControlData.cycWarmStart.toString() + "\n" + ControlData.cycWarmUse.toString() + "\n";
			for (int c : ControlData.currStudyDataSet.cycIntDvMap.keySet()) {
				ArrayList<String> t = new ArrayList<String>(ControlData.currStudyDataSet.cycIntDvMap.get(c));
				Collections.sort(t);
				w = w + "cycle: " + c + "\n" + t.toString() + "\n";
			}
			Tools.quickLog("warmstart2.txt", w);
		}

	public static void collectIntegerDV_2(StudyDataSet sd) {
			// TODO: put result in control data.
			sd.cycIntDvMap = new HashMap<Integer,LinkedHashSet<String>>();		
			sd.allIntDv = new LinkedHashSet<String>();
			
			
			//System.out.println("st.seqList: "+st.seqList);
			for (String k: sd.getModelList()){		
				int cyc = sd.getModelList().indexOf(k);
				if (cyc>=0){
					LinkedHashSet<String> intDVSet = new LinkedHashSet<String>();	
					
					ArrayList<String> dvList = sd.getModelDataSetMap().get(k).dvList;
					Map<String, Dvar> dvMap = sd.getModelDataSetMap().get(k).dvMap;
					
					for (String dvName: dvList){
						if (dvMap.get(dvName).integer==Param.yes) intDVSet.add(dvName);
					}
					sd.cycIntDvMap.put(cyc, intDVSet);
					sd.allIntDv.addAll(intDVSet);
				}
				
				//System.out.println(st.seqMap.get(k).dvList);
			}
			//System.out.println("st.modelList_effective: "+st.modelList_effective);
			
			
			
//			for (int c: ControlData.cycIntDvMap.keySet()){
//				System.out.println("cycle: "+c+"\n"+ControlData.cycIntDvMap.get(c));
//				System.out.println();
//				System.out.println();
//			}
			

			
			int nCyc = sd.cycIntDvMap.size();
			
			int firstCycWarmStart=9999;
			ArrayList<Integer> cycWarmStart = new ArrayList<Integer>();
			ArrayList<Integer> cycWarmStop = new ArrayList<Integer>();
			ArrayList<Integer> cycWarmUse = new ArrayList<Integer>();
			
			
			for (int i=0; i<nCyc-1; i++){
				if (sd.cycIntDvMap.get(i).size()>Param.cbcMinIntNumber) {
					firstCycWarmStart = i;
					cycWarmStart.add(i);
					break;
				}
			}
			
			if (firstCycWarmStart < nCyc-1){
				
				int stop = firstCycWarmStart;
				int start = firstCycWarmStart; 
				
				while(stop < nCyc && start < nCyc-1){
				
					stop  = Procedures.findWarmStop(start, nCyc, sd); 
					if ( stop > start){
						cycWarmStop.add(stop);
						
						for (int i=start+1; i<= stop; i++) cycWarmUse.add(i);
						
					} else {
						cycWarmStart.remove(cycWarmStart.size()-1);
					}
					//System.out.println("stop: "+stop);
					
					start = Procedures.findWarmStart(stop, nCyc, sd); if (start<nCyc-1) cycWarmStart.add(start);
					//System.out.println("start: "+start);
				}
				
				
				if (cycWarmStart.size()!=cycWarmStop.size()) System.out.println("*** Error in warm setting.");
				
				ControlData.cycWarmStart = cycWarmStart;
				ControlData.cycWarmStop = cycWarmStop;
				ControlData.cycWarmUse = cycWarmUse;			
				
				
			} else {
				
				ControlData.useCbcWarmStart=false;
				
			}
			
			System.out.println("cycWarmStart: "+ControlData.cycWarmStart);
	//		System.out.println("cycWarmStop: "+ControlData.cycWarmStop);
			System.out.println("cycWarmUse: " + ControlData.cycWarmUse);
			
			if (ControlData.cycWarmStart != null && ControlData.cycWarmUse != null) {

				String w = ControlData.cycWarmStart.toString() + "\n" + ControlData.cycWarmUse.toString() + "\n";
				for (int c : sd.cycIntDvMap.keySet()) {
					ArrayList<String> t = new ArrayList<String>(sd.cycIntDvMap.get(c));
					Collections.sort(t);
					w = w + "cycle: " + c + "\n" + t.toString() + "\n";
				}
//			Tools.quickLog("warmstart_new.txt", w);
			}
			
	}

	public static void collectIntegerDV_3(StudyDataSet sd) {
		
		int nCyc = sd.cycIntDvMap.size();
		
		int firstCycWarmStart=9999;
		ArrayList<Integer> cycWarmStart = new ArrayList<Integer>();
		ArrayList<Integer> cycWarmStop = new ArrayList<Integer>();
		ArrayList<Integer> cycWarmUse = new ArrayList<Integer>();
		
		
		for (int i=0; i<nCyc-1; i++){
			if (sd.cycIntDvMap.get(i).size()>Param.cbcMinIntNumber) {
				firstCycWarmStart = i;
				cycWarmStart.add(i);
				break;
			}
		}
		
		if (firstCycWarmStart < nCyc-1){
			
			int stop = firstCycWarmStart;
			int start = firstCycWarmStart; 
			
			while(stop < nCyc && start < nCyc-1){
			
				stop  = Procedures.findWarmStop(start, nCyc, sd); 
				if ( stop > start){
					cycWarmStop.add(stop);
					
					for (int i=start+1; i<= stop; i++) cycWarmUse.add(i);
					
				} else {
					cycWarmStart.remove(cycWarmStart.size()-1);
				}
				//System.out.println("stop: "+stop);
				
				start = Procedures.findWarmStart(stop, nCyc, sd); if (start<nCyc-1) cycWarmStart.add(start);
				//System.out.println("start: "+start);
			}
			
			
			if (cycWarmStart.size()!=cycWarmStop.size()) System.out.println("*** Error in warm setting.");
			
			ControlData.cycWarmStart = cycWarmStart;
			ControlData.cycWarmStop = cycWarmStop;
			ControlData.cycWarmUse = cycWarmUse;			
			
			
		} else {
			
			ControlData.useCbcWarmStart=false;
			
		}
		
		System.out.println("cycWarmStart: "+ControlData.cycWarmStart);
//		System.out.println("cycWarmStop: "+ControlData.cycWarmStop);
		System.out.println("cycWarmUse: " + ControlData.cycWarmUse);
		
		if (ControlData.cycWarmStart != null && ControlData.cycWarmUse != null) {

			String w = ControlData.cycWarmStart.toString() + "\n" + ControlData.cycWarmUse.toString() + "\n";
			for (int c : sd.cycIntDvMap.keySet()) {
				ArrayList<String> t = new ArrayList<String>(sd.cycIntDvMap.get(c));
				Collections.sort(t);
				w = w + "cycle: " + c + "\n" + t.toString() + "\n";
			}
//		Tools.quickLog("warmstart_new.txt", w);
		}
		
	}
	
	
	
}
