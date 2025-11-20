package gov.ca.water.wrims.engine.core.external;

import java.util.*;

import gov.ca.water.wrims.engine.core.components.TimeUsage;
import calsim.surrogate.SalinitySurrogateManager;


public class Functionsurrogateec extends ExternalFunction{
	private final boolean DEBUG = false;
	private static int cpuTime=0;
	private static int nCalls=0;
	private SalinitySurrogateManager ssm;

	public Functionsurrogateec(){
		long t1 = Calendar.getInstance().getTimeInMillis();
		
		ssm=SalinitySurrogateSetup.getManager();
		long t2 = Calendar.getInstance().getTimeInMillis();
		cpuTime=cpuTime+(int) (t2-t1);
		nCalls++;
		TimeUsage.cpuTimeMap.put("surrogateec", cpuTime);
		TimeUsage.nCallsMap.put("surrogateec", nCalls);
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
		TimeUsage.cpuTimeMap.put("surrogateec", cpuTime);
		TimeUsage.nCallsMap.put("surrogateec", nCalls);
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
	

}