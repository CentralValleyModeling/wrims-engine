package wrimsv2.wreslplus.elements;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.google.common.collect.HashBasedTable;

import wrimsv2.commondata.wresldata.Goal;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.Param;
import wrimsv2.components.ControlData;
import wrimsv2.components.FilePaths;
import wrimsv2.wreslparser.elements.LogUtils;
import wrimsv2.wreslparser.elements.StudyUtils;


public class Tools {
	public static String strip(String s) {
		if (s==null)  return null; 
		return s.substring(1, s.length() - 1);
	}
	
	public static String getCanonicalLowCasePath(String filePath) {
		
		String canonicalPath_lowercase=null;
		
		try {
			
			canonicalPath_lowercase = new File(filePath).getCanonicalPath().toLowerCase(); 
		
		} catch (IOException e) {
			
	        e.printStackTrace();
	        LogUtils.errMsg("IOException: " + filePath);
	    }
		
		return canonicalPath_lowercase;
		
	}
	
	public static HashSet<String> findAllOffspring (String fileName, final Map<String,HashSet<String>> kidMap){
		
		HashSet<String> out = new HashSet<String>();
		
		if (!kidMap.keySet().contains(fileName)) return out;
		
		out.addAll(kidMap.get(fileName));
		
		for (String kid : kidMap.get(fileName)) {

			out.addAll(findAllOffspring(kid, kidMap));
			
		}
		return out;
	}
	
	// TODO: move to Tools
	// be careful, the map is modified in this function
	public static void findFileHierarchy(ArrayList<HashSet<String>> hierarchySetList, Map<String,HashSet<String>> toBeSorted) {
		
		//System.out.println("hierarchySetList"+hierarchySetList);
		
		if (!toBeSorted.isEmpty()) {
					
			HashSet<String> c = new HashSet<String>();
			Set<String> toBeSorted_keySet = new HashSet<String>(toBeSorted.keySet());
			for (String s : toBeSorted_keySet){
				
				//System.out.println("%%% s: "+s);
				
				// TODO: inefficient. needs rewrite
				HashSet<String> ttt = new HashSet<String>();
				for (HashSet<String> ss : hierarchySetList){ ttt.addAll(ss); }
				if ( ttt.containsAll(toBeSorted.get(s))) {
					c.add(s);
					toBeSorted.remove(s);
				}
			}
			hierarchySetList.add(c);
			findFileHierarchy(hierarchySetList, toBeSorted);
		
		}
		
	}

	//TODO: this can be optimized for memory
	public static ArrayList<String> allToUpperCase(ArrayList<String> inArrayList){
		
		ArrayList<String> out = new ArrayList<String>();
		
		for (String s: inArrayList){
			out.add(s.toUpperCase());
		}
		
		return out;
	}
	//TODO: this can be optimized for memory
	public static ArrayList<String> allToLowerCase(ArrayList<String> inArrayList){
		
		ArrayList<String> out = new ArrayList<String>();
		
		for (String s: inArrayList){
			out.add(s.toLowerCase());
		}
		
		return out;
	}
	public static ArrayList<ArrayList<String>> allToLowerCase2(ArrayList<ArrayList<String>> inArrayList){
		
		ArrayList<ArrayList<String>> out = new ArrayList<ArrayList<String>>();
		
		for (ArrayList<String> s: inArrayList){
			out.add(allToLowerCase(s));
		}
		
		return out;
	}
	//TODO: this can be optimized for memory
	public static Map<String,String> allToLowerCase_string(Map<String,String> inMap){
		
		Map<String,String> out = new LinkedHashMap<String, String>();
		
		for (String s: inMap.keySet()){
			out.put(s.toLowerCase(),inMap.get(s).toLowerCase());
		}
		
		return out;
	}
	public static Map<String,HashSet<String>> allToLowerCase_string_set(Map<String,HashSet<String>> inMap){
		
		Map<String,HashSet<String>> out = new LinkedHashMap<String,HashSet<String>>();
		
		for (String s: inMap.keySet()){
			out.put(s.toLowerCase(), (HashSet)allToLowerCase(inMap.get(s)));
		}
		
		return out;
	}
	public static Map<String,Integer> allToLowerCaseString(Map<String,Integer> inMap){
		
		Map<String,Integer> out = new LinkedHashMap<String, Integer>();
		
		for (String s: inMap.keySet()){
			out.put(s.toLowerCase(),inMap.get(s));
		}
		
		return out;
	}
	public static LinkedHashMap<String,WeightSubgroup> allToLowerCase_weightSubgroup(Map<String,WeightSubgroup> inMap){
		
		LinkedHashMap<String,WeightSubgroup> out = new LinkedHashMap<String, WeightSubgroup>();
		
		for (String s: inMap.keySet()){
			out.put(s.toLowerCase(),allToLowerCase(inMap.get(s)));
		}
		
		return out;
	}
	public static WeightSubgroup allToLowerCase(WeightSubgroup wsg){
		
		WeightSubgroup out = new WeightSubgroup();
		
		out.deviationPenalty = wsg.deviationPenalty.toLowerCase();
		out.deviationTolerance = wsg.deviationTolerance.toLowerCase();
		out.id = wsg.id;
		out.varList = allToLowerCase(wsg.varList);
		
		return out;
	}
	public static Set<String> allToLowerCase(Set<String> inSet){
		
		Set<String> out = new LinkedHashSet<String>();
		
		for (String s: inSet){
			out.add(s.toLowerCase());
		}
		
		return out;
	}
	public static String replace_regex(String s) {
		if (s==null)  return null; 
		s=s.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)");
		s=s.replaceAll("\\.", "\\\\.");
		s=s.replaceAll("\\*", "\\\\*");
		s=s.replaceAll("\\|", "\\\\|");
		s=s.replaceAll("\\+", "\\\\+");
		s=s.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]");
		s=s.replaceAll("##", ".+");
		return s;
	}
	public static String remove_nulls(String s) {
		if (s==null)  return null; 
		s=s.replaceAll("null", "");
		s=s.replaceAll("\\s+", " ");
		return s;
	}
	public static ArrayList<String> replace_with_space(ArrayList<String> s) {
		if (s==null)  return null;
		ArrayList<String> o = new ArrayList<String>();
		for (String e : s){
			o.add(replace_with_space(e));
		}
		return o;
	}
	public static String replace_with_space(String s) {
		if (s==null)  return null; 
		s=s.replaceAll("\n+", " ").replaceAll("\r+", " ");
		s=s.replaceAll("\t+", " ");
		s=s.replaceAll("\\s+", " ");
		return s;
	}
	public static ArrayList<String> replace_ignoreChar(ArrayList<String> s) {
		if (s==null) return null;
		ArrayList<String> o = new ArrayList<String>();
		for (String e : s){
			o.add(replace_ignoreChar(e));
		}
		return o;
	}
	public static String replace_ignoreChar(String s) {
		if (s==null)  return null; 
		s=s.replaceAll("\n+", "").replaceAll("\r+", "");
		s=s.replaceAll("\t+", "");
		s=s.replaceAll("\\s+", "");
		return s;
	}
	public static ArrayList<String> add_space_between_logical(ArrayList<String> s) {
		if (s==null)  return s;
		ArrayList<String> o = new ArrayList<String>();
		for (String e : s){
			o.add(add_space_between_logical(e));
		}
		return o;
	}
	public static String add_space_between_logical(String s) {
		if (s==null)  return null; 
		
		//s = replace_ignoreChar(s);
		//s = replace_seperator(s);
		
		s=s.replaceAll("\\.AND\\.", " \\.and\\. ");
		s=s.replaceAll("\\.OR\\.",  " \\.or\\. ");
		s=s.replaceAll("\\.and\\.", " \\.and\\. ");
		s=s.replaceAll("\\.or\\.",  " \\.or\\. ");
		
		return s;
	}
	public static ArrayList<String> replace_seperator(ArrayList<String> s) {
		if (s.size()<1)  return null;
		ArrayList<String> o = new ArrayList<String>();
		for (String e: s){
			o.add(Tools.replace_seperator(e));
		}
		return o;
	}
	public static String replace_seperator(String s) {
		if (s==null)  return null; 
		s=s.replaceAll(Param.arg_seperator,Param.new_seperator);
		return s;
	}
	public static Map<String, String> readFilesFromDirAsMap(String dir)
			throws IOException {
		File folder = new File(dir);
		File[] listOfFiles = folder.listFiles();
		Map<String, String> map = new HashMap<String, String>();

		for (File file : listOfFiles) {

			if (!file.getName().contains(".svn")) {

				String filePath = file.getPath();
				String fileName = file.getName();
				map.put(fileName, readFileAsString(filePath));
			}
		}

		return map;

	}

	public static String readFileAsString(String filePath)  {
		byte[] buffer = new byte[(int) new File(filePath).length()];
		BufferedInputStream f = null;
		try {
			f = new BufferedInputStream(new FileInputStream(filePath));
			f.read(buffer);
		} catch ( IOException e){
			
	         LogUtils.errMsg("File not found: "+ filePath);

	         System.exit(1);
			
		} finally {
			if (f != null)
				try {
					f.close();
				} catch (IOException ignored) {
				}
		}
		return new String(buffer);
	}
	

	public static String readFileAsString(String file, String csName) throws IOException {
		Charset cs = Charset.forName(csName);
		// Thanks to Jon Skeet
		// No real need to close the BufferedReader/InputStreamReader
		// as they're only wrapping the stream
		FileInputStream stream = new FileInputStream(file);
		try {
			Reader reader = new BufferedReader(new InputStreamReader(stream, cs));
			StringBuilder builder = new StringBuilder();
			char[] buffer = new char[8192];
			int read;
			while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
				builder.append(buffer, 0, read);
			}
			return builder.toString();
		}
		finally {
			// Potential issue here: if this throws an IOException,
			// it will mask any others. Normally I'd use a utility
			// method which would log exceptions and swallow them
			stream.close();
		}
	}

	public static String readFileLine(String filePath) throws IOException {

		File input = new File(filePath);
		BufferedReader in = new BufferedReader(new FileReader(input));
		return in.readLine();
	}

	public static PrintWriter openFile(String dirPath, String fileName, boolean isAppend) throws IOException {

		File f = new File(dirPath, fileName);
		File dir = new File(f.getParent());
		dir.mkdirs();
		f.createNewFile();

		return new PrintWriter(new BufferedWriter(new FileWriter(f, isAppend)));

	}

	public static PrintWriter openFile(String dirPath, String fileName) throws IOException {

		return openFile(dirPath, fileName, false);

	}

	public static boolean deleteDir(String dirString) {
		File dir = new File(dirString);
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i])
						.getPath());
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

	public static ArrayList<String> getScopeList(ArrayList<String> fileList, ArrayList<String> localList) {

		ArrayList<String> scopeList = new ArrayList<String>();

		for (String f : fileList) {
			if (localList.contains(f)) {
				scopeList.add("local");
			}
			else {
				scopeList.add("global");
			}
		}
		return scopeList;

	}

	public static Map<String, String> getScopeMap(Set<String> fileSet, Set<String> localSet) {

		Map<String, String> scopeMap = new HashMap<String, String>();

		for (String f : fileSet) {
			if (localSet.contains(f)) {
				scopeMap.put(f,Param.local);
			}
			else {
				scopeMap.put(f,Param.global);
			}
		}
		return scopeMap;

	}
	
	/// type 1 map is the shallow included files, e.g., map( f1, [f7,f9])
	public static Map<String, Set<String>> getReverseMap(Map<String, Set<String>> t1Map) {

		Map<String, Set<String>> out = new HashMap<String, Set<String>>();
		
		for (String f : t1Map.keySet()) {

			for (String c : t1Map.get(f)) {

				if (out.get(c) == null) {
					Set<String> s = new HashSet<String>();
					s.add(f);
					out.put(c, s);
				}
				else {
					out.get(c).add(f);
				}

			}

		}
		return out;
	}	
	
	/// type 1 map is the shallow included files, e.g., map( f1, [f7,f9])
	public static Map<String, ArrayList<String>> getReverseMap_arrayList(Map<String, ArrayList<String>> t1Map) {

		Map<String, ArrayList<String>> out = new HashMap<String, ArrayList<String>>();
		
		for (String f : t1Map.keySet()){
			
			for (String c : t1Map.get(f)){
				
			ArrayList<String> s; 
			if (out.get(c)==null) { s = new ArrayList<String>(); s.add(f);}
			else { s= out.get(c); s.add(f);}
			
			out.put(c, s);
				
			}

		}
		return out;
	}
	
	public static void mapRetainAll (Map<String, ?> map, ArrayList<String> retainKeys){
				
		Set<String> mapKeys = new HashSet<String>(map.keySet());
		Set<String> removeKeys = new HashSet<String>(mapKeys);
		removeKeys.removeAll(retainKeys);
		
		for (String rkey: removeKeys){
			
			map.remove(rkey);	
		}

	}
	
	public static Set<String> mapRemoveAll (Map<String, ?> map, Set<String> set){
		
		Set<String> removedKeys = new LinkedHashSet<String>();
		
		for (String key: set){
			
			if (map.remove(key)!=null) removedKeys.add(key);	
		}
		return removedKeys;
	}	
	public static Set<String> mapRemoveAll (Map<String, ?> map, ArrayList<String> list){
		
		return mapRemoveAll (map, new LinkedHashSet<String>(list));
	}

	public static Set<String> convertStrToSet(String inStr){
		
		Set<String> out = new HashSet<String>();
		StringTokenizer st = new StringTokenizer(inStr);

		while (st.hasMoreTokens()) {
			out.add(st.nextToken());
		}
		out.remove("null");
		
		return out;
	}
	
	public static Set<String> restoreOrder(ArrayList<String> toBeRestored, ArrayList<String> referenceOrder,
			Set<String> member) {

		ArrayList<String> orderedList = new ArrayList<String>(referenceOrder);

		Set<String> nonMember = new HashSet<String>(referenceOrder);
		nonMember.removeAll(member);

		orderedList.removeAll(nonMember);

		toBeRestored = orderedList;

		return new LinkedHashSet<String>(orderedList);
	}
	  
	  public static Set<String> removeDuplicates(ArrayList<String> list)
	  {
	    Set<String> s = new LinkedHashSet<String>(list);

	    ArrayList<String> duplicatesList = new ArrayList<String>(list);

	    for (String x : s) {
	      duplicatesList.remove(x);
	    }

	    list.clear();
	    list.addAll(s);

	    return new LinkedHashSet<String>(duplicatesList);
	  }
	  
	  public static Map<String,Set<String>> getCycleVarMap(Set<String> setVarCycle)
	  {
		  Map<String,Set<String>> out = new HashMap<String,Set<String>>();

		  for (String x : setVarCycle) {
			  
			  int posStart = x.indexOf("[");
			  int posEnd = x.indexOf("]");
			  
			  String varName = x.substring(0,posStart);
			  String cycleName = x.substring(posStart+1, posEnd);

			  if (out.keySet().contains(cycleName)){
		  
				  out.get(cycleName).add(varName);
			  }
			  else{
				  
				  Set<String> setVarName = new HashSet<String>();
				  setVarName.add(varName);
				  out.put(cycleName, setVarName);
			  }
		  }

	    return out;
	  }
		public static void quickLog(String fn, String x, boolean isAppend) {

		    File ilpRootDir = new File(FilePaths.mainDirectory, "=ILP=");  
		    File ilpDir = new File(ilpRootDir, StudyUtils.configFileName); 

			try {
				PrintWriter quickLogFile = Tools.openFile(ilpDir.getAbsolutePath(), fn, isAppend);
				quickLogFile.println(x);
				quickLogFile.close();
			} catch (IOException e) {
				System.err.println("Error: " + e.getMessage());
			}

		}
		
		public static void quickLog(String fn, String x) {
			quickLog(fn, x, false);
		}
		
		public static String noZerofmt(double d)
		{
		    if(d == (long) d)
		        return String.format("%d",(long)d);
		    else
		        return String.format("%s",d);
		}
		
		public static String findGoalLocation(String goalName){
			ModelDataSet mds = ControlData.currModelDataSet;
			String sourceLocation = "";
			if (mds.gMap.containsKey(goalName)){
				Goal goal=mds.gMap.get(goalName);
				sourceLocation="("+goal.fromWresl+":"+goal.line+")";
			}
			return sourceLocation;
		}
}
