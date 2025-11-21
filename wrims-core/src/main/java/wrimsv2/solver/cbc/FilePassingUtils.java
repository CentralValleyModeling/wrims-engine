package wrimsv2.solver.cbc;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilePassingUtils {
    private static final Logger logger = LoggerFactory.getLogger(FilePassingUtils.class);
	public static int solverErrorCode;// 0:no problem 1: not optimal 2: other errors
	public static String solverMessage = "";
	public static Double objective_value;
	public static Map<String, Double> varDoubleMap;
	private static String solutionPath;
	private static File solutionFile;
	private static Scanner solutionScanner;

	public static int parseReturnFile(String filePath) {
		reset();
		setReturnFile(filePath);
		solverMessage = parseSolverMessage();
		
		if (!solverMessage.toLowerCase().startsWith("optimal")) {
			varDoubleMap = null;
			objective_value = null;
			solverErrorCode = 1;
        } else {
			// get objective value
			try {
				String line = solverMessage.replace('\t', ' ');
				String[] split = line.split("\\s+");
				String vstring = split[split.length-1];
				objective_value = Double.parseDouble(vstring);
                logger.debug("ov: {}", objective_value);
                // get solution
                varDoubleMap = new LinkedHashMap<>();
                parseSolution();
                solverErrorCode = 0;
			} catch (Exception ex){
                logger.error("Exception encountered when parsing objective value", ex);
				solverErrorCode = 2;
			}
        }
        return solverErrorCode;
    }

	private static void reset(){
		solverErrorCode = -99;
		solverMessage = "";
		objective_value = null;
		varDoubleMap = null;
		solutionPath = "";
		solutionFile = null;
		solutionScanner = null;
	}
	
	private static void setReturnFile(String filePath) {
		solutionPath = filePath;
		solutionFile = new File(solutionPath);

		try {
			solutionScanner = new Scanner(solutionFile);
		} catch (FileNotFoundException e) {
			logger.error("FileNotFoundException encountered when scanning solution file.", e);
		}
	}

	private static String parseSolverMessage() {
		// 1st line
		String line = solutionScanner.nextLine();
		line = line.trim();
		
		return line;
	}
	
	private static String parseObjectiveValue() {
		// 2nd line
		String line = solutionScanner.nextLine();
		line = line.trim();
		line = line.replace('\t', ' ');
		String[] split = line.split("\\s+");

        return split[2];
	}

	private static void parseSolution() {
		while (solutionScanner.hasNextLine()) {
			String line = solutionScanner.nextLine();
			line = line.replace("**", ""); // TODO: what does ** mean in the beginning of the solution line?
			line = line.trim();
			line = line.replace('\t', ' ');
			logger.debug(line);

			String[] split = line.split("\\s+");
			if (split.length < 3)
				continue;

			String key = split[1];
			String value = split[2];

            logger.debug("dvar: {} value:{}\n", key, value);
			varDoubleMap.put(key, Double.parseDouble(value));
		}
	}

	public static void main(String[] args) {
		String p = "D:\\ucd\\example\\solutionExample.txt";
		parseReturnFile(p);

        logger.info("solver message:{}", solverMessage);
        logger.info("error code:{}", solverErrorCode);
        logger.info("obj value:{}", objective_value);
		
		for (String key : varDoubleMap.keySet()) {
            logger.info("{}:{}", key, varDoubleMap.get(key));
		}
	}
}
