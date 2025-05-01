package wrimsv2.wreslplus.elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;

import wrimsv2.components.ControlData;
import wrimsv2.wreslparser.elements.LogUtils;
import wrimsv2.wreslplus.elements.procedures.ErrorCheck;
import wrimsv2.wreslplus.elements.procedures.ProcIfIncItemGroup;
import wrimsv2.wreslplus.elements.procedures.ProcIncFile;
import wrimsv2.wreslplus.elements.procedures.ProcVarIncFileList;
import wrimsv2.wreslplus.elements.procedures.ToLowerCase;
import wrimsv2.wreslplus.grammar.WreslPlusLexer;
import wrimsv2.wreslplus.grammar.WreslPlusParser;

public class ParserUtils {
	

	
	private ParserUtils(){}


	public static WreslPlusParser initParser(String inputFilePath) throws RecognitionException  {		

		// inputFilePath should be checked and converted to CanonicalPath before this method
		
		CharStream stream;
		
		try {
			stream = new ANTLRFileStream(inputFilePath, "UTF8");
			}
	    catch(Exception e) {
	         //e.printStackTrace();
	         LogUtils.errMsg("File not found: "+ inputFilePath);
	         LogUtils.closeLogFile();
	         return null;
	        }
		    
		WreslPlusLexer lexer = new WreslPlusLexer(stream);
		
		TokenStream tokenStream = new CommonTokenStream(lexer);		
		
		WreslPlusParser parser = new WreslPlusParser(tokenStream);
		
		//parser.currentAbsolutePath = new File(inputFilePath).getAbsolutePath(); 
		//parser.currentAbsoluteParent = new File(inputFilePath).getAbsoluteFile().getParent();
		
		// TODO: lowercase conversion is used to ignore the mismatched include wresl file path and the actual path of the study.
		
		try {
			parser.currentAbsolutePath = new File(inputFilePath).getCanonicalPath().toLowerCase();
			parser.currentAbsoluteParent = new File(inputFilePath).getCanonicalFile().getParent().toLowerCase();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		parser.pathRelativeToRunDir = ResourceUtils.getRelativePath(parser.currentAbsolutePath, GlobalData.runDir, File.separator);
		
		if (ControlData.isParseStudy) LogUtils.importantMsg("Parsing file: "+parser.currentAbsolutePath);
		
		return parser;
		
	}


	public static StudyTemp parseWreslMain(String inputFilePath) {
		
		
		WreslPlusParser parser = null;
		
		try {
			parser = initParser(inputFilePath);
			parser.wreslMain();
		}
		catch (RecognitionException e) {
	
			e.printStackTrace();
		}
		
		parser.styObj.absPath = parser.currentAbsolutePath;
		parser.styObj.relativePath = parser.pathRelativeToRunDir;
		parser.styObj.runDir = parser.currentAbsoluteParent;
		
		return parser.styObj;
		
	}


	public static ModelTemp parseWreslFile(String inputFilePath) {
		
		
		WreslPlusParser parser = null;
		
		try {
			parser = initParser(inputFilePath);
			parser.wreslFile();
			if (parser.number_of_errors>0) return null;
		}
		catch (RecognitionException e) {
	
			e.printStackTrace();
		}
		
		return parser.mObj;
		
	}


	public static void parseAllIncFile(ArrayList<String> relativePathList , StudyTemp st) {
	
			for (String relativePath: relativePathList){
				
				//ModelTemp fm = null;
				
				if (!st.fileModelNameMap.keySet().contains(relativePath)){
	
					String absPath = Tools.getCanonicalLowCasePath(new File(st.runDir, relativePath).getAbsolutePath());
					ModelTemp fm = parseWreslFile(absPath);
					
					if (fm==null) continue;	
	
					ToLowerCase.convert(fm);
					
					ErrorCheck.checkVarRedefined(fm, st);
					
					// check unknown dependants in if statement
					ErrorCheck.checkIfStatementHasUnknownDependants(fm, st.parameterMap.keySet());
					
					// process "if include file group"
					ProcIfIncItemGroup.process(fm);
					
					
					ProcIncFile.processPath(fm);
					ProcVarIncFileList.replaceIncFile(fm,null);
					Procedures.processDependants(fm);
					
//					SerialXml.writeModelTemp(fm, absPath+".x");
//					ModelTemp fm = SerialXml.readModelTemp(absPath+".x");
					
					// TODO: allow multiple models in a file
					String modelName = fm.id.toLowerCase();
					//System.out.println("err: modelName: "+fm.id);
					ArrayList<String> modelNameList = new ArrayList<String>();
					modelNameList.add(modelName);
					
					st.fileModelNameMap.put(relativePath, modelNameList);
					
					st.fileModelDataTable.put(relativePath, modelName, fm);
					
					// parse all included files within files
					parseAllIncFile(fm.incFileRelativePathList, st);
					
				} 				
				
			}
			
		}


	public static void setRunDir(String runDir){
		
		GlobalData.runDir = Tools.getCanonicalLowCasePath(runDir);;
	}


	public static WreslPlusParser initParserSimple(String text) throws RecognitionException  {		
	    
		ANTLRStringStream stream = new ANTLRStringStream(text);		
		WreslPlusLexer lexer = new WreslPlusLexer(stream);
		TokenStream tokenStream = new CommonTokenStream(lexer);		
		WreslPlusParser parser = new WreslPlusParser(tokenStream);
		
		return parser;
		
	}



}
	
