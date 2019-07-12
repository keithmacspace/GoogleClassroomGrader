package net.cdonald.googleClassroom.control;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import net.cdonald.googleClassroom.googleClassroomInterface.AssignmentFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.DataFetchListener;
import net.cdonald.googleClassroom.googleClassroomInterface.FetchDoneListener;
import net.cdonald.googleClassroom.googleClassroomInterface.FileFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.GoogleClassroomCommunicator;
import net.cdonald.googleClassroom.googleClassroomInterface.SheetFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.StudentFetcher;
import net.cdonald.googleClassroom.gui.ConsoleDisplayListener;
import net.cdonald.googleClassroom.gui.DataStructureChangedListener;
import net.cdonald.googleClassroom.gui.DataUpdateListener;
import net.cdonald.googleClassroom.gui.MainGoogleClassroomFrame;
import net.cdonald.googleClassroom.gui.RecompileListener;
import net.cdonald.googleClassroom.gui.StudentListModel;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.ConsoleData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.MyPreferences;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.SQLDataBase;
import net.cdonald.googleClassroom.model.StudentData;

public class DataController implements StudentListInfo, RecompileListener {
	private StudentWorkCompiler studentWorkCompiler;
	private ConsoleData consoleData;
	private ClassroomData currentCourse;
	private List<StudentData> studentData;
	private Rubric rubric;
	private DataStructureChangedListener structureListener;
	private DataUpdateListener updateListener;
	// Create these  to avoid data lock issues
	private SQLDataBase assignmentFilesDataBase;
	private SQLDataBase rubricDataBase;
	private SQLDataBase classDataBase;

	private StudentFetcher studentFetcher;

	public DataController(MainGoogleClassroomFrame mainFrame) {
		studentWorkCompiler = new StudentWorkCompiler(mainFrame);
		studentData = new ArrayList<StudentData>();
		consoleData = new ConsoleData();		
		currentCourse = null;
		structureListener = mainFrame;
		updateListener = mainFrame;
		classDataBase = new SQLDataBase();
		rubricDataBase = new SQLDataBase();
		assignmentFilesDataBase = new SQLDataBase();
		studentFetcher = null;
	}
	
	public void addConsoleListener(ConsoleDisplayListener listener) {
		consoleData.addListener(listener);
		listener.setRecompileListener(this);
	}


	public Rubric getRubric() {
		return rubric;
	}


	public void setRubric(Rubric rubric) {
		this.rubric = rubric;
		structureListener.dataStructureChanged();
	}
	

	public ClassroomData getCurrentCourse() {
		return currentCourse;
	}
	
	private void clearAllData() {
		classDataBase.disconnect();
		assignmentFilesDataBase.disconnect();
		studentData.clear();
		studentWorkCompiler.clearData();
		
	}


	public void setCurrentCourse(MyPreferences prefs, ClassroomData currentCourse) {
		clearAllData();		
		this.currentCourse = currentCourse;
		if (currentCourse != null) {
			String classDBName = prefs.getClassroomDir() + File.separator +  "classroom.db";
			String assignmentFilesDBName = prefs.getClassroomDir() + File.separator +  "files.db";
			try {
				classDataBase.connect(classDBName);
				assignmentFilesDataBase.connect(assignmentFilesDBName);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	public void closing() {
		consoleData.setDone();
		assignmentFilesDataBase.disconnect();
		classDataBase.disconnect();
		rubricDataBase.disconnect();
	}

	public List<FileData> getSourceCode(String id) {
		return studentWorkCompiler.getSourceCode(id);
	}
	
	public String getConsoleOutput(String id) {
		return consoleData.getConsoleOutput(id);
	}
	public String getConsoleInputHistory(String id) {
		return consoleData.getConsoleInputHistory(id);
	}
	
	
	public void run(String id) {
		if (studentWorkCompiler.isRunnable(id)) {
			List<FileData> fileDataList = getSourceCode(id);
			consoleData.runStarted(id, fileDataList);
			studentWorkCompiler.run(id);
		}
	}
	
	public void debugPrint() {
		consoleData.debugPrint();
	}
	
	public void assignmentSelected(ClassroomData data, GoogleClassroomCommunicator googleClassroom) {
		if (rubric != null) {
			rubric.clearStudentData();
		}
		if (studentWorkCompiler != null) {
			studentWorkCompiler.clearData();
			if (currentCourse != null) {
				FileFetcher fetcher = new FileFetcher(assignmentFilesDataBase, currentCourse, data, googleClassroom, new DataFetchListener() {
					@Override
					public void retrievedInfo(ClassroomData data) {
						FileData fileData = (FileData) data;
						studentWorkCompiler.addFile(fileData);
						updateListener.dataUpdated();

					}
					@Override
					public void remove(Set<String> ids) {
						studentWorkCompiler.removeFiles(ids);					
					}
				}, new FetchDoneListener() {

					@Override
					public void done() {
						studentWorkCompiler.compileAll();
					}
				});
				fetcher.execute();
			}
		}
	}
	
	public void runRubric(String studentId) {
		if (rubric != null) {
			CompilerMessage message = studentWorkCompiler.getCompilerMessage(studentId);
			if (message != null) {
				rubric.runAutomation(message);
				updateListener.dataUpdated();
			}			
		}
	}
	
	public void initAssignments(GoogleClassroomCommunicator googleClassroom, DataFetchListener listener) {
		if (currentCourse != null) {
			AssignmentFetcher fetcher = new AssignmentFetcher(classDataBase, currentCourse, googleClassroom, listener);
			fetcher.execute();
		}
	}
	
	public void initRubrics(MyPreferences prefs, String urlName, GoogleClassroomCommunicator googleClassroom, DataFetchListener listener) {
		String dbName = prefs.getWorkingDir() + File.separator +  "rubric.db";
		try {
			rubricDataBase.connect(dbName);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SheetFetcher fetcher = new SheetFetcher(rubricDataBase, GoogleSheetData.DB_NAME, urlName, googleClassroom, listener, null);
		fetcher.execute();
	}
	
	public void initStudents(MyPreferences prefs, GoogleClassroomCommunicator googleClassroom)  {
		if (currentCourse != null) {
			studentData.clear();
			DataFetchListener dataFetchListener = new DataFetchListener() {
				@Override
				public void retrievedInfo(ClassroomData data) {
					addStudent((StudentData) data);
				}

				@Override
				public void remove(Set<String> ids) {					
					for (String id : ids) {
						for (int i = studentData.size() - 1; i >= 0; i++) {
							if (studentData.get(i).getId().equals(id)) {
								studentData.remove(i);
								break;
							}
						}
					}					
				}

			};

			
			studentFetcher = new StudentFetcher(classDataBase, currentCourse, googleClassroom, dataFetchListener, new FetchDoneListener() {
				@Override
				public void done() {
					if (updateListener != null) {
						updateListener.dataUpdated();
					}
				}		
			});

			studentFetcher.execute();
		}
	}
	
	private void addStudent(StudentData student) {
		boolean inserted = false;
		// Student already in the list
		for (StudentData current : studentData) {
			if (current.getId().equals(student.getId())) {
				return;
			}
		}
		for (int i = 0; i < studentData.size(); i++) {
			StudentData other = studentData.get(i);
			if (other.compareTo(student) < 0) {
				studentData.add(i, student);
				inserted = true;
				break;
			}
		}
		if (inserted == false) {
			studentData.add(student);
		}
		updateListener.dataUpdated();
	}
	
	@Override
	public int getRowCount() {
		return studentData.size();
		
	}
	
	@Override
	public int getColumnCount() {
		int num = NUM_DEFAULT_COLUMNS;
		if (rubric != null) {
			num += rubric.getEntries().size();
		}
		return num;
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Object retVal = null;
		switch(columnIndex) {
		case LAST_NAME_COLUMN:
			retVal = studentData.get(rowIndex);
			break;
		case FIRST_NAME_COLUMN:
			retVal = studentData.get(rowIndex).getFirstName();
			break;
		case DATE_COLUMN:
			List<FileData> fileData = studentWorkCompiler.getSourceCode(getStudentId(rowIndex));
			if (fileData != null) {
				Date date = fileData.get(0).getDate();
				if (date != null) {
					retVal = date.toString();
				}
			}
			break;
		case COMPILER_COLUMN:
			CompilerMessage data = studentWorkCompiler.getCompilerMessage(getStudentId(rowIndex));
			if (data != null) {
				if (data.isSuccessful()) {
					retVal = "Y";
				}
				else {
					retVal = "N - " + data.getCompilerMessage();
				}
			}
			break;
		default:
			if (rubric != null) {
				retVal = rubric.getEntry(getRubricIndex(columnIndex)).getStudentValue(getStudentId(rowIndex));
			}
			break;
		}
		return retVal;
	}
	
	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if (value != null) {
			int index = getRubricIndex(columnIndex);
			if (index >= 0 && rubric != null) {
				rubric.getEntry(index).setStudentValue(getStudentId(rowIndex), (String)value);
			}
		}
	}
	
	@Override
	public String getColumnTip(int columnIndex) {
		if (columnIndex > NUM_DEFAULT_COLUMNS && rubric != null) {
			return rubric.getEntry(getRubricIndex(columnIndex)).getDescription();
		}
		return "";
	}
	
	private int getRubricIndex(int columnIndex) {
		return columnIndex - NUM_DEFAULT_COLUMNS;
	}
	
	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex < NUM_DEFAULT_COLUMNS) {
			return defaultColumnNames[columnIndex];
		}
		if (rubric != null) {
			return rubric.getEntry(getRubricIndex(columnIndex)).getColumnName();
		}
		return null;
	}
	

	public String getStudentId(int row) {
		return studentData.get(row).getId();
		
	}
	
	public List<List<Object>> getColumnValuesForSheet() {
		List<List<Object>> fullData = new ArrayList<List<Object>>();
		
		for (int col = 0; col < getColumnCount(); col++) {
			List<Object> innerData = new ArrayList<Object>();
			if (col < NUM_DEFAULT_COLUMNS) {
				innerData.add(defaultColumnNames[col]);
			}
			else {
				RubricEntry entry = rubric.getEntry(getRubricIndex(col));
				String temp = entry.getName() + "\nValue = " + entry.getValue();
				innerData.add(temp);
			}
			for (int row = 0; row < getRowCount(); row++) {
			
				if (col == LAST_NAME_COLUMN) {
					innerData.add(studentData.get(row).getName());
				}
				else {
					Object value = getValueAt(row, col);
					innerData.add(value);
				}
			}
			fullData.add(innerData);			
		}
		return fullData;
	}


	public void setOwner(StudentListModel owner) {
	//	this.owner = owner;
	}
	
	public List<String> getAllIDs() {
		List<String> ids = new ArrayList<String>();
		
		
		for (StudentData student : studentData) {
			ids.add(student.getId());
		}
		return ids;
	}
	
	public boolean loadTestFile(String fileName) {
		clearAllData();
	    FileData fileData = null;
		try {
			Path path = Paths.get(fileName);
			String text = new String(Files.readAllBytes(path));
			BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
			// Strip out the path
			String basicFileName = path.getFileName().toString();
			fileData = new FileData(basicFileName, text, "" + studentData.size(), attr.creationTime().toString());
			studentData.add(new StudentData(basicFileName, "testFile", "" + studentData.size(), null));
			studentWorkCompiler.addFile(fileData);
			studentWorkCompiler.compileAll();
			if (updateListener != null) {
				updateListener.dataUpdated();
			}

		}catch (IOException e) {
			return false;
		}
		return true;		
	}

	@Override
	public void recompileAndRun(FileData fileData, String text) {
		//System.err.println("here " + fileData + " " + fileData.getId());
		
		if (fileData != null) {
			fileData.setFileContents(text);
			List<FileData> temp = new ArrayList<FileData>();
			temp.add(fileData);
			consoleData.runStarted(fileData.getId(), temp);
			studentWorkCompiler.compileAndRun(this, fileData, text);
		}
		
	}
	@Override
	public void compileError(String text) {
		System.err.println(text);
	}
}
