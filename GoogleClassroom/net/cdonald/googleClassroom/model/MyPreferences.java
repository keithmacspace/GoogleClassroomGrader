package net.cdonald.googleClassroom.model;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import net.cdonald.googleClassroom.gui.DebugLogDialog;

/**
 * 
 * Just a small class to keep the preferences - keeps the names stable.
 *
 */
public class MyPreferences {
	// We store a few global preferences in java's prefs, but the rest we store
	// for individual classes in a per-class db
	private Preferences preferences;
	private enum PrefNames {WORKING_DIR, CLASS_ID, CLASS_NAME, FILE_DIR, GRADED_BY_NAME};
	private enum DBPrefNames {CLASS_ID, RUBRIC_URL, RUBRIC_FILE, GRADE_URL, GRADE_FILE};
	private Map<String, String> dbPrefs;
	private String classDBName;
	private SQLDataBase prefsDB;
	
	public MyPreferences() {
		preferences = Preferences.userNodeForPackage(net.cdonald.googleClassroom.gui.MainGoogleClassroomFrame.class);
		prefsDB = new SQLDataBase();
	}	
	
	public String getRubricFile() {
		return getDBPref(DBPrefNames.RUBRIC_FILE);		
	}
	

	public String getRubricURL() {
		return getDBPref(DBPrefNames.RUBRIC_URL);
	}
	
	public void setRubricInfo(String rubricName, String rubricURL) {
		setDBPref(DBPrefNames.RUBRIC_FILE, rubricName);
		setDBPref(DBPrefNames.RUBRIC_URL, rubricURL);
		saveClassPrefs();
	}
	
	public String getGradeFile() {
		return getDBPref(DBPrefNames.GRADE_FILE);		
	}
	
	public String getGradeURL() {
		return getDBPref(DBPrefNames.GRADE_URL);
	}
	
	public void setGradeInfo(String gradeFile, String gradeURL) {
		setDBPref(DBPrefNames.GRADE_FILE, gradeFile);
		setDBPref(DBPrefNames.GRADE_URL, gradeURL);
		saveClassPrefs();
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

	public String getGradedByName() {
		return preferences.get(PrefNames.GRADED_BY_NAME.toString(), System.getProperty("user.name"));
	}
	
	private void setGradedByName(String name) {
		preferences.put(PrefNames.GRADED_BY_NAME.toString(), name);
	}
	
	public ClassroomData getClassroom() {
		String id = preferences.get(PrefNames.CLASS_ID.toString(), null);
		if (id == null) {
			return null;
		}
		String name = preferences.get(PrefNames.CLASS_NAME.toString(), null);
		if (dbPrefs == null) {
			makeDirs(getClassroomDir(id));
			loadClassPrefs(id);
		}
		return new ClassroomData(name, id);
	}
	
	
	public void setClassroom(ClassroomData classroom) {
		if (classroom != null) {
			preferences.put(PrefNames.CLASS_NAME.toString(), classroom.getName());
			preferences.put(PrefNames.CLASS_ID.toString(), classroom.getId());
			makeDirs(getClassroomDir(classroom.getId()));
			loadClassPrefs(classroom.getId());			
		}		
	}
	
	private String getDBPref(DBPrefNames name) {
		if (dbPrefs != null) {
			return dbPrefs.get(name.toString());
		}
		return null;
	}
	
	private void setDBPref(DBPrefNames name, String value) {
		if (dbPrefs != null) {
			dbPrefs.put(name.toString(), value);
		}
	}
	
	private void saveClassPrefs() {
		openClassPrefs();
		try {
			prefsDB.simpleSave("CLASS_PREFS", DBPrefNames.class, dbPrefs);			
		} catch (SQLException e) {
			SQLErrorMessage(e, "Cannot save to");
		}
		prefsDB.disconnect();
		
	}
	
	private void SQLErrorMessage(SQLException e, String message) {
		JOptionPane.showMessageDialog(null, message + " prefs DB " + classDBName + " because " + e.getMessage(),  "Cannot Accesss Preferences",
				JOptionPane.ERROR_MESSAGE);		
	}
	
	private void loadClassPrefs(String classID) {
		openClassPrefs();
		try {
			dbPrefs = prefsDB.loadTableEntry("CLASS_PREFS", DBPrefNames.class, DBPrefNames.CLASS_ID.toString(), classID);			
		} catch (SQLException e) {
			// The first time we access the DB, it will be empty for this id, so start saving the data
			dbPrefs = new HashMap<String, String>();
			dbPrefs.put(DBPrefNames.CLASS_ID.toString(), classID);
		}
		prefsDB.disconnect();
	}
	
	private void openClassPrefs() {		
		try {
			prefsDB.connect(classDBName);
		} catch (SQLException e) {
			SQLErrorMessage(e, "Cannot load");
			prefsDB = null;
		}
	}
	
	public String getTokenDir() {
		return getWorkingDir() + File.separator + "tokens";
		
	}
	
	public String getClassroomDir() {
		ClassroomData currentCourse = getClassroom();
		if (currentCourse != null) {
			return getClassroomDir(currentCourse.getId());			
		}
		return null;		
	}

	public String getClassroomDir(String classID) {
		String classDirName = classID.replaceAll("\\s", "_");
		String classDir = getWorkingDir() + File.separator + classDirName;
		return classDir;
	}
	
	public String getJsonPath() {
		return getWorkingDir() + File.separator + "credentials.json";
		
	}
	
	private void makeDirs() {		
		makeDirs(getClassroomDir());
	}
	
	private void makeDirs(String classroomDir) {
		String directoryPath = getWorkingDir();
		DebugLogDialog.appendln("Creating dirs" + directoryPath + " " + classroomDir);
		if (classroomDir != null) {			
			new File(classroomDir).mkdir();
		}
		String tokenDir = directoryPath + File.separator + "tokens";
		classDBName = directoryPath + File.separator + "prefs.db";
		new File(tokenDir).mkdir();		
	}
	

}
