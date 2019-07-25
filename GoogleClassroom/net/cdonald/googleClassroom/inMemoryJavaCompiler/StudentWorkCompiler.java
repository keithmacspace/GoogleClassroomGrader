package net.cdonald.googleClassroom.inMemoryJavaCompiler;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingWorker;

import org.mdkt.compiler.CompilationException;
import org.mdkt.compiler.InMemoryJavaCompiler;

import net.cdonald.googleClassroom.listenerCoordinator.AddProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.StopRunListener;
import net.cdonald.googleClassroom.model.FileData;

public class StudentWorkCompiler {

	private Map<String, StudentBuildInfo> studentBuildInfoMap;
	private SwingWorker<Void, CompilerMessage> compilerWorker;
	private CompileListener listener;
	private RunCore runCore;
	
	

	public StudentWorkCompiler(CompileListener listener) {
		this.listener = listener;
		studentBuildInfoMap = new HashMap<String, StudentBuildInfo>();
		ListenerCoordinator.addListener(StopRunListener.class, new StopRunListener() {
			@Override
			public void fired() {
				if (runCore != null) {
					runCore.interrupt();
				}
			}
		});
	}

	public void clearData() {
		studentBuildInfoMap.clear();
	}

	public void addFile(FileData fileData) {
		String key = fileData.getId();
		
		if (studentBuildInfoMap.containsKey(key) == false) {
			studentBuildInfoMap.put(key, new StudentBuildInfo());
		}
		studentBuildInfoMap.get(key).addFileData(fileData);
		if (listener != null) {
			listener.dataUpdated();
		}
	}
	
	public void removeFiles(Set<String> ids) {
		for (String id : ids) {
			studentBuildInfoMap.remove(id);
		}
	}

	public CompileListener getListener() {
		return listener;
	}

	public void setListener(CompileListener listener) {
		this.listener = listener;
	}
	
	public Object runSpecificMethod(boolean expectingReturn, String methodName, CompilerMessage message, Class<?> []params, Object[] args) {
		String id = message.getStudentId();
		if (studentBuildInfoMap.containsKey(id)) {
			StudentBuildInfo studentBuildInfo = studentBuildInfoMap.get(id);
			if (studentBuildInfo.getStudentCompilerMap() != null) {				
				Map<String, Class<?>> compiled = studentBuildInfo.getStudentCompilerMap();
				List<FileData> files = studentBuildInfo.getStudentFileData();
				return runSpecificMethod(expectingReturn, methodName, files, compiled, params, args);
			}
		}
		throw new IllegalArgumentException();
	}

	private Object runSpecificMethod(boolean expectingReturn, String methodName, List<FileData> files, Map<String, Class<?>> compiled, Class<?>[] params, Object[] args) {
		for (FileData fileData : files) {
			Class<?> aClass = compiled.get(fileData.getClassName());
			Method method = getMethod(aClass, methodName, params);
			if (method != null) {
				return runCore(expectingReturn, method, args);
			}		
		}
		return null;
		
	}
	
	public Object compileAndRun(boolean expectingReturn, List<FileData> fileDataList, String methodName, Class<?> []params, Object[] args) {
		InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();
		Map<String, Class<?>> compiled = null;
		try {
			for (FileData fileData : fileDataList) {				
				compiler.addSource(fileData.getClassName(), fileData.getFileContents());
			}
			compiled = compiler.compileAll();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return runSpecificMethod(expectingReturn, methodName, fileDataList, compiled, params, args);
	}
	
	public void compileAll() {
		compilerWorker = new SwingWorker<Void, CompilerMessage>() {

			@Override
			protected void process(List<CompilerMessage> chunks) {
				for (CompilerMessage message : chunks) {
					studentBuildInfoMap.get(message.getStudentId()).setCompilerMessage(message);
					if (listener != null) {
						listener.dataUpdated();
					}
				}
			}

			@Override
			protected Void doInBackground() {
				final String PROGRESS_BAR_NAME = "Compiling";
				ListenerCoordinator.fire(AddProgressBarListener.class, PROGRESS_BAR_NAME);
				Set<String> keys = studentBuildInfoMap.keySet();
				for (String key : keys) {
					
					StudentBuildInfo studentBuildInfo = studentBuildInfoMap.get(key);
					studentBuildInfo.setStudentCompilerMap(null);
					List<FileData> studentFiles = studentBuildInfo.getStudentFileData();
					InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();
					try {
						for (FileData file : studentFiles) {
							compiler.addSource(file.getClassName(), file.getFileContents());
						}
						Map<String, Class<?>> compiled = compiler.compileAll();
						studentBuildInfo.setStudentCompilerMap(compiled);
						publish(new CompilerMessage(key, true));
					} catch (CompilationException e) {
						publish(new CompilerMessage(key, false, e.getLocalizedMessage()));
					} catch (Exception e2) {
						publish(new CompilerMessage(key, false, e2.getMessage()));
					}
				}
				ListenerCoordinator.fire(RemoveProgressBarListener.class, PROGRESS_BAR_NAME);
				return null;
			}

			@Override
			protected void done() {
				if (listener != null) {
					listener.compileDone();
				}
			}

		};
		compilerWorker.execute();
	}
	

	public List<FileData> getSourceCode(String id) {
		if (studentBuildInfoMap.containsKey(id) == false) {
			return null;
		}
		return studentBuildInfoMap.get(id).getStudentFileData();
	}

	public void run(String id) {

		if (studentBuildInfoMap.containsKey(id)) {
			StudentBuildInfo studentBuildInfo = studentBuildInfoMap.get(id);
			if (studentBuildInfo.getStudentCompilerMap() != null) {				
				Map<String, Class<?>> compiled = studentBuildInfo.getStudentCompilerMap();
				List<FileData> files = studentBuildInfo.getStudentFileData();
				for (FileData fileData : files) {
					Class<?> aClass = compiled.get(fileData.getClassName());
					Method method = getMethod(aClass);
					if (method != null) {
						runCore(method);
					}
				}
			}
		}
	}


	public String getCompleteMethodName(String studentId, String methodName) {
		StudentBuildInfo buildInfo = studentBuildInfoMap.get(studentId);
		if (buildInfo != null) {
			return buildInfo.getCompleteMethodName(methodName);
		}
		return null;
	}
	
	Method getMethod(Class<?> aClass) {
		Class<?> []params = { String[].class };
		return getMethod(aClass, "main", params);
	}
	
	Method getMethod(Class<?> aClass, String methodName, Class<?> []params) {

		Method method = null;
		try {
			method = aClass.getDeclaredMethod(methodName, params);
		} catch (NoSuchMethodException | SecurityException e1) {
			method = null;
		}
		return method;
	}
	private class RunCore extends Thread {
		private Object result;
		Method method;
		Object [] args;
		public RunCore(Method method, Object[] args) {
			result = null;
			this.method = method;
			this.args = args;
		}
			
		public Object getResult() {
			return result;
		}
		@Override
		public void run() {

			try {		

				result = method.invoke(null, args);
				System.out.println("Ran Successfully");
				// This is how I send the message that execution has completed
				// Hopefully none of the students will print a zero.
				// I hate doing this, but I needed some sort of semaphore
				System.out.println("\0");
				
			} 
			catch (Exception e) {				
				System.out.println("Exception Caught\n" + e.getClass() + e.getMessage() + "\n");
				System.out.println("\0");
			}
		}
		
	}
	private Object runCore(boolean expectReturn, Method method, Object[] args) {
		runCore = new RunCore(method, args);
		try {
			runCore.start();
			runCore.join();
		}
		catch (Exception e) {			
			System.out.println("Exception Caught\n" + e.getClass() + e.getMessage() + "\n");
			System.out.println("\0");
		}
		return runCore.getResult();
	}
	
	
		

	private void runCore(Method method) {
		Object[] args = {null};
		runCore(false, method, args);
	}
	
	public boolean isRunnable(String id) {
		if (studentBuildInfoMap.containsKey(id) == true) {
			StudentBuildInfo studentBuildInfo = studentBuildInfoMap.get(id);
			if (studentBuildInfo.getStudentCompilerMap() != null) {
				return true;
			}			
		}
		return false;		
	}
	
	public CompilerMessage getCompilerMessage(String id) {
		if (studentBuildInfoMap.containsKey(id) == true) {
			return studentBuildInfoMap.get(id).getCompilerMessage();
		}
		return null;
	}
}
