package main;

import java.io.IOException;
import org.antlr.runtime.RecognitionException;

import wrimsv2.wreslparser.elements.StudyUtils;

public class WreslCheck {

	public static void main(String[] args) throws RecognitionException, IOException {

		StudyUtils.checkStudy(args[0]);

	}

}
