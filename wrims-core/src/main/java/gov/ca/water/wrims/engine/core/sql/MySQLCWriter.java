package gov.ca.water.wrims.engine.core.sql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.FilePaths;
import gov.ca.water.wrims.engine.core.evaluator.DataTimeSeries;
import gov.ca.water.wrims.engine.core.evaluator.DssDataSetFixLength;
import gov.ca.water.wrims.engine.core.evaluator.DssOperation;
import gov.ca.water.wrims.engine.core.evaluator.TimeOperation;

public class MySQLCWriter {
	
	private String JDBC_DRIVER = "com.mysql.jdbc.Driver";       
	
	private String URL;
	private String database;
	
	private Connection conn = null;
	private Statement stmt = null;
	
	private ArrayList<ArrayList<String>> monthlyEntriesArr;
	private ArrayList<ArrayList<String>> dailyEntriesArr;
	
	private String monthlyTableName=FilePaths.sqlScenarioName+"_"+ControlData.partA+"_"+ControlData.svDvPartF+"_M";
	private String dailyTableName=FilePaths.sqlScenarioName+"_"+ControlData.partA+"_"+ControlData.svDvPartF+"_D";
	
	private int col_limit=750;
	
	private String slackPrefix="slack__";
	private String surplusPrefix="surplus__";
	
	public MySQLCWriter(){   
		connectToDataBase();
	}
	
	public void process(){
		deleteTablesIfExist();
		createTables();
		writeData();
		close();
	}
	
	public void connectToDataBase(){	
		
		try {
			if (ControlData.databaseURL.contains("/")){
				int index=ControlData.databaseURL.lastIndexOf("/");
				URL=ControlData.databaseURL.substring(0, index);
				database=ControlData.databaseURL.substring(index+1);
			}else{
				URL=ControlData.databaseURL;
				database=ControlData.databaseURL;
			}
			
			Class.forName(JDBC_DRIVER);
			System.out.println("Connecting to a selected database...");
			conn = DriverManager.getConnection(URL, ControlData.USER, ControlData.PASS);
			stmt = conn.createStatement();
			String sql="CREATE DATABASE IF NOT EXISTS "+database;
			stmt.executeUpdate(sql);
			sql="USE "+database;
			stmt.executeUpdate(sql);
			System.out.println("Connected database successfully");
		} catch (ClassNotFoundException e) {
			System.err.println("Failed to load database. Please install the database driver.");
			System.out.println("Model run terminated.");
			System.exit(1);
		} catch (SQLException e) {
			System.err.println("Failed to connect to the database. Please check your database URL and user profile.");
			System.out.println("Model run terminated.");
			System.exit(1);
		}
	}
		   
	public void deleteTablesIfExist(){
		System.out.println("Deleting old table...");
		deleteTablesByNames(monthlyTableName);	
		deleteTablesByNames(dailyTableName);	
		System.out.println("Deleted old table");
		
	}
	
	public void deleteTablesByNames(String tableName){
		try {
			int i=0;
			tableName=tableName.toUpperCase();
			String modTableName = tableName+"_"+i;
			stmt = conn.createStatement();
			DatabaseMetaData dbm = conn.getMetaData();
			ResultSet rs = dbm.getTables(database, null, modTableName, null);
			while (rs.next()){
				rs.close();
				String sql="DROP TABLE " + modTableName;
				stmt.executeUpdate(sql);
				i++;
				modTableName = tableName+"_"+i;
				rs = dbm.getTables(database, null, modTableName, null);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createTables(){
		System.out.println("Creating tables in given database...");
		try {
			stmt = conn.createStatement();
			monthlyEntriesArr=new ArrayList<ArrayList<String>>();
			dailyEntriesArr=new ArrayList<ArrayList<String>>();
			ArrayList<String> sqlMonthlyArr=new ArrayList<String>();
			ArrayList<String> sqlDailyArr=new ArrayList<String>();
			int kd=0;
			int km=0;
			int jm=0;
			int jd=0;
			
			Set<String> orgKeys = DataTimeSeries.dvAliasTS.keySet();
			TreeSet<String> keys = new TreeSet<String>(orgKeys);			
			Iterator<String> it = keys.iterator();
			String sqlMonthly = "CREATE TABLE " + monthlyTableName+"_"+jm + " (Date DATE, ";
			String sqlDaily = "CREATE TABLE " + dailyTableName+"_"+jd + " (Date DATE, ";
			ArrayList<String> monthlyEntries = new ArrayList<String>();
			ArrayList<String> dailyEntries = new ArrayList<String>();
			while (it.hasNext()){
				String name=it.next();
				if (!name.startsWith(surplusPrefix) && !name.startsWith(slackPrefix)){
					DssDataSetFixLength ts = DataTimeSeries.dvAliasTS.get(name);
					String timestep = ts.getTimeStep().toUpperCase();
					if (timestep.equals("1DAY")){
						if (kd<col_limit){
							sqlDaily=sqlDaily+formPathName(ts, name, timestep)+"$"+formUnitsName(ts)+" DOUBLE, ";
							dailyEntries.add(name);
							kd++;
						}else{
							int endIndex = sqlDaily.lastIndexOf(", ");
							sqlDaily=sqlDaily.substring(0, endIndex)+")";
							sqlDailyArr.add(sqlDaily);
							dailyEntriesArr.add(dailyEntries);
							kd=0;
							jd++;
							sqlDaily="CREATE TABLE " + dailyTableName+"_"+jd + " (Date DATE, ";
							sqlDaily=sqlDaily+formPathName(ts, name, timestep)+"$"+formUnitsName(ts)+" DOUBLE, ";
							dailyEntries=new ArrayList<String>();
							dailyEntries.add(name);
						}
					}else{ 
						if (km<col_limit){
							sqlMonthly=sqlMonthly+formPathName(ts, name, timestep)+"$"+formUnitsName(ts)+" DOUBLE, ";
							monthlyEntries.add(name);
							km++;
						}else{
							int endIndex = sqlMonthly.lastIndexOf(", ");
							sqlMonthly=sqlMonthly.substring(0, endIndex)+")";
							sqlMonthlyArr.add(sqlMonthly);
							monthlyEntriesArr.add(monthlyEntries);
							km=0;
							jm++;
							sqlMonthly="CREATE TABLE " + monthlyTableName+"_"+jm + " (Date DATE, ";
							sqlMonthly=sqlMonthly+formPathName(ts, name, timestep)+"$"+formUnitsName(ts)+" DOUBLE, ";
							monthlyEntries=new ArrayList<String>();
							monthlyEntries.add(name);
						}
					}
				}
			}
			
			if (monthlyEntries.size()>0) {
				int endIndex = sqlMonthly.lastIndexOf(", ");
				sqlMonthly=sqlMonthly.substring(0, endIndex)+")";
				sqlMonthlyArr.add(sqlMonthly);
				monthlyEntriesArr.add(monthlyEntries);
			}
			if (dailyEntries.size()>0){
				int endIndex = sqlDaily.lastIndexOf(", ");
				sqlDaily=sqlDaily.substring(0, endIndex)+")";
				sqlDailyArr.add(sqlDaily);
				dailyEntriesArr.add(dailyEntries);
			}
			
			for (int i=0; i<monthlyEntriesArr.size(); i++){
				stmt.executeUpdate(sqlMonthlyArr.get(i));
			}
			
			for (int i=0; i<dailyEntriesArr.size(); i++){
				stmt.executeUpdate(sqlDailyArr.get(i));
			}
			System.out.println("Created tables in given database");	
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void writeData(){
		System.out.println("Writing output in table...");
		writeMonthlyData();
		writeDailyData();
		System.out.println("Wrote output in table");
	}
	
	public void writeMonthlyData(){
		int size = monthlyEntriesArr.size();
		try {
			for (int k=0; k<size; k++){
				String modMonthlyTableName = monthlyTableName+"_"+k;
			    ArrayList<String> monthlyEntries = monthlyEntriesArr.get(k);	
				String name = monthlyEntries.get(0);
				DssDataSetFixLength ts = DataTimeSeries.dvAliasTS.get(name);
				Date date = ts.getStartTime();
				double[] data = ts.getData();
				for (int i=0; i<data.length; i++){
					Statement statement;
					statement = conn.createStatement();
					String sql="INSERT INTO " + modMonthlyTableName +" VALUES (";
					sql=sql+"'"+formDateData(date)+"', ";
					sql=sql+data[i];
					for (int j=1; j<monthlyEntries.size(); j++){
						name=monthlyEntries.get(j);
						ts = DataTimeSeries.dvAliasTS.get(name);
						data=ts.getData();
						sql=sql+", "+data[i];
					}
					sql=sql+")";
					statement.executeUpdate(sql);
					date=addOneMonth(date);				
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void writeDailyData(){
		int size = dailyEntriesArr.size();
		try {
			for (int k=0; k<size; k++){
				String modDailyTableName = dailyTableName+"_"+k;
			    ArrayList<String> dailyEntries = dailyEntriesArr.get(k);
				String name = dailyEntries.get(0);
				DssDataSetFixLength ts = DataTimeSeries.dvAliasTS.get(name);
				Date date = ts.getStartTime();
				double[] data = ts.getData();
				for (int i=0; i<data.length; i++){
					Statement statement;
					statement = conn.createStatement();
					String sql="INSERT INTO " + dailyTableName + " VALUES (";
					sql=sql+"'"+formDateData(date)+"', ";
					sql=sql+data[i];
					for (int j=1; j<dailyEntries.size(); j++){
						name=dailyEntries.get(j);
						ts = DataTimeSeries.dvAliasTS.get(name);
						data=ts.getData();
						sql=sql+", "+data[i];
					}
					sql=sql+")";
					statement.executeUpdate(sql);
					date=addOneDay(date);				
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public String formUnitsName(DssDataSetFixLength ts){
		String units=ts.getUnits().replaceAll("/", "_").replaceAll("-", "_");
		return units;
	}
	
	public String formPathName(DssDataSetFixLength ts, String name, String timestep){
		String pathName = DssOperation.getTSName(name).replaceAll("-", "_")+"$"+ts.getKind().replaceAll("-", "_");
		//String pathName = DssOperation.getTSName(name);
		return pathName;
	}
	
	public String formDateData(Date date){
		int year=date.getYear()+1900;
		int month=date.getMonth()+1;
		int day = date.getDate();
		return year+"-"+TimeOperation.monthNameNumeric(month)+"-"+TimeOperation.dayName(day);
	}
	
	public Date addOneMonth(Date date){
		int month=date.getMonth()+1;
		int year=date.getYear();
		if (month>11){
			month=month-12;
			year=year+1;
		}
		int day=TimeOperation.numberOfDays(month+1, year+1900);
		Date newDate = new Date(year, month, day);
		return newDate;
	}
	
	public Date addOneDay(Date date){
		Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, 1);
        Date newDate = c.getTime();
		return newDate;
	}
	
	public void close(){
		try {
			stmt.close();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
