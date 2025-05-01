package wrimsv2.evaluator;

import java.util.ArrayList;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;

import wrimsv2.commondata.wresldata.Alias;
import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.Goal;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.commondata.wresldata.Svar;
import wrimsv2.commondata.wresldata.WeightElement;
import wrimsv2.components.ControlData;
import wrimsv2.components.Error;
import wrimsv2.components.IntDouble;
import wrimsv2.components.VariableTimeStep;

public class PreEvaluator {
	private ArrayList<String> svList;
	private Map<String, Svar> svMap;
	private ArrayList<String> gList;
	private Map<String, Goal> gMap;
	private ArrayList<String> dvList;
	private Map<String, Dvar> dvMap;
	private ArrayList<String> asList;
	private Map<String, Alias> asMap;
	private ArrayList<String> wtList;
	private Map<String, WeightElement> wtMap;
	private ArrayList<String> wtSlackSurplusList;
	private Map<String, WeightElement> wtSlackSurplusMap;	
	private ArrayList<String> modelConditionList;

	public PreEvaluator(StudyDataSet sds ){
		ArrayList<String> modelList=sds.getModelList();
		Map<String, ModelDataSet> modelDataSetMap=sds.getModelDataSetMap();
		for (int i=0; i<modelList.size(); i++){
			String model=modelList.get(i);
			ModelDataSet mds=modelDataSetMap.get(model);
			svList=mds.svList;
			svMap=mds.svMap;
			gList=mds.gList;
			gMap=mds.gMap;
			dvList=mds.dvList;
			dvMap=mds.dvMap;
			asList=mds.asList;
			asMap=mds.asMap;
			wtList=mds.wtList;
			wtMap=mds.wtMap;
			wtSlackSurplusList=mds.wtSlackSurplusList;
			wtSlackSurplusMap=mds.wtSlackSurplusMap;
			
			preEvaluateSvar();
			preEvaluateGoal();
			preEvaluateDvar();
			preEvaluateAlias();
			preEvaluateWeight();
			preEvaluateWeightSlackSurplus();
		}
		modelConditionList=sds.getModelConditionList();
		preEvaluateModelCondition(sds);
	}
	
	public void preEvaluateModelCondition(StudyDataSet sds){
		 ArrayList<ValueEvaluatorParser> modelConditionParsers=new ArrayList<ValueEvaluatorParser>(); 
		for (String modelCondition: modelConditionList){
			//System.out.println("PreEvaluate model condition"+modelCondition);
			String evalString="c: "+modelCondition;
			ANTLRStringStream stream = new ANTLRStringStream(evalString);
			ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
			TokenStream tokenStream = new CommonTokenStream(lexer);
			modelConditionParsers.add(new ValueEvaluatorParser(tokenStream));
		}
		sds.setModelConditionParsers(modelConditionParsers);
	}
	
	public void preEvaluateWeight(){
		for (String wtName: wtList){
			//System.out.println("PreEvaluate weight "+wtName);
			WeightElement weight=wtMap.get(wtName);
			
			String evalString="v: "+weight.weight;
			ANTLRStringStream stream = new ANTLRStringStream(evalString);
			ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
			TokenStream tokenStream = new CommonTokenStream(lexer);
			weight.weightParser = new ValueEvaluatorParser(tokenStream);
			
			evalString="v: "+weight.timeArraySize;
			stream = new ANTLRStringStream(evalString);
			lexer = new ValueEvaluatorLexer(stream);
			tokenStream = new CommonTokenStream(lexer);
			weight.timeArraySizeParser = new ValueEvaluatorParser(tokenStream);
		}
	}
	
	public void preEvaluateWeightSlackSurplus(){
		for (String wtSlackSurplusName: wtSlackSurplusList){
			//System.out.println("PreEvaluate weight slack surplus"+wtName);
			WeightElement weightSlackSurplus=wtSlackSurplusMap.get(wtSlackSurplusName);
			
			String evalString="v: "+weightSlackSurplus.weight;
			ANTLRStringStream stream = new ANTLRStringStream(evalString);
			ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
			TokenStream tokenStream = new CommonTokenStream(lexer);
			weightSlackSurplus.weightParser = new ValueEvaluatorParser(tokenStream);
			
			evalString="v: "+weightSlackSurplus.timeArraySize;
			stream = new ANTLRStringStream(evalString);
			lexer = new ValueEvaluatorLexer(stream);
			tokenStream = new CommonTokenStream(lexer);
			weightSlackSurplus.timeArraySizeParser = new ValueEvaluatorParser(tokenStream);
		}
	}
	
	public void preEvaluateAlias(){
		for (String asName: asList){
			//System.out.println("PreEvaluate alias "+asName);
			Alias alias=asMap.get(asName);
			
			String evalString="v: "+alias.expression;
			ANTLRStringStream stream = new ANTLRStringStream(evalString);
			ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
			TokenStream tokenStream = new CommonTokenStream(lexer);
			alias.expressionParser = new ValueEvaluatorParser(tokenStream);
			
			evalString="v: "+alias.timeArraySize;
			stream = new ANTLRStringStream(evalString);
			lexer = new ValueEvaluatorLexer(stream);
			tokenStream = new CommonTokenStream(lexer);
			alias.timeArraySizeParser = new ValueEvaluatorParser(tokenStream);
		}
	}
	
	public void preEvaluateDvar(){
		for (String dvName: dvList){
			//System.out.println("PreEvaluate dvar "+dvName);
			Dvar dvar=dvMap.get(dvName);
			
			String evalString="v: "+dvar.upperBound;
			ANTLRStringStream stream = new ANTLRStringStream(evalString);
			ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
			TokenStream tokenStream = new CommonTokenStream(lexer);
			dvar.upperBoundParser = new ValueEvaluatorParser(tokenStream);
			
			evalString="v: "+dvar.lowerBound;
			stream = new ANTLRStringStream(evalString);
			lexer = new ValueEvaluatorLexer(stream);
			tokenStream = new CommonTokenStream(lexer);
			dvar.lowerBoundParser = new ValueEvaluatorParser(tokenStream);
			
			evalString="v: "+dvar.timeArraySize;
			stream = new ANTLRStringStream(evalString);
			lexer = new ValueEvaluatorLexer(stream);
			tokenStream = new CommonTokenStream(lexer);
			dvar.timeArraySizeParser = new ValueEvaluatorParser(tokenStream);
		}
	}

	public void preEvaluateGoal(){
		for (String gName: gList){
			//System.out.println("PreEvaluate constraint "+gName);
			Goal goal=gMap.get(gName);
			ArrayList<String> caseCondition=goal.caseCondition;
			int i=-1;
			goal.caseConditionParsers=new ArrayList<ValueEvaluatorParser>();
			goal.caseExpressionParsers=new ArrayList<EvaluatorParser>();
			while(i<=caseCondition.size()-2){
				i=i+1;
				String evalString="c: "+caseCondition.get(i);
				ANTLRStringStream stream = new ANTLRStringStream(evalString);
				ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
				TokenStream tokenStream = new CommonTokenStream(lexer);
				ValueEvaluatorParser evaluator = new ValueEvaluatorParser(tokenStream);
				goal.caseConditionParsers.add(evaluator);

				String evalString1="g: "+goal.caseExpression.get(i);
				ANTLRStringStream stream1 = new ANTLRStringStream(evalString1);
				EvaluatorLexer lexer1 = new EvaluatorLexer(stream1);
				TokenStream tokenStream1 = new CommonTokenStream(lexer1);
				EvaluatorParser evaluator1 = new EvaluatorParser(tokenStream1);
				goal.caseExpressionParsers.add(evaluator1);
			}
			String evalString="v: "+goal.timeArraySize;
			ANTLRStringStream stream = new ANTLRStringStream(evalString);
			ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
			CommonTokenStream tokenStream = new CommonTokenStream(lexer);
			goal.timeArraySizeParser = new ValueEvaluatorParser(tokenStream);
		}
	}
	
	public void preEvaluateSvar(){
		for (String svName: svList){
			//System.out.println("PreEvaluate svar "+svName);
			Svar svar=svMap.get(svName);
			ArrayList<String> caseCondition=svar.caseCondition;
			int i=-1;
			svar.caseConditionParsers=new ArrayList<ValueEvaluatorParser>();
			svar.caseExpressionParsers=new ArrayList<ValueEvaluatorParser>();
			while(i<=caseCondition.size()-2){
				i=i+1;
				String evalString="c: "+caseCondition.get(i);
				ANTLRStringStream stream = new ANTLRStringStream(evalString);
				ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
				TokenStream tokenStream = new CommonTokenStream(lexer);
				ValueEvaluatorParser evaluator = new ValueEvaluatorParser(tokenStream);
				svar.caseConditionParsers.add(evaluator);

				evalString="v: "+svar.caseExpression.get(i);
				stream = new ANTLRStringStream(evalString);
				lexer = new ValueEvaluatorLexer(stream);
				tokenStream = new CommonTokenStream(lexer);
				evaluator = new ValueEvaluatorParser(tokenStream);
				svar.caseExpressionParsers.add(evaluator);
			}
			String evalString="v: "+svar.timeArraySize;
			ANTLRStringStream stream = new ANTLRStringStream(evalString);
			ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
			CommonTokenStream tokenStream = new CommonTokenStream(lexer);
			svar.timeArraySizeParser = new ValueEvaluatorParser(tokenStream);
		}
	}
}
