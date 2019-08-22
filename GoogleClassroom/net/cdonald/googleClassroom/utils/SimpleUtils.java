package net.cdonald.googleClassroom.utils;

import java.util.ArrayList;
import java.util.List;

public class SimpleUtils {
	public static List<String> breakUpCommaList(Object object) {
		List<String> partsList = new ArrayList<String>();
		if (object instanceof String) {
			String [] parts = ((String)object).split(",");
			for (String part : parts) {
				partsList.add(part.trim());
			}			
		}
		return partsList;
		
	}
}
