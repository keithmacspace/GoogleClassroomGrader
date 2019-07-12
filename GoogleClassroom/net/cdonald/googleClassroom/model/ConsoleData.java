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

import net.cdonald.googleClassroom.gui.ConsoleDisplayListener;
import net.cdonald.googleClassroom.gui.ConsoleInputListener;
import net.cdonald.googleClassroom.gui.MainToolBarListener;

public class ConsoleData implements ConsoleInputListener{
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
	private ConsoleDisplayListener listener;



	public ConsoleData() {
		oldOut = System.out;
		oldIn = System.in;
		outputMap = new HashMap<String, String>();
		inputMap = new HashMap<String, String>();
		redirectConsole();
	}
	
	public void assignmentSelected() {
		outputMap.clear();
		inputMap.clear();
	}


	public void addListener(ConsoleDisplayListener listener) {
		this.listener = listener;
		listener.addListener(this);
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
		listener.startRunning(fileDataList);
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
				if (listener != null) {
					listener.textAdded(temp);
				}
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

	@Override
	public void textInputted(String text) {
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
