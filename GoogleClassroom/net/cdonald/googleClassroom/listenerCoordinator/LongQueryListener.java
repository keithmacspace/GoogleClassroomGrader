package net.cdonald.googleClassroom.listenerCoordinator;

import java.util.List;

public abstract class LongQueryListener<V> {

	public abstract void process(List<V> list);
	public void done() {}

}
