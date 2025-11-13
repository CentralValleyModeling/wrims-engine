package wrims.engine.core.evaluator;

import java.util.Date;

public class DssDataSetFixLength {
	public double[] data;
	private String timeStep;
	private String units;
	private String kind;
	private Date startTime;
	private boolean fromDssFile=false;
	
	public DssDataSetFixLength(){

	}

	public DssDataSetFixLength(int size){
		data=new double[size];
		for (int i=0; i<size; i++){
			data[i]=-901.0;
		}
	}

	public void setData(double[] data){
		this.data=data;
	}
	
	public double[] getData(){
		return data;
	}
	
	public void setTimeStep(String timeStep){
		this.timeStep=timeStep;
	}
	
	public void setStartTime(Date st){
		startTime=st;
	}
	
	public Date getStartTime(){
		return startTime;
	}
	
	public String getTimeStep(){
		return timeStep;
	}
	
	public boolean fromDssFile(){
		return fromDssFile;
	}
	
	public void setFromDssFile(boolean fromDssFile){
		this.fromDssFile=fromDssFile;
	}
	
	public void setUnits(String units){
		this.units=units;
	}
	
	public String getUnits(){
		return units;
	}
	
	public void setKind(String kind){
		this.kind=kind;
	}
	
	public String getKind(){
		return kind;
	}
}
