package inMemoryJavaCompiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingWorker;

import model.FileData;

public class StudentWorkCompiler {

	private Map<String, InMemoryJavaCompiler> studentCompiledData;
	private Map<String, ArrayList<FileData> > studentFileData;
	private SwingWorker<Map<String, InMemoryJavaCompiler>, Integer> swingWorker;
	public StudentWorkCompiler() {
		studentFileData = new HashMap<String, ArrayList<FileData> >();
		studentCompiledData = new HashMap<String, InMemoryJavaCompiler>();
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
	
	public void compileAll() {
		swingWorker = new SwingWorker<Map<String, InMemoryJavaCompiler>, Integer>() {

			@Override
			protected Map<String, InMemoryJavaCompiler> doInBackground()  {
				studentCompiledData = new HashMap<String, InMemoryJavaCompiler>(); 
				Set<String> keys = studentFileData.keySet();
				for (String key : keys) {
					System.out.println("Attempting " + key);
					ArrayList<FileData> studentFiles = studentFileData.get(key);
					InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();
					FileData lastFile = null;
					try {
						for (FileData file : studentFiles) {
							lastFile = file;
							compiler.addSource(file.getClassName(), file.getFileContents());
						}
						compiler.compileAll();
						studentCompiledData.put(key, compiler);
						System.out.println(key + " succeeded");
					} 
					catch (CompilationException e) {
						studentCompiledData.put(key, null);
						System.out.println(key + " failed " + e.getMessage());						
						
					}
					catch (Exception e2) {
						studentCompiledData.put(key, null);
						System.out.println(key + " failed " + e2.getMessage());
						System.out.println(lastFile.getName());
						System.out.println(lastFile.getFileContents());
					}

				}
				// TODO Auto-generated method stub
				return studentCompiledData;
			}
			
		};
		swingWorker.execute();
	}

}
