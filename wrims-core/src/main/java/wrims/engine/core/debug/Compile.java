package wrims.engine.core.debug;

import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import wrims.engine.core.commondata.wresldata.StudyDataSet;
import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.FilePaths;
import wrims.engine.core.wreslparser.elements.StudyUtils;

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
