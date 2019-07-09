package gui;

import model.ClassroomData;
import model.Rubric;

public interface MainToolBarListener {
	public void classSelected(ClassroomData data);
	public void assignmentSelected(ClassroomData data);
	public void rubricSelected(Rubric rubric);
	public void runClicked();
	public void runAllClicked();
}
