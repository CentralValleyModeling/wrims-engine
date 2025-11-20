package gov.ca.water.wrims.engine.main;

import java.io.IOException;
import org.antlr.runtime.RecognitionException;

import gov.ca.water.wrims.engine.core.wreslparser.elements.StudyUtils;

public class WreslCheck {

	public static void main(String[] args) throws RecognitionException, IOException {

		StudyUtils.checkStudy(args[0]);

	}

}
