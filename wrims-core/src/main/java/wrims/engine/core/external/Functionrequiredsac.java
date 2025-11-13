package wrims.engine.core.external;

import java.util.*;

import calsim.surrogate.SalinitySurrogateManager;
import wrims.engine.core.components.TimeUsage;

public class Functionrequiredsac extends ExternalFunction{
	private final boolean DEBUG = false;
	private static int cpuTime=0;
	private static int nCalls=0;
	private SalinitySurrogateManager ssm;


	public Functionrequiredsac(){
		long t1 = Calendar.getInstance().getTimeInMillis();
		ssm=SalinitySurrogateSetup.getManager();
		long t2 = Calendar.getInstance().getTimeInMillis();
		cpuTime=cpuTime+(int) (t2-t1);
		nCalls++;
		TimeUsage.cpuTimeMap.put("requiredsac", cpuTime);
		TimeUsage.nCallsMap.put("requiredsac", nCalls);
	}

	public void execute(Stack stack) {
		
		long t1 = Calendar.getInstance().getTimeInMillis();
		
		//values in reverse order:
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
		int year = ((Number) param19).intValue();
		int month = ((Number) param18).intValue();
		int ave_type = ((Number) param17).intValue();
		int location = ((Number) param16).intValue();
		double sacHiBound = ((Number) param15).doubleValue();
		double sacLoBound = ((Number) param14).doubleValue();
		double SMSCG_fut = ((Number) param13).doubleValue();
		Number[] SMSCG_prv_Arr = (Number[])param12;
		int size_SMSCG_prv=SMSCG_prv_Arr.length;
		double[] SMSCG_prv=new double[size_SMSCG_prv];
		for (int i=0; i<size_SMSCG_prv; i++){
			SMSCG_prv[i]=SMSCG_prv_Arr[i].doubleValue();
		}
		double DICU_fut = ((Number) param11).doubleValue();
		Number[] DICU_prv_Arr = (Number[])param10;
		int size_DICU_prv=DICU_prv_Arr.length;
		double[] DICU_prv=new double[size_DICU_prv];
		for (int i=0; i<size_DICU_prv; i++){
			DICU_prv[i]=DICU_prv_Arr[i].doubleValue();
		}
		double DXC_fut = ((Number) param9).doubleValue();
		Number[] DXC_prv_Arr = (Number[])param8;
		int size_DXC_prv=DXC_prv_Arr.length;
		double[] DXC_prv=new double[size_DXC_prv];
		for (int i=0; i<size_DXC_prv; i++){
			DXC_prv[i]=DXC_prv_Arr[i].doubleValue();
		}
		double Qsjr_fut = ((Number) param7).doubleValue();
		Number[] Qsjr_prv_Arr = (Number[])param6;
		int size_Qsjr_prv=Qsjr_prv_Arr.length;
		double[] Qsjr_prv=new double[size_Qsjr_prv];
		for (int i=0; i<size_Qsjr_prv; i++){
			Qsjr_prv[i]=Qsjr_prv_Arr[i].doubleValue();
		}
		double Qexp_est = ((Number) param5).doubleValue();
		Number[] Qexp_prv_Arr = (Number[])param4;
		int size_Qexp_prv=Qexp_prv_Arr.length;
		double[] Qexp_prv=new double[size_Qexp_prv];
		for (int i=0; i<size_Qexp_prv; i++){
			Qexp_prv[i]=Qexp_prv_Arr[i].doubleValue();
		}
		double Qsac_est = ((Number) param3).doubleValue();
		Number[] Qsac_prv_Arr = (Number[])param2;
		int size_Qsac_prv=Qsac_prv_Arr.length;
		double[] Qsac_prv=new double[size_Qsac_prv];
		for (int i=0; i<size_Qsac_prv; i++){
			Qsac_prv[i]=Qsac_prv_Arr[i].doubleValue();
		}
		double target = ((Number) param1).doubleValue();

		double result = requiredsac(target, Qsac_prv, Qsac_est, Qexp_prv, Qexp_est, Qsjr_prv, Qsjr_fut, DXC_prv, DXC_fut, DICU_prv, DICU_fut, SMSCG_prv, SMSCG_fut, sacLoBound, sacHiBound, location, ave_type, month, year);

		// push the result on the Stack
		stack.push(new Double(result));
		
		long t2 = Calendar.getInstance().getTimeInMillis();
		cpuTime=cpuTime+(int) (t2-t1);
		nCalls++;
		TimeUsage.cpuTimeMap.put("requiredsac", cpuTime);
		TimeUsage.nCallsMap.put("requiredsac", nCalls);

	}

	public double requiredsac(double target, double[] Qsac_prv, double Qsac_est, double[] Qexp_prv, double Qexp_est, double[] Qsjr_prv, double Qsjr_fut, double[] DXC_prv, double DXC_fut, double[] DICU_prv, double DICU_fut, double[] SMSCG_prv, double SMSCG_fut, double sacLoBound, double sacHiBound, int location, int ave_type, int month, int year){
		int NHIST = 5;
		int NLOC = 7; // TODO move to config?
		double[][] sac = new double[1][NHIST];
		double[][] exp = new double[1][NHIST];
		double[][] dcc = new double[1][NHIST];
		double[][] dcd = new double[1][NHIST];
		double[][] sjr = new double[1][NHIST];
		double[][] tide = new double[1][NHIST];
		double[][] smscg = new double[1][NHIST];

		// Batching isn't very useful here, and thus hard to remember what this index
		// is for. This is just a reminder it is for batching.
		final int BATCHZERO = 0;
		sac[BATCHZERO][0] = (double) (Qsac_est);
		exp[BATCHZERO][0] = (double) (Qexp_est);
		dcc[BATCHZERO][0] = (double) DXC_fut;
		dcd[BATCHZERO][0] = (double) DICU_fut;
		sjr[BATCHZERO][0] = (double) Qsjr_fut;
		tide[BATCHZERO][0] = -999.;
		smscg[BATCHZERO][0] = (double) SMSCG_fut;

		for (int ihist = 1; ihist < NHIST; ihist++) {
			sac[BATCHZERO][ihist] = (double) (Qsac_prv[ihist-1]); // Add "other" to "regular" Sac
			exp[BATCHZERO][ihist] = (double) (Qexp_prv[ihist-1]); 
			dcc[BATCHZERO][ihist] = (double) DXC_prv[ihist-1];
			dcd[BATCHZERO][ihist] = (double) DICU_prv[ihist-1];
			sjr[BATCHZERO][ihist] = (double) Qsjr_prv[ihist-1];
			tide[BATCHZERO][ihist] = 0.;
			smscg[BATCHZERO][ihist] = (double) SMSCG_prv[ihist-1];
		}

		ArrayList<double[][]> monthlyInput = new ArrayList<double[][]>(
				Arrays.asList(sac, exp, dcc, dcd, sjr, tide, smscg));
		
		float out = (float) ssm.requiredSac(target, monthlyInput, sacLoBound, sacHiBound, location, ave_type, month, year);	
		
		return out;
	}
}
