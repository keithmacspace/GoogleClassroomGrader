package net.cdonald.googleClassroom.gui;

import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.Rubric;

public interface MainToolBarListener {
	public void assignmentSelected(ClassroomData data);
	public void rubricSelected(Rubric rubric);
	public void runClicked();
}
