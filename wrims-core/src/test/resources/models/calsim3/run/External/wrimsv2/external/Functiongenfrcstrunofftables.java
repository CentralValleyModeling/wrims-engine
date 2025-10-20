package wrimsv2.external;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Stack;

import wrimsv2.components.ControlData;
import wrimsv2.components.FilePaths;
import wrimsv2.evaluator.TimeOperation;

public class Functiongenfrcstrunofftables extends ExternalFunction{

	private ArrayList<Integer> wYears=new ArrayList<Integer>() ;
	private ArrayList<ArrayList<Double>> frcst_fea_arr=new ArrayList<ArrayList<Double>>();
	ArrayList<Double> frcst_fea_list=new ArrayList<Double>();
	private ArrayList<ArrayList<Double>> frcst_sac_arr=new ArrayList<ArrayList<Double>>();
	ArrayList<Double> frcst_sac_list=new ArrayList<Double>();
	private ArrayList<ArrayList<Double>> frcst_amr_arr=new ArrayList<ArrayList<Double>>();
	ArrayList<Double> frcst_amr_list=new ArrayList<Double>();

	public Functiongenfrcstrunofftables(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param3 = stack.pop();
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		double frcst_amr = ((Number) param3).doubleValue();
		double frcst_sac  = ((Number) param2).doubleValue();
		double frcst_fea = ((Number) param1).doubleValue();

		formTables(frcst_fea, frcst_sac, frcst_amr);

		// push the result on the Stack
		stack.push(1.0);
	}

	public void formTables(double frcst_fea, double frcst_sac, double frcst_amr){

	    if (ControlData.currMonth==1){
	    	wYears.add(TimeOperation.waterYearValue());
	    	frcst_fea_list=new ArrayList<Double>();
	    	frcst_fea_list.add(frcst_fea);
	    	frcst_fea_arr.add(frcst_fea_list);
	    }else if (ControlData.currMonth>=2 && ControlData.currMonth<=5){
	    	frcst_fea_list.add(frcst_fea);
	    }

	    if (ControlData.currMonth==2){
	    	frcst_sac_list=new ArrayList<Double>();
	    	frcst_sac_list.add(frcst_sac);
	    	frcst_sac_arr.add(frcst_sac_list);
	    }else if (ControlData.currMonth>=3 && ControlData.currMonth<=5){
	    	frcst_sac_list.add(frcst_sac);
	    }

	    if (ControlData.currMonth==2){
	    	frcst_amr_list=new ArrayList<Double>();
	    	frcst_amr_list.add(frcst_amr);
	    	frcst_amr_arr.add(frcst_amr_list);
	    }else if (ControlData.currMonth>=3 && ControlData.currMonth<=9){
	    	frcst_amr_list.add(frcst_amr);
	    }



		if (ControlData.currYear==ControlData.endYear && ControlData.currMonth==ControlData.endMonth){
			writeTable("feather_runoff_forecast", 1, 5, frcst_fea_arr);
			writeTable("sacramento_runoff_forecast", 2, 5, frcst_sac_arr);
			writeTable("american_runoff_forecast", 2, 9, frcst_amr_arr);
		}
	}

	public void writeTable(String tableName, int startMonth, int endMonth, ArrayList<ArrayList<Double>> frcst_arr){
		String fullPath=FilePaths.genTableDir+tableName+".table";

		try{
			File tableFile = new File(fullPath);
			File tableFolder=tableFile.getParentFile();
			if (!tableFolder.exists()) tableFolder.mkdirs();
			tableFile.createNewFile();
			FileWriter fw = new FileWriter(tableFile);
			PrintWriter out = new PrintWriter(fw);

			out.println(tableName);
			String line="wateryear";
			for (int i=startMonth; i<=endMonth;i++){
				line=line+"\t"+TimeOperation.monthName(i);
			}
			out.println(line);

			for (int i=0; i<frcst_arr.size(); i++){
				line=wYears.get(i).toString();
				ArrayList<Double> frcst_list = frcst_arr.get(i);
				for (int j=0; j<frcst_list.size(); j++){
					line=line+"\t"+Math.round(frcst_list.get(j));
				}
				out.println(line);
			}
			out.close();
			fw.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
}
