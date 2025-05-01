package wrimsv2.wreslplus.elements;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import wrimsv2.commondata.wresldata.Param;



public class ExternalTemp implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public String fromWresl; // for test only
	public int line=1;
	public String id;
	public String fileName;
	
	public ExternalTemp(){
		
		fileName=Param.undefined; 
		fromWresl=Param.undefined;

	}
	
}
	
