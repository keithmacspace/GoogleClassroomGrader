package inMemoryJavaCompiler;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.swing.SwingWorker;

import model.FileData;

public class StudentWorkCompiler {
	private Map<String, Map<String, Class<?>>> studentCompiledData;
	private Map<String, ArrayList<FileData>> studentFileData;
	private SwingWorker<Void, CompilerMessage> compilerWorker;
	private SwingWorker<Void, String> runWorker;

	private CompileListener listener;

	public StudentWorkCompiler(CompileListener listener) {
		this.listener = listener;
		studentFileData = new HashMap<String, ArrayList<FileData>>();
		studentCompiledData = new HashMap<String, Map<String, Class<?>>>();
	}

	public void clearData() {
		if (studentFileData != null) {
			studentFileData.clear();
		}
		if (studentCompiledData != null) {
			studentCompiledData.clear();
		}
	}

	public void addFile(FileData fileData) {
		if (studentFileData.containsKey(fileData.getId()) == false) {
			studentFileData.put(fileData.getId(), new ArrayList<FileData>());
		}
		studentFileData.get(fileData.getId()).add(fileData);
	}

	public CompileListener getListener() {
		return listener;
	}

	public void setListener(CompileListener listener) {
		this.listener = listener;
	}

	public Map<String, Map<String, Class<?>>> getStudentCompiledData() {
		return studentCompiledData;
	}

	public Map<String, ArrayList<FileData>> getStudentFileData() {
		return studentFileData;
	}

	public void compileAll() {
		compilerWorker = new SwingWorker<Void, CompilerMessage>() {

			@Override
			protected void process(List<CompilerMessage> chunks) {
				if (listener != null) {
					listener.compileResults(chunks);
				}
			}

			@Override
			protected Void doInBackground() {
				studentCompiledData = new HashMap<String, Map<String, Class<?>>>();
				Set<String> keys = studentFileData.keySet();
				for (String key : keys) {
					ArrayList<FileData> studentFiles = studentFileData.get(key);
					InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();
					try {
						for (FileData file : studentFiles) {
							compiler.addSource(file.getClassName(), file.getFileContents());
						}
						Map<String, Class<?>> compiled = compiler.compileAll();
						studentCompiledData.put(key, compiled);
						publish(new CompilerMessage(key, true));
					} catch (CompilationException e) {
						studentCompiledData.put(key, null);
						publish(new CompilerMessage(key, false, e.getLocalizedMessage()));
					} catch (Exception e2) {
						studentCompiledData.put(key, null);
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
		return studentFileData.get(id);
	}

	public void run(String id) {

		if (studentCompiledData.get(id) != null) {

			runWorker = new SwingWorker<Void, String>() {

				@Override
				protected Void doInBackground() {
					Class<?> params[] = { String[].class };
					Map<String, Class<?>> compiled = studentCompiledData.get(id);

					try {
						ArrayList<FileData> files = studentFileData.get(id);
						for (FileData fileData : files) {
							Class<?> aClass = compiled.get(fileData.getClassName());
							Method method = aClass.getDeclaredMethod("main", params);
							Object[] args = { null };
							method.invoke(null, args);
						}
					} 
					catch (Exception e) {

					}
					return null;
				}

				protected void done() {
					super.done();
					System.out.println("Execution Done");
					if (listener != null) {
						listener.runDone();
					}
				}
			};

			runWorker.execute();

		}

	}

}
