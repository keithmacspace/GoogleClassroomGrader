package net.cdonald.googleClassroom.gui;

import net.cdonald.googleClassroom.model.FileData;

public interface RecompileListener extends CompileErrorListener {
	public void recompileAndRun(FileData fileData, String text);	
}

