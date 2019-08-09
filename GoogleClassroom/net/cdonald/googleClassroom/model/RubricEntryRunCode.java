package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import net.cdonald.googleClassroom.gui.UpdateSourceInterface;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SetInfoLabelListener;


public class RubricEntryRunCode extends  RubricAutomation implements UpdateSourceInterface{
	private String methodToCall;
	private List<FileData> sourceFiles;
	private String methodBeingChecked;
	boolean checkSystemOut;		
	private enum MethodNames {METHOD_TO_CALL, METHOD_BEING_CHECKED, SOURCE_FILE};


	
	public RubricEntryRunCode() {		
		sourceFiles = new ArrayList<FileData>();
	}
	
	public RubricEntryRunCode(RubricEntryRunCode other) {
		methodToCall = other.methodToCall;
		sourceFiles = new ArrayList<FileData>();
		for (FileData fileData : other.sourceFiles) {
			sourceFiles.add(fileData);
		}
		methodBeingChecked = other.methodBeingChecked;
		checkSystemOut = other.checkSystemOut;
	}
	
	public RubricAutomation newCopy() {
		return new RubricEntryRunCode(this);
	}


	public void setMethodBeingChecked(String methodBeingChecked) {
		this.methodBeingChecked = methodBeingChecked;
	}


	public void addSourceContents(FileData file) {
		file.setRubricCode(true);
		this.sourceFiles.add(file);		
	}
	
	public void removeSourceContents(String fileName) {
		for (int i = 0; i < sourceFiles.size(); i++) {
			if (sourceFiles.get(i).getName().equals(fileName)) {
				sourceFiles.remove(i);
				break;
			}
		}		
	}
	
	@Override
	public void updateSource(String fileName, String fileText) {

		for (FileData sourceFile : sourceFiles) {
			if (sourceFile.getName().equals(fileName)){
				sourceFile.setFileContents(fileText);
				break;
			}
		}
	}
	
	protected Double runAutomation_(String studentName, CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData) {
		if (message.isSuccessful()) {	
			ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "Running " + this.getOwnerName() + " for " + studentName);
			String studentId = message.getStudentId();			
			String completeMethodName = compiler.getCompleteMethodName(studentId, methodBeingChecked);
			if (completeMethodName == null) {
				String error =  "no method named " + methodBeingChecked + " in source";
				addOutput(studentId, error);
				ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
				System.out.println("\0");
			}			
			else {
				
				List<FileData> studentFiles = compiler.getSourceCode(studentId);
				List<FileData> rubricFiles = new ArrayList<FileData>(studentFiles);
				consoleData.runStarted(studentId, getOwnerName());				
				prepareForNextTest();				
				for (FileData sourceFile : sourceFiles) {
					String modifiedSource = replaceMethodName(completeMethodName, sourceFile.getFileContents());
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
		}
		return null;
	}
	
	String replaceMethodName(String completeMethodName, String sourceContents) {		
		String search = methodBeingChecked + "(";
		int methodIndex = 0;
		int priorIndex = 0;
		String fixedContents = sourceContents;
		while (methodIndex != -1) {
			methodIndex = fixedContents.indexOf(methodBeingChecked, priorIndex);						
			if (methodIndex != -1) {
				int replaceIndex = methodIndex;
				boolean replaceOK = true;
				if (methodIndex != 0) {
					char check = fixedContents.charAt(replaceIndex);
					while (replaceIndex >= 0 && replaceOK == true && Character.isWhitespace(check) == false) {
						if (Character.isLetterOrDigit(check) == false) {
							if (check != '_' && check != '.') {
								replaceOK = false;
							}
						}
						replaceIndex--;
						if (replaceIndex >= 0) {
							check = fixedContents.charAt(replaceIndex);
						}
					}				
				}
				if (replaceOK == true) {
					String endCode = fixedContents.substring(methodIndex + methodBeingChecked.length());
					fixedContents = fixedContents.substring(0, replaceIndex);
					fixedContents += completeMethodName;				
					priorIndex = fixedContents.length();
					fixedContents += endCode;
				}
				else {
					priorIndex = methodIndex + 1;
				}
				

			}
		}
		return fixedContents;
	}
	
//	public static RubricEntry createTest() {
//
//		RubricEntryRunCode test = new RubricEntryRunCode();
//		String file = "public class TestAdd {\n" + 
//				"	public static double runAddTest() {\n" + 
//				"		int numTests = 0;\n" + 
//				"		int numPassed = 0;\n" + 
//				"		for (int i = 0; i < 6; i++) {\n" + 
//				"			for (int j = 0; j < 6; j++) {numTests++;\n" + 
//				"				int expected = i + j;\n" + 
//				"				int result = add(i, j);\n" + 
//				"				if (result == expected) {\n" + 
//				"					numPassed++;\n" + 
//				"					System.out.print(\"Pass: \");\n" + 
//				"				}\n" + 
//				"				else {\n" + 
//				"					System.out.print(\"Fail: \");\n" + 
//				"				}\n" + 
//				"				System.out.println(\"add(\" + i + \", \"  + j + \") returned: \"  + result + \". Expected: \"  + expected);\n" + 
//				"			}\n" + 
//				"               System.out.flush();" +
//				"		}		return (double)numPassed/(numTests * 2);\n" + 
//				"	}\n" + 
//				"}";
//
//		test.setSourceContents(file);
//		test.setClassName("TestAdd");
//		test.setMethodBeingChecked("add");
//		test.setMethodToCall("runAddTest");
//		RubricEntry entry = new RubricEntry();
//		entry.setName("TestAdd");
//		entry.setValue(5);
//		entry.setAutomationType(RubricEntry.AutomationTypes.RUN_CODE);
//		entry.setAutomation(test);
//		return entry;
//		
//	} 
//	

	
	
	
//	public String getClassName() {
//		return className;
//	}
//
//
//	public void setClassName(String className) {
//		this.className = className;
//	}


	public String getMethodToCall() {
		return methodToCall;
	}


	public void setMethodToCall(String methodToCall) {
		this.methodToCall = methodToCall;
	}


//	public String getSourceContents() {
//		return sourceContents;
//	}


	public String getMethodBeingChecked() {
		return methodBeingChecked;
	}


	public List<FileData> getSourceFiles() {
		return sourceFiles;
	}


	public void setSourceFiles(List<FileData> sourceFiles) {
		this.sourceFiles = sourceFiles;
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
		labels.add(MethodNames.METHOD_BEING_CHECKED.toString());
		content.add(methodBeingChecked);
		labels.add(MethodNames.METHOD_TO_CALL.toString());
		content.add(methodToCall);
		columnData.add(labels);
		columnData.add(content);
		for (FileData file : sourceFiles) {
			labels.add(MethodNames.SOURCE_FILE.toString());
			content.add(file.getName());
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


	@Override
	protected void loadAutomationColumns(String entryName, Map<String, List<List<Object>>> columnData) {
		List<List<Object> > columns = columnData.get(entryName.toUpperCase());
		
		if (columns == null || columns.size() != getNumAutomationColumns()) {
			JOptionPane.showMessageDialog(null, "Expected there two be " + getNumAutomationColumns() + " columns of automation data for " + entryName + " there are " + columns.size(), "Bad rubric automation data",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		else {
			List<Object> files = new ArrayList<Object>();
			List<Object> labelRow = columns.get(0);
			methodBeingChecked = null;
			methodToCall = null;
			for (int row = 0; row < labelRow.size(); row++) {
				String label = (String)labelRow.get(row);
				if (label.equalsIgnoreCase(MethodNames.METHOD_BEING_CHECKED.toString())) {
					methodBeingChecked = (String)columns.get(1).get(row);					
				}
				else if (label.equalsIgnoreCase(MethodNames.METHOD_TO_CALL.toString())) {
					methodToCall = (String)columns.get(1).get(row);
				}
				else if (label.equalsIgnoreCase(MethodNames.SOURCE_FILE.toString())) {
					files.add(columns.get(1).get(row));
				}
			}
			if (files.size() == 0 || methodBeingChecked == null || methodToCall == null) {
				String message = "Expected these automation labels for " + entryName + ":";
				for (MethodNames methodName : MethodNames.values()) {
					message += methodName;
					message += " ";
				}
				JOptionPane.showMessageDialog(null, message, "Bad rubric automation data",
						JOptionPane.ERROR_MESSAGE);
				return;			
			}
			sourceFiles = new ArrayList<FileData>();
			for (Object file : files) {
				List<List<Object>> sourceInfo = columnData.get(((String)file).toUpperCase());
				if (sourceInfo == null || sourceInfo.size() == 0) {
					JOptionPane.showMessageDialog(null, "Expected the text for " + file + " in the rubric info", "Bad rubric automation data",
							JOptionPane.ERROR_MESSAGE);
					return;
					
				}
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
				sourceFiles.add(new FileData((String)file, text, "0", null));
			}
		}
	}
}
