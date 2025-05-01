package wrimsv2.wreslplus.elements.procedures;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import wrimsv2.wreslparser.elements.LogUtils;
import wrimsv2.wreslplus.elements.GlobalData;
import wrimsv2.wreslplus.elements.IncFileTemp;
import wrimsv2.wreslplus.elements.ModelTemp;
import wrimsv2.wreslplus.elements.ResourceUtils;
import wrimsv2.wreslplus.elements.StudyTemp;

public class ProcIncFile {

	private ProcIncFile() {
	}

	// for main file only
	public static void processPath(ModelTemp m, StudyTemp s) {

		m.pathRelativeToRunDir = ResourceUtils.getRelativePath(m.absPath, GlobalData.runDir, File.separator);

		for (String key : m.incFileMap.keySet()) {

			IncFileTemp f = m.incFileMap.get(key);

			if (f==null) {
				// key is model

				m.incFileRelativePathList.addAll(s.modelMap.get(key).incFileRelativePathList);
				m.incFileAbsPathList.addAll(s.modelMap.get(key).incFileAbsPathList);
				
				
			} else {

				try {
					f.absPath = new File(m.parentAbsPath, f.rawPath)
							.getCanonicalPath().toLowerCase();
					m.incFileAbsPathList.add(f.absPath);
				} catch (IOException e) {
					e.printStackTrace();
					LogUtils.errMsg("Include file IOException: " + f.rawPath,
							m.absPath);
				}

				f.pathRelativeToRunDir = ResourceUtils.getRelativePath(
						f.absPath, GlobalData.runDir, File.separator);
				m.incFileRelativePathList.add(f.pathRelativeToRunDir);

			}
			

		}
		
		m.incFileAbsPathList_post = new ArrayList<String>(m.incFileAbsPathList);
		m.incFileRelativePathList_post = new ArrayList<String>(m.incFileRelativePathList);

	}
	
	public static void processPath(StudyTemp s) {

		
		for (HashSet<String> modSet : s.fileGroupOrder_incModel ){
			
			for (String modName: modSet ){
				
				ModelTemp mObj = s.modelMap.get(modName);
				
				processPath(mObj, s);
				
			}
			
		}	
	}
	
	// for files other than main file
	public static void processPath(ModelTemp m) {

		m.pathRelativeToRunDir = ResourceUtils.getRelativePath(m.absPath, GlobalData.runDir, File.separator);

		for (String key : m.incFileMap.keySet()) {

			IncFileTemp f = m.incFileMap.get(key);

			if (f==null) {
				// key is model
				
				// won't happen because non-main file cannot include models
				
				
			} else {

				try {
					f.absPath = new File(m.parentAbsPath, f.rawPath)
							.getCanonicalPath().toLowerCase();
					m.incFileAbsPathList.add(f.absPath);
				} catch (IOException e) {
					e.printStackTrace();
					LogUtils.errMsg("Include file IOException: " + f.rawPath,
							m.absPath);
				}

				f.pathRelativeToRunDir = ResourceUtils.getRelativePath(
						f.absPath, GlobalData.runDir, File.separator);
				m.incFileRelativePathList.add(f.pathRelativeToRunDir);

			}
			

		}
		
		m.incFileAbsPathList_post = new ArrayList<String>(m.incFileAbsPathList);
		m.incFileRelativePathList_post = new ArrayList<String>(m.incFileRelativePathList);

	}
	
	
}
