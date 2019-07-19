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
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

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
import net.cdonald.googleClassroom.listenerCoordinator.AssignmentSelected;
import net.cdonald.googleClassroom.listenerCoordinator.ClassSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.EnableRunRubricQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentClassQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetDBNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetFileDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetRubricOutputQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentFilesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetWorkingDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LoadTestFileListener;
import net.cdonald.googleClassroom.listenerCoordinator.LongQueryListener;
import net.cdonald.googleClassroom.listenerCoordinator.RecompileListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricFileSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.SetFileDirListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetRunRubricEnableStateListener;
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
import net.cdonald.googleClassroom.model.RubricEntryRunCode;
import net.cdonald.googleClassroom.model.StudentData;


public class DataController implements StudentListInfo {
	private StudentWorkCompiler studentWorkCompiler;
	private ConsoleData consoleData;
	private ClassroomData currentCourse;
	private List<StudentData> studentData;
	private Rubric rubric;
	private DataStructureChangedListener structureListener;
	private DataUpdateListener updateListener;
	private GoogleClassroomCommunicator googleClassroom;
	private MyPreferences prefs;
	private final String TEMP_URL = "https://drive.google.com/open?id=1EYN9SBQWd9gqz5eK7UDy_qQY0B1529f6r-aOw8n2Oyk";

	public DataController(MainGoogleClassroomFrame mainFrame) {
		prefs = new MyPreferences();
		studentWorkCompiler = new StudentWorkCompiler(mainFrame);
		studentData = new ArrayList<StudentData>();
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
		ListenerCoordinator.fire(RubricFileSelectedListener.class, TEMP_URL);
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
		
		ListenerCoordinator.addListener(RecompileListener.class, new RecompileListener() {
			@Override
			public void fired(FileData fileData, String text) {
				if (fileData != null) {
					fileData.setFileContents(text);
					List<FileData> temp = new ArrayList<FileData>();
					temp.add(fileData);
					consoleData.runStarted(fileData.getId(), temp);
					//studentWorkCompiler.compileAndRun(this, fileData, text);
				}
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
		
		ListenerCoordinator.addQueryResponder(GetRubricOutputQuery.class, new GetRubricOutputQuery() {
			@Override
			public String fired(String rubricName, String studentID) {
				if (rubric != null) {
					return rubric.getRubricOutput(rubricName, studentID);
				}
				return null;
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
		
		ListenerCoordinator.addListener(RubricFileSelectedListener.class, new RubricFileSelectedListener() {
			@Override
			public void fired(String url) {
				
			}
		});
		
		ListenerCoordinator.addListener(LoadTestFileListener.class, new LoadTestFileListener() {
			@Override
			public void fired(String file) {
				loadTestFile(file);				
			}			
		});
		
		ListenerCoordinator.addListener(RubricSelected.class, new RubricSelected() {
			@Override
			public void fired(GoogleSheetData data) {
				rubric = null;
				if (data != null && data.isEmpty() == false) {
					rubric = new Rubric(data);
					try {
						googleClassroom.fillRubric(rubric);
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
				ListenerCoordinator.fire(SetRunRubricEnableStateListener.class, (Boolean)(rubric != null));
				updateListener.dataUpdated();
				
			}
		});		
		

		
		
	}

	public Rubric getRubric() {
		return rubric;
	}


	public int setRubric(Rubric rubric) {
		if (this.rubric != null && this.rubric.isInModifiedState() && rubric != this.rubric) {
			int dialogResult = JOptionPane.showConfirmDialog(null, "Do you want to save the current rubric before changing this one (cancel means do not change rubric)?", "Current Rubric Modified", JOptionPane.YES_NO_CANCEL_OPTION);
			if (dialogResult != JOptionPane.NO_OPTION) {
				return dialogResult;
			}
		}
		this.rubric = rubric;
		structureListener.dataStructureChanged();
		return JOptionPane.NO_OPTION;
	}
	

	public ClassroomData getCurrentCourse() {
		return currentCourse;
	}
	
	private void clearAllData() {
		studentData.clear();
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
	
	
	public void runRubric(String studentId) {
		if (rubric != null) {
			CompilerMessage message = studentWorkCompiler.getCompilerMessage(studentId);
			if (message != null) {
				SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
					@Override
					protected Void doInBackground() throws Exception {
						rubric.runAutomation(message, studentWorkCompiler);
						updateListener.dataUpdated();
						return null;
					}					
				};
				worker.execute();
			}			
		}
	}
	
	
	public void initStudents()  {
		if (currentCourse != null) {
			studentData.clear();
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
	
	private void saveRubric() {
		
	}

}
