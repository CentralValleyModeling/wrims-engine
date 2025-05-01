package wrimsv2.solver.mpmodel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;

import wrimsv2.solver.mpmodel.export.LpSolveExport;
import wrimsv2.wreslparser.elements.Tools;


public class MPModelUtils {

	public static void toLpSolve(MPModel in, String dir, String fileName) {
		
		String lpsolveStr = toLpSolve(in);
		
		PrintWriter pw;
		
		try {
			pw = Tools.openFile(dir, fileName);
			pw.write(lpsolveStr);
			pw.flush();
			pw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public static String toLpSolve(MPModel in) {
			
		String out = "";
			
			
			String objStr        = LpSolveExport.writeObj(in.objFunction);
			String constraintStr = LpSolveExport.writeConstraint(in.constraintLhs, in.constraintRhs);
			String dvarStr =       LpSolveExport.writeDvar(in);
			
		out = objStr + constraintStr + dvarStr;
		return out;
	}
	
	public static MPModel load(String MPModelPath) throws FileNotFoundException, IOException, ClassNotFoundException {

		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(MPModelPath));
		MPModel m = (MPModel) (ois.readObject());

		return m;

	}

}
