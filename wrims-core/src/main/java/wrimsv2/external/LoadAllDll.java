package wrimsv2.external;

import java.nio.file.Path;
import java.util.List;

public class LoadAllDll {
	public LoadAllDll(){
		new LoadDll("interfacetoann.dll");
	}

	public LoadAllDll(List<?> allDll){
		for (Object dllLocation : allDll){
			switch (dllLocation) {
				case String s -> new LoadDll(s);
				case Path p -> new LoadDll(p);
				default -> throw new IllegalArgumentException("Unsupported type: " + dllLocation.getClass());
			}
		}
	}
}
