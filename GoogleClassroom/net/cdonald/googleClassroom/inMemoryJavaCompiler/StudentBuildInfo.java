package net.cdonald.googleClassroom.inMemoryJavaCompiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.model.FileData;

/**
 * 
 * This class holds all the information about a single student's build 
 * It holds the source files, the compiler message, and the ClassLoader
 * that should be used to run the data
 *
 */
public class StudentBuildInfo {
	private Map<String, Class<?>> studentCompilerMap;
	private List<FileData> studentFileData;
	private CompilerMessage compilerMessage;
	
	public StudentBuildInfo() {
		compilerMessage = null;
		studentCompilerMap = null;
		studentFileData = new ArrayList<FileData>();
	}

	
	public List<FileData> getStudentFileData() {
		return studentFileData;
	}
	
	public void addFileData(FileData data) {
		studentFileData.add(data);
	}

	public CompilerMessage getCompilerMessage() {
		return compilerMessage;
	}

	public void setCompilerMessage(CompilerMessage compilerMessage) {
		this.compilerMessage = compilerMessage;
	}
	
	public Map<String, Class<?>> getStudentCompilerMap() {
		return studentCompilerMap;
	}

	public void setStudentCompilerMap(Map<String, Class<?>> studentCompilerMap) {
		this.studentCompilerMap = studentCompilerMap;
	}		

}
