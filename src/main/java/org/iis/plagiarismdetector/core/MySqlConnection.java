package org.iis.plagiarismdetector.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 *
 ** هو اللطیف **
 * 
 * @author Mostafa
 */

public class MySqlConnection {

	public static Connection Conn = null;
	static int num = 0;

	public static void startConnection(String dbName) {
		String url = DB_Settings.url;
		String encoding = DB_Settings.encoding;
		String driver = DB_Settings.driver;
		String userName = DB_Settings.userName;
		String password = DB_Settings.password;
		try {
			Class.forName(driver).newInstance();
			if (Conn == null || Conn.isClosed())
				Conn = DriverManager.getConnection(url + dbName + encoding,
						userName, password);
			else
				num++;
			if (num == 10000) {
				Conn.close();
				num = 0;
				Conn = DriverManager.getConnection(url + dbName, userName,
						password);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void endConnection() {
		/*
		 * try { Conn.close(); } catch (Exception e) { e.printStackTrace(); }
		 */
	}

	public ArrayList<String[]> DB_reader(String Query, String[] fields)
			throws SQLException {

		ArrayList<String[]> Results = new ArrayList<String[]>();
		ResultSet Result = null;
		Statement stmt = null;

		try {
			stmt = Conn.createStatement();
			if (stmt.execute(Query)) {
				Result = stmt.executeQuery(Query);
			} else {
				System.err.println("Query failed");
			}
			while (Result.next()) {

				String[] Res = new String[fields.length];
				for (int i = 0; i < fields.length; i++) {
					Res[i] = Result.getString(fields[i]);
				}
				Results.add(Res);
			}
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}
		return Results;
	}

	public static void DB_writer(String Query) throws SQLException {
		Statement stmt = null;
		try {
			stmt = Conn.createStatement();
			stmt.executeUpdate(Query);
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}
	}

	public static void DB_writer_Param(String Query, String[] param)
			throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = Conn.prepareStatement(Query);
			for (int ind = 0; ind < param.length; ind++)
				pstmt.setString(ind, param[ind]);
			int count = pstmt.executeUpdate();
			System.out.println(count + "row(s) affected");
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}
	}

	public static void DB_Creator(String dbName) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException, SQLException {
		String url = DB_Settings.url;
		String driver = DB_Settings.driver;
		String userName = DB_Settings.userName;
		String password = DB_Settings.password;
		String dropQuery = "DROP DATABASE " + dbName;
		String createQuery = "CREATE DATABASE "
				+ dbName
				+ "DEFAULT CHARACTER SET utf8  DEFAULT COLLATE utf8_general_ci;";
		try {
			Class.forName(driver).newInstance();
			Conn = DriverManager.getConnection(url, userName, password);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Statement stmt = null;
		try {
			stmt = Conn.createStatement();
			stmt.executeUpdate(dropQuery);
			stmt.executeUpdate(createQuery);
			Conn.close();
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}
	}

}
