package googleClassroomInterface;

import model.ClassroomData;

public class StudentFetcher extends ClassroomDataFetcher {
	private ClassroomData classSelected;
	public StudentFetcher(ClassroomData classSelected, GoogleClassroomCommunicator authorize, DataFetchListener listener) {		
		super(authorize, listener);
		this.classSelected = classSelected;
	}

	@Override
	protected Integer doInBackground() throws Exception {
		authorize.getStudents(classSelected, this);
		return 0;
	}

}
