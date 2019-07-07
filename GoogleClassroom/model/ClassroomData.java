package model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClassroomData implements Comparable<ClassroomData> {
	private String name;
	private String id;
	private Date creationDate;
	private boolean isEmpty;
	static SimpleDateFormat formatter = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSSZ");
	public ClassroomData() {
		isEmpty = true;
		name = "None";
		id = "0";
		creationDate = null;
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
		creationDate = creationTime;

	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	public Date getCreationDate() {
		return creationDate;
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
		if (o.creationDate == null) {
			return 1;
		}
		if (creationDate == null) {
			return -1;
		}
		int creationCompare = creationDate.compareTo(o.creationDate);
		if (creationCompare != 0) {
			return creationCompare;
		}

		return o.name.compareToIgnoreCase(name);
	}

}
