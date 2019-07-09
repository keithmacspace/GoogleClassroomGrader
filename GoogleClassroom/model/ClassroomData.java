package model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ClassroomData implements Comparable<ClassroomData> {
	private String name;
	private String id;
	private Date date;
	private boolean isEmpty;
	public static enum fieldNames {ID, NAME, DATE}

	static SimpleDateFormat formatter = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSSZ");
	public ClassroomData() {
		isEmpty = true;
		name = "None";
		id = "0";
		date = null;
	}

	public ClassroomData(String name, String id, String creationTime) {
		super();
		Date date = null;
		try {
			date = formatter.parse(creationTime.replaceAll("Z$",  "+0000"));
		} catch (ParseException e) {
		}
		init(name, id, date);
	}
	
	public ClassroomData(String name, String id, Date creationTime) {
		init(name, id, creationTime);
	}
	
	private void init(String name, String id, Date creationTime) {
		this.name = name;
		this.id = id;
		isEmpty = false;
		date = creationTime;

	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	public Date getDate() {
		return date;
	}

	public boolean isEmpty() {
		return isEmpty;
	}

	public void setEmpty(boolean isEmpty) {
		this.isEmpty = isEmpty;
	}

	public String toString() {
		return name;
	}

	@Override
	public int compareTo(ClassroomData o) {
		if (isEmpty != o.isEmpty()) {
			if (isEmpty) {
				return -1;
			}
			return 1;
		}
		if (o.date == null) {
			return 1;
		}
		if (date == null) {
			return -1;
		}
		int creationCompare = date.compareTo(o.date);
		if (creationCompare != 0) {
			return creationCompare;
		}

		return o.name.compareToIgnoreCase(name);
	}
	
	
	public String[] getDBValues() {
		String [] dbString = {getId(), getName(), "" + date.getTime()};
		return dbString;
		
	}
	
	public void setDBValue(fieldNames field, String value) {		
		switch (field) {
		case ID:
			id = value;
			break;
		case NAME:
			name = value;
			break;
		case DATE:
			date = new Date(Long.parseLong(value));
			break;
		default:
			throw new IllegalArgumentException("Missing enum value");
		}
	}
	

}
