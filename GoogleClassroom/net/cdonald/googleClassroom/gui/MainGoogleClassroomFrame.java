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
import net.cdonald.googleClassroom.listenerCoordinator.LaunchNewRubricDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchRubricEditorDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchRubricFileDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RubricFileSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.RunRubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.RunSelected;
import net.cdonald.googleClassroom.listenerCoordinator.SaveGradesListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;

public class MainGoogleClassroomFrame extends JFrame implements CompileListener,
		DataStructureChangedListener,
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
	private NewRubricDialog newRubricDialog;
	private InfoPanel infoPanel;


	public MainGoogleClassroomFrame() throws InterruptedException {
		super(APP_NAME);		
		dataController = new DataController(this);		
		rubricElementDialog = new RubricElementDialog(this, this);
		newRubricDialog = new NewRubricDialog(this);

		setLayout();		

		
		importExportDialog = new GoogleSheetDialog(this);

		registerListeners();
		setVisible(true);
		dataController.performFirstInit();

		rubricElementDialog.modifyRubric(new Rubric(new GoogleSheetData("Test", "Test", "Test")));
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
		infoPanel = new InfoPanel();
		setJMenuBar(mainMenu);

		add(mainToolBar, BorderLayout.PAGE_START);
		add(studentPanel, BorderLayout.WEST);
		splitPanePrimary = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, studentPanel, consoleAndSourcePanel);
		add(splitPanePrimary, BorderLayout.CENTER);
		add(infoPanel, BorderLayout.SOUTH);


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
				//importExportDialog.setVisible(SaveGradesListener.class)
			}			
		});
		
		ListenerCoordinator.addListener(LaunchRubricFileDialogListener.class, new LaunchRubricFileDialogListener() {
			@Override
			public void fired() {
				importExportDialog.setVisible("Open Rubric File", RubricFileSelectedListener.class, dataController.getRubricURL());			}			
		});
		
		ListenerCoordinator.addListener(LaunchRubricEditorDialogListener.class, new LaunchRubricEditorDialogListener() {
			@Override
			public void fired() {
				editRubric(dataController.getRubric());
			}
		});
		
		ListenerCoordinator.addListener(LaunchNewRubricDialogListener.class, new LaunchNewRubricDialogListener() {
			@Override
			public void fired() {
				newRubricDialog.setVisible(true);
				String rubricName = newRubricDialog.getRubricName();
				if (rubricName != null) {
					Rubric temp = dataController.newRubric(rubricName);
					mainToolBar.addRubricInfo(temp.getSheetInfo(), true);
					editRubric(temp);
					
				}
			}
		});
	}
	
	private void editRubric(Rubric rubricToModify) {
		Rubric copy = new Rubric(rubricToModify);
		if (rubricElementDialog.modifyRubric(dataController.getRubric()) == true) {
			dataController.saveRubric();
		}
		else {
			dataController.setRubric(copy);
		}
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
					mainToolBar.setStopEnabled(true);
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
				}
				return null;
			}
			@Override
			protected void done() {
				enableRuns();
				mainToolBar.setStopEnabled(false);

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
	public void rubricModified(Rubric rubric) {
		dataController.setRubric(rubric);
		mainToolBar.enableRunRubricButton();
	}

	
	public void saveRubric() {
		
	}
}
