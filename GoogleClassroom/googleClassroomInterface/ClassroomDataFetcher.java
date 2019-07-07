package googleClassroomInterface;

import java.util.List;

import javax.swing.SwingWorker;

import model.ClassroomData;

public abstract class ClassroomDataFetcher extends SwingWorker<Integer, ClassroomData>  implements DataFetchListener{

	protected GoogleClassroomCommunicator authorize;
	private DataFetchListener listener;
	private FetchDoneListener fetchDoneListener;
	public ClassroomDataFetcher(GoogleClassroomCommunicator authorize, DataFetchListener listener) {
		this.authorize = authorize;
		this.listener = listener;
		fetchDoneListener = null;
	}

	public ClassroomDataFetcher(GoogleClassroomCommunicator authorize, DataFetchListener listener,
			FetchDoneListener fetchDoneListener) {
		super();
		this.authorize = authorize;
		this.listener = listener;
		this.fetchDoneListener = fetchDoneListener;
	}
	
	@Override
	public void retrievedInfo(ClassroomData data) {
		publish(data);	
	}
	
	

	@Override
	protected void process(List<ClassroomData> chunks) {
		int currentSize = chunks.size();
		for (int i = 0; i < currentSize; i++) {
			listener.retrievedInfo(chunks.get(i));
		}
	}



	@Override
	protected void done() {
		if (fetchDoneListener != null) {
			fetchDoneListener.done();
		}
	}



	
	
	
	
	
	

}
