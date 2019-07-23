package net.cdonald;
import net.cdonald.googleClassroom.listenerCoordinator.LongQueryResponder;
public class LongQueryResponderTest extends LongQueryResponder<Integer> {


	@Override
	protected Void doInBackground() throws Exception {
		for (int i = 0; i < 1000; i++) {
			publish(i);
			Thread.sleep(50);
		}
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public LongQueryResponder<Integer> newInstance() {
		return new LongQueryResponderTest();
	}	

}
