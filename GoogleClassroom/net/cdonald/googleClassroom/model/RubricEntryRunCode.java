package net.cdonald.googleClassroom.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SetInfoLabelListener;


public class RubricEntryRunCode extends  RubricAutomation {
	private String methodToCall;
	private List<FileData> sourceFiles;
	private List<String> studentBaseClassNames;
	boolean checkSystemOut;		
	private enum MethodNames {METHOD_TO_CALL, CLASS_NAMES_TO_REPLACE, SOURCE_FILE};


	
	public RubricEntryRunCode() {		
		sourceFiles = new ArrayList<FileData>();
		studentBaseClassNames = new ArrayList<String>();
	}
	
	public RubricEntryRunCode(RubricEntryRunCode other) {
		methodToCall = other.methodToCall;
		sourceFiles = new ArrayList<FileData>();
		studentBaseClassNames = new ArrayList<String>();
		for (FileData fileData : other.sourceFiles) {
			sourceFiles.add(fileData);
		}
		for (String className : other.studentBaseClassNames) {
			studentBaseClassNames.add(className);
		}
		checkSystemOut = other.checkSystemOut;
	}
	
	public RubricAutomation newCopy() {
		return new RubricEntryRunCode(this);
	}


	public void addSourceContents(FileData file) {
		file.setRubricCode(true);
		if (containsSource(file) == false) {
			this.sourceFiles.add(file);
		}

	}
	
	
	@Override
	public void removeFileData(FileData fileData) {

		for (int i = 0; i < sourceFiles.size(); i++) {
			if (sourceFiles.get(i).getName().equals(fileData.getName())) {
				sourceFiles.remove(i);
				break;
			}
		}
	}
	
	public boolean containsSource(FileData file) {
		for (FileData current : sourceFiles) {
			if (current.getName().equals(file.getName())) {
				return true;
			}
		}
		return false;
		
	}
	
	public List<Method> getPossibleMethods(String studentId, StudentWorkCompiler compiler) {

		if (studentId == null) {
			return null;
		}
		List<FileData> studentFiles = compiler.getSourceCode(studentId);

		if (studentFiles == null) {
			return null;
		}
		
		studentBaseClassNames.clear();		
		for (FileData fileData : studentFiles) {
			studentBaseClassNames.add(fileData.getClassName());
		}
		
		List<FileData> rubricFiles = new ArrayList<FileData>(studentFiles);
		

		for (FileData sourceFile : sourceFiles) {
			rubricFiles.add(sourceFile);
			
		}
		
		Map<String, Class<?>> compiled = null;
		try {

			compiled = compiler.compile(rubricFiles);
		} catch (Exception e) {
			return null;
		}
		List<Method> methods = new ArrayList<Method>();
		for (FileData sourceFile : sourceFiles) {
			Class<?> aClass = compiled.get(sourceFile.getClassName());
			for (Method method : aClass.getMethods()) {

				boolean validReturn = (method.getReturnType() == double.class || method.getReturnType() == Double.class);
				if (method.getParameterCount() == 0 && validReturn) {
					methods.add(method);
				}
			}
		}
		return methods;
	}
	
	protected Double runAutomation_(String studentName, CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData) {
		if (message.isSuccessful()) {	
			ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "Running " + this.getOwnerName() + " for " + studentName);
			String studentId = message.getStudentId();			
				
			List<FileData> studentFiles = compiler.getSourceCode(studentId);
			List<FileData> rubricFiles = new ArrayList<FileData>(studentFiles);
			consoleData.runStarted(studentId, getOwnerName());				
			prepareForNextTest();				
			for (FileData sourceFile : sourceFiles) {
				String modifiedSource = replaceClassNames(sourceFile.getFileContents(), studentFiles.get(0).getClassName());
				FileData temp = new FileData(sourceFile.getName(), modifiedSource, studentId, null);
				rubricFiles.add(temp);				
			}
			Class<?> []params = {};
			Object []args = {};
			Object returnValue = null;
			try {					
				returnValue = compiler.compileAndRun(true,  rubricFiles, methodToCall, params, args);				
			}
			catch (Exception e) {
				ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
				addOutput(studentId, e.getMessage());					
				System.out.println("\0");					
				return null;
			}
			if (returnValue == null) {
				return null;
			}
			double value = 0.0;
			if (returnValue != null) {
				value = Double.parseDouble(returnValue.toString());
			}				
			waitForTestFinish();

			ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
			return value;
		}
		
		return null;
	}
	
	String replaceClassNames(String sourceContents, String studentClassName) {
		String fixedContents = sourceContents;
		for (String baseClassName : studentBaseClassNames) {
			// If they match perfectly, there is no change needed
			if (!studentClassName.contentEquals(baseClassName)) {
				int methodIndex = 0;
				int priorIndex = 0;
				while (methodIndex != -1) {
					methodIndex = fixedContents.indexOf(baseClassName, priorIndex);
					boolean replaceValid = true;
					if (methodIndex != -1) {
						// The start edge should be a whitespace
						if (methodIndex != 0) {
							char check = fixedContents.charAt(methodIndex - 1);
							if (!Character.isWhitespace(check) && check != ';') {
								replaceValid = false;
							}
						}
						int endIndex = methodIndex + baseClassName.length(); 
						if (endIndex < fixedContents.length()) {
							char check = fixedContents.charAt(endIndex);
							if (check != '(' && !Character.isWhitespace(check) && check != '.') {
								replaceValid = false;
							}
						}
						if (replaceValid == true) {
							String endCode = fixedContents.substring(endIndex);
							fixedContents = fixedContents.substring(0, methodIndex);
							fixedContents += studentClassName;				
							priorIndex = fixedContents.length();
							fixedContents += endCode;
						}
						else {
							priorIndex = methodIndex + 1;
						}
					}
				}
			}
		}
		return fixedContents;
	}
	
	public String getMethodToCall() {
		return methodToCall;
	}


	public void setMethodToCall(String methodToCall) {
		this.methodToCall = methodToCall;
	}




	public List<FileData> getSourceFiles() {
		return sourceFiles;
	}


	@Override
	public int getNumAutomationColumns() {		
		return 2;
	}
	
	@Override
	protected void saveAutomationColumns(String entryName, List<List<Object>> columnData, Map<String, List<Object>> fileData) {
		List<Object> labels = new ArrayList<Object>();
		List<Object> content = new ArrayList<Object>();
		labels.add(entryName);
		content.add(entryName);
		labels.add(MethodNames.CLASS_NAMES_TO_REPLACE.toString());
		String classes = "";
		for (int i = 0; i < studentBaseClassNames.size() - 1; i++) {
			classes += studentBaseClassNames.get(i) + ",";
		}
		if (studentBaseClassNames.size() != 0) {
			classes += studentBaseClassNames.get(studentBaseClassNames.size() - 1);
		}
		content.add(classes);
		labels.add(MethodNames.METHOD_TO_CALL.toString());
		content.add(methodToCall);
		columnData.add(labels);
		columnData.add(content);
		String sourceFileNames = "";
		
		for (int i = 0; i < sourceFiles.size() - 1; i++) {
			sourceFileNames += sourceFiles.get(i).getName() + ",";
		}
		if (sourceFiles.size() != 0) {
			sourceFileNames += sourceFiles.get(sourceFiles.size() - 1).getName();
		}
		labels.add(MethodNames.SOURCE_FILE.toString());
		content.add(sourceFileNames);
		for (FileData file : sourceFiles) {
			if (fileData.containsKey(file.getName()) == false) {
				List<Object> fileLineList = new ArrayList<Object>();
				fileLineList.add(file.getName()); // Column header
				String[] fileLineArray = file.getFileContents().split("\n");
				int rowCount = 0;
				int lineIndex = 0;
				while(lineIndex < fileLineArray.length) {
					// Start putting multiple lines on a single row.
					if (rowCount > 200) {
						String nextLine = "";
						while(nextLine.length() < 200 && lineIndex < fileLineArray.length) {
							nextLine += fileLineArray[lineIndex];
							if (nextLine.length() < 199) {
								nextLine += "\n";
							}
							lineIndex++;
						}
						fileLineList.add(nextLine);
					}
					else {
						fileLineList.add(fileLineArray[lineIndex]);
						lineIndex++;	
					}
					rowCount++;					
				}
				fileData.put(file.getName(), fileLineList);
			}
		}
	}

	private boolean showedErrorMessage = false;
	private void showErrorMessage(String entryName) {
		if (showedErrorMessage == false) {
			JOptionPane.showMessageDialog(null, "The rubric component \"" + entryName + "\" is missing run data. Edit the rubric before running.", "Rubric Incomplete - Finish Editing",
				JOptionPane.ERROR_MESSAGE);
			showedErrorMessage = true;			
		}
	}
	private List<String> breakUpCommaList(Object object) {
		List<String> partsList = new ArrayList<String>();
		if (object instanceof String) {
			String [] parts = ((String)object).split(",");
			for (String part : parts) {
				partsList.add(part.strip());
			}			
		}
		return partsList;
		
	}

	@Override
	protected void loadAutomationColumns(String entryName, Map<String, List<List<Object>>> columnData, Map<String, FileData> fileDataMap) {
		List<List<Object> > columns = columnData.get(entryName.toUpperCase());
		if (columns == null || columns.size() != getNumAutomationColumns()) {
			showErrorMessage(entryName);
			return;
		}
		else {
			List<String> files = null;
			studentBaseClassNames.clear();

			List<Object> labelRow = columns.get(0);
			methodToCall = null;
			for (int row = 0; row < labelRow.size(); row++) {
				String label = (String)labelRow.get(row);
				if (label != null) { 
					if (label.equalsIgnoreCase(MethodNames.METHOD_TO_CALL.toString())) {
						methodToCall = (String)columns.get(1).get(row);
					}
					else if (label.equalsIgnoreCase(MethodNames.SOURCE_FILE.toString())) {
						files = breakUpCommaList(columns.get(1).get(row));
					}
					else if (label.equalsIgnoreCase(MethodNames.CLASS_NAMES_TO_REPLACE.toString())) {
						studentBaseClassNames = breakUpCommaList(columns.get(1).get(row));
					}
				}
			}
			if (files == null || files.size() == 0 ||  studentBaseClassNames.size() == 0 || methodToCall == null) {
				showErrorMessage(entryName);							
			}
			sourceFiles = new ArrayList<FileData>();
			for (Object fileO : files) {
				if (fileO instanceof String) {
					String file = (String)fileO;
					// We want them all sharing the same source so that when we edit the
					// source in the rubric, all of them see the edit
					if (fileDataMap.containsKey(file)) {
						sourceFiles.add(fileDataMap.get(file));
					}
					else {
						List<List<Object>> sourceInfo = columnData.get(file.toUpperCase());
						if (sourceInfo == null || sourceInfo.size() == 0) {
							showErrorMessage(entryName);
						}
						else {
							String text = "";
							boolean firstLine = true;
							List<Object> lines = sourceInfo.get(0);
							for (Object line : lines) {
								if (line != null) {
									if (firstLine == false) {
										text += line + "\n";
									}							
									firstLine = false;
								}					
							}
							FileData fileData = new FileData(file, text, "0", null); 
							sourceFiles.add(fileData);
							fileDataMap.put(file, fileData);
						}
					}
				}
			}
		}
	}

}
