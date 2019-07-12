package net.cdonald.googleClassroom.googleClassroomInterface;

import java.io.IOException;
import java.util.Map;

import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.SQLDataBase;

public class SheetFetcher extends ClassroomDataFetcher {

	private String url;

	
	public SheetFetcher(String url, GoogleClassroomCommunicator authorize, DataFetchListener listener,
			FetchDoneListener fetchDoneListener) {
		this(null, "", url, authorize, listener, fetchDoneListener);
	}
	
	public SheetFetcher(SQLDataBase dataBase, String dbType, String url, GoogleClassroomCommunicator authorize, DataFetchListener listener,
			FetchDoneListener fetchDoneListener) {
		super(dataBase, dbType, GoogleSheetData.fieldNames.class, authorize, listener, fetchDoneListener);
		this.url = url;

	}

	@Override
	protected Integer doInBackground() throws Exception {		
		readDataBase();		
		try {
			authorize.getSheetNames(url, this);
		}
		catch(IOException e) {
			communicationException = e;
		}
		return null;
	}

	@Override
	protected ClassroomData newData(Map<String, String> initData) {
		return new GoogleSheetData(initData);
	}

}
