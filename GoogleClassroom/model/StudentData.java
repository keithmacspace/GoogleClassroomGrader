package model;

import java.util.Date;

public class StudentData extends ClassroomData {
	private String firstName;

	public StudentData(String firstName, String lastName, String id, Date creationTime) {
		super(lastName, id, creationTime);
		this.firstName = firstName;
	}
	
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

}
