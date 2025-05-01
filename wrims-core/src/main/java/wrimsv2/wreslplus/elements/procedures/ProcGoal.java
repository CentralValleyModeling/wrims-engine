package wrimsv2.wreslplus.elements.procedures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import wrimsv2.commondata.wresldata.Param;
import wrimsv2.wreslplus.elements.DvarTemp;
import wrimsv2.wreslplus.elements.GoalCase;
import wrimsv2.wreslplus.elements.GoalTemp;
import wrimsv2.wreslplus.elements.ModelTemp;
import wrimsv2.wreslplus.elements.SequenceTemp;
import wrimsv2.wreslplus.elements.StudyTemp;
import wrimsv2.wreslplus.elements.WeightTemp;

public class ProcGoal {

	private ProcGoal() {
	}

	public static void processGoalHS2(StudyTemp st) {
		
		Set<String> filesProcessed = new HashSet<String>();
		
		for (String seqName : st.seqList) {
			
			SequenceTemp seqObj = st.seqMap.get(seqName);
			ModelTemp mObj = st.modelMap.get(seqObj.model);
			
			for (String e: st.modelList_effective){
				
				if (st.allOffspringMap_incModel.keySet().contains(e)) {
					for (String f: st.allOffspringMap_incModel.get(e)) {
					
						if (!filesProcessed.contains(f)) {
							
							ModelTemp incModel = st.modelMap.get(f);
							processGoalHS2(incModel, seqObj);
							filesProcessed.add(f);
							
						}
					}
				}
			
			}
			
			for (String f: mObj.incFileRelativePathList_post){
				
				if (!filesProcessed.contains(f)) {
					
					ModelTemp incModel = st.fileModelDataTable.get(f, st.fileModelNameMap.get(f).get(0));
					processGoalHS2(incModel, seqObj);
					filesProcessed.add(f);
					
				}
				
			
			}
			
			
			processGoalHS2(mObj, seqObj);
			
			
		}
		
	}

	public static void processGoalHS2(ModelTemp mt, SequenceTemp seq) {
		
		processGoalHS2(mt);
		
	}
	
	public static void processGoalHS2(ModelTemp mObj) {
	
		for (String gKey : mObj.gl2List) {
	
			GoalTemp g2 = mObj.glMap.get(gKey);
	
			// added to prevent re-assignment in the later sequence 
			// if this goal obj is copied to different seqObj from the same included file.
			// This approach is to reduce data size by not using different goal obj.
			g2.caseCondition = new ArrayList<String>();
			
			for (int caseNumber = 0; caseNumber < g2.caseName.size(); caseNumber++) {
	
				String cn = g2.caseName.get(caseNumber);
				GoalCase gc = g2.caseMap.get(cn);
				g2.caseCondition.add(gc.condition);
	
				// convert penalty into caseExpression
				Map<String, String> o = ProcGoal.convertPenalty(g2, g2.id.toLowerCase(), caseNumber, g2.lhs, gc);
	
				String slackName = o.get("slackName");
				String surplusName = o.get("surplusName");
				// System.out.println(slackName);
				// System.out.println(surplusName);
	
				g2.slackList.add(slackName);
				g2.surplusList.add(surplusName);
				g2.caseExpression.add(o.get("caseExpression"));
	
				Map<String,String> weightMap = new HashMap<String, String>();
				ArrayList<String> dvarSlackSurplus = new ArrayList<String>();
				
				if (slackName != null) {
					WeightTemp w = new WeightTemp();
					w.weight = o.get("slackWeight");
	
					DvarTemp d = new DvarTemp();
					d.fromWresl = g2.fromWresl;
					d.kind = "slack";
					d.timeArraySize=g2.timeArraySize;
	
					if (g2.hasCase) {
						d.condition = Param.conditional;
						w.condition = Param.conditional;
						w.timeArraySize=g2.timeArraySize;
						
						mObj.ssList_hasCase.add(slackName);
						mObj.ssMap_hasCase.put(slackName, d);
						mObj.ssWeightMap_hasCase.put(slackName, w);
						
						weightMap.put(slackName, w.weight);
						dvarSlackSurplus.add(slackName);
						
					} else {
						
						w.timeArraySize=g2.timeArraySize;
						mObj.ssList_noCase.add(slackName);
						mObj.ssMap_noCase.put(slackName, d);
						mObj.ssWeightMap_noCase.put(slackName, w);
					
					}
					// System.out.println(slackName+":"+g2.ruleType+":"+d.condition);
				}
				if (surplusName != null) {
					WeightTemp w = new WeightTemp();
					w.weight = o.get("surplusWeight");
	
					DvarTemp d = new DvarTemp();
					d.fromWresl = g2.fromWresl;
					d.kind = "surplus";
					d.timeArraySize=g2.timeArraySize;
	
					if (g2.hasCase) {
						d.condition = Param.conditional;
						w.condition = Param.conditional;
						w.timeArraySize=g2.timeArraySize;
						mObj.ssList_hasCase.add(surplusName);
						mObj.ssMap_hasCase.put(surplusName, d);
						mObj.ssWeightMap_hasCase.put(surplusName, w);
						
						weightMap.put(surplusName, w.weight);
						dvarSlackSurplus.add(surplusName);
						
					} else {
						
						w.timeArraySize=g2.timeArraySize;
						mObj.ssList_noCase.add(surplusName);
						mObj.ssMap_noCase.put(surplusName, d);
						mObj.ssWeightMap_noCase.put(surplusName, w);
						
					}
					
					// System.out.println(surplusName+":"+g2.ruleType+":"+d.condition);
				}
	
				g2.dvarWeightMapList.add(weightMap);
				g2.dvarSlackSurplusList.add(dvarSlackSurplus);
				
	
			}
	

			mObj.glMap.put(gKey, g2);
	
		}
		
	
	}

	public static Map<String, String> convertPenalty(GoalTemp goal, String goalName, int caseIndex, String lhs, GoalCase cm) {
	
		String caseNumber = Integer.toString(caseIndex + 1);
		String caseExpression = null;
		String lhs_m = null;
		// String rhs_m=null;
		String relation = null;
	
		String lt = cm.lhs_lt_rhs;
		String gt = cm.lhs_gt_rhs;
	
		String slackName = null;
		String surplusName = null;
		String slackWeight = null;
		String surplusWeight = null;
	
		Map<String, String> expression_slack_surplus = new HashMap<String, String>();
	
		if (ProcGoal.isConstrain(gt) && ProcGoal.isConstrain(lt)) {
	
			lhs_m = lhs;
			relation = "=";
			caseExpression = lhs_m + relation + cm.rhs;
	
		}
		else if (ProcGoal.isConstrain(gt)) {
	
			if (ProcGoal.isFree(lt)) {
	
				lhs_m = lhs;
				relation = "<";
				caseExpression = lhs_m + relation + cm.rhs;
	
			}
			else {
	
				slackName = "slack__" + goalName + "_" + caseNumber;
				slackWeight = lt;
				if (goal.timeArraySize.equals("0")){
					lhs_m = lhs + "+" + slackName;
				}else{
					lhs_m = lhs + "+" + slackName + "($m)";
				}
				relation = "=";
				caseExpression = lhs_m + relation + cm.rhs;
			}
	
		}
		else if (ProcGoal.isConstrain(lt)) {
	
			if (ProcGoal.isFree(gt)) {
	
				lhs_m = lhs;
				relation = ">";
				caseExpression = lhs_m + relation + cm.rhs;
	
			}
			else {
	
				surplusName = "surplus__" + goalName + "_" + caseNumber;
				surplusWeight = gt;
				if (goal.timeArraySize.equals("0")){
					lhs_m = lhs + "-" + surplusName;
				}else{
					lhs_m = lhs + "-" + surplusName + "($m)";
				}
				relation = "=";
				caseExpression = lhs_m + relation + cm.rhs;
			}
	
		}
		else if (ProcGoal.isFree(gt) && ProcGoal.isFree(lt)) {
	
			caseExpression = " 1 > 0 ";
		
			// default to general treatment, that is, 
			// lhs + slack - surplus = rhs with zero weight on lhs and rhs 
			
//			surplusName = "surplus__" + goalName + "_" + caseNumber;
//			surplusWeight = gt;
//			if (goal.timeArraySize.equals("0")){
//				lhs_m = lhs + "-" + surplusName;
//			}else{
//				lhs_m = lhs + "-" + surplusName+"($m)";
//			}
//			
//			slackName = "slack__" + goalName + "_" + caseNumber;
//			slackWeight = lt;
//			if (goal.timeArraySize.equals("0")){
//				lhs_m = lhs_m + "+" + slackName;
//			}else{
//				lhs_m = lhs_m + "+" + slackName+"($m)";
//			}
//			relation = "=";
//			caseExpression = lhs_m + " = " + cm.rhs;
	
		}
		else if (ProcGoal.isFree(lt)) {
	
			surplusName = "surplus__" + goalName + "_" + caseNumber;
			surplusWeight = gt;
			if (goal.timeArraySize.equals("0")){
				lhs_m = lhs + "-" + surplusName;
			}else{
				lhs_m = lhs + "-" + surplusName + ("($m)");
			}
			relation = "<";
			caseExpression = lhs_m + relation + cm.rhs;
	
		}
		else if (ProcGoal.isFree(gt)) {
	
			slackName = "slack__" + goalName + "_" + caseNumber;
			slackWeight = lt;
			if (goal.timeArraySize.equals("0")){
				lhs_m = lhs + "+" + slackName;
			}else{
				lhs_m = lhs + "+" + slackName + "($m)";
			}
			relation = ">";
			caseExpression = lhs_m + relation + cm.rhs;
	
		}
		else {
			surplusName = "surplus__" + goalName + "_" + caseNumber;
			surplusWeight = gt;
			if (goal.timeArraySize.equals("0")){
				lhs_m = lhs + "-" + surplusName;
			}else{
				lhs_m = lhs + "-" + surplusName+"($m)";
			}
			slackName = "slack__" + goalName + "_" + caseNumber;
			slackWeight = lt;
			if (goal.timeArraySize.equals("0")){
				lhs_m = lhs_m + "+" + slackName;
			}else{
				lhs_m = lhs_m + "+" + slackName + "($m)";
			}
			relation = "=";
			caseExpression = lhs_m + relation + cm.rhs;
		}
	

		slackWeight = "-(" + slackWeight + ")";

	
		surplusWeight = "-(" + surplusWeight + ")";
		
	
		expression_slack_surplus.put("caseExpression", caseExpression);
		expression_slack_surplus.put("slackName", slackName);
		expression_slack_surplus.put("surplusName", surplusName);
		expression_slack_surplus.put("slackWeight", slackWeight);
		expression_slack_surplus.put("surplusWeight", surplusWeight);
		return expression_slack_surplus;
	}

	public static boolean isConstrain(String v) {
	
		return v.equals(Param.constrain);
	
	}

	public static boolean isFree(String v) {
	
		try {
	
			float p = Float.parseFloat(v);
			if (p == 0) return true;
	
		}
		catch (Exception e) {
		}
	
		return false;
	
	}

	public static boolean isNumber(String v) {
	
		try {
	
			Float p = Float.parseFloat(v);
			return true;
	
		}
		catch (Exception e) {
		}
	
		return false;
	
	}

	
}
