/*
package wrims.engine.core.tf;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Session.Runner;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;

public class TF {

    public static void main(String[] args) throws Exception {

        System.out.println("TensorFlow version: " + TensorFlow.version());

        SavedModelBundle model = SavedModelBundle.load("D:\\TensorFlow\\ANN_Models1\\calsim_delta_outflow_estimator", "serve");
        Session s = model.session();
        float[][] rawdata = new float[][] { { -0.317928304f,	0.053429297f,	0.291882696f,	0.16009989f,	0.347007481f,	
        	-0.575868808f,	0.159116005f,	0.132040745f,	0.11235005f,	0.470868431f,	
        	-0.69574763f,	0.165035588f,	-0.492836345f,	-0.485022216f,	-0.454307444f,	
        	1.f,	0.f,	0.f,	0.f,	0.f,	
        	0.f,	0.f,	0.f,	0.f,	0.f,
        	0.f,	0.f,	1.f,	0.f,	0.f,	0.f,	0.f} };
           
        
        Tensor<Float> input = Tensor.create(rawdata, Float.class);
        long[] sx = input.shape();
        System.out.println("input shape: " + sx[0] + " " + sx[1]);
        // Tensor.create([null,32], FloatBuffer.wrap(null))
        Runner runner = s.runner().feed("serving_default_input_preprocessed_input:0", input);
        Tensor result = runner.fetch("StatefulPartitionedCall:0").run().get(0);
        float[][] out = new float[1][1];
        result.copyTo(out);
        System.out.println("results: " + out[0][0]);
        model.close();
    }

}
*/


// the new version of the code is as follows which implements the raw data from CALSIM inputs:

package gov.ca.water.wrims.engine.core.tf;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Session.Runner;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;

public class TF {

    public static void main(String[] args) throws Exception {

        System.out.println("TensorFlow version: " + TensorFlow.version());

        SavedModelBundle model = SavedModelBundle.load("D:\\TensorFlow\\model", "serve");
        Session s = model.session();
        // the first 15 feature are considered as float values.
        float[][] rawdata = new float[][] { { 35.2f,
            25.3f,	332.0f,	191.6f,	127.3f,	20.6f,
            3.2f,	235.0f,	99.9f,	36.1f,	10.1f,
            0.6f,	230.0f,	88.7f,	17.8f } }; // Until the 15th feature
        long[][] rawdata_int = new long[][] { { 3, 2 } }; // The 16th and 17th features as integers
        
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
        System.out.println("results: " + out[0][0]);
        model.close();
    }
}
