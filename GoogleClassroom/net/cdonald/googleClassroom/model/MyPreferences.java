package net.cdonald.googleClassroom.model;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * 
 * Just a small class to keep the preferences - keeps the names stable.
 *
 */
public class MyPreferences {
	private Preferences preferences;
	private enum PrefNames {WORKING_DIR, CLASS_ID, CLASS_NAME, FILE_DIR, RUBRIC_FILE};
	
	public MyPreferences() {
		preferences = Preferences.userNodeForPackage(net.cdonald.googleClassroom.gui.MainGoogleClassroomFrame.class);
	}
	
	public String getRubricFile() {
		return preferences.get(PrefNames.RUBRIC_FILE.toString(), null);
	}
	
	public void setRubricFile(String rubricFile) {
		preferences.put(PrefNames.RUBRIC_FILE.toString(), rubricFile);
	}
	
	public String getWorkingDir() {
		return preferences.get(PrefNames.WORKING_DIR.toString(), null);		
	}

	public void setWorkingDir(String workingDir) {
		preferences.put(PrefNames.WORKING_DIR.toString(), workingDir);
		makeDirs();		
	}
	
	public String getFileDir() {
		return preferences.get(PrefNames.FILE_DIR.toString(), null);		
	}
	
	public void setFileDir(String fileDir) {
		preferences.put(PrefNames.FILE_DIR.toString(), fileDir);
	}

	
	public ClassroomData getClassroom() {
		String id = preferences.get(PrefNames.CLASS_ID.toString(), null);
		if (id == null) {
			return null;
		}
		String name = preferences.get(PrefNames.CLASS_NAME.toString(), null);
		return new ClassroomData(name, id);
	}
	
	
	public void setClassroom(ClassroomData classroom) {
		if (classroom != null) {
			preferences.put(PrefNames.CLASS_NAME.toString(), classroom.getName());
			preferences.put(PrefNames.CLASS_ID.toString(), classroom.getId());
			makeDirs();
		}
	}
	
	public String getTokenDir() {
		return getWorkingDir() + File.separator + "tokens";
		
	}
	
	public String getClassroomDir() {
		ClassroomData currentCourse = getClassroom();
		if (currentCourse != null) {
			String classDirName = currentCourse.getName().replaceAll("\\s", "_");
			String classDir = getWorkingDir() + File.separator + classDirName;
			return classDir;
		}
		return null;
		
	}
	
	public String getJsonPath() {
		return getWorkingDir() + File.separator + "credentials.json";
		
	}
	
	private void makeDirs() {
		String directoryPath = getWorkingDir();
		String classroomDir = getClassroomDir();
		if (classroomDir != null) {			
			new File(classroomDir).mkdir();
		}
		String tokenDir = directoryPath + File.separator + "tokens";		
		new File(tokenDir).mkdir();		
	}

}
