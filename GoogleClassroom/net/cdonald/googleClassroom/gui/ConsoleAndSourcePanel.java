package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.cdonald.googleClassroom.listenerCoordinator.AddRubricTabsListener;
import net.cdonald.googleClassroom.listenerCoordinator.AssignmentSelected;
import net.cdonald.googleClassroom.listenerCoordinator.GetCompilerMessageQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentFilesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentTextAreasQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.PreRunBlockingListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.SystemInListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;

public class ConsoleAndSourcePanel extends JPanel {
	private static final long serialVersionUID = 1084761781946423738L;
	private JTabbedPane overallTabbedPane;
	private JTabbedPane sourceTabbedPane;
	private JTabbedPane rubricTabbedPane;
	private JTextField consoleInput;
	private JPopupMenu popupSource;
	private JPopupMenu popupInput;
	private JPopupMenu popupDisplays;
	private SplitOutErrPanel outputWrapperPanel;
	private JPanel inputHistorWrapperPanel;
	private Map<String, SplitOutErrPanel> rubricPanels;
	private static Semaphore pauseSemaphore = new Semaphore(1);
	private JTextArea currentInputHistory;


	public ConsoleAndSourcePanel() {

		setMinimumSize(new Dimension(400, 400));

		createPopupMenu();
		createLayout();
		registerListeners();
		setVisible(true);
		rubricPanels = new HashMap<String, SplitOutErrPanel>();

	}

	public void assignmentSelected() {
		sourceTabbedPane.removeAll();
		consoleInput.setText("");		
	}

	// We can select students too quickly and end up creating a mishmash in the
	// source code
	// window so use a semaphore to make sure that doesn't happen
	public void setWindowData(String idToDisplay) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (idToDisplay != null) {
					List<FileData> fileDataList = (List<FileData>) ListenerCoordinator.runQuery(GetStudentFilesQuery.class, idToDisplay);
					CompilerMessage compilerMessage = (CompilerMessage)ListenerCoordinator.runQuery(GetCompilerMessageQuery.class, idToDisplay);

					sourceTabbedPane.removeAll();
					if (fileDataList != null) {
						int sourceIndex = 0;
						for (FileData fileData : fileDataList) {
							setSourceContents(fileData.getName(), fileData.getFileContents());													
							sourceIndex++;
						}
						if (compilerMessage != null && compilerMessage.getCompilerMessage() != null && compilerMessage.getCompilerMessage().length() > 2) {
								setSourceContents("Compiler Message", compilerMessage.getCompilerMessage());
						}

					} else {
						for (int i = 0; i < sourceTabbedPane.getTabCount(); i++) {
							sourceTabbedPane.getComponentAt(i).setVisible(false);
						}
					}
				}
				else {
					for (int i = 0; i < sourceTabbedPane.getTabCount(); i++) {
						sourceTabbedPane.getComponentAt(i).setVisible(false);
					}
				}
				bindStudentAreas(idToDisplay);
			}

		});

	}
	
	private void setSourceContents(String title, String text) {
		JPanel sourcePanel = new JPanel();
		sourcePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		sourcePanel.setLayout(new BorderLayout());
		JTextArea sourceArea = new JTextArea();
		sourceArea.setText(text);
		sourcePanel.add(new JScrollPane(sourceArea));
		sourceTabbedPane.addTab(title, sourcePanel);
		sourceArea.setCaretPosition(0);		
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
//		recompile.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//
//				int currentTab = sourceTabbedPane.getSelectedIndex();
//				if (currentTab < sourceCodeList.size()) {
//					ListenerCoordinator.fire(RecompileListener.class, currentFile,
//							sourceCodeList.get(currentTab).getText());
//				}
//
//			}
//		});
	}
	private class SplitOutErrPanel {
		private JPanel out;
		private JPanel err;
		private JSplitPane splitPane;
		public SplitOutErrPanel() {
			out = new JPanel();
			err = new JPanel();
			out.setLayout(new BorderLayout());			
			out.setBorder(BorderFactory.createTitledBorder("System.out"));
			err.setLayout(new BorderLayout());			
			err.setBorder(BorderFactory.createTitledBorder("System.err"));
			out.setComponentPopupMenu(popupDisplays);
			err.setComponentPopupMenu(popupDisplays);
			splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, out, err);
			splitPane.setResizeWeight(0.9);
		}
		
		public JSplitPane getSplitPane() {
			return splitPane;
		}

		public void clearPanels() {
			clearPanel(out);
			clearPanel(err);
		}
		public void clearAndAdd(StudentConsoleAreas.OutputAreas outputAreas) {
			clearAndAddPanel(out, outputAreas.getOutputArea());
			clearAndAddPanel(err, outputAreas.getErrorArea());
		}
	}

	private void createLayout() {
		setSize(800, 500);
		setLayout(new BorderLayout());

		consoleInput = new JTextField();



		consoleInput.setText("");
		consoleInput.setMinimumSize(new Dimension(20, 25));
		consoleInput.setPreferredSize(new Dimension(20, 25));
		consoleInput.setComponentPopupMenu(popupInput);


		JPanel ioPanel = new JPanel();
		ioPanel.setLayout(new BorderLayout());
		JPanel inputWrapper = new JPanel();
		inputWrapper.setLayout(new BorderLayout());
		;
		inputWrapper.setBorder(BorderFactory.createTitledBorder("Console Input"));		
		inputWrapper.add(consoleInput);

		outputWrapperPanel = new SplitOutErrPanel();


		inputHistorWrapperPanel = new JPanel();
		inputHistorWrapperPanel.setLayout(new BorderLayout());
		;
		inputHistorWrapperPanel.setBorder(BorderFactory.createTitledBorder("Input History"));
		inputHistorWrapperPanel.setComponentPopupMenu(popupDisplays);


		JSplitPane ioSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outputWrapperPanel.getSplitPane(), inputHistorWrapperPanel);
		ioSplit.setResizeWeight(0.9);

		ioPanel.add(ioSplit, BorderLayout.CENTER);
		ioPanel.add(inputWrapper, BorderLayout.SOUTH);
		setVisible(true);
		rubricTabbedPane = new JTabbedPane();

		sourceTabbedPane = new JTabbedPane();
		sourceTabbedPane.setComponentPopupMenu(popupSource);

		overallTabbedPane = new JTabbedPane();
		overallTabbedPane.addTab("Source", sourceTabbedPane);
		overallTabbedPane.addTab("Console", ioPanel);
		overallTabbedPane.addTab("Rubric", rubricTabbedPane);
		add(overallTabbedPane, BorderLayout.CENTER);

		consoleInput.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String text = consoleInput.getText();
				if (currentInputHistory != null) {
					currentInputHistory.setText(text);
				}
				consoleInput.setText("");
				ListenerCoordinator.fire(SystemInListener.class, text);
			}
		});

	}

	private void registerListeners() {

		ListenerCoordinator.addListener(AddRubricTabsListener.class, new AddRubricTabsListener() {
			@Override
			public void fired(Rubric rubric) {
				rubricTabbedPane.removeAll();
				rubricPanels.clear();
				if (rubric != null) {
					List<String> tabNames = rubric.getRubricTabs();
					for (String rubricName : tabNames) {
						SplitOutErrPanel rubricPanel = new SplitOutErrPanel();
						rubricPanels.put(rubricName, rubricPanel);
						rubricTabbedPane.addTab(rubricName, rubricPanel.getSplitPane());
					}
				}
			}
		});

		ListenerCoordinator.addBlockingListener(PreRunBlockingListener.class, new PreRunBlockingListener() {
			public void fired(String studentID) {
				overallTabbedPane.setSelectedIndex(1);
				try {
					// Doing this prevents forward progress until the panes are ready
					pauseSemaphore.release();
					pauseSemaphore.acquire();
					setWindowData(studentID);
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
				setWindowData(idToDisplay);
			}
		});

	}
	
	private void clearPanel(JPanel panel) {
		while (panel.getComponentCount() != 0) {
			panel.remove(0);
		}
		
	}
	
	private void clearAndAddPanel(JPanel panel, Component componentToAdd) {
		clearPanel(panel);
		if (componentToAdd != null) {
			panel.add(new JScrollPane(componentToAdd));
		}
	}
	private void bindStudentAreas(String studentID) {
		if (studentID != null) {
			StudentConsoleAreas currentAreas = (StudentConsoleAreas)ListenerCoordinator.runQuery(GetStudentTextAreasQuery.class, studentID);
			outputWrapperPanel.clearAndAdd(currentAreas.getOutputAreas());
			currentInputHistory = currentAreas.getInputArea();
			clearAndAddPanel(inputHistorWrapperPanel, currentInputHistory);
			Set<String> rubricKeys = rubricPanels.keySet();
			for (String rubricName : rubricKeys) {
				rubricPanels.get(rubricName).clearAndAdd(currentAreas.getRubricArea(rubricName));
			}
		}
		else {
			outputWrapperPanel.clearPanels();
			currentInputHistory = null;
			clearPanel(inputHistorWrapperPanel);
			Set<String> rubricKeys = rubricPanels.keySet();
			for (String rubricName : rubricKeys) {
				rubricPanels.get(rubricName).clearPanels();
			}
		}
	}


}
