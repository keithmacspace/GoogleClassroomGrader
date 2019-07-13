package net.cdonald.googleClassroom.googleClassroomInterface;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.SQLDataBase;

public abstract class ClassroomDataFetcher extends SwingWorker<Integer, ClassroomData> implements DataFetchListener {

	private List<ClassroomData> dbAdd;
	private Set<String> readFromDB;
	protected GoogleClassroomCommunicator authorize;
	private DataFetchListener listener;
	private FetchDoneListener fetchDoneListener;
	protected IOException communicationException;
	private SQLException databaseException;
	private Exception miscException;
	private SQLDataBase dataBase;
	private String dataBaseTable;
	private Class<? extends Enum<?>> tableLabelEnum;

	public ClassroomDataFetcher(GoogleClassroomCommunicator authorize, DataFetchListener listener) {
		this(null, null, null, authorize, listener, null);
	}

	public ClassroomDataFetcher(SQLDataBase dataBase, String dataBaseTable, GoogleClassroomCommunicator authorize,
			DataFetchListener listener) {
		this(dataBase, dataBaseTable, ClassroomData.fieldNames.class, authorize, listener, null);
	}

	public ClassroomDataFetcher(GoogleClassroomCommunicator authorize, DataFetchListener listener,
			FetchDoneListener fetchDoneListener) {
		this(null, null, null, authorize, listener, fetchDoneListener);
	}

	public ClassroomDataFetcher(SQLDataBase dataBase, String dataBaseTable, Class<? extends Enum<?>> tableLabelEnum,
			GoogleClassroomCommunicator authorize, DataFetchListener listener, FetchDoneListener fetchDoneListener) {
		super();
		this.authorize = authorize;
		this.listener = listener;
		this.fetchDoneListener = fetchDoneListener;
		this.dataBase = dataBase;
		this.dataBaseTable = dataBaseTable;
		this.tableLabelEnum = tableLabelEnum;
		communicationException = null;
		databaseException = null;
		miscException = null;
		dbAdd = new ArrayList<ClassroomData>();
		readFromDB = new HashSet<String>();
	}

	protected abstract ClassroomData newData(Map<String, String> initData);

	@Override
	public void retrievedInfo(ClassroomData data) {
		publish(data);
	}

	protected void readDataBase() {		
		if (dataBase != null) {
			List<Map<String, String>> data = null;
			try {
				data = dataBase.load(dataBaseTable, tableLabelEnum);			
			if (data != null) {
				for (Map<String, String> dbInfo : data) {
					ClassroomData temp = newData(dbInfo);
					publish(temp);
				}
			}
			} catch (SQLException e) {
				System.err.println(e.getMessage());
				databaseException = e;
			}
			catch (Exception x) {
				miscException = x;
			}
		}
	}

	@Override
	protected void process(List<ClassroomData> chunks) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				int currentSize = chunks.size();
				if (currentSize != 0) {
					for (int i = 0; i < currentSize; i++) {
						ClassroomData data = chunks.get(i);
						// If it is in our readFromDB list, that means we got it from the database
						// already & the listener added it
						if (readFromDB == null || readFromDB.contains(data.getId()) == false) {
							listener.retrievedInfo(data);
						}
						if (data.isRetrievedFromGoogle() == true) {
							dbAdd.add(data);							
						}
						else {
							// Start out adding everything in the database to the remove list
							// In done(), we'll go through and remove all the ones we're adding
							// to the database leaving with only the last few to remove
							readFromDB.add(data.getId());
						}
					}
				}
			}
		});
	}

	@Override
	protected void done() {

		if (communicationException != null) {
			System.err.println("google classroom error " + communicationException.getMessage());
		}
		if (databaseException != null) {
			System.err.println("local database error " + databaseException.getMessage());
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				if (dataBase != null && databaseException == null && miscException == null) {
					try {						
						dataBase.save(dataBaseTable, tableLabelEnum, dbAdd);
						for (ClassroomData dataCheck : dbAdd) {
							readFromDB.remove(dataCheck.getId());
						}
						if (readFromDB.size() != 0) {
							dataBase.delete(dataBaseTable, tableLabelEnum, readFromDB);
						}

						
					} catch (SQLException e) {
						System.err.println(e.getMessage());
						databaseException = e;
					} catch (Exception x) {
						miscException = x;
					}

				}
			}
		});
		if (fetchDoneListener != null) {
			fetchDoneListener.done();
		}
	}
	
	/**
	 * This will never be called by the googleClassroomInterface, only the add portion
	 */
	@Override
	public void remove(Set<String> ids) {
		
	}

	public IOException getCommunicationException() {
		return communicationException;
	}

	public SQLException getDatabaseException() {
		return databaseException;
	}

	public Exception getMiscException() {
		return miscException;
	}
}
