package wrimsv2.sql;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import wrimsv2.components.ControlData;
import wrimsv2.components.FilePaths;
import wrimsv2.evaluator.CsvOperation;
import wrimsv2.evaluator.DataTimeSeries;
import wrimsv2.evaluator.DssDataSet;
import wrimsv2.evaluator.DssDataSetFixLength;
import wrimsv2.evaluator.DssOperation;
import wrimsv2.evaluator.TimeOperation;
import wrimsv2.sql.socket.Client;

public class SQLServerRWriter{
	
	private String JDBC_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";       
    private String tableName = ControlData.sqlGroup;                 //input 
	private String scenarioTableName="Scenario";
	
	private String URL;
	private String database;
	
	private Connection conn = null;
	private Statement stmt = null;
	
	private String scenarioName=FilePaths.sqlScenarioName;
	private int scenarioIndex;
	
	private String csvRemotePath;
	private String csvLocalPath;
	private String host;
	
	public SQLServerRWriter(){   
		connectToDataBase();
	}
			
	public void process(){
		if (tableName.equalsIgnoreCase(scenarioTableName)){
			tableName=tableName+"_Studies";
		}
		setScenarioIndex();
		createTable();
		createScenarioCSV();
		deleteOldData();
		createCSV();
		if (transferCSV()){
			writeData();
		}
		createIndex();
		close();
	}
	
	public void connectToDataBase(){	
		
		try {
			if (ControlData.databaseURL.contains(";")){
				int index=ControlData.databaseURL.lastIndexOf(";");
				URL=ControlData.databaseURL.substring(0, index);
				database=ControlData.databaseURL.substring(index+14);
			}else{
				URL=ControlData.databaseURL;
				database=ControlData.databaseURL;
			}
			
			Class.forName(JDBC_DRIVER);
			System.out.println("Connecting to a selected database...");
			conn = DriverManager.getConnection(URL, ControlData.USER, ControlData.PASS);
			stmt = conn.createStatement();
			String sql="IF (db_id('"+database+"') IS NULL) CREATE DATABASE "+database;
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
			String sql = "IF (object_id('"+scenarioTableName+"', 'U') IS NULL) CREATE TABLE "+ scenarioTableName + " (ID Integer NOT NULL, Table_Name VarChar(40), Scenario VarChar(80), Part_A VarChar(20), Part_F VarChar(30), PRIMARY KEY(ID))";
			stmt.executeUpdate(sql);
			sql="select ID FROM "+scenarioTableName+" WHERE TABLE_NAME='"+tableName+"' AND SCENARIO='"+scenarioName+"' AND PART_A='"+ControlData.partA+"' AND PART_F='"+ControlData.svDvPartF+"'";
			ResultSet rs1 = stmt.executeQuery(sql);
			if (!rs1.next()){
				rs1.close();
				sql="select count(*) AS rc from "+scenarioTableName;
				ResultSet rs2 = stmt.executeQuery(sql);
				rs2.next();
				int count=rs2.getInt("rc");
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
	
	public void createScenarioCSV(){
		try {
			String sql="select * from "+scenarioTableName;
			ResultSet rs = stmt.executeQuery(sql);
			
			PrintWriter csvWriter = new PrintWriter(new File(FilePaths.dvarDssDirectory+"\\scenario.csv")) ;
		    ResultSetMetaData meta = rs.getMetaData() ; 
		    int numberOfColumns = meta.getColumnCount() ; 
		    String dataHeaders = meta.getColumnName(1) ; 
		    for (int i = 2 ; i < numberOfColumns + 1 ; i ++ ) { 
		    	dataHeaders += "," + meta.getColumnName(i);
		    }
		    csvWriter.println(dataHeaders) ;
		    while (rs.next()) {
		    	String row = rs.getString(1); 
		        for (int i = 2 ; i < numberOfColumns + 1 ; i ++ ) {
		        	row += "," + rs.getString(i);
		        }
		        csvWriter.println(row) ;
		    }
		    csvWriter.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void createTable(){
		System.out.println("Creating table in given database...");
		try {
			stmt = conn.createStatement();
			String sql = "IF (object_id('"+tableName+"', 'U') IS NULL) CREATE TABLE " + tableName + " (ID int, PartA VarChar(40), PartF VarChar(40), Timestep VarChar(8), Units VarChar(20), Date_Time smalldatetime, Variable VarChar(40), Kind VarChar(30), Value Float(8))";
			stmt.executeUpdate(sql);
			sql = "IF EXISTS (SELECT * FROM sys.indexes WHERE name = 'Variable_Index' AND object_id = OBJECT_ID('"+tableName+"')) DROP INDEX Variable_Index ON "+tableName;
			stmt.executeUpdate(sql);
			System.out.println("Created table in given database");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void deleteOldData(){
		try {
			System.out.println("Deleting old data in table...");
			stmt = conn.createStatement();
			String sql="DELETE FROM "+tableName+" WHERE ID="+scenarioIndex;
			stmt.executeUpdate(sql);
			System.out.println("Deleted old data in table");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createCSV(){
		host=URL.toLowerCase().replaceFirst("jdbc:sqlserver://", "");
		host= host.substring(0, host.lastIndexOf(":"));
		int index = FilePaths.fullDvarDssPath.lastIndexOf(".");
		csvLocalPath = FilePaths.fullDvarDssPath.substring(0, index)+".csv";
		csvRemotePath = "G:\\tempCSV\\"+csvLocalPath.substring(csvLocalPath.lastIndexOf("\\") + 1, csvLocalPath.length());;
		
		CsvOperation co = new CsvOperation();
		co.ouputCSV(csvLocalPath, scenarioIndex);
	}
	
	public boolean transferCSV(){
		Client client=new Client();
		client.connect(host, csvLocalPath);
		return client.sendFile();
	}
	
	public void writeData(){
		try {
			System.out.println("Importing output into table...");
			stmt = conn.createStatement();
			String sql = "Bulk INSERT "+tableName+" From '"+csvRemotePath+"' WITH (FIELDTERMINATOR=',', ROWTERMINATOR='\n', FIRSTROW=2)";
			stmt.executeUpdate(sql);
			System.out.println("Imported output into table");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createIndex(){
		try {
			System.out.println("Creating Table Index...");
			stmt = conn.createStatement();
			String sql = "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'Variable_Index' AND object_id = OBJECT_ID('"+tableName+"')) CREATE INDEX Variable_Index ON "+tableName+" (ID, Variable)";
			stmt.executeUpdate(sql);
			System.out.println("Created Table Index");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void close(){
		try {
			stmt.close();
			conn.close();
			//if (csvFile.exists()) csvFile.delete();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}
