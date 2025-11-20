package gov.ca.water.wrims.engine.core.sql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.tools.Decryption;

public class DataBaseProfile {
	
	public DataBaseProfile(String[] args){
		int i=0;
		boolean isConfigExist=false;
		
		while (i<args.length && !isConfigExist){
			if (args[i].toLowerCase().startsWith("-config")){
				String configFilePath = args[i].substring(args[i].indexOf("=") + 1, args[i].length());
				String profileFilePath=configFilePath+".dpf";
				File profileFile=new File(profileFilePath);
				if (profileFile.exists()){
					try {
						FileInputStream fs = new FileInputStream(profileFile.getAbsolutePath());
						BufferedReader br = new BufferedReader(new InputStreamReader(fs));
					    ControlData.USER = Decryption.procDecryption(br.readLine());
					    ControlData.PASS = Decryption.procDecryption(br.readLine());
					    br.close();
					    fs.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					profileFile.delete();
				}
				isConfigExist=true;
			}else if (args[i].toLowerCase().startsWith("-dss_sql")){
				String configFilePath = args[i].substring(args[i].indexOf("=") + 1, args[i].length());
				File configFile = new File (configFilePath);
				if (configFile.exists()){
					try {
						FileInputStream fs = new FileInputStream(configFile.getAbsolutePath());
						BufferedReader br = new BufferedReader(new InputStreamReader(fs));
					    ControlData.USER = Decryption.procDecryption(br.readLine());
					    ControlData.PASS = Decryption.procDecryption(br.readLine());
					    ControlData.databaseURL = br.readLine().toLowerCase();
					    ControlData.sqlGroup = br.readLine();
					    DssToSQLDatabase.dssInfoFilePath = br.readLine();
					    ControlData.ovOption = Integer.parseInt(br.readLine());
					    ControlData.ovFile = br.readLine();
					    br.close();
					    fs.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					configFile.delete();
				}
				isConfigExist=true;
			}
			i++;
		}		
	}

}
