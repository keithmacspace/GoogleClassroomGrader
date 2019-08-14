package net.cdonald.googleClassroom.googleClassroomInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.listenerCoordinator.StudentListInfo;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.StudentData;

public class SaveGrades extends LoadGrades{

	private Set<String> graded;
	public SaveGrades(GoogleClassroomCommunicator communicator, GoogleSheetData targetFile, Rubric rubric, List<StudentData> students, String graderName, Map<String, Map<String, String>> graderCommentsMap) {
		super(targetFile, rubric, students, graderName, graderCommentsMap);		
		graded = new HashSet<String>();
		try {
			loadData(communicator, true);
		} catch (IOException e) {
		}
	}
	

	public void addStudentScore(StudentData student, String rubricName, Double score) {
		graded.add(rubricName);
		super.addStudentColumn(student, rubricName, score);
		
	}
	
	public void addStudentNotes(StudentData student, String notes) {
		if (getColumnLocation(getNotesHeader()) == -1) {
			addColumnName(getNotesHeader());
		}
		graded.add(getNotesHeader());
		super.addStudentColumn(student, getNotesHeader(), notes);
	}
	
	public void addAssignmentDate(StudentData student, String assignmentDate) {
		super.addStudentColumn(student, StudentListInfo.defaultColumnNames[StudentListInfo.DATE_COLUMN], assignmentDate);
	}
	
	@Override
	public SaveSheetData getSheetSaveState() {
		// At this point we take all the data that has been added and create the actual save sheet data
		SaveSheetData saveData = new SaveSheetData(SaveSheetData.ValueType.USER_ENTERED, getRubric().getName());
		List<Object> assignmentRow = new ArrayList<Object>();
		int currentRow = 1;
		saveData.addOneRow(assignmentRow, currentRow++);
		int numColumns = getNumColumns();
		List<Object> gradedByRow = new ArrayList<Object>(numColumns);
		List<Object> columnNameRow = new ArrayList<Object>(numColumns);
		List<Object> rubricValueRow = new ArrayList<Object>(numColumns);
		for (int col = 0; col < numColumns; col++) {
			gradedByRow.add(null);
			columnNameRow.add(null);
			rubricValueRow.add(null);
		}
		gradedByRow.set(0, "Graded By");
		rubricValueRow.set(0, "Rubric Value");

		int totalColumn = 0;

		for (int col = 0; col < numColumns; col++) {
			String columnName = getColumnName(col);
				
			if (columnName.equalsIgnoreCase("Total")) {
				totalColumn = col;
			}

			columnNameRow.set(col, columnName);
			for (RubricEntry entry : getRubric().getEntries()) {
				if (entry.getName().equalsIgnoreCase(columnName)) {					
					rubricValueRow.set(col, entry.getValue());
				}
			}
		}
		saveData.addOneRow(gradedByRow, currentRow++);		
		saveData.addOneRow(rubricValueRow, currentRow++);
		saveData.addOneRow(columnNameRow, currentRow++);
		int firstStudentRow = currentRow;
		for (int studentNum = 0; studentNum < getNumStudents(); studentNum++) {
			saveData.addOneRow(generateStudentRow(studentNum), currentRow++);
		}

		int lastStudentRow = currentRow;
		List<Object> totalColumnStrings = new ArrayList<Object>();
		
		for (int i = firstStudentRow; i < lastStudentRow; i++) {
			String startColumnName = GoogleClassroomCommunicator.getColumnName(totalColumn + 1);
			String endColumnName = GoogleClassroomCommunicator.getColumnName(saveData.getMaxColumn());
			totalColumnStrings.add("=SUM(" + startColumnName + i + ":" + endColumnName + i+ ")");
		}
		saveData.writeColumnEntries(totalColumnStrings, totalColumn, firstStudentRow);
		return saveData;
	}
	
}
