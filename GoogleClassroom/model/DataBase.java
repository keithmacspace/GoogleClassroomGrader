package model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataBase {
	private List<StudentData> students;
	Connection conn;
	public DataBase() {
		super();
		students = new ArrayList<StudentData>();
		conn = null;
	}
	
	
	public void connect() throws SQLException {
		if (conn == null) {
			String url = "jdbc:sqlite:c:/temp/test.db";
			conn = DriverManager.getConnection(url);
			conditionallyAddTable();
			System.out.println("Connection established");
		}
	}
	
	public void disconnect() {
		if (conn != null) {
			try {
				conn.close();
				System.out.println("Connection closed");
				conn = null;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}
	private String buildSQL(String postFieldString, String postBuildString, boolean useFieldNames) {
		String sqlString = "";
		int count = 0;
		/**
		for (Person.FieldNames fieldName : Person.FieldNames.values()) {
			if (count != 0) {
				sqlString += ", ";
			}
			if (useFieldNames) {
				sqlString += fieldName.toString();
			}
			sqlString += postFieldString;
			count++;
		}
		*/
		return sqlString + postBuildString;
	}
	
	public void save()  throws SQLException{
		String insertValues = "values (" + buildSQL("?", ")", false);
		String insertSQL = "insert into students (" + buildSQL("", ") " + insertValues, true);
		String updateSQL = "update students set " + buildSQL("=?", " where id=?", true);
			
		PreparedStatement checkStmt = conn.prepareStatement("SELECT count(*) as count FROM students WHERE id=?");
		PreparedStatement insertStmt = conn.prepareStatement(insertSQL);
		PreparedStatement updateStmt = conn.prepareStatement(updateSQL);
		for (StudentData student : students) {			
			String id = student.getId();
			String [] mainData = {student.getName(), student.getFirstName()};			
			checkStmt.setString(1, id);			
			ResultSet checkResult = checkStmt.executeQuery();
			checkResult.next();
			int count = checkResult.getInt(1);
			int col = 1;
			PreparedStatement stmtToUse = null; 
			if (count == 0) {
				stmtToUse = insertStmt;
			}
			else {
				stmtToUse = updateStmt;
			}

			for(String value : mainData) {
				stmtToUse.setString(col++, value);
			}


			// Update has one last field, we use the id a second time for the query
			if (count != 0) {
				stmtToUse.setString(col++, id);
			}			
			stmtToUse.executeUpdate();
		}
		updateStmt.close();			
		insertStmt.close();		
		checkStmt.close();
	}
	
	public void load() throws SQLException {

		students.clear();
		final String selectSQL = "SELECT " + buildSQL("", " FROM students", true);
		Statement selectStmt = conn.createStatement();
		ResultSet results = selectStmt.executeQuery(selectSQL);
		/*
		while (results.next()) {
			Map<Person.FieldNames, String> fields = new HashMap<Person.FieldNames, String>();
			for (Person.FieldNames fieldName : Person.FieldNames.values()) {
				fields.put(fieldName, results.getString(fieldName.toString()));
			}
			addPerson(new Person(fields));			
		}
		*/
		selectStmt.close();
		
	}
	
	private void conditionallyAddTable() {
		try {
			if (conn != null) {
				boolean found = false;
				DatabaseMetaData dbmd = conn.getMetaData();
				String table[] = {"TABLE"};
				ResultSet rs = dbmd.getTables(null, null, null, table);
				while (rs.next()) {
					if (rs.getString("TABLE_NAME").compareToIgnoreCase("students") == 0) {
						found = true;
						break;
					}
				}
				if (found == false) {
					String sql = "CREATE TABLE PEOPLE " +
							"(id INTEGER not NULL, " +
							" name VARCHAR(255), " +
							" occupation VARCHAR(255), " +
							" age VARCHAR(255), " + //ENUM ('CHILD', 'ADULT', 'SENIOR')," +
							" employment VARCHAR(255), " +
							" taxID VARCHAR(255), " +
							" citizen INTEGER, " +
							" gender VARCHAR(255), " +
						    " PRIMARY KEY (id))";
					PreparedStatement stmt = conn.prepareStatement(sql);
					stmt.executeUpdate();					
				}
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	/*
	public void addPerson(Person person) {
		students.add(person);
	}
	
	public void removePerson(int index) {
		students.remove(index);
	}
	
	public List<Person> getPeople() {
		return Collections.unmodifiableList(students);
	}
	
	public void saveToFile(File file) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		ObjectOutputStream stream = new ObjectOutputStream(out);
		
		Person [] persons = students.toArray(new Person[students.size()]);
		stream.writeObject(persons);
		
		stream.writeObject(persons);
		
		stream.close();
	}
	
	public boolean loadFromFile(File file) throws IOException {
		FileInputStream in = new FileInputStream(file);
		ObjectInputStream stream = new ObjectInputStream(in);
		
		try {
			Person [] persons =(Person[]) stream.readObject();
			students.clear();
			students.addAll(Arrays.asList(persons));
		}
		catch (ClassNotFoundException x) {
			stream.close();
			return false;
		}
		stream.close();
		return true;
	}
	*/
}
