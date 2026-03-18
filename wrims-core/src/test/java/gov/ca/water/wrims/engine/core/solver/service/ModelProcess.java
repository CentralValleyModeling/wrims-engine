package gov.ca.water.wrims.engine.core.solver.service;

import java.util.ArrayList;
import java.util.Map;

import gov.ca.water.wrims.engine.core.commondata.wresldata.ModelDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.StudyDataSet;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.ilp.ILP;

public class ModelProcess
{
	/**
	 * Method imitating the `runModelILP` method of `ControllerBatch`
	 * @param studyDataSet the input data
	 */
	public void runModel(StudyDataSet studyDataSet)
	{
		ILP.initializeIlp();

		// appropriate models to process from the study
		ArrayList<String> modelList = studyDataSet.getModelList();
		Map<String, ModelDataSet> modelDataSetMap = studyDataSet.getModelDataSetMap();

		// Solver name provided by configuration file
		Solver solver = SolverBroker.findSolver(ControlData.solverName);

		// initialize the solver's state
		solver.init();

		try {
			for (String modelName : modelList)
			{
				ModelDataSet mds = modelDataSetMap.get(modelName);

				// ...
				// More setup for the model processing
 				// ...

				ControlData.currSvMap=mds.svMap;
				ControlData.currSvFutMap=mds.svFutMap;
				ControlData.currDvMap=mds.dvMap;
				ControlData.currDvSlackSurplusMap=mds.dvSlackSurplusMap;
				ControlData.currAliasMap=mds.asMap;
				ControlData.currGoalMap=mds.gMap;
				ControlData.currTsMap=mds.tsMap;
				ControlData.isPostProcessing=false;
				mds.processModel();

				// ...
				// More setup for the model processing
				// ...

				ILP.closeCplexLpFile();

				solver.setLP(ILP.cplexLpFilePath);

				solver.solve();

				ILP.closeIlpFile();
			}
		}
		finally
		{
			// close the solver and log any statistics
			solver.close();
		}

	}
}
