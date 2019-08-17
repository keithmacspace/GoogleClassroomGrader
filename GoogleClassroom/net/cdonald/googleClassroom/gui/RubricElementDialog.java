package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.DefaultCaret;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.GetFileDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RunRubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.SaveRubricListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetFileDirListener;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.MyPreferences;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntryRunCode;
import net.cdonald.googleClassroom.utils.HighlightText;

public class RubricElementDialog extends JDialog implements RubricElementListener {
	private static final long serialVersionUID = -5580080426150572162L;
	private JTable entriesTable;
	private RubricElementTableModel entriesModel;	
	private Rubric rubricToModify;	
	private RunCode runCode;
	private JPanel defaultPanel;
	private int priorSelectedIndex;
	private MyPreferences prefs;
	private List<JButton> buttons;
	private JButton goldenSourceButton;
	private JButton cancelButton;
	private StudentWorkCompiler compiler;
	
	

	public void modifyRubric(Rubric rubric) {
		this.rubricToModify = new Rubric(rubric);
		entriesModel.setRubricToModify(rubricToModify);
		rubricToModify.setInModifiedState(true);
		if (runCode.isActive()) {
			runCode.fillMethodCombo();
		}
		possiblyLoadGoldenSource();
		setVisible(true);
		
	}
		
	
	public RubricElementDialog(Frame parent, MyPreferences prefs, StudentWorkCompiler compiler) {
		super(parent, "Edit Rubric", false);
		this.prefs = prefs;
		this.compiler = compiler;
		buttons = new ArrayList<JButton>();		
		priorSelectedIndex = -1;		
		entriesModel = new RubricElementTableModel();
		entriesTable = new JTable(entriesModel);		
		entriesTable.setDefaultRenderer(RubricEntry.AutomationTypes.class, new RubricElementRenderer(this));
		entriesTable.setDefaultEditor(RubricEntry.AutomationTypes.class, new RubricElementEditor());
		entriesTable.setRowHeight(20);
		entriesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		buttons = new ArrayList<JButton>();
		goldenSourceButton = newButton("Load Gold Source");
		goldenSourceButton.setToolTipText("Load the source file(s) representing code that passes 100% of the rubrics");
		JButton okButton = newButton("OK");
		JButton saveButton = newButton("Save");
		JButton testButton = newButton("Test Run");
		JButton addRubricEntry = newButton("Add Row");
		JButton deleteButton = newButton("Delete Row");
		cancelButton = newButton("Cancel");
		
		cancelButton.setMnemonic(KeyEvent.VK_C);



		JPanel constantPanel = new JPanel();
		constantPanel.setLayout(new BorderLayout());
		
		JPanel buttonsPanel = createButtonPanel(8);
		buttonsPanel.add(okButton);
		buttonsPanel.add(goldenSourceButton);
		buttonsPanel.add(addRubricEntry);
		buttonsPanel.add(deleteButton);
		buttonsPanel.add(saveButton);
		buttonsPanel.add(testButton);
		buttonsPanel.add(cancelButton);




		constantPanel.add(buttonsPanel, BorderLayout.NORTH);

		defaultPanel = new JPanel();
		// cards = new JPanel(new CardLayout());
		defaultPanel.setLayout(new BorderLayout());
		defaultPanel.add(new JScrollPane(entriesTable), BorderLayout.CENTER);
		defaultPanel.add(constantPanel, BorderLayout.EAST);		
		if (prefs.dimensionExists(MyPreferences.Dimensions.RUBRIC_EDIT)) {
			defaultPanel.setPreferredSize(prefs.getDimension(MyPreferences.Dimensions.RUBRIC_EDIT, 0, 0));
		}

		add(defaultPanel, BorderLayout.CENTER);
		
		runCode = new RunCode();
		
		ListSelectionModel selectionModel = entriesTable.getSelectionModel();

		goldenSourceButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadGoldSource();
			}
		});
		
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(SetRubricListener.class, rubricToModify, SetRubricListener.RubricType.PRIMARY);
				setVisible(false);
			}
		});
		
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(SaveRubricListener.class);								
			}
		});

		testButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(RunRubricSelected.class, true);				
			}
		});

		
		
		addRubricEntry.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rubricToModify.addNewEntry();
				entriesModel.fireTableDataChanged();
				int selectionIndex = rubricToModify.getEntries().size() - 1;
				selectionModel.setSelectionInterval(selectionIndex, selectionIndex);
			}
		});
		
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedIndex = selectionModel.getMinSelectionIndex();
				if (selectedIndex != -1) {
					rubricToModify.removeEntry(selectedIndex);
					entriesModel.fireTableDataChanged();
				}
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(SetRubricListener.class, null, SetRubricListener.RubricType.RUBRIC_BEING_EDITED);
				setVisible(false);
			}
		});
		

		selectionModel.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int selectedIndex = selectionModel.getMinSelectionIndex();
				if (selectedIndex != -1 && priorSelectedIndex != selectedIndex) {
					for (int i = 0; i < entriesModel.getColumnCount(); i++) {
						if (entriesModel.getColumnClass(i) == RubricEntry.AutomationTypes.class) {
							typeSelected((RubricEntry.AutomationTypes)entriesModel.getValueAt(selectedIndex, i), true);
							break;
						}
					}
				}
			}
		});
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent ev) {				
				ListenerCoordinator.fire(SetRubricListener.class, null, SetRubricListener.RubricType.RUBRIC_BEING_EDITED);
			}
		});

		createPopupMenu(selectionModel);
		pack();
	}
	
	private JButton newButton(String name) {
		JButton button = new JButton(name);		
		Dimension former = null;
		if (buttons.size() > 0) {
			former = buttons.get(0).getPreferredSize();
		}		
		Dimension current = button.getPreferredSize();
		if (former != null && current.getWidth() > former.getWidth()) {
			for (JButton oldButton : buttons) {				
				oldButton.setPreferredSize(current);
			}
		}
		else if (former != null){			
			button.setPreferredSize(former);
		}
		buttons.add(button);
		return button;
	}
	
	
	private void createPopupMenu(ListSelectionModel selectionModel) {
		JPopupMenu rightClickPopup = new JPopupMenu();
		JMenuItem moveUpItem = new JMenuItem("Move Up");
		JMenuItem moveDownItem = new JMenuItem("Move Down");
		JMenuItem insertAbove = new JMenuItem("Add Entry Above");
		JMenuItem insertBelow = new JMenuItem("Add Entry Below");
		rightClickPopup.add(moveUpItem);		
		rightClickPopup.add(moveDownItem);
		rightClickPopup.add(insertAbove);
		rightClickPopup.add(insertBelow);
		
		moveUpItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedIndex = selectionModel.getMinSelectionIndex();
				rubricToModify.swapEntries(selectedIndex, selectedIndex -1);
				entriesModel.fireTableDataChanged();
			}			
		});
		moveDownItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedIndex = selectionModel.getMinSelectionIndex();
				rubricToModify.swapEntries(selectedIndex, selectedIndex + 1);
				entriesModel.fireTableDataChanged();
			}			
		});
		insertAbove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedIndex = selectionModel.getMinSelectionIndex();
				rubricToModify.addNewEntry(selectedIndex);
				entriesModel.fireTableDataChanged();
			}			
		});
		insertBelow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedIndex = selectionModel.getMinSelectionIndex();
				rubricToModify.addNewEntry(selectedIndex + 1);
				entriesModel.fireTableDataChanged();
			}			
		});

		entriesTable.setComponentPopupMenu(rightClickPopup);
		

	}
	
	private JPanel createButtonPanel(int numButtons) {
		final int SPACE = 6;
		final int BUTTON_TOP_SPACE = 5;
		JPanel buttonsPanel;
		GridLayout buttonLayout;
		buttonsPanel = new JPanel();
		// buttonsPanel.setLayout(new FlowLayout());
		buttonLayout = new GridLayout(numButtons, 0);
		final int GAP_SIZE = 6;
		buttonLayout.setVgap(GAP_SIZE);
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(BUTTON_TOP_SPACE, SPACE, SPACE, SPACE));
		buttonsPanel.setLayout(buttonLayout);
		return buttonsPanel;
	}

	@Override
	public void typeSelected(RubricEntry.AutomationTypes automationType, boolean isSelected) {		
		if (isSelected) {
			boolean stateChanged = false;
			RubricEntry entry = getCurrentEntry();
			if (entry.getAutomationType() != automationType) {
				entry.setAutomationType(automationType);
			}
			if (priorSelectedIndex != entriesTable.getSelectedRow()) {				
				if (runCode.isActive()) {
					runCode.removeRunCodeItems();
					stateChanged = true;
				}
				priorSelectedIndex = entriesTable.getSelectedRow();
			}
			if (entry.getAutomationType() == RubricEntry.AutomationTypes.RUN_CODE) {
				if (runCode.isActive() == false) {			
					runCode.addRunCodeItems();
					stateChanged = true;
				}
			}
			else {
				if (runCode.isActive()) {
					runCode.removeRunCodeItems();
					stateChanged = true;
				}
			}
			if (stateChanged) {
				revalidate();			
				repaint();
				pack();
			}			
		}
	}
	
	public RubricEntry getCurrentEntry() {
		int entryNum = entriesTable.getSelectedRow();
		return rubricToModify.getEntry(entryNum);
	}
	
	public boolean validateTextField(String name, JTextField field) {
		String text = field.getText();
		return validateText(name, text);
	}
	public boolean validateComboField(String name, JComboBox<String> combo) {
		String text = (String)combo.getSelectedItem();
		return validateText(name, text);
	}
	public boolean validateText(String name, String text) {		
		text = text.replaceAll("\\s+", "");
		if (text.length() == 0) {			
			JOptionPane.showMessageDialog(null,  name + " field must have a value", "Field Is Empty", JOptionPane.ERROR_MESSAGE);
			return false;
		}		
		return true;
	}
	

	private void addLabel(JPanel parent, JLabel label, int y) {
		GridBagConstraints l = new GridBagConstraints();
		l.weightx = 0;
		l.weighty = 0;
		l.gridx = 0;
		l.gridy = y;
		l.gridheight = 1;
		l.anchor = GridBagConstraints.LINE_END;
		parent.add(label, l);		
	}
	private void addLabelAndComponent(JPanel parent, JLabel label, JComponent component, int y) {
		addLabel(parent, label, y);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = y;
		parent.add(component, c);
	}
	
	private void loadGoldSource() {
		List<FileData> allFiles = loadSource();
		if (allFiles != null & allFiles.size() != 0) {
			rubricToModify.setGoldenSource(allFiles);
			possiblyLoadGoldenSource();
		}	
	}
	
	private List<FileData> loadSource() {
		JFileChooser fileChooser = null;
		String currentWorkingDir = (String)ListenerCoordinator.runQuery(GetFileDirQuery.class);
		if (currentWorkingDir != null) {
			fileChooser = new JFileChooser(currentWorkingDir);
		} else {
			fileChooser = new JFileChooser();
		}
		fileChooser.setMultiSelectionEnabled(true);
		List<FileData> allFiles = new ArrayList<FileData>();
		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			for (File file : fileChooser.getSelectedFiles()) { 
				Path path = Paths.get(file.getAbsolutePath());
				ListenerCoordinator.fire(SetFileDirListener.class, path.getParent().toString());
				String fileName = path.getFileName().toString();
				
				try {
					String text = new String(Files.readAllBytes(path));
					FileData fileData = new FileData(fileName, text, FileData.GOLDEN_SOURCE_ID, null);
					allFiles.add(fileData);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}						
			}
		}
		return allFiles;
	}
	
	private void possiblyLoadGoldenSource() {
		boolean enable = (rubricToModify != null && rubricToModify.getGoldenSource() != null && rubricToModify.getGoldenSource().size() != 0);
		for (JButton button : buttons) {
			if (button != cancelButton && button != goldenSourceButton) {
				button.setEnabled(enable);
			}
		}
		if (enable) {
			ListenerCoordinator.fire(SetRubricListener.class, rubricToModify, SetRubricListener.RubricType.RUBRIC_BEING_EDITED);
		}
	}
	
	private class RunCode implements RunCodeFileListTableModelListener {
		private Dimension runCodeDimension;
		private JButton addFilesButton;
		private JButton removeFilesButton;
		private JTable fileToUseList;
		private JScrollPane fileScroll;
		private RunCodeFileListTableModel fileToUseModel;
		private JComboBox<String> methodToCallCombo;
		private JLabel methodToCallLabel;
		private Map<String, JTextArea> sourceCodeArea;
		private Map<String, Method> methodMap;
		private JSplitPane runSplit;
		private JPanel runPanel;
		private JTabbedPane sourceTabs;
		private JPanel sourceCodePanel;
		private RubricEntryRunCode associatedAutomation;
		private boolean isActive;
		public RunCode() {
			sourceCodeArea = new HashMap<String, JTextArea>();
			methodMap = new HashMap<String, Method>();
			isActive = false;
			addFilesButton = newButton("Add Files");
			removeFilesButton = newButton("Remove Files");
			
			JPanel buttonsPanel = createButtonPanel(2);
			buttonsPanel.add(addFilesButton);
			buttonsPanel.add(removeFilesButton);
			JPanel buttonHolder = new JPanel();
			buttonHolder.setLayout(new BorderLayout());
			buttonHolder.add(buttonsPanel, BorderLayout.NORTH);
			JPanel namePanel = createNamePanel();
			JPanel nameAndButtons = new JPanel();
			nameAndButtons.setLayout(new BorderLayout());
			//nameAndButtons.add(fileScroll, BorderLayout.CENTER);
			nameAndButtons.add(namePanel, BorderLayout.CENTER);
			nameAndButtons.add(buttonHolder, BorderLayout.EAST);

			sourceCodePanel = new JPanel();
			sourceCodePanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			sourceCodePanel.setLayout(new BorderLayout());

			JPanel sourcePanel = new JPanel();
			sourcePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			sourcePanel.setLayout(new BorderLayout());
			JTextArea sourceArea = new JTextArea();
			for (int i = 0; i < 20; i++) {
				sourceArea.append("\n");
			}
			sourcePanel.add(new JScrollPane(sourceArea));
			
			
			sourceTabs = new JTabbedPane();
			sourceTabs.addTab("",  sourcePanel);
			JPanel sourcePanelX = new JPanel();
			sourcePanelX.setLayout(new BorderLayout());
			sourcePanelX.add(sourceTabs, BorderLayout.CENTER);
			sourcePanelX.setPreferredSize(new Dimension(0, 200));

			sourceCodePanel.add(sourcePanelX, BorderLayout.CENTER);

			
			
			
			runPanel = new JPanel();
			runPanel.setLayout(new BorderLayout());
			runPanel.add(nameAndButtons, BorderLayout.NORTH);
			runPanel.add(sourceCodePanel, BorderLayout.CENTER);
			
			if (prefs.dimensionExists(MyPreferences.Dimensions.RUN_CODE)) {
				runPanel.setPreferredSize(prefs.getDimension(MyPreferences.Dimensions.RUN_CODE, 0, 0));
			}
			
			addFilesButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					addFile();
				}
			});
			
			removeFilesButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					removeFile();
				}
			});
			
			methodToCallCombo.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					methodSelected();
				}
			});
		}
		
	
		private void addRunCodeItems() {
			isActive = true;
			RubricEntry associatedEntry = getCurrentEntry();
			if (associatedEntry.getAutomation() == null || !(associatedEntry.getAutomation() instanceof RubricEntryRunCode)) {
				associatedEntry.setAutomation(new RubricEntryRunCode());
			}
			associatedAutomation = (RubricEntryRunCode)associatedEntry.getAutomation();
			
			fillRunCode();
			fileToUseModel.init();
			remove(defaultPanel);
			if (runCodeDimension != null) {
				runPanel.setPreferredSize(runCodeDimension);
			}
			Map<String, FileData> allFiles = rubricToModify.getFileDataMap();
			for (String key : allFiles.keySet()) {
				fileToUseModel.addFile(allFiles.get(key));
			}
			runSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, defaultPanel, runPanel);
			add(runSplit, BorderLayout.CENTER);
			fillMethodCombo();
			pack();
		}
		
		private JPanel createNamePanel() {
			JPanel namePanel = new JPanel();
			namePanel.setLayout(new GridBagLayout());
			final int SPACE = 5;
			JLabel fileToUseLabel = new JLabel("File To Use: ");
			fileToUseList = new JTable();
			fileToUseModel = new RunCodeFileListTableModel(rubricToModify, this);
			fileToUseList.setModel(fileToUseModel);
			fileToUseList.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
			fileToUseList.getColumnModel().getColumn(0).setMaxWidth(10);
			fileToUseList.setTableHeader(null);
			namePanel.setBorder(BorderFactory.createEmptyBorder(SPACE, 0, SPACE, 0));
			methodToCallCombo = new JComboBox<String>();
			methodToCallCombo.setEditable(false);

		 

			methodToCallLabel = new JLabel("Method to call: ");
			methodToCallLabel.setToolTipText("Method's signature should be public static double methodName(). Enter only the name of the method, without parameters or return type.");
			fileScroll = new JScrollPane();
			fileScroll.setViewportView(fileToUseList);
			fileScroll.setPreferredSize(new Dimension(0, fileToUseList.getRowHeight() * 5 + 1));
			JPanel filePanel = new JPanel();
			filePanel.setLayout(new BorderLayout());
			filePanel.add(fileScroll, BorderLayout.CENTER);
			filePanel.setPreferredSize(new Dimension(0, fileToUseList.getRowHeight() * 5 + 1));
			addLabelAndComponent(namePanel, fileToUseLabel, filePanel, 0);
			addLabelAndComponent(namePanel, methodToCallLabel, methodToCallCombo, 1);
			return namePanel;

		}
		
		private void addFile() {
			List<FileData> allFiles = loadSource();
			if (allFiles != null) {
				for (FileData fileData : allFiles) {
					if (rubricToModify.getFileDataMap().containsKey(fileData.getName()) == false) {
						rubricToModify.addFileData(fileData);
						addRunCodeFile(fileData);
						fileToUseModel.addFile(fileData);
						fileToUseModel.fireTableDataChanged();
					}
				}
			}		
		}
		
		private void removeFile() {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					List<FileData> removeList = new ArrayList<FileData>();
					for (int row : fileToUseList.getSelectedRows()) {								
						FileData fileToRemove = (FileData)fileToUseModel.getValueAt(row, 1);
						removeList.add(fileToRemove);
						rubricToModify.removeFileData(fileToRemove);
						removeRunCodeFile(fileToRemove);
					}
					for (FileData fileToRemove : removeList) {								
						fileToUseModel.removeFile(fileToRemove.getName());
					}
					fileToUseModel.fireTableDataChanged();
					
				}
			});
		}
		
		private void methodSelected() {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					String methodName = (String)methodToCallCombo.getSelectedItem();
					if (methodName != null) {
						Method method = methodMap.get(methodName);
						associatedAutomation.setMethodToCall(methodName);						
						for (FileData fileData : associatedAutomation.getSourceFiles()) {
							int index = fileData.getFileContents().indexOf(methodName); 
							if (index != -1) {
								for (int i = 0; i < sourceTabs.getTabCount(); i++) {
									if (sourceTabs.getTitleAt(i).contentEquals(fileData.getName())) {
										sourceTabs.setSelectedIndex(i);
										JTextArea textArea = sourceCodeArea.get(fileData.getName());										
										HighlightText.highlightMethod(textArea, method);
										break;
									}
								}
							}
						}
					}
				}
			});
		}
		
		
		public boolean isActive() {
			return isActive;
		}
		

		private void removeRunCodeItems() {
			isActive = false;
			Dimension current = runSplit.getTopComponent().getSize();
			runCodeDimension = new Dimension(runPanel.getSize());			
			prefs.setDimension(MyPreferences.Dimensions.RUN_CODE, runCodeDimension);
			remove(runSplit);
			defaultPanel.setPreferredSize(current);
			prefs.setDimension(MyPreferences.Dimensions.RUBRIC_EDIT, current);
			add(defaultPanel);
		}
		private void fillRunCode() {				
			while(sourceTabs.getTabCount() != 0) {
				sourceTabs.removeTabAt(0);				
			}
			List<FileData> sourceFiles = associatedAutomation.getSourceFiles();
			for (FileData sourceFile : sourceFiles) {
				addCodeTabs(sourceFile);
			}
			methodToCallCombo.setSelectedItem(associatedAutomation.getMethodToCall());
		}

		
		private boolean firstTabIsEmpty() {
			return (sourceTabs.getTabCount() == 1 && sourceTabs.getTitleAt(0).length() == 0);
		}
		
		private void addCodeTabs(FileData fileData) {
			JPanel sourcePanel = new JPanel();
			sourcePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			sourcePanel.setLayout(new BorderLayout());
			JTextArea sourceArea = new JTextArea();
			DefaultCaret caret = (DefaultCaret) sourceArea.getCaret();
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
			JScrollPane scrollPane = new JScrollPane(sourceArea);
			sourcePanel.add(scrollPane);
			sourceArea.setText(fileData.getFileContents());
			sourceArea.getDocument().addDocumentListener(new SourceDocumentListener(fileData, sourceArea));
			sourceCodeArea.put(fileData.getName(), sourceArea);						
			if (firstTabIsEmpty()) {
				sourceTabs.setTitleAt(0, fileData.getName());
				sourceTabs.setComponentAt(0, sourcePanel);
			}
			else {
				sourceTabs.addTab(fileData.getName(), sourcePanel);
			}

		}
		

		@Override
		public void addRunCodeFile(FileData fileData) {
			addCodeTabs(fileData);
			associatedAutomation.addSourceContents(fileData);
			fillMethodCombo();
		}
		@Override
		public void removeRunCodeFile(FileData fileData) {
			for (int i = 0; i < sourceTabs.getTabCount(); i++) {
				String fileName = sourceTabs.getTitleAt(i);
				if (fileName.equals(fileData.getName())) {
					sourceTabs.removeTabAt(i);
					break;
				}
			}
			sourceCodeArea.remove(fileData.getName());			
			associatedAutomation.removeFileData(fileData);
			fillMethodCombo();
		}
		@Override
		public boolean containsSource(FileData fileData) {
			return associatedAutomation.containsSource(fileData);
		}
		
		private void fillMethodCombo() {
			List<Method> methods = associatedAutomation.getPossibleMethods(rubricToModify.getGoldenSource(), compiler);
			if (methods == null) {
				methodToCallCombo.removeAllItems();
				methodMap.clear();
			}
			else {				
				boolean changed = false;
				for (Method method : methods) {
					if (methodMap.containsKey(method.getName()) == false) {
						changed = true;
						break;
					}
				}
				if (changed) {
					methodToCallCombo.removeAllItems();
					methodMap.clear();				
					for (Method method : methods) {
						methodToCallCombo.addItem(method.getName());
						methodMap.put(method.getName(), method);
						
					}
				}				
			}			
		}
	}
}
