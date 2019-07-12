package net.cdonald;
import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;

import net.cdonald.googleClassroom.gui.MainGoogleClassroomFrame;

public class GUIRunner implements Runnable {
	

	@Override
	public void run() {
		try {
			new MainGoogleClassroomFrame();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//new GradingWindow();
	}


	public static void main(String[] args) {
		


		SwingUtilities.invokeLater(new GUIRunner());

	}

}
