package wrims.engine.core.sql.socket;

import java.io.Serializable;

public class FileEvent implements Serializable {

	public FileEvent() {
	}

	private static final long serialVersionUID = 1L;

	private String destinationDirectory;
	private String sourceDirectory;
	private String filename;
	private long fileSize;
	private byte[] fileData;
	private String status;

	public String getDestinationDirectory() {
		return destinationDirectory;
	}

	public void setDestinationDirectory(String destinationDirectory) {
		this.destinationDirectory = destinationDirectory;
	}

	public String getSourceDirectory() {
		return sourceDirectory;
	}

	public void setSourceDirectory(String sourceDirectory) {
		this.sourceDirectory = sourceDirectory;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public byte[] getFileData() {
		return fileData;
	}

	public void setFileData(byte[] fileData) {
		this.fileData = fileData;
	}
	
	public void resetFileData(){
		fileData=new byte[1];
	}
}

