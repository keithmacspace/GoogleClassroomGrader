package net.cdonald.googleClassroom.gui;

import java.util.List;

import net.cdonald.googleClassroom.model.FileData;

public interface ConsoleDisplayListener {
	public void textAdded(String text);
	public void addListener(ConsoleInputListener listener);
	public void setRecompileListener(RecompileListener recompileListener);
	public void startRunning(List<FileData> sourceList);
}
