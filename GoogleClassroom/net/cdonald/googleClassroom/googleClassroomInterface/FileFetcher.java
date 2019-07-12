package net.cdonald.googleClassroom.googleClassroomInterface;

import java.util.Map;

import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.SQLDataBase;

public class FileFetcher extends ClassroomDataFetcher {
	ClassroomData course;
	ClassroomData assignment;
	public FileFetcher(SQLDataBase dataBase, ClassroomData course, ClassroomData assignment, GoogleClassroomCommunicator authorize, DataFetchListener listener, FetchDoneListener fetchListener) {
		super(dataBase, FileData.dbTableName(assignment), FileData.fieldNames.class, authorize, listener, fetchListener);
		this.course = course;
		this.assignment = assignment;
	}

	@Override
	protected Integer doInBackground() throws Exception {
		authorize.getStudentWork(course, assignment, this);
		return 0;
	}

	@Override
	protected ClassroomData newData(Map<String, String> initData) {
		// TODO Auto-generated method stub
		return new FileData(initData);
	}

}
