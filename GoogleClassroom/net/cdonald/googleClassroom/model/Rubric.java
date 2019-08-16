package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.googleClassroomInterface.LoadSheetData;
import net.cdonald.googleClassroom.googleClassroomInterface.SaveSheetData;
import net.cdonald.googleClassroom.googleClassroomInterface.SheetAccessorInterface;
import net.cdonald.googleClassroom.gui.DataUpdateListener;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;

public class Rubric implements SheetAccessorInterface {
	private GoogleSheetData sheetData;
	private List<RubricEntry> entries;
	private boolean inModifiedState;
	private Map<String, FileData> fileDataMap;
	

	public Rubric(GoogleSheetData sheetData) {
		super();
		this.sheetData = sheetData;
		entries = new ArrayList<RubricEntry>();
		fileDataMap = new HashMap<String, FileData>();
	}
	
	public Rubric(Rubric other) {		
		sheetData = new GoogleSheetData(other.sheetData);
		entries = new ArrayList<RubricEntry>();
		for (RubricEntry otherEntry : other.entries) {
			entries.add(new RubricEntry(otherEntry));
		}
		inModifiedState = other.inModifiedState;
		fileDataMap = new HashMap<String, FileData>();
		for (String key : other.fileDataMap.keySet()) {
			fileDataMap.put(key, other.fileDataMap.get(key));
		}
	}
	
	// This form is used when we are creating a new rubric from scratch
	public Rubric() {
		inModifiedState = true;
		entries = new ArrayList<RubricEntry>();
		fileDataMap = new HashMap<String, FileData>();
	}


	public Map<String, FileData> getFileDataMap() {
		return fileDataMap;
	}
	
	public void addFileData(FileData fileData) {
		fileDataMap.put(fileData.getName(), fileData);
		for (RubricEntry entry : entries) {
			entry.removeFileData(fileData);
		}
	}
	
	public void removeFileData(FileData fileData) {
		fileDataMap.remove(fileData.getName());
	}

	public String getTotalCount(String id) {
		double value = 0.0;
		for (RubricEntry entry : entries) {
			Double studentValue = entry.getStudentDoubleValue(id);
			if (studentValue != null) {
				value += studentValue;
			}
		}
		return "" + value;
	}
	
	public List<String> getRubricTabs() {
		List<String> rubricTabs = new ArrayList<String>();
		for (RubricEntry entry : entries) {
			entry.addRubricTab(rubricTabs);
		}
		return rubricTabs;
	}
	
	
	public String getName() {
		return sheetData.getName();
	}
	
	public boolean isEmpty() {
		return entries.isEmpty();
	}
	
	public void addNewEntry() {
		entries.add(new RubricEntry());
	}
	
	public void addNewEntry(int index) {
		entries.add(index, new RubricEntry());
	}
	
	public void modifyEntry(RubricEntry entry) {
		boolean modified = false;
		for (int i = 0; (i < entries.size() && modified == false); i++) {
			if (entries.get(i).getName().equalsIgnoreCase(entry.getName())) {
				entries.set(i, entry);
				modified = true;
			}
		}
		if (modified == false) {
			entries.add(entry);
		}
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

	public void removeEntry(int index) {
		if (index >= 0 && index <= entries.size()) {
			entries.remove(index);
		}
	}
	
	public void swapEntries(int index, int otherIndex) {
		if (index >= 0 && index < entries.size() && otherIndex >= 0 && otherIndex < entries.size()) {
			RubricEntry temp = entries.set(index, entries.get(otherIndex));
			entries.set(otherIndex, temp);
		}
	}
	
	public boolean isInRubric(String name) {
		for (RubricEntry entry : entries) {
			if (entry.getName().equals(name)) {
				return true;
			}
		}
		return false;
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
	
	public void runAutomation(DataUpdateListener updateListener, String studentName, CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData ) {				
		for (RubricEntry entry : entries) {
			entry.runAutomation(studentName, message, compiler, consoleData);
			updateListener.dataUpdated();
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
	
	public void deleteEntry(String elementName) {
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).getName().equals(elementName)) {
				entries.remove(i);
				break;
			}
		}
	}
	
	public void loadFromSheet(LoadSheetData loadSheetData) {
		entries.clear();
		fileDataMap.clear();
		if (loadSheetData == null || loadSheetData.isEmpty() == true) {
			return;
		}

		int currentAutomationHeader = 0;
		Map<String, List<List<Object>>> entryColumns = new HashMap<String, List<List<Object>>>();
		List<String> columnHeaders = new ArrayList<String>();
		// First read the label names & map them to columns
		List<Object> row = loadSheetData.readRow(0);
		
		for (Object entry : row) {
			String entryName = (String)entry;
			columnHeaders.add(entryName);
			if (entry != null && entry instanceof String) {							
				if (entryName.length() > 1 && columnHeaders.size() > 4) {
					if (entryColumns.containsKey(entryName) == false) {
						entryColumns.put(entryName.toUpperCase(), new ArrayList<List<Object>>());
					}
					if (currentAutomationHeader == 0) {
						currentAutomationHeader = columnHeaders.size() - 1;
					}
				}
			}							
		}
		
		// Now read all the standard components of the entries		
		for (int i = 1; i < loadSheetData.getNumRows(); i++) {
			List<Object> entries = loadSheetData.readRow(i);				
			if (entries != null && entries.get(0) != null && entries.get(0).toString().length() > 0) {
				modifyEntry(new RubricEntry(columnHeaders, entries));
			}
			else {
				break;
			}
		}
		
		// Now read the automation columns
		if (currentAutomationHeader != 0) {
			while(columnHeaders.size() > currentAutomationHeader && columnHeaders.get(currentAutomationHeader) != null) {
				String entryNameKey = columnHeaders.get(currentAutomationHeader).toUpperCase();			
				List<Object> column = loadSheetData.readColumn(currentAutomationHeader);
				entryColumns.get(entryNameKey).add(column);
				currentAutomationHeader++;
			}
			for (RubricEntry entry : entries) {
				entry.loadAutomationColumns(entryColumns, fileDataMap);
			}			
		}
		
		
	}
	
	@Override
	public SaveSheetData getSheetSaveState() {
		SaveSheetData saveState = new SaveSheetData(SaveSheetData.ValueType.RAW, sheetData.getName());
		List<List<Object>> columnData = new ArrayList<List<Object>>();
		Map<String, List<Object>> fileData = new HashMap<String, List<Object>>();
		
		
		saveState.addOneRow(RubricEntry.getRubricHeader(), 1);
		int currentRow = 2;
		for (RubricEntry entry : entries) {
			saveState.addOneRow(entry.getRubricEntryInfo(), currentRow);
			entry.saveAutomationData(columnData, fileData);	
			currentRow++;
		}
		

		int currentColumn = RubricEntry.HeadingNames.values().length + 1;
		for (List<Object> column : columnData) {
			saveState.writeFullColumn(column, currentColumn);
			currentColumn++;
		}
		for (String key : fileData.keySet()) {
			List<Object> fileColumn = fileData.get(key);
			saveState.writeFullColumn(fileColumn, currentColumn);
			currentColumn++;			
		}
		// Add 3 blank columns to the end.  This should take care of deletions
		for (int i = 0; i < 3; i++) {
			
		}
		return saveState;		
	}
	


	@Override
	public GoogleSheetData getSheetInfo() {
		// TODO Auto-generated method stub
		return sheetData;
	}

}
