package wrimsv2.external;

import java.nio.file.Path;

public class LoadDll {
	public LoadDll(String dllName) {
		System.loadLibrary(dllName.replace(".dll", ""));
	}
	public LoadDll(Path dllLocation) {
		System.load(dllLocation.toAbsolutePath().toString());
	}
}
