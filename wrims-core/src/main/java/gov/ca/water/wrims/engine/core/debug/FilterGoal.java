package gov.ca.water.wrims.engine.core.debug;

public class FilterGoal {
	private String alias="None";
	private String tolerance="0.0";
	private String control="n";
	private String content="";
	
	public String getAlias(){
		return alias;
	}
	
	public void setAlias(String alias){
		this.alias=alias;
	}
	
	public String getTolerance(){
		return tolerance;
	}
	
	public void setTolerance(String tolerance){
		this.tolerance=tolerance;
	}
	
	public String getContent(){
		return content;
	}
	
	public void setContent(String content){
		this.content=content;
	}
	
	public String getControl(){
		return control;
	}
	
	public void setControl(String control){
		this.control=control;
	}
}
