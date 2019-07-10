package net.cdonald.googleClassroom.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.StudentData;

public class StudentListRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = -7082168845923165249L;
	private Date assignmentDate;
	private Font redFont;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		Component c = null;
		boolean makeRed = false;
		if (value != null) {
			String valueString = null;

			switch (column) {
			case StudentListModel.DATE_COLUMN:
				Date date = ((FileData)value).getDate();
				if (date != null) {
					valueString = date.toString();

					if (assignmentDate != null && date.compareTo(assignmentDate) < 0) {
						makeRed = true;
					}					
				}else {
					valueString = value.toString();
				}
				break;
			case StudentListModel.COMPILER_COLUMN:
				CompilerMessage message = (CompilerMessage) value;
				if (message.isSuccessful()) {
					valueString = "Y";
				} else {
					valueString = "N - " + message.getCompilerMessage();
					makeRed = true;
				}
				break;
			case StudentListModel.LAST_NAME_COLUMN:
				StudentData studentData = (StudentData)value;
				valueString = studentData.getFirstName();
				break;
			case StudentListModel.FIRST_NAME_COLUMN:
				StudentData lastNameStudentData = (StudentData)value;
				valueString = lastNameStudentData.getName();
				break;
			default:
				valueString = value.toString();
				break;				
			}
			c = super.getTableCellRendererComponent(table, valueString, isSelected, hasFocus, row, column);
		} else {
			c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
		if (makeRed) {
			c.setForeground(Color.RED);
		} else {
			c.setForeground(Color.BLACK);
		}
		return c;
	}

	public void setAssignment(ClassroomData assignment) {
		if (assignment == null) {
			assignmentDate = null;
		} else {
			assignmentDate = assignment.getDate();
		}
	}

}
