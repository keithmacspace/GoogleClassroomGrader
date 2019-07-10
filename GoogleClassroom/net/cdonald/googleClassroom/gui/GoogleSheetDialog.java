package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import net.cdonald.googleClassroom.googleClassroomInterface.DataFetchListener;
import net.cdonald.googleClassroom.googleClassroomInterface.FetchDoneListener;
import net.cdonald.googleClassroom.model.ClassroomData;

public class GoogleSheetDialog extends JDialog implements DataFetchListener, FetchDoneListener {

	private static final long serialVersionUID = 3861011773987299144L;
	private JTextField url;	
	private JLabel urlLabel;
	private JLabel sheetLabel;
	private JButton okButton;
	private JButton cancelButton;
	private JComboBox<ClassroomData> sheetCombo;	
	private GoogleSheetDialogListener listener;
	public GoogleSheetDialog(Frame parent, GoogleSheetDialogListener listener, String title) {
		super(parent, title, false);
		
		urlLabel = new JLabel("url: ");
		sheetLabel = new JLabel("book: ");
		url = new JTextField(40);
		sheetCombo = new JComboBox<ClassroomData>();		
		sheetCombo.setPreferredSize(new JTextField("sample size").getPreferredSize());
		sheetCombo.setEditable(true);
		sheetCombo.addItem(new ClassroomData("", ""));
		okButton = new JButton("OK");
		cancelButton = new JButton("CANCEL");
		this.listener = listener; 
		setSize(600, 140);
		initLayout();		
	}
	
	private void initLayout() {
		JPanel textPanel = new JPanel();
		JPanel controlPanel = new JPanel();
		JPanel buttonsPanel = new JPanel();
		
		int space = 6;
		Border titleBorder = BorderFactory.createTitledBorder("Copy in the url of the Google sheet you wish to use");
		Border spaceBorder = BorderFactory.createEmptyBorder(space, space, space, space);
		Border both = BorderFactory.createCompoundBorder(spaceBorder, titleBorder);
		textPanel.setBorder(spaceBorder);
		controlPanel.setBorder(titleBorder);
		buttonsPanel.setBorder(spaceBorder);		
		buttonsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		controlPanel.add(urlLabel);
		controlPanel.add(url);
		controlPanel.add(sheetLabel);
		controlPanel.add(sheetCombo);
		okButton.setPreferredSize(cancelButton.getPreferredSize());
		buttonsPanel.add(okButton);
		buttonsPanel.add(cancelButton);
		setLayout(new BorderLayout());
		add(textPanel, BorderLayout.NORTH);
		add(controlPanel, BorderLayout.CENTER);
		add(buttonsPanel, BorderLayout.EAST);
		url.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (listener.urlChanged(url.getText()) == false) {
					url.setText("");
				}
			}			
		});
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String urlName = url.getText();
				String sheetName = sheetCombo.getSelectedItem().toString();
				if (listener.okSelected(urlName, sheetName) == true) {
					setVisible(false);
				}
			}			
		});
		
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);				
			}			
		});	
	}

	@Override
	public void retrievedInfo(ClassroomData data) {
		sheetCombo.addItem(data);
	}

	@Override
	public void done() {
		// TODO Auto-generated method stub
		
	}
	
}
