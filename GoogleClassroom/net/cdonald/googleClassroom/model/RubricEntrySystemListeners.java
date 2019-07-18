package net.cdonald.googleClassroom.model;

import java.util.concurrent.Semaphore;

import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SystemOutListener;

public class RubricEntrySystemListeners {
	private String sysOutText;
	private SystemOutListener sysOutListener;
	private Semaphore semaphore = new Semaphore(1);
	
	private String owner;
	public RubricEntrySystemListeners(String owner) {
		this.owner = owner;
		sysOutText = "";
		sysOutListener = new SystemOutListener() {
			@Override
			public void fired(String text, Boolean finished) {
				sysOutText += text;
				if (finished) {
					semaphore.release();
				}
			}
		};
		ListenerCoordinator.addListener(SystemOutListener.class, sysOutListener);
			
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
		sysOutText = "";
		ListenerCoordinator.enableListener(SystemOutListener.class, sysOutListener);
	}
	private void detach() {
		
		ListenerCoordinator.disableListener(SystemOutListener.class, sysOutListener);	
	}
	
	public String getSysOutText() {
		// We have to wait until the current invocation finishes so that we 
		// can get all the sysOut values

		try {
			semaphore.acquire();
			semaphore.release();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String copy = sysOutText;;
		detach();
		return copy;
	}
}
