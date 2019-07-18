package net.cdonald.googleClassroom.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.swing.SwingWorker;

import net.cdonald.googleClassroom.listenerCoordinator.GetConsoleInputHistoryQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetConsoleOutputQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.PreRunBlockingListener;
import net.cdonald.googleClassroom.listenerCoordinator.SystemInListener;
import net.cdonald.googleClassroom.listenerCoordinator.SystemOutListener;


public class ConsoleData {
	private PipedInputStream inPipe;
	private final PipedInputStream outPipe = new PipedInputStream();
	private PrintWriter inWriter;
	private PrintStream outWriter;	
	private SwingWorker<Void, Character> consoleWorker;
	private boolean done;
	private Map<String, String> outputMap;
	private Map<String, String> inputMap;
	private String currentStudentID;
	private PrintStream oldOut;
	private InputStream oldIn;
	private static Semaphore runSemaphore = new Semaphore(1);	



	public ConsoleData() {
		oldOut = System.out;
		oldIn = System.in;
		outputMap = new HashMap<String, String>();
		inputMap = new HashMap<String, String>();
		registerListeners();
		redirectConsole();
		
	}
	
	private void registerListeners() {
		ListenerCoordinator.addListener(SystemInListener.class, new SystemInListener() {
			@Override
			public void fired(String text) {
				// Only do this when we are running, don't accidentally absorb extra data.
				if (runSemaphore.availablePermits() == 0) {
					inWriter.println(text);
					System.out.println();
					String temp = outputMap.get(currentStudentID) + text;
					outputMap.put(currentStudentID, temp);
					temp = inputMap.get(currentStudentID) + text;
					inputMap.put(currentStudentID, temp);
				}		
			}
		});
		
		ListenerCoordinator.addQueryResponder(GetConsoleOutputQuery.class, new GetConsoleOutputQuery() {
			@Override
			public String fired(String studentID) {
				// TODO Auto-generated method stub
				return outputMap.get(studentID);
			}			
		});
		ListenerCoordinator.addQueryResponder(GetConsoleInputHistoryQuery.class, new GetConsoleInputHistoryQuery() {
			@Override
			public String fired(String studentID) {
				// TODO Auto-generated method stub
				return inputMap.get(studentID);
			}			
		});

		
	}
	
	public void assignmentSelected() {
		outputMap.clear();
		inputMap.clear();
	}

	public void studentSelected(String id) {
		currentStudentID = id;
	}

	public void runStarted(String id, List<FileData> fileDataList) {
		// This will force us to wait until the last run stops
		try {
			runSemaphore.acquire();
		} catch (InterruptedException e) {

		}
		ListenerCoordinator.fire(PreRunBlockingListener.class, fileDataList);

		currentStudentID = id;
		outputMap.put(id, "");
		inputMap.put(id, "");

	}
	private void runStopped() {		

		inPipe = new PipedInputStream();
		try {
			inWriter = new PrintWriter(new PipedOutputStream(inPipe), true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.setIn(inPipe);		
		runSemaphore.release();

	}
	
	public void debugPrint() {

	}

	public void setDone() {
		// Put things back
		System.out.flush();
		System.setOut(oldOut);
		System.setIn(oldIn);
		done = true;
		consoleWorker.cancel(true);
	}

	private void redirectConsole() {
		inPipe = new PipedInputStream();
		try {
			outWriter = new PrintStream(new PipedOutputStream(outPipe), true);
			inWriter = new PrintWriter(new PipedOutputStream(inPipe), true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.setIn(inPipe);

		done = false;


		System.setOut(outWriter);

		consoleWorker = new SwingWorker<Void, Character>() {
			@Override
			protected void done() {
				super.done();
			}

			@Override
			protected void process(List<Character> chunks) {
				boolean done = false;
				String temp = outputMap.get(currentStudentID);
				for (Character str : chunks) {					
					// Special flag sent by StudentWorkCompiler to tell us when we are done running
					// a
					// program. I know it is a little ugly, I just can't figure out how to wait
					// until
					// all the output finishes flowing down
					if (str == 0) {						
						done = true;
					} else {						
						temp += str.toString();
					}
				}
				ListenerCoordinator.fire(SystemOutListener.class, temp, (Boolean)done);
				outputMap.put(currentStudentID, temp);
				if (done == true) {
					runStopped();
				}
			}

			@Override
			protected Void doInBackground() {
				try {
					while (done == false) {
						if (outPipe.available() != 0) {
							publish((char) outPipe.read());
						}
					}
				} catch (Exception e) {

				}
				// Show what happened
				return null;
			}

		};
		consoleWorker.execute();
	}

	public String getConsoleOutput(String studentID) {
		if (outputMap.containsKey(studentID)) {
			return outputMap.get(studentID);
		}
		return "";
	}
	
	public String getConsoleInputHistory(String studentID) {
		if (inputMap.containsKey(studentID)) {
			return inputMap.get(studentID);
		}
		return "";	
	}
	

}
