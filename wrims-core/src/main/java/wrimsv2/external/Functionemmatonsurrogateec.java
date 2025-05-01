package wrimsv2.external;

import java.util.*;

import wrimsv2.components.TimeUsage;
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
import calsim.surrogate.examples.EmmatonExampleTensorFlowANN;
import calsim.surrogate.examples.SalinitySurrogateManager;

public class Functionemmatonsurrogateec extends ExternalFunction{
	private final boolean DEBUG = false;
	private static int cpuTime=0;
	private static int nCalls=0;
	private SalinitySurrogateManager ssm;

	public Functionemmatonsurrogateec(){
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
		TimeUsage.cpuTimeMap.put("emmatonsurrogateec", cpuTime);
		TimeUsage.nCallsMap.put("emmatonsurrogateec", nCalls);
	}

	public void execute(Stack stack) {

		long t1 = Calendar.getInstance().getTimeInMillis();
		
		//values in reverse order:
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
		int year = ((Number) param11).intValue();
		int month = ((Number) param10).intValue();
		int ave_type = ((Number) param9).intValue();
		int variable = ((Number) param8).intValue();
		int location = ((Number) param7).intValue();
		Number[] smscg_Arr = (Number[])param6;
		int size_smscg=smscg_Arr.length;
		double[] smscg=new double[size_smscg];
		for (int i=0; i<size_smscg; i++){
			smscg[i]=smscg_Arr[i].doubleValue();
		}
		Number[] sjr_Arr = (Number[])param5;
		int size_sjr=sjr_Arr.length;
		double[] sjr=new double[size_sjr];
		for (int i=0; i<size_sjr; i++){
			sjr[i]=sjr_Arr[i].doubleValue();
		}
		Number[] net_dcd_Arr = (Number[])param4;
		int size_net_dcd=net_dcd_Arr.length;
		double[] net_dcd=new double[size_net_dcd];
		for (int i=0; i<size_net_dcd; i++){
			net_dcd[i]=net_dcd_Arr[i].doubleValue();
		}
		Number[] dcc_Arr = (Number[])param3;
		int size_dcc=dcc_Arr.length;
		double[] dcc=new double[size_dcc];
		for (int i=0; i<size_dcc; i++){
			dcc[i]=dcc_Arr[i].doubleValue();
		}
		Number[] exp_Arr = (Number[])param2;
		int size_exp=exp_Arr.length;
		double[] exp=new double[size_exp];
		for (int i=0; i<size_exp; i++){
			exp[i]=exp_Arr[i].doubleValue();
		}
		Number[] sac_Arr = (Number[])param1;
		int size_sac=sac_Arr.length;
		double[] sac=new double[size_sac];
		for (int i=0; i<size_sac; i++){
			sac[i]=sac_Arr[i].doubleValue();
		}

		float result = surrogateec(sac, exp, dcc, net_dcd, sjr, smscg, location, variable, ave_type, month, year);

		// push the result on the Stack
		stack.push(new Double(result));
		
		long t2 = Calendar.getInstance().getTimeInMillis();
		cpuTime=cpuTime+(int) (t2-t1);
		nCalls++;
		TimeUsage.cpuTimeMap.put("emmatonsurrogateec", cpuTime);
		TimeUsage.nCallsMap.put("emmatonsurrogateec", nCalls);
	}

	public float surrogateec(double[] sac, double[] exp, double[] dcc, double[] net_dcd, double[] sjr, double[] smscg, int location, int variable, int ave_type, int month, int year){	
		
		double[][] sac1 = { sac };
		double[][] exp1 = { exp };

		double[][] dcc1 = { dcc };
		double[][] net_dcd1 = { net_dcd };
		double[][] sjr1 = { sjr };
		double[][] tide = { { 6.560, 6.184, 5.508, 5.083, 6.913 } };
		double[][] smscg1 = { smscg };
		
		if (DEBUG){
			System.out.println("sac " + Arrays.toString(sac));
			System.out.println("exp " + Arrays.toString(exp));
			System.out.println("dcc " + Arrays.toString(dcc));
			System.out.println("net_dcd " + Arrays.toString(net_dcd));
			System.out.println("sjr " + Arrays.toString(sjr));
			System.out.println("smscg " + Arrays.toString(smscg));
			System.out.println("Month " + month);
			System.out.println("Year " + year);
		}
		
		ArrayList<double[][]> floatInput = new ArrayList<double[][]>();
		floatInput.add(sac1);
		floatInput.add(exp1);
		floatInput.add(dcc1);
		floatInput.add(net_dcd1);
		floatInput.add(sjr1);
		floatInput.add(tide);
		floatInput.add(smscg1);

		float out = ssm.annEC(floatInput, location, variable, ave_type, month, year);
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
