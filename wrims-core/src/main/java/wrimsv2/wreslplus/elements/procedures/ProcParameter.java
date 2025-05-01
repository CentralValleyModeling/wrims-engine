package wrimsv2.wreslplus.elements.procedures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;

import wrimsv2.commondata.wresldata.Param;
import wrimsv2.commondata.wresldata.Svar;
import wrimsv2.components.ControlData;
import wrimsv2.components.IntDouble;
import wrimsv2.components.Error;
import wrimsv2.evaluator.ValueEvaluatorLexer;
import wrimsv2.evaluator.ValueEvaluatorParser;
import wrimsv2.wreslplus.elements.ParamTemp;
import wrimsv2.wreslplus.elements.StudyTemp;
import wrimsv2.wreslplus.elements.SvarTemp;

public class ProcParameter {

	private ProcParameter() {
	}

	public static void process(StudyTemp st){


		st.controlDataParameterMap = convertParamMapToSvarMap(st.parameterMap);
		ControlData.parameterMap=st.controlDataParameterMap;
		evaluateAllParams(st.parameterList, st.controlDataParameterMap);	
		
	}	


	private static void evaluateAllParams(ArrayList<String> keyList, Map<String, Svar> map) {
		
		for (String key: keyList) {
			
			Svar svar = map.get(key);
			ControlData.currEvalName=key;
			ControlData.currEvalTypeIndex=8;
			
			
			for (int i=0; i<svar.caseCondition.size(); i++) {
				
				String conditionStr = svar.caseCondition.get(i);
				boolean condition = callBooleanEvaluator(conditionStr);
				
				if (condition) {
					
					String evalStr = svar.caseExpression.get(i);
					IntDouble evalValue = callNumberEvaluator(evalStr);
					svar.setData(evalValue);
					
				}
			
			}
						
		}
		
	}
	private static boolean callBooleanEvaluator(String evalString) {

		boolean evalBoolean = false;
		
		evalString="c: "+evalString;
		ANTLRStringStream stream = new ANTLRStringStream(evalString);
		ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
		TokenStream tokenStream = new CommonTokenStream(lexer);
		ValueEvaluatorParser evaluator = new ValueEvaluatorParser(tokenStream);
		
		
		try {
			evaluator.evaluator();

			if (Error.getTotalError() > 0) {
				Error.writeErrorLog();
			}

			evalBoolean = evaluator.evalCondition;
			
		} catch (RecognitionException e) {
			
				Error.writeErrorLog();
		}
		
		return evalBoolean;

	}
	
	private static IntDouble callNumberEvaluator(String evalString) {

		IntDouble evalValue=null;
		
		evalString="v: "+evalString;
		ANTLRStringStream stream = new ANTLRStringStream(evalString);
		ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
		TokenStream tokenStream = new CommonTokenStream(lexer);
		ValueEvaluatorParser evaluator = new ValueEvaluatorParser(tokenStream);
		
		
		try {
			evaluator.evaluator();

			if (Error.getTotalError() > 0) {
				Error.writeErrorLog();
			}

			evalValue = evaluator.evalValue.copyOf();
			
		} catch (RecognitionException e) {
			
				Error.writeErrorLog();
		}
		
		return evalValue;

	}
	
	
	
	private static LinkedHashMap<String, Svar> convertParamMapToSvarMap(
			Map<String, SvarTemp> simpleMap) {
		
		LinkedHashMap<String, Svar> pm = new LinkedHashMap<String, Svar>();
		
		for (String key: simpleMap.keySet()){
			
			SvarTemp jObj = simpleMap.get(key);
			
			Svar svObj = new Svar();
			
//			svObj.caseName.add(Param.defaultCaseName);
//			svObj.caseCondition.add(Param.always);
//			svObj.caseExpression.add(simpleMap.get(key).expression);

			svObj.caseName = jObj.caseName;
			svObj.caseCondition = jObj.caseCondition;
			svObj.caseExpression = jObj.caseExpression;
			
			pm.put(key, svObj);
			
		}
		
		return pm;
	}
}
