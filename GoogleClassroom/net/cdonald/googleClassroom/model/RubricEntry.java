package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;

public class RubricEntry {
	public static enum HeadingNames {
		ID, NAME, VALUE, DESCRIPTION, AUTOMATION_TYPE
	}

	public static enum AutomationTypes {
		NONE, COMPILES, CALL_MAIN, CALL_METHOD, RUN_CODE
	}

	String id;
	String name;
	String description;
	int value;
	AutomationTypes automationType;
	Map<String, Double> studentScores;
	RubricAutomation automation;



	public RubricEntry(List<String> headings, List<Object> entries) {
		value = 0;
		automationType = AutomationTypes.NONE;
		for (int i = 0; i < headings.size(); i++) {
			if (i < entries.size()) {
				String headingName = headings.get(i);

				for (HeadingNames heading : HeadingNames.values()) {

					if (heading.name().compareToIgnoreCase(headingName) == 0) {
						setValue(heading, entries.get(i).toString());
					}
				}
			}
		}
		studentScores = new HashMap<String, Double>();
	}
	
	
	// This is the form used when we create it via the dialog box in addRubricEntry
	public RubricEntry() {
		automationType = AutomationTypes.NONE;
		studentScores = new HashMap<String, Double>();
	}
	
	public RubricEntry(RubricEntry other) {
		id = other.id;
		name = other.name;
		description = other.description;
		value = other.value;
		automationType = other.automationType;
		studentScores = new HashMap<String, Double>();
		for (String key : other.studentScores.keySet()) {
			studentScores.put(key, other.studentScores.get(key));
		}
		if (other.automation != null) {
			setAutomation(other.automation.newCopy());
		}
	}

	public String setStudentValue(String studentID, String stringValue) {
		double newValue = 0.0;

		try {
			if (stringValue != null && stringValue.length() > 0) {
				newValue = Double.parseDouble(stringValue);
			}
			if (newValue <= value) {
				studentScores.put(studentID, newValue);
			}
		} catch (NumberFormatException e) {

		}
		return getStudentValue(studentID); 
	}
	
	public Double getStudentDoubleValue(String studentID) {
		Double studentValue = null;
		if (studentScores.containsKey(studentID)) {			
			studentValue = studentScores.get(studentID);
		}
		return studentValue;
	}
	
	public String getStudentValue(String studentID) {
		String displayValue = "";
		
		if (studentScores.containsKey(studentID)) {			
			Double doubleValue = studentScores.get(studentID);
			if (doubleValue != null) {				
				double test = doubleValue;
				if ((int) test == test) {
					displayValue = "" + ((int) test);
				} else {
					displayValue = "" + test;
				}
			}
		}
		return displayValue;
	}
	
	public void setTableValue(HeadingNames headingName, Object param) {
		switch (headingName) {
		case NAME:
			name = (String)param;
			break;
		case VALUE:
			value = (Integer)param;
			break;
		case DESCRIPTION:
			description = (String)param;
			break;
		case AUTOMATION_TYPE:
			automationType = (AutomationTypes)param;
			switch(automationType) {
			case RUN_CODE:
				setAutomation(new RubricEntryRunCode());
				break;
			}
			break;
		default:
			break;
		}		
	}
	
	public Object getTableValue(HeadingNames headingName) {
		switch (headingName) {
		case NAME:
			return name;
		case VALUE:
			return (Integer)value;
		case DESCRIPTION:
			return description;
		case AUTOMATION_TYPE:

			return automationType;
		default:
			break;
		}
		return null;
		
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
			for (AutomationTypes automationValue : AutomationTypes.values()) {
				if (automationValue.name().compareToIgnoreCase(param) == 0) {
					automationType = automationValue;
					switch(automationValue) {
					case RUN_CODE:
						setAutomation(new RubricEntryRunCode());
						break;
						
					}
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

	void runAutomation(String studentName, CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData) {
		if (automation != null) {
			double score = 0.0;
			score = automation.runAutomation(studentName, message, compiler, consoleData);
			score *= value;
			// Just truncate below two digits of precision
			score *= 100.0;
			score = (int)score;
			score /= 100.0;						
			studentScores.put(message.getStudentId(),  score);

		}
		
		else {
			switch (automationType) {
			case COMPILES:
				if (message.isSuccessful()) {
					studentScores.put(message.getStudentId(), (double)value);
				}
				else {
					studentScores.put(message.getStudentId(), 0.0);
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


	public RubricAutomation getAutomation() {
		return automation;
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
	
	public int getNumAutomationColumns() {
		if (automation != null) {
			return automation.getNumAutomationColumns();
		}
		return 0;
	}
	
	public List<Object> getRubricEntryInfo() {
		List<Object> row = new ArrayList<Object>();
		row.add(name);
		row.add("" + value);
		row.add(description);
		if (automationType != AutomationTypes.NONE) {
			row.add(automationType.toString());
		}
		return row;
	}
	
	public static List<Object> getRubricHeader() {
		List<Object> row = new ArrayList<Object>();
		for (HeadingNames name : HeadingNames.values()) {
			if (name != HeadingNames.ID) {
				row.add(name.toString());
			}
		}
		return row;
	}


	
	public void loadAutomationColumns(Map<String, List<List<Object>>> columnData) {
	
		if (automation != null) {
			automation.loadAutomationColumns(name, columnData);
		}
	}
	
	public void saveAutomationData(List<List<Object>> columnData, Map<String, List<Object>> fileData) {
		if (automation != null) {
			automation.saveAutomationColumns(name, columnData, fileData);
		}
	}


	public void addRubricTab(List<String> rubricTabs) {
		if (automation != null) {
			rubricTabs.add(name);
		}		
	}
}
