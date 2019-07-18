package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;


import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import net.cdonald.googleClassroom.control.DataController;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompileListener;
import net.cdonald.googleClassroom.listenerCoordinator.ClassSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.ExitFiredListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RunRubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.RunSelected;
import net.cdonald.googleClassroom.listenerCoordinator.SaveGradesListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;

import net.cdonald.googleClassroom.model.Rubric;

public class MainGoogleClassroomFrame extends JFrame implements CompileListener,
		StudentPanelListener, GoogleSheetDialogListener, DataStructureChangedListener,
		RubricModifiedListener {
	private static final long serialVersionUID = 7452928818734325088L;
	public static final String APP_NAME = "Google Classroom Grader";
	private StudentPanel studentPanel;
	private MainToolBar mainToolBar;
	private JSplitPane splitPanePrimary;
	private ConsoleAndSourcePanel consoleAndSourcePanel;
	private SwingWorker<Void, String> runWorker;
	private MainMenu mainMenu;
	private GoogleSheetDialog importExportDialog;
	private DataController dataController;		
	private RubricElementDialog rubricElementDialog;


	public MainGoogleClassroomFrame() throws InterruptedException {
		super(APP_NAME);

		dataController = new DataController(this);		
		rubricElementDialog = new RubricElementDialog(this, this);

		setLayout();		

		
		importExportDialog = new GoogleSheetDialog(this, this, "Google Sheet URL");

		registerListeners();
		setVisible(true);
		dataController.performFirstInit();
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
		studentPanel = new StudentPanel(dataController);
		mainMenu = new MainMenu(this);
		setJMenuBar(mainMenu);

		add(mainToolBar, BorderLayout.PAGE_START);
		add(studentPanel, BorderLayout.WEST);
		studentPanel.setStudentPanelListener(this);
		splitPanePrimary = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, studentPanel, consoleAndSourcePanel);
		add(splitPanePrimary, BorderLayout.CENTER);


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


	private void registerListeners() {

		
		ListenerCoordinator.addListener(ClassSelectedListener.class, new ClassSelectedListener() {
			@Override
			public void fired(ClassroomData course) {
				if (course != null) {
					setTitle(APP_NAME + " - " + course.getName());
				}
				else {
					setTitle(APP_NAME);
				}
			}
		}		);

		
		ListenerCoordinator.addListener(ExitFiredListener.class, new ExitFiredListener() {
			public void fired() {
				WindowListener[] listeners = getWindowListeners();
				for (WindowListener listener : listeners) {
					listener.windowClosing(new WindowEvent(MainGoogleClassroomFrame.this, 0));
				}				
			}
		});
		
		ListenerCoordinator.addListener(RunRubricSelected.class, new RunRubricSelected() {

			@Override
			public void fired() {
				if (dataController.getRubric() == null) {
					return;
				}
				runRubricOrCode(false);
			}
		});

		ListenerCoordinator.addListener(RunSelected.class, new RunSelected() {
			public void fired() {
				runRubricOrCode(true);
			}
		});
		
		ListenerCoordinator.addListener(SaveGradesListener.class, new SaveGradesListener() {
			@Override
			public void fired() {
				importExportDialog.setVisible(true);
			}			
		});

	}
	
	private void runRubricOrCode(boolean runSource) {
		runWorker = new SwingWorker<Void, String>() {

			@Override
			protected Void doInBackground() throws Exception {
				disableRuns();
				List<String> ids = studentPanel.getSelectedIds();
				
				if (ids.size() == 0) {
					ids = dataController.getAllIDs();
				}
				try {
					for (String id : ids) {
						if (runSource == true) {
							dataController.run(id);												
						}
						else {
							List<FileData> fileDataList = dataController.getSourceCode(id);
							consoleAndSourcePanel.setWindowData(fileDataList, dataController.getConsoleOutput(id),
									dataController.getConsoleInputHistory(id));
							dataController.runRubric(id);
						}
						publish(id);
					}
				} catch (Exception e) {
					JOptionPane.showMessageDialog(MainGoogleClassroomFrame.this, e.getMessage(), "Error while running",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace(System.err);
				}
				return null;
			}
			@Override
			protected void done() {
				enableRuns();

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
	public boolean urlChanged(String url) {
//		// Validate the url doesn't match the rubric db
//		if (TEMP_URL.equals(url)) {
//			JOptionPane.showMessageDialog(this, "Cannot use rubric db as grade sheet", "Don't do that!",
//					JOptionPane.ERROR_MESSAGE);
//			return false;
//		}
//		//System.err.println("Fetching from url:" + url);		
//		SheetFetcher fetcher = new SheetFetcher(url, googleClassroom, importExportDialog, importExportDialog);
//		fetcher.execute();
		return true;
	}

	@Override
	public boolean okSelected(String url, String sheetName) {
//		loadSaveWorker = new SwingWorker<Void, Void>() {
//			@Override
//			protected Void doInBackground() throws Exception {
//				List<List<Object>> currentStatus = dataController.getColumnValuesForSheet();
//				try {
//					googleClassroom.writeSheet(url, sheetName, currentStatus);
//				} catch (Exception e) {
//					JOptionPane.showMessageDialog(MainGoogleClassroomFrame.this, e.getMessage(), e.getMessage(),
//							JOptionPane.ERROR_MESSAGE);
//				}
//				return null;
//			}
//
//		};
//		loadSaveWorker.execute();
		return true;
	}
	
	public void saveRubric() {
		
	}
}