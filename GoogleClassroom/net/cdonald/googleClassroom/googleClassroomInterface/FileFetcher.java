package net.cdonald.googleClassroom.googleClassroomInterface;


import java.util.Map;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentAssignmentQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentClassQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetDBNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;


public class FileFetcher extends ClassroomDataFetcher {

	public FileFetcher(GoogleClassroomCommunicator authorize) {
		super(authorize);
	}

	@Override
	protected Void doInBackground() throws Exception {
		ClassroomData course = (ClassroomData) ListenerCoordinator.runQuery(GetCurrentClassQuery.class);
		ClassroomData assignment = (ClassroomData)ListenerCoordinator.runQuery(GetCurrentAssignmentQuery.class);		
		if (course != null && assignment != null) {
			
			String assignmentFilesDBName = (String)ListenerCoordinator.runQuery(GetDBNameQuery.class, GetDBNameQuery.DBType.ASSIGNMENT_FILES_DB);
//			String dataBaseTable = FileData.dbTableName(assignment);
//			readDataBase(assignmentFilesDBName, dataBaseTable, FileData.fieldNames.class);
			authorize.getStudentWork(course, assignment, this);
		}
		return null;
	}

	@Override
	protected ClassroomData newData(Map<String, String> initData) {
		// TODO Auto-generated method stub
		return new FileData(initData);
	}

}
