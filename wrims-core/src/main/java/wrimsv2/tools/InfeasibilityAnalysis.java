package wrimsv2.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.antlr.runtime.RecognitionException;

import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.components.ControlData;
import wrimsv2.components.FilePaths;
import wrimsv2.wreslparser.elements.FileParser;
import wrimsv2.wreslparser.elements.SimulationDataSet;
import wrimsv2.wreslparser.elements.StudyUtils;
import wrimsv2.wreslparser.grammar.WreslTreeWalker;
import wrimsv2.wreslplus.elements.ModelTemp;
import wrimsv2.wreslplus.elements.ParserUtils;
import wrimsv2.wreslplus.elements.StudyTemp;

public class InfeasibilityAnalysis {
	private final static String ifsExt=".ifs";
	private final static String wExt=".wresl";
	private final static String cPref="c: ";
	private final static String fPref="f: ";
	public static LinkedHashSet<String> constraintSetMixedCases = new LinkedHashSet<String>();
	public static LinkedHashSet<String> constraintSet = new LinkedHashSet<String>();
	
	public static void procIfsFile(){
		constraintSetMixedCases = new LinkedHashSet<String>();
		
		String ifsFilePath=StudyUtils.configFilePath+ifsExt;
		File ifsFile = new File(ifsFilePath);
		if (!ifsFile.exists()){
			return;
		}
		
		String folderPath=new File(ifsFilePath).getParentFile().getAbsolutePath().toLowerCase();
		Map<String, ModelDataSet> mdsm = ControlData.currStudyDataSet.getModelDataSetMap();
		ModelDataSet mds = mdsm.get(ControlData.currCycleName);
		try {
			FileInputStream fs = new FileInputStream(ifsFilePath);
			BufferedReader br = new BufferedReader(new InputStreamReader(fs));
		    String line = br.readLine();
		    while (line !=null){
		    	line=line.toLowerCase();
		    	if (!line.equals("")){
		    		if (line.startsWith(cPref)){
		    			constraintSetMixedCases.add(line.substring(3));
		    		}else if (line.startsWith(fPref)){
		    			String path = line.substring(3);
		    			String absPath;
		    			if (new File(path).isAbsolute()){
		    				absPath=path;
		    			}else{
		    				absPath=procRelativePath(path, folderPath);
		    			}
		    			LinkedHashSet<String> fpSet = new LinkedHashSet<String>();
		    			File f = new File(absPath);
		    			if (f.isDirectory()){
		    				fpSet.addAll(procFolderToFiles(absPath, mds));
		    			}else if (f.isFile()){
		    				if (absPath.endsWith(wExt) && isStudyInclFile(absPath, mds)){
		    					fpSet.add(absPath);
		    				}
		    			}
		    			procIfsSelFiles(fpSet);
		    		}
		    	}
		    	line = br.readLine();
		    }
		    br.close();
		    fs.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		convertToLowerCase();
		return;
	}
	
	public static void procIfsSelFiles(LinkedHashSet<String> fpSet){
		
		Iterator<String> it = fpSet.iterator();
		while (it.hasNext()){
			String fp = it.next();
			if (fp.equalsIgnoreCase(FilePaths.fullMainPath) && StudyUtils.useWreslPlus){
				StudyTemp studyTemp = ParserUtils.parseWreslMain(fp);
				Map<String, ModelTemp> modelMap = studyTemp.modelMap;
				ArrayList<String> modelList = studyTemp.modelList;
				for (String model: modelList){
					ModelTemp modelTemp = modelMap.get(model);
					constraintSetMixedCases.addAll(modelTemp.glList);
				}
			}else if (StudyUtils.useWreslPlus){
				ModelTemp modelTemp = ParserUtils.parseWreslFile(fp);	
				constraintSetMixedCases.addAll(modelTemp.glList);
			}else{
				WreslTreeWalker walker;
				try {
					walker = FileParser.parseOneFileForDebug(fp);
					SimulationDataSet fileDataSet = walker.thisFileDataSet;	
					constraintSetMixedCases.addAll(fileDataSet.gList);
				} catch (RecognitionException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static String procRelativePath(String path, String folderPath){
		String absPath=folderPath+"\\"+path;
		return absPath;
	}
	
	public static LinkedHashSet<String> procFolderToFiles(String absPath, ModelDataSet mds){
		LinkedHashSet<String> subFpSet = new LinkedHashSet<String>();
		File[] files = new File(absPath).listFiles(); 

		if (files !=null){
			for (File file : files) {
				if (file.isFile()) {
					String fp = file.getAbsolutePath().toLowerCase();
					if (fp.endsWith(wExt) && isStudyInclFile(fp, mds)){
						subFpSet.add(fp);
					}
				}else if (file.isDirectory()){
					String fp = file.getAbsolutePath().toLowerCase();
					subFpSet.addAll(procFolderToFiles(fp, mds));
				}
			}
		}
		return subFpSet;
	}
	
	public static boolean isStudyInclFile(String fp, ModelDataSet mds){
		boolean isIncluded=false;	
		ArrayList<String> ifl = mds.incFileList;
		for (int i=0; i<ifl.size(); i++){
			String ifp = ifl.get(i).toLowerCase();
			if (fp.equals(ifp)){
				isIncluded=true;
			}
		}
		return isIncluded;
	}
	
	public static void convertToLowerCase(){
		constraintSet = new LinkedHashSet<String>();
		Iterator<String> it = constraintSetMixedCases.iterator();
		while (it.hasNext()){
			constraintSet.add(it.next().toLowerCase());
		}
	}
}
