package wrimsv2.components;

import java.util.Date;
import java.util.Properties;

public class BuildProps {
	
	private Properties buildProps = null;
	

	public BuildProps(){
		
		buildProps = new Properties();
		//buildProps.put("version", "2.0");
		try {
			buildProps.load(getClass().getResourceAsStream(
					"/wrimsv2/version.props"));
		} catch (Exception e) {
			buildProps.put("vn", "NOT BUILT EVER - DEV Env");
			buildProps.put("buildtime", "" + new Date());
			//buildProps.put("system", "OS: " + System.getProperty("os.name"));
			// e.printStackTrace();
		}
		
	}
	
	public String getVN() {
		
		return buildProps.getProperty("version");
		
	}	

}