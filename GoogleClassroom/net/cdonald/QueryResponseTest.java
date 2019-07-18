package net.cdonald;

import java.util.List;
import java.util.concurrent.Semaphore;

import net.cdonald.googleClassroom.listenerCoordinator.LongQueryListener;

public class QueryResponseTest extends LongQueryListener<Integer> {


	Semaphore done = new Semaphore(0);
	@Override
	public void process(List<Integer> list) {
		for (Integer i : list) {
			System.out.println(i);
		}		
	}
	
	@Override
	public void done() {
		done.release();
	}

	public boolean isDone() {
		try {
			done.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
}
