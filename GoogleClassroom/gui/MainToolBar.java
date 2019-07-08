package gui;

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
import javax.swing.border.BevelBorder;

import model.ClassroomData;

public class MainToolBar extends JToolBar {
	private static final long serialVersionUID = 5112657453014257288L;
	private List<MainToolBarListener> listeners;
	private JComboBox<ClassroomData> classroomCombo;
	private JComboBox<ClassroomData> assignmentCombo;
	private DefaultComboBoxModel<ClassroomData> assignmentModel;
	private DefaultComboBoxModel<ClassroomData> classModel;
	private ClassroomData empty;
	private JButton run;
	private JButton runAll;


	public MainToolBar() {
		listeners = new ArrayList<MainToolBarListener>();
		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		classroomCombo = new JComboBox<ClassroomData>();
		classModel = new DefaultComboBoxModel<ClassroomData>();
		classroomCombo.setModel(classModel);
		assignmentCombo = new JComboBox<ClassroomData>();
		assignmentModel = new DefaultComboBoxModel<ClassroomData>();
		assignmentCombo.setModel(assignmentModel);
		run = new JButton("Run");
		runAll = new JButton("RunAll");

		setLayout(new FlowLayout(FlowLayout.LEFT));
		empty = new ClassroomData();
		add(classroomCombo);
		add(assignmentCombo);
		add(run);
		add(runAll);

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
		classroomCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ClassroomData data = (ClassroomData) classroomCombo.getSelectedItem();
				for (MainToolBarListener listener : listeners) {
					listener.classSelected(data);
				}
			}
		});

		assignmentCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ClassroomData data = (ClassroomData) assignmentCombo.getSelectedItem();
				for (MainToolBarListener listener : listeners) {
					listener.assignmentSelected(data);
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
		
		runAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (MainToolBarListener listener : listeners) {
					listener.runAllClicked();
				}
			}			
		});


	}

	public void clearClasses() {
		classModel.removeAllElements();
		classModel.addElement(empty);
		clearAssignment();
	}

	public void clearAssignment() {
		assignmentModel.removeAllElements();
		assignmentModel.addElement(empty);
	}

	public ClassroomData getCourse() {
		return classModel.getElementAt(classroomCombo.getSelectedIndex());
	}

	public ClassroomData getAssignmentSelected() {
		return assignmentModel.getElementAt(assignmentCombo.getSelectedIndex());
	}

	public void addClass(ClassroomData className) {
		classModel.addElement(className);
	}

	public void addAssignment(ClassroomData data) {
		assignmentModel.insertElementAt(data, 1);
	}

}
