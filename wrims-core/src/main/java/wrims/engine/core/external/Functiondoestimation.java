package wrims.engine.core.external;

import java.util.*;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.Session.Runner;

public class Functiondoestimation extends ExternalFunction{
	private final boolean DEBUG = false;
	static {
		// set TensorFlow native library debug property
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");
	
	ProcessBuilder pb = new ProcessBuilder("myCommand", "myArg");
	Map<String, String> env = pb.environment();
	env.put("TF_CPP_MIN_LOG_LEVEL", "3");
	}

	public Functiondoestimation(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
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
		int wyt_SAC = ((Number) param17).intValue();
		int month = ((Number) param16).intValue();
		float I_FOL_Prv2 = ((Number) param15).floatValue();
		float I_OROVL_Prv2 = ((Number) param14).floatValue();
		float I_SHSTA_Prv2 = ((Number) param13).floatValue();
		float I_WKYTN_Prv2 = ((Number) param12).floatValue();
		float I_TRNTY_Prv2 = ((Number) param11).floatValue();
		float I_FOL_Prv1 = ((Number) param10).floatValue();
		float I_OROVL_Prv1 = ((Number) param9).floatValue();
		float I_SHSTA_Prv1 = ((Number) param8).floatValue();
		float I_WKYTN_Prv1 = ((Number) param7).floatValue();
		float I_TRNTY_Prv1 = ((Number) param6).floatValue();
		float I_FOL = ((Number) param5).floatValue();
		float I_OROVL = ((Number) param4).floatValue();
		float I_SHSTA = ((Number) param3).floatValue();
		float I_WKYTN = ((Number) param2).floatValue();
		float I_TRNTY = ((Number) param1).floatValue();

		float result = doestimation(I_TRNTY, I_WKYTN, I_SHSTA, I_OROVL, I_FOL, I_TRNTY_Prv1, I_WKYTN_Prv1, I_SHSTA_Prv1, I_OROVL_Prv1, I_FOL_Prv1, I_TRNTY_Prv2, I_WKYTN_Prv2, I_SHSTA_Prv2, I_OROVL_Prv2, I_FOL_Prv2, month, wyt_SAC);

		// push the result on the Stack
		stack.push(new Float(result));

	}

	public float doestimation(float I_TRNTY, float I_WKYTN, float I_SHSTA, float I_OROVL, float I_FOL, float I_TRNTY_Prv1, float I_WKYTN_Prv1, float I_SHSTA_Prv1, float I_OROVL_Prv1, float I_FOL_Prv1, float I_TRNTY_Prv2, float I_WKYTN_Prv2, float I_SHSTA_Prv2, float I_OROVL_Prv2, float I_FOL_Prv2, int month, int wyt_SAC){
        
		SavedModelBundle model = SavedModelBundle.load(externalDir+"model", "serve");
        Session s = model.session();
        // the first 15 feature are considered as float values.
        float[][] rawdata = new float[][] { { 
        	I_TRNTY, I_WKYTN, I_SHSTA, I_OROVL, I_FOL, 
        	I_TRNTY_Prv1, I_WKYTN_Prv1, I_SHSTA_Prv1, I_OROVL_Prv1, I_FOL_Prv1, 
        	I_TRNTY_Prv2, I_WKYTN_Prv2, I_SHSTA_Prv2, I_OROVL_Prv2, I_FOL_Prv2 } }; // Until the 15th feature
        long[][] rawdata_int = new long[][] { { month, wyt_SAC } }; // The 16th and 17th features as integers
        
                String[] tensorNames = new String[] {
        		"serving_default_trinity_inflow_input:0",
        		"serving_default_whiskeytown_inflow_input:0",
        		"serving_default_shasta_inflow_input:0",
        		 "serving_default_oroville_inflow_input:0",
        		"serving_default_folsom_inflow_input:0",
        		"serving_default_trinity_inflow_1m_input:0",
        		"serving_default_whiskeytown_inflow_1m_input:0",
        		 "serving_default_shasta_inflow_1m_input:0",
        		"serving_default_oroville_inflow_1m_input:0",
        		"serving_default_folsom_inflow_1m_input:0",
        		 "serving_default_trinity_inflow_2m_input:0",
        		 "serving_default_whiskeytown_inflow_2m_input:0",
        		 "serving_default_shasta_inflow_2m_input:0",
        		"serving_default_oroville_inflow_2m_input:0",
        		"serving_default_folsom_inflow_2m_input:0",
        };

        String[] tensorNames_int = new String[] {
                "serving_default_wy_month_input:0",
                "serving_default_wyt_sac_input:0"
            };

        Runner runner = s.runner();

        int nFeatures = tensorNames.length;
        for (int i = 0; i < nFeatures; i++) {
            float[][] featureData = new float[][] { { rawdata[0][i] } };
            Tensor<Float> input = Tensor.create(featureData, Float.class);
            runner.feed(tensorNames[i], input);
        }

        int nFeatures_int = tensorNames_int.length;
        for (int i = 0; i < nFeatures_int; i++) {
            long[][] featureData_int = new long[][] { { rawdata_int[0][i] } };
            Tensor<Long> input_int = Tensor.create(featureData_int, Long.class);
            runner.feed(tensorNames_int[i], input_int);
        }

        Tensor result = runner.fetch("StatefulPartitionedCall_2:0").run().get(0);
        float[][] out = new float[1][1];
        result.copyTo(out);
        //System.out.println("results: " + out[0][0]);
        model.close();
        return out[0][0];
	}
}
