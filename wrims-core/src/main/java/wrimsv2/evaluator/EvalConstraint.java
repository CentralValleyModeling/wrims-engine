package wrimsv2.evaluator;

public class EvalConstraint {
	private EvalExpression evalExpression=new EvalExpression();
	private String sign;
	
	public void setEvalExpression(EvalExpression ee){
		evalExpression=ee;
	}
	
	public void setSign(String sign){
		this.sign=sign;
	}
	
	public EvalExpression getEvalExpression(){
		return evalExpression;
	}
	
	public String getSign(){
		return sign;
	}
	
	public EvalConstraint copyOf(){
		EvalConstraint evalConstraint= new EvalConstraint();
		evalConstraint.setEvalExpression(evalExpression.copyOf());
		evalConstraint.setSign(new String(sign));
		return evalConstraint;
	}
}
