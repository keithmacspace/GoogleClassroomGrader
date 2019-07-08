package gui;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import inMemoryJavaCompiler.CompilerMessage;
import model.ClassroomData;
import model.FileData;
import model.StudentData;

public class StudentListModel extends AbstractTableModel {
	private static final long serialVersionUID = -3240265069491780098L;
	private List<StudentData> studentData;
	private List<ArrayList<Double>> rubricValues;
	private List<String> columnNames;
	private Map<String, Integer> idToRowMap;
	private List<CompilerMessage> compilerMessage;
	private List<FileData> fileData;

	private static final int DATE_COLUMN = 2;
	private static final int COMPILER_COLUMN = 3;	
	private static final int NUM_DEFAULT_COLUMNS = 4;

	public StudentListModel() {
		studentData = new ArrayList<StudentData>();
		rubricValues = new ArrayList<ArrayList<Double>>();
		compilerMessage = new ArrayList<CompilerMessage>();
		fileData = new ArrayList<FileData>();
		columnNames = new ArrayList<String>();
		idToRowMap = new HashMap<String, Integer>();
		clearAll();
		addColumn("Last Name");
		addColumn("First Name");
		addColumn("Turned In");
		addColumn("Compiled");
	}


	@Override
	public String getColumnName(int column) {
		return columnNames.get(column);
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == DATE_COLUMN) {
			return Date.class;
		}
		else if (columnIndex == COMPILER_COLUMN) {
			return CompilerMessage.class;
		}
		else if (columnIndex < NUM_DEFAULT_COLUMNS) {
			return String.class;
		}
		return Double.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex < NUM_DEFAULT_COLUMNS) {
			return false;
		}
		return true;
	}

	@Override
	public int getRowCount() {
		return studentData.size();
	}

	@Override
	public int getColumnCount() {
		return rubricValues.size() + NUM_DEFAULT_COLUMNS;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (rowIndex >= studentData.size()) {
			return null;
		}
		switch (columnIndex) {
		case 0:
			return studentData.get(rowIndex).getName();
		case 1:
			return studentData.get(rowIndex).getFirstName();
		case 2:
			Date date = null;
			FileData file = fileData.get(rowIndex);
			if (file != null) {
				date = fileData.get(rowIndex).getDate();
			}
			return date;
		case 3:
			CompilerMessage message = compilerMessage.get(rowIndex);			
			return message;
		default:
			return rubricValues.get(columnIndex - NUM_DEFAULT_COLUMNS).get(rowIndex);

		}
	}
	
	public void clearAll() {
		studentData.clear();
		rubricValues.clear();
		compilerMessage.clear();
		fileData.clear();
		columnNames.clear();
		idToRowMap.clear();
		addColumn("Last Name");
		addColumn("First Name");
		addColumn("Turned In");
		addColumn("Compiled");		
	}
	
	public void clearRubric() {
		rubricValues.clear();
		for (int i = 0; i < fileData.size(); i++) {
			fileData.set(i, null);
		}
		for (int i = 0; i < compilerMessage.size(); i++) {
			compilerMessage.set(i, null);
		}
	}

	public void addColumn(String name) {
		columnNames.add(name);
	}	

	public void addCompilerMessages(List<CompilerMessage> messages) {
		for (CompilerMessage message : messages) {
			compilerMessage.set(idToRowMap.get(message.getStudentId()), message);
		}
		fireTableDataChanged();		
	}
	
	public void addFileData(FileData fileDataInfo) {
		if (fileDataInfo.getDate() != null) {
			int index = idToRowMap.get(fileDataInfo.getId());
			FileData current = fileData.get(index);
			if ((current == null) || (current.getDate().compareTo(fileDataInfo.getDate()) < 0)) {
				fileData.set(index, fileDataInfo);
			}
		}
		fireTableDataChanged();
	}

	public void addStudent(StudentData student) {		
		boolean inserted = false;
		for (int i = 0; i < studentData.size(); i++) {
			StudentData other = studentData.get(i);
			if (other.compareTo(student) < 0) {
				studentData.add(i, student);
				inserted = true;
				break;
			}
		}
		if (inserted == false) {
			studentData.add(student);
		}
		compilerMessage.add(null);
		fileData.add(null);
		idToRowMap.clear();
		int index = 0;
		for (StudentData data : studentData) {
			idToRowMap.put(data.getId(), index);
			index++;
		}
		fireTableDataChanged();
	}
	
	public String getStudentId(int row) {
		if (row >= 0 && row < studentData.size()) {			
			ClassroomData student = studentData.get(row);
			return student.getId();
		}
		return null;
	}
}
