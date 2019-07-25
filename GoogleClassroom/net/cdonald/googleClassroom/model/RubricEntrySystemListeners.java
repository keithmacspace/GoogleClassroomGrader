package net.cdonald.googleClassroom.model;

import java.util.concurrent.Semaphore;

import net.cdonald.googleClassroom.listenerCoordinator.GetConsoleOutputQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SystemOutListener;

public class RubricEntrySystemListeners {	
	private SystemOutListener sysOutListener;
	private Semaphore semaphore = new Semaphore(1);
	
	private String owner;
	public RubricEntrySystemListeners(String owner) {
		this.owner = owner;
		sysOutListener = new SystemOutListener() {
			@Override
			public void fired(String studentID, String rubricName, String text, Boolean finished) {
				if (finished) {
					semaphore.release();
				}
			}
		};
		ListenerCoordinator.addListener(SystemOutListener.class, sysOutListener);
		ListenerCoordinator.disableListener(SystemOutListener.class, sysOutListener);
			
	}
	
	@Override
	public String toString() {		
		return "System Listener " + owner;
	}

	public void prepareForNextTest() {
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		attach();
	}
	
	private void attach() {		
		ListenerCoordinator.enableListener(SystemOutListener.class, sysOutListener);
	}
	private void detach() {
		
		ListenerCoordinator.disableListener(SystemOutListener.class, sysOutListener);	
	}
	
	public String getSysOutText(String studentID) {
		// We have to wait until the current invocation finishes so that we 
		// can get all the sysOut values

		String copy = null;
		try {
			semaphore.acquire();
			copy = (String)ListenerCoordinator.runQuery(GetConsoleOutputQuery.class, studentID, owner);			
			detach();
			semaphore.release();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return copy;
	}
}
