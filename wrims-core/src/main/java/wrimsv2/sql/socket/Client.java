package wrimsv2.sql.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);
	private Socket socket = null;
	private ObjectOutputStream outputStream = null;
	private boolean isConnected = false;
	private String sourceFilePath;
	private String destinationPath="G:\\tempCSV\\";
	private FileEvent fileEvent = null;
	private int port = 13267;
	private String message="";

	public Client() {
		
	}

	public void connect(String host, String sourceFilePath) {
		this.sourceFilePath=sourceFilePath;
		while (!isConnected) {
			try {
				socket = new Socket(host, port);
				outputStream = new ObjectOutputStream(socket.getOutputStream());
				isConnected = true;
			} catch (IOException e) {
				e.printStackTrace();
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		logger.info("Server Client Connected");
	}

	public boolean sendFile() {
		fileEvent = new FileEvent();
		String fileName = sourceFilePath.substring(sourceFilePath.lastIndexOf("\\") + 1, sourceFilePath.length());
		String path = sourceFilePath.substring(0, sourceFilePath.lastIndexOf("\\") + 1);
		fileEvent.setDestinationDirectory(destinationPath);
		fileEvent.setFilename(fileName);
		fileEvent.setSourceDirectory(sourceFilePath);
		File file = new File(sourceFilePath);
		if (file.isFile()) {
			try {
				DataInputStream diStream = new DataInputStream(new FileInputStream(file));
				long len = (int) file.length();
				byte[] fileBytes = new byte[(int) len];
				int read = 0;
				int numRead = 0;
				while (read < fileBytes.length && (numRead = diStream.read(fileBytes, read, fileBytes.length - read)) >= 0) {
					read = read + numRead;
				}
				fileEvent.setFileSize(len);
				fileEvent.setFileData(fileBytes);
				fileEvent.setStatus("Success");
			} catch (Exception e) {
				e.printStackTrace();
				fileEvent.setStatus("Error");
				closeSocket();
				return false;
			}
		} else {
			logger.error("path specified is not pointing to a file");
			fileEvent.setStatus("Error");
			closeSocket();
			return false;
		}

		try {
			outputStream.writeObject(fileEvent);
			logger.info("CSV Committing Done");
			Thread.sleep(3000);
			
		    InputStream is = socket.getInputStream();
		    InputStreamReader isr = new InputStreamReader(is);
		    BufferedReader br = new BufferedReader(isr);
		    message = br.readLine();
		    br.close();
		    isr.close();
		    is.close();
		    logger.info("CSV Downloading Done");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		closeSocket();
		if (message.equals("CSV Transferred")){
			return true;
	    }else{
	    	return false;
	    }
	}
	
	public void closeSocket(){
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}