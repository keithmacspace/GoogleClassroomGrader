package net.cdonald.googleClassroom.googleClassroomInterface;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.cdonald.googleClassroom.listenerCoordinator.StudentListInfo;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;

public class SaveGrades implements SheetAccessorInterface{
	public class StudentRow {
		Map<String, Object> columns;
		public StudentRow(String lastName, String firstName, String assignmentSubmitDate) {
			columns = new HashMap<String, Object>();
			columns.put(StudentListInfo.defaultColumnNames[StudentListInfo.LAST_NAME_COLUMN], lastName);
			columns.put(StudentListInfo.defaultColumnNames[StudentListInfo.FIRST_NAME_COLUMN], firstName);
			columns.put(StudentListInfo.defaultColumnNames[StudentListInfo.DATE_COLUMN], assignmentSubmitDate);
		}
		public void addScore(String rubricName, Double score) {
			columns.put(rubricName, score);
		}
		public List<Object> generateRow(Map<Integer, String> columnLocations) {
			List<Object> row = new ArrayList<Object>();
			for (Integer column : columnLocations.keySet()) {
				while (row.size() <= column) {
					row.add(null);
				}
				String key = columnLocations.get(column);
				if (columns.containsKey(key)) {
					row.set(column, columns.get(key));
				}
			}
			return row;
		}
	}
	private Rubric rubric;
	private String assignmentName;
	private String graderName;
	private List<StudentRow> studentRowList;
	private Map<String, StudentRow> studentRowMap;
	private Map<Integer, String> columnLocations;
	private Set<String> graded;
	private GoogleSheetData targetFile;
	public SaveGrades(GoogleSheetData targetFile, Rubric rubric, String assignmentName, String graderName) {
		this.targetFile = targetFile;
		this.rubric = rubric;
		this.assignmentName = assignmentName;
		this.graderName = graderName;
		graded = new HashSet<String>();
		studentRowMap  = new HashMap<String, StudentRow>();
		studentRowList  = new ArrayList<StudentRow>();
		columnLocations = new HashMap<Integer, String>();
		int column = 0;
		for (String columnName : StudentListInfo.defaultColumnNames) {
			if (!columnName.equals(StudentListInfo.defaultColumnNames[StudentListInfo.COMPILER_COLUMN]) ) {
				columnLocations.put(column, columnName);
				column++;
			}			
		}		
		for (RubricEntry entry : rubric.getEntries()) {
			columnLocations.put(column, entry.getName());
			column++;
		}
	}
	public void addStudent(String lastName, String firstName, String assigmentSubmitDate) {
		String key = lastName + firstName;
		StudentRow studentRow = new StudentRow(lastName, firstName, assigmentSubmitDate); 
		studentRowMap.put(key, studentRow);
		studentRowList.add(studentRow);
	}
	
	public void addStudentScore(String lastName, String firstName, String rubricName, Double score) {
		String key = lastName + firstName;
		graded.add(rubricName);
		studentRowMap.get(key).addScore(rubricName, score);
	}
	
	
	@Override
	public GoogleSheetData getSheetInfo() {
		return targetFile;
	}
	@Override
	public SaveSheetData getSheetSaveState() {
		// At this point we take all the data that has been added and create the actual save sheet data
		SaveSheetData saveData = new SaveSheetData(rubric.getName());
		List<Object> assignmentRow = new ArrayList<Object>();
		assignmentRow.add("Assignment");
		assignmentRow.add(assignmentName);
		int currentRow = 1;
		saveData.addOneRow(assignmentRow, currentRow++);
		List<Object> gradedByRow = new ArrayList<Object>();
		List<Object> columnNameRow = new ArrayList<Object>();
		List<Object> rubricValueRow = new ArrayList<Object>();
		gradedByRow.add("Graded By");
		rubricValueRow.add("Rubric Value");

		int totalColumn = 0;

				
		for (Integer column : columnLocations.keySet()) {
			while (columnNameRow.size() <= column) {
				columnNameRow.add(null);
			}
			// These two will always be the same size
			while (gradedByRow.size() <= column) {
				gradedByRow.add(null);				
				rubricValueRow.add(null);
			}
			String columnName = columnLocations.get(column);
			if (columnName.equalsIgnoreCase("Total")) {
				totalColumn = column;
			}
			if (graded.contains(columnName)) {
				gradedByRow.set(column, graderName);
			}
			columnNameRow.set(column, columnName);
			for (RubricEntry entry : rubric.getEntries()) {
				if (entry.getName().equalsIgnoreCase(columnName)) {					
					rubricValueRow.set(column, entry.getValue());
				}
			}
		}
		saveData.addOneRow(gradedByRow, currentRow++);		
		saveData.addOneRow(rubricValueRow, currentRow++);
		saveData.addOneRow(columnNameRow, currentRow++);
		int firstStudentRow = currentRow;
		for (StudentRow studentRow : studentRowList) {
			saveData.addOneRow(studentRow.generateRow(columnLocations), currentRow++);
		}
		int lastStudentRow = currentRow;
		List<Object> totalColumnStrings = new ArrayList<Object>();
		
		for (int i = firstStudentRow; i < lastStudentRow; i++) {
			String startColumnName = GoogleClassroomCommunicator.getColumnName(totalColumn + 1);
			String endColumnName = GoogleClassroomCommunicator.getColumnName(saveData.getMaxColumn());
			totalColumnStrings.add("=SUM(" + startColumnName + i + ":" + endColumnName + i+ ")");
		}
		saveData.writeOneColumn(totalColumnStrings, totalColumn, firstStudentRow);
		return saveData;
	}
}
