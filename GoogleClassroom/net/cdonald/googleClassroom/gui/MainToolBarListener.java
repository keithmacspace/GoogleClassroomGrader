package net.cdonald.googleClassroom.gui;

import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.GoogleSheetData;


public interface MainToolBarListener extends RunSelected, RunRubricSelected {
	public void assignmentSelected(ClassroomData data);
	public void rubricSelected(GoogleSheetData rubric);

}
