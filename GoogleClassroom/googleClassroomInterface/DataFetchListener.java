package googleClassroomInterface;

import java.util.List;

import model.ClassroomData;

public interface DataFetchListener {
	public abstract void retrievedInfo(ClassroomData data);
}
