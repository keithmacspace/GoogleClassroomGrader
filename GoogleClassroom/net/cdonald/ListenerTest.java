package net.cdonald;

public class ListenerTest {
	String value;

	public void fired(String message) {
		System.out.println(message + value);
	}


	public ListenerTest(String value) {
		super();
		this.value = value;
	}

}
