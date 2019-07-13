package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import net.cdonald.googleClassroom.control.DataController;
import net.cdonald.googleClassroom.googleClassroomInterface.CourseFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.DataFetchListener;
import net.cdonald.googleClassroom.googleClassroomInterface.GoogleClassroomCommunicator;
import net.cdonald.googleClassroom.googleClassroomInterface.SheetFetcher;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompileListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.MyPreferences;
import net.cdonald.googleClassroom.model.Rubric;

public class MainGoogleClassroomFrame extends JFrame implements MainToolBarListener, CompileListener,
		StudentPanelListener, MainMenuListener, FileResponseListener, GoogleSheetDialogListener, DataStructureChangedListener,
		RubricModifiedListener {
	private static final long serialVersionUID = 7452928818734325088L;
	public static final String APP_NAME = "Google Classroom Grader";
	private StudentPanel studentPanel;
	private MainToolBar mainToolBar;
	private JSplitPane splitPanePrimary;
	private ConsoleAndSourcePanel consoleAndSourcePanel;
	private SwingWorker<Void, Void> rubricWorker;
	private SwingWorker<Void, String> runWorker;
	private SwingWorker<Void, Void> loadSaveWorker;
	private MainMenu mainMenu;
	private GoogleSheetDialog importExportDialog;
	private final String TEMP_URL = "https://drive.google.com/open?id=1EYN9SBQWd9gqz5eK7UDy_qQY0B1529f6r-aOw8n2Oyk";
	private DataController dataController;
	private GoogleClassroomCommunicator googleClassroom;
	private MyPreferences prefs;
	private RubricElementDialog rubricElementDialog;


	public MainGoogleClassroomFrame() throws InterruptedException {
		super(APP_NAME);

		dataController = new DataController(this);
		prefs = new MyPreferences();
		rubricElementDialog = new RubricElementDialog(this, this);
		initGoogle();
		setLayout();
		initClassOptions();
		initRubrics();
		
		importExportDialog = new GoogleSheetDialog(this, this, "Google Sheet URL");
		if (prefs.getClassroom() != null) {
			classSelected(prefs.getClassroom());
		}
		setVisible(true);
	}

	private void initGoogle() {
		try {
			googleClassroom = new GoogleClassroomCommunicator(MainGoogleClassroomFrame.APP_NAME, prefs.getTokenDir(), prefs.getJsonPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void setLayout() {
		try {
			// UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				// System.out.println(info.getName());
				if ("Windows".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
				}
			}
			// UIManager.setLookAndFeel("javax.swing.plaf.motif.MotifLookAndFeel");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setMinimumSize(new Dimension(400, 400));
		setSize(800, 500);
		setLayout(new BorderLayout());
		consoleAndSourcePanel = new ConsoleAndSourcePanel();
		dataController.addConsoleListener(consoleAndSourcePanel);

		mainToolBar = new MainToolBar();
		studentPanel = new StudentPanel(dataController);
		mainMenu = new MainMenu(this);
		mainMenu.setListener(this);
		setJMenuBar(mainMenu);

		add(mainToolBar, BorderLayout.PAGE_START);
		add(studentPanel, BorderLayout.WEST);
		studentPanel.setStudentPanelListener(this);
		splitPanePrimary = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, studentPanel, consoleAndSourcePanel);
		add(splitPanePrimary, BorderLayout.CENTER);

		mainToolBar.addListener(this);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent ev) {
				dataController.closing();
				dispose();
				System.gc();
			}
		});
	}

	private void initClassOptions() {

		CourseFetcher fetcher = new CourseFetcher(googleClassroom, new DataFetchListener() {
			@Override
			public void retrievedInfo(ClassroomData data) {
				mainMenu.addClass(data);
			}

			@Override
			public void remove(Set<String> ids) {
				mainMenu.removeClasses(ids);				
			}
		});
		fetcher.execute();
	}
	
	@Override
	public void classSelected(ClassroomData data) {
		prefs.setClassroom(data);
		dataController.setCurrentCourse(prefs, data);		
		initAssignmentOptions(data);
		initStudentList(data);
		if (data != null) {
			setTitle(APP_NAME + " - " + data.getName());
		}
	}

	private void initAssignmentOptions(ClassroomData classSelected) {
		mainToolBar.clearAssignment();
		if (classSelected != null) {
			dataController.initAssignments(googleClassroom, new DataFetchListener() {
				@Override
				public void retrievedInfo(ClassroomData data) {
					mainToolBar.addAssignment(data);
				}
				@Override
				public void remove(Set<String> ids) {
					
				}
			});
		}

	}

	private void initStudentList(ClassroomData classSelected) {
		studentPanel.clearStudents();
		dataController.initStudents(prefs, googleClassroom);
	}

	private void initRubrics() {
		dataController.initRubrics(prefs, TEMP_URL, googleClassroom, new DataFetchListener() {
			@Override
			public void retrievedInfo(ClassroomData data) {
				mainToolBar.addRubric((GoogleSheetData)data);
			}
			@Override
			public void remove(Set<String> ids) {
				
			}
		});
	}

	@Override
	public void assignmentSelected(ClassroomData data) {
		if (data == null || data.isEmpty()) {
			return;
		}
		consoleAndSourcePanel.assignmentSelected();
		dataController.assignmentSelected(data, googleClassroom);

	}

	@Override
	public void rubricSelected(GoogleSheetData data) {
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			
			protected Void doInBackground() {
				Rubric rubric = new Rubric(data);
				try {
					googleClassroom.fillRubric(rubric);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(MainGoogleClassroomFrame.this, e.getMessage(), "Error accessing rubric db sheet",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
					rubric = null;
				}
				
				if (dataController.setRubric(rubric) == JOptionPane.YES_OPTION) {
					saveRubric();
				}
				
				
				if (rubric != null) {
					mainToolBar.enableRunRubricButton();
				}
				return null;
			}
		};
		worker.execute();
	}

	@Override
	public void runRubricSelected() {
		if (dataController.getRubric() == null) {
			return;
		}

			
		rubricWorker = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				disableRuns();
				List<String> ids = studentPanel.getSelectedIds();
				
				if (ids.size() == 0) {
					ids = dataController.getAllIDs();
				}
				try {
					for (String id : ids) {
						List<FileData> fileDataList = dataController.getSourceCode(id);
						consoleAndSourcePanel.setWindowData(fileDataList, dataController.getConsoleOutput(id),
								dataController.getConsoleInputHistory(id));
						dataController.runRubric(id);
					}
				} catch (Exception e) {
					JOptionPane.showMessageDialog(MainGoogleClassroomFrame.this, e.getMessage(), "Error running the auto-check rubrics",
							JOptionPane.ERROR_MESSAGE);
				}
				return null;
			}
			@Override
			protected void done() {
				enableRuns();

			}
		};
		rubricWorker.execute();
	}

	@Override
	public void runSelected() {
		runWorker = new SwingWorker<Void, String>() {

			@Override
			protected Void doInBackground() {
				disableRuns();

				List<String> ids = studentPanel.getSelectedIds();
				
				if (ids.size() == 0) {
					ids = dataController.getAllIDs();
				}
				for (String id : ids) {
					dataController.run(id);
					publish(id);
				}
				return null;
			}
			
			@Override
			protected void done() {
				enableRuns();
				dataController.debugPrint();
			}
		};
		runWorker.execute();
	}
	
	private void disableRuns() {
		mainToolBar.disableButtons();
	}
	
	private void enableRuns() {
		mainToolBar.enableRunButton();
		if (dataController.getRubric() != null) {
			mainToolBar.enableRunRubricButton();
		}		
	}

	@Override
	public void dataUpdated() {
		studentPanel.dataChanged();
	}
	
	@Override
	public void dataStructureChanged() {
		studentPanel.structureChanged();
	}

	@Override
	public void compileDone() {
		mainToolBar.enableRunButton();
		mainToolBar.enableRunRubricButton();
	}

	@Override
	public void studentSelected(String id) {
		List<FileData> fileDataList = dataController.getSourceCode(id);
		String outputText = dataController.getConsoleOutput(id);
		String inputHistoryText = dataController.getConsoleInputHistory(id); 
		consoleAndSourcePanel.setWindowData(fileDataList, outputText, inputHistoryText);
	}
	
	@Override
	public void openRubricEditorDialog() {
		rubricElementDialog.modifyRubric(dataController.getRubric());
	}
	
	@Override 
	public void rubricModified(Rubric rubric) {
		dataController.setRubric(rubric);
		mainToolBar.enableRunRubricButton();
	}


	@Override
	public void exitFired() {
		WindowListener[] listeners = getWindowListeners();
		for (WindowListener listener : listeners) {
			listener.windowClosing(new WindowEvent(this, 0));
		}
	}


	@Override
	public void exportFired() {
		importExportDialog.setVisible(true);

	}

	@Override
	public void importFired() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setWorkingDirFired() {

		JFileChooser workingDirChooser = null;
		if (prefs.getWorkingDir() != null) {
			workingDirChooser = new JFileChooser(prefs.getWorkingDir());
		} else {
			workingDirChooser = new JFileChooser();
		}
		workingDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (workingDirChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File directory = workingDirChooser.getSelectedFile();
			String directoryPath = directory.getAbsolutePath();
			prefs.setWorkingDir(directoryPath);
		}
	}
	
	@Override
	public void loadTestFile(String testFile) {
		classSelected(null);
		dataController.setCurrentCourse(prefs, null);
		dataController.loadTestFile(testFile);
	}

	@Override
	public boolean urlChanged(String url) {
		// Validate the url doesn't match the rubric db
		if (TEMP_URL.equals(url)) {
			JOptionPane.showMessageDialog(this, "Cannot use rubric db as grade sheet", "Don't do that!",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		//System.err.println("Fetching from url:" + url);		
		SheetFetcher fetcher = new SheetFetcher(url, googleClassroom, importExportDialog, importExportDialog);
		fetcher.execute();
		return true;
	}

	@Override
	public boolean okSelected(String url, String sheetName) {
		loadSaveWorker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				List<List<Object>> currentStatus = dataController.getColumnValuesForSheet();
				try {
					googleClassroom.writeSheet(url, sheetName, currentStatus);
				} catch (Exception e) {
					JOptionPane.showMessageDialog(MainGoogleClassroomFrame.this, e.getMessage(), e.getMessage(),
							JOptionPane.ERROR_MESSAGE);
				}
				return null;
			}

		};
		loadSaveWorker.execute();
		return true;
	}
	
	public void saveRubric() {
		
	}
}
