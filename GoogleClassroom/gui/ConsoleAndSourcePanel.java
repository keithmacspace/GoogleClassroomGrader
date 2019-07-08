package gui;

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
import java.util.Scanner;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import model.FileData;

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
	private boolean done;
	private Map<String, String> outputMap;
	private String currentRun;
	private PrintStream oldOut;
	private InputStream oldIn;

	public ConsoleAndSourcePanel() {
		oldOut = System.out;
		oldIn = System.in;

		setMinimumSize(new Dimension(400, 400));
		outputMap = new HashMap<String, String>();
		setSize(800, 500);
		setLayout(new BorderLayout());
		redirectConsole();
		setVisible(true);

	}

	public void studentSelected(String id, List<FileData> fileDataList) {
		sourceCode.setText("");
		if (outputMap.containsKey(id)) {
			consoleOutput.setText(outputMap.get(id));
		} else {
			consoleOutput.setText("");
		}
		if (fileDataList != null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					for (FileData fileData : fileDataList) {
						sourceCode.append(fileData.getFileContents());
					}
				}
			});
		}
	}

	public void runStarted(String id) {
		currentRun = id;
		consoleOutput.setText("");
	}

	public void runStopped() {
		currentRun = null;		
		inPipe = new PipedInputStream();
		try {
			inWriter = new PrintWriter(new PipedOutputStream(inPipe), true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.setIn(inPipe);
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
		sourceSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(sourceCode),
				new JScrollPane(consoleOutput));
		sourceSplit.setResizeWeight(0.5);
		add(sourceSplit, BorderLayout.CENTER);
		add(consoleInput, BorderLayout.SOUTH);

		consoleOutput.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				if (currentRun != null) {
					outputMap.put(currentRun, consoleOutput.getText());
				}

			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				if (currentRun != null) {
					outputMap.put(currentRun, consoleOutput.getText());
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				if (currentRun != null) {
					outputMap.put(currentRun, consoleOutput.getText());
				}
			}

		});

		consoleInput.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String text = consoleInput.getText();
				consoleInput.setText("");
				inWriter.println(text);
				consoleOutput.append(text + "\n");

			}
		});
		System.setOut(outWriter);

		consoleWorker = new SwingWorker<Void, Character>() {
			@Override
			protected void done() {
				System.out.println("Closing streams");
				super.done();
			}

			@Override
			protected void process(List<Character> chunks) {
				for (Character str : chunks) {
					consoleOutput.append(str.toString());
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
