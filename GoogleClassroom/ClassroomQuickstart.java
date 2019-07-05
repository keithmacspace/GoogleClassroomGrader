import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
import com.google.api.services.classroom.model.DriveFile;
import com.google.api.services.classroom.model.ListCourseWorkResponse;
import com.google.api.services.classroom.model.ListCoursesResponse;
import com.google.api.services.classroom.model.ListStudentSubmissionsResponse;
import com.google.api.services.classroom.model.ListStudentsResponse;
import com.google.api.services.classroom.model.Name;
import com.google.api.services.classroom.model.Student;
import com.google.api.services.classroom.model.StudentSubmission;
import com.google.api.services.classroom.model.UserProfile;
import com.google.api.services.drive.Drive;


public class ClassroomQuickstart {
    private static final String APPLICATION_NAME = "Google Classroom API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = new ArrayList<String>();
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        SCOPES.add(ClassroomScopes.CLASSROOM_COURSES_READONLY);
        SCOPES.add(ClassroomScopes.CLASSROOM_ROSTERS_READONLY);
        SCOPES.add(ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS_READONLY);
        SCOPES.add("https://www.googleapis.com/auth/drive https://www.googleapis.com/auth/drive.file");
        SCOPES.add("https://www.googleapis.com/auth/drive.appdata");
        SCOPES.add("https://www.googleapis.com/auth/drive.apps.readonly");

        
    	
        InputStream in = ClassroomQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Classroom service = new Classroom.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        
        Drive driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT)).setApplicationName(APPLICATION_NAME).build();

        // List the first 10 courses that the user has access to.
        ListCoursesResponse response = service.courses().list()
                .setPageSize(10)
                .execute();
        List<Course> courses = response.getCourses();
        if (courses == null || courses.size() == 0) {
            System.out.println("No courses found.");
        } else {
            System.out.println("Courses:");
            for (Course course : courses) {
                System.out.printf("%s\n", course.getName());
                System.out.println(course.getId());
                Map<String, String> studentMap = new HashMap<String, String>();
                ListStudentsResponse studentListResponse = service.courses().students().list(course.getId()).execute();
                for (Student student : studentListResponse.getStudents()) {
                	UserProfile studentProfile = student.getProfile();
                	Name name = studentProfile.getName();
                	studentMap.put(studentProfile.getId(), name.getFamilyName() + ", " + name.getGivenName());                	
                }
                System.out.println(studentMap);

                ListCourseWorkResponse courseListResponse = service.courses().courseWork().list(course.getId()).execute();
                for (CourseWork courseWork : courseListResponse.getCourseWork()) {
                	System.out.println(courseWork.getTitle() + " " + courseWork.getCreationTime());
                	ListStudentSubmissionsResponse studentSubmissionResponse = service.courses().courseWork().studentSubmissions().list(course.getId(), courseWork.getId()).execute();
                	for (StudentSubmission submission : studentSubmissionResponse.getStudentSubmissions()) {
                		AssignmentSubmission assignmentSubmission = submission.getAssignmentSubmission();
						if (assignmentSubmission != null && assignmentSubmission.getAttachments() != null) {
							String studentNameKey = submission.getUserId();
							System.out.println("\tSubmission from " + studentNameKey + " : " + studentMap.get(studentNameKey));
							for (Attachment attachment : assignmentSubmission.getAttachments()) {
								DriveFile driveFile = attachment.getDriveFile();
								String title = driveFile.getTitle();
								if (title.contains(".java")) {
									System.out.println(driveFile.getTitle());
									


									ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
									driveService.files().get(driveFile.getId()).executeMediaAndDownloadTo(outputStream);
									String fileContents = outputStream.toString("US-ASCII");
									PrintWriter fileStream = new PrintWriter("c:\\temp\\" + driveFile.getTitle());
									fileStream.print(fileContents);									
									outputStream.close();
									fileStream.close();
									return;
								}
							}
							break;
						}
                	}
                }

            }
        }
        

    }
}