package net.cdonald.googleClassroom.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import net.cdonald.googleClassroom.control.StudentListInfo;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.StudentData;

public class StudentListRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = -7082168845923165249L;


	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		Component c = null;
		boolean makeRed = false;
		if (value != null) {
			String valueString = null;

			switch (column) {
//			case StudentListInfo.DATE_COLUMN:
//				Date date = ((FileData)value).getDate();
//				if (date != null) {
//					valueString = date.toString();
//
//					if (assignmentDate != null && date.compareTo(assignmentDate) < 0) {
//						makeRed = true;
//					}					
//				}else {
//					valueString = value.toString();
//				}
//				break;
			case StudentListInfo.COMPILER_COLUMN:
				if (value != null) {
					valueString = (String)value;
					if (valueString.compareTo("Y") != 0) {
						makeRed = true;
					}
				}
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


}
