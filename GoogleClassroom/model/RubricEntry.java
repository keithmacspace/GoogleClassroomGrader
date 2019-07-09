package model;

import java.util.List;

import inMemoryJavaCompiler.CompilerMessage;

public class RubricEntry {
	public static enum HeadingNames{NAME, VALUE, DESCRIPTION, AUTOMATION, AUTOMATION_FILE, AUTOMATION_METHOD}
	public static enum AutomationTypes{COMPILES, RUNS, METHOD, NONE}
	String name;
	String description;
	String automationFile;
	String automationMethod;
	int value;
	AutomationTypes automationType;

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
	
	Double compileDone(CompilerMessage message) {
		Double newValue = null;
		switch (automationType) {
		case COMPILES:
			if (message.isSuccessful()) { 
				newValue = (double)value;
			}
			break;
		case RUNS:
			break;
		case METHOD:
			break;
		default:
			break;		
		}
		return newValue;
		
	}

}
