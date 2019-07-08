package googleClassroomInterface;



import model.ClassroomData;

public interface DataFetchListener {
	public abstract void retrievedInfo(ClassroomData data);
}
