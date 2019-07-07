package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.swing.JFrame;
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
import inMemoryJavaCompiler.StudentWorkCompiler;
import model.ClassroomData;
import model.FileData;
import model.StudentData;

public class MainGoogleClassroomFrame extends JFrame implements MainToolBarListener {
	private static final long serialVersionUID = 7452928818734325088L;
	private static final String APP_NAME = "Google Classroom Grader";
	private MainToolBar mainToolBar;
	GoogleClassroomCommunicator googleClassroom;
	StudentPanel studentPanel;
	StudentWorkCompiler studentWorkCompiler;

	public MainGoogleClassroomFrame() {
		super(APP_NAME);
		initGoogle();
		setLayout();
		initClassOptions();
		studentWorkCompiler = new StudentWorkCompiler();
		setMinimumSize(new Dimension(400, 400));
		setSize(800, 500);
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
		setLayout(new BorderLayout());
		mainToolBar = new MainToolBar();
		studentPanel = new StudentPanel();
		add(mainToolBar, BorderLayout.PAGE_START);
		add(studentPanel);
		mainToolBar.addListener(this);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent ev) {
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

	@Override
	public void classSelected(ClassroomData data) {
		initAssignmentOptions(data);
		initStudentList(data);
	}

	@Override
	public void assignmentSelected(ClassroomData data) {
		if (studentWorkCompiler != null) {

			studentWorkCompiler.clearData();
			FileFetcher fetcher = new FileFetcher(mainToolBar.getCourse(), data, googleClassroom,
					new DataFetchListener() {
						@Override
						public void retrievedInfo(ClassroomData data) {
							studentWorkCompiler.addFile((FileData) data);
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
