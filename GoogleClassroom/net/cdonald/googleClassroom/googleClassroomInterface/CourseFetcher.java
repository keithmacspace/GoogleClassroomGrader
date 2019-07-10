package net.cdonald.googleClassroom.googleClassroomInterface;


public class CourseFetcher extends ClassroomDataFetcher {
	public CourseFetcher(GoogleClassroomCommunicator authorize, DataFetchListener listener) {
		super(authorize, listener);
	}
	
	
	@Override
	protected Integer doInBackground() throws Exception {
		authorize.getClasses(this);
		return 0;
	}



}
