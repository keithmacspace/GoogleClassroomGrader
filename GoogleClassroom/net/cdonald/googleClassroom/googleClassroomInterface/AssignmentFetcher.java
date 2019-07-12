package net.cdonald.googleClassroom.googleClassroomInterface;

import java.io.IOException;
import java.util.Map;

import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.SQLDataBase;



public class AssignmentFetcher extends ClassroomDataFetcher {
	private ClassroomData classSelected;

	
	public AssignmentFetcher(SQLDataBase dataBase, ClassroomData classSelected, GoogleClassroomCommunicator authorize, DataFetchListener listener) {
		super(dataBase, "Assignments", ClassroomData.fieldNames.class, authorize, listener, null);
		this.classSelected = classSelected;

	}

	@Override
	protected Integer doInBackground()  {
		readDataBase();
		try {
			authorize.getAssignments(classSelected, this);
		} catch (IOException e) {
			communicationException = e;
		}
		return 0;
	}

	@Override
	protected ClassroomData newData(Map<String, String> initData) {
		// TODO Auto-generated method stub
		return new ClassroomData(initData);
	}

}
