package wrimsv2.components;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.apache.commons.io.FileUtils;

import wrimsv2.wreslparser.elements.LogUtils;
import wrimsv2.wreslplus.elements.IncFileSimple;
import wrimsv2.wreslplus.elements.LookupTableSimple;
import wrimsv2.wreslplus.elements.ResourceUtils;
import wrimsv2.wreslplus.grammar.IncFileFinderLexer;
import wrimsv2.wreslplus.grammar.IncFileFinderParser;

public class IncFileCollector {
	
	public static LinkedHashSet<String> incFileList_all;
	public static LinkedHashSet<String> lookupTableList_all;
	public static int number_of_errors;
	private static ArrayList<IncFileSimple> tempList ;
	private static String baseDir; // this is the parent of run folder, e.g., baseDir="d:\study" if main wresl is d:\study\run\main.wresl
	private static String runDir;
	private static String mainFilePath;
	private static final String logFileName = "=FileCollector=.log";
	private static String lookupSubDir="";

	
	public IncFileCollector(String mainWreslPath, String targetDir){
		
		new IncFileCollector(mainWreslPath,targetDir,null);
	}
	
	
	public IncFileCollector(String mainWreslPath, String targetDir, String lookupSubDir){
		
		// args[0] is main wresl file absolute path
		// args[1] is optional. If specified, this is the target folder where all the include files will be copied into.
		// args[2] is optional. If specified, this is the subfolder where lookup tables reside.
					
		initialize(mainWreslPath);
		collect();
		
		// if the target folder (second argument) is specified, 
		// then the inc files will be copy to the target folder.
		
		if (targetDir!=null && targetDir.trim().length()>0 ) {
			
			if (lookupSubDir!=null && lookupSubDir.trim().length()>0) {
				IncFileCollector.lookupSubDir = lookupSubDir.trim();
			}
			
			copyWreslsAndLookupTablesTo(targetDir.trim());
		}
		
		if (number_of_errors==0) {
			LogUtils.importantMsg("IncFileFinder completed successfully.");
		}
	}

	public static void initialize(String mainFilePath){
		
		number_of_errors = 0;
		IncFileCollector.mainFilePath = mainFilePath;	

		
		try {
			LogUtils.setLogFile(new File(mainFilePath).getParentFile().getCanonicalPath(), logFileName);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//baseDir= new File(mainFilePath).getParentFile().getParent();
		runDir =new File(mainFilePath).getParent();
		
		incFileList_all = new LinkedHashSet<String>();
		incFileList_all.add(mainFilePath);
		
		lookupTableList_all = new LinkedHashSet<String>();
		
	}
	
	public static void collect() {
		
		// call initialize() before using collect()
		
		ArrayList<IncFileSimple> ifs_array = new ArrayList<IncFileSimple>();
		ifs_array.add(new IncFileSimple(mainFilePath));

		searchIncFiles(ifs_array);
		
//		for (String s: lookupTableList_all){
//			System.out.println(s);
//		}
		
	}
		
	public static void searchIncFiles(ArrayList<IncFileSimple> ifs_array){
		
		for (IncFileSimple ifs : ifs_array) {
		
			tempList = parseWresl(ifs);
			
			if (tempList!=null && tempList.size()>0) {	
			
				searchIncFiles(tempList);		
			} 
		}
	}
	
	public static ArrayList<IncFileSimple> parseWresl(IncFileSimple ifs) {
		
		IncFileFinderParser parser = null;
		
		LogUtils.importantMsg("Parsing file: "+ifs.absPath);
			
		try {
			parser= initParser(ifs);
			if (parser !=null){
				parser.wreslFile();
			} else {
				return null;
				//System.exit(1);
			}
			
			for (IncFileSimple fs: parser.incFileSimpleList){
				incFileList_all.add(fs.absPath);
			}
			
			for (LookupTableSimple lts: parser.lookupTableSimpleList){	
				lookupTableList_all.add(lts.tableName);
			}
			
			return parser.incFileSimpleList;
			
		}
		catch (RecognitionException e) {
	
			e.printStackTrace();
		}		
		
		return null;
	}
	
	public static IncFileFinderParser initParser(IncFileSimple ifs) throws RecognitionException  {		
		
		CharStream stream;
		
		try {
			stream = new ANTLRFileStream(ifs.absPath, "UTF8");
		} catch (IOException e) {
			LogUtils.warningMsgLocation(ifs.fromWresl, ifs.line, "File not found: "+ifs.absPath);
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		    
		IncFileFinderLexer lexer = new IncFileFinderLexer(stream);
		
		TokenStream tokenStream = new CommonTokenStream(lexer);		
		
		IncFileFinderParser parser = new IncFileFinderParser(tokenStream);
				
		try {
			parser.currentAbsolutePath = new File(ifs.absPath).getCanonicalPath().toLowerCase();
			parser.currentAbsoluteParent = new File(ifs.absPath).getCanonicalFile().getParent().toLowerCase();
			//String pathRelativeToRunDir = ResourceUtils.getRelativePath(parser.currentAbsoluteParent, GlobalData.runDir, File.separator);			
			
			lexer.currentAbsolutePath = parser.currentAbsolutePath;
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return parser;
		
	}
	
	private static void copyWreslsAndLookupTablesTo(String targetDir) {	
		
		File targetDirF = new File(targetDir);
		
		int idex = runDir.indexOf(":");
		
		File targetLookupDirF = new File(new File(targetDir, runDir.substring(idex+1)), "Lookup" );
		File srcLookupDirF = new File(runDir, "Lookup");
		
		if (lookupSubDir.length()>0) {
			 targetLookupDirF = new File(targetLookupDirF, lookupSubDir);
			 srcLookupDirF = new File(srcLookupDirF, lookupSubDir);
		}
		
		try {
			LogUtils.importantMsg("Wresl files will be copied to: "+ targetDirF.getCanonicalPath());
			LogUtils.importantMsg("Lookup tables will be copied to: "+ targetLookupDirF.getCanonicalPath());
		} catch (IOException e1) {
			targetDirF.mkdirs();
			targetLookupDirF.mkdirs();
		}
		
		for (String s: incFileList_all){
			
			//String relativePath = ResourceUtils.getRelativePath(s, baseDir, File.separator);
			int idx = s.indexOf(":");
			String absPathWithoutDrive = s.substring(idx+1);
			//System.out.println(absPathWithoutDrive);
			
			try {
				File targetPath = new File(targetDir,absPathWithoutDrive);
				FileUtils.copyFile(new File(s), targetPath);
			} catch (IOException e) {
				LogUtils.warningMsg("File not found: "+s);
			}			
		}
		
		// copy lookup tables	
		for (String s: lookupTableList_all){
			
			File targetPath = new File(targetLookupDirF, s+".table");
			File srcPath = new File(srcLookupDirF, s+".table");
			try {
				FileUtils.copyFile(srcPath, targetPath);
			} catch (IOException e) {
				LogUtils.errMsg("Lookup table IO exception, probably not found? "+srcPath);
			}			
		}
		
	}

	public static void main(String[] args){
		
		//args = new String[3];
		//args[0]="D:\\cvwrsm\\trunk\\CalGUI\\Scenarios\\Run_Details\\DEFAULT\\Run\\main.wresl";
		//args[0]="D:\\cvwrsm\\trunk\\calsim30\\calsim30_bo\\conv\\Run\\mainCONV_30_SA.wresl";
		//args[1]="z:\\cs3";             // optional. target folder
		//args[2]="lookupSubFolderName"; // optional. 
				
		new IncFileCollector(args[0],args[1],args[2]);
		
	}

}