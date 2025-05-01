package wrimsv2.commondata.tabledata;

public class Alias {

	private String type;
	private String units;
	private String expression;

	public Alias(){
		type="undefined";
		units="undefined";
	}

	public void setType(String type){
		this.type=type;
	}

	public void setUnits(String units){
		this.units=units;
	}

	public void setExpression(String expression){
		this.expression=expression;
	}
	
	public String getType(){
		return type;
	}
	
	public String getUnits(){
		return units;
	}
	
	public String getExpression(){
		return expression;
	}
}

