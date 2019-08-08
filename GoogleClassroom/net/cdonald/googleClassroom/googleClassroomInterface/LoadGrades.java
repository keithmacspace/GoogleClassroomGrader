package net.cdonald.googleClassroom.googleClassroomInterface;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.listenerCoordinator.StudentListInfo;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.StudentData;

public class LoadGrades extends GradeAccessor{

	public LoadGrades(GoogleSheetData targetFile, Rubric rubricParam, List<StudentData> students)  {
		super(targetFile, rubricParam, students);
		
		
	}
	
	public void loadData(GoogleClassroomCommunicator communicator, boolean keepCurrentRubricValues) throws IOException {
		LoadSheetData data = communicator.readSheet(this);
		if (data != null) {
			int nameRow = processColumnNames(data);
			// If namerow comes back -1, then we are starting right from the starting line
			nameRow++;		
			processNameData(nameRow, data, keepCurrentRubricValues);
		}
		
	}

	
	private int processColumnNames(LoadSheetData data) {
		
		for (int i = 0; i < data.getNumRows(); i++) {
			List<Object> row = data.readRow(i);
			int matchCount = 0;
			for (Object columnObject : row) {
				if (columnObject instanceof String) {
					String column = (String)columnObject;
					if (column.equalsIgnoreCase(StudentListInfo.defaultColumnNames[StudentListInfo.LAST_NAME_COLUMN])) {
						matchCount++;
					}
					if (column.equalsIgnoreCase(StudentListInfo.defaultColumnNames[StudentListInfo.FIRST_NAME_COLUMN])) {
						matchCount++;
					}
					if (matchCount == 2) {
						break;
					}
				}
			}
			if (matchCount == 2) {
				int columnIndex = 0;
				for (Object columnObject : row) {
					boolean insertBlank = true;
					if (columnObject instanceof String) {
						String column = (String)columnObject;
						if (column.length() > 1) {
							insertBlank = false;
							int currentIndex = getColumnLocation(column);
							if (currentIndex == -1) {
								insertColumn(columnIndex, column);
							}
							else if (currentIndex != columnIndex) {
								moveColumn(currentIndex, columnIndex, column);
							}
						}
					}
					if (insertBlank) {
						insertColumn(columnIndex, "no header" + columnIndex);
					}
					columnIndex++;
				}
				return i;				
			}
		}
		return -1;		
	}
	
	
	private void processNameData(int nameRow, LoadSheetData data, boolean keepCurrentRubricValues) {
		int lastNameColumn = getColumnLocation(StudentListInfo.defaultColumnNames[StudentListInfo.LAST_NAME_COLUMN]);
		int firstNameColumn = getColumnLocation(StudentListInfo.defaultColumnNames[StudentListInfo.FIRST_NAME_COLUMN]);
		Map<String, Integer> foundCountMap = new HashMap<String, Integer>();
		Map<String, RubricEntry> rubricEntryMap = new HashMap<String, RubricEntry>();
		for (int i = nameRow; i < data.getNumRows(); i++) {
			List<Object> row = data.readRow(i);
			String lastName = (String)row.get(lastNameColumn);
			String firstName = (String)row.get(firstNameColumn);
			String key = getNameKey(lastName, firstName);
			if (foundCountMap.containsKey(key) == false) {
				foundCountMap.put(key, 0);
			}
			int foundCount = foundCountMap.get(key);
			foundCountMap.put(key, foundCount+1);
			StudentRow studentRow = getStudentRow(lastName, firstName, foundCount);
			String studentID = studentRow.getStudent().getId();
			for (int col = 0; col < row.size(); col++) {
				if (col != firstNameColumn && col != lastNameColumn) {
					String columnName = getColumnName(col);
					studentRow.addColumn(columnName, row.get(col));
					addDataToRubric(studentID, columnName, row.get(col), rubricEntryMap, keepCurrentRubricValues);
				}				
			}
		}
	}
	
	private void addDataToRubric(String studentID, String columnName, Object info, Map<String, RubricEntry> rubricEntryMap, boolean keepCurrentRubricValues) {
		Rubric rubric = getRubric();
		if (rubricEntryMap.containsKey(columnName) == false) {
			boolean found = false;
			for (RubricEntry entry : rubric.getEntries()) {
				if (entry.getName().equalsIgnoreCase(columnName)) {
					rubricEntryMap.put(columnName, entry);
					found = true;
					break;
				}
			}
			if (found == false) {
				rubricEntryMap.put(columnName, null);
			}
		}
		RubricEntry entry = rubricEntryMap.get(columnName);
		if (entry != null && info != null && info.toString().length() > 0) {
			if (keepCurrentRubricValues == false || entry.getStudentDoubleValue(studentID) == null) {
				entry.setStudentValue(studentID, info.toString());
			}
		}							
	}
	
	@Override
	public SaveSheetData getSheetSaveState() {
		// TODO Auto-generated method stub
		return null;
	}

}
