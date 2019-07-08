package inMemoryJavaCompiler;

import java.util.List;

public interface CompileListener {
	public void compileResults(List<CompilerMessage> result);
	public void compileDone();
	public void runDone();

}
