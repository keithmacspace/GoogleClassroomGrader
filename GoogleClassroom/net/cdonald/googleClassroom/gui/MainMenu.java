package net.cdonald.googleClassroom.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.MyPreferences;

public class MainMenu extends JMenuBar {
	private JMenu file;
	private JMenu help;
	private JMenu edit;
	private JMenu run;
	private JMenu openClassroom;
	private MainMenuListener listener;






	
	public MainMenu() {
		file = new JMenu("File");
		edit = new JMenu("Edit");
		run = new JMenu("Run");
		help = new JMenu("Help");
		
		fillFileMenu();
		fillEditMenu();
		fillRunMenu();
		fillHelpMenu();		
	}
	
	private void fillFileMenu() {

		openClassroom = new JMenu("Open Classroom");
		JMenuItem exportToSheet = new JMenuItem("Export...");
		JMenuItem importFromSheet = new JMenuItem("Import...");
		JMenuItem setWorkingDirectory = new JMenuItem("Working Dir...");
		JMenuItem exit = new JMenuItem("Exit");
		
		file.add(openClassroom);
		file.addSeparator();
		file.add(exportToSheet);
		file.add(importFromSheet);
		file.addSeparator();
		file.add(setWorkingDirectory);
		file.addSeparator();
		file.add(exit);
		file.setMnemonic(KeyEvent.VK_F);
		exit.setMnemonic(KeyEvent.VK_X);
		importFromSheet.setMnemonic(KeyEvent.VK_I);
		exportToSheet.setMnemonic(KeyEvent.VK_E);
		exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
		exit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (listener != null) {
					listener.exitFired();
				}				
			}			
		});
		
		setWorkingDirectory .addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (listener != null) {
					listener.setWorkingDirFired();
				}

				
			}
			
		});
		exportToSheet.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (listener != null) {
					listener.exportFired();					
				}
				
			}
		});
		
		add(file);
	}
	
	public void addClass(ClassroomData classroom) {
			JMenuItem classroomOption = new JMenuItem(classroom.getName());
			classroomOption.addActionListener(new OpenClassroomListener(classroom));
			openClassroom.add(classroomOption);					
	}
	
	private class OpenClassroomListener implements ActionListener {
		ClassroomData classroom;
		public OpenClassroomListener(ClassroomData classroom) {
			super();
			this.classroom = classroom;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (listener != null) {
				listener.classSelected(classroom);
			}			
		}



		
	}
	
	private void fillEditMenu() {
		
	}
	
	private void fillRunMenu() {
		
	}
	
	private void fillHelpMenu() {
		
	}

	public void setListener(MainMenuListener listener) {
		this.listener = listener;
	}
}
