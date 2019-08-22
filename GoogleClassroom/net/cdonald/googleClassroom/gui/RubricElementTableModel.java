package net.cdonald.googleClassroom.gui;

import java.util.Arrays;

import javax.swing.table.DefaultTableModel;

import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;

public class RubricElementTableModel extends DefaultTableModel {
	private static final long serialVersionUID = 338186915498223268L;
	private Rubric rubricToModify;

	public RubricElementTableModel() {
		super(Arrays.copyOfRange(RubricEntry.HeadingNames.values(), 1, RubricEntry.HeadingNames.values().length), 0);
		String[] headers = new String[RubricEntry.HeadingNames.values().length - 1];
		int headerIndex = 0;
		for (int i = 1; i < RubricEntry.HeadingNames.values().length; i++, headerIndex++) {
			headers[headerIndex] = RubricEntry.HeadingNames.values()[i].toString();
		}
		setColumnIdentifiers(headers);
		
	}
	
	public void setRubricToModify(Rubric rubricToModify) {
		this.rubricToModify = rubricToModify;
	}

	@Override
	public int getRowCount() {
		int size = 1;
		if (rubricToModify != null) {
			size = rubricToModify.getEntryCount() + 1;
		}
		return size;
	}
	RubricEntry.HeadingNames getColumnHeading(int column) {
		String columnName = getColumnName(column);
		return RubricEntry.HeadingNames.valueOf(columnName);	
	}

	@Override
	public Object getValueAt(int row, int column) {		
		RubricEntry entry = rubricToModify.getEntry(row);
		Object retVal = entry.getTableValue(getColumnHeading(column));
		return retVal;
	}

	@Override
	public void setValueAt(Object aValue, int row, int column) {		
		RubricEntry entry = rubricToModify.getEntry(row);
		entry.setTableValue(getColumnHeading(column), aValue);
		if (row == getRowCount() - 1) {
			this.fireTableStructureChanged();
		}
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		if (column == 0) {
			if (row == 0) {
				return true;
			}
			RubricEntry entry = rubricToModify.getEntry(row - 1);
			// Only allow filling in in order (if the entry above is empty, don't allow
			// more entries
			if (entry.getName() != null && entry.getName().length() != 0) {
				return true;
			}
		}
		else {
			RubricEntry entry = rubricToModify.getEntry(row);
			return (entry.getName() != null && entry.getName().length() != 0);
		}
		return false;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		RubricEntry.HeadingNames heading = RubricEntry.HeadingNames.values()[columnIndex + 1];
		switch(heading) {
		case VALUE:
			return Integer.class;
		case AUTOMATION_TYPE:
			return RubricEntry.AutomationTypes.class;
		default:
			return String.class;
		}
	}


}
