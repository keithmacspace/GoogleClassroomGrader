package net.cdonald.googleClassroom.googleClassroomInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.cdonald.googleClassroom.listenerCoordinator.StudentListInfo;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.StudentData;

public abstract class GradeAccessor implements SheetAccessorInterface {
	private Rubric rubric;
	private String assignmentName;
	private List<StudentRow> studentRowList;
	private Map<String, StudentRow > studentRowMap;
	private Map<String, List<StudentRow> > studentRowNameMap;
	private List<String> columns;
	private GoogleSheetData targetFile;
	public GradeAccessor(GoogleSheetData targetFile, Rubric rubric, List<StudentData> students) {
		this.targetFile = targetFile;
		this.setRubric(rubric);
		this.setAssignmentName(null);
		columns = new ArrayList<String>();
		studentRowMap  = new HashMap<String, StudentRow>();
		studentRowList  = new ArrayList<StudentRow>();
		studentRowNameMap = new HashMap<String, List<StudentRow> >(); 
		for (String columnName : StudentListInfo.defaultColumnNames) {
			if (!columnName.equals(StudentListInfo.defaultColumnNames[StudentListInfo.COMPILER_COLUMN]) ) {
				addColumnName(columnName);
			}			
		}
		
		for (RubricEntry entry : rubric.getEntries()) {
			// Don't use getColumnName, that is for our display where we add the value
			addColumnName(entry.getName());
		}

		
		for (StudentData student : students) {
			String key = student.getId();
			String lastName = student.getName(); 
			String firstName = student.getFirstName();
			String nameKey = getNameKey(lastName, firstName);
			StudentRow studentRow = new StudentRow(student);
			if (studentRowNameMap.containsKey(nameKey) == false) {
				studentRowNameMap.put(nameKey, new ArrayList<StudentRow>());
			}
			studentRowNameMap.get(nameKey).add(studentRow);
			studentRowMap.put(key, studentRow);
			studentRowList.add(studentRow);
		}
	}
	
	public String getNameKey(String lastName, String firstName) {
		return lastName.toUpperCase() + firstName.toUpperCase();
	}
	
	public int getNumColumns() {
		return columns.size();
	}
		
	public String getColumnName(int column) {
		return columns.get(column);
	}
	
	public int getColumnLocation(String columnName) {
		for (int i = 0; i < columns.size(); i++) {
			if (columnName.equalsIgnoreCase(columns.get(i))) {
				return i;
			}
		}
		return -1;		
	}
	
	public void insertColumn(int index, String columnName) {
		columns.add(index, columnName);
	}
	
	public void moveColumn(int oldIndex, int newIndex, String columnName) {
		if (oldIndex > newIndex) {
			columns.add(newIndex, columns.remove(oldIndex));
		}
		else {
			columns.add(newIndex - 1, columns.remove(oldIndex));
		}
	}
	
	public void addColumnName(String columnName) {
		columns.add(columnName);
	} 
	
	public void addStudentColumn(StudentData student,  String columnName, Object value) {
		String key = student.getId();
		studentRowMap.get(key).addColumn(columnName, value);
	}
	
	public StudentRow getStudentRow(String lastName, String firstName, int foundCount) {
		String nameKey = getNameKey(lastName, firstName);
		List<StudentRow> studentRow = studentRowNameMap.get(nameKey);
		if (studentRow != null) {
			return studentRow.get(foundCount);
		}
		return null;	
	}
	
	public int getNumStudents() {
		return studentRowList.size();
	}
	
	public List<Object> generateStudentRow(int studentNum) {
		return studentRowList.get(studentNum).generateRow(columns);
	}
	
	
	@Override
	public GoogleSheetData getSheetInfo() {
		return targetFile;
	}

	public Rubric getRubric() {
		return rubric;
	}
	public void setRubric(Rubric rubric) {
		this.rubric = rubric;
	}

	public String getAssignmentName() {
		return assignmentName;
	}
	public void setAssignmentName(String assignmentName) {
		this.assignmentName = assignmentName;
	}

	protected class StudentRow {
		StudentData student;
		Map<String, Object> columns;
		public StudentRow(StudentData student) {
			this.student = student;
			columns = new HashMap<String, Object>();
			addColumn(StudentListInfo.defaultColumnNames[StudentListInfo.LAST_NAME_COLUMN].toUpperCase(), student.getName());
			addColumn(StudentListInfo.defaultColumnNames[StudentListInfo.FIRST_NAME_COLUMN].toUpperCase(), student.getFirstName());			
		}
		
		StudentData getStudent() {
			return student;
		}
		

		
		public void addColumn(String columnName, Object score) {
			columns.put(columnName.toUpperCase(), score);
		}
		public List<Object> generateRow(List<String> columnNames) {
			List<Object> row = new ArrayList<Object>(columnNames.size());
			int index = 0;
			for (String column : columnNames) {
				String key = column.toUpperCase();
				if (columns.containsKey(key)) {
					row.add(columns.get(key));
				}
				else {
					row.add(null);
				}
				index++;
			}
			return row;
		}
	}
}