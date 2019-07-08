import javax.swing.SwingUtilities;


import gui.MainGoogleClassroomFrame;

public class GUIRunner implements Runnable {

	@Override
	public void run() {
		new MainGoogleClassroomFrame();
		//new GradingWindow();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new GUIRunner());

	}

}
