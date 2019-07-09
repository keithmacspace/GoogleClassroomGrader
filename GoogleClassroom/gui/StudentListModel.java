package gui;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import inMemoryJavaCompiler.CompilerMessage;
import model.ClassroomData;
import model.FileData;
import model.Rubric;
import model.RubricEntry;
import model.StudentData;

public class StudentListModel extends AbstractTableModel {
	private static final long serialVersionUID = -3240265069491780098L;
	private List<StudentData> studentData;
	private List<ArrayList<Double>> rubricValues;
	private List<String> columnNames;
	private Map<String, Integer> idToRowMap;
	private List<CompilerMessage> compilerMessage;
	private List<FileData> fileData;
	private List<Integer> maxValues;
	private Rubric currentRubric;

	public static final int LAST_NAME_COLUMN = 0;
	public static final int FIRST_NAME_COLUMN = 1;
	public static final int DATE_COLUMN = 2;
	public static final int COMPILER_COLUMN = 3;
	public static final int NUM_DEFAULT_COLUMNS = 4;

	public StudentListModel() {
		studentData = new ArrayList<StudentData>();
		rubricValues = new ArrayList<ArrayList<Double>>();
		compilerMessage = new ArrayList<CompilerMessage>();
		fileData = new ArrayList<FileData>();
		columnNames = new ArrayList<String>();
		idToRowMap = new HashMap<String, Integer>();
		maxValues = new ArrayList<Integer>();
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
			return FileData.class;
		} else if (columnIndex == COMPILER_COLUMN) {
			return CompilerMessage.class;
		}

		else if (columnIndex <= FIRST_NAME_COLUMN) {
			return StudentData.class;
		}
		return String.class;
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
		int size = rubricValues.size() + NUM_DEFAULT_COLUMNS;
		return size;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (rowIndex >= studentData.size()) {
			return null;
		}
		switch (columnIndex) {
		case LAST_NAME_COLUMN:
			return studentData.get(rowIndex);
		case FIRST_NAME_COLUMN:
			return studentData.get(rowIndex);
		case DATE_COLUMN:
			Date date = null;
			FileData file = fileData.get(rowIndex);
			return file;
		case COMPILER_COLUMN:
			CompilerMessage message = compilerMessage.get(rowIndex);
			return message;
		default:
			double value = rubricValues.get(columnIndex - NUM_DEFAULT_COLUMNS).get(rowIndex);
			if ((int) value == value) {
				return "" + ((int) value);
			}
			return "" + value;

		}
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

		if (aValue != null) {
			if (columnIndex >= NUM_DEFAULT_COLUMNS) {
				if (aValue instanceof String) {				
					int valueIndex = columnIndex - NUM_DEFAULT_COLUMNS;
					Double value = 0.0;
					try {
						value = Double.parseDouble((String) aValue);
					} catch (NumberFormatException e) {
						return;
					}
					if (value <= maxValues.get(valueIndex)) {
						rubricValues.get(valueIndex).set(rowIndex, value);
						fireTableCellUpdated(rowIndex, columnIndex);
					}
				}
			}
			else if (columnIndex == DATE_COLUMN) {
				fileData.set(rowIndex, (FileData)aValue);				
			}
			else if (columnIndex <= FIRST_NAME_COLUMN) {
				studentData.set(rowIndex, (StudentData)aValue);
			}
			else if (columnIndex == COMPILER_COLUMN) {
				if (currentRubric != null) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							Map<Integer, Double> columnsToChange = currentRubric.compileDone((CompilerMessage)aValue, COMPILER_COLUMN + 1);
							if (columnsToChange != null) {
								for (Integer column : columnsToChange.keySet()) {
									setValueAt(columnsToChange.get(column).toString(), rowIndex, columnIndex);
								}
							}
						}	
					});
					
				}
			}
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

	public void newAssignmentSelected() {
		clearRubric();
		for (int i = 0; i < fileData.size(); i++) {
			fileData.set(i, null);
		}
		for (int i = 0; i < compilerMessage.size(); i++) {
			compilerMessage.set(i, null);
		}
		fireTableDataChanged();
	}

	public void clearRubric() {
		currentRubric = null;
		rubricValues.clear();
		maxValues.clear();
		fireTableStructureChanged();
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
		for (List<Double> rubricValue : rubricValues) {
			rubricValue.add(0.0);
		}
		int index = 0;
		for (StudentData data : studentData) {
			idToRowMap.put(data.getId(), index);
			index++;
		}
		fireTableDataChanged();
	}

	public String getStudentId(int row) {
		if (row >= 0 && row < studentData.size()) {
			ClassroomData student = (ClassroomData)getValueAt(row, 0);
			return student.getId();
		}
		return null;
	}

	public void setRubric(Rubric rubric) {
		clearRubric();
		currentRubric = rubric;
		for (RubricEntry entry : rubric.getEntries()) {
			ArrayList<Double> values = new ArrayList<Double>();
			maxValues.add(entry.getValue());
			rubricValues.add(values);
			String name = entry.getName();
			name = "<html>" + name + "<br>Value = " + entry.getValue() + "</html>";
			addColumn(name);
			for (int i = 0; i < studentData.size(); i++) {
				values.add(0.0);
			}
		}
		fireTableStructureChanged();
	}
	 
}
