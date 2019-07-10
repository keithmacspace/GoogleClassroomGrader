package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import net.cdonald.googleClassroom.googleClassroomInterface.AssignmentFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.CourseFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.DataFetchListener;
import net.cdonald.googleClassroom.googleClassroomInterface.FetchDoneListener;
import net.cdonald.googleClassroom.googleClassroomInterface.FileFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.GoogleClassroomCommunicator;
import net.cdonald.googleClassroom.googleClassroomInterface.SheetFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.StudentFetcher;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompileListener;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.MyPreferences;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.StudentData;

public class MainGoogleClassroomFrame extends JFrame implements MainToolBarListener, CompileListener,
		StudentPanelListener, MainMenuListener, SelectWorkingDirListener, GoogleSheetDialogListener {
	private static final long serialVersionUID = 7452928818734325088L;
	private static final String APP_NAME = "Google Classroom Grader";
	private GoogleClassroomCommunicator googleClassroom;
	private StudentPanel studentPanel;
	private StudentWorkCompiler studentWorkCompiler;
	private MainToolBar mainToolBar;
	private JSplitPane splitPanePrimary;
	private ConsoleAndSourcePanel consoleAndSourcePanel;
	private SwingWorker<Void, Void> getRubricWorker;
	private SwingWorker<Void, Void> runWorker;
	private SwingWorker<Void, Void> loadSaveWorker;
	private MainMenu mainMenu;
	private ClassroomData currentCourse;
	private MyPreferences prefs;
	private GoogleSheetDialog importExportDialog;
	static Semaphore pauseSemaphore = new Semaphore(1);
	private final String TEMP_URL = "https://drive.google.com/open?id=1EYN9SBQWd9gqz5eK7UDy_qQY0B1529f6r-aOw8n2Oyk";

	public MainGoogleClassroomFrame() {
		super(APP_NAME);
		prefs = new MyPreferences();
		initGoogle();
		setLayout();
		initClassOptions();
		initRubrics();
		importExportDialog = new GoogleSheetDialog(this, this, "Google Sheet URL");
		studentWorkCompiler = new StudentWorkCompiler(this);
		if (prefs.getClassroom() != null) {
			classSelected(prefs.getClassroom());
		}

		setVisible(true);
	}

	private void initGoogle() {
		try {
			googleClassroom = new GoogleClassroomCommunicator(APP_NAME, prefs.getTokenDir(), prefs.getJsonPath());
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

		mainToolBar = new MainToolBar();
		studentPanel = new StudentPanel();
		mainMenu = new MainMenu();
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
				consoleAndSourcePanel.setDone();
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
		});
		fetcher.execute();
	}

	private void initAssignmentOptions(ClassroomData classSelected) {
		mainToolBar.clearAssignment();
		AssignmentFetcher fetcher = new AssignmentFetcher(classSelected, googleClassroom, new DataFetchListener() {
			@Override
			public void retrievedInfo(ClassroomData data) {
				mainToolBar.addAssignment(data);
			}
		});
		fetcher.execute();
	}

	private void initStudentList(ClassroomData classSelected) {
		studentPanel.clearStudents();
		StudentFetcher fetcher = new StudentFetcher(classSelected, googleClassroom, new DataFetchListener() {
			@Override
			public void retrievedInfo(ClassroomData data) {
				studentPanel.addStudent((StudentData) data);
			}
		});
		fetcher.execute();
	}

	private void initRubrics() {
		getRubricWorker = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				// TODO Auto-generated method stub
				List<Rubric> rubrics = new ArrayList<Rubric>();
				googleClassroom.getRubrics(
						TEMP_URL, rubrics);
				mainToolBar.setRubrics(rubrics);
				return null;

			}

		};
		getRubricWorker.execute();

	}

	@Override
	public void assignmentSelected(ClassroomData data) {
		consoleAndSourcePanel.assignmentSelected();
		if (studentWorkCompiler != null) {

			studentWorkCompiler.clearData();
			if (currentCourse != null) {
				studentPanel.setAssignment(currentCourse);
				FileFetcher fetcher = new FileFetcher(currentCourse, data, googleClassroom, new DataFetchListener() {
					@Override
					public void retrievedInfo(ClassroomData data) {
						FileData fileData = (FileData) data;
						studentWorkCompiler.addFile(fileData);
						studentPanel.addFileData(fileData);
					}
				}, new FetchDoneListener() {

					@Override
					public void done() {
						studentWorkCompiler.compileAll();
					}

				});
				fetcher.execute();
			}

		}

	}

	@Override
	public void rubricSelected(Rubric rubric) {
		studentPanel.setRubric(rubric);
	}

	@Override
	public void runClicked() {
		String[] ids = studentPanel.getSelectedIds();

		runWorker = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() {
				try {
					pauseSemaphore.acquire();
					for (String id : ids) {
						if (studentWorkCompiler.isRunnable(id)) {
							consoleAndSourcePanel.runStarted(id);
							System.err.println("Running " + id);
							List<FileData> fileDataList = studentWorkCompiler.getSourceCode(id);
							// This will release the semaphore when we have completed redirecting the screen
							consoleAndSourcePanel.studentSelected(id, fileDataList, pauseSemaphore);
							pauseSemaphore.acquire();
							studentWorkCompiler.run(id);
						}
					}
				} catch (InterruptedException e) {

				}
				pauseSemaphore.release();
				return null;
			}

		};
		runWorker.execute();
	}

	@Override
	public void compileResults(List<CompilerMessage> result) {
		studentPanel.addCompilerMessages(result);
	}

	@Override
	public void compileDone() {

	}

	@Override
	public void studentSelected(String id) {
		List<FileData> fileDataList = studentWorkCompiler.getSourceCode(id);
		consoleAndSourcePanel.studentSelected(id, fileDataList, null);
	}

	@Override
	public void runFired() {
		runClicked();

	}

	@Override
	public void exitFired() {
		WindowListener[] listeners = getWindowListeners();
		for (WindowListener listener : listeners) {
			listener.windowClosing(new WindowEvent(this, 0));
		}
	}

	@Override
	public void classSelected(ClassroomData data) {
		currentCourse = data;
		prefs.setClassroom(data);
		initAssignmentOptions(data);
		initStudentList(data);
		setTitle(APP_NAME + " - " + data.getName());
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
	public boolean urlChanged(String url) {
		// Validate the url doesn't match the rubric db
		if (TEMP_URL.equals(url)) {
			JOptionPane.showMessageDialog(this, "Cannot use rubric db as grade sheet", "Bad URL Name", JOptionPane.ERROR_MESSAGE);
			return false;
		}		
		SheetFetcher fetcher = new SheetFetcher(url, googleClassroom, importExportDialog, importExportDialog);
		fetcher.execute();
		return true;		
	}

	@Override
	public boolean okSelected(String url, String sheetName) {
		loadSaveWorker = new SwingWorker<Void, Void>() {
			

			@Override
			protected Void doInBackground() throws Exception {
				List<List<Object> >currentStatus = studentPanel.getColumnValuesForSheet();
				try {
					googleClassroom.writeSheet(url, sheetName, currentStatus);
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(MainGoogleClassroomFrame.this, e.getMessage(), e.getMessage(), JOptionPane.ERROR_MESSAGE);
				}
				return null;
			}

		};
		loadSaveWorker.execute();
		return true;
	}
}
