package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dialog;
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
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultCaret;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.AddRubricTabsListener;
import net.cdonald.googleClassroom.listenerCoordinator.GetFileDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RunRubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.SaveRubricListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetFileDirListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentInfoChangedListener;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.MyPreferences;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntryMethodContains;
import net.cdonald.googleClassroom.model.RubricEntryRunCode;
import net.cdonald.googleClassroom.utils.HighlightText;

public class RubricElementDialog extends JDialog implements RubricElementListener {
	private static final long serialVersionUID = -5580080426150572162L;
	private JTable entriesTable;
	private RubricElementTableModel entriesModel;	
	private Rubric rubricToModify;	
	
	private JPanel defaultPanel;
	private int priorSelectedIndex;
	private List<JButton> buttons;
	private List<JButton> goldSourceEnabledButtons;
	private JButton goldenSourceButton;
	private JButton cancelButton;
	private StudentWorkCompiler compiler;
	private JPanel automationPanel;
	private JSplitPane mainSplit;
	private RunCode runCode;
	private CodeContainsString codeContainsString;
	

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
		super(parent, "Edit Rubric", Dialog.ModalityType.MODELESS);
		this.compiler = compiler;
		this.setUndecorated(false);
		buttons = new ArrayList<JButton>();		
		priorSelectedIndex = -1;		
		entriesModel = new RubricElementTableModel();
		entriesTable = new JTable(entriesModel);		
		entriesTable.setDefaultRenderer(RubricEntry.AutomationTypes.class, new RubricElementRenderer(this));
		entriesTable.setDefaultEditor(RubricEntry.AutomationTypes.class, new RubricElementEditor());
		entriesTable.setRowHeight(20);
		entriesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		buttons = new ArrayList<JButton>();
		goldSourceEnabledButtons = new ArrayList<JButton>();
		JButton okButton = newButton("OK", false);
		JButton saveButton = newButton("Save", false);
		JButton deleteButton = newButton("Delete Row", false);
		goldenSourceButton = newButton("Load Gold Source", false);
		goldenSourceButton.setToolTipText("Load the source file(s) representing code that passes 100% of the rubrics");

		JButton testButton = newButton("Test Run", true);
		
		cancelButton = newButton("Cancel", false);
		
		cancelButton.setMnemonic(KeyEvent.VK_C);



		JPanel constantPanel = new JPanel();
		constantPanel.setLayout(new BorderLayout());
		
		JPanel buttonsPanel = createButtonPanel(6);
		buttonsPanel.add(okButton);		
		buttonsPanel.add(saveButton);
		buttonsPanel.add(deleteButton);
		buttonsPanel.add(goldenSourceButton);
		buttonsPanel.add(testButton);
		buttonsPanel.add(cancelButton);




		constantPanel.add(buttonsPanel, BorderLayout.NORTH);

		defaultPanel = new JPanel();
		// cards = new JPanel(new CardLayout());
		defaultPanel.setLayout(new BorderLayout());
		defaultPanel.add(new JScrollPane(entriesTable), BorderLayout.CENTER);
		defaultPanel.add(constantPanel, BorderLayout.EAST);		
		if (prefs.dimensionExists(MyPreferences.Dimensions.RUBRIC_EDIT)) {
			setPreferredSize(prefs.getDimension(MyPreferences.Dimensions.RUBRIC_EDIT, 0, 0));
		}
		
		automationPanel = new JPanel();
		automationPanel.setLayout(new BorderLayout());
		
		mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, defaultPanel, automationPanel);
		if (prefs.getSplitLocation(MyPreferences.Dividers.RUBRIC_SPLIT) != 0) {
			mainSplit.setDividerLocation(prefs.getSplitLocation(MyPreferences.Dividers.RUBRIC_SPLIT));
			
		}
			
		add(mainSplit, BorderLayout.CENTER);
		
		runCode = new RunCode();
		codeContainsString = new CodeContainsString();
		
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
				preOKSaveTest();
				ListenerCoordinator.fire(SetRubricListener.class, rubricToModify, SetRubricListener.RubricType.PRIMARY);
				prefs.setDimension(MyPreferences.Dimensions.RUBRIC_EDIT, getSize());
				prefs.setSplitLocation(MyPreferences.Dividers.RUBRIC_SPLIT, mainSplit.getDividerLocation());
				rubricToModify.setInModifiedState(false);
				setVisible(false);
			}
		});
		
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				preOKSaveTest();
				rubricToModify.setInModifiedState(false);
				ListenerCoordinator.fire(SaveRubricListener.class);								
			}
		});

		testButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				preOKSaveTest();
				ListenerCoordinator.fire(StudentInfoChangedListener.class);
				ListenerCoordinator.fire(AddRubricTabsListener.class, rubricToModify);
				ListenerCoordinator.fire(RunRubricSelected.class, true);				
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
				prefs.setDimension(MyPreferences.Dimensions.RUBRIC_EDIT, getSize());
				prefs.setSplitLocation(MyPreferences.Dividers.RUBRIC_SPLIT, mainSplit.getDividerLocation());
				rubricToModify.setInModifiedState(false);
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

	public void preOKSaveTest() {
		if (entriesTable.getCellEditor() != null) {
			entriesTable.getCellEditor().stopCellEditing();
		}
		rubricToModify.cleanup();
	}

	
	private JButton newButton(String name, boolean requiresGoldSource) {
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
		if (requiresGoldSource) {
			goldSourceEnabledButtons.add(button);
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
	public boolean typeSelected(RubricEntry.AutomationTypes automationType, boolean isSelected) {
		boolean validSelection = true;
		if (isSelected) {
			boolean stateChanged = false;
			RubricEntry entry = getCurrentEntry();

			if (entry.getAutomationType() != automationType) {
				entry.setAutomationType(automationType);
			}
			if ((entry.getAutomationType().ordinal() > RubricEntry.AutomationTypes.COMPILES.ordinal()) &&
					(rubricToModify.getGoldenSource() == null || rubricToModify.getGoldenSource().size() == 0)) {
				entry.setAutomationType(RubricEntry.AutomationTypes.NONE);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {					
						JOptionPane.showMessageDialog(null, "Before this automation type can be selected, golden source must be loaded.", "Golden Source Missing",
								JOptionPane.ERROR_MESSAGE);
					}
				});
				stateChanged = true;
				validSelection = false;
			}
			else {
				if (priorSelectedIndex != entriesTable.getSelectedRow()) {				
					if (runCode.isActive()) {
						runCode.removeRunCodeItems();
						stateChanged = true;
					}
					if (codeContainsString.isActive()) {
						codeContainsString.removeCodeContainsStringItems();
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
				if (entry.getAutomationType() == RubricEntry.AutomationTypes.CODE_CONTAINS_METHOD) {
					if (codeContainsString.isActive() == false) {
						codeContainsString.addCodeContainsStringItems();
						stateChanged = true;
					}
				}
				else {
					if (codeContainsString.isActive()) {
						codeContainsString.removeCodeContainsStringItems();
						stateChanged = true;
						
					}
				}
			}
			if (stateChanged) {
				revalidate();			
				repaint();
			}			
		}
		return validSelection;
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
		for (JButton button : goldSourceEnabledButtons) {			
				button.setEnabled(enable);			
		}
		if (enable) {
			ListenerCoordinator.fire(SetRubricListener.class, rubricToModify, SetRubricListener.RubricType.RUBRIC_BEING_EDITED);
			runCode.goldenSourceEnabled(enable);
			codeContainsString.goldenSourceEnabled(enable);
		}
	}
	private class RunCode implements RunCodeFileListTableModelListener {
		
		private JButton addFilesButton;
		private JButton removeFilesButton;
		private JTable fileToUseList;
		private JScrollPane fileScroll;
		private RunCodeFileListTableModel fileToUseModel;
		private JComboBox<String> methodToCallCombo;
		private JLabel methodToCallLabel;
		private Map<String, JTextArea> sourceCodeArea;
		private Map<String, Method> methodMap;
		private JPanel runPanel;
		private JTabbedPane sourceTabs;
		private JPanel sourceCodePanel;
		private RubricEntryRunCode associatedAutomation;
		private JLabel explanation;
		private boolean isActive;
		public RunCode() {
			sourceCodeArea = new HashMap<String, JTextArea>();
			methodMap = new HashMap<String, Method>();
			isActive = false;
			addFilesButton = newButton("Add Files", true);
			removeFilesButton = newButton("Remove Files", true);
			explanation = new JLabel("<html>Load new or select an already loaded test file that contains the code to run."
					+ " Then select the method that will be run. "
					+ "The method should have the signature: <br/> <i>public static double methodName()</i> <br/>"
					+ "The return value should be between 0 and 1 inclusive (calculated by dividing numTestsPassing/numTestRun)."
					+  "<br/>Test by selecting the \"Test Run\" button."
					+ "<br/>View results on the main screen (including rubric run output).</html>");


			
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
			//nameAndButtons.add(explanation, BorderLayout.NORTH);
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
		
	
		public void goldenSourceEnabled(boolean enable) {

			
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

			Map<String, FileData> allFiles = rubricToModify.getFileDataMap();
			for (String key : allFiles.keySet()) {
				fileToUseModel.addFile(allFiles.get(key));
			}			
			fillMethodCombo();

			automationPanel.add(runPanel, BorderLayout.CENTER);
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
			methodToCallLabel.setToolTipText("Method's signature should be public static double methodName()."
					+ " Enter only the name of the method, without parameters or return type.");
			fileScroll = new JScrollPane();
			fileScroll.setViewportView(fileToUseList);
			fileScroll.setPreferredSize(new Dimension(0, fileToUseList.getRowHeight() * 5 + 1));
			JPanel filePanel = new JPanel();
			filePanel.setLayout(new BorderLayout());
			filePanel.add(fileScroll, BorderLayout.CENTER);
			filePanel.setPreferredSize(new Dimension(0, fileToUseList.getRowHeight() * 5 + 1));
			addLabelAndComponent(namePanel, new JLabel(), explanation, 0);
			addLabelAndComponent(namePanel, new JLabel(), new JLabel("                 "), 1);
			addLabelAndComponent(namePanel, fileToUseLabel, filePanel, 2);
			addLabelAndComponent(namePanel, methodToCallLabel, methodToCallCombo, 3);
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
			automationPanel.remove(runPanel);
			isActive = false;		
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
	
	private class CodeContainsString {
		private JPanel codeContainsPanel;
		private JComboBox<Method> methodToSearchCombo;
		private JTable valuesToSearchForTable;
		private DefaultTableModel valuesToSearchForModel;
		private JLabel explanation;
		private boolean isActive;		
		private boolean enableTableListener;
		private RubricEntryMethodContains associatedAutomation;
		public CodeContainsString() {
			isActive = false;
			explanation = new JLabel("<html>Method to search is the one that should contain the string(s) of interest.<br/>"
					+ "  The method must be part of your golden source. "
					+ "By default, all of the method names in the golden source are possible strings to find.<br/>"
					+ "This automation isn't terribly clever, it just searches the student's source for the same the method, "
					+ "and then searches that method for all strings checked (though it will skip comments)."
					+ "<br/>Test by selecting the \"Test Run\" button."
					+ "<br/>View results on the main screen (including rubric run output).<br/></html>");
		
			JPanel namePanel = createNamePanel();
			codeContainsPanel = new JPanel();
			codeContainsPanel.setLayout(new BorderLayout());
			
			
			//codeContainsPanel.add(explanation, BorderLayout.NORTH);
			codeContainsPanel.add(namePanel, BorderLayout.CENTER);
		
			methodToSearchCombo.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					//methodSelected();
				}
			});
		}
		
		private JPanel createNamePanel() {
			JPanel namePanel = new JPanel();
			namePanel.setLayout(new GridBagLayout());
			final int SPACE = 5;
			JLabel methodToSearchLabel = new JLabel("Method To Search: ");
			
			JLabel stringToSearchFor = new JLabel("String(s) to search for: ");
			


			
			valuesToSearchForModel = new DefaultTableModel(null, new String[] {"", "String(s) To Search For"}) {
				private static final long serialVersionUID = 1L;

				@Override
				public Class<?> getColumnClass(int columnIndex) {
					if (columnIndex == 0) {
						return Boolean.class;
					}
					else {
						return String.class;
					}
				}
				@Override
				public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
					super.setValueAt(aValue, rowIndex, columnIndex);
					
					if (associatedAutomation != null) {
						
						if (columnIndex == 0) {
							List<String> valuesToFind = new ArrayList<String>();
							for (int row = 0; row < valuesToSearchForModel.getRowCount(); row++) {
								Object valueAtCol = valuesToSearchForModel.getValueAt(row, 0);
								if (valueAtCol instanceof Boolean) {
									Boolean booleanVal = (Boolean)valueAtCol;

									if (booleanVal != null && booleanVal) {
										String name = (String)valuesToSearchForModel.getValueAt(row,  1);
										if (name != null && name.length() > 1) {
											valuesToFind.add(name);
										}
									}
								}
							}
							
							associatedAutomation.setStringsToFind(valuesToFind);
						}
						else if (columnIndex == 1) {
							setValueAt(Boolean.TRUE, rowIndex, 0);
						}
					}
					
				}
				@Override
				public boolean isCellEditable(int row, int column) {
					if (column == 0) {
						String col1Value = (String)getValueAt(row, column + 1);
						if (col1Value == null || col1Value.length() == 0) {
							return false;
						}
					}
					return true;
				}

			};
			valuesToSearchForTable = new JTable(valuesToSearchForModel);


			valuesToSearchForModel.addTableModelListener(new TableModelListener() {
				@Override
				public void tableChanged(TableModelEvent e) {
					if (enableTableListener == true && e.getType() == TableModelEvent.UPDATE) {
						if ((e.getLastRow() + 1) == valuesToSearchForModel.getRowCount()) {
							valuesToSearchForModel.addRow(new Object[] {Boolean.FALSE, ""});
						}
					}
				}
			});
			valuesToSearchForTable.getColumnModel().getColumn(0).setMaxWidth(10);
			valuesToSearchForTable.setModel(valuesToSearchForModel);
			valuesToSearchForTable.setEnabled(false);
			namePanel.setBorder(BorderFactory.createEmptyBorder(SPACE, 0, SPACE, 0));
			methodToSearchCombo = new JComboBox<Method>();
			methodToSearchCombo.setEditable(false);	
			methodToSearchCombo.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Method methodToSearch = (Method)methodToSearchCombo.getSelectedItem();
					valuesToSearchForTable.setEnabled(methodToSearch != null);
					if (associatedAutomation != null) {
						associatedAutomation.setMethodToSearch(methodToSearch);
					}
				}
			});
			
			addLabelAndComponent(namePanel, new JLabel(), explanation, 0);
			addLabelAndComponent(namePanel, new JLabel(), new JLabel("                 "), 1);
			addLabelAndComponent(namePanel, methodToSearchLabel, methodToSearchCombo, 2);
			addLabelAndComponent(namePanel, stringToSearchFor, valuesToSearchForTable, 3);
			return namePanel;

		}
		
	
		public void goldenSourceEnabled(boolean enable) {
			fillMethodCombo();
			
		}


		private void addCodeContainsStringItems() {
			isActive = true;
			enableTableListener = false;
			// Do it it this way to prevent infinite recursion in when we call set value at
			associatedAutomation = null;
			for (int row = 0; row < valuesToSearchForModel.getRowCount(); row++) {
				valuesToSearchForModel.setValueAt(Boolean.FALSE, row, 0);
			}
			methodToSearchCombo.setSelectedIndex(0);
			RubricEntryMethodContains automation = (RubricEntryMethodContains)getCurrentEntry().getAutomation();
			if (automation != null) {				
				for (String strToFind : automation.getStringsToFind()) {
					boolean found = false;
					for (int row = 0; row < valuesToSearchForModel.getRowCount(); row++) {
						String name = (String)valuesToSearchForModel.getValueAt(row,  1);
						if (name != null && name.equals(strToFind)) {
							valuesToSearchForModel.setValueAt(Boolean.TRUE, row, 0);
							found = true;
							break;
						}
					}
					if (found == false) {
						valuesToSearchForModel.addRow(new Object[]{Boolean.TRUE, strToFind});
					}
				}
				valuesToSearchForTable.setEnabled(false);
				for (int i = 1; i < methodToSearchCombo.getItemCount(); i++) {
					Method method = methodToSearchCombo.getItemAt(i);
					if (method.toString().equals(automation.getFullMethodSignature())) {
						methodToSearchCombo.setSelectedIndex(i);
						valuesToSearchForTable.setEnabled(true);
						break;
					}
				}
			}
			associatedAutomation = automation;
			enableTableListener = true;
			automationPanel.add(codeContainsPanel, BorderLayout.CENTER);
		}
		

	
		public boolean isActive() {
			return isActive;
		}
		

		private void removeCodeContainsStringItems() {
			automationPanel.remove(codeContainsPanel);
			isActive = false;		
		}


		
		
		
		private void fillMethodCombo() {
			enableTableListener = false;
			Map<String, Class<?>> classMap = null;
			List<FileData> goldenSource = rubricToModify.getGoldenSource();
			try {
				classMap = compiler.compile(goldenSource);
			} catch (Exception e) {

			}
			methodToSearchCombo.removeAllItems();
			methodToSearchCombo.addItem(null);
			while (valuesToSearchForModel.getRowCount() > 0) {
				valuesToSearchForModel.removeRow(0);
			}
			if (classMap != null) {
				for (String className : classMap.keySet()) {
					Class<?> classContainer = classMap.get(className);
					for (Method method : classContainer.getMethods()) {
						String methodName = method.getName();
					
						for (FileData file : goldenSource) {
							if (file.getFileContents().indexOf(methodName) != -1) {						
								methodToSearchCombo.addItem(method);
								if (methodName != "main") {
									valuesToSearchForModel.addRow(new Object[] {Boolean.FALSE, methodName});
								}
							}
						}
					}
				}
				valuesToSearchForModel.addRow(new Object[] {null, ""});
			}
			else {
				JOptionPane.showMessageDialog(null, "Golden source does not compile.  You can view the compiler message in the source window of the main screen.", "Golden Source Does Not Compile",
						JOptionPane.ERROR_MESSAGE);
			}
			enableTableListener = true;
		}
	}
}
