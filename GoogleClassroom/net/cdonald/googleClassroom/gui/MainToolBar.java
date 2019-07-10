package net.cdonald.googleClassroom.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.Rubric;

public class MainToolBar extends JToolBar {
	private static final long serialVersionUID = 5112657453014257288L;
	private List<MainToolBarListener> listeners;
	private JComboBox<ClassroomData> assignmentCombo;
	private JComboBox<Rubric> rubricCombo; 
	private DefaultComboBoxModel<ClassroomData> assignmentModel;
	private DefaultComboBoxModel<Rubric> rubricModel;
	private ClassroomData empty;
	private JButton run;


	public MainToolBar() {
		listeners = new ArrayList<MainToolBarListener>();
		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));



		assignmentCombo = new JComboBox<ClassroomData>();
		assignmentModel = new DefaultComboBoxModel<ClassroomData>();		
		assignmentCombo.setModel(assignmentModel);
		rubricCombo = new JComboBox<Rubric>();
		rubricModel = new DefaultComboBoxModel<Rubric>();
		rubricCombo.setModel(rubricModel);
		rubricModel.addElement(new Rubric("", "None", 0));
		run = new JButton("Run");


		setLayout(new FlowLayout(FlowLayout.LEFT));
		empty = new ClassroomData();
		assignmentModel.addElement(empty);
		add(assignmentCombo);
		add(rubricCombo);
		add(run);


		addSelectionListeners();
	}

	public void addListener(MainToolBarListener listener) {
		listeners.add(listener);
	}

	public void removeListener(MainToolBarListener listener) {
		for (int i = 0; i < listeners.size(); i++) {
			if (listeners.get(i) == listener) {
				listeners.remove(i);
				break;
			}
		}
	}

	private void addSelectionListeners() {

		assignmentCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ClassroomData data = (ClassroomData) assignmentCombo.getSelectedItem();
				for (MainToolBarListener listener : listeners) {
					listener.assignmentSelected(data);
				}
				rubricCombo.setSelectedIndex(0);
			}
		});
		
		rubricCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Object item = rubricCombo.getSelectedItem();
				if (item != null) {
					Rubric rubric = (Rubric)item;
					for (MainToolBarListener listener : listeners) {
						listener.rubricSelected(rubric);
					}
				}
				
			}
			
		});
		
		run.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (MainToolBarListener listener : listeners) {
					listener.runClicked();
				}
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

	public void setRubrics(List<Rubric> rubrics) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				for (Rubric rubric : rubrics)  {
					rubricModel.addElement(rubric);
				}
				
			}
			
		});
	}

}
