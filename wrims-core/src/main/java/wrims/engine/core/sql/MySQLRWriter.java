package wrims.engine.core.sql;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.FilePaths;
import wrims.engine.core.evaluator.CsvOperation;

public class MySQLRWriter{
	
	private String JDBC_DRIVER = "com.mysql.jdbc.Driver";       
    private String tableName = ControlData.sqlGroup;                 //input 
	private String scenarioTableName="Scenario";
	
	private String URL;
	private String database;
	
	private Connection conn = null;
	private Statement stmt = null;
	
	private String scenarioName=FilePaths.sqlScenarioName;
	private int scenarioIndex;
	
	private String csvPath;
	private String csvMySQLPath;
	private File csvFile;
	
	public MySQLRWriter(){   
		connectToDataBase();
	}
			
	public void process(){
		if (tableName.equalsIgnoreCase(scenarioTableName)){
			tableName=tableName+"_Studies";
		}
		setScenarioIndex();
		createScenarioCSV();
		createTable();
		deleteOldData();
		createCSV();
		writeData();
		createIndex();
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
			String sql = "CREATE TABLE IF NOT EXISTS "+tableName + " (ID int, PartA VarChar(40), PartF VarChar(40), Timestep VarChar(8), Units VarChar(20), Date_Time DATE, Variable VarChar(40), Kind VarChar(30), Value Double)";
			stmt.executeUpdate(sql);
			
			sql = "SELECT COUNT(*) as rowcount FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema = '"+database+"' AND table_name='"+tableName+"' AND index_name = 'Variable_Index'"; 
			ResultSet rs = stmt.executeQuery(sql);
			rs.next();
			int count=rs.getInt("rowcount");
			rs.close();
			if (count>0){
				sql="DROP INDEX Variable_Index ON "+tableName;
				stmt.executeUpdate(sql);
			}
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
	
	public void createCSV(){
		String host=URL.toLowerCase().replaceFirst("jdbc:mysql://", "");
		host="\\\\"+host.substring(0, host.lastIndexOf(":"));
		int index = FilePaths.fullDvarDssPath.lastIndexOf(".");
		csvPath = FilePaths.fullDvarDssPath.substring(0, index)+".csv";
		csvMySQLPath = csvPath.replace("\\", "\\\\");
		
		CsvOperation co = new CsvOperation();
		co.ouputCSV(csvPath, scenarioIndex);
	}
	
	public void writeData(){
		try {
			System.out.println("Importing output into table...");
			stmt = conn.createStatement();
			String sql = "LOAD DATA LOCAL INFILE '"+csvMySQLPath+"' INTO TABLE " + tableName + " CHARACTER SET UTF8 FIELDS TERMINATED BY ',' ENCLOSED BY '\"' LINES TERMINATED BY '\n' IGNORE 1 LINES";
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
			String sql = "SELECT COUNT(*) as rowcount FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema = '"+database+"' AND table_name='"+tableName+"' AND index_name = 'Variable_Index'"; 
			ResultSet rs = stmt.executeQuery(sql);
			rs.next();
			int count=rs.getInt("rowcount");
			rs.close();
			if (count==0){
				sql="CREATE INDEX Variable_Index ON "+tableName+" (ID, Variable)";
				stmt.executeUpdate(sql);
			}
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
