package net.cdonald.googleClassroom.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;

public class RubricEntry {
	public static enum HeadingNames {
		ID, NAME, VALUE, DESCRIPTION, METHOD_TO_CALL, NUM_PARAMS, AUTOMATION_TYPE, AUTOMATION_FILE, AUTOMATION_METHOD
	}

	public static enum AutomationTypes {
		NONE, COMPILES, CALL_MAIN, CALL_METHOD, RUN_CODE
	}

	String id;
	String name;
	String description;
	int value;
	AutomationTypes automationType;
	Map<String, String> studentScores;
	RubricAutomation automation;




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
	
	// This is the form used when we create it via the dialog box in addRubricEntry
	public RubricEntry() {
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
	
	public String getRubricOutput(String studentID) {
		if (automation != null) {
			automation.getRubricOutput(studentID);
		}
		return "";
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
		case AUTOMATION_TYPE:
			for (AutomationTypes automation : AutomationTypes.values()) {
				if (automation.name().compareToIgnoreCase(param) == 0) {
					automationType = automation;
					break;
				}
			}
			break;
		default:
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


	public AutomationTypes getAutomationType() {
		return automationType;
	}

	@Override
	public String toString() {
		return "RubricEntry [name=" + name + ", description=" + description + 
			  ", value=" + value + ", automationType=" + automationType
				+ "]";
	}

	void runAutomation(CompilerMessage message, StudentWorkCompiler compiler) {


		if (automation != null) {
			double score = 0.0;
			score = automation.runAutomation(message, compiler);
			score *= value;
			// Just truncate below two digits of precision
			score *= 100.0;
			score = (int)score;
			score /= 100.0;						
			studentScores.put(message.getStudentId(), "" + score);

		}
		
		else {
			switch (automationType) {
			case COMPILES:
				if (message.isSuccessful()) {
					studentScores.put(message.getStudentId(), "" + value);
				}
				else {
					studentScores.put(message.getStudentId(), "0");
				}
				break;
			default:
				break;
			}
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
	
	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}


	public void setValue(int value) {
		this.value = value;
	}

	public void setAutomationType(AutomationTypes automationType) {
		this.automationType = automationType;
	}


	public void setAutomation(RubricAutomation automation) {
		this.automation = automation;
		if (automation != null) {
			automation.setOwnerName(getName());
		}
	}
}
