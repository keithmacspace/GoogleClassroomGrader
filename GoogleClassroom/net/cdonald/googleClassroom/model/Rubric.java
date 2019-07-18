package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.List;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;

public class Rubric {
	GoogleSheetData sheetData;
	List<RubricEntry> entries;
	boolean inModifiedState;
	public Rubric(GoogleSheetData sheetData) {
		super();
		this.sheetData = sheetData;
		entries = new ArrayList<RubricEntry>();
	}
	
	// This form is used when we are creating a new rubric from scratch
	public Rubric() {
		inModifiedState = true;
		entries = new ArrayList<RubricEntry>();
	}
	

	public void addEntries(List<List<Object> > sheetEntries) {
		if (sheetEntries.size() > 0) {
			List<Object> headings = sheetEntries.get(0);
			for (int i = 1; i < sheetEntries.size(); i++) {
				addEntry(new RubricEntry(headings, sheetEntries.get(i)));
			}
		}
	}
	
	public String getName() {
		return sheetData.getName();
	}
	
	public String getSpreadsheetId() {
		return sheetData.getSpreadsheetId();
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}
	
	public void addEntry(RubricEntry entry) {
		entries.add(entry);
	}
	
	public void removeEntry(String name) {
		for (int i = 0; i < entries.size(); i++) {
			RubricEntry entry = entries.get(i);
			if (entry.getName().compareToIgnoreCase(name) == 0){
				entries.remove(i);
				break;
			}
		}
	}
	@Override
	public String toString() {
		return sheetData.getName();
	}

	public List<RubricEntry> getEntries() {
		return entries;
	}
	
	public RubricEntry getEntry(int index) {
		if (index >= 0 && index < entries.size()) {
			return entries.get(index);
		}
		return null;
	}
	
	public void runAutomation(CompilerMessage message, StudentWorkCompiler compiler) {				
		for (RubricEntry entry : entries) {
			entry.runAutomation(message, compiler);
		}

	}
	
	public void clearStudentData() {
		for (RubricEntry entry : entries) {
			entry.clearStudentData();
		}
	}

	public boolean isInModifiedState() {
		return inModifiedState;
	}

	public void setInModifiedState(boolean inModifiedState) {
		this.inModifiedState = inModifiedState;
	}
	
	public String getRubricOutput(String name, String studentID) {
		for (RubricEntry entry : entries) {
			if (entry.getName().compareToIgnoreCase(name) == 0) {
				return entry.getRubricOutput(studentID);
			}
		}
		return "";
	}
	
	

	

}
