package net.cdonald.googleClassroom.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;

public class RubricEntry {
	public static enum HeadingNames {
		NAME, VALUE, DESCRIPTION, AUTOMATION, AUTOMATION_FILE, AUTOMATION_METHOD
	}

	public static enum AutomationTypes {
		COMPILES, RUNS, METHOD, NONE
	}

	String name;
	String description;
	String automationFile;
	String automationMethod;
	int value;
	AutomationTypes automationType;
	Map<String, String> studentScores;

	public RubricEntry(List<Object> headings, List<Object> entries) {
		value = 0;
		automationType = AutomationTypes.NONE;
		for (int i = 0; i < headings.size(); i++) {
			if (i < entries.size()) {
				String headingName = headings.get(i).toString();

				for (HeadingNames heading : HeadingNames.values()) {

					if (heading.name().compareToIgnoreCase(headingName) == 0) {
						setValue(heading, entries.get(i).toString());
					}
				}
			}
		}
		studentScores = new HashMap<String, String>();
	}

	public String setStudentValue(String studentID, String stringValue) {
		double newValue = 0.0;

		try {
			if (stringValue != null && stringValue.length() > 0) {
				newValue = Double.parseDouble(stringValue);

			}
			if (newValue <= value) {

				studentScores.put(studentID, "" + newValue);
			}
		} catch (NumberFormatException e) {

		}
		return getStudentValue(studentID); 
	}
	
	public String getStudentValue(String studentID) {
		String displayValue = "";
		
		if (studentScores.containsKey(studentID)) {			
			String testString = studentScores.get(studentID);
			if (testString != null) {
				
				double test = Double.parseDouble(testString);
				if ((int) test == test) {
					displayValue = "" + ((int) test);
				} else {
					displayValue = "" + test;
				}
			}
		}
		return displayValue;
	}

	public void setValue(HeadingNames headingName, String param) {
		switch (headingName) {
		case NAME:
			name = param;
			break;
		case VALUE:
			value = Integer.parseInt(param);
			break;
		case DESCRIPTION:
			description = param;
			break;
		case AUTOMATION:
			for (AutomationTypes automation : AutomationTypes.values()) {
				if (automation.name().compareToIgnoreCase(param) == 0) {
					automationType = automation;
					break;
				}
			}
			break;
		case AUTOMATION_FILE:
			automationFile = param;
			break;
		case AUTOMATION_METHOD:
			automationMethod = param;
			break;
		}
	}

	public String getName() {
		return name;
	}

	public int getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	public String getAutomationFile() {
		return automationFile;
	}

	public String getAutomationMethod() {
		return automationMethod;
	}

	public AutomationTypes getAutomationType() {
		return automationType;
	}

	@Override
	public String toString() {
		return "RubricEntry [name=" + name + ", description=" + description + ", automationFile=" + automationFile
				+ ", automationMethod=" + automationMethod + ", value=" + value + ", automationType=" + automationType
				+ "]";
	}

	void runAutomation(CompilerMessage message) {

		switch (automationType) {
		case COMPILES:
			if (message.isSuccessful()) {
				studentScores.put(message.getStudentId(), "" + value);
			}
			else {
				studentScores.put(message.getStudentId(), "0");
			}
			break;
		case RUNS:
			break;
		case METHOD:
			break;
		default:
			break;
		}
	}

	public void clearStudentData() {
		studentScores.clear();		
	}
	
	public String getColumnName() {
		String header = getName();
		header = "<html>" + header + "<br>Value = " + getValue() + "</html>";
		return header;
	}
}
