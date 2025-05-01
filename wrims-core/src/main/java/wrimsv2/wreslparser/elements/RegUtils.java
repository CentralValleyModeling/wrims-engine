package wrimsv2.wreslparser.elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegUtils {
	
	
	public static String getLastMatch(String inputStr, String patternStr) {

		// String patternStr = "([a-zA-Z]+[0-9]+)";
		String output = null;

		int total = 0;
		// Compile regular expression
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(inputStr);

		// Count all occurrences of pattern in input
		while (matcher.find()) {

			total = total + 1;
			output = matcher.group();
		}
		return output;
	}

	
	

    public static String replaceWith(String inputStr, String patternStr, String replaceStr ) {


        // Compile regular expression
        Pattern pattern = Pattern.compile(patternStr);

        // Replace all occurrences of pattern in input
        Matcher matcher = pattern.matcher(inputStr);
        String output = matcher.replaceAll(replaceStr);

		return output;

    }
    
    public static int timesOfMatches(String inputStr, String patternStr)
    {
    	//String patternStr = "([a-zA-Z]+[0-9]+)";
    	
    	int total = 0;
    	// Compile regular expression
    	Pattern pattern = Pattern.compile(patternStr);
    	Matcher matcher = pattern.matcher(inputStr);

    	// Count all occurrences of pattern in input
    	while (matcher.find()) {

    		total = total +1;
    	}

		return total;
    }

	
}
