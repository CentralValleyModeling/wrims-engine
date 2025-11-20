package gov.ca.water.wrims.engine.core.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.FilePaths;
import gov.ca.water.wrims.engine.core.evaluator.DataTimeSeries;
import gov.ca.water.wrims.engine.core.evaluator.DssDataSetFixLength;
import gov.ca.water.wrims.engine.core.evaluator.DssOperation;
import gov.ca.water.wrims.engine.core.evaluator.TimeOperation;

public class MySQLRWriterAlt{
	
	private String JDBC_DRIVER = "com.mysql.jdbc.Driver";       
    private String tableName = ControlData.sqlGroup;                 //input 
	private String scenarioTableName="Scenario";
	
	private String URL;
	private String database;
	
	private Connection conn = null;
	private Statement stmt = null;
	
	private String scenarioName=FilePaths.sqlScenarioName;
	private int scenarioIndex;
	
	private String slackPrefix="slack__";
	private String surplusPrefix="surplus__";
	
	public MySQLRWriterAlt(){   
		connectToDataBase();
	}
			
	public void process(){
		if (tableName.equalsIgnoreCase(scenarioTableName)){
			tableName=tableName+"_Studies";
		}
		setScenarioIndex();
		createTable();
		deleteOldData();
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
	
	public void setScenarioIndex(){
		System.out.println("Setting scenario index...");
		try {
			stmt = conn.createStatement();
			String sql = "CREATE TABLE IF NOT EXISTS "+scenarioTableName + " (ID Integer NOT NULL, Table_Name VarChar(40), Scenario VarChar(80), Part_A VarChar(20), Part_F VarChar(30), PRIMARY KEY(ID))";
			stmt.executeUpdate(sql);
			sql="select ID FROM "+scenarioTableName+" WHERE TABLE_NAME='"+tableName+"' AND SCENARIO='"+scenarioName+"' AND PART_A='"+ControlData.partA+"' AND PART_F='"+ControlData.svDvPartF+"'";
			ResultSet rs1 = stmt.executeQuery(sql);
			if (!rs1.next()){
				rs1.close();
				sql="select count(*) AS rowcount from "+scenarioTableName;
				ResultSet rs2 = stmt.executeQuery(sql);
				rs2.next();
				int count=rs2.getInt("rowcount");
				rs2.close();
				if (count==0){
					scenarioIndex=0;
				}else{
					sql="select Max(ID) as maxIndex from "+scenarioTableName;
					ResultSet rs3 = stmt.executeQuery(sql);
					rs3.next();
					int maxIndex=rs3.getInt("maxIndex");
					rs3.close();
					scenarioIndex=maxIndex+1;
				}
				sql="INSERT into "+scenarioTableName+" VALUES ("+scenarioIndex+", '" + tableName + "', '"+scenarioName+"', '"+ControlData.partA+"', '"+ControlData.svDvPartF+"')";
				stmt.executeUpdate(sql);
			}else{
				scenarioIndex=rs1.getInt("ID");
				rs1.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Set scenario index");
	}
	
	public void createTable(){
		System.out.println("Creating table in given database...");
		try {
			stmt = conn.createStatement();
			String sql = "CREATE TABLE IF NOT EXISTS "+tableName + " (ID int, Timestep VarChar(8), Units VarChar(20), Date DATE, Variable VarChar(40), Kind VarChar(30), Value Double)";
			stmt.executeUpdate(sql);
			System.out.println("Created table in given database");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void deleteOldData(){
		try {
			System.out.println("Deleting old data in table...");
			Statement statement = conn.createStatement();
			String sql="DELETE FROM "+tableName+" WHERE ID="+scenarioIndex;
			stmt.executeUpdate(sql);
			System.out.println("Deleted old data in table");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void writeData(){
		try {
			System.out.println("Writing output in table...");
			Statement statement = conn.createStatement();
			Set<String> keys = DataTimeSeries.dvAliasTS.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()){
				String name=it.next();
				if (!name.startsWith(slackPrefix) && !name.startsWith(surplusPrefix)){
					DssDataSetFixLength ts = DataTimeSeries.dvAliasTS.get(name);
					String timestep=ts.getTimeStep().toUpperCase();
					if (timestep.equals("1DAY")){
						String sql="INSERT into "+tableName+" VALUES ";
						Date date = ts.getStartTime();
						String unitsName=formUnitsName(ts);
						String variableName=formVariableName(name);
						String kindName=formKindName(ts.getKind());
						double[] data = ts.getData();
						for (int i=0; i<data.length; i++){
							sql=sql +"("+ scenarioIndex+", '1DAY', '"+unitsName+"', '"+formDateData(date)+"', '"+variableName+"', '"+kindName+"', "+data[i]+"), ";
							date=addOneDay(date);
						}
						if (sql.endsWith(", ")){
							sql=sql.substring(0, sql.lastIndexOf(","));
						}
						stmt.executeUpdate(sql);
					}else{
						String sql="INSERT into "+tableName+" VALUES ";
						Date date = ts.getStartTime();
						String unitsName=formUnitsName(ts);
						String variableName=formVariableName(name);
						String kindName=formKindName(ts.getKind());
						double[] data = ts.getData();
						for (int i=0; i<data.length; i++){
							sql=sql +"("+ scenarioIndex+", '1MON', '"+unitsName+"', '"+formDateData(date)+"', '"+variableName+"', '"+kindName+"', "+data[i]+"), ";
							date=addOneMonth(date);
						}
						if (sql.endsWith(", ")){
							sql=sql.substring(0, sql.lastIndexOf(","));
						}
						stmt.executeUpdate(sql);
					}
				}
			}
			System.out.println("Wrote output in table");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public String formUnitsName(DssDataSetFixLength ts){
		String units=ts.getUnits().replaceAll("/", "_").replaceAll("-", "_");
		return units;
	}
	
	public String formVariableName(String name){
		String variableName = DssOperation.getTSName(name).replaceAll("-", "_");
		return variableName;
	}
	
	public String formKindName(String name){
		String kindName = name.replaceAll("-", "_");
		return kindName;
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
