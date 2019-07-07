package model;

public class FileData extends ClassroomData {

	String fileContents;
	String packageName;
	String className;
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
			System.out.println(packageName);
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
}
