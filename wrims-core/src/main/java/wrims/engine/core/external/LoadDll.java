package wrims.engine.core.external;

public class LoadDll {
	public LoadDll(String dllName){
		//System.load(System.getenv("WRIMS_v2_path")+dllName);
		System.loadLibrary(dllName.replace(".dll", ""));
	}
}
