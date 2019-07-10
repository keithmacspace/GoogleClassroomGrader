package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;


import net.cdonald.googleClassroom.model.FileData;

public class ConsoleAndSourcePanel extends JPanel {
	private static final long serialVersionUID = 1084761781946423738L;
	private PipedInputStream inPipe;
	private final PipedInputStream outPipe = new PipedInputStream();
	private PrintWriter inWriter;
	private PrintStream outWriter;
	private JTextArea sourceCode;
	private SwingWorker<Void, Character> consoleWorker;
	private JTextField consoleInput;

	private JTextArea consoleOutput;
	private JSplitPane sourceSplit;
	//private JSplitPane ioSplit;
	//private Map<String, String> inputMap;
	//private JTextArea consoleInputDisplay;
	private boolean done;
	private Map<String, String> outputMap;

	private String currentRun;
	private PrintStream oldOut;
	private InputStream oldIn;
	private static Semaphore runSemaphore = new Semaphore(1);

	public ConsoleAndSourcePanel() {
		oldOut = System.out;
		oldIn = System.in;

		setMinimumSize(new Dimension(400, 400));
		outputMap = new HashMap<String, String>();
		//inputMap = new HashMap<String, String>();
		setSize(800, 500);
		setLayout(new BorderLayout());
		redirectConsole();
		setVisible(true);

	}
	
	public void assignmentSelected() {
		sourceCode.setText("");
		consoleOutput.setText("");
		consoleInput.setText("");
		//consoleInputDisplay.setText("");
		outputMap.clear();
	}

	public void studentSelected(String id, List<FileData> fileDataList, Semaphore pauseSemaphore) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				sourceCode.setText("");				

				if (currentRun == null) {
					if (outputMap.containsKey(id)) {
						consoleOutput.setText(outputMap.get(id));
					} 
					else {
						consoleOutput.setText("");
					}
//					if (inputMap.containsKey(id)) {
//						consoleInputDisplay.setText(inputMap.get(id));
//					}
//					else {
//						consoleInputDisplay.setText("");
//					}
				}

				if (fileDataList != null) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							for (FileData fileData : fileDataList) {
								sourceCode.append(fileData.getFileContents());
								sourceCode.setCaretPosition(0);
							}
						}
					});
				}
				if (pauseSemaphore != null) {
					pauseSemaphore.release();
				}
			}

		});

	}

	public void runStarted(String id) {
		// This will force us to wait until the last run stops
		try {
			runSemaphore.acquire();
		} catch (InterruptedException e) {

		}
		currentRun = id;
		consoleOutput.setText("");
		consoleInput.setText("");
		//consoleInputDisplay.setText("");
	}
	public void runStopped() {		
		outputMap.put(currentRun, consoleOutput.getText());
		//inputMap.put(currentRun, consoleInputDisplay.getText());
		currentRun = null;
		inPipe = new PipedInputStream();
		try {
			inWriter = new PrintWriter(new PipedOutputStream(inPipe), true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.setIn(inPipe);		
		runSemaphore.release();
		
		System.err.println("done");
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
		consoleInput = new JTextField();

		sourceCode = new JTextArea();
		consoleInput.setText("");
		consoleInput.setMinimumSize(new Dimension(20, 25));
		consoleInput.setPreferredSize(new Dimension(20, 25));
		consoleOutput = new JTextArea();
//		consoleInputDisplay = new JTextArea();
//		consoleInputDisplay.setEditable(false);
//		ioSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(consoleOutput), new JScrollPane(consoleInputDisplay));
//		ioSplit.setResizeWeight(0.9);
//		sourceSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(sourceCode), ioSplit);
		sourceSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(sourceCode), new JScrollPane(consoleOutput));
		sourceSplit.setResizeWeight(0.8);
		
		add(sourceSplit, BorderLayout.CENTER);
		add(consoleInput, BorderLayout.SOUTH);


		consoleInput.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String text = consoleInput.getText();
				consoleInput.setText("");
				inWriter.println(text);
				consoleOutput.append(text + "\n");
				//consoleInputDisplay.append(text + "\n");
				
			}
		});

		System.setOut(outWriter);

		consoleWorker = new SwingWorker<Void, Character>() {
			@Override
			protected void done() {
				super.done();
			}

			@Override
			protected void process(List<Character> chunks) {
				for (Character str : chunks) {
					// Special flag sent by StudentWorkCompiler to tell us when we are done running
					// a
					// program. I know it is a little ugly, I just can't figure out how to wait
					// until
					// all the output finishes flowing down
					if (str == 0) {
						runStopped();
					} else {
						consoleOutput.append(str.toString());
					}

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
	

}
