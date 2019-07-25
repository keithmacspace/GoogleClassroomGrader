package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultEditorKit;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.listenerCoordinator.AddRubricTabListener;
import net.cdonald.googleClassroom.listenerCoordinator.AssignmentSelected;
import net.cdonald.googleClassroom.listenerCoordinator.GetCompilerMessageQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetConsoleInputHistoryQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetConsoleOutputQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentFilesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.PreRunBlockingListener;
import net.cdonald.googleClassroom.listenerCoordinator.RecompileListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetRubricTextListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.SystemInListener;
import net.cdonald.googleClassroom.listenerCoordinator.SystemOutListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;

public class ConsoleAndSourcePanel extends JPanel {
	private static final long serialVersionUID = 1084761781946423738L;
	private List<JTextArea> sourceCodeList;
	private Map<String, JTextArea> rubricOutput;
	private JTabbedPane overallTabbedPane;
	private JTabbedPane sourceTabbedPane;
	private JTextField consoleInput;
	private JTextArea consoleOutput;
	private JPopupMenu popupSource;
	private JPopupMenu popupInput;
	private JPopupMenu popupDisplays;
	private FileData currentFile;

	private static Semaphore pauseSemaphore = new Semaphore(1);
	private JTextArea consoleInputHistory;

	public ConsoleAndSourcePanel() {

		setMinimumSize(new Dimension(400, 400));

		createPopupMenu();
		createLayout();
		registerListeners();
		setVisible(true);

	}

	public void assignmentSelected() {
		for (int i = 0; i < sourceTabbedPane.getTabCount(); i++) {
			sourceTabbedPane.getComponentAt(i).setVisible(false);
		}
		for (JTextArea sourceCode : sourceCodeList) {
			sourceCode.setText("");
		}
		consoleOutput.setText("");
		consoleInput.setText("");
		consoleInputHistory.setText("");
	}

	// We can select students too quickly and end up creating a mishmash in the
	// source code
	// window so use a semaphore to make sure that doesn't happen
	public void setWindowData(List<FileData> fileDataList, String outputText, String inputHistory) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				if (fileDataList != null) {
					// Use i <= the last pane will always be the compile message
					for (int i = sourceTabbedPane.getTabCount(); i <= fileDataList.size(); i++) {
						JPanel sourcePanel = new JPanel();
						sourcePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
						sourcePanel.setLayout(new BorderLayout());
						JTextArea sourceArea = new JTextArea();
						sourceCodeList.add(sourceArea);
						sourcePanel.add(new JScrollPane(sourceArea));
						sourceTabbedPane.addTab("", sourcePanel);
					}
					for (int i = fileDataList.size(); i < sourceTabbedPane.getTabCount(); i++) {
						sourceTabbedPane.getComponentAt(i).setVisible(false);
					}
					int sourceIndex = 0;
					for (FileData fileData : fileDataList) {
						setSourceContents(fileData.getName(), fileData.getFileContents(), sourceIndex);						
						currentFile = fileData;
						sourceIndex++;
					}
				} else {
					for (int i = 0; i < sourceTabbedPane.getTabCount(); i++) {
						sourceTabbedPane.getComponentAt(i).setVisible(false);
					}
				}
				if (outputText == null) {
					consoleOutput.setText("");
				}
				else {
					consoleOutput.setText(outputText);
				}
				if (inputHistory == null) {
					consoleInputHistory.setText("");
				}
				else {
					consoleInputHistory.setText(inputHistory);
				}
				consoleOutput.setCaretPosition(0);
				consoleInputHistory.setCaretPosition(0);
			}

		});

	}
	
	private void setSourceContents(String title, String text, int sourceIndex) {
		if (sourceIndex >= 0 && sourceIndex < sourceCodeList.size()) {
			sourceTabbedPane.getComponentAt(sourceIndex).setVisible(true);
			sourceTabbedPane.setTitleAt(sourceIndex, title);						
			JTextArea sourceCode = sourceCodeList.get(sourceIndex);
			double lineCount = sourceCode.getLineCount();
			double oldCaretRatio = 0;
			if (lineCount > 0) {
				oldCaretRatio = sourceCode.getCaretPosition();
				oldCaretRatio /= lineCount;
			}
			sourceCode.setText(text);
			int newCaretPosition = (int) (sourceCode.getLineCount() * oldCaretRatio);
			sourceCode.setCaretPosition(newCaretPosition);
		}
	}

	private void createPopupMenu() {
		popupSource = new JPopupMenu();
		popupDisplays = new JPopupMenu();
		popupInput = new JPopupMenu();

		Action cut = new DefaultEditorKit.CutAction();
		cut.putValue(Action.NAME, "Cut");
		cut.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
		popupSource.add(cut);
		popupInput.add(cut);

		Action copy = new DefaultEditorKit.CopyAction();
		copy.putValue(Action.NAME, "Copy");
		copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
		popupSource.add(copy);
		popupInput.add(cut);
		popupDisplays.add(copy);

		Action paste = new DefaultEditorKit.PasteAction();
		paste.putValue(Action.NAME, "Paste");
		paste.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
		popupSource.add(paste);
		popupInput.add(paste);

		JMenuItem recompile = new JMenuItem("Recompile And Run");
		popupSource.add(recompile);
		recompile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				int currentTab = sourceTabbedPane.getSelectedIndex();
				if (currentTab < sourceCodeList.size()) {
					ListenerCoordinator.fire(RecompileListener.class, currentFile,
							sourceCodeList.get(currentTab).getText());
				}

			}
		});
	}

	private void createLayout() {
		setSize(800, 500);
		setLayout(new BorderLayout());

		consoleInput = new JTextField();
		sourceCodeList = new ArrayList<JTextArea>();
		rubricOutput = new HashMap<String, JTextArea>();

		consoleInput.setText("");
		consoleInput.setMinimumSize(new Dimension(20, 25));
		consoleInput.setPreferredSize(new Dimension(20, 25));
		consoleInput.setComponentPopupMenu(popupInput);
		consoleOutput = new JTextArea();
		consoleInputHistory = new JTextArea();
		consoleInputHistory.setEditable(false);
		consoleOutput.setEditable(false);
		consoleInputHistory.setComponentPopupMenu(popupDisplays);
		consoleOutput.setComponentPopupMenu(popupDisplays);
//		sourceSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(sourceCode), ioSplit);
		// sourceSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new
		// JScrollPane(sourceCode), new JScrollPane(consoleOutput));
		// sourceSplit.setResizeWeight(0.8);

		JPanel ioPanel = new JPanel();
		ioPanel.setLayout(new BorderLayout());
		JPanel inputWrapper = new JPanel();
		inputWrapper.setLayout(new BorderLayout());
		;
		inputWrapper.setBorder(BorderFactory.createTitledBorder("Console Input"));
		// inputWrapper.setMinimumSize(new Dimension(40, 65));
		inputWrapper.add(consoleInput);

		JPanel outputWrapper = new JPanel();
		outputWrapper.setLayout(new BorderLayout());
		;
		outputWrapper.setBorder(BorderFactory.createTitledBorder("Console Output"));
		outputWrapper.add(new JScrollPane(consoleOutput));
		JPanel inputHistorWrapper = new JPanel();
		inputHistorWrapper.setLayout(new BorderLayout());
		;
		inputHistorWrapper.setBorder(BorderFactory.createTitledBorder("Input History"));
		inputHistorWrapper.add(new JScrollPane(consoleInputHistory));
		JSplitPane ioSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outputWrapper, inputHistorWrapper);
		ioSplit.setResizeWeight(0.9);

		ioPanel.add(ioSplit, BorderLayout.CENTER);
		ioPanel.add(inputWrapper, BorderLayout.SOUTH);
		setVisible(true);

		sourceTabbedPane = new JTabbedPane();
		sourceTabbedPane.setComponentPopupMenu(popupSource);

		overallTabbedPane = new JTabbedPane();
		overallTabbedPane.addTab("Source", sourceTabbedPane);
		overallTabbedPane.addTab("Console", ioPanel);
		add(overallTabbedPane, BorderLayout.CENTER);

		consoleInput.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String text = consoleInput.getText();
				consoleInputHistory.setText(text);
				consoleInput.setText("");
				ListenerCoordinator.fire(SystemInListener.class, text);
			}
		});

	}

	private void registerListeners() {
		ListenerCoordinator.addListener(SystemOutListener.class, new SystemOutListener() {
			@Override
			public void fired(String studentId, String rubricName, String text, Boolean finished) {
				consoleOutput.append(text);
			}
		});

		ListenerCoordinator.addListener(AddRubricTabListener.class, new AddRubricTabListener() {
			@Override
			public void fired(String rubricName) {
				if (rubricOutput.containsKey(rubricName) == false) {
					JPanel rubricPanel = new JPanel();
					rubricPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
					rubricPanel.setLayout(new BorderLayout());
					JTextArea rubricArea = new JTextArea();
					rubricOutput.put(rubricName, rubricArea);
					rubricPanel.add(new JScrollPane(rubricArea));
					rubricPanel.setVisible(true);
					overallTabbedPane.addTab(rubricName, rubricPanel);
				}
			}
		});

		ListenerCoordinator.addListener(SetRubricTextListener.class, new SetRubricTextListener() {
			@Override
			public void fired(String rubricName, String rubricText) {
				if (rubricText == null) {
					rubricText = "";
				}
				JTextArea area = rubricOutput.get(rubricName);
				if (area != null) {					
					area.setText(rubricText);
				}
			}
		});

		ListenerCoordinator.addBlockingListener(PreRunBlockingListener.class, new PreRunBlockingListener() {
			public void fired(ArrayList<FileData> sourceList) {
				overallTabbedPane.setSelectedIndex(1);
				try {
					// Doing this prevents forward progress until the panes are ready
					pauseSemaphore.release();
					pauseSemaphore.acquire();
					setWindowData(sourceList, "", "");
					// We will now hang here until the release in setWindowData
					pauseSemaphore.acquire();
					pauseSemaphore.release();
				} catch (InterruptedException e) {
				}
			}

		});

		ListenerCoordinator.addListener(AssignmentSelected.class, new AssignmentSelected() {
			@Override
			public void fired(ClassroomData data) {
				if (data == null || data.isEmpty()) {
					return;
				}
				assignmentSelected();
			}
		});

		ListenerCoordinator.addListener(StudentSelectedListener.class, new StudentSelectedListener() {

			@Override
			public void fired(String idToDisplay) {
				if (idToDisplay != null) {
					List<FileData> studentFiles = (List<FileData>) ListenerCoordinator.runQuery(GetStudentFilesQuery.class, idToDisplay);
					String consoleOutput = (String) ListenerCoordinator.runQuery(GetConsoleOutputQuery.class,
							idToDisplay, null);
					String consoleInput = (String) ListenerCoordinator.runQuery(GetConsoleInputHistoryQuery.class,
							idToDisplay);
					CompilerMessage compilerMessage = (CompilerMessage)ListenerCoordinator.runQuery(GetCompilerMessageQuery.class, idToDisplay);
					setWindowData(studentFiles, consoleOutput, consoleInput);
					if (compilerMessage != null) {
						setSourceContents("Compiler Message", compilerMessage.getCompilerMessage(), sourceCodeList.size() - 1);
					}
					else {
						setSourceContents("Compiler Message", "", sourceCodeList.size() - 1);
					}
					for (String rubricName : rubricOutput.keySet()) {
						String rubricText = (String)ListenerCoordinator.runQuery(GetConsoleOutputQuery.class,
								idToDisplay, rubricName);
						if (rubricText == null) {
							rubricText = "";
						}
						rubricOutput.get(rubricName).setText(rubricText);
					}
				} else {
					setWindowData(null, "", "");
					for (String rubricName : rubricOutput.keySet()) {
						rubricOutput.get(rubricName).setText("");
					}
				}
			}
		});

	}

}
