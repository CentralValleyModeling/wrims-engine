package gov.ca.water.wrims.engine.core.solver.clp;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class FilePassingUtils {

	public static int solverErrorCode;// 0:no problem 1: not optimal 2: other errors
	public static String solverMessage = "";
	public static Double objective_value;
	public static Map<String, Double> varDoubleMap;
	private static String soluPath;
	private static File soluFile;
	private static Scanner soluScan;

	

	public static int parseReturnFile(String filePath) {
		
		reset();
		setReturnFile(filePath);
		solverMessage = parseSolverMessage();
		
		if (!solverMessage.equalsIgnoreCase("optimal")) {

			varDoubleMap = null;
			objective_value = null;
			solverErrorCode = 1;
			
			return solverErrorCode;
			
		} else {
			
			// get objective value
			try {
			
				objective_value = Double.parseDouble(parseObjectiveValue());
				
			} catch (Exception ex){
			
				solverErrorCode = 2;
				return solverErrorCode;
			}
			
			// get solution
			varDoubleMap = new LinkedHashMap<String, Double>();
			parseSolution();
			
			solverErrorCode = 0;
			return solverErrorCode;

		}
	}

	
	private static void reset(){
		
		solverErrorCode = -99;
		solverMessage = "";
		objective_value = null;
		varDoubleMap = null;
		soluPath = "";
		soluFile = null;
		soluScan = null;
		
		
	}
	
	private static void setReturnFile(String filePath) {

		soluPath = filePath;
		soluFile = new File(filePath);

		try {
			soluScan = new Scanner(soluFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static String parseSolverMessage() {
		
		// 1st line
		String line = soluScan.nextLine();
		line = line.trim();
		
		return line;

	}
	
	private static String parseObjectiveValue() {
				
		// 2nd line
		String line = soluScan.nextLine();
		line = line.trim();
		line = line.replace('\t', ' ');
		String[] splited = line.split("\\s+");
		
		String objective_value_string = splited[2];
		return objective_value_string;
		
//		try {
//			
//			objective_value = Double.parseDouble(splited[2]);
//			
//			return true;
//			
//		} catch (Exception ex){
//			
//			solverError = true;
//			return false;
//		}

		
	}
	private static void parseSolution() {


		
		while (soluScan.hasNextLine()) {
			String line = soluScan.nextLine();

			line = line.replace("**", ""); // TODO: what does ** mean in the beginning of the solution line?
			line = line.trim();
			line = line.replace('\t', ' ');

			// System.out.println(line);

			// System.out.println(line);

			String[] splited = line.split("\\s+");

			if (splited.length < 3)
				continue;

			String num = splited[0];

			String key = splited[1];

			String value = splited[2];

			// System.out.println("dvar: "+key+" value:"+value+"\n");
			//System.out.println(line);
			varDoubleMap.put(key, Double.parseDouble(value));

			// break at the line "End Config"
			// if (key.equalsIgnoreCase("end") &
			// value.equalsIgnoreCase("config"))
			// break;

		}

	}

	public static void main(String[] args) {

		String p = "D:\\ucd\\example\\solutionExample.txt";
		parseReturnFile(p);

		System.out.println("solver message:"+solverMessage);
		System.out.println("error code:"+solverErrorCode);
		System.out.println("obj value:"+objective_value);
		
		for (String key : varDoubleMap.keySet()) {
			System.out.println(key + ":" + varDoubleMap.get(key));
		}

	}
}
