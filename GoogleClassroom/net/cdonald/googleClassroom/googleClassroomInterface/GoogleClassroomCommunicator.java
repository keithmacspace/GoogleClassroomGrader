package net.cdonald.googleClassroom.googleClassroomInterface;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.ClassroomScopes;
import com.google.api.services.classroom.model.AssignmentSubmission;
import com.google.api.services.classroom.model.Attachment;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.Date;
import com.google.api.services.classroom.model.DriveFile;
import com.google.api.services.classroom.model.ListCourseWorkResponse;
import com.google.api.services.classroom.model.ListCoursesResponse;
import com.google.api.services.classroom.model.ListStudentSubmissionsResponse;
import com.google.api.services.classroom.model.ListStudentsResponse;
import com.google.api.services.classroom.model.Name;
import com.google.api.services.classroom.model.Student;
import com.google.api.services.classroom.model.StudentSubmission;
import com.google.api.services.classroom.model.TimeOfDay;
import com.google.api.services.classroom.model.UserProfile;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesResponse;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.ImmutableList;

import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.StudentData;

public class GoogleClassroomCommunicator {
	private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES = new ImmutableList.Builder<String>()
			.add(ClassroomScopes.CLASSROOM_COURSES_READONLY).add(ClassroomScopes.CLASSROOM_ROSTERS_READONLY)
			.add(ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS_READONLY).add(DriveScopes.DRIVE)
			.add(DriveScopes.DRIVE_FILE).add(DriveScopes.DRIVE_APPDATA).add(DriveScopes.DRIVE_METADATA)
			.add(SheetsScopes.SPREADSHEETS, SheetsScopes.DRIVE, SheetsScopes.DRIVE_FILE).build();

	NetHttpTransport httpTransport;
	private String applicationName;
	private String tokensDirectoryPath;
	private String credentialsFilePath;
	private Classroom classroomService;
	private Drive driveService;
	private Sheets sheetsService;
	private static Semaphore readAssignmentsSemaphore = new Semaphore(1);
	private static Semaphore getCredentialsSemaphore = new Semaphore(1);
	private static Semaphore readStudentsSemaphore = new Semaphore(1);
	private static Semaphore readStudentsWorkSemaphore = new Semaphore(1);
	private static boolean cancelCurrentAssignmentRead = false;
	private static boolean cancelCurrentStudentRead = false;
	private static boolean cancelCurrentStudentWorkRead = false;
	private final String[] columnNames = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
			"P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };

	public GoogleClassroomCommunicator(String applicationName, String tokensDirectoryPath, String credentialsFilePath)
			throws IOException, GeneralSecurityException {
		this.applicationName = applicationName;
		this.tokensDirectoryPath = tokensDirectoryPath.replace('\\', '/');
		this.credentialsFilePath = credentialsFilePath;// .replace('\\', '/');
		httpTransport = GoogleNetHttpTransport.newTrustedTransport();
	}

	private void initServices() throws IOException {
		try {
			getCredentialsSemaphore.acquire();
		} catch (InterruptedException e) {
			getCredentialsSemaphore.release();
		}
		// Build a new authorized API client service.

		if (classroomService == null || driveService == null || sheetsService == null) {
			Credential credentials = getCredentials();
			if (classroomService == null) {
				classroomService = new Classroom.Builder(httpTransport, JSON_FACTORY, credentials)
						.setApplicationName(applicationName).build();
			}
			if (driveService == null) {
				driveService = new Drive.Builder(httpTransport, JSON_FACTORY, credentials)
						.setApplicationName(applicationName).build();
			}
			if (sheetsService == null) {
				sheetsService = new Sheets.Builder(httpTransport, JSON_FACTORY, credentials)
						.setApplicationName(applicationName).build();
			}
		}
		getCredentialsSemaphore.release();
	}

	private Credential getCredentials() throws IOException {

		InputStream in = new FileInputStream(credentialsFilePath);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDirectoryPath)))
						.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		Credential cred = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

		return cred;
	}

	private boolean acquireReadAssignmentSemaphore() {
		boolean worked = true;
		try {
			readAssignmentsSemaphore.acquire();
		} catch (InterruptedException e) {
			worked = false;
			readAssignmentsSemaphore.release();
		}
		cancelCurrentAssignmentRead = false;
		return worked;
	}

	private boolean acquireReadStudentsSemaphore() {
		boolean worked = true;
		try {
			readStudentsSemaphore.acquire();
		} catch (InterruptedException e) {
			readStudentsSemaphore.release();
			worked = false;
		}
		cancelCurrentStudentRead = false;
		return worked;
	}

	private boolean acquireReadStudentsWorkSemaphore() {
		boolean worked = true;
		try {
			readStudentsWorkSemaphore.acquire();
		} catch (InterruptedException e) {
			worked = false;
			readStudentsWorkSemaphore.release();
		}
		worked = true;
		cancelCurrentStudentWorkRead = false;
		return worked;
	}

	public void getClasses(DataFetchListener fetchListener) throws IOException {
		initServices();
		try {
			ListCoursesResponse response = classroomService.courses().list().execute();
			List<Course> courses = response.getCourses();
			for (Course course : courses) {
				ClassroomData data = new ClassroomData(course.getName(), course.getId(), course.getCreationTime());
				data.setRetrievedFromGoogle(true);
				fetchListener.retrievedInfo(data);
			}
		} catch (IOException e) {
			throw e;
		}

	}

	public void getStudents(ClassroomData course, DataFetchListener fetchListener) throws IOException {
		if (course.isEmpty()) {
			return;
		}
		try {
			cancelCurrentStudentWorkRead = true;
			acquireReadStudentsSemaphore();
			initServices();
			ListStudentsResponse studentListResponse = classroomService.courses().students().list(course.getId())
					.execute();
			for (Student student : studentListResponse.getStudents()) {
				UserProfile studentProfile = student.getProfile();
				if (cancelCurrentStudentRead) {
					break;
				}
				Name name = studentProfile.getName();
				StudentData data = new StudentData(name.getGivenName(), name.getFamilyName(), studentProfile.getId(),
						course.getDate());
				data.setRetrievedFromGoogle(true);
				fetchListener.retrievedInfo(data);
			}
		} catch (IOException e) {
			readStudentsSemaphore.release();
			throw e;
		}

		readStudentsSemaphore.release();
	}

	public void getAssignments(ClassroomData course, DataFetchListener fetchListener) throws IOException {
		if (course.isEmpty()) {
			return;
		}
		cancelCurrentAssignmentRead = true;
		acquireReadAssignmentSemaphore();
		acquireReadStudentsWorkSemaphore();
		try {
			initServices();

			ListCourseWorkResponse courseListResponse = classroomService.courses().courseWork().list(course.getId())
					.execute();

			for (CourseWork courseWork : courseListResponse.getCourseWork()) {
				if (cancelCurrentAssignmentRead) {
					break;
				}

				Date date = courseWork.getDueDate();
				TimeOfDay timeOfDay = courseWork.getDueTime();
				if (date != null && timeOfDay != null) {
					Integer hours = timeOfDay.getHours();
					Integer minutes = timeOfDay.getMinutes();
					Calendar temp = new GregorianCalendar(date.getYear(), date.getMonth(), date.getDay(),
							(hours == null) ? 0 : hours, (minutes == null) ? 0 : minutes);
					java.util.Date dueDate = temp.getTime();
					ClassroomData data = new ClassroomData(courseWork.getTitle(), courseWork.getId(), dueDate);
					data.setRetrievedFromGoogle(true);
					fetchListener.retrievedInfo(data);
				}
			}
		} catch (IOException e) {
			readStudentsWorkSemaphore.release();
			readAssignmentsSemaphore.release();
			throw e;
		}

		readStudentsWorkSemaphore.release();
		readAssignmentsSemaphore.release();
	}

	public void getStudentWork(ClassroomData course, ClassroomData assignment, DataFetchListener fetchListener)
			throws IOException {
		if (course.isEmpty() || assignment.isEmpty()) {
			return;
		}

		try {

			acquireReadStudentsWorkSemaphore();

			initServices();
			ListStudentSubmissionsResponse studentSubmissionResponse = classroomService.courses().courseWork()
					.studentSubmissions().list(course.getId(), assignment.getId()).execute();
			for (StudentSubmission submission : studentSubmissionResponse.getStudentSubmissions()) {
				if (cancelCurrentStudentWorkRead) {
					break;
				}
				AssignmentSubmission assignmentSubmission = submission.getAssignmentSubmission();
				if (assignmentSubmission != null && assignmentSubmission.getAttachments() != null) {
					String studentNameKey = submission.getUserId();
					for (Attachment attachment : assignmentSubmission.getAttachments()) {
						if (cancelCurrentStudentWorkRead) {
							break;
						}
						DriveFile driveFile = attachment.getDriveFile();
						String title = driveFile.getTitle();
						if (title.contains(".java")) {

							ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
							driveService.files().get(driveFile.getId()).executeMediaAndDownloadTo(outputStream);
							String fileContents = outputStream.toString("US-ASCII");
							ClassroomData data = new FileData(driveFile.getTitle(), fileContents, studentNameKey,
									submission.getUpdateTime());
							data.setRetrievedFromGoogle(true);
							fetchListener.retrievedInfo(data);
							outputStream.close();
						}
					}
				}
			}
		} catch (IOException e) {
			readStudentsWorkSemaphore.release();
			throw e;
		}
		readStudentsWorkSemaphore.release();
	}

	private String googleSheetID(String sheetURL) {

		final Pattern p = Pattern.compile("/spreadsheets/d/([a-zA-Z0-9-_]+)");
		Matcher m = p.matcher(sheetURL);
		String id = "";
		if (m.find()) {
			// System.err.println("m find");
			for (int i = 0; i < m.groupCount(); i++) {
				// System.err.println("M + " + i + " " + m.group(i));
			}
			id = m.group(0).substring(1).replace("/spreadsheets/d/", "");
		} else {
			final String ID_QUERY = "?id=";
			int idIndex = sheetURL.indexOf(ID_QUERY);
			if (idIndex != -1) {
				id = sheetURL.substring(idIndex + ID_QUERY.length());
			}
			int slashIndex = id.lastIndexOf('/');
			if (slashIndex != -1) {
				id = id.substring(slashIndex + 1);
			}
		}

		// System.err.println(id);
		return id;
	}

	public List<Sheet> getSheetNames(String sheetURL, DataFetchListener fetchListener) throws IOException {
		initServices();
		//System.err.println("Sheet url = " + sheetURL);
		String id = googleSheetID(sheetURL);
		// System.err.println("id = " + id);
		Spreadsheet spreadSheet = sheetsService.spreadsheets().get(id).execute();
		List<Sheet> innerSheets = spreadSheet.getSheets();
		for (Sheet sheet : innerSheets) {
			if (fetchListener != null) {
				ClassroomData data = new GoogleSheetData(sheet.getProperties().getTitle(),
						id,
						sheet.getProperties().getSheetId().toString());
				data.setRetrievedFromGoogle(true);
				fetchListener.retrievedInfo(data);
				// System.err.println(sheet.getProperties().getTitle());
			}
		}
		return innerSheets;
	}

	public void fillRubric(String sheetURL, Rubric rubric) throws IOException {
		initServices();
		String id = googleSheetID(sheetURL);
		String name = rubric.getName();
		String range = name + "!A1:Z1000";
		ValueRange response = sheetsService.spreadsheets().values().get(id, range).execute();
		List<List<Object>> values = response.getValues();
		rubric.addEntries(values);
	}

	public String writeSheet(String sheetURL, String sheetName, List<List<Object>> values) throws IOException {
		initServices();
		String id = googleSheetID(sheetURL);
		boolean newSheet = true;
		List<Sheet> existing = getSheetNames(sheetURL, null);

		for (Sheet sheet : existing) {
			String name = sheet.getProperties().getTitle();
			if (name.compareToIgnoreCase(sheetName) == 0) {
				sheetName = name;
				newSheet = false;
			}
		}
		if (newSheet == true) {
			AddSheetRequest addRequest = new AddSheetRequest();
			SheetProperties sheetProperties = new SheetProperties();
			addRequest.setProperties(sheetProperties);
			addRequest.setProperties(sheetProperties.setTitle(sheetName));

			BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();

			// Create requestList and set it on the batchUpdateSpreadsheetRequest
			List<Request> requestsList = new ArrayList<Request>();
			batchUpdateSpreadsheetRequest.setRequests(requestsList);

			// Create a new request with containing the addSheetRequest and add it to the
			// requestList
			Request request = new Request();
			request.setAddSheet(addRequest);
			requestsList.add(request);

			// Add the requestList to the batchUpdateSpreadsheetRequest
			batchUpdateSpreadsheetRequest.setRequests(requestsList);
			sheetsService.spreadsheets().batchUpdate(id, batchUpdateSpreadsheetRequest).execute();
		}
		int columnIndex = 0;
		List<ValueRange> data = new ArrayList<ValueRange>();
		for (List<Object> rowValues : values) {
			int rowIndex = 1;
			for (Object value : rowValues) {
				if (value == null || value instanceof String && ((String) value).length() == 0) {
					rowIndex++;
				}
				List<List<Object>> tempRow = new ArrayList<List<Object>>();
				tempRow.add(new ArrayList<Object>());
				tempRow.get(0).add(value);
				String range = sheetName + "!" + columnNames[columnIndex] + rowIndex + ":" + columnNames[columnIndex]
						+ rowIndex;
				data.add(new ValueRange().setRange(range).setValues(tempRow));
				rowIndex++;
			}
			columnIndex++;
		}

		BatchUpdateValuesRequest body = new BatchUpdateValuesRequest().setValueInputOption("RAW").setData(data);

		BatchUpdateValuesResponse result = sheetsService.spreadsheets().values().batchUpdate(id, body).execute();
		return result.toString();
	}

	public void listFoldersInRoot() throws IOException {
		initServices();
		FileList result = driveService.files().list()
				.setQ("'root' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
				.setSpaces("drive").setFields("nextPageToken, files(id, name, parents)").execute();
		List<com.google.api.services.drive.model.File> folders = result.getFiles();
		for (com.google.api.services.drive.model.File folder : folders) {
			//System.out.println(folder);
		}

	}

	public Map<File, List<File>> listChildItemsOfFolder(String searchParentFolderName) throws IOException {
		Map<File, List<File>> results = new HashMap<File, List<File>>();
		initServices();
		FileList result = driveService.files().list()
				.setQ(String.format(
						"name = '%s' and mimeType = 'application/vnd.google-apps.folder' and trashed = false",
						searchParentFolderName))
				.setSpaces("drive").setFields("nextPageToken, files(id, name, parents)").execute();

		List<File> foldersMatchingSearchName = result.getFiles();

		if (foldersMatchingSearchName != null && !foldersMatchingSearchName.isEmpty()) {
			for (File folder : foldersMatchingSearchName) {
				FileList childResult = driveService.files().list()
						.setQ(String.format("'%s' in parents and trashed = false", folder.getId())).setSpaces("drive")
						.setFields("nextPageToken, files(id, name, parents)").execute();

				List<File> childItems = childResult.getFiles();

				if (childItems != null && !childItems.isEmpty()) {
					results.put(folder, childItems);
				}
			}
		}

		return results;
	}

	public static void main(String[] args) throws IOException, GeneralSecurityException {

//		String temp = "https://drive.google.com/open?id=1qWx7zrmbKctD8XDx8TJ_3fecPgedE6fIumuPF41Cej8";
//		List<ArrayList<Object> > tempValues = new ArrayList<ArrayList<Object> >();
//		ArrayList<Object> col0 = new ArrayList<Object>();
//		col0.add("Name");
//		col0.add("Josh");
//		col0.add("Hi");
//		tempValues.add(col0);
//	
//		ArrayList<Object> col1 = new ArrayList<Object>();
//		col1.add("");
//		col1.add("");
//		col1.add("here");
//		col1.add("");
//		col1.add("");
//		col1.add("dog");
//		tempValues.add(col1);
//		GoogleClassroomCommunicator communicator = new GoogleClassroomCommunicator("Google Classroom Grader", "C:\\Users\\kdmacdon\\Documents\\Teals\\GoogleClassroomData\\tokens", "C:\\Users\\kdmacdon\\Documents\\Teals\\GoogleClassroomData\\credentials.json");
//		communicator.listFoldersInRoot();
//		System.out.println()
//		communicator.writeSheet(temp, "Test3", null, tempValues);
	}

}
