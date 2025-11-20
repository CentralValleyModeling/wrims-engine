package gov.ca.water.wrims.engine.core.external;

import java.util.*;

import calsim.surrogate.AggregateMonths;
import calsim.surrogate.DailyToSurrogate;
import calsim.surrogate.DailyToSurrogateBlocked;
import calsim.surrogate.DisaggregateMonths;
import calsim.surrogate.DisaggregateMonthsDaysToOps;
import calsim.surrogate.DisaggregateMonthsRepeat;
import calsim.surrogate.DisaggregateMonthsSpline;
import calsim.surrogate.Surrogate;
import calsim.surrogate.SurrogateMonth;
import calsim.surrogate.TensorWrapper;
import calsim.surrogate.examples.SalinitySurrogateManager;
import gov.ca.water.wrims.engine.core.components.TimeUsage;

public class Functionemmatonsurrogatelinegen extends ExternalFunction{
	private final boolean DEBUG = false;
	private static int cpuTime=0;
	private static int nCalls=0;
	private SalinitySurrogateManager ssm;
	
	public Functionemmatonsurrogatelinegen(){
		long t1 = Calendar.getInstance().getTimeInMillis();
		ssm=SalinitySurrogateManager.INSTANCE;
		//set up an ANN surrogate month for emmaton	
		int location = ssm.EMM_CALSIM;
		int aveType = ssm.MEAN;
		DisaggregateMonths spline = new DisaggregateMonthsSpline(5);
		DisaggregateMonths repeat = new DisaggregateMonthsRepeat(5);
		DisaggregateMonths daysOps = new DisaggregateMonthsDaysToOps(5, 1., 0.);
		DisaggregateMonths[] disagg = { spline, spline, daysOps, spline, spline, spline, repeat };
		Surrogate emm = emmatonANN();
		AggregateMonths agg = AggregateMonths.MONTHLY_MEAN;
		SurrogateMonth month = new SurrogateMonth(disagg, emm, agg);
		ssm.setSurrogateForSite(location, aveType, month);
		
		long t2 = Calendar.getInstance().getTimeInMillis();
		cpuTime=cpuTime+(int) (t2-t1);
		nCalls++;
		TimeUsage.cpuTimeMap.put("emmatonsurrogatelinegen", cpuTime);
		TimeUsage.nCallsMap.put("emmatonsurrogatelinegen", nCalls);
	}

	public void execute(Stack stack) {

		long t1 = Calendar.getInstance().getTimeInMillis();
		
		//values in reverse order:
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
		int currYear = ((Number) param18).intValue();
		int currMonth = ((Number) param17).intValue();
		int ave_type = ((Number) param16).intValue();
		int variable = ((Number) param15).intValue();
		int location = ((Number) param14).intValue();
		double ECTARGET = ((Number) param13).doubleValue();
		double SMSCG_fut = ((Number) param12).doubleValue();
		Number[] SMSCG_prv_Arr = (Number[])param11;
		int size_SMSCG_prv=SMSCG_prv_Arr.length;
		double[] SMSCG_prv=new double[size_SMSCG_prv];
		for (int i=0; i<size_SMSCG_prv; i++){
			SMSCG_prv[i]=SMSCG_prv_Arr[i].doubleValue();
		}
		double DICU_fut = ((Number) param10).doubleValue();
		Number[] DICU_prv_Arr = (Number[])param9;
		int size_DICU_prv=DICU_prv_Arr.length;
		double[] DICU_prv=new double[size_DICU_prv];
		for (int i=0; i<size_DICU_prv; i++){
			DICU_prv[i]=DICU_prv_Arr[i].doubleValue();
		}
		double DXC_fut = ((Number) param8).doubleValue();
		Number[] DXC_prv_Arr = (Number[])param7;
		int size_DXC_prv=DXC_prv_Arr.length;
		double[] DXC_prv=new double[size_DXC_prv];
		for (int i=0; i<size_DXC_prv; i++){
			DXC_prv[i]=DXC_prv_Arr[i].doubleValue();
		}
		double Qsjr_fut = ((Number) param6).doubleValue();
		Number[] Qsjr_prv_Arr = (Number[])param5;
		int size_Qsjr_prv=Qsjr_prv_Arr.length;
		double[] Qsjr_prv=new double[size_Qsjr_prv];
		for (int i=0; i<size_Qsjr_prv; i++){
			Qsjr_prv[i]=Qsjr_prv_Arr[i].doubleValue();
		}
		double Qexp_est = ((Number) param4).doubleValue();
		Number[] Qexp_prv_Arr = (Number[])param3;
		int size_Qexp_prv=Qexp_prv_Arr.length;
		double[] Qexp_prv=new double[size_Qexp_prv];
		for (int i=0; i<size_Qexp_prv; i++){
			Qexp_prv[i]=Qexp_prv_Arr[i].doubleValue();
		}
		double Qsac_est = ((Number) param2).doubleValue();
		Number[] Qsac_prv_Arr = (Number[])param1;
		int size_Qsac_prv=Qsac_prv_Arr.length;
		double[] Qsac_prv=new double[size_Qsac_prv];
		for (int i=0; i<size_Qsac_prv; i++){
			Qsac_prv[i]=Qsac_prv_Arr[i].doubleValue();
		}

		double result = emmatonsurrogatelinegen(Qsac_prv, Qsac_est, Qexp_prv, Qexp_est, Qsjr_prv, Qsjr_fut, DXC_prv, DXC_fut, DICU_prv, DICU_fut, SMSCG_prv, SMSCG_fut, ECTARGET, location, variable, ave_type, currMonth, currYear);

		// push the result on the Stack
		stack.push(new Double(result));
		
		long t2 = Calendar.getInstance().getTimeInMillis();
		cpuTime=cpuTime+(int) (t2-t1);
		nCalls++;
		TimeUsage.cpuTimeMap.put("emmatonsurrogatelinegen", cpuTime);
		TimeUsage.nCallsMap.put("emmatonsurrogatelinegen", nCalls);
	}

	public double emmatonsurrogatelinegen(double[] Qsac_prv, double Qsac_est, double[] Qexp_prv, double Qexp_est, double[] Qsjr_prv, double Qsjr_fut, double[] DXC_prv, double DXC_fut, double[] DICU_prv, double DICU_fut, double[] SMSCG_prv, double SMSCG_fut, double ECTARGET, int location, int variable, int ave_type, int currMonth, int currYear){
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

		float out = (float) ssm.lineGenImpl(monthlyInput, location, variable, ave_type, currMonth, currYear);	
		
		return out;
	}
	
	public static Surrogate emmatonANN() {
		String fname = externalDir+ "ann_calsim-main/emmaton";
		String[] tensorNames = { "serving_default_sac_input:0", "serving_default_exports_input:0",
				"serving_default_dcc_input:0", "serving_default_net_dcd_input:0", "serving_default_sjr_input:0",
				"serving_default_tide_input:0", "serving_default_smscg_input:0", };

		String[] tensorNamesInt = new String[0];
		String outName = "StatefulPartitionedCall:0";
		DailyToSurrogate dayToANN = new DailyToSurrogateBlocked(8, 10, 11);
		Surrogate wrap = new TensorWrapper(fname, tensorNames, tensorNamesInt, outName, dayToANN);
		return wrap;
	}
}
