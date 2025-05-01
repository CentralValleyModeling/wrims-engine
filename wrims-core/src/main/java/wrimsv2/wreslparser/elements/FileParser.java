package wrimsv2.wreslparser.elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;




import wrimsv2.commondata.wresldata.Param;
import wrimsv2.wreslparser.grammar.WreslTreeLexer;
import wrimsv2.wreslparser.grammar.WreslTreeParser;
import wrimsv2.wreslparser.grammar.WreslTreeWalker;

public class FileParser {
	
	private static CharStream stream;	
	public String scope;
	public String kind;
	public String units;
	public String expression;

	
	public FileParser(){
		scope=Param.undefined;
		kind=Param.undefined;
		units=Param.undefined;
		expression=Param.undefined;


	}

	public static WreslTreeWalker parseFile(String inputFilePath) throws RecognitionException{
		
		return parseFile(inputFilePath, false);
	}
	public static WreslTreeWalker parseFile(String inputFilePath, boolean showTree) throws RecognitionException  {		

		
		try {
			stream = new ANTLRFileStream(inputFilePath, "UTF8");
			}
	    catch(Exception e) {
	         //e.printStackTrace();
	         
	         LogUtils.errMsg("File not found: "+ inputFilePath);
	         LogUtils.closeLogFile();
	         return null;
	         //System.exit(1);
	         
	        }
		    
		WreslTreeLexer lexer = new WreslTreeLexer(stream);
		TokenStream tokenStream = new CommonTokenStream(lexer);		

		WreslTreeParser parser = new WreslTreeParser(tokenStream);
		
		parser.currentAbsolutePath = new File(inputFilePath).getAbsolutePath(); 
		parser.currentAbsoluteParent = new File(inputFilePath).getAbsoluteFile().getParent();
		
		LogUtils.importantMsg("Parsing file: "+parser.currentAbsolutePath);
		
		WreslTreeParser.evaluator_return parser_evaluator = parser.evaluator();
		
//		// / check if sequence contains models not defined
//		ArrayList<String> undefined_models = parser.model_in_sequence;
//		parser.model_in_sequence.removeAll(parser.model_list);
//		if (undefined_models.size()>0 ){
//			LogUtils.errMsg("Sequence has undefined models: ", undefined_models);
//		}
		
		// / for debug info
		parser.commonTree = (CommonTree) parser_evaluator.getTree();
		if (showTree) LogUtils.importantMsg("tree = " + parser.commonTree.toStringTree());
		
		// / feed walker with parser's tree output
		CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(parser_evaluator.getTree());
		nodeStream.setTokenStream(tokenStream); // important trick to avoid null exception in tree walker
		WreslTreeWalker walker = new WreslTreeWalker(nodeStream);
		
		walker.commonTree = parser.commonTree;
		walker.currentAbsolutePath = parser.currentAbsolutePath; 
		walker.currentAbsoluteParent = parser.currentAbsoluteParent;

		if (showTree) LogUtils.importantMsg("Walking tree: "+parser.currentAbsolutePath);
		
		walker.evaluator();
		
		return walker;
		
	}
	

	
	public static Map<String,SimulationDataSet> processNestedFile(String inputFilePath) throws RecognitionException, IOException {
		
		SimulationDataSet mainData = parseFile(inputFilePath).thisFileDataSet;

		
		Map<String,SimulationDataSet> out = new HashMap<String, SimulationDataSet>();
		out.put(inputFilePath, mainData);
		
		if (mainData.incFileList.isEmpty()) {return out;}
		else {
			for (String file : mainData.incFileList) {

				Map<String,SimulationDataSet> eachMap = processNestedFile(file);

				out.putAll(eachMap);
			}

			return out;
		}
	}

	public static Map<String,SimulationDataSet> processNestedFileExceptFor(String inputFilePath, Set<String> existingSet) throws RecognitionException, IOException {
		
		//if(existingSet.contains(inputFilePath)) return null;
		
		SimulationDataSet mainData = parseFile(inputFilePath).thisFileDataSet;
		
		Map<String,SimulationDataSet> out = new HashMap<String, SimulationDataSet>();
		out.put(inputFilePath, mainData);
		
		if (mainData.incFileSet.isEmpty()) return out;
		else {
			for (String file : mainData.incFileSet) {
				
				if (existingSet.contains(file)) continue; 
				else {
					Map<String, SimulationDataSet> eachMap = processNestedFile(file);

					out.putAll(eachMap);
				}
			}

			return out;
		}
	}	
	
	public static SimulationDataSet processFileIntoSimulationDataSet(String inputFilePath, String scope) throws RecognitionException, IOException {
		
		WreslTreeWalker parser = parseFile(inputFilePath);
		
		SimulationDataSet out = new SimulationDataSet();
		out = parser.thisFileDataSet;
		
		if (scope == "Local"){
			out =Tools.convertSimulationDataSetToLocal(out);
		}	
		else if (scope == "Global"){ /* Do nothing */ }
		else { LogUtils.errMsg("Wrong scope: "+scope+ " for file: ", inputFilePath);}
			
		
		return out;
	}

	public static Map<String,SimulationDataSet> processNestedFileIntoSimulationDataSetMap(String inputFilePath, String scope) throws RecognitionException, IOException {
		
		SimulationDataSet mainData = processFileIntoSimulationDataSet(inputFilePath, scope);
		
		Map<String,SimulationDataSet> out = new HashMap<String, SimulationDataSet>();
		out.put(inputFilePath, mainData);
		
		if (mainData.incFileList.isEmpty()) {return out;}
		else {
			for (String file : mainData.incFileList) {

				String subscope;
				if (scope == "local") {
					subscope = "local";
				} // main file scope overrides subfile scope
				else {
					subscope = mainData.incFileMap.get(file).scope;
				}

				//SimulationDataSet each = processFileIntoSimulationDataSet(file, subscope);
				Map<String,SimulationDataSet> eachMap = processNestedFileIntoSimulationDataSetMap(file, subscope);

				//out.put(file, each);
				out.putAll(eachMap);
			}

			return out;
		}
	}
	
	public static WreslTreeWalker parseMainFile(String inputFilePath) throws RecognitionException{
		
		return parseMainFile(inputFilePath, false);
	}

	public static WreslTreeWalker parseMainFile(String inputFilePath, boolean showTree) throws RecognitionException  {		
	
		
		try {
			stream = new ANTLRFileStream(inputFilePath, "UTF8");
			}
	    catch(Exception e) {
	         //e.printStackTrace();
	         
	         LogUtils.errMsg("File not found: "+ inputFilePath);
	         LogUtils.closeLogFile();
	         return null;
	         //System.exit(1);
	         
	        }
		    
		WreslTreeLexer lexer = new WreslTreeLexer(stream);
		TokenStream tokenStream = new CommonTokenStream(lexer);		
	
		WreslTreeParser parser = new WreslTreeParser(tokenStream);
		
		parser.currentAbsolutePath = new File(inputFilePath).getAbsolutePath(); 
		parser.currentAbsoluteParent = new File(inputFilePath).getAbsoluteFile().getParent();
		
		LogUtils.importantMsg("Parsing file: "+parser.currentAbsolutePath);
		
		WreslTreeParser.mainFile_return parser_return = parser.mainFile();
		
		// / check if sequence contains models not defined
		ArrayList<String> undefined_models = parser.model_in_sequence;
		parser.model_in_sequence.removeAll(parser.model_list);
		if (undefined_models.size()>0 ){
			LogUtils.errMsg("Sequence has undefined models: ", undefined_models);
		}
		
		// / for debug info
		parser.commonTree = (CommonTree) parser_return.getTree();
		if (showTree) LogUtils.importantMsg("tree = " + parser.commonTree.toStringTree());
		
		// / feed walker with parser's tree output
		CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(parser_return.getTree());
		nodeStream.setTokenStream(tokenStream); // important trick to avoid null exception in tree walker
		WreslTreeWalker walker = new WreslTreeWalker(nodeStream);
		
		walker.commonTree = parser.commonTree;
		walker.currentAbsolutePath = parser.currentAbsolutePath; 
		walker.currentAbsoluteParent = parser.currentAbsoluteParent;
	
		if (showTree) LogUtils.importantMsg("Walking tree: "+parser.currentAbsolutePath);
		
		walker.evaluator();
		
		return walker;
		
	}	

	public static WreslTreeWalker parseOneFileForDebug(String inputFilePath) throws RecognitionException  {		

		
		try {
			stream = new ANTLRFileStream(inputFilePath, "UTF8");
			}
	    catch(Exception e) {
	         //e.printStackTrace();
	         
	         System.out.println("File not found: "+ inputFilePath);
	         return null;
	         //System.exit(1);
	         
	        }
		    
		WreslTreeLexer lexer = new WreslTreeLexer(stream);
		TokenStream tokenStream = new CommonTokenStream(lexer);		

		WreslTreeParser parser = new WreslTreeParser(tokenStream);
		
		parser.currentAbsolutePath = new File(inputFilePath).getAbsolutePath(); 
		parser.currentAbsoluteParent = new File(inputFilePath).getAbsoluteFile().getParent();
		
		//LogUtils.importantMsg("Parsing file: "+parser.currentAbsolutePath);
		
		WreslTreeParser.evaluator_return parser_evaluator = parser.evaluator();
		
//		// / check if sequence contains models not defined
//		ArrayList<String> undefined_models = parser.model_in_sequence;
//		parser.model_in_sequence.removeAll(parser.model_list);
//		if (undefined_models.size()>0 ){
//			LogUtils.errMsg("Sequence has undefined models: ", undefined_models);
//		}
		
		// / for debug info
		parser.commonTree = (CommonTree) parser_evaluator.getTree();
		//if (showTree) LogUtils.importantMsg("tree = " + parser.commonTree.toStringTree());
		
		// / feed walker with parser's tree output
		CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(parser_evaluator.getTree());
		nodeStream.setTokenStream(tokenStream); // important trick to avoid null exception in tree walker
		WreslTreeWalker walker = new WreslTreeWalker(nodeStream);
		
		walker.commonTree = parser.commonTree;
		walker.currentAbsolutePath = parser.currentAbsolutePath; 
		walker.currentAbsoluteParent = parser.currentAbsoluteParent;

		//if (showTree) LogUtils.importantMsg("Walking tree: "+parser.currentAbsolutePath);
		
		walker.evaluator();
		
		return walker;
		
	}
}
	
