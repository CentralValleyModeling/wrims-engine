package wrimsv2.wreslparser.elements;

import wrimsv2.commondata.wresldata.Param;

public class IncludeFile {
	
	public String scope;
	public String absFilePath;
	public String fromWresl;
	
	public IncludeFile(){
		scope=Param.undefined;
		fromWresl=Param.undefined;

	}
	
	public String equalEva(){
			
		String temp = scope; 
		
		return temp;
	}

	@Override
	public boolean equals(Object obj)
	{

		if ((obj == null) || (obj.getClass() != this.getClass())) {
			return false;
		}

		else if (((IncludeFile) obj).equalEva() == null) {
			return false;
		}

		else if (this.equalEva() == ((IncludeFile) obj).equalEva()) {
			return true;
		}

		else {
			return false;
		}
	}
}
	
