package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
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

import be.pcl.swing.ImprovedFormattedTextField;
import net.cdonald.googleClassroom.listenerCoordinator.GetFileDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SaveRubricListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetFileDirListener;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntryRunCode;

public class RubricElementDialog extends JDialog implements ItemListener {
	private static final long serialVersionUID = -5580080426150572162L;
	private JButton saveButton;
	private JButton modifyButton;
	private JButton cancelButton;
	private JComboBox<RubricEntry.AutomationTypes> automationTypeCombo;	
	private JComboBox<String> nameCombo;
	private JTextField descriptionField;
	private JFormattedTextField valueField;
	private JTable entriesTable;
	
	private RubricEntry.AutomationTypes currentAutomation;
	private Map<String, String> sourceMap;
	private JPanel namePanel;
	private JPanel buttonsPanel;
	private GridLayout buttonLayout;
	private Rubric rubricToModify;
	private RubricModifiedListener listener;
	
	
	private JButton addFilesButton;
	private JButton removeFilesButton;
	private JButton deleteButton;
	private JTextField methodNameField;
	private JLabel methodToCallLabel;
	
	private JLabel methodBeingCalledLabel;
	private JTextField methodBeingCalledField;
	
	private JTextArea sourceCodeArea;
	private JTable fileNamesTable;

	private DefaultTableModel fileNamesModel;
	private JTable mainRunnerTable;
	private JPanel sourceCodePanel;
	private JSplitPane sourceSplit;
	private String DEFAULT_SOURCE_STRING = "";
	private boolean saveRubric;
		
	
	public RubricElementDialog(Frame parent, RubricModifiedListener listener) {
		super(parent, "Rubric Element", true);
		currentAutomation = RubricEntry.AutomationTypes.NONE;
		sourceMap = new HashMap<String, String>();
		this.listener = listener;


		saveButton = new JButton("Save");
		modifyButton = new JButton("Modify");
		deleteButton = new JButton("Delete");
		cancelButton = new JButton("Cancel");
		cancelButton.setMnemonic(KeyEvent.VK_C);

		JPanel comboPanel = new JPanel();
		final int SPACE = 6;
		final int COMBO_TOP_SPACE = 15;
		final int NAME_TOP_SPACE = 5;
		final int BUTTON_TOP_SPACE = 5;
		comboPanel.setBorder(BorderFactory.createEmptyBorder(COMBO_TOP_SPACE, 0, 0, SPACE));
		comboPanel.setLayout(new FlowLayout());
		comboPanel.add(new JLabel("automation type: "));
		automationTypeCombo = new JComboBox<RubricEntry.AutomationTypes>();
		automationTypeCombo.addItemListener(this);
		for (RubricEntry.AutomationTypes automationType : RubricEntry.AutomationTypes.values()) {
			automationTypeCombo.addItem(automationType);
		}
		comboPanel.add(automationTypeCombo);
		
		automationTypeCombo.setSelectedItem(RubricEntry.AutomationTypes.NONE);

		namePanel = new JPanel();

		namePanel.setBorder(BorderFactory.createEmptyBorder(NAME_TOP_SPACE, 0, SPACE, SPACE));
		namePanel.setLayout(new GridBagLayout());
		nameCombo = new JComboBox<String>();
		nameCombo.setEditable(true);
		descriptionField = new JTextField("", 20);

		
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		intFormat.setGroupingUsed(false);
		intFormat.setMaximumFractionDigits(0);
		valueField = new ImprovedFormattedTextField(intFormat);
		namePanel.setMinimumSize(new Dimension(0, 80));

		addLabelAndComponent(namePanel, "name: ", nameCombo, 0);
		addLabelAndComponent(namePanel, "value: ", valueField, 1);
		addLabelAndComponent(namePanel, "description: ", descriptionField, 2);

		buttonsPanel = new JPanel();
		// buttonsPanel.setLayout(new FlowLayout());
		buttonLayout = new GridLayout(4, 0);
		final int GAP_SIZE = 6;
		buttonLayout.setVgap(GAP_SIZE);
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(BUTTON_TOP_SPACE, SPACE, SPACE, SPACE));
		buttonsPanel.setLayout(buttonLayout);
		buttonsPanel.add(saveButton);
		buttonsPanel.add(modifyButton);
		buttonsPanel.add(deleteButton);
		buttonsPanel.add(cancelButton);
		deleteButton.setEnabled(false);

		JPanel constantPanel = new JPanel();
		constantPanel.setLayout(new BorderLayout());
		constantPanel.add(comboPanel, BorderLayout.LINE_START);
		constantPanel.add(namePanel, BorderLayout.CENTER);
		constantPanel.add(buttonsPanel, BorderLayout.LINE_END);

		// cards = new JPanel(new CardLayout());
		setLayout(new BorderLayout());
		add(constantPanel, BorderLayout.PAGE_START);

		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveRubric = true;				
				setVisible(false);				
			}
		});
		
		modifyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				modifyRubric();
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveRubric = false;
				setVisible(false);
			}
		});
		
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String name = (String)nameCombo.getSelectedItem();
				if (rubricToModify.isInRubric(name)) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							rubricToModify.deleteEntry(name);
							nameCombo.remove(nameCombo.getSelectedIndex());							
							nameCombo.setSelectedIndex(0);
							nameCombo.revalidate();
							nameCombo.repaint();
							listener.rubricModified(rubricToModify);							
						}
					});
				}
			
			}
		});
		
		nameCombo.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {					
					String text = (String)nameCombo.getSelectedItem();
					fillSelectedState(text);
					deleteButton.setEnabled(rubricToModify.isInRubric(text));
				}				
			}
		});

		
		instantiateDynamics();
		pack();
	}
	
	private void fillSelectedState(String text) {
		for (RubricEntry entry : rubricToModify.getEntries()) {
			if (entry.getName().equals(text)) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						valueField.setText("" + entry.getValue());
						descriptionField.setText(entry.getDescription());
						RubricEntry.AutomationTypes automationType = entry.getAutomationType();
						int selectionIndex = automationType.ordinal();
						automationTypeCombo.setSelectedIndex(selectionIndex);
						switch(automationType) {
						case RUN_CODE:
							fillRunCode(entry);
						}
					}
				});
			}
		}
	}
	
	
	private void instantiateDynamics() {
		addFilesButton = new JButton("Add Files");
		removeFilesButton = new JButton("Remove Files");
		methodNameField = new JTextField("", 20);
		sourceCodeArea = new JTextArea(DEFAULT_SOURCE_STRING);
		sourceCodeArea.setEditable(false);
		fileNamesModel = new DefaultTableModel(new String[] {"FileNames"}, 0){
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		fileNamesTable = new JTable(fileNamesModel);
		methodToCallLabel = new JLabel("Method to call: ");
		sourceCodePanel = new JPanel();
		sourceCodePanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		sourceCodePanel.setLayout(new BorderLayout());
		sourceSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,  new JScrollPane(fileNamesTable), new JScrollPane(sourceCodeArea));
		sourceCodePanel.add(sourceSplit, BorderLayout.CENTER);
		sourceSplit.setDividerLocation(0.1);
		sourceSplit.setResizeWeight(0.1);
		
		methodBeingCalledLabel = new JLabel("Testing Student Method: ");
		methodBeingCalledField = new JTextField("", 20);
		
		addFilesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = null;
				String currentWorkingDir = (String)ListenerCoordinator.runQuery(GetFileDirQuery.class);
				if (currentWorkingDir != null) {
					fileChooser = new JFileChooser(currentWorkingDir);
				} else {
					fileChooser = new JFileChooser();
				}
				fileChooser.setMultiSelectionEnabled(true);

				if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					for (File file : fileChooser.getSelectedFiles()) { 
						Path path = Paths.get(file.getAbsolutePath());
						ListenerCoordinator.fire(SetFileDirListener.class, path.getParent().toString());
						String fileName = path.getFileName().toString();
						
						try {
							String text = new String(Files.readAllBytes(path));
							addRunCodeFile(fileName, text);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}						
					}
					fileNamesModel.fireTableStructureChanged();
				}
			}
		});
		
		removeFilesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						for (int i = fileNamesTable.getSelectedRows().length - 1; i >= 0; i--) {
							int row = fileNamesTable.getSelectedRows()[i];
							fileNamesModel.removeRow(row);
						}
						fileNamesModel.fireTableDataChanged();
					}
				});
			}
		});
	
		
		
		fileNamesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				ListSelectionModel lsm = (ListSelectionModel)e.getSource();
				if (lsm.isSelectionEmpty() == false) {
					removeFilesButton.setEnabled(true);
					int selectedRow = lsm.getMinSelectionIndex();
					String fileName = (String) fileNamesModel.getValueAt(selectedRow, 0);
					String newText = sourceMap.get(fileName);
					if (!sourceCodeArea.getText().equals(newText)) {
						sourceCodeArea.setText(newText);
						sourceCodeArea.setCaretPosition(0);
					}
				}
				else {
					removeFilesButton.setEnabled(false);					
				}
			}
	
		});	
	}
	

	public boolean modifyRubric() {
		RubricEntry entry = new RubricEntry();
		if (validateComboField("Name", nameCombo) ) {
			entry.setValue(RubricEntry.HeadingNames.NAME, (String)nameCombo.getSelectedItem());
			if (validateTextField("Value", valueField)) {				
				entry.setValue(RubricEntry.HeadingNames.VALUE, valueField.getText());
				if (validateTextField("Description", descriptionField)) {
					entry.setValue(RubricEntry.HeadingNames.DESCRIPTION, descriptionField.getText());

					entry.setValue(RubricEntry.HeadingNames.AUTOMATION_TYPE, automationTypeCombo.getSelectedItem().toString());
					if (createRubricAutomation(entry) == true) {
						rubricToModify.modifyEntry(entry);
						listener.rubricModified(rubricToModify);
						return true;
					}
				}
			}		
		}
		return false;
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
	

	

	public boolean modifyRubric(Rubric rubricToModify) {
		this.rubricToModify = rubricToModify;
		nameCombo.removeAllItems();
		setTitle("Edit Rubric: " + rubricToModify.getName());
		nameCombo.addItem("");
		for (RubricEntry element : rubricToModify.getEntries()) {
			nameCombo.addItem(element.getName());
		}
		saveRubric = false;
		setVisible(true);
		return saveRubric;
		
	}

	@Override
	public void itemStateChanged(ItemEvent evt) {

		RubricEntry.AutomationTypes automationType = (RubricEntry.AutomationTypes) evt.getItem();
		if (automationType != currentAutomation) {
			switch(currentAutomation) {
			case RUN_CODE:
				removeRunCodeItems();
			}
			currentAutomation = automationType;

			switch(currentAutomation) {
			case RUN_CODE:
				addRunCodeItems();
				break;
			default:
				pack();
			}

			revalidate();
			//pack();
			repaint();

		}
	}
	
	private boolean createRubricAutomation(RubricEntry entry) {
		RubricEntry.AutomationTypes automationType = (RubricEntry.AutomationTypes)automationTypeCombo.getSelectedItem();
		switch (automationType) {
		case RUN_CODE:
			return createRunCodeAutomation(entry);
		}
		return true;
	}



	private void createMainRunnerPanel() {
		JPanel panel = new JPanel();

		panel.setLayout(new BorderLayout());
		panel.setMinimumSize(new Dimension(20, 30));
		DefaultTableModel tableModel = new DefaultTableModel(new String[] { "System.in", "Expected System.out" }, 0);
		mainRunnerTable = new JTable(tableModel);
		tableModel.setNumRows(200);
		tableModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				int lastRow = e.getLastRow();
				int rowCount = tableModel.getRowCount();				
				if (lastRow + 1 == rowCount) {
					String entry = (String) tableModel.getValueAt(lastRow, 0);
					if (entry.length() != 0) {
						tableModel.addRow(new String[] { "", "" });
					}
				}
			}
		});
		mainRunnerTable.setCellSelectionEnabled(true);
		new ExcelAdapter(mainRunnerTable);

		panel.add(new JScrollPane(mainRunnerTable), BorderLayout.CENTER);

	}

	private void addRunCodeItems() {
		add(sourceCodePanel, BorderLayout.CENTER);
		removeFilesButton.setEnabled(false);

		addLabelAndComponent(namePanel, methodToCallLabel, methodNameField, 3);
		addLabelAndComponent(namePanel, methodBeingCalledLabel, methodBeingCalledField, 4);
		buttonLayout.setRows(buttonLayout.getRows() + 2);
		buttonsPanel.add(addFilesButton);
		buttonsPanel.add(removeFilesButton);
		namePanel.revalidate();
		buttonsPanel.revalidate();
		
		pack();
	}

	private void fillRunCode(RubricEntry entry) {
		RubricEntryRunCode runCode = (RubricEntryRunCode)entry.getAutomation();

		if (runCode != null) {
			
			methodNameField.setText(runCode.getMethodToCall());
			methodBeingCalledField.setText(runCode.getMethodBeingChecked());
			sourceMap.clear();
			while (fileNamesModel.getRowCount() != 0) {
				fileNamesModel.removeRow(0);
			}
			List<FileData> sourceFiles = runCode.getSourceFiles();
			for (FileData sourceFile : sourceFiles) {
				addRunCodeFile(sourceFile.getName(), sourceFile.getFileContents());
			}
		}		
	}
	
	private void addRunCodeFile(String fileName, String fileText) {
		fileNamesModel.addRow(new String[] {fileName});
		sourceMap.put(fileName, fileText);
		sourceCodeArea.setText(fileText);
		sourceCodeArea.setCaretPosition(0);
	}
	
	private void removeRunCodeItems() {
		remove(sourceCodePanel);
		namePanel.remove(methodNameField);
		namePanel.remove(methodToCallLabel);
		namePanel.remove(methodBeingCalledLabel);
		namePanel.remove(methodBeingCalledField);
		
		buttonsPanel.remove(addFilesButton);
		buttonsPanel.remove(removeFilesButton);
		buttonLayout.setRows(3);
		
		namePanel.revalidate();
		buttonsPanel.revalidate();

		
	}
	
	private boolean createRunCodeAutomation(RubricEntry entry) {
		if (validateTextField(methodBeingCalledLabel.getText(), methodBeingCalledField)) {
			if (validateTextField(methodToCallLabel.getText(), methodNameField)) {
				if (fileNamesModel.getRowCount() == 0) {
					JOptionPane.showMessageDialog(null,  "There have been no files added", "Need source file", JOptionPane.ERROR_MESSAGE);										
				}
				else {
					RubricEntryRunCode runCode = new RubricEntryRunCode();
					for (int i = 0; i < fileNamesModel.getRowCount(); i++) {
						String fileName = (String)fileNamesModel.getValueAt(i, 0);
						FileData sourceFile = new FileData(fileName, sourceMap.get(fileName), "", null);
						runCode.addSourceContents(sourceFile);
					}
					runCode.setMethodBeingChecked(methodBeingCalledField.getText());
					runCode.setMethodToCall(methodNameField.getText());
					entry.setAutomation(runCode);
					return true;					
				}
			}
		}
		return false;
	}


	private void addLabelAndComponent(JPanel parent, String label, JComponent component, int y) {
		addLabelAndComponent(parent, new JLabel(label), component, y);
	}
	private void addLabelAndComponent(JPanel parent, JLabel label, JComponent component, int y) {
		
		GridBagConstraints l = new GridBagConstraints();
		l.weightx = 0;
		l.weighty = 0;
		l.gridx = 0;
		l.gridy = y;
		l.gridheight = 1;
		l.anchor = GridBagConstraints.LINE_END;
		parent.add(label, l);
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
}
