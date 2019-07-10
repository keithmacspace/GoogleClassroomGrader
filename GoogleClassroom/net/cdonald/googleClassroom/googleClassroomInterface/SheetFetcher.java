package net.cdonald.googleClassroom.googleClassroomInterface;



public class SheetFetcher extends ClassroomDataFetcher {

	private String url;
	public SheetFetcher(String url, GoogleClassroomCommunicator authorize, DataFetchListener listener,
			FetchDoneListener fetchDoneListener) {
		super(authorize, listener, fetchDoneListener);
		this.url = url;
	}

	@Override
	protected Integer doInBackground() throws Exception {
		authorize.getSheetNames(url, this);
		// TODO Auto-generated method stub
		return null;
	}

}
