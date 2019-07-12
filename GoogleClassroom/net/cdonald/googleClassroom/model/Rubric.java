package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;

public class Rubric {
	String name;

	String rubricID;
	List<RubricEntry> entries;
	boolean finalized;
	public Rubric(String name, String rubricID) {
		super();
		this.name = name;
		this.rubricID = rubricID;
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
		return name;
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
		return name;
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
	
	public void runAutomation(CompilerMessage message) {				
		for (RubricEntry entry : entries) {
			entry.runAutomation(message);
		}

	}
	
	public void clearStudentData() {
		for (RubricEntry entry : entries) {
			entry.clearStudentData();
		}
	}
	

	

}
