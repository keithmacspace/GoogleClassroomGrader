package googleClassroomInterface;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Semaphore;

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
import com.google.common.collect.ImmutableList;

import model.ClassroomData;
import model.FileData;
import model.StudentData;

public class GoogleClassroomCommunicator {
	private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES = new ImmutableList.Builder<String>()
			.add(ClassroomScopes.CLASSROOM_COURSES_READONLY).add(ClassroomScopes.CLASSROOM_ROSTERS_READONLY)
			.add(ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS_READONLY).add(DriveScopes.DRIVE)
			.add(DriveScopes.DRIVE_FILE).add(DriveScopes.DRIVE_APPDATA).add(DriveScopes.DRIVE_METADATA_READONLY)
			.build();

	NetHttpTransport httpTransport;
	private String applicationName;
	private String tokensDirectoryPath;
	private String credentialsFilePath;
	private Classroom classroomService;
	private Drive driveService;
	private static Semaphore readAssignmentsSemaphore = new Semaphore(1);
	private static Semaphore getCredentialsSemaphore = new Semaphore(1);
	private static Semaphore readStudentsSemaphore = new Semaphore(1);
	private static Semaphore readStudentsWorkSemaphore = new Semaphore(1);
	private static boolean cancelCurrentAssignmentRead = false;
	private static boolean cancelCurrentStudentRead = false;
	private static boolean cancelCurrentStudentWorkRead = false;

	public GoogleClassroomCommunicator(String applicationName, String tokensDirectoryPath, String credentialsFilePath)
			throws IOException, GeneralSecurityException {
		this.applicationName = applicationName;
		this.tokensDirectoryPath = tokensDirectoryPath;
		this.credentialsFilePath = credentialsFilePath;
		httpTransport = GoogleNetHttpTransport.newTrustedTransport();
	}

	private void initServices() throws IOException {
		try {
			getCredentialsSemaphore.acquire();
		} catch (InterruptedException e) {
			getCredentialsSemaphore.release();
		}
		// Build a new authorized API client service.

		if (classroomService == null) {
			classroomService = new Classroom.Builder(httpTransport, JSON_FACTORY, getCredentials())
					.setApplicationName(applicationName).build();
		}
		if (driveService == null) {
			driveService = new Drive.Builder(httpTransport, JSON_FACTORY, getCredentials())
					.setApplicationName(applicationName).build();
		}
		getCredentialsSemaphore.release();
	}

	private Credential getCredentials() throws IOException {

		InputStream in = GoogleClassroomCommunicator.class.getResourceAsStream(credentialsFilePath);
		if (in == null) {
			throw new FileNotFoundException("Resource not found: " + credentialsFilePath);
		}
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

	private void acquireReadAssignmentSemaphore() {
		
		cancelCurrentAssignmentRead = true;
		try {
			readAssignmentsSemaphore.acquire();
		} catch (InterruptedException e) {
			readAssignmentsSemaphore.release();
		}
		cancelCurrentAssignmentRead = false;
	}

	private void acquireReadStudentsSemaphore() {
		
		cancelCurrentStudentRead = true;
		try {
			readStudentsSemaphore.acquire();
		} catch (InterruptedException e) {
			readStudentsSemaphore.release();
		}
		cancelCurrentStudentRead = false;
	}

	private void acquireReadStudentsWorkSemaphore() {
		
		cancelCurrentStudentWorkRead = true;
		try {
			readStudentsWorkSemaphore.acquire();
		} catch (InterruptedException e) {
			readStudentsWorkSemaphore.release();
		}
		cancelCurrentStudentWorkRead = false;
	}

	public void getClasses(DataFetchListener fetchListener) throws IOException {

		initServices();		
		acquireReadAssignmentSemaphore();
		acquireReadStudentsSemaphore();
		try {
			ListCoursesResponse response = classroomService.courses().list().execute();
			List<Course> courses = response.getCourses();
			for (Course course : courses) {
				fetchListener.retrievedInfo(new ClassroomData(course.getName(), course.getId(), course.getCreationTime()));
			}
		} catch (IOException e) {
			readAssignmentsSemaphore.release();
			readStudentsSemaphore.release();
			throw e;
		}
		readAssignmentsSemaphore.release();
		readStudentsSemaphore.release();
	}


	public void getStudents(ClassroomData course, DataFetchListener fetchListener) throws IOException {
		if (course.isEmpty()) {
			return;
		}
		try {
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
				fetchListener.retrievedInfo(new StudentData(name.getGivenName(), name.getFamilyName(),
						studentProfile.getId(), course.getDate()));
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
					Calendar temp = new GregorianCalendar(date.getYear(), date.getMonth(), date.getDay(), (hours == null)?0:hours, (minutes == null)?0:minutes);
					
					java.util.Date dueDate = temp.getTime();
					fetchListener.retrievedInfo(new ClassroomData(courseWork.getTitle(), courseWork.getId(), dueDate));
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
							fetchListener.retrievedInfo(new FileData(driveFile.getTitle(), fileContents, studentNameKey,
									submission.getUpdateTime()));
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

}
