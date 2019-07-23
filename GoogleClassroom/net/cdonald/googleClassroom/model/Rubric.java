package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.googleClassroomInterface.SaveStateData;
import net.cdonald.googleClassroom.googleClassroomInterface.SheetAccessorInterface;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;

public class Rubric implements SheetAccessorInterface {
	private GoogleSheetData sheetData;
	private List<RubricEntry> entries;
	private boolean inModifiedState;
	private SheetStateMachine sheetStateMachine;

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
	
	public String getTotalCount(String id) {
		double value = 0.0;
		for (RubricEntry entry : entries) {
			value += entry.getStudentDoubleValue(id);
		}
		return "" + value;
	}
	
	
	public String getName() {
		return sheetData.getName();
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

	private void loadFromSheet(List<List<Object> > sheetEntries) {
		if (sheetEntries.size() > 0) {
			List<Object> headings = sheetEntries.get(0);
			for (int i = 1; i < sheetEntries.size(); i++) {
				addEntry(new RubricEntry(headings, sheetEntries.get(i)));
			}
		}
	}

	private class SheetStateMachine {
		private RubricEntry currentSheetAccessEntry;
		private int currentSheetColumn;
		private int currentEntryColumnCount;
		private Map<String, List<List<Object>>> entryColumns;
		private List<String> columnHeaders;
		public SheetStateMachine() {			
			currentSheetAccessEntry = null;
			currentSheetColumn = 0;
			currentEntryColumnCount = 0;
			entryColumns = new HashMap<String, List<List<Object>>>();			
		}
		String getNextRange(int rangeCount) {
			if (rangeCount == 0) {
				currentSheetColumn = 1; 
				return columnNames[0] + "1:" + columnNames[columnNames.length - 1] + "1";
			}
			else if (rangeCount == 1) {
				currentSheetColumn += 3;
				return columnNames[0] + "1:" + columnNames[currentSheetColumn] + "1000";
			}
			else if (columnHeaders.size() != 0) {
				currentSheetColumn++;
				return columnNames[currentSheetColumn] + ":" + columnNames[currentSheetColumn]; 
			}
			else {				
				for (RubricEntry entry : entries) {						
					entry.loadAutomationColumns(entryColumns);

				}
			}		
			return null;
		}
		public void setResponseData(List<List<Object>> sheetEntries, int rangeCount) {
			if (rangeCount == 0) {
				columnHeaders = new ArrayList<String>();
				int headerCount = 0;
				for (List<Object> row : sheetEntries) {
					for (Object entry : row) {
						if (headerCount > 3) {
							if (entry != null && entry instanceof String) {
								String entryName = (String)entry;
								columnHeaders.add(entryName);
								if (entryColumns.containsKey(entryName) == false) {
									entryColumns.put(entryName.toUpperCase(), new ArrayList<List<Object>>());
								}
							}
						}
						headerCount++;
					}
				}				
			}
			else if (rangeCount == 1) {
				entries.clear();
				loadFromSheet(sheetEntries);				
			}
			else {
				String entryName = columnHeaders.remove(0);
				List<Object> column = new ArrayList<Object>();
				for (List<Object> row : sheetEntries) {
					for (Object str : row) {
						if (str instanceof String) {
							column.add((String)str);
						}
					}
					entryColumns.get(entryName).add(column);				
				}
			}
		}		
	}

	
	@Override
	public SaveStateData getSheetSaveState() {
		SaveStateData saveState = new SaveStateData(sheetData.getName());
		List<List<Object>> columnData = new ArrayList<List<Object>>();
		Map<String, List<Object>> fileData = new HashMap<String, List<Object>>();
		
		
		saveState.addOneRow(RubricEntry.getRubricHeader(), 1);
		int currentRow = 2;
		for (RubricEntry entry : entries) {
			saveState.addOneRow(entry.getRubricEntryInfo(), currentRow);
			entry.saveAutomationData(columnData, fileData);			
		}
		

		int currentColumn = RubricEntry.HeadingNames.values().length + 1;
		for (List<Object> column : columnData) {
			saveState.addOneColumn(column, currentColumn);
			currentColumn++;
		}
		for (String key : fileData.keySet()) {
			List<Object> fileColumn = fileData.get(key);
			saveState.addOneColumn(fileColumn, currentColumn);
			currentColumn++;			
		}
		return saveState;
		
	}
	
	
	@Override
	public String getNextRange(int rangeCount) {
		if (rangeCount == 0) {
			sheetStateMachine = new SheetStateMachine();
		}
		return sheetStateMachine.getNextRange(rangeCount);
	}

	@Override
	public void setResponseData(List<List<Object>> sheetEntries, int rangeCount) {
		sheetStateMachine.setResponseData(sheetEntries, rangeCount);		
	}

	@Override
	public GoogleSheetData getSheetInfo() {
		// TODO Auto-generated method stub
		return sheetData;
	}
	

}
