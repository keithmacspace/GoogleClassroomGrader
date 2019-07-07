package gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import model.FileData;
import model.StudentData;

public class StudentListModel extends AbstractTableModel {
	private static final long serialVersionUID = -3240265069491780098L;
	List<StudentData> studentData;
	List<ArrayList<Double> > rubricValues;
	List<String> columnNames;
	Map<String, Integer> idToRowMap;
	public StudentListModel() {
		studentData = new ArrayList<StudentData>();
		rubricValues = new ArrayList<ArrayList<Double> >();
		columnNames = new ArrayList<String>();
		idToRowMap = new HashMap<String, Integer>();		
		addColumn("Last Name");
		addColumn("First Name");
	}
	

	
	public void addColumn(String name) {
		columnNames.add(name);
	}

	@Override
	public String getColumnName(int column) {		
		return columnNames.get(column);
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex < 2) {
			return String.class;
		}
		return Double.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex < 2) {
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
		return 2 + rubricValues.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == 0) {
			return studentData.get(rowIndex).getName();
		}
		else if (columnIndex == 1) {
			return studentData.get(rowIndex).getFirstName();
		}
		else {
			return rubricValues.get(columnIndex - 2).get(rowIndex);
		}
	}
	
	public void addStudent(StudentData student) {
		System.out.println(student.getName() + " id " + student.getId());
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
		idToRowMap.clear();
		int index = 0;
		for (StudentData data : studentData) {
			idToRowMap.put(data.getId(), index);
			index++;
		}
		fireTableDataChanged();		
	}
	


}
