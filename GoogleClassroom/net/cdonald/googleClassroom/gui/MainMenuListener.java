package net.cdonald.googleClassroom.gui;

import net.cdonald.googleClassroom.model.ClassroomData;

public interface MainMenuListener extends SelectWorkingDirListener, ClassSelectedListener {
	public void runFired();
	public void exitFired();
	public void exportFired();
	public void importFired();
 

}
