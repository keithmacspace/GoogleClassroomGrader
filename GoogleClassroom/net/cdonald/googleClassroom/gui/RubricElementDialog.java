package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntry.HeadingNames;

public class RubricElementDialog extends JDialog implements ItemListener {
	private static final long serialVersionUID = -5580080426150572162L;
	private JButton okButton;
	private JButton cancelButton;
	private JComboBox<RubricEntry.AutomationTypes> automationTypeCombo;
	private JTextField nameField;
	private JTextField descriptionField;
	private JTextField valueField;
	private JPanel cards;
	private JTable mainRunnerTable;
	Rubric rubricToModify;
	RubricModifiedListener listener;

	public RubricElementDialog(Frame parent, RubricModifiedListener listener) {
		super(parent, "Rubric Element", true);
		this.listener = listener;
		createCards();

		okButton = new JButton("OK");
		okButton.setMnemonic(KeyEvent.VK_O);
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

		JPanel namePanel = new JPanel();

		namePanel.setBorder(BorderFactory.createEmptyBorder(NAME_TOP_SPACE, 0, SPACE, SPACE));
		namePanel.setLayout(new GridBagLayout());
		nameField = new JTextField("", 20);
		descriptionField = new JTextField("", 20);
		valueField = new JTextField("", 20);
		namePanel.setMinimumSize(new Dimension(0, 80));

		addLabelAndComponent(namePanel, "name: ", nameField, 0);
		addLabelAndComponent(namePanel, "value: ", valueField, 1);
		addLabelAndComponent(namePanel, "description: ", descriptionField, 2);

		JPanel buttonsPanel = new JPanel();
		// buttonsPanel.setLayout(new FlowLayout());
		GridLayout buttonLayout = new GridLayout(2, 0);
		final int GAP_SIZE = 6;
		buttonLayout.setVgap(GAP_SIZE);
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(BUTTON_TOP_SPACE, SPACE, SPACE, SPACE));
		buttonsPanel.setLayout(buttonLayout);
		buttonsPanel.add(okButton);
		buttonsPanel.add(cancelButton);

		JPanel constantPanel = new JPanel();
		constantPanel.setLayout(new BorderLayout());
		constantPanel.add(comboPanel, BorderLayout.LINE_START);
		constantPanel.add(namePanel, BorderLayout.CENTER);
		constantPanel.add(buttonsPanel, BorderLayout.LINE_END);

		// cards = new JPanel(new CardLayout());
		setLayout(new BorderLayout());
		add(constantPanel, BorderLayout.NORTH);
		add(cards, BorderLayout.SOUTH);
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addRubricEntry();
				setVisible(false);
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		pack();

		// createLayout();
	}

	public void addRubricEntry() {
		if (rubricToModify == null) {
			rubricToModify = new Rubric();
		}
		RubricEntry entry = new RubricEntry();
		entry.setValue(RubricEntry.HeadingNames.NAME, nameField.getText());
		entry.setValue(RubricEntry.HeadingNames.VALUE, valueField.getText());
		entry.setValue(RubricEntry.HeadingNames.DESCRIPTION, descriptionField.getText());
		entry.setValue(RubricEntry.HeadingNames.AUTOMATION_TYPE, automationTypeCombo.getSelectedItem().toString());
		rubricToModify.addEntry(entry);
		listener.rubricModified(rubricToModify);

	}

	public void modifyRubric(Rubric rubricToModify) {
		this.rubricToModify = rubricToModify;
		setVisible(true);
	}

	@Override
	public void itemStateChanged(ItemEvent evt) {
		CardLayout cl = (CardLayout) (cards.getLayout());
		RubricEntry.AutomationTypes automationType = (RubricEntry.AutomationTypes) evt.getItem();
		cl.show(cards, automationType.toString());
	}

	private void createCards() {
		cards = new JPanel(new CardLayout());
		createMainRunnerCard();
		createEmptyCard();
	}

	private void createMainRunnerCard() {
		JPanel panel = new JPanel();

		panel.setMinimumSize(new Dimension(20, 30));
		DefaultTableModel tableModel = new DefaultTableModel(new String[] { "System.in", "Expected System.out" }, 0);
		mainRunnerTable = new JTable(tableModel);
		tableModel.setNumRows(200);
		tableModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				int lastRow = e.getLastRow();
				int rowCount = tableModel.getRowCount();
				// System.err.println("Last row = " + lastRow + " Row count = " + rowCount);
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

		panel.add(new JScrollPane(mainRunnerTable));
		cards.add(panel, RubricEntry.AutomationTypes.CALL_MAIN.toString());

	}

	private void createEmptyCard() {
		
		RubricEntry.AutomationTypes [] types = {RubricEntry.AutomationTypes.NONE, RubricEntry.AutomationTypes.COMPILES};
		for (RubricEntry.AutomationTypes type : types) {
			JPanel panel = new JPanel();
			panel.setMinimumSize(new Dimension(20, 30));
			cards.add(panel, type.toString());
		}
	}

	private void createLayout() {
//		setLayout(new BorderLayout());
//			
//		JPanel options = new JPanel();
//		options.setLayout(new GridBagLayout());
//		//int p = 0;
//		for (int i = 1; i < RubricEntry.HeadingNames.values().length; i++) {
//			JTextField field = new JTextField(15);
//			//fields.add(field);
//			String label = RubricEntry.HeadingNames.values()[i] + ": ";
//			addLabelAndComponent(options, label, field, i);
//			p = i;
//		}
//
//		
//		JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, options, new JScrollPane(new JTextArea()));
//		
// 
//
//		
//		add(splitter, BorderLayout.NORTH);
//		add(buttonsPanel, BorderLayout.SOUTH);

	}

	private void addLabelAndComponent(JPanel parent, String label, JTextField component, int y) {

		GridBagConstraints l = new GridBagConstraints();
		l.weightx = 0;
		l.weighty = 0;
		l.gridx = 0;
		l.gridy = y;
		l.gridheight = 1;
		l.anchor = GridBagConstraints.LINE_END;
		parent.add(new JLabel(label), l);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = y;
		parent.add(component, c);
	}
}
