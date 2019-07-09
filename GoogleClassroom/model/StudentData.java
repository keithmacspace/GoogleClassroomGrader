package model;

import java.util.Date;

public class StudentData extends ClassroomData {
	private String firstName;

	public static enum fieldNames {
		ID, LAST, FIRST
	};

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

	@Override
	public String[] getDBValues() {
		String[] superNames = super.getDBValues();
		String[] dbNames = { superNames[ClassroomData.fieldNames.ID.ordinal()],
				superNames[ClassroomData.fieldNames.NAME.ordinal()], firstName };
		return dbNames;
	}

	public void setDBValue(fieldNames field, String value) {
		switch (field) {
		case ID:
			super.setDBValue(ClassroomData.fieldNames.ID, value);
			break;
		case LAST:
			super.setDBValue(ClassroomData.fieldNames.NAME, value);
			break;
		case FIRST:
			firstName = value;
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

}
