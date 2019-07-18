package net.cdonald.googleClassroom.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import net.cdonald.googleClassroom.googleClassroomInterface.AssignmentFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.SheetFetcher;
import net.cdonald.googleClassroom.listenerCoordinator.AssignmentSelected;
import net.cdonald.googleClassroom.listenerCoordinator.SetRunRubricEnableStateListener;
import net.cdonald.googleClassroom.listenerCoordinator.ClassSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.EnableRunRubricQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentAssignmentQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentRubricQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LongQueryListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricFileSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.RunRubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.RunSelected;
import net.cdonald.googleClassroom.listenerCoordinator.SetRunEnableStateListener;
import net.cdonald.googleClassroom.listenerCoordinator.SheetFetcherListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.GoogleSheetData;


public class MainToolBar extends JToolBar {
	private static final long serialVersionUID = 5112657453014257288L;

	private JComboBox<ClassroomData> assignmentCombo;
	private JComboBox<GoogleSheetData> rubricCombo; 
	private DefaultComboBoxModel<ClassroomData> assignmentModel;
	private DefaultComboBoxModel<GoogleSheetData> rubricModel;
	private GoogleSheetData emptySheet;
	private ClassroomData empty;
	private JButton runButton;
	private JButton runRubricButton;


	public MainToolBar() {
		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));



		assignmentCombo = new JComboBox<ClassroomData>();
		assignmentModel = new DefaultComboBoxModel<ClassroomData>();		
		assignmentCombo.setModel(assignmentModel);
		rubricCombo = new JComboBox<GoogleSheetData>();
		rubricModel = new DefaultComboBoxModel<GoogleSheetData>();
		rubricCombo.setModel(rubricModel);		
		runButton = new JButton("Run");
		runRubricButton = new JButton("Run Rubrics");

		disableButtons();

		setLayout(new FlowLayout(FlowLayout.LEFT));
		empty = new ClassroomData();
		assignmentModel.addElement(empty);
		emptySheet = new GoogleSheetData();
		rubricModel.addElement(emptySheet);
		add(assignmentCombo);
		add(rubricCombo);
		add(runButton);
		add(runRubricButton);
		registerListeners();
		addSelectionListeners();
	}
	

	
	public void enableRunButton() {
		runButton.setEnabled(true);
	}
	
	public void enableRunRubricButton() {
		if (runButton.isEnabled()) {
			runRubricButton.setEnabled(true);
		}
	}
	
	public void disableButtons() {
		runButton.setEnabled(false);
		runRubricButton.setEnabled(false);		
	}

	private void registerListeners() {
		
		ListenerCoordinator.addQueryResponder(GetCurrentAssignmentQuery.class, new GetCurrentAssignmentQuery() {
			@Override
			public ClassroomData fired() {
				ClassroomData data = (ClassroomData) assignmentCombo.getSelectedItem();
				if (data != null && data.isEmpty() == false) {
					return data;
				}
				return null;
			}			
		});
		ListenerCoordinator.addQueryResponder(GetCurrentRubricQuery.class, new GetCurrentRubricQuery() {
			@Override
			public GoogleSheetData fired() {
				Object item = rubricCombo.getSelectedItem();
				if (item != null) {
					GoogleSheetData rubric = (GoogleSheetData)item;
					if (rubric.isEmpty() == false) {
						return rubric;
					}
				}
				return null;
			}			
		}); 
		
		// When we change the overall class, we have to change the possible assignments
		ListenerCoordinator.addListener(ClassSelectedListener.class, new ClassSelectedListener() {

			@Override
			public void fired(ClassroomData course) {
				ListenerCoordinator.runLongQuery(AssignmentFetcher.class, new LongQueryListener<ClassroomData>() {
					@Override
					public void process(List<ClassroomData> list) {
						for (ClassroomData assignment : list) {
							addAssignment(assignment);
						}						
					}
				});
			}			
		});
		
		ListenerCoordinator.addListener(RubricFileSelectedListener.class, new RubricFileSelectedListener() {
			@Override
			public void fired(String url) {
				for (int i = 1; i < rubricCombo.getComponentCount(); i++) {
					rubricCombo.remove(i);
				}
				ListenerCoordinator.runLongQuery(SheetFetcher.class, new SheetFetcherListener(url) {
					@Override
					public void process(List<ClassroomData> list) {
						for (ClassroomData data : list) {
							rubricCombo.addItem((GoogleSheetData)data);
						}
					}					
				});
				
			}			
		});
		
		ListenerCoordinator.addListener(SetRunEnableStateListener.class, new SetRunEnableStateListener() {
			public void fired(Boolean setRunEnabled) {
				runButton.setEnabled(setRunEnabled);
				if ((Boolean)ListenerCoordinator.runQuery(EnableRunRubricQuery.class) == true) {
					runRubricButton.setEnabled(true);
				}
			}
		});
		
		ListenerCoordinator.addListener(SetRunRubricEnableStateListener.class, new SetRunRubricEnableStateListener() {
			public void fired(Boolean enable) {				 
				if (runButton.isEnabled() == false) {
					enable = false;
				}
				runRubricButton.setEnabled(enable);
				
			}
		});
	}

	private void addSelectionListeners() {

		assignmentCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ClassroomData data = (ClassroomData) assignmentCombo.getSelectedItem();
				if (data != null && data.isEmpty() == false) {
					ListenerCoordinator.fire(AssignmentSelected.class, data);
				}
			}
		});
		
		rubricCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Object item = rubricCombo.getSelectedItem();
				GoogleSheetData rubric = (GoogleSheetData)item;
				ListenerCoordinator.fire(RubricSelected.class, rubric);
			}
			
		});
		
		runButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(RunSelected.class);
			}			
		});	
		
		runRubricButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(RunRubricSelected.class);
			}			
		});	

	}


	public void clearAssignment() {
		assignmentModel.removeAllElements();
		assignmentModel.addElement(empty);
	}


	public ClassroomData getAssignmentSelected() {
		return assignmentModel.getElementAt(assignmentCombo.getSelectedIndex());
	}


	public void addAssignment(ClassroomData data) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				assignmentModel.insertElementAt(data, 1);
			}
		});
	}

	public void addRubric(GoogleSheetData rubric) {
		rubricModel.addElement(rubric);
	}

}
