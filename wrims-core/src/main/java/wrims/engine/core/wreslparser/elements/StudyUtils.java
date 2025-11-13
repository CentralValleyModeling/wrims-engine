package wrims.engine.core.wreslparser.elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.antlr.runtime.RecognitionException;
import org.apache.commons.io.FilenameUtils;
//import org.codehaus.jackson.JsonGenerationException;
//import org.codehaus.jackson.JsonParseException;
//import org.codehaus.jackson.map.JsonMappingException;
//import org.codehaus.jackson.map.ObjectMapper;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import wrims.engine.core.commondata.wresldata.*;
import wrims.engine.core.components.BuildProps;
import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.FilePaths;
import wrims.engine.core.evaluator.WeightEval;
import wrims.engine.core.solver.mpmodel.MPModel;
import wrims.engine.core.sql.socket.FileEvent;
import wrims.engine.core.tools.Warmstart;
import wrims.engine.core.wreslplus.elements.*;
import wrims.engine.core.wreslplus.elements.procedures.ToWreslData;


public class StudyUtils {

	public static int config_errors = 0;
	public static int total_errors = 0;
	public static boolean loadParserData=false;
	public static boolean compileOnly=false;

	public static String parserDataPath = "";
	public static String configFilePath = "";
	public static String configFileCanonicalPath = "";
	public static String configDir = "";
	public static String configFileName = "default";
	public static boolean useWreslPlus = false;
	public static boolean parserCheckVarUndefined = true;
	public static boolean showCompileLog = false;
	
	private StudyUtils() {

	}
	
	public static void reset() {

		StudyParser.reset();
		total_errors = 0;
		config_errors = 0;
	}

	public static StudyDataSet compileStudy(String inMainWreslPath) throws IOException {
		
		StudyUtils.compileOnly = true;
		StudyDataSet sds = checkStudy(inMainWreslPath);
		WeightEval.procWt(sds);
		return sds;
	}	
	
	public static StudyDataSet checkStudy(String inMainWreslPath) throws IOException {
		
		// TODO: clean up this mess. simplify these checkStudy methods.
		if (FilePaths.mainFile.length()<1) FilePaths.mainFile = FilenameUtils.getName(inMainWreslPath);
		
		String mainFileName = FilenameUtils.removeExtension(FilePaths.mainFile);
		
		String csvFolderName = "=WreslCheck_"+mainFileName+"=";
		String logFileName = "=WreslCheck_"+mainFileName+"=.log";
		
		if (!ControlData.outputWreslCSV) {
			csvFolderName = "";  // disable csv output
		}

		return checkStudy(inMainWreslPath, logFileName, csvFolderName, ControlData.sendAliasToDvar);

	}

	public static StudyDataSet checkStudy(String inMainWreslPath, String csvFolderName, boolean sendAliasToDvar) throws IOException {

		String mainFileName = FilenameUtils.removeExtension(FilePaths.mainFile);
		
		String logFileName = "=WreslCheck_"+mainFileName+"=.log";

		return checkStudy(inMainWreslPath, logFileName, csvFolderName, sendAliasToDvar);

	}
	
	public static StudyDataSet checkStudy(String inMainWreslPath, String logFileName, String csvFolderName,
			boolean sendAliasToDvar) throws IOException {

		StudyDataSet sds = null;

		File mainWreslFile = sanityCheck(inMainWreslPath);

		LogUtils.setLogFile(mainWreslFile.getParentFile().getCanonicalPath(), logFileName);

		LogUtils.titleMsg(Param.wreslChekerName + new BuildProps().getVN());

			
		try {
			
			if (useWreslPlus) {
				sds = parseWreslPlus(mainWreslFile);
				total_errors = StudyParser.total_errors;
			} else {
				sds = parseWresl(mainWreslFile, sendAliasToDvar);
			}
			
		}
		catch (RecognitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (csvFolderName != null && csvFolderName.length() > 0) {
			String csvFolderPath = mainWreslFile.getParentFile() + File.separator + csvFolderName;
			WriteCSV.study(sds, csvFolderPath);
		}

		if (StudyUtils.compileOnly) {
			
			if (StudyUtils.total_errors ==0) {
				StudyUtils.compileObject(FilePaths.fullMainPath, sds);
				//System.exit(0);
			} else {
				LogUtils.errMsg("Compilation unsuccessful.");
				//System.exit(1);
			}
	
		}
		
		LogUtils.closeLogFile();

		if (total_errors==0){
		if (ControlData.useCbcWarmStart || ControlData.cbcCheckIntErr || ControlData.cbc_debug_routeCbc || ControlData.cbc_debug_routeXA){
			if (ControlData.solverName.equalsIgnoreCase("Cbc")  || ControlData.solverName.equalsIgnoreCase("Cbc1")){
				
				Warmstart.collectIntegerDV_2(sds);			
			
			}
		}
		}
		return sds;

	}

	public static void compileObject(String inMainWreslPath, StudyDataSet sds) {

		StringBuilder b = new StringBuilder(inMainWreslPath);
		b.replace(inMainWreslPath.lastIndexOf("."), inMainWreslPath.length(), ".par");
		String objFilePath = b.toString();

		StringBuilder b2 = new StringBuilder(FilePaths.mainFile);
		b2.replace(FilePaths.mainFile.lastIndexOf("."), FilePaths.mainFile.length(), ".par");
		String objFileName = b2.toString();
		
		LogUtils.importantMsg("Writing parser data....");
		writeObj(sds, objFilePath);
		LogUtils.importantMsg("Wresl files are compiled into a binary file: "+objFileName );

	}

	public static StudyDataSet loadObject(String objFilePath) {

		System.out.println("Loading precompiled parser data: "+objFilePath);
		
		return readObj(objFilePath);

	}

	private static File sanityCheck(String inMainWreslPath) throws IOException {

		String m = inMainWreslPath;
		File validatedAbsFile = null;

		if (m.length() < 7) {

		throw new IOException("Invalid wresl file: " + m); }

		String extension = m.substring(m.length() - 5);

		if (!extension.equalsIgnoreCase("wresl")) { throw new IOException("Invalid wresl file: " + m); }

		validatedAbsFile = new File(inMainWreslPath).getAbsoluteFile();

		if (!validatedAbsFile.exists()) { throw new IOException("File not found: " + inMainWreslPath); }

		return validatedAbsFile;

	}

	private static StudyDataSet parseWresl(File validatedMainWreslFile, boolean sendAliasToDvar)
			throws RecognitionException, IOException {

		try {

			TempData td = new TempData();
			StudyParser.reset();
			VarCycleIndex.clearVarCycleIndexList();
			StudyConfig sc = StudyParser.processMainFileIntoStudyConfig(validatedMainWreslFile.getCanonicalPath());

			// td.model_dataset_map=StudyParser.parseModels(sc,td);
			td.model_dataset_map = StudyParser.parseModels(sc, td, false, sendAliasToDvar);

			StudyDataSet sd = StudyParser.writeWreslData(sc, td);			
			
			StudyParser.analyzeVarNeededFromCycles(sc, sd);
			
			total_errors = StudyParser.total_errors;

			return sd;

		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private static StudyDataSet parseWreslPlus(File validatedMainWreslFile)
			throws RecognitionException, IOException {
	
		StudyUtils.reset();
		
		try {
	
			
			//total_errors = StudyParser.total_errors;
			StudyTemp st = null;
			if (showCompileLog) {
				st = Workflow.checkStudy_compileLog(validatedMainWreslFile.getCanonicalPath());
			} else {
				st = Workflow.checkStudy(validatedMainWreslFile.getCanonicalPath());
			}
			
			StudyDataSet sd = ToWreslData.convertStudy(st);
			
			st = null;
			System.gc();
			
			return sd;
	
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	
		return null;
	}

	public static void writeObj(StudyDataSet sds, String objFilePath) {

		Kryo kryo = new Kryo();
		registerClasses(kryo);
		try {
			Output output = new Output(new FileOutputStream(objFilePath));
		    kryo.writeObject(output, sds);
		    output.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			objectMapper.writeValue(new File(objFilePath), sds);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}

	private static StudyDataSet readObj(String objFilePath) {

		StudyDataSet sds=new StudyDataSet();
		Kryo kryo = new Kryo();
		registerClasses(kryo);
		try {
			Input input = new Input(new FileInputStream(objFilePath));
			sds = kryo.readObject(input, StudyDataSet.class);
		    input.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		ObjectMapper objectMapper = new ObjectMapper();
		StudyDataSet sds=new StudyDataSet();
		try {
			sds = objectMapper.readValue(new File(objFilePath), StudyDataSet.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		return sds;

	}
	
	public static void registerClasses(Kryo kryo){
		kryo.register(Alias.class);
		kryo.register(Dvar.class);
		kryo.register(External.class);
		kryo.register(Goal.class);
		kryo.register(ModelDataSet.class);
		kryo.register(StudyDataSet.class);
		kryo.register(Svar.class);
		kryo.register(Timeseries.class);
		kryo.register(WeightElement.class);
		kryo.register(MPModel.class);
		kryo.register(FileEvent.class);
		kryo.register(AliasTemp.class);
		kryo.register(DvarTemp.class);
		kryo.register(ExternalTemp.class);
		kryo.register(GoalCase.class);
		kryo.register(GoalHS.class);
		kryo.register(GoalTemp.class);
		kryo.register(IfIncItemGroup.class);
		kryo.register(IncFileTemp.class);
		kryo.register(ModelTemp.class);
		kryo.register(ParamTemp.class);
		kryo.register(SequenceTemp.class);
		kryo.register(StudyTemp.class);
		kryo.register(SvarTemp.class);
		kryo.register(TimeseriesTemp.class);
		kryo.register(WeightSubgroup.class);
		kryo.register(WeightTable.class);
		kryo.register(WeightTemp.class);
	}
}