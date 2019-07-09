package model;

import model.StudentData.fieldNames;

public class FileData extends ClassroomData {

	private String fileContents;
	private String packageName;
	private String className;
	public static enum fieldNames  {NAME, STUDENT_ID, CREATION_TIME, FILE_CONTENTS}
	public FileData(String name, String fileContents, String id, String creationTime) {
		super(name, id, creationTime);
		this.fileContents = fileContents;
		className = getName().replace(".java", "");									
		int packageIndex = fileContents.indexOf("package");
		packageName = "";
		if (packageIndex != -1) {
			int semicolonIndex = fileContents.indexOf(";", packageIndex);
			packageName = fileContents.substring(packageIndex + "package ".length(), semicolonIndex);
			packageName = packageName.replaceAll("\\s", "");			
			className = packageName + "." + className;
		}									
	}
	
	public String getFileContents() {
		return fileContents;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getClassName() {
		return className;
	}
	@Override
	public String[] getDBValues() {
		String[] superNames = super.getDBValues();
		String[] dbNames = { superNames[ClassroomData.fieldNames.NAME.ordinal()],
				superNames[ClassroomData.fieldNames.ID.ordinal()], 
				superNames[ClassroomData.fieldNames.DATE.ordinal()], 
				fileContents};
		return dbNames;
	}
	
	public void setDBValue(fieldNames field, String value) {
		switch (field) {
		case STUDENT_ID:
			super.setDBValue(ClassroomData.fieldNames.ID, value);
			break;
		case NAME:
			super.setDBValue(ClassroomData.fieldNames.NAME, value);
			break;
		case CREATION_TIME:
			super.setDBValue(ClassroomData.fieldNames.DATE, value);
			break;
		case FILE_CONTENTS:
			fileContents = value;
			break;
		default:
			throw new IllegalArgumentException();
		}
	}
}
