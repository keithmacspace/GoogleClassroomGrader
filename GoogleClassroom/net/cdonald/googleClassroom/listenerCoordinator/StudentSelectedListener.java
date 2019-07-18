package net.cdonald.googleClassroom.listenerCoordinator;

import java.util.List;

public interface StudentSelectedListener {
	public void fired(List<String> ids, String idToDisplay);

}
