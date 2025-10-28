/*
 * Water Resource Integrated Modeling System (WRIMS) Copyright (c) 2024.
 *
 * WRIMS 2 is copyrighted by the State of California Department of Water Resources.
 * It is licensed under the Eclipse Public License, Version 1.0.
 * See Eclipse Public License for more details.
 */

package wrimsv2.testWRESL;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

import org.antlr.runtime.RecognitionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.components.ControlData;
import wrimsv2.evaluator.TimeOperation;
import wrimsv2.wreslparser.elements.StudyUtils;

final class WRESLTest {

	private static StudyDataSet sds;
	
	@BeforeAll
	static void compileWRESL() throws RecognitionException, IOException{
		StudyUtils.useWreslPlus=true;
    	String mainFilePath= "C:\\9.3.1_danube_adj\\Run\\mainCS3_ReOrg_UWplusVF.wresl";   	
		sds = StudyUtils.checkStudy(mainFilePath);
	}
	
    @CsvSource({"38"})
    @ParameterizedTest
    void testCycleNumber(String numOfCyclesStr) throws RecognitionException, IOException {
    	
    	int numOfCycles=Integer.parseInt(numOfCyclesStr);
    	int numOfCyclesInModel = sds.getModelList().size();

    	assertTrue(numOfCyclesInModel==numOfCycles, "Number of cycles in the study is  "+numOfCycles);
    }
	
    @CsvSource({"GENTables"})
    @ParameterizedTest
    void testCycleName(String cycleName) throws RecognitionException, IOException {    	
    	int cycleIndex=1;
    	String cycleNameInModel = sds.getModelList().get(cycleIndex-1);

    	assertTrue(cycleNameInModel.equalsIgnoreCase(cycleName), "Cycle "+cycleIndex+" is named "+cycleName);
    }

}
