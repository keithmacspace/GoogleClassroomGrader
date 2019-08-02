package net.cdonald.googleClassroom.gui;

import net.cdonald.googleClassroom.model.RubricEntry;

public interface RubricElementListener {
	public void typeSelected(RubricEntry.AutomationTypes typeSelected, boolean isSelected);
}
