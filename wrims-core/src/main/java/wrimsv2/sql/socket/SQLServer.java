package wrimsv2.sql.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class SQLServer {
    private static final Logger logger = LoggerFactory.getLogger(SQLServer.class);
	private ServerSocket serverSocket = null;
	private Socket socket = null;
	private ObjectInputStream inputStream = null;
	private FileEvent fileEvent;
	private File dstFile = null;
	private FileOutputStream fileOutputStream = null;
	private String returnMessage = "CSV Transferred";
	
	public int port=13267;

	public SQLServer() {

	}

	/**
	* Accepts socket connection
	*/
	public void process() {
		try {
			serverSocket = new ServerSocket(port);
			while (true){
				logger.info("waiting...");
				socket = serverSocket.accept();
				inputStream = new ObjectInputStream(socket.getInputStream());
				downloadFile();
			}
		} catch (IOException e) {
			e.printStackTrace();
			if (socket != null){
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			if (serverSocket != null){
				try {
					serverSocket.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
			newServer();
		}
	}
	

	public void downloadFile() {
		try {
			fileEvent = (FileEvent) inputStream.readObject();
			if (fileEvent.getStatus().equalsIgnoreCase("Error")) {
				logger.error("Error occurred");
			}
			String outputFile = fileEvent.getDestinationDirectory() + fileEvent.getFilename();
			if (!new File(fileEvent.getDestinationDirectory()).exists()) {
				new File(fileEvent.getDestinationDirectory()).mkdirs();
			}
			dstFile = new File(outputFile);
			if (dstFile.exists()) dstFile.delete();
			dstFile.createNewFile();
			fileOutputStream = new FileOutputStream(dstFile);
			fileOutputStream.write(fileEvent.getFileData());
			fileOutputStream.flush();
			fileOutputStream.close();
			fileEvent.resetFileData();
			Thread.sleep(3000);
			
			//Sending the response back to the client.
	        OutputStream os = socket.getOutputStream();
	        OutputStreamWriter osw = new OutputStreamWriter(os);
	        BufferedWriter bw = new BufferedWriter(osw);
	        bw.write(returnMessage);
	        logger.info("Message sent to the client is "+returnMessage);
	        bw.flush();
	        bw.close();
	        osw.close();
	        os.close();
	        
	        System.gc();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}
	
	public void newServer(){
		SQLServer sqlServer = new SQLServer();
		sqlServer.process();
	}
	
	public static void main (String [] args ) {
		SQLServer sqlServer = new SQLServer();
		sqlServer.process();
	}
}
