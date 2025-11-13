package wrims.engine.core.solver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.FilePaths;

public class SetXALog {
	static boolean isConfigRead=false; 
	static String line="set sortName YES MPSX Yes Set FreqLog :01";
	
	public static void enableXALog(){
		File config=new File ("xa_config.dat");
		if (config.exists() && !isConfigRead){
			BufferedReader br;
			try {
				FileReader fr=new FileReader(config);
				br = new BufferedReader(fr);
				line = br.readLine();
				isConfigRead=true;
				System.out.println("Retrieve XA configuation from xa_config.dat file");
				ControlData.xasolver.setCommand( "set debug no ToRcc Yes FileName  "+FilePaths.mainDirectory+"  Output "+FilePaths.mainDirectory+"\\xa.log "+line);
				br.close();
				fr.close();
			} catch (Exception e) {
				e.printStackTrace();
				ControlData.xasolver.setCommand( "set debug no ToRcc Yes FileName  "+FilePaths.mainDirectory+"  Output "+FilePaths.mainDirectory+"\\xa.log set sortName YES MPSX Yes Set FreqLog :01") ;
			} 
		}else{
			ControlData.xasolver.setCommand( "set debug no ToRcc Yes FileName  "+FilePaths.mainDirectory+"  Output "+FilePaths.mainDirectory+"\\xa.log "+line) ;
			//ControlData.xasolver.setCommand("When +10920 FileName d:\\xatemp\\xa%d ToRcc Yes Output xa%d set sortName YES MPSX Yes");
		}
	}
	
	public static void disableXALog(){
		ControlData.xasolver.setCommand("set debug no");
	}
}
