package wrimsv2.commondata.wresldata;

import java.io.Serializable;


public class External implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public String scope;
	public String type;
	public String fromWresl;
	public int line=1;
	
	public External(){
		scope=Param.undefined;
		type=Param.undefined;
		fromWresl=Param.undefined;

	}
}
	
