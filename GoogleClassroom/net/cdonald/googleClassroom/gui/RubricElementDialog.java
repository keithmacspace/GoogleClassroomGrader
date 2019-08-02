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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultEditorKit;

import net.cdonald.googleClassroom.listenerCoordinator.GetFileDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchRubricEditorDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SetFileDirListener;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntryRunCode;

public class RubricElementDialog extends JDialog implements RubricElementListener {
	private static final long serialVersionUID = -5580080426150572162L;
	private JButton saveButton;
	private JButton addRubricEntry;
	private JButton cancelButton;
	private JTable entriesTable;
	private RubricElementTableModel entriesModel;	
	private Rubric rubricToModify;
	private RubricModifiedListener listener;
	private JButton deleteButton;
	private RunCode runCode;
	private JPanel defaultPanel;
	private JTable mainRunnerTable;
	private String DEFAULT_SOURCE_STRING = "";
	private boolean saveRubric;
	private int priorSelectedIndex;

		
	
	public RubricElementDialog(Frame parent, RubricModifiedListener listener) {
		super(parent, "Rubric Element", true);
		priorSelectedIndex = -1;		
		this.listener = listener;
		entriesModel = new RubricElementTableModel();
		entriesTable = new JTable(entriesModel);		
		entriesTable.setDefaultRenderer(RubricEntry.AutomationTypes.class, new RubricElementRenderer(this));
		entriesTable.setDefaultEditor(RubricEntry.AutomationTypes.class, new RubricElementEditor());
		entriesTable.setRowHeight(20);
		entriesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);



		saveButton = new JButton("Save");
		addRubricEntry = new JButton("Add");
		deleteButton = new JButton("Delete");
		cancelButton = new JButton("Cancel");
		cancelButton.setMnemonic(KeyEvent.VK_C);



		JPanel constantPanel = new JPanel();
		constantPanel.setLayout(new BorderLayout());
		
		JPanel buttonsPanel = createButtonPanel(4);
		buttonsPanel.add(saveButton);
		buttonsPanel.add(addRubricEntry);
		buttonsPanel.add(deleteButton);
		buttonsPanel.add(cancelButton);			


		//constantPanel.add(entriesTable, BorderLayout.LINE_START);
		//constantPanel.add(new JScrollPane(entriesTable), BorderLayout.CENTER);
		constantPanel.add(buttonsPanel, BorderLayout.NORTH);

		defaultPanel = new JPanel();
		// cards = new JPanel(new CardLayout());
		defaultPanel.setLayout(new BorderLayout());
		defaultPanel.add(new JScrollPane(entriesTable), BorderLayout.CENTER);
		defaultPanel.add(constantPanel, BorderLayout.EAST);

		add(defaultPanel, BorderLayout.CENTER);
		ListSelectionModel selectionModel = entriesTable.getSelectionModel();

		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveRubric = true;				
				setVisible(false);				
			}
		});
		
		addRubricEntry.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rubricToModify.addNewEntry();
				entriesModel.fireTableStructureChanged();
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
					entriesModel.fireTableStructureChanged();
				}
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveRubric = false;
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

		createPopupMenu(selectionModel);

		runCode = new RunCode();
		pack();
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
	

	

	public boolean modifyRubric(Rubric rubricToModify) {
		this.rubricToModify = rubricToModify;
		entriesModel.setRubricToModify(rubricToModify);
		saveRubric = false;
		setVisible(true);
		return saveRubric;
		
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
	
	private class RunCode {
		private Dimension runCodeDimension;
		private JButton addFilesButton;
		private JButton removeFilesButton;
		private JTextField methodToCallField;
		private JLabel methodToCallLabel;
		private JLabel methodBeingCalledLabel;
		private JTextField methodBeingCalledField;
		private JTextArea sourceCodeArea;	
		private JSplitPane runSplit;
		private JPanel runPanel;
		private JTabbedPane sourceTabs;
		private JPanel sourceCodePanel;
		private RubricEntryRunCode associatedAutomation;
		private boolean isActive;
		public RunCode() {
			isActive = false;
			JPanel namePanel = new JPanel();
			namePanel.setLayout(new GridBagLayout());
			methodToCallField = new JTextField("", 20);
			sourceCodeArea = new JTextArea(DEFAULT_SOURCE_STRING);
			sourceCodeArea.setEditable(false);

			methodToCallLabel = new JLabel("Method to call: ");
			
			methodBeingCalledLabel = new JLabel("Testing Student Method: ");
			methodBeingCalledField = new JTextField("", 20);
			addLabelAndComponent(namePanel, methodToCallLabel, methodToCallField, 0);
			addLabelAndComponent(namePanel, methodBeingCalledLabel, methodBeingCalledField, 1);
			
			
			addFilesButton = new JButton("Add Files");
			removeFilesButton = new JButton("Remove File");
			JPanel buttonsPanel = createButtonPanel(2);
			buttonsPanel.add(addFilesButton);
			buttonsPanel.add(removeFilesButton);
			
			JPanel nameAndButtons = new JPanel();
			nameAndButtons.setLayout(new BorderLayout());
			nameAndButtons.add(namePanel, BorderLayout.CENTER);
			nameAndButtons.add(buttonsPanel, BorderLayout.EAST);

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
								addRunCodeFile(fileName, text, true);
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}						
						}
					}
				}
			});
			
			removeFilesButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							if (sourceTabs.getTabCount() > 0) {
								int selectedIndex = sourceTabs.getSelectedIndex();
								String fileName = sourceTabs.getTitleAt(selectedIndex);
								associatedAutomation.removeSourceContents(fileName);
								sourceTabs.removeTabAt(selectedIndex);
							}
						}
					});
				}
			});
			
			methodToCallField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {					
					associatedAutomation.setMethodToCall(methodToCallField.getText());
					
				}
				@Override
				public void removeUpdate(DocumentEvent e) {
					associatedAutomation.setMethodToCall(methodToCallField.getText());
					
				}
				@Override
				public void changedUpdate(DocumentEvent e) {
				}				
			});
			
			methodBeingCalledField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {					
					associatedAutomation.setMethodBeingChecked(methodBeingCalledField.getText());
					
				}
				@Override
				public void removeUpdate(DocumentEvent e) {
					associatedAutomation.setMethodBeingChecked(methodBeingCalledField.getText());					
				}
				@Override
				public void changedUpdate(DocumentEvent e) {
				}				
				
			});
		
		}
		public boolean isActive() {
			return isActive;
		}
		
		private void addRunCodeItems() {
			isActive = true;
			RubricEntry associatedEntry = getCurrentEntry();
			if (associatedEntry.getAutomation() == null || !(associatedEntry.getAutomation() instanceof RubricEntryRunCode)) {
				associatedEntry.setAutomation(new RubricEntryRunCode());
			}
			associatedAutomation = (RubricEntryRunCode)associatedEntry.getAutomation();
			fillRunCode();
			remove(defaultPanel);
			if (runCodeDimension != null) {
				runPanel.setPreferredSize(runCodeDimension);
			}
			runSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, defaultPanel, runPanel);
			add(runSplit, BorderLayout.CENTER);		
			pack();
		}
		private void removeRunCodeItems() {
			isActive = false;
			Dimension current = runSplit.getTopComponent().getSize();
			runCodeDimension = new Dimension(runPanel.getSize());
			remove(runSplit);
			defaultPanel.setPreferredSize(current);
			add(defaultPanel);
		}
		private void fillRunCode() {				
			methodToCallField.setText(associatedAutomation.getMethodToCall());
			methodBeingCalledField.setText(associatedAutomation.getMethodBeingChecked());
			while(sourceTabs.getTabCount() != 0) {
				sourceTabs.removeTabAt(0);				
			}
			List<FileData> sourceFiles = associatedAutomation.getSourceFiles();
			for (FileData sourceFile : sourceFiles) {
				addRunCodeFile(sourceFile.getName(), sourceFile.getFileContents(), false);
			}		
		}
		private void addRunCodeFile(String fileName, String fileText, boolean addToAutomation) {
			JPanel sourcePanel = new JPanel();
			sourcePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			sourcePanel.setLayout(new BorderLayout());
			JTextArea sourceArea = new JTextArea();		
			sourcePanel.add(new JScrollPane(sourceArea));
			sourceArea.setText(fileText);
			sourceArea.getDocument().addDocumentListener(new SourceDocumentListener(associatedAutomation, fileName, sourceArea));
			if (firstTabIsEmpty()) {
				sourceTabs.setTitleAt(0, fileName);
				sourceTabs.setComponentAt(0, sourcePanel);
			}
			else {

				sourceTabs.addTab(fileName, sourcePanel);
			}
			if (addToAutomation) {
				associatedAutomation.addSourceContents(new FileData(fileName, fileText, "0", null));
			}
			runPanel.revalidate();
		}
		
		private boolean firstTabIsEmpty() {
			return (sourceTabs.getTabCount() == 1 && sourceTabs.getTitleAt(0).length() == 0);
		}
		
		private boolean sourceTabsAreEmpty() {
			if (sourceTabs.getTabCount() == 0 || firstTabIsEmpty()) {
				return true;
			}
			return false;
		}
	}
}
