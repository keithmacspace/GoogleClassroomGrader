package net.cdonald.googleClassroom.model;

import java.util.Map;

import net.cdonald.googleClassroom.gui.DebugLogDialog;

public class FileData extends ClassroomData {

	private String fileContents;
	private String packageName;
	private String className;
	private static final String DB_NAME = "JavaFiles";
	private boolean isRubricCode;

	public static String dbTableName(ClassroomData assignment) {
		return DB_NAME + assignment.getId();
	}

	public static enum fieldNames {
		STUDENT_ID, NAME, CREATION_TIME, FILE_CONTENTS
	}
	
	public FileData() {
		super();
	}

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

	public FileData(Map<String, String> dbInfo) {
		for (String fieldName : dbInfo.keySet()) {
			for (fieldNames field : fieldNames.values()) {
				if (fieldName.compareToIgnoreCase(field.toString()) == 0) {
					setDBValue(field, dbInfo.get(fieldName));
				}
			}
		}
	}

	public String getFileContents() {
		return fileContents;
	}

	public void setFileContents(String fileContents) {
		this.fileContents = fileContents;
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
		String[] dbNames = { superNames[ClassroomData.fieldNames.ID.ordinal()],
				superNames[ClassroomData.fieldNames.NAME.ordinal()],
				superNames[ClassroomData.fieldNames.DATE.ordinal()], fileContents };
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

	public boolean isRubricCode() {
		return isRubricCode;
	}

	public void setRubricCode(boolean isRubricCode) {
		this.isRubricCode = isRubricCode;
	}

	public void instrumentFile(String instrumentCall) {
		addInstrumentation("while", instrumentCall);
		addInstrumentation("for", instrumentCall);

	}

	private void addInstrumentation(String conditionalName, String instrumentCall) {
		int condLoc = 0;
		String tempText = fileContents;
		do {
			condLoc = conditionalStartLocation(tempText, conditionalName, condLoc);

			if (condLoc != -1) {
				tempText = addInstrumentation(tempText, condLoc, instrumentCall);
				condLoc++;
			}
		} while (condLoc != -1);
		fileContents = tempText;
	}

	private String addInstrumentation(String fileText, int location, String instrumentationLine) {
		String tempText = fileText.substring(0, location + 1);
		tempText += instrumentationLine;
		tempText += fileText.substring(location + 1);
		return tempText;
	}

	/**
	 * 
	 * Pass in "if" "while" or "for" and it will return the location of the starting
	 * {
	 */

	private int conditionalStartLocation(String fileText, String word, int location) {
		if (location < fileText.length()) {
			int loc = findStringSkipComments(fileText, word, location);

			if (loc != -1 && loc < fileText.length()) {
				if (loc != 0) {
					char priorChar = fileText.charAt(loc - 1);
					if (Character.isWhitespace(priorChar) == false && priorChar != ';' && priorChar != '}') {
						return conditionalStartLocation(fileText, word, loc + 1);
					}
				}
				int parenLoc = findExpectedChar(fileText, '(', loc + word.length());
				if (parenLoc == -1) {
					return conditionalStartLocation(fileText, word, loc + 1);
				}
				int closingParen = findClosingBracketMatchIndex(fileText, parenLoc);
				if (closingParen == -1) {
					return conditionalStartLocation(fileText, word, loc + 1);
				}
				int curlyBrace = findExpectedChar(fileText, '{', closingParen + 1);
				if (curlyBrace == -1) {
					return conditionalStartLocation(fileText, word, loc + 1);
				}
				return curlyBrace;
			}
		}
		return -1;
	}

	private static int findStringSkipComments(String str, String wordToFind, int pos) {
		while (pos < str.length()) {
			pos = skipComment(str, pos);
			if (pos + wordToFind.length() < str.length()) {
				String check = str.substring(pos, pos + wordToFind.length());
				if (check.equals(wordToFind)) {
					return pos;
				}
			}
			pos++;
		}
		return pos;
	}

	/**
	 * Searches forward for an expected character, if it finds anything other than
	 * whitepaces, returns -1;
	 * 
	 */
	private static int findExpectedChar(String str, char expectedChar, int pos) {
		while (pos < str.length()) {
			pos = skipComment(str, pos);
			if (pos < str.length()) {
				char check = str.charAt(pos);
				if (check == expectedChar) {
					return pos;
				}
				if (Character.isWhitespace(check) == false) {
					return -1;
				}
				pos++;
			}
		}
		return -1;
	}

	private static int skipComment(String str, int pos) {
		if (pos < str.length() - 1) {
			if (str.charAt(pos) == '/') {
				if (str.charAt(pos + 1) == '/') {
					int newLine = str.indexOf('\n', pos + 1);
					if (newLine == -1) {
						return str.length();
					}
					return skipComment(str, newLine + 1);
				}
				if (str.charAt(pos + 1) == '*') {
					int nextAsterisk = pos + 2;
					do {
						nextAsterisk = str.indexOf('*', nextAsterisk);
						if (nextAsterisk != -1) {
							nextAsterisk++;
							if (nextAsterisk < str.length()) {
								if (str.charAt(nextAsterisk) == '/') {
									return skipComment(str, nextAsterisk + 1);
								}
							}
						}
					} while (nextAsterisk < str.length());
				}
			}
		}
		return pos;
	}

	private static int findClosingBracketMatchIndex(String str, int pos) {
		int depth = 1;
		pos++;
		while (pos < str.length()) {
			pos = skipComment(str, pos);
			if (pos < str.length()) {
				switch (str.charAt(pos)) {
				case '(':
					depth++;
					break;
				case ')':
					if (--depth == 0) {
						return pos;
					}
					break;
				}
				pos++;
			}
		}
		return -1; // No matching closing parenthesis
	}

	public static void main(String[] args) {
		FileData fileData = new FileData("file.java", "", "0", null);
		String contents = "// this is a test\n" + "while(a != b && (c != d))\n /*{\n" + "here is a comment\n" + "*/{\n";
		fileData.setFileContents(contents);
		fileData.instrumentFile("words.words.call()");
		System.out.println(fileData.getFileContents());

	}

}
