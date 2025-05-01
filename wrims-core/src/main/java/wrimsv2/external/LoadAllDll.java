package wrimsv2.external;

import java.util.ArrayList;

import jep.Jep;
import jep.JepException;
import wrimsv2.components.Error;


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
