package net.cdonald;

public class TestAdd {
	public static double runAddTest() {
		int numTests = 0;
		int numPassed = 0;
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 6; j++) {
				int expected = i + j;
				int result =Test.add(i, j);
				if (result == expected) {
					numPassed++;
					System.out.print("Pass: ");
				}
				else {
					System.out.print("Fail: ");
				}
				System.out.println("add(" + i + ", "  + j + ") returned: "  + result + ". Expected: "  + expected);
			}
		}
		return (double)numPassed/numTests;
	}
}
