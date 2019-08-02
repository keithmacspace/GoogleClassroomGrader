package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class DebugLogDialog extends JDialog {
	private JTextArea textArea;
	public DebugLogDialog(Frame parent) {
		super(parent, "Debug Logs", false);
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(400, 400));
		textArea = new JTextArea();
		add(new JScrollPane(textArea));
	}
	
	public void append(String text) {
		textArea.append(text);
	}
	
}
