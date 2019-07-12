package net.cdonald.googleClassroom.gui;

import net.cdonald.googleClassroom.model.ClassroomData;


public interface MainToolBarListener extends RunSelected, RunRubricSelected {
	public void assignmentSelected(ClassroomData data);
	public void rubricSelected(ClassroomData rubric);

}
