package gov.ca.water.wrims.engine.core.evaluator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.Error;

public class DssDataSet {
	private ArrayList<Double> data;
	private String timeStep;
	private String units;
	private String convertToUnits="";
	private String kind;
	private Date startTime;
	private boolean fromDssFile=false;
	private int studyStartIndex=-1;
	private static HashMap<Double, Date> selDataMap=new HashMap<Double, Date>();
	
	public void setData(ArrayList<Double> data){
		this.data=data;
	}
	
	public ArrayList<Double> getData(){
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
	
	public String getConvertToUnits(){
		return convertToUnits;
	}
	
	public void setConvertToUnits(String convertToUnits){
		this.convertToUnits=convertToUnits;
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
	
	public void generateStudyStartIndex(){
		Date st=getStartTime();
		//long sTime=st.getTime();
		int sYear=st.getYear()+1900;
		int sMonth=st.getMonth()+1; //Originally it should be getMonth()-1. However, dss data store at 24:00 Jan31, 1921 is considered to store at 0:00 Feb 1, 1921 
		Date studyStart=new Date(ControlData.startYear-1900, ControlData.startMonth-1, ControlData.startDay);
		if (TimeOperation.isMonthlyInterval(getTimeStep())){
			studyStartIndex=ControlData.startYear*12+ControlData.startMonth-(sYear*12+sMonth);
		}else{
			//double indexValue=(studyStartTime-sTime)/(1000*60*60*24);
			Calendar c1=Calendar.getInstance();
			c1.setTime(st);
			Calendar c2=Calendar.getInstance();
			c2.setTime(studyStart);
			long indexValue = Duration.between(c1.toInstant(), c2.toInstant()).toDays();
			studyStartIndex=(int)indexValue+1;
		}
	}
	
	public int getStudyStartIndex(){
		return studyStartIndex;
	}
	
	public ArrayList<Double> getTimeseriesDataWithOptions(String selMon, Date selSd, Date selEd){
		ArrayList<Double> selData=new ArrayList<Double>();
		selDataMap=new HashMap<Double, Date>();
		String m1="all";
		String m2="all";
		double sum=0;
		if (selMon.length()==3){
			m1=selMon;
			m2=selMon;		
		}else{
			m1=selMon.substring(0, 3);
			m2=selMon.substring(3);
		}
		Date dataDate = startTime;
		if (TimeOperation.isMonthlyInterval(timeStep)){
			dataDate=TimeOperation.backOneMonth(dataDate);
		}else{
			dataDate=TimeOperation.backOneDay(dataDate);
		}
		Date entryDate=dataDate;
		for (int i=0; i<data.size(); i++){
			if (dataDate.after(selEd)){
				return selData;
			}else if (dataDate.before(selSd)){
				if (TimeOperation.isMonthlyInterval(timeStep)){
					dataDate=TimeOperation.addOneMonth(dataDate);
				}else{
					dataDate=TimeOperation.addOneDay(dataDate);
				}
			}else{
				if (selMon.equalsIgnoreCase("all")){
					selData.add(data.get(i));
					selDataMap.put(sum, dataDate);
				}else if (TimeOperation.range(dataDate.getMonth()+1, m1, m2)){
					if (TimeOperation.range(dataDate.getMonth()+1, m1, m1)){
						sum=0;
						entryDate=dataDate;
					}
					sum=sum+data.get(i);
					if (TimeOperation.range(dataDate.getMonth()+1, m2, m2)){
						selData.add(sum);
						selDataMap.put(sum, entryDate);
					}
				}
				if (TimeOperation.isMonthlyInterval(timeStep)){
					dataDate=TimeOperation.addOneMonth(dataDate);
				}else{
					dataDate=TimeOperation.addOneDay(dataDate);
				}
			}
		}
		return selData;	
	}
	
	public static double getExceedance(ArrayList<Double> optedData, double exc) {
		int size = optedData.size();
		if (size==0){
			Error.addEvaluationError("The data in the specified time period and selected month(s) from the timeseries doesn't exist in the Exceedance function.");
			return 1.0;
		}
		Double[] values =optedData.toArray(new Double[size]);
		Arrays.sort(values);
		if (exc==1){
			return values[0];
		}
		double excIndex = (1.0-exc)*size;
		if (excIndex>=1.0){
			int index = (int) Math.floor(excIndex);
			double fract = excIndex-index;
			double value = values[index-1]+fract*(values[index]-values[index-1]);
			return value;
		}else{
			double value=values[0]-(1.0-excIndex)*(values[1]-values[0]);
			return value;
		}
	}
	
	public static int getExceedance_tsi(ArrayList<Double> optedData, double exc) {
		int size = optedData.size();
		if (size==0){
			Error.addEvaluationError("The data in the specified time period and selected month(s) from the timeseries doesn't exist in the Exceedance function.");
			return 0;
		}
		Double[] values =optedData.toArray(new Double[size]);
		Arrays.sort(values);
		double excIndex = (1.0-exc)*size;
		int index=(int)Math.round(excIndex)-1;
		if (index<0) index=0;
		Date dataDate=selDataMap.get(values[index]);
		int dataYear=dataDate.getYear()+1900;
		int dataMonth=dataDate.getMonth()+1; //HEC DSS7 uses getMonth()+1. However, Vista/HecDSS6 uses getMonth() because dss data store at 24:00 Jan31, 1921 is considered to store at 0:00 Feb 1, 1921 
		int dataDay = dataDate.getDate();
		if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
			return (dataYear-ControlData.currYear)*12+(dataMonth-ControlData.currMonth);
		}else{
			Date currDate=new Date (ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
			//long dataTime=dataDate.getTime();
			//long currTime=currDate.getTime();
			//double tsi=(dataTime-currTime)/(24*60*60*1000l);
			Calendar c1=Calendar.getInstance();
			c1.setTime(currDate);
			Calendar c2=Calendar.getInstance();
			c2.setTime(dataDate);
			long tsi = Duration.between(c1.toInstant(), c2.toInstant()).toDays();
			return (int)tsi;
		}
		
	}
}
