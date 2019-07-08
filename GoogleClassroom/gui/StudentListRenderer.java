package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import inMemoryJavaCompiler.CompilerMessage;
import model.ClassroomData;

public class StudentListRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = -7082168845923165249L;
	private Date assignmentDate;
	private Font redFont;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		String valueString = null;
		boolean makeRed = false;
		if (value != null && value instanceof Date) {
			Date date = (Date) value;
			valueString = date.toString();
			
			if (assignmentDate != null && date.compareTo(assignmentDate) < 0) {
				makeRed = true;
			}
		}
		if (value != null && value instanceof CompilerMessage) {
			CompilerMessage message = (CompilerMessage) value;
			if (message.isSuccessful()) {
				valueString = "Y";
			} else {
				valueString = "N - " + message.getCompilerMessage();
				makeRed = true;
			}
		}

		Component c = super.getTableCellRendererComponent(table, valueString, isSelected, hasFocus, row, column);
		if (makeRed) {			
			c.setForeground(Color.RED);
		}
		else {
			c.setForeground(Color.BLACK);
		}

		return c;
	}

	public void setAssignment(ClassroomData assignment) {
		if (assignment == null) {
			assignmentDate = null;
		}
		else {
			assignmentDate = assignment.getDate();
		}
	}

}
