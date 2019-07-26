package net.cdonald.googleClassroom.gui;

import java.util.Arrays;

import javax.swing.table.DefaultTableModel;

import net.cdonald.googleClassroom.model.RubricEntry;

public class RubricElementTableModel extends DefaultTableModel {
	private static final long serialVersionUID = 338186915498223268L;

	public RubricElementTableModel() {
		super(Arrays.copyOfRange(RubricEntry.HeadingNames.values(), 1, RubricEntry.HeadingNames.values().length), 0);
		String[] headers = new String[RubricEntry.HeadingNames.values().length - 1];
		int headerIndex = 0;
		for (int i = 1; i < RubricEntry.HeadingNames.values().length; i++, headerIndex++) {
			headers[headerIndex] = RubricEntry.HeadingNames.values()[i].toString();
		}
		System.out.println(Arrays.toString(headers));
		setColumnIdentifiers(headers);
		
	}
	
	@Override
	public void addRow(Object[] rowData) {
		// TODO Auto-generated method stub
		super.addRow(rowData);
	}

	@Override
	public int getRowCount() {
		
		int count = super.getRowCount();
		if (count < 30) {
			return 30;
		}
		return count;
	}

	@Override
	public void setValueAt(Object aValue, int row, int column) {
		// TODO Auto-generated method stub
		super.setValueAt(aValue, row, column);
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

//	@Override
//	public int getColumnCount() {
//		// TODO Auto-generated method stub
//		return RubricEntry.HeadingNames.values().length - 1;
//	}

}
