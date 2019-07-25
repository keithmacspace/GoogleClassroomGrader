package net.cdonald.googleClassroom.googleClassroomInterface;

import java.util.ArrayList;
import java.util.List;

import com.google.api.services.sheets.v4.model.ValueRange;

public class SaveStateData {
	private List<ValueRange> saveState;
	private String sheetName;
	public SaveStateData(String sheetName) {
		this.sheetName = sheetName;
		saveState = new ArrayList<ValueRange>();
	}
	
	public void addOneColumn(List<Object> column, int currentColumn) {

		List<List<Object> > tempSave = new ArrayList<List<Object>>();
		for (int i = 0; i < column.size(); i++) {
			tempSave.add(new ArrayList<Object>());
			tempSave.get(i).add(column.get(i));
		
		}
		String columnName = SheetAccessorInterface.columnNames[currentColumn];
		saveState.add(new ValueRange().setRange(sheetName + "!" + columnName + "1:" + columnName + column.size()).setValues(tempSave));
	}
	
	public void addOneRow(List<Object> row, int currentRow) {
		List<List<Object> > tempSave = new ArrayList<List<Object>>();
		List<Object> copy = new ArrayList<Object>(row); 
		tempSave.add(copy);
		String startColumn = SheetAccessorInterface.columnNames[0];
		String endColumn = SheetAccessorInterface.columnNames[row.size() - 1];
		String range = sheetName + "!" + startColumn + currentRow + ":" + endColumn + currentRow;
		saveState.add(new ValueRange().setRange(range).setValues(tempSave));		
	}
	
	public List<ValueRange> getSaveState() {
		return saveState;
	}
}
