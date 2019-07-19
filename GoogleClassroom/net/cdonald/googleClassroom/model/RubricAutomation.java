package net.cdonald.googleClassroom.model;

import java.util.HashMap;
import java.util.Map;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.AddRubricTabListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SetRubricTextListener;


public abstract class RubricAutomation {
	private Map<String, String> perStudentOutput;
	private String ownerName;
	public RubricAutomation() {		
		perStudentOutput = new HashMap<String, String>();
	}
	
	public String getRubricOutput(String studentId) {
		return perStudentOutput.get(studentId);
	}
	
	public double runAutomation(CompilerMessage message, StudentWorkCompiler compiler) {
		perStudentOutput.put(message.getStudentId(), "");
		ListenerCoordinator.fire(SetRubricTextListener.class, ownerName, "");
		return runAutomation_(message, compiler);		
	}
	protected abstract double runAutomation_(CompilerMessage message, StudentWorkCompiler compiler);
	public void setOwnerName(String name) {
		ownerName = name;
		ListenerCoordinator.fire(AddRubricTabListener.class, ownerName);
	}
	protected void addOutput(String id, String text) {
		String textToDisplay = perStudentOutput.get(id) + text;
		perStudentOutput.put(id, textToDisplay);
		ListenerCoordinator.fire(SetRubricTextListener.class, ownerName, textToDisplay);
	}

}
