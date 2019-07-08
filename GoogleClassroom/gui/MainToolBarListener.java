package gui;

import model.ClassroomData;

public interface MainToolBarListener {
	public void classSelected(ClassroomData data);
	public void assignmentSelected(ClassroomData data);
	public void runClicked();
	public void runAllClicked();
}
