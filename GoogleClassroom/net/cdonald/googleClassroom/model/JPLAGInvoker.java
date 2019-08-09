package net.cdonald.googleClassroom.model;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.utils.FileUtils;



public class JPLAGInvoker {

 

	public static String invokeJPLAG(Map<String, List<FileData>> files, List<StudentData> students, String workingDir) {
		String assignmentPath = saveFiles(files, students, workingDir);
		String jplagOut = assignmentPath + File.separator + "jplagOut";
		List<String> args = new ArrayList<String>();
		String javaPath = System.getProperty("java.home");
		javaPath += File.separator + "bin" + File.separator + "java.exe";
		args.add("\"" + javaPath + "\"");
		args.add("-jar");	
		try {
			String jarDir = new File(JPLAGInvoker.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
			jarDir += File.separator + "jplag-2.12.1.jar";
			args.add(jarDir);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		args.add("-vq");
		args.add("-s");
		args.add("\"" + assignmentPath + "\"");
		args.add("-r");
		args.add("\"" + jplagOut + "\"");
		args.add("-l");
		args.add("java19");
		for (String arg : args) {
			System.out.print(arg + " ");
		}
		ProcessBuilder pb = new ProcessBuilder(args);		
		pb.inheritIO();
		try {
			Process process = pb.start();
			process.waitFor();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String finalOutput = jplagOut.replace("\\", "/");
		finalOutput += "/index.html";

		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			try {
				Desktop.getDesktop().browse(new URI(finalOutput));
			} catch (IOException e) {

			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return finalOutput;
	}

	private static String saveFiles(Map<String, List<FileData>> files, List<StudentData> students, String workingDir) {
		String assignmentPath = workingDir + File.separator + "JPLAG";
		try {
			FileUtils.removeRecursive(Paths.get(assignmentPath));
		} catch (IOException e1) {

		}		
		new File(assignmentPath).mkdir();
		for (StudentData student : students) {
			List<FileData> studentFiles = files.get(student.getId());
			if (studentFiles != null) {
				String studentPath = assignmentPath + File.separator + student.getName() + student.getFirstName();
				new File(studentPath).mkdir();
				for (FileData file : studentFiles) {
					String studentFileName = studentPath + File.separator + file.getName();
					try {
						PrintWriter out = new PrintWriter(studentFileName);
						out.print(file.getFileContents());
						out.flush();
						out.close();
					} catch (FileNotFoundException e) {

					}
				}
			}
		}
		return assignmentPath;
	}

}