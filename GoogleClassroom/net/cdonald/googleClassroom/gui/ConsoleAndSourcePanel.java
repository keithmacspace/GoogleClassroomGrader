package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultEditorKit;

import net.cdonald.googleClassroom.model.FileData;

public class ConsoleAndSourcePanel extends JPanel implements ConsoleDisplayListener {
	private static final long serialVersionUID = 1084761781946423738L;
	private List<ConsoleInputListener> listeners;
	private RecompileListener recompileListener;
	private JTextArea sourceCode;		
	private JTabbedPane tabbedPane;
	private JTextField consoleInput;
	private JTextArea consoleOutput;
	private JPopupMenu popupSource;
	private JPopupMenu popupInput;
	private JPopupMenu popupDisplays;
	private FileData currentFile;


	private static Semaphore pauseSemaphore = new Semaphore(1);
	private JTextArea consoleInputHistory;
	
	public ConsoleAndSourcePanel() {
		listeners = new ArrayList<ConsoleInputListener>();
		setMinimumSize(new Dimension(400, 400));

		createPopupMenu();
		createLayout();		
		setVisible(true);

	}
	
    
	public void addListener(ConsoleInputListener listener) {
		listeners.add(listener);
	}
	
	public void assignmentSelected() {
		sourceCode.setText("");
		consoleOutput.setText("");
		consoleInput.setText("");
		consoleInputHistory.setText("");
	}
	
	@Override
	public void startRunning(List<FileData> sourceList) {
		tabbedPane.setSelectedIndex(1);
		try {
			// Doing this prevents forward progress until the panes are ready
			pauseSemaphore.release();
			pauseSemaphore.acquire();			
			setWindowData(sourceList, "", "");
			// We will now hang here until the release in setWindowData
			pauseSemaphore.acquire();
			pauseSemaphore.release();
		} catch (InterruptedException e) {

		}		
	}

	// We can select students too quickly and end up creating a mishmash in the source code
	// window so use a semaphore to make sure that doesn't happen
	public void setWindowData(List<FileData> fileDataList, String outputText, String inputHistory) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				sourceCode.setText("");				
				if (fileDataList != null) {
					int oldPosition = 0;
					for (FileData fileData : fileDataList) {
						sourceCode.append(fileData.getFileContents());
						sourceCode.setCaretPosition(oldPosition);
						currentFile = fileData;
					}
				}
				consoleOutput.setText(outputText);
				consoleInputHistory.setText(inputHistory);
				consoleOutput.setCaretPosition(0);
				consoleInputHistory.setCaretPosition(0);				
				pauseSemaphore.release();				
			}

		});

	}


	public void setRecompileListener(RecompileListener recompileListener) {
		this.recompileListener = recompileListener;
	}


	private void createPopupMenu() {
		popupSource = new JPopupMenu();
		popupDisplays = new JPopupMenu();
		popupInput = new JPopupMenu();
			

        Action cut = new DefaultEditorKit.CutAction();
        cut.putValue(Action.NAME, "Cut");
        cut.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
        popupSource.add(cut);
        popupInput.add(cut);

        Action copy = new DefaultEditorKit.CopyAction();
        copy.putValue(Action.NAME, "Copy");
        copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
        popupSource.add(copy);
        popupInput.add(cut);
        popupDisplays.add(copy);

        Action paste = new DefaultEditorKit.PasteAction();
        paste.putValue(Action.NAME, "Paste");
        paste.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
        popupSource.add(paste);
        popupInput.add(paste);
        
        JMenuItem recompile = new JMenuItem("Recompile And Run");
        popupSource.add(recompile);
        recompile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (recompileListener != null) {
					recompileListener.recompileAndRun(currentFile, sourceCode.getText());
				}
				
			}        	
        });
    }



	private void createLayout() {
		setSize(800, 500);
		setLayout(new BorderLayout());

		consoleInput = new JTextField();
		sourceCode = new JTextArea();
		sourceCode.setComponentPopupMenu(popupSource);
		consoleInput.setText("");		
		consoleInput.setMinimumSize(new Dimension(20, 25));
		consoleInput.setPreferredSize(new Dimension(20, 25));
		consoleInput.setComponentPopupMenu(popupInput);
		consoleOutput = new JTextArea();
		consoleInputHistory = new JTextArea();
		consoleInputHistory.setEditable(false);
		consoleOutput.setEditable(false);
		consoleInputHistory.setComponentPopupMenu(popupDisplays);
		consoleOutput.setComponentPopupMenu(popupDisplays);
//		sourceSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(sourceCode), ioSplit);
		//sourceSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(sourceCode), new JScrollPane(consoleOutput));
		//sourceSplit.setResizeWeight(0.8);

		JPanel sourcePanel = new JPanel();
		sourcePanel.setBorder(BorderFactory.createEmptyBorder(5,  5,  5,  5));
		sourcePanel.setLayout(new BorderLayout());
		sourcePanel.add( new JScrollPane(sourceCode));
		sourcePanel.setVisible(true);
		JPanel ioPanel = new JPanel();
		ioPanel.setLayout(new BorderLayout());
		JPanel inputWrapper = new JPanel();
		inputWrapper.setLayout(new BorderLayout());;
		inputWrapper.setBorder(BorderFactory.createTitledBorder("Console Input"));
		//inputWrapper.setMinimumSize(new Dimension(40, 65));
		inputWrapper.add(consoleInput);

		JPanel outputWrapper = new JPanel();
		outputWrapper.setLayout(new BorderLayout());;
		outputWrapper.setBorder(BorderFactory.createTitledBorder("Console Output"));
		outputWrapper.add(new JScrollPane(consoleOutput));
		JPanel inputHistorWrapper = new JPanel();
		inputHistorWrapper.setLayout(new BorderLayout());;
		inputHistorWrapper.setBorder(BorderFactory.createTitledBorder("Input History"));
		inputHistorWrapper.add(new JScrollPane(consoleInputHistory));
		JSplitPane ioSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outputWrapper, inputHistorWrapper);
		ioSplit.setResizeWeight(0.9);

		
		
		ioPanel.add(ioSplit, BorderLayout.CENTER);
		ioPanel.add(inputWrapper, BorderLayout.SOUTH);
		setVisible(true);

		
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Source", sourcePanel);
		tabbedPane.addTab("Console", ioPanel);
		add(tabbedPane, BorderLayout.CENTER);


		consoleInput.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String text = consoleInput.getText();
				consoleInputHistory.setText(text);
				consoleInput.setText("");
				for (ConsoleInputListener listener : listeners) {
					listener.textInputted(text);
				}				
			}
		});

	}

	@Override
	public void textAdded(String text) {
		consoleOutput.append(text);
		
	}
	

}
