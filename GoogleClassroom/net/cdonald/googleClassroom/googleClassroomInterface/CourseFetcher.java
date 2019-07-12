package net.cdonald.googleClassroom.googleClassroomInterface;

import java.util.Map;

import net.cdonald.googleClassroom.model.ClassroomData;

public class CourseFetcher extends ClassroomDataFetcher {
	public CourseFetcher(GoogleClassroomCommunicator authorize, DataFetchListener listener) {
		super(authorize, listener);
	}
	
	
	@Override
	protected Integer doInBackground() throws Exception {
		authorize.getClasses(this);
		return 0;
	}


	@Override
	protected ClassroomData newData(Map<String, String> initData) {
		// TODO Auto-generated method stub
		return null;
	}



}
