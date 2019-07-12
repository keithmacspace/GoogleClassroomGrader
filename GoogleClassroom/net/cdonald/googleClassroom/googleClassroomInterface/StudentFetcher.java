package net.cdonald.googleClassroom.googleClassroomInterface;


import java.io.IOException;
import java.util.Map;

import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.SQLDataBase;
import net.cdonald.googleClassroom.model.StudentData;

public class StudentFetcher extends ClassroomDataFetcher {
	private ClassroomData classSelected;

	
	public StudentFetcher(SQLDataBase dataBase, ClassroomData classSelected, GoogleClassroomCommunicator authorize, DataFetchListener listener, FetchDoneListener fetchDoneListener) {		
		super(dataBase, StudentData.DB_TABLE_NAME, StudentData.fieldNames.class, authorize, listener, fetchDoneListener);
		this.classSelected = classSelected;	
	}


	@Override
	protected Integer doInBackground()  {
		readDataBase();
		try {
			authorize.getStudents(classSelected, this);
		} catch (IOException e) {
			communicationException = e;
		}		
		return 0;
	}


	@Override
	protected ClassroomData newData(Map<String, String> initData) {
		// TODO Auto-generated method stub
		return new StudentData(initData);
	}


}
