package net.cdonald.googleClassroom.control;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import net.cdonald.googleClassroom.googleClassroomInterface.AssignmentFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.CourseFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.FileFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.GoogleClassroomCommunicator;
import net.cdonald.googleClassroom.googleClassroomInterface.SheetFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.StudentFetcher;
import net.cdonald.googleClassroom.gui.DataStructureChangedListener;
import net.cdonald.googleClassroom.gui.DataUpdateListener;
import net.cdonald.googleClassroom.gui.MainGoogleClassroomFrame;
import net.cdonald.googleClassroom.gui.StudentListModel;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.AddProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.AddRubricTabsListener;
import net.cdonald.googleClassroom.listenerCoordinator.AssignmentSelected;
import net.cdonald.googleClassroom.listenerCoordinator.ClassSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.EnableRunRubricQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCompilerMessageQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentClassQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentRubricURL;
import net.cdonald.googleClassroom.listenerCoordinator.GetDBNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetFileDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentFilesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetWorkingDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LoadTestFileListener;
import net.cdonald.googleClassroom.listenerCoordinator.LongQueryListener;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricFileSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.SaveRubricListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetFileDirListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetRunRubricEnableStateListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetRunningLabelListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetWorkingDirListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentListInfo;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.CompileErrorListener;
import net.cdonald.googleClassroom.model.ConsoleData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.MyPreferences;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.StudentData;


public class DataController implements StudentListInfo {
	private StudentWorkCompiler studentWorkCompiler;
	private ConsoleData consoleData;
	private ClassroomData currentCourse;
	private List<StudentData> studentData;
	private Map<String, StudentData> studentMap;
	private Rubric rubric;
	private DataStructureChangedListener structureListener;
	private DataUpdateListener updateListener;
	private GoogleClassroomCommunicator googleClassroom;
	private MyPreferences prefs;
	private ClassroomData rubricURL;
	

	public DataController(MainGoogleClassroomFrame mainFrame) {
		prefs = new MyPreferences();
		studentWorkCompiler = new StudentWorkCompiler(mainFrame);
		studentData = new ArrayList<StudentData>();
		studentMap = new HashMap<String, StudentData>();
		consoleData = new ConsoleData();		
		currentCourse = null;
		structureListener = mainFrame;
		updateListener = mainFrame;
		initGoogle();
		registerListeners();
	}
	
	private void initGoogle() {
		try {
			googleClassroom = new GoogleClassroomCommunicator(MainGoogleClassroomFrame.APP_NAME, prefs.getTokenDir(), prefs.getJsonPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void performFirstInit() {
		if (prefs.getClassroom() != null) {
			currentCourse = prefs.getClassroom();
			ListenerCoordinator.fire(ClassSelectedListener.class, currentCourse);
		}
		String rubricURLName = prefs.getRubricFile();
		if (rubricURLName != null) {
			ListenerCoordinator.fire(RubricFileSelectedListener.class, rubricURLName);
		}
	}
	
	private void registerListeners() {
		
		ListenerCoordinator.addLongQueryReponder(StudentFetcher.class, new StudentFetcher(googleClassroom));
		ListenerCoordinator.addLongQueryReponder(AssignmentFetcher.class, new AssignmentFetcher(googleClassroom));
		ListenerCoordinator.addLongQueryReponder(CourseFetcher.class, new CourseFetcher(googleClassroom));
		ListenerCoordinator.addLongQueryReponder(FileFetcher.class, new FileFetcher(googleClassroom));
		ListenerCoordinator.addLongQueryReponder(SheetFetcher.class, new SheetFetcher(googleClassroom));
		
		ListenerCoordinator.addListener(ClassSelectedListener.class, new ClassSelectedListener() {

			@Override
			public void fired(ClassroomData course) {
				setCurrentCourse(course);				
			}
			
		});
		
		ListenerCoordinator.addListener(AssignmentSelected.class, new AssignmentSelected() {
			@Override
			public void fired(ClassroomData data) {	
				studentWorkCompiler.clearData();
				ListenerCoordinator.runLongQuery(FileFetcher.class, new LongQueryListener<ClassroomData>() {
					@Override
					public void process(List<ClassroomData> list) {
						for (ClassroomData data : list) {
							FileData fileData = (FileData) data;
							studentWorkCompiler.addFile(fileData);
							updateListener.dataUpdated();
						}
					}
					@Override
					public void done() {						
						studentWorkCompiler.compileAll();						
					}					
				});
			}			
		});
		

		ListenerCoordinator.addListener(CompileErrorListener.class, new CompileErrorListener() {
			@Override
			public void fired(String text) {
				System.err.println("Compile error caught " + text);				
			}			
		});
		
		ListenerCoordinator.addQueryResponder(GetCurrentClassQuery.class, new GetCurrentClassQuery() {
			@Override
			public ClassroomData fired() {
				return currentCourse;
			}
		});	
		
		ListenerCoordinator.addQueryResponder(GetFileDirQuery.class, new GetFileDirQuery() {
			@Override
			public String fired() {
				return prefs.getFileDir();
			}
		});	
		
		ListenerCoordinator.addQueryResponder(GetDBNameQuery.class, new GetDBNameQuery() {
			@Override
			public String fired(DBType type) {
				switch (type) {
				case ASSIGNMENT_FILES_DB:
					return prefs.getClassroomDir() + File.separator +  "files.db";
				case CLASS_DB:
					return prefs.getClassroomDir() + File.separator +  "class.db";
				case RUBRIC_DB:
					return prefs.getWorkingDir() + File.separator + "rubric.db";
				case STUDENT_DB:
					return prefs.getClassroomDir() + File.separator  + "student.db";
				default:				
				}
				// TODO Auto-generated method stub
				return null;
			}
		});
		
		ListenerCoordinator.addQueryResponder(GetCompilerMessageQuery.class, new GetCompilerMessageQuery() {
			@Override
			public CompilerMessage fired(String studentId) {
				return studentWorkCompiler.getCompilerMessage(studentId);
			}
		});

		ListenerCoordinator.addQueryResponder(GetWorkingDirQuery.class, new GetWorkingDirQuery() {

			@Override
			public String fired() {
				return prefs.getWorkingDir();
			}
			
		});
		
		ListenerCoordinator.addQueryResponder(EnableRunRubricQuery.class, new EnableRunRubricQuery() {
			@Override
			public Boolean fired() {
				return (Boolean)(rubric != null);
			}
			
		});
		
		ListenerCoordinator.addQueryResponder(GetStudentFilesQuery.class, new GetStudentFilesQuery() {
			@Override
			public List<FileData> fired(String studentID) {
				// TODO Auto-generated method stub
				return studentWorkCompiler.getSourceCode(studentID);
			}			
		});
		
		ListenerCoordinator.addQueryResponder(GetCurrentRubricURL.class, new GetCurrentRubricURL() {
			@Override
			public String fired() {
				return rubricURL.getName();
			}
		});
		
		ListenerCoordinator.addListener(SetWorkingDirListener.class,  new SetWorkingDirListener() {

			@Override
			public void fired(String workingDir) {
				prefs.setWorkingDir(workingDir);				
			}			
		});
		
		ListenerCoordinator.addListener(SetFileDirListener.class, new SetFileDirListener() {
			@Override
			public void fired(String workingDir) {
				prefs.setFileDir(workingDir);				
			}			

		});
			
		ListenerCoordinator.addListener(LoadTestFileListener.class, new LoadTestFileListener() {
			@Override
			public void fired(String file) {
				loadTestFile(file);				
			}			
		});
		
		ListenerCoordinator.addListener(RubricFileSelectedListener.class, new RubricFileSelectedListener() {
			@Override
			public void fired(String text) {
				if (rubricURL == null || !rubricURL.getName().equals(text)) {
					String urlID = googleClassroom.googleSheetID(text);
					rubricURL = new ClassroomData(text, urlID);
					prefs.setRubricFile(text);
				}
			}
		});
		
		ListenerCoordinator.addListener(RubricSelected.class, new RubricSelected() {
			@Override
			public void fired(GoogleSheetData data) {
				if (data != null && data.isEmpty() == false) {
					if (rubric == null || rubric.getName().equals(data.getName()) == false) {
						rubric = new Rubric(data);
						try {
							ListenerCoordinator.fire(AddProgressBarListener.class, "Loading Rubric");
							googleClassroom.fillRubric(rubric);
							ListenerCoordinator.fire(RemoveProgressBarListener.class, "Loading Rubric");
						} catch (IOException e) {
							JOptionPane.showMessageDialog(null, e.getMessage(), "Error accessing rubric db sheet",
									JOptionPane.ERROR_MESSAGE);
							e.printStackTrace();
							rubric = null;
						}

						if (setRubric(rubric) == JOptionPane.YES_OPTION) {
							saveRubric();
						}
					}
				
				}
				else {
					rubric = null;
				}

				ListenerCoordinator.fire(SetRunRubricEnableStateListener.class, (Boolean)(rubric != null));
				updateListener.dataUpdated();				
			}
		});	
		
		ListenerCoordinator.addListener(SaveRubricListener.class, new SaveRubricListener() {
			@Override
			public void fired() {
				saveRubric();

			}			
		});
		

		
		
	}

	public Rubric getRubric() {
		return rubric;
	}

	public int setRubric(Rubric rubric) {
		// The name check is for when we do a restore after cancel
		if (this.rubric != null && this.rubric.isInModifiedState() && rubric != this.rubric && rubric.getName() != this.rubric.getName()) {
			int dialogResult = JOptionPane.showConfirmDialog(null, "Do you want to save the current rubric before changing this one (cancel means do not change rubric)?", "Current Rubric Modified", JOptionPane.YES_NO_CANCEL_OPTION);
			if (dialogResult != JOptionPane.NO_OPTION) {
				return dialogResult;
			}
		}
		this.rubric = rubric;
		structureListener.dataStructureChanged();
		ListenerCoordinator.fire(AddRubricTabsListener.class, rubric);
		return JOptionPane.NO_OPTION;
	}
	

	public ClassroomData getCurrentCourse() {
		return currentCourse;
	}
	
	private void clearAllData() {
		studentData.clear();
		studentMap.clear();
		studentWorkCompiler.clearData();		
	}


	private void setCurrentCourse(ClassroomData currentCourse) {
		this.currentCourse = currentCourse;
		clearAllData();
		initStudents();
	}


	public void closing() {
		consoleData.setDone();
	}

	public List<FileData> getSourceCode(String id) {
		return studentWorkCompiler.getSourceCode(id);
	}
	
	
	
	public void run(String id) {

		if (studentWorkCompiler.isRunnable(id)) {
			consoleData.runStarted(id, "");
			StudentData student = studentMap.get(id);
			if (student != null) {
				ListenerCoordinator.fire(SetRunningLabelListener.class, "Running: " + student.getFirstName() + " " + student.getName());
			}
			studentWorkCompiler.run(id);
			ListenerCoordinator.fire(SetRunningLabelListener.class, "");
		}
	}
	
	public void debugPrint() {
		consoleData.debugPrint();
	}
	
	
	public void runRubric(String studentId) {
		if (rubric != null) {
			CompilerMessage message = studentWorkCompiler.getCompilerMessage(studentId);
			if (message != null) {
				StudentData student = studentMap.get(studentId);
				String studentName = "";
				if (student != null) {
					studentName = student.getFirstName() + " " + student.getName();
				}
				rubric.runAutomation(updateListener, studentName, message, studentWorkCompiler, consoleData);
				updateListener.dataUpdated();
			}			
		}
		ListenerCoordinator.fire(SetRunningLabelListener.class, "");
	}
	
	
	public void initStudents()  {
		if (currentCourse != null) {
			studentData.clear();
			studentMap.clear();
			ListenerCoordinator.runLongQuery(StudentFetcher.class,  new LongQueryListener<ClassroomData>() {

				@Override
				public void process(List<ClassroomData> list) {
					for (ClassroomData data : list) {
						addStudent((StudentData) data);
						
					}					
				}
				
			});
		}
	}
	
	private void addStudent(StudentData student) {
		boolean inserted = false;
		// Student already in the list
		if (studentMap.containsKey(student.getId())) {
			return;
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
		studentMap.put(student.getId(), student);
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
					retVal = "N";
				}
			}
			break;
		case TOTAL_COLUMN:
			if (rubric != null) {
				return rubric.getTotalCount(getStudentId(rowIndex));				
			}
			break;			
		default:
			if (rubric != null) {
				RubricEntry entry = rubric.getEntry(getRubricIndex(columnIndex));
				if (entry != null) {
					retVal = entry.getStudentValue(getStudentId(rowIndex));
				}
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


	public String getRubricURL() {
		return rubricURL.getName();
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
	
	public void saveRubric() {
		if (rubric != null) {
			try {
				ListenerCoordinator.fire(AddProgressBarListener.class, "Saving Rubric");
				googleClassroom.writeSheet(rubric);				
			}
			catch(IOException e) {
				JOptionPane.showMessageDialog(null, e.getMessage(), "Error saving to rubric db sheet",
						JOptionPane.ERROR_MESSAGE);
				
			}
			ListenerCoordinator.fire(RemoveProgressBarListener.class, "Saving Rubric");
		}
	}
	
	public Rubric newRubric(String name) {
		GoogleSheetData sheetData = new GoogleSheetData(name, rubricURL.getId(), "NewRubric");
		rubric = new Rubric(sheetData);
		return rubric;
	}

}
