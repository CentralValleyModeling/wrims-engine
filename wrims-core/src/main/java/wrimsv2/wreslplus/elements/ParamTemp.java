package wrimsv2.wreslplus.elements;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;



public class ParamTemp implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public String fromWresl; // for test only
	public int line=1;
	public String id;
	public Set<String> dependants;
	public Set<String> dependants_unknown;

	public String expression;
		
	
	public ParamTemp(){
		
		dependants = new LinkedHashSet<String>();
		expression = "";

	}
	
}
	
