package net.cdonald.googleClassroom.model;

import java.util.Map;

import net.cdonald.googleClassroom.model.FileData.fieldNames;

public class GoogleSheetData extends ClassroomData {
	private String sheetID;
	public enum fieldNames{URL_ID, NAME, SHEET_ID}
	public static final String DB_NAME = "Rubric_Names";
	public GoogleSheetData(String name, String urlID, String sheetID) {
		super(name, urlID + "?gid=" + sheetID);
		this.sheetID = sheetID;
	}
	
	public GoogleSheetData(Map<String, String> dbInfo) {
		for (String fieldName : dbInfo.keySet()) {
			for (fieldNames field : fieldNames.values()) {
				if (fieldName.compareToIgnoreCase(field.toString()) == 0) {
					setDBValue(field, dbInfo.get(fieldName));
				}
			}
		}
	}
	
	public void setDBValue(fieldNames field, String value) {
		switch (field) {
		case URL_ID:
			super.setDBValue(ClassroomData.fieldNames.ID, value);
			break;
		case NAME:
			super.setDBValue(ClassroomData.fieldNames.NAME, value);
			break;
		case SHEET_ID:
			sheetID = value;
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	public String getSheetID() {
		return sheetID;
	}
	
	public String[] getDBValues() {
		String [] superString = super.getDBValues();
		String [] dbString = {superString[0], superString[1], sheetID};
		return dbString;
	}
	
}
