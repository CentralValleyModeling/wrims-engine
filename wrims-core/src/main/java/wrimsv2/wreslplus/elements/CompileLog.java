package wrimsv2.wreslplus.elements;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

import wrimsv2.commondata.wresldata.Param;

public class CompileLog {
		
	private CompileLog(){}

	public static String createMainWreslLog( StudyTemp st, LinkedHashMap<String,Integer> fileMap_reverse) {
		
		String r = "";
		r=r+doSequence(st);
		r=r+doIncModel(st);
		
		r=r+doModels(st, fileMap_reverse);

		return r;
	}
	public static String writeLog( String r, String canonicalFilePath) {
		
		String f = FilenameUtils.removeExtension(canonicalFilePath) + ".de";
		PrintWriter p = null;
		try {
			p = new PrintWriter(new BufferedWriter(new FileWriter(f)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		p.print(r);	
		p.flush();
		p.close();
		return r;
	}
	

	public static String createWreslFileLog( ModelTemp mt, Set<String> deps) {
		
		String r = "";

		r=r+doModel(mt, deps);	
		
		return r;
	}
	
	private static String doModel( ModelTemp mt, Set<String> deps) {
		
		// use symbol instead of full file path
		
		
		
		String r="";
		
			r=r+doTimeseries(mt);
			r=r+doDv(mt);
			
			
			r=r+doAlias(mt, deps);
			r=r+"------------------\n";
			
			
			r=r+doSvIncFileModel(mt);
			r=r+doGoal(mt, deps);
			
			Set<String> varsInModel = new HashSet<String>();
			
			varsInModel.addAll(Tools.allToLowerCase(mt.svList));
			varsInModel.addAll(Tools.allToLowerCase(mt.tsList));
			varsInModel.addAll(Tools.allToLowerCase(mt.asList));
			varsInModel.addAll(Tools.allToLowerCase(mt.dvList));
			
			deps.removeAll(varsInModel);
			
		return r;
		
	}
	private static String doModels( StudyTemp st, LinkedHashMap<String,Integer> fileMap_reverse) {
		
		// use symbol instead of full file path
		
		
		
		String r="";
		r=r+"model{\n";
		
		//LinkedHashMap<String,Integer> fileMap_reverse = new LinkedHashMap<String, Integer>();
		
		for (String m: st.modelList){
			ModelTemp mt = st.modelMap.get(m);
			r=r+"\t"+m.toLowerCase()+"{\n";
			
			r=r+doTimeseries(mt);
			r=r+doDv(mt);
			
			r=r+doAlias(mt,null);
			r=r+"------------------\n";
			
			
			r=r+doSvIncFileModel(mt,fileMap_reverse);
			r=r+doGoal(mt,null);
			
			r=r+"\t}\n"; //+m.toLowerCase()+"\n";
		}

		r=r+"}\n";	
		return r;
		
	}
	private static String doSvIncFileModel(ModelTemp mt) {		
		
	    String r="";
		for (int i=0; i<mt.itemList.size();i++){
			
			String v = mt.itemList.get(i);
			
			if (mt.itemTypeList.get(i)==Param.incFileType) {
				
				String f=mt.incFileMap.get(v).rawPath.toLowerCase();
					r = r + "\t\t@f:" + f +"\n";

			} else if (mt.itemTypeList.get(i)==Param.svType) {
				
				r = r + "@s:" + v.toLowerCase() + "\n" ;
				r = r + doSv(mt.svMap.get(v));
			}
			
		}
		return r;


	}	
	private static String doSvIncFileModel(ModelTemp mt, LinkedHashMap<String,Integer> fileMap_reverse) {		
		
	    String r="";
		for (int i=0; i<mt.itemList.size();i++){
			if (mt.itemTypeList.get(i)==Param.incFileType) {
				String f=mt.incFileMap.get(mt.itemList.get(i)).rawPath.toLowerCase();
				if (fileMap_reverse.keySet().contains(f)) {
					r=r+"\t\t@f"+fileMap_reverse.get(f) +"^\n";
				} else {
					Integer fi = fileMap_reverse.size();
					fileMap_reverse.put(f, fi);
					r=r+"\t\t@f"+fi+":" + f +"\n";
				}

				
			} else if (mt.itemTypeList.get(i)==Param.incModelType) {
				String m = mt.itemList.get(i).toLowerCase();
				r=r+"\t\t@m:" + m.substring(9,m.length())+"\n";
			} else if (mt.itemTypeList.get(i)==Param.svType) {
				r=r+"\t\t@s:" + mt.itemList.get(i).toLowerCase()+"\n";
			}
			
		}
		return r;


	}
	
	private static String doTimeseries(ModelTemp mt) {
		String r="";
		for (String d : mt.tsList) {
			TimeseriesTemp dt = mt.tsMap.get(d);
			r=r+"\t\t@t:" + d.toLowerCase();
			if (!dt.dssBPart.equalsIgnoreCase(d)) r=r+"\tb:" + dt.dssBPart.toLowerCase();
			if (dt.kind!=Param.undefined) r=r+"\tk:" + dt.kind.toLowerCase();
			if (dt.units!=Param.undefined) r=r+"\tu:" + dt.units.toLowerCase();
			r=r+"\n";
		}
		return r;
	}
	
	private static String doAlias(ModelTemp mt, Set<String> deps) {
		String r="";
		for (String d : mt.asList) {
			AliasTemp dt = mt.asMap.get(d);
			r=r+"\t\t@a:" + d;
			r=r+"\te:" + dt.expression.toLowerCase();
			if (dt.kind!=Param.undefined) r=r+"\tk:" + dt.kind.toLowerCase();
			if (dt.units!=Param.undefined) r=r+"\tu:" + dt.units.toLowerCase();
			r=r+"\n";
			
			// collect dependents
			if (deps != null) {
				Set<String> dependents = Tools.allToLowerCase(new HashSet<String>(dt.dependants));
				dependents.removeAll(Param.reservedSet);
				deps.addAll(dependents);
			}
		}
		return r;
	}

	private static String doGoal(ModelTemp mt, Set<String> deps) {
		String r="";
		for (String d : mt.glList) {
			GoalTemp dt = mt.glMap.get(d);
			
			if (!dt.hasLhs){
				// simple
				r=r+"@gs:" + d;
				r=r+"\te:" + dt.caseExpression.get(0).toLowerCase();
				r=r+"\n";
			
			} else if (!dt.hasCase){
				// no case
				r=r+"@gnc:" + d;
				r=r+"\tl:" + dt.lhs.toLowerCase();	
				GoalCase gc = dt.caseMap.get(dt.caseName.get(0));
				r=r+"\tr:" + gc.rhs.toLowerCase();
				if (!gc.lhs_gt_rhs.equalsIgnoreCase(Param.constrain)) r=r+"\tlhs>rhs:" + gc.lhs_gt_rhs.toLowerCase();
				if (!gc.lhs_lt_rhs.equalsIgnoreCase(Param.constrain)) r=r+"\tlhs<rhs:" + gc.lhs_gt_rhs.toLowerCase();
				r=r+"\n";
				
			} else {
				// has case
				r=r+"@gc:" + d;
				r=r+"\tl:" + dt.lhs.toLowerCase();
				r=r+"\n";
				for (int i=0;i<dt.caseName.size();i++) {
					GoalCase gc = dt.caseMap.get(dt.caseName.get(i));
					if (!gc.condition.equalsIgnoreCase(Param.always)) r=r+"\tc:" + gc.condition.toLowerCase();
					r=r+"\tr:" + gc. rhs.toLowerCase();
					if (!gc.lhs_gt_rhs.equalsIgnoreCase(Param.constrain)) r=r+"\tlhs>rhs:" + gc.lhs_gt_rhs.toLowerCase();
					if (!gc.lhs_lt_rhs.equalsIgnoreCase(Param.constrain)) r=r+"\tlhs<rhs:" + gc.lhs_gt_rhs.toLowerCase();
					r=r+"\n";
				}
			}
			
			// collect dependents
			if (deps != null) {
				Set<String> dependents = Tools.allToLowerCase(new HashSet<String>(dt.dependants));
				dependents.removeAll(Param.reservedSet);
				deps.addAll(dependents);
			}

		}
		return r;
	}
	
	private static String doSv(SvarTemp svT) {
		String r = "";
		for (int i=0; i<svT.caseName.size();i++) {
			r = r + "\tn:" + svT.caseName.get(i).toLowerCase();
			
			if (!svT.caseCondition.get(i).toLowerCase().equals(Param.always)) { 
				r = r + "\tc:" + svT.caseCondition.get(i).toLowerCase();
			}
			r = r + "\te:" + svT.caseExpression.get(i).toLowerCase();
			r=r+"\n";
		}		
		
		return r;
	}
	
	private static String doDv(ModelTemp mt) {
		String r="";
		for (String d : mt.dvList) {
			DvarTemp dt = mt.dvMap.get(d);
			r=r+"\t\t@d:" + d;
			r=r+"\tl:" + dt.lowerBound;
			r=r+"\tu:" + dt.upperBound;
			r=r+"\n";
		}
		return r;

	}
	
	
	
	private static String doIncModel( StudyTemp st) {
		
		String r="";
		// find included groups
		Set<String> includedModels = new LinkedHashSet<String>();

		r=r+"includeModel{\n";
		for (String s: st.modelList){
			includedModels.addAll(st.modelMap.get(s).incModelList);
		}
		
		for (String s: includedModels){
			r=r+"\t"+s.toLowerCase()+"\n";
		}

	
		r=r+"}\n";
		
		return r;
		
	}
	
	private static String doSequence( StudyTemp st) {
		String r="";
		r=r+"sequence{\n";
		for (String s: st.seqList){

			r=r+"\t"+st.seqMap.get(s).model.toLowerCase();
			r=r+"\ts:"+s.toLowerCase();
			if (!st.seqMap.get(s).condition.toLowerCase().equals(Param.always)) r=r+"\tc:"+st.seqMap.get(s).condition.toLowerCase();
			r=r+"\to:"+st.seqMap.get(s).order;
			if (st.seqMap.get(s).timeStep!=Param.undefined) r=r+"\tt:"+st.seqMap.get(s).timeStep.toLowerCase();
			r=r+"\n";
		}
		r=r+"}\n";	
		return r;
		
	}
}
