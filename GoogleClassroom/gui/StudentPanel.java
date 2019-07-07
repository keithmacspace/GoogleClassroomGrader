package gui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import model.FileData;
import model.StudentData;

public class StudentPanel extends JPanel {
	private static final long serialVersionUID = 3480731067309159048L;
	private StudentListModel studentModel;
	private JTable studentList;
	
	public StudentPanel() {
		setLayout(new BorderLayout());
		studentModel = new StudentListModel();
		studentList = new JTable(studentModel);
		//studentList.setRowHeight(25);
		add(new JScrollPane(studentList), BorderLayout.CENTER);				
	}
	
	public void clearStudents() {
		
	}
	
	public void clearRubric() {
		
	}
	
	public void addStudent(StudentData student) {
		studentModel.addStudent(student);
	}	

}
