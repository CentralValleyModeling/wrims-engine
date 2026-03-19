package gov.ca.water.wrims.engine.core.evaluator;

import java.util.ArrayList;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.BufferedTreeNodeStream;
import org.antlr.runtime.tree.CommonTree;

import gov.ca.water.wrims.engine.core.commondata.wresldata.ModelDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.StudyDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Svar;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.Error;

public class PreEvaluatorTree {
	private ArrayList<String> svList;
	private Map<String, Svar> svMap;

	public PreEvaluatorTree(StudyDataSet sds ){
		ArrayList<String> modelList=sds.getModelList();
		Map<String, ModelDataSet> modelDataSetMap=sds.getModelDataSetMap();
		for (int i=0; i<modelList.size(); i++){
			String model=modelList.get(i);
			ModelDataSet mds=modelDataSetMap.get(model);
			svList=mds.svList;
			svMap=mds.svMap;
			preEvaluateSvar();
		}
	}

	public void preEvaluateSvar(){
		for (String svName: svList){
			System.out.println("PreEvaluate svar "+svName);
			Svar svar=svMap.get(svName);
			ArrayList<String> caseCondition=svar.caseCondition;
			int i=-1;
			svar.caseConditionWalkers=new ArrayList<ValueEvaluatorTreeWalker>();
			svar.caseExpressionWalkers=new ArrayList<ValueEvaluatorTreeWalker>();
			while(i<=caseCondition.size()-2){
				i=i+1;
				String evalString="c: "+caseCondition.get(i);
				ANTLRStringStream stream = new ANTLRStringStream(evalString);
				ValueEvaluatorTreeLexer lexer = new ValueEvaluatorTreeLexer(stream);
				TokenStream tokenStream = new CommonTokenStream(lexer);
				ValueEvaluatorTreeParser evaluator = new ValueEvaluatorTreeParser(tokenStream);
				ValueEvaluatorTreeParser.evaluator_return parser_evaluator;
				try {
					parser_evaluator = evaluator.evaluator();
					CommonTree commonTree = (CommonTree) parser_evaluator.getTree();
					BufferedTreeNodeStream nodeStream = new BufferedTreeNodeStream(commonTree);
					nodeStream.setTokenStream(tokenStream); 
					ValueEvaluatorTreeWalker walker = new ValueEvaluatorTreeWalker(nodeStream);
					svar.caseConditionWalkers.add(walker);
				} catch (RecognitionException e) {
					ControlData.currCycleIndex=0;
					ControlData.currEvalName=svName;
					Error.addEvaluationError(e.getMessage());
				}

				evalString="v: "+svar.caseExpression.get(i);
				stream = new ANTLRStringStream(evalString);
				lexer = new ValueEvaluatorTreeLexer(stream);
				tokenStream = new CommonTokenStream(lexer);
				evaluator = new ValueEvaluatorTreeParser(tokenStream);
				try {
					parser_evaluator = evaluator.evaluator();
					CommonTree commonTree = (CommonTree) parser_evaluator.getTree();
					BufferedTreeNodeStream nodeStream = new BufferedTreeNodeStream(commonTree);
					nodeStream.setTokenStream(tokenStream); 
					ValueEvaluatorTreeWalker walker = new ValueEvaluatorTreeWalker(nodeStream);
					svar.caseExpressionWalkers.add(walker);
				} catch (RecognitionException e) {
					ControlData.currCycleIndex=0;
					ControlData.currEvalName=svName;
					Error.addEvaluationError(e.getMessage());
				}
			}
		}
	}
}
