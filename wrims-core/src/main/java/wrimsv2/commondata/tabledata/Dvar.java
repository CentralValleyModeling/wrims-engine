package wrimsv2.commondata.tabledata;

public class Dvar {

	private String type;
	private String units;
	private String lowerBound;
	private String upperBound;
	private boolean integerType;
	private String sourceFile;
	private int lineNumber;
	private int posLowerBound;
	private int posUpperBound;

	public Dvar(){
		type="undefined";
		units="undefined";
		integerType=false;
	}

	public void setType(String type){
		this.type=type;
	}

	public void setUnits(String units){
		this.units=units;
	}

	public void setLowerBound(String lowerBound){
		this.lowerBound=lowerBound;
	}

	public void setUpperBound(String upperBound){
		this.upperBound=upperBound;
	}
	
	public void setIntegerType(boolean isInteger){
		integerType=isInteger;
	}
	
	public void setSourceFileName(String fileName){
		sourceFile=fileName;
	}
	
	public void setLineNumber(int number){
		lineNumber=number;
	}
	
	public void setPosLowerBound(int pos){
		posLowerBound=pos;
	}
	
	public void setPosUpperBound(int pos){
		posUpperBound=pos;
	}
	
	public String getLowerBound(){
		return lowerBound;
	}
	
	public String getUpperBound(){
		return upperBound;
	}
	
	public String getUnits(){
		return units;
	}	
	
	public String getType(){
		return type;
	}
	
	public String getIntegerType(){
		if (integerType){
			return "true";
		}else{
			return "false";
		}
	}
	
	public String getSourceFileName(){
		return sourceFile;
	}
	
	public int getLineNumber(){
		return lineNumber;
	}
	
	public int getPosLowerBound(){
		return posLowerBound;
	}
	
	public int getPosUpperBound(){
		return posUpperBound;
	}
}

