package net.cdonald.googleClassroom.gui;

public interface MainMenuListener extends FileResponseListener, ClassSelectedListener, RunSelected {
	public void exitFired();
	public void exportFired();
	public void importFired();
 

}
