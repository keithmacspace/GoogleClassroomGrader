package net.cdonald.googleClassroom.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SetInfoLabelListener;
import net.cdonald.googleClassroom.utils.SimpleUtils;


public class RubricEntryRunCode extends  RubricAutomation {
	private String methodToCall;
	private List<FileData> sourceFiles;
	private List<String> studentBaseClassNames;
	boolean checkSystemOut;		
	private enum ColumnNames {METHOD_TO_CALL, CLASS_NAMES_TO_REPLACE, SOURCE_FILE};


	
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
	
	public List<Method> getPossibleMethods(List<FileData> goldenSource, StudentWorkCompiler compiler) {

		if (goldenSource == null || goldenSource.size() == 0) {
			return null;
		}
		
		studentBaseClassNames.clear();		
		for (FileData fileData : goldenSource) {
			studentBaseClassNames.add(fileData.getClassName());
		}
		
		List<FileData> rubricFiles = new ArrayList<FileData>(goldenSource);
		

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
	
	protected Double runAutomation_(RubricEntry entry, String studentName, String studentId, CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData) {
		if (message == null) {
			return null;
		}
		if (message.isSuccessful()) {

			ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "Running " + this.getOwnerName() + " for " + studentName);
		

			List<FileData> studentFiles = compiler.getSourceCode(studentId);
			return runAutomation_(studentFiles, studentId, compiler, consoleData);
		}
		return null;
	}
	protected Double runAutomation_(List<FileData> studentFiles, String studentId, StudentWorkCompiler compiler, ConsoleData consoleData) {
		if (studentFiles != null && studentFiles.size() != 0)
		{
			List<FileData> rubricFiles = new ArrayList<FileData>(studentFiles);
			consoleData.runStarted(studentId, getOwnerName());				
			prepareForNextTest();

			for (FileData sourceFile : sourceFiles) {
				String modifiedSource = replaceClassNames(sourceFile.getFileContents(), studentFiles.get(0).getClassName());
				if (modifiedSource == null) {
					ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
					addOutput(studentId, "Could not modify source to change class name to call student's code");					
					System.out.println("\0");					
					return null;					
				}
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
		try {
			CompilationUnit cu = StaticJavaParser.parse(sourceContents);
			for (String baseClassName : studentBaseClassNames) {
				ModifierVisitor<Void> nameChange = new ClassNameModifier(baseClassName, studentClassName);
				nameChange.visit(cu, null);
				
			}
			return cu.toString();
		}
		// Student source might not be parsable, in which case that is fine
		catch(Exception e) {

		}
		return null;
	}
	
	private static class ClassNameModifier extends ModifierVisitor<Void>  {
		String original;
		String newName;
		public ClassNameModifier(String original, String newName) {
			super();
			this.original = original;
			this.newName = newName;
		}
		@Override
		public Visitable visit(FieldAccessExpr n, Void arg) {
			// TODO Auto-generated method stub
			super.visit(n, arg);
			
			return n;
		}
		@Override
		public Visitable visit(MethodCallExpr n, Void arg) {
			// TODO Auto-generated method stub
			super.visit(n, arg);
			Optional<Expression> scope = n.getScope();
			if (scope.isPresent()) {				
				if (scope.get().toString().equals(original)) {
					String newCall = newName + "." + n.getNameAsString();					
					MethodCallExpr newMethodCall = new MethodCallExpr(newCall);
					newMethodCall.setArguments(n.getArguments());
					return newMethodCall;
				}
			}					
			return n;
		}
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
	protected void saveAutomationColumns(String entryName, List<List<Object>> columnData, Map<String, List<Object>> fileData) {
		List<Object> labels = new ArrayList<Object>();
		List<Object> content = new ArrayList<Object>();
		labels.add(entryName);
		content.add(entryName);
		labels.add(ColumnNames.CLASS_NAMES_TO_REPLACE.toString());
		String classes = "";
		for (int i = 0; i < studentBaseClassNames.size() - 1; i++) {
			classes += studentBaseClassNames.get(i) + ",";
		}
		if (studentBaseClassNames.size() != 0) {
			classes += studentBaseClassNames.get(studentBaseClassNames.size() - 1);
		}
		content.add(classes);
		labels.add(ColumnNames.METHOD_TO_CALL.toString());
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
		labels.add(ColumnNames.SOURCE_FILE.toString());
		content.add(sourceFileNames);
		for (FileData file : sourceFiles) {
			if (fileData.containsKey(file.getName()) == false) {
				List<Object> fileLineList = file.fillSaveData();
				fileData.put(file.getName(), fileLineList);
			}
		}
	}

	
	private void showErrorMessage(String entryName) {
		Rubric.showLoadError("The rubric component \"" + entryName + "\" is missing run data.");
	}


	@Override
	protected void loadAutomationColumns(String entryName, Map<String, List<List<Object>>> columnData, Map<String, FileData> fileDataMap) {
		List<List<Object> > columns = columnData.get(entryName.toUpperCase());
		if (columns == null || columns.size() != 2) {
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
					if (label.equalsIgnoreCase(ColumnNames.METHOD_TO_CALL.toString())) {
						methodToCall = (String)columns.get(1).get(row);
					}
					else if (label.equalsIgnoreCase(ColumnNames.SOURCE_FILE.toString())) {
						files = SimpleUtils.breakUpCommaList(columns.get(1).get(row));
					}
					else if (label.equalsIgnoreCase(ColumnNames.CLASS_NAMES_TO_REPLACE.toString())) {
						studentBaseClassNames = SimpleUtils.breakUpCommaList(columns.get(1).get(row));
					}
				}
			}
			if (files == null || files.size() == 0 ||  studentBaseClassNames.size() == 0 || methodToCall == null) {
				showErrorMessage(entryName);							
			}
			if (files == null) {
				return;
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
						FileData fileData = FileData.newFromSheet(file, columnData.get(file.toUpperCase()));
						if (fileData == null) {
							showErrorMessage(entryName);
						}
						else {
							sourceFiles.add(fileData);
							fileDataMap.put(file, fileData);
						}
					}
				}
			}
		}
	}

}
