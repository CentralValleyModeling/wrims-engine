package gov.ca.water.wrims.engine.core.external;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateCompileFiles {
	public static HashMap<String, String[]> dllFunctions=new HashMap<String, String[]>();
	public static HashMap<String, String> dllDlls = new HashMap<String, String>();
	public static HashMap<String, String> functionTypes=new HashMap<String, String>();
	public static HashMap<String, String[]> functionVariableNames = new HashMap<String, String[]> ();
	public static HashMap<String, String[]> functionVariableTypes = new HashMap<String, String[]> ();
	private static ArrayList<String> functionArray=new ArrayList<String>();
	public static ArrayList<String> error=new ArrayList<String>(); 
	public static String workingDir;
	public static boolean setPath=false;
	private static String currentDirectory;
	
	public static void generateBatchFile(){
		String batchFileFullPath=workingDir+"processDll.bat";

		Iterator fvni=functionVariableNames.keySet().iterator();
		Iterator dfi=dllFunctions.keySet().iterator();
		
		try{
			FileWriter batchFile = new FileWriter(batchFileFullPath, false);
			PrintWriter out = new PrintWriter(batchFile);
			if (setPath){
				out.println("set path="+currentDirectory+"/../jdk/bin;"+currentDirectory+"/../mingw/bin;");
			}
			
			while (fvni.hasNext()){
				String functionName=(String)fvni.next();
				if (setPath){
					out.println("javac -cp "+currentDirectory+"/../lib/wrims-engine.jar "+"gov\\ca\\water\\wrims\\engine\\core\\external\\Function"+functionName+".java");
				}else{
					out.println("javac -cp . gov\\ca\\water\\wrims\\engine\\core\\external\\Function"+functionName+".java");
				}
				out.println("javah -jni gov.ca.water.wrims.engine.core.external.Function"+functionName);
				out.println("gcc -c -Wall -D_JNI_IMPLEMENTATION_ -Wl,--kill-at -I\""+currentDirectory+"/../jdk/include\"  -I\""+currentDirectory+"/../jdk/include/win32\" wrims_external_Function"+functionName+".c");
			}
			String compileString="gcc -Wall -D_JNI_IMPLEMENTATION_ -Wl,--kill-at -shared "; 
			while (dfi.hasNext()){
				String dllName=(String)dfi.next();
				String fortranDllName=dllDlls.get(dllName);
				String[] functions=dllFunctions.get(dllName);
				for (int i=0; i<functions.length; i++){
					compileString=compileString+"wrims_external_Function"+functions[i]+".o ";
				}
				compileString=compileString+"-o "+dllName+".dll -L./ "+fortranDllName+".dll";
				out.println(compileString);
			}
			//out.println("Exit");
			out.close();
		} catch (IOException e){
			error.add(e.getMessage());
		}	
	}
	
	public static void generateJavaFile(String functionName){	
		String javaFileFullPath=workingDir+"gov\\ca\\water\\wrims\\engine\\core\\external\\Function"+functionName+".java";
		String functionType=functionTypes.get(functionName);
		String[] variableNames=functionVariableNames.get(functionName);
		String[] variableTypes=functionVariableTypes.get(functionName);
		try{
			FileWriter javaFile = new FileWriter(javaFileFullPath, false);
			PrintWriter out = new PrintWriter(javaFile);
			out.println("package gov.ca.water.wrims.engine.core.external;");
			out.println();
			out.println("import java.util.*;");
			out.println();
			out.println("import gov.ca.water.wrims.engine.core.components.TimeUsage;");
			out.println();
			out.println("public class Function"+functionName+" extends ExternalFunction{");
			out.println("	private final boolean DEBUG = false;");
			out.println("	private static int cpuTime=0;");
			out.println("	private static int nCalls=0;");
			out.println();
			for (int i=0; i<variableNames.length; i++){
				if (variableTypes[i].equals("int*")){
					out.println("	public int "+variableNames[i]+";");
				}else if (variableTypes[i].equals("float*")){
					out.println("	public float "+variableNames[i]+";");
				}else if (variableTypes[i].equals("double*")){
					out.println("	public double "+variableNames[i]+";");
				}
			}
			out.println();	
			out.println("	public Function"+functionName+"(){");
			out.println();		
			out.println("	}");
			out.println();
			out.println("	public void execute(Stack stack) {");
			out.println();
			out.println("		long t1 = Calendar.getInstance().getTimeInMillis();");
			out.println();
			out.println("		//values in reverse order:");
			for (int i=variableNames.length; i>0; i--){
				out.println("		Object param"+i+" = stack.pop();");
			}
			out.println();
			out.println("		//cast params to correct types:");
			for (int i=variableNames.length; i>0; i--){
				if (variableTypes[i-1].equals("String")){
					out.println("		"+variableTypes[i-1] +" "+ variableNames[i-1]+" = param"+i+".toString();");
				}else if (variableTypes[i-1].equals("int[]")){
					String size="size_"+variableNames[i-1];
					String variableArrName=variableNames[i-1]+"_Arr";
					out.println("		Number[] "+variableArrName+" = (Number[])param"+i+";");
					out.println("		int "+size+"="+variableArrName+".length;");
					out.println("		int[] "+variableNames[i-1]+"=new int["+size+"];");
					out.println("		for (int i=0; i<"+size+"; i++){");
					out.println("			"+variableNames[i-1]+"[i]="+variableArrName+"[i].intValue();");
					out.println("		}");
				}else if (variableTypes[i-1].equals("int[][]")){
					String size1="size1_"+variableNames[i-1];
					String size2="size2_"+variableNames[i-1];
					String variableArrName=variableNames[i-1]+"_Arr";
					out.println("		Number[][] "+variableArrName+" = (Number[][])param"+i+";");
					out.println("		int "+size1+"="+variableArrName+".length;");
					out.println("		int "+size2+"="+variableArrName+"[0].length;");
					out.println("		int[][] "+variableNames[i-1]+"=new int["+size1+"]["+size2+"];");
					out.println("		for (int i=0; i<"+size1+"; i++){");
					out.println("			for (int j=0; j<"+size2+"; j++){");
					out.println("				"+variableNames[i-1]+"[i][j]="+variableArrName+"[i][j].intValue();");
					out.println("			}");
					out.println("		}");
				}else if (variableTypes[i-1].equals("int*")){
					out.println("		"+variableNames[i-1]+" = ((Number) param"+i+").intValue();");
				}else if (variableTypes[i-1].equals("float[]")){
					String size="size_"+variableNames[i-1];
					String variableArrName=variableNames[i-1]+"_Arr";
					out.println("		Number[] "+variableArrName+" = (Number[])param"+i+";");
					out.println("		int "+size+"="+variableArrName+".length;");
					out.println("		float[] "+variableNames[i-1]+"=new float["+size+"];");
					out.println("		for (int i=0; i<"+size+"; i++){");
					out.println("			"+variableNames[i-1]+"[i]="+variableArrName+"[i].floatValue();");
					out.println("		}");
				}else if (variableTypes[i-1].equals("float[][]")){
					String size1="size1_"+variableNames[i-1];
					String size2="size2_"+variableNames[i-1];
					String variableArrName=variableNames[i-1]+"_Arr";
					out.println("		Number[][] "+variableArrName+" = (Number[][])param"+i+";");
					out.println("		int "+size1+"="+variableArrName+".length;");
					out.println("		int "+size2+"="+variableArrName+"[0].length;");
					out.println("		float[][] "+variableNames[i-1]+"=new float["+size1+"]["+size2+"];");
					out.println("		for (int i=0; i<"+size1+"; i++){");
					out.println("			for (int j=0; j<"+size2+"; j++){");
					out.println("				"+variableNames[i-1]+"[i][j]="+variableArrName+"[i][j].floatValue();");
					out.println("			}");
					out.println("		}");
				}else if (variableTypes[i-1].equals("float*")){
					out.println("		"+variableNames[i-1]+" = ((Number) param"+i+").floatValue();");
				}else if (variableTypes[i-1].equals("double[]")){
					String size="size_"+variableNames[i-1];
					String variableArrName=variableNames[i-1]+"_Arr";
					out.println("		Number[] "+variableArrName+" = (Number[])param"+i+";");
					out.println("		int "+size+"="+variableArrName+".length;");
					out.println("		double[] "+variableNames[i-1]+"=new double["+size+"];");
					out.println("		for (int i=0; i<"+size+"; i++){");
					out.println("			"+variableNames[i-1]+"[i]="+variableArrName+"[i].doubleValue();");
					out.println("		}");
				}else if (variableTypes[i-1].equals("double[][]")){
					String size1="size1_"+variableNames[i-1];
					String size2="size2_"+variableNames[i-1];
					String variableArrName=variableNames[i-1]+"_Arr";
					out.println("		Number[][] "+variableArrName+" = (Number[][])param"+i+";");
					out.println("		int "+size1+"="+variableArrName+".length;");
					out.println("		int "+size2+"="+variableArrName+"[0].length;");
					out.println("		double[][] "+variableNames[i-1]+"=new double["+size1+"]["+size2+"];");
					out.println("		for (int i=0; i<"+size1+"; i++){");
					out.println("			for (int j=0; j<"+size2+"; j++){");
					out.println("				"+variableNames[i-1]+"[i][j]="+variableArrName+"[i][j].doubleValue();");
					out.println("			}");
					out.println("		}");
				}else if (variableTypes[i-1].equals("double*")){
					out.println("		"+variableNames[i-1]+" = ((Number) param"+i+").doubleValue();");
				}else{
					out.println("		"+variableTypes[i-1] +" "+ variableNames[i-1]+" = ((Number) param"+i+")."+variableTypes[i-1]+"Value();");
				}
			}
			out.println();
			String resultString="";
			if (functionType.equals("void")){
				resultString="		"+functionName+"(";
			}else{
				resultString="		"+functionType+" result = "+functionName+"(";
			}
			for (int i=0; i<variableNames.length-1; i++){
				resultString=resultString+variableNames[i]+", ";
			}
			resultString=resultString+variableNames[variableNames.length-1]+");";
			out.println(resultString);
			out.println();
			out.println("		// push the result on the Stack");
			if (functionType.equals("int")){
				out.println("		stack.push(new Integer(result));");
			}else if (functionType.equals("float")){
				out.println("		stack.push(new Float(result));");
			}else if (functionType.equals("double")){
				out.println("		stack.push(new Double(result));");
			}else if (functionType.equals("void")){
			}else{
				out.println("		stack.push(result);");
			}
			out.println();
			out.println("		long t2 = Calendar.getInstance().getTimeInMillis();");
			out.println("		cpuTime=cpuTime+(int) (t2-t1);");
			out.println("		nCalls++;");
			out.println("		TimeUsage.cpuTimeMap.put(\""+functionName+"\", cpuTime);");
			out.println("		TimeUsage.nCallsMap.put(\""+functionName+"\", nCalls);");
			out.println();
			out.println("	}");
			out.println();
			String nativeString="	public native "+functionType+" "+functionName+"(";
			for (int i=0; i<variableNames.length-1; i++){
				if (variableTypes[i].equals("int*")){
					nativeString=nativeString+"int "+variableNames[i]+", ";
				}else if (variableTypes[i].equals("float*")){
					nativeString=nativeString+"float "+variableNames[i]+", ";
				}else if(variableTypes[i].equals("double*")){
					nativeString=nativeString+"double "+variableNames[i]+", ";
				}else{
					nativeString=nativeString+variableTypes[i]+" "+variableNames[i]+", ";
				}
			}
			if (variableTypes[variableNames.length-1].equals("int*")){
				nativeString=nativeString+"int "+variableNames[variableNames.length-1]+");";
			}else if (variableTypes[variableNames.length-1].equals("float*")){
				nativeString=nativeString+"float "+variableNames[variableNames.length-1]+");";
			}else if(variableTypes[variableNames.length-1].equals("double*")){
				nativeString=nativeString+"double "+variableNames[variableNames.length-1]+");";
			}else{
				nativeString=nativeString+variableTypes[variableTypes.length-1]+" "+variableNames[variableNames.length-1]+");";
			}
			out.println(nativeString);
			out.println("}");
			out.close();
		} catch (IOException e){
			error.add(e.getMessage());
		}		
	}
	
	public static void generateCFile(String functionName){
		String cFileFullPath=workingDir+"wrims_external_Function"+functionName+".c";
		String functionType=functionTypes.get(functionName);
		String[] variableNames=functionVariableNames.get(functionName);
		String[] variableTypes=functionVariableTypes.get(functionName);
		try{
			FileWriter cFile = new FileWriter(cFileFullPath, false);
			PrintWriter out = new PrintWriter(cFile);
			out.println("#include <jni.h>");
			out.println("#include \"wrims_external_Function"+functionName+".h\"");
			out.println();
			String externalString="extern "+functionType+" "+functionName.toUpperCase()+"(";
			String variableType;
			for (int i=0; i<variableNames.length-1; i++){
				if (variableTypes[i].equals("int")){
					variableType="long";
				}else if (variableTypes[i].equals("String")){
					variableType="char";
				}else if (variableTypes[i].startsWith("int[]")){
					variableType="long";
				}else if (variableTypes[i].startsWith("float[]")){
					variableType="float";
				}else if (variableTypes[i].startsWith("double[]")){
					variableType="double";
				}else if (variableTypes[i].equals("int*")){
					variableType="long";
				}else if (variableTypes[i].equals("float*")){
					variableType="float";
				}else if (variableTypes[i].equals("double*")){
					variableType="double";
				}else{
					variableType=variableTypes[i];
				}
				externalString=externalString+variableType+"* "+variableNames[i]+", ";
			}
			if (variableTypes[variableTypes.length-1].equals("int")){
				variableType="long";
			}else if (variableTypes[variableTypes.length-1].equals("String")){
				variableType="char";
			}else if (variableTypes[variableTypes.length-1].startsWith("int[]")){
				variableType="long";
			}else if (variableTypes[variableTypes.length-1].startsWith("float[]")){
				variableType="float";
			}else if (variableTypes[variableTypes.length-1].startsWith("double[]")){
				variableType="double";
			}else if (variableTypes[variableTypes.length-1].equals("int*")){
				variableType="long";
			}else if (variableTypes[variableTypes.length-1].equals("float*")){
				variableType="float";
			}else if (variableTypes[variableTypes.length-1].equals("double*")){
				variableType="double";
			}else{
				variableType=variableTypes[variableTypes.length-1];
			}
			externalString=externalString+variableType+"* "+variableNames[variableNames.length-1]+");";
			out.println(externalString);
			out.println();
			out.println();
			String modifiedFunctionName=functionName.replaceAll("_", "_1");
			String exportString="";
			if (functionType.startsWith("int[]")){
				exportString="JNIEXPORT jintArray JNICALL Java_wrims_external_Function"+modifiedFunctionName+"_"+modifiedFunctionName+"(JNIEnv *env, jobject obj, ";
			}else if (functionType.startsWith("float[]")){
				exportString="JNIEXPORT jfloatArray JNICALL Java_wrims_external_Function"+modifiedFunctionName+"_"+modifiedFunctionName+"(JNIEnv *env, jobject obj, ";
			}else if (functionType.startsWith("double[]")){
				exportString="JNIEXPORT jdoubleArray JNICALL Java_wrims_external_Function"+modifiedFunctionName+"_"+modifiedFunctionName+"(JNIEnv *env, jobject obj, ";
			}else if (functionType.equals("void")){
				exportString="JNIEXPORT void JNICALL Java_wrims_external_Function"+modifiedFunctionName+"_"+modifiedFunctionName+"(JNIEnv *env, jobject obj, ";
			}else{
				exportString="JNIEXPORT j"+functionType+" JNICALL Java_wrims_external_Function"+modifiedFunctionName+"_"+modifiedFunctionName+"(JNIEnv *env, jobject obj, ";
			}
			for (int i=0; i<variableNames.length-1; i++){
				if (variableTypes[i].startsWith("int[]")){
					exportString=exportString+"jintArray "+variableNames[i]+", ";	
				}else if (variableTypes[i].startsWith("float[]")){
					exportString=exportString+"jfloatArray "+variableNames[i]+", ";	
				}else if (variableTypes[i].startsWith("double[]")){
					exportString=exportString+"jdoubleArray "+variableNames[i]+", ";	
				}else if (variableTypes[i].equals("int*")){
					exportString=exportString+"jint "+variableNames[i]+", ";	
				}else if (variableTypes[i].equals("float*")){
					exportString=exportString+"jfloat "+variableNames[i]+", ";	
				}else if (variableTypes[i].equals("double*")){
					exportString=exportString+"jdouble "+variableNames[i]+", ";	
				}else{
					exportString=exportString+"j"+variableTypes[i].toLowerCase()+" "+variableNames[i]+", ";
				}
			}
			if (variableTypes[variableTypes.length-1].startsWith("int[]")){
				exportString=exportString+"jintArray "+variableNames[variableTypes.length-1]+") {";	
			}else if (variableTypes[variableTypes.length-1].startsWith("float[]")){
				exportString=exportString+"jfloatArray "+variableNames[variableTypes.length-1]+") {";	
			}else if (variableTypes[variableTypes.length-1].startsWith("double[]")){
				exportString=exportString+"jdoubleArray "+variableNames[variableTypes.length-1]+") {";	
			}else if (variableTypes[variableTypes.length-1].equals("int*")){
				exportString=exportString+"jint "+variableNames[variableTypes.length-1]+") {";	
			}else if (variableTypes[variableTypes.length-1].equals("float*")){
				exportString=exportString+"jfloat "+variableNames[variableTypes.length-1]+") {";	
			}else if (variableTypes[variableTypes.length-1].equals("double*")){
				exportString=exportString+"jdouble "+variableNames[variableTypes.length-1]+") {";	
			}else{
				exportString=exportString+"j"+variableTypes[variableTypes.length-1].toLowerCase()+" "+variableNames[variableNames.length-1]+") {";
			}
			out.println(exportString);
			out.println();
			for (int i=0; i<variableNames.length; i++){
				if (variableTypes[i].equals("String")){
					out.println("	char* "+variableNames[i]+"Chars = (*env)->GetStringUTFChars(env, "+variableNames[i]+", 0);");
				}else if (variableTypes[i].equals("int[]")){
					out.println("	jint* "+variableNames[i]+"_arr = (*env)->GetIntArrayElements(env, "+variableNames[i]+", 0);");
				}else if (variableTypes[i].equals("float[]")){
					out.println("	jfloat* "+variableNames[i]+"_arr = (*env)->GetFloatArrayElements(env, "+variableNames[i]+", 0);");
				}else if (variableTypes[i].equals("double[]")){
					out.println("	jdouble* "+variableNames[i]+"_arr = (*env)->GetDoubleArrayElements(env, "+variableNames[i]+", 0);");
				}else if (variableTypes[i].equals("int[][]")){
					preCall_2DArray(out, "int", variableNames[i]);
				}else if (variableTypes[i].equals("float[][]")){
					preCall_2DArray(out, "float", variableNames[i]);
				}else if (variableTypes[i].equals("double[][]")){
					preCall_2DArray(out, "double", variableNames[i]);
				}else if (variableTypes[i].equals("double[]")){
					out.println("	jdouble* "+variableNames[i]+"_arr = (*env)->GetDoubleArrayElements(env, "+variableNames[i]+", 0);");
				}
			}
			String resultString="";
			if (functionType.startsWith("int[]")){
				resultString="	jint* result = "+functionName.toUpperCase()+"(";
			}else if (functionType.startsWith("float[]")){
				resultString="	jfloat* result = "+functionName.toUpperCase()+"(";
			}else if (functionType.startsWith("double[]")){
				resultString="	jdouble* result = "+functionName.toUpperCase()+"(";
			}else if (functionType.equals("void")){
				resultString="	"+functionName.toUpperCase()+"(";
			}else{
				resultString="	j"+functionType.toLowerCase()+" result = "+functionName.toUpperCase()+"(";
			}
			
			for (int i=0; i<variableNames.length-1; i++){
				if (variableTypes[i].equals("String")){
					resultString=resultString+variableNames[i]+"Chars, ";
				}else if (variableTypes[i].equals("int[]")){
					resultString=resultString+variableNames[i]+"_arr, ";
				}else if (variableTypes[i].equals("float[]")){
					resultString=resultString+variableNames[i]+"_arr, ";
				}else if (variableTypes[i].equals("double[]")){
					resultString=resultString+variableNames[i]+"_arr, ";
				}else if (variableTypes[i].equals("int[][]")|variableTypes[i].equals("float[][]")|variableTypes[i].equals("double[][]")){
					resultString=resultString+"&"+variableNames[i]+"_arr[0][0], ";
				}else{
					resultString=resultString+"&"+variableNames[i]+", ";
				}
			}
			if (variableTypes[variableNames.length-1].equals("String")){
				resultString=resultString+variableNames[variableNames.length-1]+"Chars);";
			}else if (variableTypes[variableNames.length-1].equals("int[]")){
				resultString=resultString+variableNames[variableNames.length-1]+"_arr);";
			}else if (variableTypes[variableNames.length-1].equals("float[]")){
				resultString=resultString+variableNames[variableNames.length-1]+"_arr);";
			}else if (variableTypes[variableNames.length-1].equals("double[]")){
				resultString=resultString+variableNames[variableNames.length-1]+"_arr);";
			}else if (variableTypes[variableNames.length-1].equals("int[][]")|variableTypes[variableNames.length-1].equals("float[][]")|variableTypes[variableNames.length-1].equals("double[][]")){
				resultString=resultString+"&"+variableNames[variableNames.length-1]+"_arr[0][0]);";
			}else{
				resultString=resultString+"&"+variableNames[variableNames.length-1]+");";
			}
			out.println(resultString);
			out.println();
			
			out.println("jclass clazz = (*env)->GetObjectClass(env, obj);");
			out.println();
			
			for (int i=0; i<variableNames.length; i++){
				if (variableTypes[i].equals("String")){
					out.println("	(*env)->ReleaseStringUTFChars(env, "+variableNames[i]+", "+variableNames[i]+"Chars);");
				}else if (variableTypes[i].equals("int[]")){
					out.println("	(*env)->ReleaseIntArrayElements(env, "+variableNames[i]+", "+variableNames[i]+"_arr, 0);");
				}else if (variableTypes[i].equals("float[]")){
					out.println("	(*env)->ReleaseFloatArrayElements(env, "+variableNames[i]+", "+variableNames[i]+"_arr, 0);");
				}else if (variableTypes[i].equals("double[]")){
					out.println("	(*env)->ReleaseDoubleArrayElements(env, "+variableNames[i]+", "+variableNames[i]+"_arr, 0);");
				}else if (variableTypes[i].equals("int[][]")){
					afterCall_2DArray(out, "int", variableNames[i]);
				}else if (variableTypes[i].equals("float[][]")){
					afterCall_2DArray(out, "float", variableNames[i]);
				}else if (variableTypes[i].equals("double[][]")){
					afterCall_2DArray(out, "double", variableNames[i]);
				}else if (variableTypes[i].equals("int*")){
					afterCallScalar(out, "int", variableNames[i]);
				}else if (variableTypes[i].equals("float*")){
					afterCallScalar(out, "float", variableNames[i]);
				}else if (variableTypes[i].equals("double*")){
					afterCallScalar(out, "double", variableNames[i]);
				}
			}
			String returnString;
			if (functionType.equals("void")){
				returnString ="	return;";
			}else{
				returnString ="	return result;";
			}
			out.println(returnString);
			out.println("}");
			out.close();
		} catch (IOException e){
			error.add(e.getMessage());
		}		
	}
	
	public static boolean setDllFunction(String fileName) {	
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(fileName);
		    DataInputStream in = new DataInputStream(fstream);
		    BufferedReader br = new BufferedReader(new InputStreamReader(in));
		    String nDllFileString;
		    int line=0;
		    
		    int nDllFiles;
		    nDllFileString=br.readLine();
		    line=line+1;
		    if (nDllFileString ==null){
		    	error.add("Line "+line+": Number of interface dll files is not defined." );
		    	return false;
		    }
		    nDllFiles=Integer.parseInt(nDllFileString);
		    
		    for (int i=0; i<nDllFiles; i++){
		    	String interfaceDllString=br.readLine();
		    	line=line+1;
		    	if (interfaceDllString==null){
		    		error.add("Line " +line+": The number of defined interface dlls is less than the number in Line 1. Please define interface dll name.");
		    		return false;
		    	}
		    	interfaceDllString=removeLeadingTailingSpace(interfaceDllString);
		    	if (interfaceDllString.contains("\\s")) {
		    		error.add("Line "+line+": Only one interface dll name could be used in one line.");
		    		return false;
		    	}
		    	if (!isNameFormatRight(interfaceDllString)) {
		    		error.add("Line "+line+": Interface dll name contains non-characters or non-numeric.");
		    		return false;
		    	}
		    	
		    	String fortranDllString=br.readLine();
		    	line=line+1;
		    	if (fortranDllString==null){
		    		error.add("Line " +line+": The number of defined interface dlls is less than the number in Line 1. Please define Fortran dll name.");
		    		return false;
		    	}
		    	fortranDllString=removeLeadingTailingSpace(fortranDllString);
		    	if (fortranDllString.contains("\\s")) {
		    		error.add("Line "+line+": Only one Fortran dll name could be used in one line.");
		    		return false;
		    	}
		    	if (!isNameFormatRight(fortranDllString)) {
		    		error.add("Line "+line+": Fortran dll name contains non-characters or non-numeric.");
		    		return false;
		    	}
		    	
		    	String functionString=br.readLine();
		    	line=line+1;
		    	if (functionString==null){
		    		error.add("Line "+line+": The number of defined interface dlls is less than the number in Line 1. Please define function names");
		    		return false;
		    	}
		    	functionString=removeLeadingTailingSpace(functionString);
		    	String[] function=functionString.split("\\s+");
		    	for (int j=0; j<function.length; j++){
		    		if (!isNameFormatRight(function[j])) {
		    			error.add("Line "+line+": Function name contains non-characters or non-numeric.");
		    			return false;
		    		}
		    		functionArray.add(function[j]);
		    	}
		    	dllFunctions.put(interfaceDllString, function);
		    	dllDlls.put(interfaceDllString, fortranDllString);
		    }
		    
		    br.readLine();
		    line=line+1;
		    int nFunctions=functionArray.size();
		    
		    for (int i=0; i<nFunctions; i++){
		    	String functionName=br.readLine();
		    	line=line+1;
		    	if (functionName==null){
		    		error.add("Line " +line+": The number of defined functions is less than the total number of functions. Please provide the function name.");
		    		return false;
		    	}
		    	functionName=removeLeadingTailingSpace(functionName);
		    	if (functionName.contains("\\s")) {
		    		error.add("Line "+line+": Only one function name could be used in this line.");
		    		return false;
		    	}
		    	if (functionArray.contains(functionName)){
		    		functionArray.remove(functionName);
		    	}else{
		    		error.add("Line "+line+": The defined function name is not found in the dll definition above.");
		    		return false;
		    	}
		    	
		    	String functionType=br.readLine();
		    	line=line+1;
		    	if (functionType==null){
		    		error.add("Line " +line+": The number of defined functions is less than the total number of functions. Please provide the function type.");
		    		return false;
		    	}
		    	functionType=removeLeadingTailingSpace(functionType);
		    	if (functionType.contains("\\s")) {
		    		error.add("Line "+line+": Only one function type could be used in this line.");
		    		return false;
		    	}
		    	
		    	String variableTypeString=br.readLine();
		    	line=line+1;
		    	if (variableTypeString==null){
		    		error.add("Line "+line+": The number of defined functions is less than the total number of functions. Please define variable type");
		    		return false;
		    	}
		    	variableTypeString=removeLeadingTailingSpace(variableTypeString);
		    	String[] variableTypes=variableTypeString.split("\\s+");
		    	/*
		    	for (int j=0; j<variableTypes.length; j++){
		    		if (!isNameFormatRight(variableTypes[j])) {
		    			error.add("Line "+line+": Variable type contains non-characters or non-numeric.");
		    			return false;
		    		}
		    	}
		    	*/
		    	
		    	String variableNameString=br.readLine();
		    	line=line+1;
		    	if (variableNameString==null){
		    		error.add("Line "+line+": The number of defined functions is less than the total number of functions. Please define variable name");
		    		return false;
		    	}
		    	variableNameString=removeLeadingTailingSpace(variableNameString);
		    	String[] variableNames=variableNameString.split("\\s+");
		    	if (variableNames.length!=variableTypes.length){
		    		error.add("Line "+line+": The number of variable names is different from the number of variable types in the same function.");
		    		return false;
		    	}
		    	for (int j=0; j<variableNames.length; j++){
		    		if (!isNameFormatRight(variableNames[j])) {
		    			error.add("Line "+line+": Variable names contains non-characters or non-numeric.");
		    			return false;
		    		}
		    	}
		    	
		    	functionTypes.put(functionName, functionType);
		    	functionVariableTypes.put(functionName, variableTypes);
		    	functionVariableNames.put(functionName, variableNames);
		    }
		    
		} catch (Exception e) {
			error.add(e.getMessage());
			return false;
		}
		return true;
	}
	
	public static String removeLeadingTailingSpace(String text){
	    text=text.replaceAll("^\\s+", "");
	    text=text.replaceAll("\\s+$", "");
	    return text;
	}
	
	public static boolean isNameFormatRight(String name){
		if (Character.isDigit(name.charAt(0))){
			return false;
		}
		Pattern alphaNumberic = Pattern.compile("[A-Za-z0-9_]+(\\[\\])*");
		Matcher m = alphaNumberic.matcher(name);
		return m.matches();	
	}
	
	public static void generateFiles(){
		String strDirectory =workingDir+"gov\\ca\\water\\wrims\\engine\\core\\external\\";
		boolean isDone=(new File(strDirectory)).mkdirs();
		
		Iterator fvni=functionVariableNames.keySet().iterator();
		while (fvni.hasNext()){
			String functionName=(String)fvni.next();
			generateJavaFile(functionName);
			generateCFile(functionName);
		}
		
		generateBatchFile();
	}
	
	public static void reportErrors(){
		String errorFileFullPath=workingDir+"error.log";
		try{
			FileWriter errorFile = new FileWriter(errorFileFullPath, false);
			PrintWriter out = new PrintWriter(errorFile);

			for (int i=0; i<error.size(); i++){
				out.println(error.get(i));
			}
			out.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public static void preCall_2DArray(PrintWriter out, String vt, String vn){
		String r1="r_"+vn;
		String ir1="ir_"+vn;
		String c1="c_"+vn;
		String oneDim1="oneDim_"+vn;
		String oneDim1_0=oneDim1+"_0";
		String elements1="elements_"+vn;
		String jvtArray="j"+vt+"Array";
		String jvt="j"+vt; 
		String arrt=vt;
		if (vt.equals("int")) arrt="long";
		String vn_arr=vn+"_arr";
		String getVtArrayElements;
		if (vt.equals("int")){
			getVtArrayElements="GetIntArrayElements";
		}else if (vt.equals("float")){
			getVtArrayElements="GetFloatArrayElements";
		}else{
			getVtArrayElements="GetDoubleArrayElements";
		}
		
		out.println("	int "+r1+"=(*env)->GetArrayLength(env, "+vn+");");
		out.println("	int "+ir1+";");
		out.println();
		out.println("	"+jvtArray+" "+oneDim1+"["+r1+"];");
		out.println("	"+jvt+" *"+elements1+"["+r1+"];");
		out.println();
		out.println("	"+jvtArray+" "+oneDim1_0+" = ("+jvtArray+") (*env)->GetObjectArrayElement(env, "+vn+", 0);");
		out.println();
		out.println("	int "+c1+"=(*env)->GetArrayLength(env, "+oneDim1_0+");");
		out.println();
		out.println("	"+arrt+" "+vn_arr+"["+c1+"]["+r1+"];");
		out.println();
		out.println("	for ("+ir1+"=0; "+ir1+"<"+r1+"; "+ir1+"++){");
		out.println("		"+oneDim1+"["+ir1+"] = ("+jvtArray+") (*env)->GetObjectArrayElement(env, "+vn+", "+ir1+");");
		out.println("		"+elements1+"["+ir1+"] = (*env)->"+getVtArrayElements+"(env, "+oneDim1+"["+ir1+"], 0);");
		out.println();
		out.println("		int j;");
		out.println("		for (j=0; j<"+c1+"; j++){");
		out.println("			"+vn_arr+"[j]["+ir1+"]="+elements1+"["+ir1+"][j];");
		out.println("		}");
		out.println("	}");
	}
	
	public static void afterCall_2DArray(PrintWriter out, String vt, String vn){
		String r1="r_"+vn;
		String ir1="ir_"+vn;
		String c1="c_"+vn;
		String oneDim1="oneDim_"+vn;
		String oneDim1_0=oneDim1+"_0";
		String elements1="elements_"+vn;
		String jvtArray="j"+vt+"Array";
		String jvt="j"+vt; 
		String arrt=vt;
		String vn_arr=vn+"_arr";
		String releaseVtArrayElements;
		if (vt.equals("int")){
			releaseVtArrayElements="ReleaseIntArrayElements";
		}else if (vt.equals("float")){
			releaseVtArrayElements="ReleaseFloatArrayElements";
		}else{
			releaseVtArrayElements="ReleaseDoubleArrayElements";
		}
		
		out.println("for ("+ir1+"=0; "+ir1+"<"+r1+"; "+ir1+"++){");
		out.println("	int j;");
		out.println("	for (j=0; j<+"+c1+"; j++){");
		out.println("		"+elements1+"["+ir1+"][j]="+vn_arr+"[j]["+ir1+"];");
		out.println("	}");
		out.println();
		out.println("	(*env)->"+releaseVtArrayElements+"(env, "+oneDim1+"["+ir1+"], "+elements1+"["+ir1+"], 0);");
		out.println("	(*env)->DeleteLocalRef(env, "+oneDim1+"["+ir1+"]);");
		out.println("}");
	}
	
	/*
	public static void preCallScalar(PrintWriter out, String vt, String vn){
		String jvt="j"+vt;
		String vn0=vn+"_0";
		String vnArr=vn+"_arr";
		String pvt=vt;
		if (vt.equals("int")) pvt="long";
		String getVtArrayElements;
		if (vt.equals("int")){
			getVtArrayElements="GetIntArrayElements";
		}else if (vt.equals("float")){
			getVtArrayElements="GetFloatArrayElements";
		}else{
			getVtArrayElements="GetDoubleArrayElements";
		}
		
		out.println("	"+jvt+"* "+vn+"_arr = (*env)->"+getVtArrayElements+"(env, "+vn+", 0);");
		out.println("	"+pvt+" "+vn0+"="+vnArr+"[0];");
	}
	*/
	
	public static void afterCallScalar(PrintWriter out, String vt, String vn){
		String vnField=vn+"Field";
		String type="I";
		String setVtField="SetIntField";
		if (vt.equals("int")){
			type="I";
			setVtField="SetIntField";
		}else if (vt.equals("float")){
			type="F";
			setVtField="SetFloatField";
		}else if (vt.equals("double")){
			type="D";
			setVtField="SetDoubleField";
		}
		out.println("	jfieldID "+vnField+" = (*env)->GetFieldID(env, clazz, \""+vn+"\", \""+type+"\");");
		out.println("	(*env)->"+setVtField+"(env, obj, "+vnField+", "+vn+");");
	}
	
	public static void setWorkingDirectory(String fileFullPath){
		int index=fileFullPath.lastIndexOf(File.separator);
		workingDir= fileFullPath.substring(0,index+1);
	}
	
	public static void main(String args[]){
		setWorkingDirectory(args[0]);
		if (args.length>1) {
			setPath=true;
			currentDirectory=args[1];
		}
		if (setDllFunction(args[0])) generateFiles();
		reportErrors();
	}
}
