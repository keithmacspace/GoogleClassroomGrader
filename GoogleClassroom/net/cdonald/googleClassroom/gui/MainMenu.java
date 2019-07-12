package net.cdonald.googleClassroom.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import net.cdonald.googleClassroom.model.ClassroomData;

public class MainMenu extends JMenuBar {
	private JMenu file;
	private JMenu help;
	private JMenu edit;
	private JMenu run;
	private JMenu openClassroom;
	private MainMenuListener listener;
	private JFrame owner;





	
	public MainMenu(JFrame owner) {
		this.owner = owner;
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
		JMenuItem loadTemporaryFile = new JMenuItem("Load Test File...");
		JMenuItem exit = new JMenuItem("Exit");
		
		file.add(openClassroom);
		file.addSeparator();
		file.add(exportToSheet);
		file.add(importFromSheet);
		file.addSeparator();
		file.add(setWorkingDirectory);
		file.addSeparator();
		file.add(loadTemporaryFile);
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
		
		loadTemporaryFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser tempFileChooser = new JFileChooser();

				if (tempFileChooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) {
					File file = tempFileChooser.getSelectedFile();
					String directoryPath = file.getAbsolutePath();
					listener.loadTestFile(directoryPath);
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
