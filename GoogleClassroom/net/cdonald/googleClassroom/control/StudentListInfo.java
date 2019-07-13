package net.cdonald.googleClassroom.control;

import net.cdonald.googleClassroom.gui.StudentListModel;


public interface StudentListInfo {
	public static final int LAST_NAME_COLUMN = 0;
	public static final int FIRST_NAME_COLUMN = 1;
	public static final int DATE_COLUMN = 2;
	public static final int COMPILER_COLUMN = 3;
	public static final int NUM_DEFAULT_COLUMNS = 4;
	public static final String[] defaultColumnNames = {"Last Name", "First Name", "Date", "Compiles"};
	public int getRowCount();
	public int getColumnCount();
	public Object getValueAt(int rowIndex, int columnIndex);
	public void setValueAt(Object value, int rowIndex, int columnIndex);
	public String getColumnTip(int column);
	public String getColumnName(int column);
	public void setOwner(StudentListModel owner);
}
