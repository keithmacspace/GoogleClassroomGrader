package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.cdonald.googleClassroom.googleClassroomInterface.SheetFetcher;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SheetFetcherListener;
import net.cdonald.googleClassroom.model.ClassroomData;

public class GoogleSheetDialog extends JDialog {

	private static final long serialVersionUID = 3861011773987299144L;
	private JTextField url;
	private JButton okButton;
	private JButton cancelButton;
	private JButton validateButton;
	private JComboBox<String> sheetCombo;
	private Class<?> listenerClass;
	private JLabel fileNameLabel;
	private JProgressBar progressBar;


	public GoogleSheetDialog(Frame parent) {
		super(parent, "", false);
		this.listenerClass = null;
		initLayout();
	}
	
	public void setVisible(String title, Class<?> listener, String currentURL) {
		this.listenerClass = listener;
		setTitle(title);
		url.setText(currentURL);
		setVisible(true);
	}

	private void initLayout() {		
		progressBar = new JProgressBar();

		JPanel buttonsPanel = setupButtonLayout();
		JPanel controlPanel = setupControlLayout();		

		add(controlPanel, BorderLayout.CENTER);
		add(buttonsPanel, BorderLayout.EAST);
		
		url.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				okButton.setEnabled(false);				
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}			
		});
		
		
		
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String urlName = url.getText();				
				String sheetName = sheetCombo.getSelectedItem().toString();
				ListenerCoordinator.fire(listenerClass, urlName, sheetName);
				setVisible(false);
			}
		});
		
		validateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String urlName = url.getText();				
				//sheetCombo.setEnabled(false);
				fillSheets(urlName);

			}
		});

		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		pack();
	}
	
	private void fillSheets(String urlName) {

		fileNameLabel.setVisible(false);
		progressBar.setVisible(true);
		while (sheetCombo.getItemCount() != 0) {
			sheetCombo.removeItemAt(0);
		}
		ListenerCoordinator.runLongQuery(SheetFetcher.class, new SheetFetcherListener(urlName) {
			@Override
			public void process(List<ClassroomData> list) {
				for (ClassroomData data : list) {
					if (data.isEmpty()) {
						fileNameLabel.setText("filename: " + data.getName());
					}
					else {
						sheetCombo.addItem(data.getName());
					}
				}
			}					
			@Override
			public void done() {
				progressBar.setVisible(false);
				fileNameLabel.setVisible(true);
				if (sheetCombo.getItemCount() == 0) {
					JOptionPane.showMessageDialog(GoogleSheetDialog.this, "Cannot load that URL, make sure it is shared with you, and it is the correct url", "Can't load",
							JOptionPane.ERROR_MESSAGE);
				}
				else {
					//sheetCombo.setEnabled(true);
					okButton.setEnabled(true);
					//sheetCombo.setEditable(true);
				}
				
			}
		});	
	}


	
	private JPanel setupButtonLayout() {
		int SPACE = 6;
		JPanel buttonsPanel = new JPanel();
		Border spaceBorder = BorderFactory.createEmptyBorder(SPACE, SPACE, SPACE, SPACE);
		buttonsPanel.setBorder(spaceBorder);
		GridLayout buttonLayout = new GridLayout(3, 0);
		final int GAP_SIZE = 6;
		buttonLayout.setVgap(GAP_SIZE);			
		buttonsPanel.setLayout(buttonLayout);
		
		okButton = new JButton("OK");
		okButton.setEnabled(false);
		validateButton = new JButton("Validate");
		cancelButton = new JButton("CANCEL");
		buttonsPanel.add(okButton);
		buttonsPanel.add(validateButton);
		buttonsPanel.add(cancelButton);
		setLayout(new BorderLayout());
		return buttonsPanel;
	}
	private JPanel setupControlLayout() {
		Border titleBorder = BorderFactory.createTitledBorder("Copy in the shareable link of the Google sheet you wish to use");
		JPanel controlPanel = new JPanel();				
		url = new JTextField(40);
		sheetCombo = new JComboBox<String>();
		sheetCombo.setPreferredSize(new JTextField("sample size longish name").getPreferredSize());
		sheetCombo.setEditable(true);
		sheetCombo.setEnabled(true);
		sheetCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("comboBoxEdited")) {
					sheetCombo.insertItemAt((String) sheetCombo.getSelectedItem(), 0);
				}
				
			}
			
		});
		controlPanel.setBorder(titleBorder);
		controlPanel.setLayout(new GridBagLayout());
		GridBagConstraints urlConstraints = new GridBagConstraints();
		urlConstraints.fill = GridBagConstraints.HORIZONTAL;
		urlConstraints.gridwidth = GridBagConstraints.REMAINDER;
		addLabelAndComponent(controlPanel, "url:", url, 0, urlConstraints);
		GridBagConstraints comboConstraints = new GridBagConstraints();
		comboConstraints.fill = GridBagConstraints.NONE;
		
		fileNameLabel = new JLabel("");
		addLabelAndComponent(controlPanel, "book name:", sheetCombo, 1, comboConstraints);
		GridBagConstraints fileNameConstraints = new GridBagConstraints();
		fileNameConstraints.gridx = 2;
		fileNameConstraints.gridy = 1;
		fileNameConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
		controlPanel.add(fileNameLabel, fileNameConstraints);
		
		GridBagConstraints progressBarConstraints = new GridBagConstraints();
		progressBarConstraints.gridx = 3;
		progressBarConstraints.gridy = 1;
		progressBarConstraints.anchor = GridBagConstraints.LINE_END;
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);
		controlPanel.add(progressBar, progressBarConstraints);
		
		return controlPanel;
	}
	
	
	
	private void addLabelAndComponent(JPanel parent, String label, Component component, int y, GridBagConstraints c) {

		GridBagConstraints l = new GridBagConstraints();
		l.weightx = 0;
		l.weighty = 0;
		l.gridx = 0;
		l.gridy = y;
		l.gridheight = 1;	
		l.anchor = GridBagConstraints.LINE_END;
		parent.add(new JLabel(label), l);		
		c.weightx = 1.0;
		c.weighty = 0.0;		
		c.anchor = GridBagConstraints.LINE_START;
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = y;

		parent.add(component, c);
	}

}
