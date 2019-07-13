package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.List;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;

public class RubricEntryCallMethod {
	public static Class<?>[] classesSupported = { int.class, boolean.class, double.class, String.class, Integer.class,
			Double.class, Boolean.class, int[].class, boolean[].class, double[].class, String[].class, List.class,
			ArrayList.class, };
	private Class<?>[] paramTypes;
	private String methodToCall;
	boolean checkSystemOut;
	List<List<String>> inputs;
	List<String> outputs;
	String sysOut;

	public RubricEntryCallMethod(List<String> paramTypes, String methodToCall, String returnType) {
		super();
		this.paramTypes = new Class<?>[paramTypes.size()];
		for (int i = 0; i < paramTypes.size(); i++) {
			this.paramTypes[i] = convertStringToClass(paramTypes.get(i));
		}
		this.methodToCall = methodToCall;
		if (returnType == "System.out") {
			checkSystemOut = true;
		}
		this.methodToCall = methodToCall;
		inputs = new ArrayList<List<String>>();
		outputs = new ArrayList<String>();
	}

	public void addParameterPair(List<String> inputs, String output) {
		this.inputs.add(inputs);
		this.outputs.add(output);
	}

	public void runAutomation(CompilerMessage message, StudentWorkCompiler compiler) {
		int runCount = 0;
		int failCount = 0;
		if (message.isSuccessful()) {
			if (checkSystemOut) {
				// attach a listener to system out, system out only, not system. in
			}
			Object[] args = new Object[paramTypes.length];
			for (int i = 0; i < inputs.size(); i++) {
				List<String> inputList = inputs.get(i);
				int argCount = 0;
				for (String input : inputList) {
					args[argCount] = convertStringToObject(input, paramTypes[argCount]);
					argCount++;
				}
				runCount++;

				// here is where we translate the individual param types into that actual type
				String results = compiler.runSpecificMethod(!checkSystemOut, methodToCall, message, paramTypes,
						classesSupported);
				if (checkSystemOut) {
					results = sysOut;
				}
				if (results.equals(outputs.get(i)) == false) {
					failCount++;
				}
			}
		}
	}

	public Object convertStringToObject(String value, Class<?> typeToCovertTo) {
		return null;
	}

	public static Class<?> convertStringToClass(String value) {
		for (Class<?> check : classesSupported) {
			System.out.println("check = " + check + " " + "value = " + value);
			if (check.toString().compareTo(value) == 0) {
				return check;
			}
		}
		return null;
	}

	public static void main(String[] args) {
		for (Class<?> x : classesSupported) {
			System.out.println(x);
			System.out.println(convertStringToClass(x.toString()));
		}
	}

}
