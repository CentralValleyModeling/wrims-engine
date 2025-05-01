package wrimsv2.wreslplus.elements;

import java.io.IOException;
import java.io.PrintWriter;

public class OutputUtils {

	private static PrintWriter _outputFile;


	public static void closeLogFile(){
		
		_outputFile.close();		
	}

	public static PrintWriter setLogFile(String parentDir, String logFileName){
		
		try {
			_outputFile = Tools.openFile(parentDir, logFileName);
			
		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return _outputFile;	
	}	
	
	public static PrintWriter setLogFile(String logFileName){
				
		try {
			_outputFile = Tools.openFile(System.getProperty("user.dir"), logFileName);
			
		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return _outputFile;	
	}	
}
	
