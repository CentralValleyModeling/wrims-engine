package wrims.engine.core.external;

import java.util.*;

import wrims.engine.core.components.FilePaths;

public class Functionhydroforecast extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functionhydroforecast(){

	}

	public void execute(Stack stack) {

		if (stack.size()==2){
			Object param2 = stack.pop();
			Object param1 = stack.pop();
			
			int ICODE = ((Number) param2).intValue();
			int ICALC = ((Number) param1).intValue();
			int IYPOS = 0;
			int IMPOS = 0;
			int IYEAR = 0;
			int IM = 0; 
			int IFREQOD = 0;
			int IFREQ1 = 0;
			int IFREQ2 = 0;
			int IFREQ3 = 0;
			int IFREQ4 = 0;
			int IFREQ5 = 0;
			int ICC = 0;
			float[] a1={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a2={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a3={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a4={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a5={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a6={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a7={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a8={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a9={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a10={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a11={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a12={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a13={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a14={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a15={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a16={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a17={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a18={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a19={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a20={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a21={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a22={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a23={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a24={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a25={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a26={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a27={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a28={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a29={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a30={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a31={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a32={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a33={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a34={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a35={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a36={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a37={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a38={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a39={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a40={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a41={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a42={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a43={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a44={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a45={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a46={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a47={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a48={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a49={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a50={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a51={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a52={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a53={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a54={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a55={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a56={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a57={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			float[] a58={0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f};
			
			String cPath=FilePaths.genTableDir;
			int iLenPath=cPath.length();
			
			float result = hydroforecast(cPath, iLenPath, ICALC, ICODE, IYPOS, IMPOS, IYEAR, IM, IFREQOD, IFREQ1, IFREQ2, IFREQ3, IFREQ4, IFREQ5, ICC, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10,  a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58);
			
			// push the result on the Stack
			stack.push(new Float(result));
		}else{
			//values in reverse order:
			Object param71 = stack.pop();
			Object param70 = stack.pop();
			Object param69 = stack.pop();
			Object param68 = stack.pop();
			Object param67 = stack.pop();
			Object param66 = stack.pop();
			Object param65 = stack.pop();
			Object param64 = stack.pop();
			Object param63 = stack.pop();
			Object param62 = stack.pop();
			Object param61 = stack.pop();
			Object param60 = stack.pop();
			Object param59 = stack.pop();
			Object param58 = stack.pop();
			Object param57 = stack.pop();
			Object param56 = stack.pop();
			Object param55 = stack.pop();
			Object param54 = stack.pop();
			Object param53 = stack.pop();
			Object param52 = stack.pop();
			Object param51 = stack.pop();
			Object param50 = stack.pop();
			Object param49 = stack.pop();
			Object param48 = stack.pop();
			Object param47 = stack.pop();
			Object param46 = stack.pop();
			Object param45 = stack.pop();
			Object param44 = stack.pop();
			Object param43 = stack.pop();
			Object param42 = stack.pop();
			Object param41 = stack.pop();
			Object param40 = stack.pop();
			Object param39 = stack.pop();
			Object param38 = stack.pop();
			Object param37 = stack.pop();
			Object param36 = stack.pop();
			Object param35 = stack.pop();
			Object param34 = stack.pop();
			Object param33 = stack.pop();
			Object param32 = stack.pop();
			Object param31 = stack.pop();
			Object param30 = stack.pop();
			Object param29 = stack.pop();
			Object param28 = stack.pop();
			Object param27 = stack.pop();
			Object param26 = stack.pop();
			Object param25 = stack.pop();
			Object param24 = stack.pop();
			Object param23 = stack.pop();
			Object param22 = stack.pop();
			Object param21 = stack.pop();
			Object param20 = stack.pop();
			Object param19 = stack.pop();
			Object param18 = stack.pop();
			Object param17 = stack.pop();
			Object param16 = stack.pop();
			Object param15 = stack.pop();
			Object param14 = stack.pop();
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

			Number[] QSJ1_Arr = (Number[])param71;
			int size_QSJ1=QSJ1_Arr.length;
			float[] QSJ1=new float[size_QSJ1];
			for (int i=0; i<size_QSJ1; i++){
				QSJ1[i]=QSJ1_Arr[i].floatValue();
			}
			Number[] QME1_Arr = (Number[])param70;
			int size_QME1=QME1_Arr.length;
			float[] QME1=new float[size_QME1];
			for (int i=0; i<size_QME1; i++){
				QME1[i]=QME1_Arr[i].floatValue();
			}
			Number[] QTU1_Arr = (Number[])param69;
			int size_QTU1=QTU1_Arr.length;
			float[] QTU1=new float[size_QTU1];
			for (int i=0; i<size_QTU1; i++){
				QTU1[i]=QTU1_Arr[i].floatValue();
			}
			Number[] QST1_Arr = (Number[])param68;
			int size_QST1=QST1_Arr.length;
			float[] QST1=new float[size_QST1];
			for (int i=0; i<size_QST1; i++){
				QST1[i]=QST1_Arr[i].floatValue();
			}
			Number[] QWH1_Arr = (Number[])param67;
			int size_QWH1=QWH1_Arr.length;
			float[] QWH1=new float[size_QWH1];
			for (int i=0; i<size_QWH1; i++){
				QWH1[i]=QWH1_Arr[i].floatValue();
			}
			Number[] QTR1_Arr = (Number[])param66;
			int size_QTR1=QTR1_Arr.length;
			float[] QTR1=new float[size_QTR1];
			for (int i=0; i<size_QTR1; i++){
				QTR1[i]=QTR1_Arr[i].floatValue();
			}
			Number[] QBD1_Arr = (Number[])param65;
			int size_QBD1=QBD1_Arr.length;
			float[] QBD1=new float[size_QBD1];
			for (int i=0; i<size_QBD1; i++){
				QBD1[i]=QBD1_Arr[i].floatValue();
			}
			Number[] QSH1_Arr = (Number[])param64;
			int size_QSH1=QSH1_Arr.length;
			float[] QSH1=new float[size_QSH1];
			for (int i=0; i<size_QSH1; i++){
				QSH1[i]=QSH1_Arr[i].floatValue();
			}
			Number[] QYU1_Arr = (Number[])param63;
			int size_QYU1=QYU1_Arr.length;
			float[] QYU1=new float[size_QYU1];
			for (int i=0; i<size_QYU1; i++){
				QYU1[i]=QYU1_Arr[i].floatValue();
			}
			Number[] QFO1_Arr = (Number[])param62;
			int size_QFO1=QFO1_Arr.length;
			float[] QFO1=new float[size_QFO1];
			for (int i=0; i<size_QFO1; i++){
				QFO1[i]=QFO1_Arr[i].floatValue();
			}
			Number[] QOR1_Arr = (Number[])param61;
			int size_QOR1=QOR1_Arr.length;
			float[] QOR1=new float[size_QOR1];
			for (int i=0; i<size_QOR1; i++){
				QOR1[i]=QOR1_Arr[i].floatValue();
			}
			Number[] VPDSJ_Arr = (Number[])param60;
			int size_VPDSJ=VPDSJ_Arr.length;
			float[] VPDSJ=new float[size_VPDSJ];
			for (int i=0; i<size_VPDSJ; i++){
				VPDSJ[i]=VPDSJ_Arr[i].floatValue();
			}
			Number[] VPDME_Arr = (Number[])param59;
			int size_VPDME=VPDME_Arr.length;
			float[] VPDME=new float[size_VPDME];
			for (int i=0; i<size_VPDME; i++){
				VPDME[i]=VPDME_Arr[i].floatValue();
			}
			Number[] VPDTU_Arr = (Number[])param58;
			int size_VPDTU=VPDTU_Arr.length;
			float[] VPDTU=new float[size_VPDTU];
			for (int i=0; i<size_VPDTU; i++){
				VPDTU[i]=VPDTU_Arr[i].floatValue();
			}
			Number[] VPDST_Arr = (Number[])param57;
			int size_VPDST=VPDST_Arr.length;
			float[] VPDST=new float[size_VPDST];
			for (int i=0; i<size_VPDST; i++){
				VPDST[i]=VPDST_Arr[i].floatValue();
			}
			Number[] VPDWH_Arr = (Number[])param56;
			int size_VPDWH=VPDWH_Arr.length;
			float[] VPDWH=new float[size_VPDWH];
			for (int i=0; i<size_VPDWH; i++){
				VPDWH[i]=VPDWH_Arr[i].floatValue();
			}
			Number[] VPDTR_Arr = (Number[])param55;
			int size_VPDTR=VPDTR_Arr.length;
			float[] VPDTR=new float[size_VPDTR];
			for (int i=0; i<size_VPDTR; i++){
				VPDTR[i]=VPDTR_Arr[i].floatValue();
			}
			Number[] VPDSH_Arr = (Number[])param54;
			int size_VPDSH=VPDSH_Arr.length;
			float[] VPDSH=new float[size_VPDSH];
			for (int i=0; i<size_VPDSH; i++){
				VPDSH[i]=VPDSH_Arr[i].floatValue();
			}
			Number[] VPDYU_Arr = (Number[])param53;
			int size_VPDYU=VPDYU_Arr.length;
			float[] VPDYU=new float[size_VPDYU];
			for (int i=0; i<size_VPDYU; i++){
				VPDYU[i]=VPDYU_Arr[i].floatValue();
			}
			Number[] VPDFO_Arr = (Number[])param52;
			int size_VPDFO=VPDFO_Arr.length;
			float[] VPDFO=new float[size_VPDFO];
			for (int i=0; i<size_VPDFO; i++){
				VPDFO[i]=VPDFO_Arr[i].floatValue();
			}
			Number[] VPDOR_Arr = (Number[])param51;
			int size_VPDOR=VPDOR_Arr.length;
			float[] VPDOR=new float[size_VPDOR];
			for (int i=0; i<size_VPDOR; i++){
				VPDOR[i]=VPDOR_Arr[i].floatValue();
			}
			Number[] TSJ_Arr = (Number[])param50;
			int size_TSJ=TSJ_Arr.length;
			float[] TSJ=new float[size_TSJ];
			for (int i=0; i<size_TSJ; i++){
				TSJ[i]=TSJ_Arr[i].floatValue();
			}
			Number[] TME_Arr = (Number[])param49;
			int size_TME=TME_Arr.length;
			float[] TME=new float[size_TME];
			for (int i=0; i<size_TME; i++){
				TME[i]=TME_Arr[i].floatValue();
			}
			Number[] TTU_Arr = (Number[])param48;
			int size_TTU=TTU_Arr.length;
			float[] TTU=new float[size_TTU];
			for (int i=0; i<size_TTU; i++){
				TTU[i]=TTU_Arr[i].floatValue();
			}
			Number[] TST_Arr = (Number[])param47;
			int size_TST=TST_Arr.length;
			float[] TST=new float[size_TST];
			for (int i=0; i<size_TST; i++){
				TST[i]=TST_Arr[i].floatValue();
			}
			Number[] TWH_Arr = (Number[])param46;
			int size_TWH=TWH_Arr.length;
			float[] TWH=new float[size_TWH];
			for (int i=0; i<size_TWH; i++){
				TWH[i]=TWH_Arr[i].floatValue();
			}
			Number[] TTR_Arr = (Number[])param45;
			int size_TTR=TTR_Arr.length;
			float[] TTR=new float[size_TTR];
			for (int i=0; i<size_TTR; i++){
				TTR[i]=TTR_Arr[i].floatValue();
			}
			Number[] TSH_Arr = (Number[])param44;
			int size_TSH=TSH_Arr.length;
			float[] TSH=new float[size_TSH];
			for (int i=0; i<size_TSH; i++){
				TSH[i]=TSH_Arr[i].floatValue();
			}
			Number[] TYU_Arr = (Number[])param43;
			int size_TYU=TYU_Arr.length;
			float[] TYU=new float[size_TYU];
			for (int i=0; i<size_TYU; i++){
				TYU[i]=TYU_Arr[i].floatValue();
			}
			Number[] TFO_Arr = (Number[])param42;
			int size_TFO=TFO_Arr.length;
			float[] TFO=new float[size_TFO];
			for (int i=0; i<size_TFO; i++){
				TFO[i]=TFO_Arr[i].floatValue();
			}
			Number[] TOR_Arr = (Number[])param41;
			int size_TOR=TOR_Arr.length;
			float[] TOR=new float[size_TOR];
			for (int i=0; i<size_TOR; i++){
				TOR[i]=TOR_Arr[i].floatValue();
			}
			Number[] PSJ_Arr = (Number[])param40;
			int size_PSJ=PSJ_Arr.length;
			float[] PSJ=new float[size_PSJ];
			for (int i=0; i<size_PSJ; i++){
				PSJ[i]=PSJ_Arr[i].floatValue();
			}
			Number[] PME_Arr = (Number[])param39;
			int size_PME=PME_Arr.length;
			float[] PME=new float[size_PME];
			for (int i=0; i<size_PME; i++){
				PME[i]=PME_Arr[i].floatValue();
			}
			Number[] PTU_Arr = (Number[])param38;
			int size_PTU=PTU_Arr.length;
			float[] PTU=new float[size_PTU];
			for (int i=0; i<size_PTU; i++){
				PTU[i]=PTU_Arr[i].floatValue();
			}
			Number[] PST_Arr = (Number[])param37;
			int size_PST=PST_Arr.length;
			float[] PST=new float[size_PST];
			for (int i=0; i<size_PST; i++){
				PST[i]=PST_Arr[i].floatValue();
			}
			Number[] PWH_Arr = (Number[])param36;
			int size_PWH=PWH_Arr.length;
			float[] PWH=new float[size_PWH];
			for (int i=0; i<size_PWH; i++){
				PWH[i]=PWH_Arr[i].floatValue();
			}
			Number[] PTR_Arr = (Number[])param35;
			int size_PTR=PTR_Arr.length;
			float[] PTR=new float[size_PTR];
			for (int i=0; i<size_PTR; i++){
				PTR[i]=PTR_Arr[i].floatValue();
			}
			Number[] PSH_Arr = (Number[])param34;
			int size_PSH=PSH_Arr.length;
			float[] PSH=new float[size_PSH];
			for (int i=0; i<size_PSH; i++){
				PSH[i]=PSH_Arr[i].floatValue();
			}
			Number[] PYU_Arr = (Number[])param33;
			int size_PYU=PYU_Arr.length;
			float[] PYU=new float[size_PYU];
			for (int i=0; i<size_PYU; i++){
				PYU[i]=PYU_Arr[i].floatValue();
			}
			Number[] PFO_Arr = (Number[])param32;
			int size_PFO=PFO_Arr.length;
			float[] PFO=new float[size_PFO];
			for (int i=0; i<size_PFO; i++){
				PFO[i]=PFO_Arr[i].floatValue();
			}
			Number[] POR_Arr = (Number[])param31;
			int size_POR=POR_Arr.length;
			float[] POR=new float[size_POR];
			for (int i=0; i<size_POR; i++){
				POR[i]=POR_Arr[i].floatValue();
			}
			Number[] QSJ_Arr = (Number[])param30;
			int size_QSJ=QSJ_Arr.length;
			float[] QSJ=new float[size_QSJ];
			for (int i=0; i<size_QSJ; i++){
				QSJ[i]=QSJ_Arr[i].floatValue();
			}
			Number[] QME_Arr = (Number[])param29;
			int size_QME=QME_Arr.length;
			float[] QME=new float[size_QME];
			for (int i=0; i<size_QME; i++){
				QME[i]=QME_Arr[i].floatValue();
			}
			Number[] QTU_Arr = (Number[])param28;
			int size_QTU=QTU_Arr.length;
			float[] QTU=new float[size_QTU];
			for (int i=0; i<size_QTU; i++){
				QTU[i]=QTU_Arr[i].floatValue();
			}
			Number[] QST_Arr = (Number[])param27;
			int size_QST=QST_Arr.length;
			float[] QST=new float[size_QST];
			for (int i=0; i<size_QST; i++){
				QST[i]=QST_Arr[i].floatValue();
			}
			Number[] QWH_Arr = (Number[])param26;
			int size_QWH=QWH_Arr.length;
			float[] QWH=new float[size_QWH];
			for (int i=0; i<size_QWH; i++){
				QWH[i]=QWH_Arr[i].floatValue();
			}
			Number[] QTR_Arr = (Number[])param25;
			int size_QTR=QTR_Arr.length;
			float[] QTR=new float[size_QTR];
			for (int i=0; i<size_QTR; i++){
				QTR[i]=QTR_Arr[i].floatValue();
			}
			Number[] QBD_Arr = (Number[])param24;
			int size_QBD=QBD_Arr.length;
			float[] QBD=new float[size_QBD];
			for (int i=0; i<size_QBD; i++){
				QBD[i]=QBD_Arr[i].floatValue();
			}
			Number[] QSH_Arr = (Number[])param23;
			int size_QSH=QSH_Arr.length;
			float[] QSH=new float[size_QSH];
			for (int i=0; i<size_QSH; i++){
				QSH[i]=QSH_Arr[i].floatValue();
			}
			Number[] QYU_Arr = (Number[])param22;
			int size_QYU=QYU_Arr.length;
			float[] QYU=new float[size_QYU];
			for (int i=0; i<size_QYU; i++){
				QYU[i]=QYU_Arr[i].floatValue();
			}
			Number[] QFO_Arr = (Number[])param21;
			int size_QFO=QFO_Arr.length;
			float[] QFO=new float[size_QFO];
			for (int i=0; i<size_QFO; i++){
				QFO[i]=QFO_Arr[i].floatValue();
			}
			Number[] QOR_Arr = (Number[])param20;
			int size_QOR=QOR_Arr.length;
			float[] QOR=new float[size_QOR];
			for (int i=0; i<size_QOR; i++){
				QOR[i]=QOR_Arr[i].floatValue();
			}
			Number[] SQNB_Arr = (Number[])param19;
			int size_SQNB=19;
			float[] SQNB=new float[size_SQNB];
			for (int i=0; i<size_SQNB; i++){
				if (i>=SQNB_Arr.length){
					SQNB[i]=0.0f;
				}else{
					SQNB[i]=SQNB_Arr[i].floatValue();
				}
			}
			Number[] SQSW_Arr = (Number[])param18;
			int size_SQSW=19;
			float[] SQSW=new float[size_SQSW];
			for (int i=0; i<size_SQSW; i++){
				if (i>=SQSW_Arr.length){
					SQSW[i]=0.0f;
				}else{
					SQSW[i]=SQSW_Arr[i].floatValue();
				}
			}
			Number[] SQFP_Arr = (Number[])param17;
			int size_SQFP=19;
			float[] SQFP=new float[size_SQFP];
			for (int i=0; i<size_SQFP; i++){
				if (i>=SQFP_Arr.length){
					SQFP[i]=0.0f;
				}else{
					SQFP[i]=SQFP_Arr[i].floatValue();
				}
			}
			Number[] SQFW_Arr = (Number[])param16;
			int size_SQFW=19;
			float[] SQFW=new float[size_SQFW];
			for (int i=0; i<size_SQFW; i++){
				if (i>=SQFW_Arr.length){
					SQFW[i]=0.0f;
				}else{
					SQFW[i]=SQFW_Arr[i].floatValue();
				}
			}
			Number[] SQOR_Arr = (Number[])param15;
			int size_SQOR=19;
			float[] SQOR=new float[size_SQOR];
			for (int i=0; i<size_SQOR; i++){
				if (i>=SQOR_Arr.length){
				SQOR[i]=0.0f;
				}else{
					SQOR[i]=SQOR_Arr[i].floatValue();
				}
			}
			Number[] SQKS_Arr = (Number[])param14;
			int size_SQKS=19;
			float[] SQKS=new float[size_SQKS];
			for (int i=0; i<size_SQKS; i++){
				if (i>=SQKS_Arr.length){
				SQKS[i]=0.0f;
				}else{
					SQKS[i]=SQKS_Arr[i].floatValue();
				}
			}
			int ICC = ((Number) param13).intValue();
			int IFREQ5 = ((Number) param12).intValue();
			int IFREQ4 = ((Number) param11).intValue();
			int IFREQ3 = ((Number) param10).intValue();
			int IFREQ2 = ((Number) param9).intValue();
			int IFREQ1 = ((Number) param8).intValue();
			int IFREQOD = ((Number) param7).intValue();
			int IM = ((Number) param6).intValue();
			int IYEAR = ((Number) param5).intValue();
			int IMPOS = ((Number) param4).intValue();
			int IYPOS = ((Number) param3).intValue();
			int ICODE = ((Number) param2).intValue();
			int ICALC = ((Number) param1).intValue();

			String cPath=FilePaths.genTableDir;
			int iLenPath=cPath.length();
			
			float result = hydroforecast(cPath, iLenPath, ICALC, ICODE, IYPOS, IMPOS, IYEAR, IM, IFREQOD, IFREQ1, IFREQ2, IFREQ3, IFREQ4, IFREQ5, ICC, SQKS, SQOR, SQFW, SQFP, SQSW, SQNB, QOR, QFO, QYU, QSH, QBD, QTR, QWH, QST, QTU, QME, QSJ, POR, PFO, PYU, PSH, PTR, PWH, PST, PTU, PME, PSJ, TOR, TFO, TYU, TSH, TTR, TWH, TST, TTU, TME, TSJ, VPDOR, VPDFO, VPDYU, VPDSH, VPDTR, VPDWH, VPDST, VPDTU, VPDME, VPDSJ, QOR1, QFO1, QYU1, QSH1, QBD1, QTR1, QWH1, QST1, QTU1, QME1, QSJ1);

			// push the result on the Stack
			stack.push(new Float(result));
		
		}

	}

	public native float hydroforecast(String cPath, int iLenPath, int ICALC, int ICODE, int IYPOS, int IMPOS, int IYEAR, int IM, int IFREQOD, int IFREQ1, int IFREQ2, int IFREQ3, int IFREQ4, int IFREQ5, int ICC, float[] SQKS, float[] SQOR, float[] SQFW, float[] SQFP, float[] SQSW, float[] SQNB, float[] QOR, float[] QFO, float[] QYU, float[] QSH, float[] QBD, float[] QTR, float[] QWH, float[] QST, float[] QTU, float[] QME, float[] QSJ, float[] POR, float[] PFO, float[] PYU, float[] PSH, float[] PTR, float[] PWH, float[] PST, float[] PTU, float[] PME, float[] PSJ, float[] TOR, float[] TFO, float[] TYU, float[] TSH, float[] TTR, float[] TWH, float[] TST, float[] TTU, float[] TME, float[] TSJ, float[] VPDOR, float[] VPDFO, float[] VPDYU, float[] VPDSH, float[] VPDTR, float[] VPDWH, float[] VPDST, float[] VPDTU, float[] VPDME, float[] VPDSJ, float[] QOR1, float[] QFO1, float[] QYU1, float[] QSH1, float[] QBD1, float[] QTR1, float[] QWH1, float[] QST1, float[] QTU1, float[] QME1, float[] QSJ1);
}
