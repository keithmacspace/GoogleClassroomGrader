package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class DebugLogDialog extends JDialog {
	private static DebugLogDialog dbg = null;
	private JTextArea textArea;
	public DebugLogDialog(Frame parent) {
		super(parent, "Debug Logs", false);		
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(400, 400));
		textArea = new JTextArea();
		dbg = this;
		add(new JScrollPane(textArea));
	}
	
	public static void append(String text) {
		if (dbg != null) {
			dbg.textArea.append(text);
		}
		else {
			System.err.print(text);
		}
	}
	public static void appendln(String text) {
		if (dbg != null) {
			dbg.textArea.append(text + "\n");
		}
		else {
			System.err.println(text);
		}
	}	
}
