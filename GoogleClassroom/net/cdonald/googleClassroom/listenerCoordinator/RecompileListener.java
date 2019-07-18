package net.cdonald.googleClassroom.listenerCoordinator;
import net.cdonald.googleClassroom.model.FileData;

public interface RecompileListener {
	public void fired(FileData fileData, String text);	
}

