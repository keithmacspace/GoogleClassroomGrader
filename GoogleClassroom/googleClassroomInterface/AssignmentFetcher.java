package googleClassroomInterface;

import model.ClassroomData;

public class AssignmentFetcher extends ClassroomDataFetcher {
	private ClassroomData classSelected;
	public AssignmentFetcher(ClassroomData classSelected, GoogleClassroomCommunicator authorize, DataFetchListener listener) {
		super(authorize, listener);
		this.classSelected = classSelected;
	}

	@Override
	protected Integer doInBackground() throws Exception {
		authorize.getAssignments(classSelected, this);
		return 0;
	}

}
