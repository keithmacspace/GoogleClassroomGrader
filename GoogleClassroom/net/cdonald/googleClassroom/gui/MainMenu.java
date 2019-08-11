package net.cdonald.googleClassroom.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import net.cdonald.googleClassroom.googleClassroomInterface.CourseFetcher;
import net.cdonald.googleClassroom.listenerCoordinator.ChooseGradeFileListener;
import net.cdonald.googleClassroom.listenerCoordinator.ClassSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.ExitFiredListener;
import net.cdonald.googleClassroom.listenerCoordinator.GetFileDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetWorkingDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchNewRubricDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchRubricEditorDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchRubricFileDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LoadGradesListener;
import net.cdonald.googleClassroom.listenerCoordinator.LoadTestFileListener;
import net.cdonald.googleClassroom.listenerCoordinator.LongQueryListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricFileValidListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.RunJPLAGListener;
import net.cdonald.googleClassroom.listenerCoordinator.SaveGradesListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetWorkingDirListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.GoogleSheetData;

public class MainMenu extends JMenuBar {
	private static final long serialVersionUID = 5459790818047149118L;
	private JMenu file;
	private JMenu rubric;
	private JMenu help;
	private JMenu edit;
	private JMenu run;
	private JMenu jplag;
	private JMenu openClassroom;
	private JMenuItem newRubric;
	private JMenuItem editRubric;
	private JFrame owner;





	
	public MainMenu(JFrame owner) {
		this.owner = owner;
		file = new JMenu("File");
		rubric = new JMenu("Rubrics");
		jplag = new JMenu("JPLAG");
		edit = new JMenu("Edit");
		run = new JMenu("Run");
		help = new JMenu("Help");
		
		fillFileMenu();
		fillRubricMenu();
		fillJPLAGMenu();
		fillEditMenu();
		fillRunMenu();
		fillHelpMenu();		
	}
	
	private void fillFileMenu() {

		openClassroom = new JMenu("Open Classroom");
		JMenuItem chooseGradeFile = new JMenuItem("Choose Grade File...");
		JMenuItem syncGrades = new JMenuItem("Sync Grades");
		JMenuItem setWorkingDirectory = new JMenuItem("Working Dir...");
		
		syncGrades.setEnabled(false);
		
		JMenuItem exit = new JMenuItem("Exit");
		
		file.add(openClassroom);
		file.addSeparator();
		file.add(chooseGradeFile);
		file.add(syncGrades);
		file.addSeparator();
		file.add(setWorkingDirectory);
		file.addSeparator();
		file.add(exit);
		file.setMnemonic(KeyEvent.VK_F);
		exit.setMnemonic(KeyEvent.VK_X);
		syncGrades.setMnemonic(KeyEvent.VK_S);
		syncGrades.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
		add(file);
		
		exit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(ExitFiredListener.class);
			}			
		});
		
		setWorkingDirectory.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser workingDirChooser = null;
				String currentWorkingDir = (String)ListenerCoordinator.runQuery(GetWorkingDirQuery.class);
				if (currentWorkingDir != null) {
					workingDirChooser = new JFileChooser(currentWorkingDir);
				} else {
					workingDirChooser = new JFileChooser();
				}
				workingDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				if (workingDirChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					File directory = workingDirChooser.getSelectedFile();
					String directoryPath = directory.getAbsolutePath();
					ListenerCoordinator.fire(SetWorkingDirListener.class, directoryPath);
				}
			}			
		});
		
		chooseGradeFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(ChooseGradeFileListener.class);
				
			}
			
		});
		
		syncGrades.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(SaveGradesListener.class);
				
			}
		});	
		


		ListenerCoordinator.runLongQuery(CourseFetcher.class, new LongQueryListener<ClassroomData>() {
			@Override
			public void process(List<ClassroomData> list) {
				for (ClassroomData classroom : list) {
					addClass(classroom);
				}
			}			
		});
		
		ListenerCoordinator.addListener(RubricSelected.class, new RubricSelected() {
			@Override
			public void fired(GoogleSheetData googleSheet) {
				syncGrades.setEnabled(!googleSheet.isEmpty());
				editRubric.setEnabled(!googleSheet.isEmpty());
			}
		});

	}
	
	private void fillRubricMenu() {
		JMenuItem loadTemporaryFile = new JMenuItem("Load File To Test Rubric...");
		newRubric = new JMenuItem("New Rubric...");
		editRubric = new JMenuItem("Edit Rubric...");
		newRubric.setEnabled(false);
		editRubric.setEnabled(false);
		JMenuItem setRubricFile = new JMenuItem("Rubric File...");
		JMenuItem saveRubric = new JMenuItem("Save Rubric");
		
		rubric.add(setRubricFile);
		rubric.addSeparator();
		rubric.add(newRubric);
		rubric.add(editRubric);
		rubric.add(saveRubric);
		rubric.addSeparator();
		rubric.add(loadTemporaryFile);
		add(rubric);
		setRubricFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(LaunchRubricFileDialogListener.class);
			}
		});
		
		newRubric.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(LaunchNewRubricDialogListener.class);
			}
		});
		
		editRubric.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(LaunchRubricEditorDialogListener.class);
			}
		});

		loadTemporaryFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser tempFileChooser = null;
				String currentWorkingDir = (String)ListenerCoordinator.runQuery(GetFileDirQuery.class);
				if (currentWorkingDir != null) {
					tempFileChooser = new JFileChooser(currentWorkingDir);
				} else {
					tempFileChooser = new JFileChooser();
				}


				if (tempFileChooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) {
					File file = tempFileChooser.getSelectedFile();
					String directoryPath = file.getAbsolutePath();
					ListenerCoordinator.fire(LoadTestFileListener.class, directoryPath);
				}	
			}
		});			
		ListenerCoordinator.addListener(RubricFileValidListener.class, new RubricFileValidListener() {
			@Override
			public void fired() {
				newRubric.setEnabled(true);				
			}
		});
		
		ListenerCoordinator.addListener(RubricSelected.class, new RubricSelected() {
			@Override
			public void fired(GoogleSheetData googleSheet) {
				if (googleSheet.isEmpty() == false) {
					editRubric.setEnabled(true);
				}
			}
			
		});	
	}
	
	public void fillJPLAGMenu() {
		JMenuItem runJPLAG = new JMenuItem("Run JPLAG...");
		jplag.add(runJPLAG);
		add(jplag);
		runJPLAG.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(RunJPLAGListener.class);
			}
		});
	}
	
	public void addClass(ClassroomData classroom) {
			JMenuItem classroomOption = new JMenuItem(classroom.getName());
			classroomOption.addActionListener(new OpenClassroomListener(classroom));
			openClassroom.add(classroomOption);					
	}
	public void removeClasses(Set<String> ids) {
		for (String id : ids) {
			boolean removed = false;
			for (int i = 0; removed == false && i < openClassroom.getItemCount(); i++) {
				ActionListener[] listeners = openClassroom.getActionListeners();
				for (ActionListener listener : listeners) {
					if (listener instanceof OpenClassroomListener) {
						if (((OpenClassroomListener)listener).getClassroom().getId().equalsIgnoreCase(id)) {
							openClassroom.remove(i);
							removed = true;
							break;							
						}
					}
				}
			}
		}
	}
	
	private class OpenClassroomListener implements ActionListener {
		ClassroomData classroom;
		public ClassroomData getClassroom() {
			return classroom;
		}

		public OpenClassroomListener(ClassroomData classroom) {
			super();
			this.classroom = classroom;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			ListenerCoordinator.fire(ClassSelectedListener.class, classroom);			
		}
		
	}
	
	private void fillEditMenu() {
		
	}
	
	private void fillRunMenu() {
		
	}
	
	private void fillHelpMenu() {
		
	}
}
