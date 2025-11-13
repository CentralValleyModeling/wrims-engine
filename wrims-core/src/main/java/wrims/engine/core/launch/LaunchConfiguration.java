package wrims.engine.core.launch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.FilePaths;
import wrims.engine.core.evaluator.TimeOperation;
import wrims.engine.core.ilp.ILP;
import wrims.engine.core.wreslparser.elements.StudyUtils;

public class LaunchConfiguration {
	
	private HashMap fAttributes=new HashMap();
	
	public static final String ID_WPP_PLUGIN="wrims-ide";
	public static final String ID_WPP_DEBUG_MODEL = "wpp.debugModel";
	public static final String ID_WPP_VARIABLE_VIEW="wpp.variableview";
	public static final String ID_WPP_ALLVARIABLE_VIEW="wpp.allvariableview";
	public static final String ID_WPP_WATCH_VIEW="wpp.watchview";
	public static final String ID_WPP_VARIABLEDETAIL_VIEW="wpp.vardetailview";
	public static final String ID_WPP_VARIABLEMONITOR_VIEW="wpp.varmonitorview";
	public static final String ID_WPP_GOAL_VIEW="wpp.goalview";
	public static final String ID_WPP_ALLGOAL_VIEW="wpp.allgoalview";
	public static final String ID_WPP_FILEINCEXPLORE_VIEW = "wpp.fileincexploreview";
	public static final String ID_WPP_CALSIMHYDRO_VIEW = "wpp.calsimhydroview";
	public static final String ID_WPP_EXCEPTION_VIEW = "wpp.exceptionview";
	public static final String ID_WPP_EDITOR="wpp.editor";
	public static final String ID_WPP_RESUMEMENU="wpp.resume";
	public static final String ID_WPP_TERMINATEMENU="wpp.terminate";
	public static final String ID_WPP_PAUSEMENU="wpp.pause";
	public static final String ID_WPP_SUSPENDMENU="wpp.suspend";
	public static final String ID_WPP_NEXTTIMESTEP="wpp.nexttimestep";
	public static final String ID_WPP_NEXTCYCLE="wpp.nextcycle";
	public static final String ID_WPP_RESIMMENU="wpp.resim";
	public static final String ID_WPP_CONDITIONALBREAKPOINT="wpp.conditionalbreakpoint";
	public static final String ID_WPP_CLEARCONDITIONALBREAKPOINT="wpp.clearconditionalbreakpoint";
	public static final String ID_WPP_SAVETODVFILE="wpp.savedvfile";
	public static final String ID_WPP_SAVETOSVFILE="wpp.savesvfile";
	public static final String ID_WPP_SOLVEROPTIONMENU="wpp.solveroption";
	
	public static final String ID_WPP_PAUSERESUMEBUTTON="wpp.pauseresumebutton";
	public static final String ID_WPP_NEXTCYCLEBUTTON="wpp.nextcyclebutton";
	public static final String ID_WPP_NEXTTIMESTEPBUTTON="wpp.nexttimestepbutton";
	
	public static final String ATTR_WPP_STUDY=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_STUDY";
	public static final String ATTR_WPP_AUTHOR=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_AUTHOR";
	public static final String ATTR_WPP_DATE=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_DATE";
	public static final String ATTR_WPP_DESCRIPTION=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_DESCRIPTION";
	public static final String ATTR_WPP_PROGRAM = ID_WPP_DEBUG_MODEL + ".ATTR_WPP_PROGRAM";
	public static final String ATTR_WPP_DVARFILE = ID_WPP_DEBUG_MODEL + ".ATTR_WPP_DVAR";
	public static final String ATTR_WPP_SVARFILE = ID_WPP_DEBUG_MODEL + ".ATTR_WPP_SVAR";
	public static final String ATTR_WPP_INITFILE = ID_WPP_DEBUG_MODEL + ".ATTR_WPP_INIT";
	public static final String ATTR_WPP_GWDATAFOLDER=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_GWDATA";
	public static final String ATTR_WPP_APART=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_APART";
	public static final String ATTR_WPP_SVFPART=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_SVFPART";
	public static final String ATTR_WPP_INITFPART=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_INITFPART";
	public static final String ATTR_WPP_TIMESTEP=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_TIMESTEP";
	public static final String ATTR_WPP_STARTYEAR=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_STARTYEAR";
	public static final String ATTR_WPP_STARTMONTH=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_STARTMONTH";
	public static final String ATTR_WPP_STARTDAY=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_STARTDAY";
	public static final String ATTR_WPP_ENDYEAR=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_ENDYEAR";
	public static final String ATTR_WPP_ENDMONTH=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_ENDMONTH";
	public static final String ATTR_WPP_ENDDAY=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_ENDDAY";
	public static final String ATTR_WPP_WRESLPLUS=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_WRESLPLUS";
	public static final String ATTR_WPP_FREEXA=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_FREEXA";
	public static final String ATTR_WPP_ALLOWSVTSINIT=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_ALLOWSVTSINIT";
	public static final String ATTR_WPP_LAUNCHTYPE=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_LAUNCHTYPE";
	public static final String ATTR_WPP_MULTISTUDY=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_MULTISTUDY";
	public static final String ATTR_WPP_ISFIXDURATION=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_ISFIXDURATION";
	public static final String ATTR_WPP_FIXEDDURATION=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_MSDURATION";
	public static final String ATTR_WPP_VARIABLEDURATION=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_VARIABLEDURATION";
	public static final String ATTR_WPP_DATATRANSFER=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_DATATRANSFER";
	public static final String ATTR_WPP_PASTARTINTERVAL=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_PASTARTINTERVAL";
	public static final String ATTR_WPP_PADURATION=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_PADURATION";
	public static final String ATTR_WPP_PADELINIT=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_PADELINIT";
	public static final String ATTR_WPP_PARESETDVSTART=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_PARESETDVSTART";
	public static final String ATTR_WPP_PADVSTARTYEAR=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_PADVSTARTYEAR";
	public static final String ATTR_WPP_PADVSTARTMONTH=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_PADVSTARTMONTH";
	public static final String ATTR_WPP_PADVSTARTDAY=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_PADVSTARTDAY";
	public static final String ATTR_WPP_DATABASEURL=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_DATABASEURL";
	public static final String ATTR_WPP_SQLGROUP=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_SQLGROUP";
	public static final String ATTR_WPP_OVOPTION=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_OVOPTION";
	public static final String ATTR_WPP_OVFILE=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_OVFILE";
	public static final String ATTR_WPP_ISSENSITIVITYRUN=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_ISSENSITIVITYRUN";
	public static final String ATTR_WPP_SENSITIVITYINDEXTABLENAME=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_SENSITIVITYINDEXTABLENAME";
	public static final String ATTR_WPP_NUMSENSITIVITYRUN=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_NUMSENSITIVITYRUN";
	public static final String ATTR_WPP_CALSIMHYDRORUN=ID_WPP_DEBUG_MODEL + ".CALSIMHYDRORUN";
	public static final String ATTR_WPP_CALSIMHYDROEXE=ID_WPP_DEBUG_MODEL + ".CALSIMHYDROEXE";
	public static final String ATTR_WPP_ILPLOG=ID_WPP_DEBUG_MODEL+".ILPLOG";
	public static final String ATTR_WPP_ALLRESTARTFILES=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_ALLRESTARTFILES";
	public static final String ATTR_WPP_NUMBERRESTARTFILES=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_NUMBERRESTARTFILES";
	public static final String ATTR_WPP_UNCHANGEGWRESTART=ID_WPP_DEBUG_MODEL + "ATTR_WPP_UNCHANGEGWRESTART";
	public static final String ATTR_WPP_VHECLIB=ID_WPP_DEBUG_MODEL + ".ATTR_WPP_VHECLIB";
	
	//private ILaunchConfigurationType fType;
	
	public LaunchConfiguration(String filePath){
		
		Element root = null;
		
		try {
			InputStream stream = new FileInputStream(filePath);
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			root = parser.parse(new InputSource(stream)).getDocumentElement();
			initializeFromXML(root);
			setConfig(filePath);
			setPref();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
	}
	
	protected void initializeFromXML(Element root) {
		if (!root.getNodeName().equalsIgnoreCase("launchConfiguration")) { //$NON-NLS-1$
			return;
		}
		
		//read type
		String id = root.getAttribute("type"); //$NON-NLS-1$
		if (id == null) {
			return;
		}
		
		/*
		DebugPlugin debugPlugin = DebugPlugin.getDefault();
		if (debugPlugin !=null){
			ILaunchManager launchManager = debugPlugin.getLaunchManager();
			ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(id);
			if (type == null) {
				return;
			}
			setType(type);
		}
		*/
		
		NodeList list = root.getChildNodes();
		int length = list.getLength();
		for (int i = 0; i < length; ++i) {
			Node node = list.item(i);
			short nodeType = node.getNodeType();
			if (nodeType == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (nodeName.equalsIgnoreCase("stringAttribute")) { //$NON-NLS-1$
					setStringAttribute(element);
				} else if (nodeName.equalsIgnoreCase("intAttribute")) { //$NON-NLS-1$
					setIntegerAttribute(element);
				} else if (nodeName.equalsIgnoreCase("booleanAttribute"))  { //$NON-NLS-1$
					setBooleanAttribute(element);
				} else if (nodeName.equalsIgnoreCase("listAttribute")) {   //$NON-NLS-1$
					setListAttribute(element);					
				} else if (nodeName.equalsIgnoreCase("mapAttribute")) {    //$NON-NLS-1$
					setMapAttribute(element);										
				}
			}		
		}
	}	
	
	/*
	protected void setType(ILaunchConfigurationType type) {
		fType = type;
	}
	*/
	
	protected void setAttribute(String key, Object value) {
		if (value == null) {
			getAttributeTable().remove(key);
		} else {
			getAttributeTable().put(key, value);
		}
	}
	
	protected void setStringAttribute(Element element) {
		String key = getKeyAttribute(element);
		String value = getValueAttribute(element);
		setAttribute(key, value);
	}
	
	protected void setIntegerAttribute(Element element) {
		String key = getKeyAttribute(element);
		String value = getValueAttribute(element);
		setAttribute(key, new Integer(value));
	}
	
	protected void setBooleanAttribute(Element element) {
		String key = getKeyAttribute(element);
		String value = getValueAttribute(element);
		setAttribute(key, new Boolean(value));
	}
	
	protected void setListAttribute(Element element) {
		String listKey = element.getAttribute("key");  //$NON-NLS-1$		
		NodeList nodeList = element.getChildNodes();
		int entryCount = nodeList.getLength();
		List list = new ArrayList(entryCount);
		for (int i = 0; i < entryCount; i++) {
			Node node = nodeList.item(i);
			short type = node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				Element subElement = (Element) node;
				String nodeName = subElement.getNodeName();				
				if (!nodeName.equalsIgnoreCase("listEntry")) { //$NON-NLS-1$
					return; 
				}
				String value = getValueAttribute(subElement);
				list.add(value);
			}
		}
		setAttribute(listKey, list);
	}
	
	protected void setMapAttribute(Element element) {
		String mapKey = element.getAttribute("key");  //$NON-NLS-1$
		NodeList nodeList = element.getChildNodes();
		int entryCount = nodeList.getLength();
		Map map = new HashMap(entryCount);
		for (int i = 0; i < entryCount; i++) {
			Node node = nodeList.item(i);
			short type = node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				Element subElement = (Element) node;
				String nodeName = subElement.getNodeName();				
				if (!nodeName.equalsIgnoreCase("mapEntry")) { //$NON-NLS-1$
					return; 
				}
				String key = getKeyAttribute(subElement);
				String value = getValueAttribute(subElement);
				map.put(key, value);
			}
		}
		setAttribute(mapKey, map);
	}	
	
	private HashMap getAttributeTable() {
		return fAttributes;
	}
	
	public String getStringAttribute(String key, String defaultValue) {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof String) {
				return (String)attr;
			} 
		}
		return defaultValue;
	}

	public int getIntAttribute(String key, int defaultValue) {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof Integer) {
				return ((Integer)attr).intValue();
			} 
		}
		return defaultValue;
	}

	public boolean getBooleanAttribute(String key, boolean defaultValue) {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof Boolean) {
				return ((Boolean)attr).booleanValue();
			} 
		}
		return defaultValue;
	}

	public List getListAttribute(String key, List defaultValue) {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof List) {
				return (List)attr;
			} 
		}
		return defaultValue;
	}

	public Map getMapAttribute(String key, Map defaultValue) {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof Map) {
				return (Map)attr;
			} 
		}
		return defaultValue;
	}
	
	protected String getKeyAttribute(Element element) {
		String key = element.getAttribute("key"); //$NON-NLS-1$
		if (key == null) {
			return null;
		}
		return key;
	}

	protected String getValueAttribute(Element element) {
		String value = element.getAttribute("value"); //$NON-NLS-1$
		if (value == null) {
			return null;
		}
		return value;
	}

	public void setConfig(String filePath){			
		String mainFile = null;
		mainFile = getStringAttribute(ATTR_WPP_PROGRAM, (String)null);
		if (new File(mainFile).isAbsolute()){
			String mainFilePath= mainFile;
			FilePaths.setMainFilePaths(mainFilePath);
		}else{
			String mainFilePath= procRelativePath(mainFile, filePath);
			FilePaths.setMainFilePaths(mainFilePath);
		}
		
		String dvarFile = null;
		dvarFile = getStringAttribute(ATTR_WPP_DVARFILE, (String)null);
		if (new File(dvarFile).isAbsolute()){
			String dvarFilePath=dvarFile;
			FilePaths.setDvarFilePaths(dvarFilePath);
		}else{
			String dvarFilePath=procRelativePath(dvarFile, filePath);
			FilePaths.setDvarFilePaths(dvarFilePath);
		}
		
		String svarFile = null;
		svarFile = getStringAttribute(ATTR_WPP_SVARFILE, (String)null);
		if (new File(svarFile).isAbsolute()){
			String svarFilePath=svarFile;
			FilePaths.setSvarFilePaths(svarFilePath);
		}else{
			String svarFilePath=procRelativePath(svarFile, filePath);
			FilePaths.setSvarFilePaths(svarFilePath);
		}
		
		String initFile = null;
		initFile = getStringAttribute(ATTR_WPP_INITFILE, (String)null);
		if (new File(initFile).isAbsolute()){
			String initFilePath=initFile;
			FilePaths.setInitFilePaths(initFilePath);
		}else{
			String initFilePath=procRelativePath(initFile, filePath);
			FilePaths.setInitFilePaths(initFilePath);
		}
		
		String gwDataFolder = "";
		gwDataFolder = getStringAttribute(ATTR_WPP_GWDATAFOLDER, (String)"");
		if (gwDataFolder.equals("")){
			FilePaths.groundwaterDir="";
		}else{
			gwDataFolder=gwDataFolder+File.separator;
			if (new File(gwDataFolder).isAbsolute()){
				FilePaths.groundwaterDir=gwDataFolder;
			}else{
				FilePaths.groundwaterDir=procRelativePath(gwDataFolder, filePath);
			}
		}	
		
		ControlData.partA = getStringAttribute(ATTR_WPP_APART, (String)null);
		
		ControlData.svDvPartF = getStringAttribute(ATTR_WPP_SVFPART, (String)null);
		
		ControlData.initPartF = getStringAttribute(ATTR_WPP_INITFPART, (String)null);
		
		ControlData.timeStep = getStringAttribute(ATTR_WPP_TIMESTEP, (String)null);
					
		ControlData.startYear = Integer.parseInt(getStringAttribute(ATTR_WPP_STARTYEAR, (String)null));
		ControlData.startMonth = TimeOperation.monthValue(getStringAttribute(ATTR_WPP_STARTMONTH, (String)null));
	
		ControlData.endYear = Integer.parseInt(getStringAttribute(ATTR_WPP_ENDYEAR, (String)null));
		ControlData.endMonth = TimeOperation.monthValue(getStringAttribute(ATTR_WPP_ENDMONTH, (String)null));
	
		ControlData.startDay= Integer.parseInt(getStringAttribute(ATTR_WPP_STARTDAY, (String)null));
		ControlData.endDay=Integer.parseInt(getStringAttribute(ATTR_WPP_ENDDAY, (String)null));
		
		String wreslPlus=getStringAttribute(ATTR_WPP_WRESLPLUS, "no");
		if (wreslPlus.equalsIgnoreCase("no")){
			StudyUtils.useWreslPlus = false;
		}else{
			StudyUtils.useWreslPlus = true;
		}
				
		String allRestartFiles = getStringAttribute(ATTR_WPP_ALLRESTARTFILES, "no");
		if (allRestartFiles.equalsIgnoreCase("no")){
			ControlData.allRestartFiles=false;
		}else{
			ControlData.allRestartFiles=true;
		}
		ControlData.numberRestartFiles  = Integer.parseInt(getStringAttribute(ATTR_WPP_NUMBERRESTARTFILES, "12"));
		ControlData.vHecLib = Integer.parseInt(getStringAttribute(ATTR_WPP_VHECLIB, "6"));
		
		ControlData.databaseURL=getStringAttribute(ATTR_WPP_DATABASEURL, "none");
		ControlData.sqlGroup=getStringAttribute(ATTR_WPP_SQLGROUP, "calsim");
		ControlData.ovOption=Integer.parseInt(getStringAttribute(ATTR_WPP_OVOPTION, "0"));
		ControlData.ovFile=getStringAttribute(ATTR_WPP_OVFILE, "");
		
		String unchangeGWRestart = getStringAttribute(ATTR_WPP_UNCHANGEGWRESTART, "no");
		if (unchangeGWRestart.equalsIgnoreCase("no")){
			ControlData.unchangeGWRestart=false;
		}else{
			ControlData.unchangeGWRestart=true;
		}
		
		String ilpLog=getStringAttribute(ATTR_WPP_ILPLOG, "no");
		if (ilpLog.equalsIgnoreCase("no")){
			ILP.logging = false;
			ILP.loggingAllCycles=false;
			ILP.loggingCplexLp = false;
		}else{
			ILP.logging = true;
			ILP.loggingAllCycles=true;
			ILP.loggingCplexLp = true;
		}
	}
	
	public String procRelativePath(String path, String launchFilePath){
		String absPath=new File(launchFilePath).getParentFile().getAbsolutePath();
		absPath=absPath+"\\"+path;
		return absPath;
	}
	
	public void setPref(){
		String w2dir = System.getenv("temp_wrims2");
		try {
			String dataDir = new File(w2dir).getCanonicalPath()+"\\data";
			SettingPref.load(dataDir);
			SettingPref.loadCBCSetting(dataDir);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
}