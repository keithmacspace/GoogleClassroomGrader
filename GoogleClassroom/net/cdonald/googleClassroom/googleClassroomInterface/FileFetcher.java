package net.cdonald.googleClassroom.googleClassroomInterface;

import net.cdonald.googleClassroom.model.ClassroomData;

public class FileFetcher extends ClassroomDataFetcher {
	ClassroomData course;
	ClassroomData assignment;
	public FileFetcher(ClassroomData course, ClassroomData assignment, GoogleClassroomCommunicator authorize, DataFetchListener listener, FetchDoneListener fetchListener) {
		super(authorize, listener, fetchListener);
		this.course = course;
		this.assignment = assignment;
	}

	@Override
	protected Integer doInBackground() throws Exception {
		authorize.getStudentWork(course, assignment, this);
		return 0;
	}

}
