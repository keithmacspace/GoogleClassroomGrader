package net.cdonald.googleClassroom.inMemoryJavaCompiler;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingWorker;

import net.cdonald.googleClassroom.gui.CompileErrorListener;
import net.cdonald.googleClassroom.gui.RecompileListener;
import net.cdonald.googleClassroom.model.FileData;

public class StudentWorkCompiler {

	private Map<String, StudentBuildInfo> studentBuildInfoMap;
	private SwingWorker<Void, CompilerMessage> compilerWorker;	

	private CompileListener listener;

	public StudentWorkCompiler(CompileListener listener) {
		this.listener = listener;
		studentBuildInfoMap = new HashMap<String, StudentBuildInfo>();
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
					runCore(aClass);
				}

			}
		}
	}

	public void compileAndRun(RecompileListener listener, FileData fileData, String text) {
		compilerWorker = new SwingWorker<Void, CompilerMessage>() {

			@Override
			protected Void doInBackground() throws Exception {
				InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();
				Class<?> aClass = null;
				try {
					
					aClass = compiler.compile(fileData.getClassName(), text);
				} catch (Exception e) {
					System.err.println(e.getMessage());
					listener.compileError(e.getMessage());
				}
				
				if (aClass != null) {
					//System.err.println("Running ");
					runCore(aClass);
				}
				return null;
			}
			
		};
		compilerWorker.execute();
		
	}

	private void runCore(Class<?> aClass) {
		Class<?> params[] = { String[].class };
		try {
			Method method = aClass.getDeclaredMethod("main", params);
			Object[] args = { null };
			method.invoke(null, args);
			System.out.println("Ran Successfully");
			// This is how I send the message that execution has completed
			// Hopefully none of the students will print a zero.
			// I hate doing this, but I needed some sort of semaphore
			System.out.println("\0");
		} 
		catch (Exception e) {
			System.err.println("Exception Caught");
			System.out.println("Exception Caught\n" + e.getMessage() + "\n");
			System.out.println("\0");
		}
		
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
