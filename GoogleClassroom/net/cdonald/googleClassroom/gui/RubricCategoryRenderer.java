package net.cdonald.googleClassroom.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.cdonald.googleClassroom.model.RubricEntry;


public class RubricCategoryRenderer implements TableCellRenderer {

	private JComboBox<RubricEntry.HeadingNames> combo;
	public RubricCategoryRenderer() {
		combo = new JComboBox<RubricEntry.HeadingNames>(RubricEntry.HeadingNames.values());
		combo.setBackground(Color.WHITE);
	}
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int colum) {
		combo.setSelectedItem(value);
		return combo;
	}
	
	

}
