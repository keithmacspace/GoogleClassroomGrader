package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
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
	private List<ClassroomData> sheets;

	public GoogleSheetDialog(Frame parent, GoogleSheetDialogListener listener, String title) {
		super(parent, title, false);
		sheets = new ArrayList<ClassroomData>();
		urlLabel = new JLabel("url: ");
		sheetLabel = new JLabel("book: ");
		url = new JTextField(40);
		sheetCombo = new JComboBox<ClassroomData>();
		sheetCombo.setPreferredSize(new JTextField("sample size").getPreferredSize());
		sheetCombo.setEditable(true);
		sheetCombo.setEnabled(false);
		okButton = new JButton("LOAD");
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
	
	@Override
	public void remove(Set<String> ids) {
		
	}

}
