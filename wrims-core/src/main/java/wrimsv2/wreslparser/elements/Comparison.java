package wrimsv2.wreslparser.elements;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;

public class Comparison {
	
	public static void compareFolder(Set<String> modelSet, String outParent, String expectedParent  ){
		
		for (String model : modelSet) {
			
			String outFolder = outParent + model;
			String expectedFolder = expectedParent + model;
			
			Map<String, String> actual;
			Map<String, String> expected;
			
			try {
				actual = Tools.readFilesFromDirAsMap(outFolder);
				expected = Tools.readFilesFromDirAsMap(expectedFolder);
				Assert.assertEquals(actual, expected);
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	}
	
	
}
