package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;

public class Rubric {
	String name;
	String urlID;
	int rubricID;
	List<RubricEntry> entries;
	public Rubric(String urlID, String name, int rubricID) {
		super();
		this.name = name;
		this.urlID = urlID;
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
	
	public Map<Integer, Double> compileDone(CompilerMessage message, int startColumn) {
		Map<Integer, Double> columnsToChange = null;
		
		int columnNumber = startColumn;
		for (RubricEntry entry : entries) {
			Double newValue = entry.compileDone(message);
			if (newValue != null) {
				if (columnsToChange != null) {
					columnsToChange = new HashMap<Integer, Double>();
				}
				columnsToChange.put(columnNumber, newValue);
				columnNumber++;
			}
		}
		return columnsToChange;
	}
	

}
