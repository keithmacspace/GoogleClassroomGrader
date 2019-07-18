package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;

public class RubricEntryRunCode extends  RubricAutomation{
	private String className;
	private String methodToCall; 
	private String sourceContents;
	private String methodBeingChecked;
	boolean checkSystemOut;	
	RubricEntrySystemListeners sysListeners;	
	Map<String, String> perStudentResults;

	
	public RubricEntryRunCode() {
		sysListeners = new RubricEntrySystemListeners("Run Code");
		perStudentResults = new HashMap<String, String>();
	}


	public void setMethodBeingChecked(String methodBeingChecked) {
		this.methodBeingChecked = methodBeingChecked;
	}


	public void setSourceContents(String methodToCall) {
		this.sourceContents = methodToCall;
	}
	
	
	protected double runAutomation_(CompilerMessage message, StudentWorkCompiler compiler) {
		if (message.isSuccessful()) {			
			String studentId = message.getStudentId();
			String completeMethodName = compiler.getCompleteMethodName(studentId, methodBeingChecked);
			if (completeMethodName == null) {
				String error =  "no method named " + methodBeingChecked + " in source";
				System.err.println(error);
				perStudentResults.put(studentId, error);
			}			
			else {
				List<FileData> studentFiles = compiler.getSourceCode(studentId);
				List<FileData> rubricFiles = new ArrayList<FileData>(studentFiles);
				String modifiedSource = replaceMethodName(completeMethodName);
				sysListeners.prepareForNextTest();
				FileData fileData = new FileData(className + ".java", modifiedSource, studentId,  null);
				rubricFiles.add(fileData);
				
				fileData.setRubricCode(true);
				Class<?> []params = {};
				Object []args = {};
				String returnValue = compiler.compileAndRun(true,  rubricFiles, methodToCall, params, args);
				addOutput(studentId, sysListeners.getSysOutText());
				double value = Double.parseDouble(returnValue);
				
				return value;
			}
		}
		return 0.0;
	}
	
	String replaceMethodName(String completeMethodName) {
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
	
	public static RubricEntry createTest() {

		RubricEntryRunCode test = new RubricEntryRunCode();
		String file = "public class TestAdd {\n" + 
				"	public static double runAddTest() {\n" + 
				"		int numTests = 0;\n" + 
				"		int numPassed = 0;\n" + 
				"		for (int i = 0; i < 6; i++) {\n" + 
				"			for (int j = 0; j < 6; j++) {numTests++;\n" + 
				"				int expected = i + j;\n" + 
				"				int result = add(i, j);\n" + 
				"				if (result == expected) {\n" + 
				"					numPassed++;\n" + 
				"					System.out.print(\"Pass: \");\n" + 
				"				}\n" + 
				"				else {\n" + 
				"					System.out.print(\"Fail: \");\n" + 
				"				}\n" + 
				"				System.out.println(\"add(\" + i + \", \"  + j + \") returned: \"  + result + \". Expected: \"  + expected);\n" + 
				"			}\n" + 
				"               System.out.flush();" +
				"		}		return (double)numPassed/(numTests * 2);\n" + 
				"	}\n" + 
				"}";

		test.setSourceContents(file);
		test.setClassName("TestAdd");
		test.setMethodBeingChecked("add");
		test.setMethodToCall("runAddTest");
		RubricEntry entry = new RubricEntry();
		entry.setName("TestAdd");
		entry.setValue(5);
		entry.setAutomationType(RubricEntry.AutomationTypes.RUN_CODE);
		entry.setAutomation(test);
		return entry;
		
	} 
	

	
	
	
	public String getClassName() {
		return className;
	}


	public void setClassName(String className) {
		this.className = className;
	}


	public String getMethodToCall() {
		return methodToCall;
	}


	public void setMethodToCall(String methodToCall) {
		this.methodToCall = methodToCall;
	}


	public String getSourceContents() {
		return sourceContents;
	}


	public String getMethodBeingChecked() {
		return methodBeingChecked;
	}


	public static void main(String [] args ) {
		RubricEntryRunCode code = new RubricEntryRunCode();
		code.setSourceContents("            java.lang.run(test, test)\n\t\t\trun(test, test)");
		code.setMethodBeingChecked("run");
		System.out.println("\"" + code.replaceMethodName("test.run") + "\"");
		
	}
	

	

}
