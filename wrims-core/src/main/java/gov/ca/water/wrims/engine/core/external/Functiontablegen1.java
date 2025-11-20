package gov.ca.water.wrims.engine.core.external;

import java.util.*;

import gov.ca.water.wrims.engine.core.components.FilePaths;

public class Functiontablegen1 extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functiontablegen1(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param13 = stack.pop();
		Object param12 = stack.pop();
		Object param11 = stack.pop();
		Object param10 = stack.pop();
		Object param9 = stack.pop();
		Object param8 = stack.pop();
		Object param7 = stack.pop();
		Object param6 = stack.pop();
		Object param5 = stack.pop();
		Object param4 = stack.pop();
		Object param3 = stack.pop();
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		Number[] Q100SJ_Arr = (Number[])param13;
		int size_Q100SJ=Q100SJ_Arr.length;
		float[] Q100SJ=new float[size_Q100SJ];
		for (int i=0; i<size_Q100SJ; i++){
			Q100SJ[i]=Q100SJ_Arr[i].floatValue();
		}
		Number[] Q100ME_Arr = (Number[])param12;
		int size_Q100ME=Q100ME_Arr.length;
		float[] Q100ME=new float[size_Q100ME];
		for (int i=0; i<size_Q100ME; i++){
			Q100ME[i]=Q100ME_Arr[i].floatValue();
		}
		Number[] Q100TU_Arr = (Number[])param11;
		int size_Q100TU=Q100TU_Arr.length;
		float[] Q100TU=new float[size_Q100TU];
		for (int i=0; i<size_Q100TU; i++){
			Q100TU[i]=Q100TU_Arr[i].floatValue();
		}
		Number[] Q100ST_Arr = (Number[])param10;
		int size_Q100ST=Q100ST_Arr.length;
		float[] Q100ST=new float[size_Q100ST];
		for (int i=0; i<size_Q100ST; i++){
			Q100ST[i]=Q100ST_Arr[i].floatValue();
		}
		Number[] Q100WH_Arr = (Number[])param9;
		int size_Q100WH=Q100WH_Arr.length;
		float[] Q100WH=new float[size_Q100WH];
		for (int i=0; i<size_Q100WH; i++){
			Q100WH[i]=Q100WH_Arr[i].floatValue();
		}
		Number[] Q100TR_Arr = (Number[])param8;
		int size_Q100TR=Q100TR_Arr.length;
		float[] Q100TR=new float[size_Q100TR];
		for (int i=0; i<size_Q100TR; i++){
			Q100TR[i]=Q100TR_Arr[i].floatValue();
		}
		Number[] Q100BD_Arr = (Number[])param7;
		int size_Q100BD=Q100BD_Arr.length;
		float[] Q100BD=new float[size_Q100BD];
		for (int i=0; i<size_Q100BD; i++){
			Q100BD[i]=Q100BD_Arr[i].floatValue();
		}
		Number[] Q100SH_Arr = (Number[])param6;
		int size_Q100SH=Q100SH_Arr.length;
		float[] Q100SH=new float[size_Q100SH];
		for (int i=0; i<size_Q100SH; i++){
			Q100SH[i]=Q100SH_Arr[i].floatValue();
		}
		Number[] Q100YU_Arr = (Number[])param5;
		int size_Q100YU=Q100YU_Arr.length;
		float[] Q100YU=new float[size_Q100YU];
		for (int i=0; i<size_Q100YU; i++){
			Q100YU[i]=Q100YU_Arr[i].floatValue();
		}
		Number[] Q100FO_Arr = (Number[])param4;
		int size_Q100FO=Q100FO_Arr.length;
		float[] Q100FO=new float[size_Q100FO];
		for (int i=0; i<size_Q100FO; i++){
			Q100FO[i]=Q100FO_Arr[i].floatValue();
		}
		Number[] Q100OR_Arr = (Number[])param3;
		int size_Q100OR=Q100OR_Arr.length;
		float[] Q100OR=new float[size_Q100OR];
		for (int i=0; i<size_Q100OR; i++){
			Q100OR[i]=Q100OR_Arr[i].floatValue();
		}
		int ICODE1 = ((Number) param2).intValue();
		int ICALC1 = ((Number) param1).intValue();

		String cPath=FilePaths.genTableDir;
		int iLenPath=cPath.length();
		
		float result = tablegen1(cPath, iLenPath, ICALC1, ICODE1, Q100OR, Q100FO, Q100YU, Q100SH, Q100BD, Q100TR, Q100WH, Q100ST, Q100TU, Q100ME, Q100SJ);

		// push the result on the Stack
		stack.push(new Float(result));

	}

	public native float tablegen1(String cPath, int iLenPath, int ICALC1, int ICODE1, float[] Q100OR, float[] Q100FO, float[] Q100YU, float[] Q100SH, float[] Q100BD, float[] Q100TR, float[] Q100WH, float[] Q100ST, float[] Q100TU, float[] Q100ME, float[] Q100SJ);
}
