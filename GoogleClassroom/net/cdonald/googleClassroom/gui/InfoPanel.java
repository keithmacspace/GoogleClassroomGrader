package net.cdonald.googleClassroom.gui;

import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.cdonald.googleClassroom.listenerCoordinator.AddProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetInfoLabelListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetRunningLabelListener;

public class InfoPanel extends JPanel {
	private Map<String, JProgressBar> progressBars;
	private JLabel infoLabel;
	private JLabel runningLabel;
	private final String INFO_STRING = "                                                                      ";
	private final String RUNNING_STRING = "                           ";
	public InfoPanel() {
		super();
		progressBars = new HashMap<String, JProgressBar>();
		infoLabel = new JLabel(INFO_STRING);
		runningLabel = new JLabel(RUNNING_STRING);
		setLayout(new FlowLayout());
		add(infoLabel);
		add(runningLabel);
		ListenerCoordinator.addListener(AddProgressBarListener.class, new AddProgressBarListener() {
			@Override
			public void fired(String progressBarName) {
				JProgressBar progress = new JProgressBar();
				progress.setString(progressBarName);	
				progress.setIndeterminate(true);
				progress.setStringPainted(true);
				progressBars.put(progressBarName, progress);
				add(progress);
				revalidate();
				repaint();
			}
		});
		ListenerCoordinator.addListener(RemoveProgressBarListener.class, new RemoveProgressBarListener() {
			@Override
			public void fired(String progressBarName) {
				JProgressBar progress = progressBars.remove(progressBarName);
				if (progress != null) {
					progress.setVisible(false);
					remove(progress);
					revalidate();
					repaint();
				}
			}
		});
		
		ListenerCoordinator.addListener(SetRunningLabelListener.class, new SetRunningLabelListener() {
			@Override
			public void fired(String label) {				
				String text = label;
				if (text.length() < RUNNING_STRING.length()) {
					text += RUNNING_STRING.substring(text.length());
				}
				runningLabel.setText(text);
			}
			
		});
		ListenerCoordinator.addListener(SetInfoLabelListener.class, new SetInfoLabelListener() {
			@Override
			public void fired(String label) {				
				String text = label;
				if (text.length() < INFO_STRING.length()) {
					text += INFO_STRING.substring(text.length());
				}
				runningLabel.setText(text);
			}
			
		});
	}

}
