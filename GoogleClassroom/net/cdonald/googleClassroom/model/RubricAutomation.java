package net.cdonald.googleClassroom.model;
import java.util.List;
import java.util.Map;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.AddRubricTabListener;
import net.cdonald.googleClassroom.listenerCoordinator.AppendOutputTextListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;



public abstract class RubricAutomation {

	private String ownerName;
	private RubricEntrySystemListeners sysListeners;
	

	public RubricAutomation() {		
		
	}

	
	public double runAutomation(CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData) {		
		return runAutomation_(message, compiler, consoleData);		
	}
	
	public void setOwnerName(String name) {
		ownerName = name;
		sysListeners = new RubricEntrySystemListeners(ownerName);
		ListenerCoordinator.fire(AddRubricTabListener.class, ownerName);
	}
		
	public String getOwnerName() {
		return ownerName;
	}
	
	protected void prepareForNextTest() {
		sysListeners.prepareForNextTest();
	}
	
	protected String getSysOutText(String studentID) {
		return sysListeners.getSysOutText(studentID);
	}

	
	protected void addOutput(String id, String text) {
		ListenerCoordinator.fire(AppendOutputTextListener.class, id, ownerName, text);
	}

	public abstract int getNumAutomationColumns();

	protected abstract double runAutomation_(CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData);
	protected abstract void loadAutomationColumns(String entryName, Map<String, List<List<Object>>> columnData);
	protected abstract void saveAutomationColumns(String entryName, List<List<Object>> columnData, Map<String, List<Object>> fileData);
	public abstract RubricAutomation newCopy();

}
