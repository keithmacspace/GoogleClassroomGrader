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

import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.gui.StudentConsoleAreas;
import net.cdonald.googleClassroom.listenerCoordinator.AppendOutputTextListener;
import net.cdonald.googleClassroom.listenerCoordinator.GetDebugDialogQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentTextAreasQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.PreRunBlockingListener;
import net.cdonald.googleClassroom.listenerCoordinator.SystemInListener;
import net.cdonald.googleClassroom.listenerCoordinator.SystemOutListener;


public class ConsoleData {
	private PipedInputStream inPipe;
	private final PipedInputStream outPipe = new PipedInputStream();
	private final PipedInputStream errPipe = new PipedInputStream();
	private PrintWriter inWriter;
	private PrintStream outWriter;
	private PrintStream errWriter;
	private SwingWorker<Void, Character> consoleWorker;
	private SwingWorker<Void, Character> consoleErrWorker;
	private boolean done;
	private Map<String, StudentConsoleAreas> studentConsoleAreaMap;
	private String currentStudentID;
	private String currentRubricName;
	private JTextArea currentOutputTextArea;
	private JTextArea currentErrorTextArea;
	private JTextArea currentInputTextArea;
	private PrintStream oldOut;
	private PrintStream oldErr;
	private InputStream oldIn;
	private DebugLogDialog dbg;
	private static Semaphore runSemaphore = new Semaphore(1);	



	public ConsoleData() {
		oldOut = System.out;
		oldIn = System.in;
		oldErr = System.err;
		registerListeners();
		redirectConsole();
		studentConsoleAreaMap = new HashMap<String, StudentConsoleAreas>();
		
	}


	
	private void registerListeners() {
		ListenerCoordinator.addBlockingListener(SystemInListener.class, new SystemInListener() {
			@Override
			public void fired(String text) {
				// Only do this when we are running, don't accidentally absorb extra data.
				if (runSemaphore.availablePermits() == 0) {
					inWriter.println(text);
					//System.out.println();
					if (currentOutputTextArea != null) {
						currentOutputTextArea.append(text + "\n");
					}
					if (currentInputTextArea != null) {
						currentInputTextArea.append(text + "\n");
					}
				}		
			}
		});
		
		ListenerCoordinator.addBlockingListener(AppendOutputTextListener.class, new AppendOutputTextListener() {
			public void fired(String studentID, String rubricName, String text) {				
				getStudentConsoleAreaMap(studentID).appendToOutput(rubricName, text);
			}
		});
		
		ListenerCoordinator.addQueryResponder(GetStudentTextAreasQuery.class, new GetStudentTextAreasQuery() {
			@Override
			public StudentConsoleAreas fired(String studentID) {
				return getStudentConsoleAreaMap(studentID);
			}
		});		
	}
	
	public StudentConsoleAreas getStudentConsoleAreaMap(String studentID) {
		if (studentConsoleAreaMap.containsKey(studentID) == false) {
			studentConsoleAreaMap.put(studentID, new StudentConsoleAreas());
		}
		return studentConsoleAreaMap.get(studentID);		
	}
	
	public void assignmentSelected() {
		studentConsoleAreaMap.clear();
	}

	public void studentSelected(String id) {
		currentStudentID = id;
	}

	public void runStarted(String id, String rubricName) {
		// This will force us to wait until the last run stops
		try {
			runSemaphore.acquire();
		} catch (InterruptedException e) {

		}
		currentStudentID = id;
		currentRubricName = rubricName;
		StudentConsoleAreas currentArea = getStudentConsoleAreaMap(id);
		if (rubricName != "") {
			currentOutputTextArea = currentArea.getRubricArea(rubricName).getOutputArea();
			currentErrorTextArea = currentArea.getRubricArea(rubricName).getErrorArea();
			currentInputTextArea = null;
		}
		else {
			currentOutputTextArea = currentArea.getOutputAreas().getOutputArea();
			currentErrorTextArea = currentArea.getOutputAreas().getErrorArea();
			currentInputTextArea = currentArea.getInputArea();
		}
		currentOutputTextArea.setText("");
		currentErrorTextArea.setText("");
		if (currentInputTextArea != null) {
			currentInputTextArea.setText("");
		}
		redirectStreams();
		ListenerCoordinator.fire(PreRunBlockingListener.class, id);
	}
	private void runStopped() {		
		restoreStreams();
		inPipe = new PipedInputStream();
		try {
			inWriter = new PrintWriter(new PipedOutputStream(inPipe), true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.setIn(inPipe);	
		currentOutputTextArea = null;
		currentInputTextArea = null;
		currentStudentID = null;
		currentRubricName = null;
		runSemaphore.release();

	}
	
	private void redirectStreams() {
		inPipe = new PipedInputStream();
		try {
			if (outWriter == null) {
				outWriter = new PrintStream(new PipedOutputStream(outPipe), true);
			}
			if (errWriter == null) {
				errWriter = new PrintStream(new PipedOutputStream(errPipe), true);
			}
			inWriter = new PrintWriter(new PipedOutputStream(inPipe), true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.setIn(inPipe);
		System.setOut(outWriter);
		System.setErr(errWriter);
	}

	private void restoreStreams() {
		// Put things back
		System.out.flush();
		System.err.flush();
		System.setOut(oldOut);
		System.setIn(oldIn);
		System.setErr(oldErr);		
	}
	
	public void debugPrint() {

	}

	public void setDone() {
		restoreStreams();
		done = true;
		consoleWorker.cancel(true);
		consoleErrWorker.cancel(true);
	}
	private class ConsoleWorker extends SwingWorker<Void, Character> {
		private boolean outArea;
		public ConsoleWorker(boolean outArea) {
			this.outArea = outArea;
		}


		@Override
		protected void done() {
			super.done();
		}

		@Override
		protected void process(List<Character> chunks) {
			boolean done = false;
			String temp = "";
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
			if (dbg == null) {
				dbg = (DebugLogDialog)ListenerCoordinator.runQuery(GetDebugDialogQuery.class);
			}
			
			
			if (outArea) {
				//dbg.append("Out: " + temp + "\n");
				
				if (currentOutputTextArea != null) {
					currentOutputTextArea.append(temp);
				}
				if (done == true) {
					runStopped();
				}
				ListenerCoordinator.fire(SystemOutListener.class, currentStudentID, currentRubricName, temp, (Boolean)done);
				
			}
			else {
				//dbg.append("Err: " + temp + "\n");
				if (currentErrorTextArea != null) {
					currentErrorTextArea.append(temp);
				}
			}
		}

		@Override
		protected Void doInBackground() {
			try {
				while (true) {
					PipedInputStream pipe = (outArea) ? outPipe : errPipe;
					if (pipe.available() != 0) {
						publish((char) pipe.read());
					}
				}
			} catch (Exception e) {

			}
			// Show what happened
			return null;
		}
		
	}

	private void redirectConsole() {

		consoleWorker = new ConsoleWorker(true);
		consoleErrWorker = new ConsoleWorker(false);
		consoleWorker.execute();
		consoleErrWorker.execute();
	}

}
