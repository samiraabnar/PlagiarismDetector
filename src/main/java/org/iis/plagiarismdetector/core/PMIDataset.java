package org.iis.plagiarismdetector.core;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class PMIDataset {

	public static MySqlConnection mySqlCon = new MySqlConnection();

	public static void saveNormalizedPMI(String fstToken, String secondToken,
			Integer distance, double npmi) throws SQLException {
		if (!containsPMI(fstToken, secondToken, distance)) {
			String Query = "INSERT INTO  normalized_pmis (fstword,sndword,distance,npmi) VALUES(?,?,?,?)";
			if ((MySqlConnection.Conn == null)
					|| !MySqlConnection.Conn.isValid(0))
				MySqlConnection.startConnection("pmis");
			java.sql.PreparedStatement pstmt = null;
			try {
				pstmt = MySqlConnection.Conn.prepareStatement(Query);
				pstmt.setString(1, fstToken);
				pstmt.setString(2, secondToken);
				pstmt.setInt(3, distance);
				pstmt.setDouble(4, npmi);

				// System.out.print(Query);
				pstmt.executeUpdate();
				// System.out.println(count + "row(s) affected");
			} catch (SQLException ex) {
				System.out.println("SQLException: " + ex.getMessage());
				System.out.println("SQLState: " + ex.getSQLState());
				System.out.println("VendorError: " + ex.getErrorCode());
			}
		}
	}

	public static void savePMI(String fstToken, String secondToken,
			Integer distance, double pmi) throws SQLException {
		if (!containsPMI(fstToken, secondToken, distance)) {
			String Query = "INSERT INTO  pmis (fstword,sndword,distance,pmi) VALUES(?,?,?,?)";
			if ((MySqlConnection.Conn == null)
					|| !MySqlConnection.Conn.isValid(0))
				MySqlConnection.startConnection("pmis");
			java.sql.PreparedStatement pstmt = null;
			try {
				pstmt = MySqlConnection.Conn.prepareStatement(Query);
				pstmt.setString(1, fstToken);
				pstmt.setString(2, secondToken);
				pstmt.setInt(3, distance);
				pstmt.setDouble(4, pmi);
				pstmt.executeUpdate();
			} catch (SQLException ex) {
				System.out.println("SQLException: " + ex.getMessage());
				System.out.println("SQLState: " + ex.getSQLState());
				System.out.println("VendorError: " + ex.getErrorCode());
			}
		}

	}

	public static void saveHits(String token, BigDecimal hits)
			throws SQLException {
		if (!contains(token)) {
			String Query = "INSERT INTO  hits (word,hit) VALUES(?,?)";
			if ((MySqlConnection.Conn == null)
					|| !MySqlConnection.Conn.isValid(0))
				MySqlConnection.startConnection("pmis");
			java.sql.PreparedStatement pstmt = null;
			try {
				pstmt = MySqlConnection.Conn.prepareStatement(Query);
				pstmt.setString(1, token);
				pstmt.setString(2, hits.toString());

				// System.out.print(Query);
				pstmt.executeUpdate();
				// System.out.println(count + "row(s) affected");
			} catch (SQLException ex) {
				System.out.println("SQLException: " + ex.getMessage());
				System.out.println("SQLState: " + ex.getSQLState());
				System.out.println("VendorError: " + ex.getErrorCode());
			}
		}
	}

	public static boolean contains(String token) throws SQLException {
		if ((MySqlConnection.Conn == null) || !MySqlConnection.Conn.isValid(0))
			MySqlConnection.startConnection("pmis");
		String query = "SELECT COUNT(*) as resultCount FROM hits where word = ?";
		java.sql.PreparedStatement pstmt = MySqlConnection.Conn
				.prepareStatement(query);
		pstmt.setString(1, token);
		ResultSet result = pstmt.executeQuery();
		int count = 0;
		if (result.next())
			count = result.getInt("resultCount");
		return count > 0;
	}

	public static BigDecimal getHits(String token) throws SQLException {
		if ((MySqlConnection.Conn == null) || !MySqlConnection.Conn.isValid(0))
			MySqlConnection.startConnection("pmis");
		String query = "SELECT hit FROM hits where word='" + token + "'";
		String[] Fields = { "hit" };

		// System.out.println(query);
		ArrayList<String[]> hits = mySqlCon.DB_reader(query, Fields);

		return new BigDecimal(hits.get(0)[0]);
	}

	public static double getNormalizedPMI(String fstToken, String sndToken,
			int distance) throws SQLException {
		if ((MySqlConnection.Conn == null) || !MySqlConnection.Conn.isValid(0))
			MySqlConnection.startConnection("pmis");
		String query = "SELECT  npmi FROM normalized_pmis where fstword = ? and sndword = ? and distance = ? ";
		java.sql.PreparedStatement pstmt = MySqlConnection.Conn
				.prepareStatement(query);
		pstmt.setString(1, fstToken);
		pstmt.setString(2, sndToken);
		pstmt.setDouble(3, distance);

		ResultSet result = pstmt.executeQuery();

		double npmi = 0;
		if (result.next())
			npmi = result.getDouble("npmi");

		return npmi;
	}

	public static double getPMI(String fstToken, String sndToken, int distance)
			throws SQLException {
		if ((MySqlConnection.Conn == null) || !MySqlConnection.Conn.isValid(0))
			MySqlConnection.startConnection("pmis");
		String query = "SELECT  pmi FROM pmis where fstword= ? and sndword= ? and distance= ? ";
		java.sql.PreparedStatement pstmt = MySqlConnection.Conn
				.prepareStatement(query);
		pstmt.setString(1, fstToken);
		pstmt.setString(2, sndToken);
		pstmt.setDouble(3, distance);

		ResultSet result = pstmt.executeQuery();

		double pmi = 0;
		if (result.next())
			pmi = result.getDouble("pmi");

		return pmi;
	}

	public static boolean containsNormalizedPMI(String fstToken,
			String sndToken, int distance) throws SQLException {
		if ((MySqlConnection.Conn == null) || !MySqlConnection.Conn.isValid(0))
			MySqlConnection.startConnection("pmis");
		String query = "SELECT  COUNT(*) as resultCount FROM normalized_pmis where fstword= ? and sndword= ? and distance= ? ";
		java.sql.PreparedStatement pstmt = MySqlConnection.Conn
				.prepareStatement(query);
		pstmt.setString(1, fstToken);
		pstmt.setString(2, sndToken);
		pstmt.setDouble(3, distance);

		ResultSet result = pstmt.executeQuery();

		int count = 0;
		if (result.next())
			count = result.getInt("resultCount");

		return count > 0;
	}

	public static boolean containsPMI(String fstToken, String sndToken,
			int distance) throws SQLException {
		if ((MySqlConnection.Conn == null) || !MySqlConnection.Conn.isValid(0))
			MySqlConnection.startConnection("pmis");
		String query = "SELECT  COUNT(*) as resultCount FROM pmis where fstword= ? and sndword= ? and distance= ? ";
		java.sql.PreparedStatement pstmt = MySqlConnection.Conn
				.prepareStatement(query);
		pstmt.setString(1, fstToken);
		pstmt.setString(2, sndToken);
		pstmt.setDouble(3, distance);

		ResultSet result = pstmt.executeQuery();

		int count = 0;
		if (result.next())
			count = result.getInt("resultCount");

		return count > 0;
	}

}
