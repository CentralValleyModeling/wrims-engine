package wrimsv2.external;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import wrimsv2.components.Error;
import jep.Jep;
import jep.JepConfig;
import jep.JepException;

public class Functionsuitablehabitat extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functionsuitablehabitat(){

	}

	@Override
	public void execute(Stack stack) {
		
		//values in reverse order:
		Object param3 = stack.pop();
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		float x2 = ((Number) param3).floatValue();
		float x1 = ((Number) param2).floatValue();
		float x0 = ((Number) param1).floatValue();

		float result = SuitableHabitat(x0, x1, x2);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public float SuitableHabitat(float x0, float x1, float x2){
		try {
			//setPythonEnv();
			new LoadDll("jep.dll");
			
			String scriptPath = externalDir;
			
			JepConfig cfg = new JepConfig();
	        cfg.addSharedModules("numpy", "pandas");
	        cfg.addIncludePaths(scriptPath);

			Jep jep = new Jep(cfg);
									
			jep.eval("import external_python_process");
			jep.set("x0", x0);
		    jep.set("x1", x1);
		    jep.set("x2", x2);
		    jep.eval("y = external_python_process.SuitableHabitat(x0, x1, x2)");
		    Object result = jep.getValue("y");		    
			//jep.runScript(scriptPath+File.separator+"external_python_process.py");
			
			jep.close();
			return Float.valueOf(result.toString());
		} catch (JepException e) {
			e.printStackTrace();
			Error.addEvaluationError(e.toString());
			return 0;
		}
	}
	
	public void setPythonEnv(){
		/*
		ProcessBuilder pb = new ProcessBuilder("CMD", "/C", "SET");
	    Map<String, String> env = pb.environment();
	    env.put("PYTHONHOME", "C:\\ProgramData\\Anaconda3");
	    env.put("PYTHONPATH", "C:\\ProgramData\\Anaconda3");
	    Process p;
		try {
			p = pb.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
		
	    Map<String, String> env = System.getenv();
	    Field field;
		try {
			field = env.getClass().getDeclaredField("m");
		    field.setAccessible(true);
		    Map<String, String> newEnv=new HashMap<String, String> ();
		    newEnv.put("PYTHONHOME", "C:\\ProgramData\\Anaconda3");
		    newEnv.put("PYTHONPATH", "C:\\ProgramData\\Anaconda3");
		    ((Map<String, String>) field.get(env)).putAll(newEnv);
		} catch (NoSuchFieldException e) {
			Error.addEvaluationError(e.toString());
		} catch (SecurityException e) {
			Error.addEvaluationError(e.toString());
		} catch (IllegalArgumentException e) {
			Error.addEvaluationError(e.toString());
		} catch (IllegalAccessException e) {
			Error.addEvaluationError(e.toString());
		}
	}
}
