package net.cdonald.googleClassroom.listenerCoordinator;

import java.util.List;

import net.cdonald.googleClassroom.model.FileData;

public interface LoadTestFileListener {
	public void fired(List<FileData> allFiles);
}
