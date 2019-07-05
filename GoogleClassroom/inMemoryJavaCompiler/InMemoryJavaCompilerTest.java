package inMemoryJavaCompiler;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;



public class InMemoryJavaCompilerTest {


	public void compile_WhenTypical() throws Exception {
		StringBuffer sourceCode = new StringBuffer();

		sourceCode.append("package org.mdkt;\n");
		sourceCode.append("public class HelloClass {\n");
		sourceCode.append("   public String hello() { return \"hello\"; }");
		sourceCode.append("}");

		Class<?> helloClass = InMemoryJavaCompiler.newInstance().compile("org.mdkt.HelloClass", sourceCode.toString());
		Object obj = helloClass.getDeclaredConstructor().newInstance();
		Class<?> noparams[] = {};
		Method method = helloClass.getDeclaredMethod("hello", noparams);
		System.out.println(method.invoke(obj));
		
//		//Assert.//AssertNotNull(helloClass);
//		//Assert.//AssertEquals(1, helloClass.getDeclaredMethods().length);
	}

	
	public void compileAll_WhenTypical() throws Exception {
		String cls1 = "public class A{ public B b() { return new B(); }}";
		String cls2 = "public class B{ public String toString() { return \"B!\"; }}";

		Map<String, Class<?>> compiled = InMemoryJavaCompiler.newInstance().addSource("A", cls1).addSource("B", cls2).compileAll();

		//Assert.//AssertNotNull(compiled.get("A"));
		//Assert.//AssertNotNull(compiled.get("B"));

		Class<?> aClass = compiled.get("A");
		Object a = aClass.newInstance();
		//Assert.//AssertEquals("B!", aClass.getMethod("b").invoke(a).toString());
	}

	
	public void compile_WhenSourceContainsInnerClasses() throws Exception {
		StringBuffer sourceCode = new StringBuffer();

		sourceCode.append("package org.mdkt;\n");
		sourceCode.append("public class HelloClass {\n");
		sourceCode.append("   private static class InnerHelloWorld { int inner; }\n");
		sourceCode.append("   public String hello() { return \"hello\"; }");
		sourceCode.append("}");

		Class<?> helloClass = InMemoryJavaCompiler.newInstance().compile("org.mdkt.HelloClass", sourceCode.toString());
		//Assert.//AssertNotNull(helloClass);
		//Assert.//AssertEquals(1, helloClass.getDeclaredMethods().length);
	}

	
	public void compile_whenError() throws Exception {
		//thrown.expect(CompilationException.class);
		//thrown.expectMessage("Unable to compile the source");
		StringBuffer sourceCode = new StringBuffer();

		sourceCode.append("package org.mdkt;\n");
		sourceCode.append("public classHelloClass {\n");
		sourceCode.append("   public String hello() { return \"hello\"; }");
		sourceCode.append("}");
		InMemoryJavaCompiler.newInstance().compile("org.mdkt.HelloClass", sourceCode.toString());
	}

	
	public void compile_WhenFailOnWarnings() throws Exception {
		//thrown.expect(CompilationException.class);
		StringBuffer sourceCode = new StringBuffer();

		sourceCode.append("package org.mdkt;\n");
		sourceCode.append("public class HelloClass {\n");
		sourceCode.append("   public java.util.List<String> hello() { return new java.util.ArrayList(); }");
		sourceCode.append("}");
		InMemoryJavaCompiler.newInstance().compile("org.mdkt.HelloClass", sourceCode.toString());
	}

	
	public void compile_WhenIgnoreWarnings() throws Exception {
		StringBuffer sourceCode = new StringBuffer();

		sourceCode.append("package org.mdkt;\n");
		sourceCode.append("public class HelloClass {\n");
		sourceCode.append("   public java.util.List<String> hello() { return new java.util.ArrayList(); }");
		sourceCode.append("}");
		Class<?> helloClass = InMemoryJavaCompiler.newInstance().ignoreWarnings().compile("org.mdkt.HelloClass", sourceCode.toString());
		List<?> res = (List<?>) helloClass.getMethod("hello").invoke(helloClass.newInstance());
		//Assert.//AssertEquals(0, res.size());
	}

	
	public void compile_WhenWarningsAndErrors() throws Exception {
//		thrown.expect(CompilationException.class);
		StringBuffer sourceCode = new StringBuffer();

		sourceCode.append("package org.mdkt;\n");
		sourceCode.append("public class HelloClass extends xxx {\n");
		sourceCode.append("   public java.util.List<String> hello() { return new java.util.ArrayList(); }");
		sourceCode.append("}");
		try {
			InMemoryJavaCompiler.newInstance().compile("org.mdkt.HelloClass", sourceCode.toString());
		} catch (Exception e) {
			System.out.println("Exception caught: {}" + e);

		}
	}
	
	public static void main(String [] args) throws Exception {
		InMemoryJavaCompilerTest test = new InMemoryJavaCompilerTest();
		test.compile_WhenTypical();
		try {
		test.compile_whenError();
		}
		catch(CompilationException e) {
			System.out.println(e.getMessage());	
		}
		
	}
}
