package gui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import inMemoryJavaCompiler.CompilerMessage;
import model.ClassroomData;
import model.FileData;
import model.StudentData;

public class StudentPanel extends JPanel {
	private static final long serialVersionUID = 3480731067309159048L;
	private StudentListModel studentModel;
	private JTable studentPanel;
	private StudentListRenderer submissionRenderer;
	private StudentPanelListener studentPanelListener;

	public StudentPanel() {
		setLayout(new BorderLayout());
		studentModel = new StudentListModel();
		studentPanel = new JTable(studentModel);
		submissionRenderer = new StudentListRenderer();
		studentPanel.setDefaultRenderer(Date.class, submissionRenderer);
		studentPanel.setDefaultRenderer(CompilerMessage.class, submissionRenderer);
		studentPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {				
				super.mouseClicked(e);
				if (studentPanelListener != null) {
					if (e.getButton() == MouseEvent.BUTTON1) {
						int row = studentPanel.rowAtPoint(e.getPoint());
						String id = studentModel.getStudentId(row);
						if (id != null) {
							studentPanelListener.studentSelected(id);
						}
					}
					
					
				}
			}
			
		});
		// studentList.setRowHeight(25);
		add(new JScrollPane(studentPanel), BorderLayout.CENTER);
	}

	public void clearStudents() {
		studentModel.clearAll();

	}

	public void clearRubric() {
		studentModel.clearRubric();
	}

	public void addStudent(StudentData student) {
		studentModel.addStudent(student);
	}

	public void addColumn(String name) {
		studentModel.addColumn(name);
	}

	public void setAssignment(ClassroomData assignment) {
		submissionRenderer.setAssignment(assignment);
		studentModel.clearRubric();
	}

	public void addCompilerMessages(List<CompilerMessage> messages) {
		studentModel.addCompilerMessages(messages);
	}

	public void addFileData(FileData fileDataInfo) {
		studentModel.addFileData(fileDataInfo);
	}

	public void setStudentPanelListener(StudentPanelListener studentPanelListener) {
		this.studentPanelListener = studentPanelListener;
	}
	
	public String getSelectedId() {
		int selectedRow = studentPanel.getSelectedRow();
		return studentModel.getStudentId(selectedRow);		
	}

}
