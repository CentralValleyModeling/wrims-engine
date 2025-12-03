package wrimsv2.external;

import java.util.*;

/**
 * Custom JEP class for calculating Final Mrdo.
 * @author Clay Booher
 */
public class Functiongetfinalmrdo extends ExternalFunction{
	private int _lastmonth;

	// TO DO: pass in absolute path of dll file from study directory structure; USE load instead of loadLibrary
	public Functiongetfinalmrdo() {

	}

	public void execute(Stack stack) {

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

		//cast params to correct types:
		int daysin = ((Number) param39).intValue();
		int month = ((Number) param38).intValue();
		int wyear = ((Number) param37).intValue();
		int eday_flw = ((Number) param36).intValue();
		int bday_flw = ((Number) param35).intValue();
		float Qflw = ((Number) param34).floatValue();
		int eday_rsl2 = ((Number) param33).intValue();
		int bday_rsl2 = ((Number) param32).intValue();
		float Qrsl2 = ((Number) param31).floatValue();
		int eday_rsl1 = ((Number) param30).intValue();
		int bday_rsl1 = ((Number) param29).intValue();
		float Qrsl1 = ((Number) param28).floatValue();
		int eday_col2 = ((Number) param27).intValue();
		int bday_col2 = ((Number) param26).intValue();
		float Qcol2 = ((Number) param25).floatValue();
		int eday_col1 = ((Number) param24).intValue();
		int bday_col1 = ((Number) param23).intValue();
		float Qcol1 = ((Number) param22).floatValue();
		int eday_jpt2 = ((Number) param21).intValue();
		int bday_jpt2 = ((Number) param20).intValue();
		float Qjpt2 = ((Number) param19).floatValue();
		int eday_jpt1 = ((Number) param18).intValue();
		int bday_jpt1 = ((Number) param17).intValue();
		float Qjpt1 = ((Number) param16).floatValue();
		int eday_emt2 = ((Number) param15).intValue();
		int bday_emt2 = ((Number) param14).intValue();
		float Qemt2 = ((Number) param13).floatValue();
		int eday_emt1 = ((Number) param12).intValue();
		int bday_emt1 = ((Number) param11).intValue();
		float Qemt1 = ((Number) param10).floatValue();
		int eday_cnf = ((Number) param9).intValue();
		int bday_cnf = ((Number) param8).intValue();
		float Qcnf = ((Number) param7).floatValue();
		int eday_chs = ((Number) param6).intValue();
		int bday_chs = ((Number) param5).intValue();
		float Qchs = ((Number) param4).floatValue();
		int eday_roe = ((Number) param3).intValue();
		int bday_roe = ((Number) param2).intValue();
		float Qroe = ((Number) param1).floatValue();

		stack.push(new Float(getFinalMrdo(Qroe, bday_roe, eday_roe, Qchs, bday_chs, eday_chs, Qcnf,
			bday_cnf, eday_cnf, Qemt1, bday_emt1, eday_emt1, Qemt2, bday_emt2, eday_emt2, Qjpt1,
			bday_jpt1, eday_jpt1, Qjpt2, bday_jpt2, eday_jpt2, Qcol1, bday_col1, eday_col1, Qcol2,
			bday_col2, eday_col2, Qrsl1, bday_rsl1, eday_rsl1, Qrsl2, bday_rsl2, eday_rsl2, Qflw,
			bday_flw, eday_flw, wyear, month, daysin)));
		
		//System.gc();

	}

	private float getFinalMrdo(float Qroe, int bday_roe, int eday_roe, float Qchs, int bday_chs,  //CB tried synchronized to prevent java.io.FileNotFoundException but it still happened
		int eday_chs, float Qcnf, int bday_cnf, int eday_cnf, float Qemt1, int bday_emt1,
		int eday_emt1, float Qemt2, int bday_emt2, int eday_emt2, float Qjpt1, int bday_jpt1,
		int eday_jpt1, float Qjpt2, int bday_jpt2, int eday_jpt2, float Qcol1, int bday_col1,
		int eday_col1, float Qcol2, int bday_col2, int eday_col2, float Qrsl1, int bday_rsl1,
		int eday_rsl1, float Qrsl2, int bday_rsl2, int eday_rsl2, float Qflw, int bday_flw,
		int eday_flw, int wyear, int month, int daysin) {

		float[] Qout = new float[daysin + 1]; //CB +1 for Java (vs. Fortran)
		float mrdo;

		//CHARACTER(LEN=20),DIMENSION(daysin):: control
		String[] control = new String[daysin + 1]; //CB +1 for Java (vs. Fortran)
		// CHARACTER(LEN=20):: Gcontrol
		String Gcontrol;
		int em, jp, co, rs;

		//if (wyear==1922 && month==1)  OPEN(17,FILE='delta-control.out',STATUS='new')
		//if (wyear==1922 && month==1)  OPEN(18,FILE='g-control.out',STATUS='new')
/*		File deltaControlOut = new File("delta-control.out");
		if (deltaControlOut.exists())
			deltaControlOut.delete();
		File gControlOut = new File("g-control.out");
		if (gControlOut.exists())
			gControlOut.delete();
		PrintStream deltaOutPS = null;
		PrintStream gOutPS = null;
		try {
			FileOutputStream deltaFOS = new FileOutputStream(deltaControlOut); // creates new one if not existing
			FileOutputStream gFOS = new FileOutputStream(gControlOut); // creates new one if not existing
			deltaOutPS = new PrintStream(deltaFOS);
			gOutPS = new PrintStream(gFOS);
		} catch (FileNotFoundException fnfe) {
        	fnfe.printStackTrace();
        } finally {
        	//CB TODO may need something here if System.gc() above does not eliminate the "The requested operation cannot be performed on a file with a user-mapped section open"
        } */
        //WRITE(17,100) 'YEAR = ',wyear, 'MONTH = ',month
        //100 FORMAT(1x,a,i4,1x,a,i2)
        //System.out.println(" YEAR = " + wyear + " MONTH = " + month);

//        DO i=1,daysin        // automatically done in Java
//          Qout(i) = 0.0
//        END DO

		for(int i = 1; i <= daysin; ++i) {
			if (i >= bday_roe && i <= eday_roe && Qroe > Qout[i]) {
				Qout[i] = Qroe;
				control[i] = "X2 Roe";
			}
			if (i >= bday_chs && i <= eday_chs && Qchs > Qout[i]) {
				Qout[i] = Qchs;
				control[i] = "X2 Chipps";
			}
			if (i >= bday_cnf && i <= eday_cnf && Qcnf > Qout[i]) {
				Qout[i] = Qcnf;
				control[i] = "X2 Confluence";
			}
			if (i >= bday_emt1 && i <= eday_emt1 && Qemt1 > Qout[i]) {
				Qout[i] = Qemt1;
				control[i] = "Emmaton";
			}
			if (i >= bday_emt2 && i <= eday_emt2 && Qemt2 > Qout[i]) {
				Qout[i] = Qemt2;
				control[i] = "Emmaton";
			}
			if (i >= bday_jpt1 && i <= eday_jpt1 && Qjpt1 > Qout[i]) {
				Qout[i] = Qjpt1;
				control[i] = "Jersey Point";
			}
			if (i >= bday_jpt2 && i <= eday_jpt2 && Qjpt2 > Qout[i]) {
				Qout[i] = Qjpt2;
				control[i] = "Jersey Point";
			}
			if (i >= bday_col1 && i <= eday_col1 && Qcol1 > Qout[i]) {
				Qout[i] = Qcol1;
				control[i] = "Collinsville";
			}
			if (i >= bday_col2 && i <= eday_col2 && Qcol2 > Qout[i]) {
				Qout[i] = Qcol2;
				control[i] = "Collinsville";
			}
			if (i >= bday_rsl1 && i <= eday_rsl1 && Qrsl1 > Qout[i]) {
				Qout[i] = Qrsl1;
				control[i] = "Rock Slough";
			}
			if (i >= bday_rsl2 && i <= eday_rsl2 && Qrsl2 > Qout[i]) {
				Qout[i] = Qrsl2;
				control[i] = "Rock Slough";
			}
			if (i >= bday_flw && i <= eday_flw && Qflw > Qout[i]) {
				Qout[i] = Qflw;
				control[i] = "Flow";
			}

			//WRITE(17,110)	'DAY = ',i,Qout(i),' control = ',control(i)
			//110 FORMAT(1x,a,i2,2x,f16.2,2x,a,a)
			//System.out.println(" DAY = " + i + "  " + Qout[i] + "  control = " + control[i]); // TO DO: 16.2 format
		}

		//determine what G-model is controling for each month
		if (wyear==1922 && month==1) _lastmonth = 12;
		em = 0;
		jp = 0;
		co = 0;
		rs = 0;
        for(int i = 1; i <= daysin; ++i) {
			if (control[i] == "Emmaton") em = em + 1;
			if (control[i] == "Jersey Point") jp = jp + 1;
			if (control[i] == "Collinsville") co = co + 1;
			if (control[i] == "Rock Slough") rs = rs + 1;
		}
        Gcontrol = "none";
        if(em >= 5) Gcontrol = "Emmaton";
        if(jp >= 5) Gcontrol = "Jersey_Point";
        if(co >= 5) Gcontrol = "Collinsville";
        if(rs >= 5) Gcontrol = "Rock_Slough";
//        if(month /= lastmonth) WRITE(18,111) wyear,month,Gcontrol
        //111 FORMAT(i4,x,i2,x,a)
        //if(month != _lastmonth) System.out.println(wyear + " " + month + " " + Gcontrol);
        _lastmonth = month;
        mrdo = sum(Qout)/daysin;

//      if (wyear==1994 && month==12) CLOSE(17)
//      if (wyear==1994 && month==12) CLOSE(18)
/*        if (wyear==1994 && month==12) {
        	AppUtils.deltaOutPS.close();
        	AppUtils.gOutPS.close();
        	AppUtils.deltaOutPS = null;
        	AppUtils.gOutPS = null;
        	System.gc();
        } */
        return mrdo;
	}

	private float sum(float[] array) { //CB this works even tough the array is one element (#0) longer than it should be; #0 = 0.0.
		if(array != null) {
			float sum = 0.0f;
			for(int i = 0; i < array.length; ++i) {
				sum += array[i];
			}
			return sum;
		} else {
			throw new NullPointerException("Method SUM argument cannot be null");
		}
	}
}
