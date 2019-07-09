package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import googleClassroomInterface.AssignmentFetcher;
import googleClassroomInterface.CourseFetcher;
import googleClassroomInterface.DataFetchListener;
import googleClassroomInterface.FetchDoneListener;
import googleClassroomInterface.FileFetcher;
import googleClassroomInterface.GoogleClassroomCommunicator;
import googleClassroomInterface.StudentFetcher;
import inMemoryJavaCompiler.CompileListener;
import inMemoryJavaCompiler.CompilerMessage;
import inMemoryJavaCompiler.StudentWorkCompiler;
import model.ClassroomData;
import model.FileData;
import model.Rubric;
import model.StudentData;

public class MainGoogleClassroomFrame extends JFrame
		implements MainToolBarListener, CompileListener, StudentPanelListener {
	private static final long serialVersionUID = 7452928818734325088L;
	private static final String APP_NAME = "Google Classroom Grader";
	private GoogleClassroomCommunicator googleClassroom;
	private StudentPanel studentPanel;
	private StudentWorkCompiler studentWorkCompiler;
	private MainToolBar mainToolBar;
	private JSplitPane splitPanePrimary;
	private ConsoleAndSourcePanel consoleAndSourcePanel;
	private SwingWorker<Void, Void> getRubricWorker;
	private SwingWorker<Void, Void> runAllWorker;

	public MainGoogleClassroomFrame() {
		super(APP_NAME);
		initGoogle();
		setLayout();
		initClassOptions();
		initRubrics();
		studentWorkCompiler = new StudentWorkCompiler(this);
		setVisible(true);
	}

	private void initGoogle() {
		try {
			googleClassroom = new GoogleClassroomCommunicator(APP_NAME, "tokens", "/credentials.json");
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
		mainToolBar.clearClasses();
		CourseFetcher fetcher = new CourseFetcher(googleClassroom, new DataFetchListener() {
			@Override
			public void retrievedInfo(ClassroomData data) {
				mainToolBar.addClass(data);
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
						"https://drive.google.com/open?id=1EYN9SBQWd9gqz5eK7UDy_qQY0B1529f6r-aOw8n2Oyk", rubrics);
				mainToolBar.setRubrics(rubrics);
				return null;

			}

		};
		getRubricWorker.execute();

	}

	@Override
	public void classSelected(ClassroomData data) {
		initAssignmentOptions(data);
		initStudentList(data);
	}

	@Override
	public void assignmentSelected(ClassroomData data) {
		if (studentWorkCompiler != null) {

			studentWorkCompiler.clearData();
			studentPanel.setAssignment(mainToolBar.getCourse());
			FileFetcher fetcher = new FileFetcher(mainToolBar.getCourse(), data, googleClassroom,
					new DataFetchListener() {
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

	@Override
	public void rubricSelected(Rubric rubric) {
		studentPanel.setRubric(rubric);
	}

	@Override
	public void runClicked() {
		String id = studentPanel.getSelectedId();
		if (id != null) {
			consoleAndSourcePanel.runStarted(id);
			studentWorkCompiler.run(id);
		}
	}

	@Override
	public void runAllClicked() {
		runAllWorker = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {

				int student = 0;
				String id = studentPanel.getStudentId(student);
				while (id != null) {
					studentPanel.selectStudent(student);
					runClicked();
					student++;
					id = studentPanel.getStudentId(student);
					wait(500);
					System.out.println("moving on");
				}
				return null;
			}

		};
		runAllWorker.execute();
	}

	@Override
	public void compileResults(List<CompilerMessage> result) {
		studentPanel.addCompilerMessages(result);
	}

	@Override
	public void compileDone() {

	}

	@Override
	public void runDone() {
		consoleAndSourcePanel.runStopped();
	}

	@Override
	public void studentSelected(String id) {
		List<FileData> fileDataList = studentWorkCompiler.getSourceCode(id);
		consoleAndSourcePanel.studentSelected(id, fileDataList);
	}

}
