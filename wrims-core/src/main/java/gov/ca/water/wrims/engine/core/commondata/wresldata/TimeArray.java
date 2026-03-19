package gov.ca.water.wrims.engine.core.commondata.wresldata;

import org.antlr.runtime.RecognitionException;

import gov.ca.water.wrims.engine.core.components.Error;
import gov.ca.water.wrims.engine.core.components.IntDouble;
import gov.ca.water.wrims.engine.core.evaluator.ValueEvaluatorParser;

public class TimeArray {
	public int getTimeArraySize(ValueEvaluatorParser timeArraySizeParser){
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
}
