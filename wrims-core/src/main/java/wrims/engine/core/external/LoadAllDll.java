package wrims.engine.core.external;

import java.util.ArrayList;


public class LoadAllDll {
	public LoadAllDll(){
		new LoadDll("interfacetoann.dll");
	}
	
	public LoadAllDll(ArrayList<String> allDll){
		for (int i=0; i<allDll.size(); i++){
			new LoadDll(allDll.get(i));
		}
	}
}
