package net.cdonald.googleClassroom.gui;

import net.cdonald.googleClassroom.model.ClassroomData;

public interface MainMenuListener extends FileResponseListener, ClassSelectedListener, RunSelected {
	public void exitFired();
	public void exportFired();
	public void importFired();
 

}
