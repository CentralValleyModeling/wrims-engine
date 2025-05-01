package wrimsv2.debug;

import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.components.ControlData;
import wrimsv2.components.FilePaths;
import wrimsv2.wreslparser.elements.StudyUtils;

public class Compile {
	public static StudyDataSet checkStudy(String inMainWreslPath, boolean useWreslPlus) throws IOException {
		
		StudyUtils.useWreslPlus=useWreslPlus;
		
		FilePaths.setMainFilePaths(inMainWreslPath);
		
		String mainFileName = FilenameUtils.removeExtension(FilePaths.mainFile);
		
		String csvFolderName = "=WreslCheck_"+mainFileName+"=";
		String logFileName = "=WreslCheck_"+mainFileName+"=.log";
		
		if (!ControlData.outputWreslCSV) {
			csvFolderName = "";  // disable csv output
		}

		return StudyUtils.checkStudy(inMainWreslPath, logFileName, csvFolderName, ControlData.sendAliasToDvar);

	}
}
