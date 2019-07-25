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
	private LoadSheetStateMachine sheetStateMachine;

	public Rubric(GoogleSheetData sheetData) {
		super();
		this.sheetData = sheetData;
		entries = new ArrayList<RubricEntry>();
	}
	
	public Rubric(Rubric other) {
		
		this.sheetData = new GoogleSheetData(other.sheetData);
		entries = new ArrayList<RubricEntry>();
		for (RubricEntry otherEntry : other.entries) {
			entries.add(new RubricEntry(otherEntry));
		}
		this.inModifiedState = other.inModifiedState;
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
	
	public void runAutomation(CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData ) {				
		for (RubricEntry entry : entries) {
			entry.runAutomation(message, compiler, consoleData);
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
	
	private void loadFromSheet(List<List<Object> > sheetEntries) {
		if (sheetEntries.size() > 0) {
			List<Object> headings = sheetEntries.get(0);
			for (int i = 1; i < sheetEntries.size(); i++) {
				List<Object> entries = sheetEntries.get(i);
				if (entries != null && entries.get(0) != null && entries.get(0).toString().length() > 0) {
					modifyEntry(new RubricEntry(headings, sheetEntries.get(i)));
				}
				else {
					break;
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
			currentRow++;
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
			sheetStateMachine = new LoadSheetStateMachine();
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
	public enum States{READ_ROW1, READ_COL1, READ_STANDARD_PARTS, READ_AUTOMATION_COLUMNS, DONE};
	private class LoadSheetStateMachine {

		private States currentState;
		private int initialRowCount;
		private int currentAutomationHeader;
		private Map<String, List<List<Object>>> entryColumns;
		private List<String> columnHeaders;
		public LoadSheetStateMachine() {
			currentState = States.READ_ROW1;
			initialRowCount = 0;
			entryColumns = new HashMap<String, List<List<Object>>>();
			columnHeaders = new ArrayList<String>();
		}
		String range = null;
		
		String getNextRange(int rangeCount) {
			switch(currentState) {
			case READ_ROW1:
				range = "1:1";
				break;
			case READ_COL1:
				range = columnNames[0] + ":" + columnNames[0];
				break;
			case READ_STANDARD_PARTS:
				// We subtract 2 because ID is not included and for zero based indexing
				int lastColumn = RubricEntry.HeadingNames.values().length - 2;
				range = columnNames[0] + "1:" + columnNames[lastColumn] + "" + initialRowCount;
				break;
			case READ_AUTOMATION_COLUMNS:
				range = columnNames[currentAutomationHeader] + ":" + columnNames[currentAutomationHeader];
				break;
			case DONE:
				for (RubricEntry entry : entries) {
					entry.loadAutomationColumns(entryColumns);
				}
				range = null;
			default:
				range = null;
			}
			return range;

		}
		public void setResponseData(List<List<Object>> sheetEntries, int rangeCount) {
			switch(currentState) {
			case READ_ROW1:
				for (List<Object> row : sheetEntries) {
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
				}
				currentState = States.READ_COL1;
				break;			
			case READ_COL1:
				for (List<Object> row : sheetEntries) {
					if (row != null && row.size() > 0 && row.get(0) != null && row.get(0).toString().length() > 0) {
						initialRowCount++;
					}
					else {
						break;
					}
				}
				currentState = States.READ_STANDARD_PARTS;
				break;
			case READ_STANDARD_PARTS:
				entries.clear();
				loadFromSheet(sheetEntries);
				currentState = States.READ_AUTOMATION_COLUMNS;
				break;
			case READ_AUTOMATION_COLUMNS:
				String entryName = null;
				if (sheetEntries != null) {						
					List<Object> column = new ArrayList<Object>();
					for (List<Object> row : sheetEntries) {
						for (Object str : row) {
							if (str instanceof String) {
								column.add((String)str);
								if (entryName == null) {
									entryName = (String)str;
								}
							}
						}
					}					
					if (entryName != null) {
						entryColumns.get(entryName.toUpperCase()).add(column);
					}
				}
				if (entryName == null) {
					currentState = States.DONE;
				}
				currentAutomationHeader++;
			}
		}		
	}
}
