package net.cdonald.googleClassroom.googleClassroomInterface;

import java.util.List;

import com.google.api.services.sheets.v4.model.ValueRange;

import net.cdonald.googleClassroom.model.GoogleSheetData;

public interface SheetAccessorInterface {
	public final String[] columnNames = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
			"P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };
	public GoogleSheetData getSheetInfo();
	public String getNextRange(int rangeCount);
	public void setResponseData(List<List<Object> > sheetEntries, int rangeCount);
	public SaveStateData getSheetSaveState();

}
