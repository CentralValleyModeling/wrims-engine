package wrimsv2.commondata.wresldata;

import org.antlr.runtime.RecognitionException;

import wrimsv2.components.Error;
import wrimsv2.components.IntDouble;
import wrimsv2.evaluator.ValueEvaluatorParser;

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
