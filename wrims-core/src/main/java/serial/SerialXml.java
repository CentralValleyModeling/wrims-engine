package serial;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import wrimsv2.wreslplus.elements.ModelTemp;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;

public class SerialXml {

	public static void writeModelTemp(ModelTemp m, String xPath) {

		FileWriter writer = null;

		try {
			writer = new FileWriter(xPath);

		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		CompactWriter cw = new CompactWriter(writer);

		XStream xstream = new XStream();
		xstream.marshal(m, cw);

		cw.close();

	}

	public static ModelTemp readModelTemp(String xPath) {

		File file = new File(xPath);

		XStream xstream = new XStream();

		ModelTemp out = (ModelTemp) xstream.fromXML(file);

		return out;

	}

}
