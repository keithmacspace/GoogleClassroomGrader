package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.cdonald.googleClassroom.googleClassroomInterface.DataFetchListener;
import net.cdonald.googleClassroom.googleClassroomInterface.FetchDoneListener;
import net.cdonald.googleClassroom.model.ClassroomData;

public class GoogleSheetDialog extends JDialog implements DataFetchListener, FetchDoneListener {

	private static final long serialVersionUID = 3861011773987299144L;
	private JTextField url;
	private JButton okButton;
	private JButton cancelButton;
	private JComboBox<ClassroomData> sheetCombo;
	private GoogleSheetDialogListener listener;
	private List<ClassroomData> sheets;

	public GoogleSheetDialog(Frame parent, GoogleSheetDialogListener listener, String title) {
		super(parent, title, false);
		sheets = new ArrayList<ClassroomData>();
		this.listener = listener;
		initLayout();
	}

	private void initLayout() {		

		JPanel buttonsPanel = setupButtonLayout();
		JPanel controlPanel = setupControlLayout();		

		add(controlPanel, BorderLayout.CENTER);
		add(buttonsPanel, BorderLayout.EAST);
		
		url.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				SwingWorker<Void, String> checkValidity = new SwingWorker<Void, String>() {


					@Override
					protected void process(List<String> chunks) {

					}

					@Override
					protected Void doInBackground() throws Exception {
						publish(url.getText());
						return null;
					}
					
				};
				
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		
		
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String urlName = url.getText();
				if (okButton.getText().compareTo("LOAD") == 0) {
					sheets.clear();
					listener.urlChanged(urlName);
				} else {
					String sheetName = sheetCombo.getSelectedItem().toString();
					if (listener.okSelected(urlName, sheetName) == true) {
						setVisible(false);
					}
				}
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

	@Override
	public void retrievedInfo(ClassroomData data) {
		sheets.add(data);
	}

	@Override
	public void done() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (sheets.size() != 0) {
					sheetCombo.setEnabled(true);
					for (ClassroomData data : sheets) {
						sheetCombo.addItem(data);
					}
						
					okButton.setText("SAVE");
					
				}
				else {
					sheetCombo.setEnabled(false);
					JOptionPane.showMessageDialog(GoogleSheetDialog.this, "Cannot load that URL, make sure it is shared with you", "Can't load",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}
	
	
	private JPanel setupButtonLayout() {
		int SPACE = 6;
		JPanel buttonsPanel = new JPanel();
		Border spaceBorder = BorderFactory.createEmptyBorder(SPACE, SPACE, SPACE, SPACE);
		buttonsPanel.setBorder(spaceBorder);
		GridLayout buttonLayout = new GridLayout(2, 0);
		final int GAP_SIZE = 6;
		buttonLayout.setVgap(GAP_SIZE);			
		buttonsPanel.setLayout(buttonLayout);
		
		okButton = new JButton("OK");
		okButton.setEnabled(false);
		cancelButton = new JButton("CANCEL");
		buttonsPanel.add(okButton);
		buttonsPanel.add(cancelButton);
		setLayout(new BorderLayout());
		return buttonsPanel;
	}
	private JPanel setupControlLayout() {
		Border titleBorder = BorderFactory.createTitledBorder("Copy in the url of the Google sheet you wish to use");
		JPanel controlPanel = new JPanel();				
		url = new JTextField(40);
		sheetCombo = new JComboBox<ClassroomData>();
		sheetCombo.setPreferredSize(new JTextField("sample size longish name").getPreferredSize());
		sheetCombo.setEditable(true);
		sheetCombo.setEnabled(false);
		controlPanel.setBorder(titleBorder);
		controlPanel.setLayout(new GridBagLayout());
		GridBagConstraints urlConstraints = new GridBagConstraints();
		urlConstraints.fill = GridBagConstraints.HORIZONTAL;
		urlConstraints.gridwidth = GridBagConstraints.REMAINDER;
		addLabelAndComponent(controlPanel, "url:", url, 0, urlConstraints);
		GridBagConstraints comboConstraints = new GridBagConstraints();
		comboConstraints.fill = GridBagConstraints.NONE;
		addLabelAndComponent(controlPanel, "book name:", sheetCombo, 1, comboConstraints);
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
